/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Engine.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: Nullable[A] for createComponent return
 *   Idiom: inner classes replaced by private methods/lambdas
 *   Idiom: ComponentOperationHandler takes () => Boolean (not BooleanInformer trait)
 *   Idiom: Listener[Entity] implemented via anonymous class
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 2
 * Covenant-baseline-loc: 234
 * Covenant-baseline-methods: Engine,addEntity,addEntityInternal,addEntityListener,addSystem,componentAdded,componentOperationHandler,componentRemoved,createComponent,createEntity,delayed,empty,entityAdded,entityManager,entityRemoved,familyManager,getEntities,getEntitiesFor,getSystem,getSystems,receive,removeAllEntities,removeAllSystems,removeEntity,removeEntityInternal,removeEntityListener,removeSystem,systemAdded,systemManager,systemRemoved,systems,update,updating
 * Covenant-source-reference: com/badlogic/ashley/core/Engine.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

import sge.ecs.signals.{ Listener, Signal }
import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable

/** The heart of the Entity framework. It is responsible for keeping track of [[Entity]] and managing [[EntitySystem]] objects. The Engine should be updated every tick via the [[update]] method.
  *
  * With the Engine you can:
  *   - Add/Remove [[Entity]] objects
  *   - Add/Remove [[EntitySystem]]s
  *   - Obtain a list of entities for a specific [[Family]]
  *   - Update the main loop
  *   - Register/unregister [[EntityListener]] objects
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
class Engine {

  private val empty: Family = Family.all().get()

  private val componentAdded: Listener[Entity] = new Listener[Entity] {
    override def receive(signal: Signal[Entity], obj: Entity): Unit =
      familyManager.updateFamilyMembership(obj)
  }

  private val componentRemoved: Listener[Entity] = new Listener[Entity] {
    override def receive(signal: Signal[Entity], obj: Entity): Unit =
      familyManager.updateFamilyMembership(obj)
  }

  private val systemManager: SystemManager = new SystemManager(
    new SystemManager.SystemListener {
      override def systemAdded(system: EntitySystem): Unit =
        system.addedToEngineInternal(Engine.this)
      override def systemRemoved(system: EntitySystem): Unit =
        system.removedFromEngineInternal(Engine.this)
    }
  )

  private val entityManager: EntityManager = new EntityManager(
    new EntityListener {
      override def entityAdded(entity: Entity): Unit =
        addEntityInternal(entity)
      override def entityRemoved(entity: Entity): Unit =
        removeEntityInternal(entity)
    }
  )

  private val componentOperationHandler: ComponentOperationHandler =
    new ComponentOperationHandler(() => updating)

  private val familyManager: FamilyManager = new FamilyManager(entityManager.getEntities)

  private var updating: Boolean = false

  /** Creates a new Entity object.
    * @return
    *   a new [[Entity]]
    */
  def createEntity(): Entity = new Entity()

  /** Creates a new [[Component]]. Override this method or use [[PooledEngine]] for component pooling.
    *
    * The default implementation throws — create components directly with `new` or override in a subclass.
    */
  def createComponent[T <: Component](componentType: Class[T]): Nullable[T] =
    throw new UnsupportedOperationException(
      "Engine.createComponent is not supported by default. " +
        "Create components with `new` directly, or use PooledEngine."
    )

  /** Adds an entity to this Engine. This will throw an IllegalArgumentException if the given entity was already registered with an engine.
    */
  def addEntity(entity: Entity): Unit = {
    val delayed = updating || familyManager.notifying
    entityManager.addEntity(entity, delayed)
  }

  /** Removes an entity from this Engine. */
  def removeEntity(entity: Entity): Unit = {
    val delayed = updating || familyManager.notifying
    entityManager.removeEntity(entity, delayed)
  }

  /** Removes all entities of the given [[Family]]. */
  def removeAllEntities(family: Family): Unit = {
    val delayed = updating || familyManager.notifying
    entityManager.removeAllEntities(getEntitiesFor(family), delayed)
  }

  /** Removes all entities registered with this Engine. */
  def removeAllEntities(): Unit = {
    val delayed = updating || familyManager.notifying
    entityManager.removeAllEntities(delayed)
  }

  /** Returns an [[ImmutableArray]] of [[Entity]] that is managed by the Engine but cannot be used to modify the state of the Engine. This Array is not Immutable in the sense that its contents will
    * not be modified, but in the sense that it only reflects the state of the engine.
    *
    * The Array is "Managed" by the Engine itself. The engine may add or remove items from the array and this will be reflected in the returned array.
    *
    * @return
    *   An unmodifiable array of entities that will match the state of the entities in the engine.
    */
  def getEntities: ImmutableArray[Entity] = entityManager.getEntities

  /** Adds the [[EntitySystem]] to this Engine. If the Engine already had a system of the same class, the new one will replace the old one.
    */
  def addSystem(system: EntitySystem): Unit =
    systemManager.addSystem(system)

  /** Removes the [[EntitySystem]] from this Engine. */
  def removeSystem(system: EntitySystem): Unit =
    systemManager.removeSystem(system)

  /** Removes all systems from this Engine. */
  def removeAllSystems(): Unit =
    systemManager.removeAllSystems()

  /** Quick [[EntitySystem]] retrieval. */
  def getSystem[T <: EntitySystem](systemType: Class[T]): Nullable[T] =
    systemManager.getSystem(systemType)

  /** @return immutable array of all entity systems managed by the [[Engine]]. */
  def getSystems: ImmutableArray[EntitySystem] = systemManager.getSystems

  /** Returns immutable collection of entities for the specified [[Family]]. Returns the same instance every time for the same Family.
    */
  def getEntitiesFor(family: Family): ImmutableArray[Entity] =
    familyManager.getEntitiesFor(family)

  /** Adds an [[EntityListener]].
    *
    * The listener will be notified every time an entity is added/removed to/from the engine.
    */
  def addEntityListener(listener: EntityListener): Unit =
    addEntityListener(empty, 0, listener)

  /** Adds an [[EntityListener]]. The listener will be notified every time an entity is added/removed to/from the engine. The priority determines in which order the entity listeners will be called.
    * Lower value means it will get executed first.
    */
  def addEntityListener(priority: Int, listener: EntityListener): Unit =
    addEntityListener(empty, priority, listener)

  /** Adds an [[EntityListener]] for a specific [[Family]].
    *
    * The listener will be notified every time an entity is added/removed to/from the given family.
    */
  def addEntityListener(family: Family, listener: EntityListener): Unit =
    addEntityListener(family, 0, listener)

  /** Adds an [[EntityListener]] for a specific [[Family]]. The listener will be notified every time an entity is added/removed to/from the given family. The priority determines in which order the
    * entity listeners will be called. Lower value means it will get executed first.
    */
  def addEntityListener(family: Family, priority: Int, listener: EntityListener): Unit =
    familyManager.addEntityListener(family, priority, listener)

  /** Removes an [[EntityListener]]. */
  def removeEntityListener(listener: EntityListener): Unit =
    familyManager.removeEntityListener(listener)

  /** Updates all the systems in this Engine.
    * @param deltaTime
    *   The time passed since the last frame.
    */
  def update(deltaTime: Float): Unit = {
    if (updating) {
      throw new IllegalStateException("Cannot call update() on an Engine that is already updating.")
    }

    updating = true
    val systems = systemManager.getSystems
    try {
      var i = 0
      while (i < systems.size) {
        val system = systems(i)

        if (system.checkProcessing) {
          system.update(deltaTime)
        }

        while (componentOperationHandler.hasOperationsToProcess || entityManager.hasPendingOperations) {
          componentOperationHandler.processOperations()
          entityManager.processPendingOperations()
        }

        i += 1
      }
    } finally
      updating = false
  }

  protected def addEntityInternal(entity: Entity): Unit = {
    entity.componentAdded.add(componentAdded)
    entity.componentRemoved.add(componentRemoved)
    entity.componentOperationHandler = componentOperationHandler

    familyManager.updateFamilyMembership(entity)
  }

  protected def removeEntityInternal(entity: Entity): Unit = {
    familyManager.updateFamilyMembership(entity)

    entity.componentAdded.remove(componentAdded)
    entity.componentRemoved.remove(componentRemoved)
    entity.componentOperationHandler = null.asInstanceOf[ComponentOperationHandler] // null on removal, guarded by null check in Entity
  }
}
