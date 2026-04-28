/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/systems/IntervalIteratingSystem.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.systems` -> `sge.ecs.systems`
 *   Convention: split packages
 *   Idiom: Nullable for entities field (set on addedToEngine)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 64
 * Covenant-baseline-methods: IntervalIteratingSystem,addedToEngine,endProcessing,entities,family,getEntities,processEntity,startProcessing,updateInterval
 * Covenant-source-reference: com/badlogic/ashley/systems/IntervalIteratingSystem.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs
package systems

import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable

/** A simple [[EntitySystem]] that processes a [[Family]] of entities not once per frame, but after a given interval. Entity processing logic should be placed in [[processEntity]].
  *
  * @author
  *   David Saltares (original implementation)
  */
abstract class IntervalIteratingSystem(
  val family: Family,
  interval:   Float,
  priority:   Int = 0
) extends IntervalSystem(interval, priority) {

  private var entities: Nullable[ImmutableArray[Entity]] = Nullable.empty

  override def addedToEngine(engine: Engine): Unit =
    entities = Nullable(engine.getEntitiesFor(family))

  override protected def updateInterval(): Unit =
    entities.foreach { ents =>
      startProcessing()
      var i = 0
      while (i < ents.size) {
        processEntity(ents(i))
        i += 1
      }
      endProcessing()
    }

  /** @return set of entities processed by the system */
  def getEntities: Nullable[ImmutableArray[Entity]] = entities

  /** The user should place the entity processing logic here.
    * @param entity
    *   The current entity being processed
    */
  protected def processEntity(entity: Entity): Unit

  /** This method is called once on every update call of the EntitySystem, before entity processing begins. Override this method to implement your specific startup conditions.
    */
  def startProcessing(): Unit = {}

  /** This method is called once on every update call of the EntitySystem after entity processing is complete. Override this method to implement your specific end conditions.
    */
  def endProcessing(): Unit = {}
}
