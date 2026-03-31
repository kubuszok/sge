/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/systems/IteratingSystem.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.systems` -> `sge.ecs.systems`
 *   Convention: split packages
 *   Idiom: Nullable for entities field (cleared on removedFromEngine)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs
package systems

import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable

/** A simple [[EntitySystem]] that iterates over each entity and calls [[processEntity]] for each entity every time the EntitySystem is updated. This is really just a convenience class as most systems
  * iterate over a list of entities.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
abstract class IteratingSystem(val family: Family, priority: Int = 0) extends EntitySystem(priority) {

  private var entities: Nullable[ImmutableArray[Entity]] = Nullable.empty

  override def addedToEngine(engine: Engine): Unit =
    entities = Nullable(engine.getEntitiesFor(family))

  override def removedFromEngine(engine: Engine): Unit =
    entities = Nullable.empty

  override def update(deltaTime: Float): Unit =
    entities.foreach { ents =>
      startProcessing()
      var i = 0
      while (i < ents.size) {
        processEntity(ents(i), deltaTime)
        i += 1
      }
      endProcessing()
    }

  /** @return set of entities processed by the system */
  def getEntities: Nullable[ImmutableArray[Entity]] = entities

  /** This method is called on every entity on every update call of the EntitySystem. Override this to implement your system's specific processing.
    * @param entity
    *   The current Entity being processed
    * @param deltaTime
    *   The delta time between the last and current frame
    */
  protected def processEntity(entity: Entity, deltaTime: Float): Unit

  /** This method is called once on every update call of the EntitySystem, before entity processing begins. Override this method to implement your specific startup conditions.
    */
  def startProcessing(): Unit = {}

  /** This method is called once on every update call of the EntitySystem after entity processing is complete. Override this method to implement your specific end conditions.
    */
  def endProcessing(): Unit = {}
}
