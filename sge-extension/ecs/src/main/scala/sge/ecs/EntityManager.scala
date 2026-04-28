/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/EntityManager.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: ArrayBuffer instead of Array; HashSet instead of ObjectSet
 *   Idiom: sge.utils.Pool trait instead of LibGDX Pool for EntityOperation recycling
 *   Idiom: Nullable[Entity] instead of null for EntityOperation fields
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 175
 * Covenant-baseline-methods: EntityManager,EntityOperation,EntityOperationPool,Type,addEntity,addEntityInternal,entities,entity,entityOperationPool,entitySet,getEntities,hasPendingOperations,i,immutableEntities,initialCapacity,max,newObject,operationType,pendingOperations,processPendingOperations,removeAllEntities,removeEntity,removeEntityInternal,removed,reset
 * Covenant-source-reference: com/badlogic/ashley/core/EntityManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable
import sge.utils.Pool

/** Manages entities within an [[Engine]]. Supports immediate and delayed add/remove operations.
  *
  * Delayed operations are queued when the engine is updating and flushed after each system update.
  */
private[ecs] class EntityManager(listener: EntityListener) {

  private val entities:            ArrayBuffer[Entity]          = ArrayBuffer.empty
  private val entitySet:           HashSet[Entity]              = HashSet.empty
  private val immutableEntities:   ImmutableArray[Entity]       = new ImmutableArray[Entity](entities)
  private val pendingOperations:   ArrayBuffer[EntityOperation] = ArrayBuffer.empty
  private val entityOperationPool: EntityOperationPool          = new EntityOperationPool()

  def addEntity(entity: Entity): Unit =
    addEntity(entity, delayed = false)

  def addEntity(entity: Entity, delayed: Boolean): Unit = {
    entity.scheduledForRemoval = false
    if (delayed) {
      val operation = entityOperationPool.obtain()
      operation.entity = Nullable(entity)
      operation.operationType = EntityOperation.Type.Add
      pendingOperations += operation
    } else {
      addEntityInternal(entity)
    }
  }

  def removeEntity(entity: Entity): Unit =
    removeEntity(entity, delayed = false)

  def removeEntity(entity: Entity, delayed: Boolean): Unit =
    if (delayed) {
      if (!entity.scheduledForRemoval) {
        entity.scheduledForRemoval = true
        val operation = entityOperationPool.obtain()
        operation.entity = Nullable(entity)
        operation.operationType = EntityOperation.Type.Remove
        pendingOperations += operation
      }
    } else {
      removeEntityInternal(entity)
    }

  def removeAllEntities(): Unit =
    removeAllEntities(immutableEntities)

  def removeAllEntities(delayed: Boolean): Unit =
    removeAllEntities(immutableEntities, delayed)

  def removeAllEntities(entitiesToRemove: ImmutableArray[Entity]): Unit =
    removeAllEntities(entitiesToRemove, delayed = false)

  def removeAllEntities(entitiesToRemove: ImmutableArray[Entity], delayed: Boolean): Unit =
    if (delayed) {
      entitiesToRemove.foreach { entity =>
        entity.scheduledForRemoval = true
      }
      val operation = entityOperationPool.obtain()
      operation.operationType = EntityOperation.Type.RemoveAll
      operation.entities = Nullable(entitiesToRemove)
      pendingOperations += operation
    } else {
      while (entitiesToRemove.size > 0)
        removeEntity(entitiesToRemove.first, delayed = false)
    }

  def getEntities: ImmutableArray[Entity] = immutableEntities

  def hasPendingOperations: Boolean = pendingOperations.nonEmpty

  def processPendingOperations(): Unit = {
    var i = 0
    while (i < pendingOperations.size) {
      val operation = pendingOperations(i)

      operation.operationType match {
        case EntityOperation.Type.Add =>
          operation.entity.foreach(addEntityInternal)
        case EntityOperation.Type.Remove =>
          operation.entity.foreach(removeEntityInternal)
        case EntityOperation.Type.RemoveAll =>
          operation.entities.foreach { ents =>
            while (ents.size > 0)
              removeEntityInternal(ents.first)
          }
      }

      entityOperationPool.free(operation)
      i += 1
    }

    pendingOperations.clear()
  }

  protected def removeEntityInternal(entity: Entity): Unit = {
    val removed = entitySet.remove(entity)

    if (removed) {
      entity.scheduledForRemoval = false
      entity.removing = true
      entities -= entity
      listener.entityRemoved(entity)
      entity.removing = false
    }
  }

  protected def addEntityInternal(entity: Entity): Unit = {
    if (entitySet.contains(entity)) {
      throw new IllegalArgumentException("Entity is already registered " + entity)
    }

    entities += entity
    entitySet += entity

    listener.entityAdded(entity)
  }
}

/** A single delayed entity operation (add, remove, or remove-all). */
final private[ecs] class EntityOperation extends Pool.Poolable {
  var operationType: EntityOperation.Type             = EntityOperation.Type.Add
  var entity:        Nullable[Entity]                 = Nullable.empty
  var entities:      Nullable[ImmutableArray[Entity]] = Nullable.empty

  override def reset(): Unit = {
    entity = Nullable.empty
    entities = Nullable.empty
  }
}

private[ecs] object EntityOperation {
  enum Type extends java.lang.Enum[Type] {
    case Add, Remove, RemoveAll
  }
}

/** Pool for reusing [[EntityOperation]] instances. */
final private[ecs] class EntityOperationPool extends Pool[EntityOperation] {
  override protected val max:             Int = Int.MaxValue
  override protected val initialCapacity: Int = 16

  override protected def newObject(): EntityOperation = new EntityOperation()
}
