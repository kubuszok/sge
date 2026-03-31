/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/systems/SortedIteratingSystem.java
 * Original authors: Santo Pfingsten
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.systems` -> `sge.ecs.systems`
 *   Convention: split packages
 *   Idiom: ArrayBuffer instead of Array for sorted entities
 *   Idiom: Ordering[Entity] instead of Comparator[Entity]
 *   Idiom: ImmutableArray wrapping sorted buffer
 *   Idiom: implements EntityListener for add/remove detection
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs
package systems

import scala.collection.mutable.ArrayBuffer

import sge.ecs.utils.ImmutableArray

/** A simple [[EntitySystem]] that processes each entity of a given family in the order specified by a comparator and calls [[processEntity]] for each entity every time the EntitySystem is updated.
  * This is really just a convenience class as rendering systems tend to iterate over a list of entities in a sorted manner. Adding entities will cause the entity list to be resorted. Call
  * [[forceSort]] if you changed your sorting criteria.
  *
  * @author
  *   Santo Pfingsten (original implementation)
  */
abstract class SortedIteratingSystem(
  val family: Family,
  comparator: Ordering[Entity],
  priority:   Int = 0
) extends EntitySystem(priority)
    with EntityListener {

  private val sortedEntities: ArrayBuffer[Entity]    = ArrayBuffer.empty
  private val entities:       ImmutableArray[Entity] = new ImmutableArray[Entity](sortedEntities)
  private var shouldSort:     Boolean                = false

  /** Call this if the sorting criteria have changed. The actual sorting will be delayed until the entities are processed.
    */
  def forceSort(): Unit =
    shouldSort = true

  private def sort(): Unit =
    if (shouldSort) {
      val sorted = sortedEntities.sorted(using comparator)
      sortedEntities.clear()
      sortedEntities ++= sorted
      shouldSort = false
    }

  override def addedToEngine(engine: Engine): Unit = {
    val newEntities = engine.getEntitiesFor(family)
    sortedEntities.clear()
    if (newEntities.size > 0) {
      var i = 0
      while (i < newEntities.size) {
        sortedEntities += newEntities(i)
        i += 1
      }
      val sorted = sortedEntities.sorted(using comparator)
      sortedEntities.clear()
      sortedEntities ++= sorted
    }
    shouldSort = false
    engine.addEntityListener(family, this)
  }

  override def removedFromEngine(engine: Engine): Unit = {
    engine.removeEntityListener(this)
    sortedEntities.clear()
    shouldSort = false
  }

  override def entityAdded(entity: Entity): Unit = {
    sortedEntities += entity
    shouldSort = true
  }

  override def entityRemoved(entity: Entity): Unit = {
    sortedEntities -= entity
    shouldSort = true
  }

  override def update(deltaTime: Float): Unit = {
    sort()
    startProcessing()
    var i = 0
    while (i < sortedEntities.size) {
      processEntity(sortedEntities(i), deltaTime)
      i += 1
    }
    endProcessing()
  }

  /** @return set of entities processed by the system */
  def getEntities: ImmutableArray[Entity] = {
    sort()
    entities
  }

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
