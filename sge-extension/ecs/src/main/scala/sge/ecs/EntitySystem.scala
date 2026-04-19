/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/EntitySystem.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: Nullable[Engine] for engine reference (set/cleared by Engine)
 *   Idiom: processing as public var (replaces get/set pair)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: EntitySystem,_engine,addedToEngine,addedToEngineInternal,checkProcessing,engine,processing,removedFromEngine,removedFromEngineInternal,update
 * Covenant-source-reference: com/badlogic/ashley/core/EntitySystem.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ecs

import sge.utils.Nullable

/** Abstract class for processing sets of [[Entity]] objects.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
abstract class EntitySystem(val priority: Int = 0) {

  /** Whether or not the system should be processed. */
  var processing: Boolean = true

  private var _engine: Nullable[Engine] = Nullable.empty

  /** Called when this EntitySystem is added to an [[Engine]].
    * @param engine
    *   The [[Engine]] this system was added to.
    */
  def addedToEngine(engine: Engine): Unit = {}

  /** Called when this EntitySystem is removed from an [[Engine]].
    * @param engine
    *   The [[Engine]] the system was removed from.
    */
  def removedFromEngine(engine: Engine): Unit = {}

  /** The update method called every tick.
    * @param deltaTime
    *   The time passed since last frame in seconds.
    */
  def update(deltaTime: Float): Unit = {}

  /** @return Whether or not the system should be processed. */
  def checkProcessing: Boolean = processing

  /** @return engine instance the system is registered to, or Nullable.empty if not associated. */
  def engine: Nullable[Engine] = _engine

  final private[ecs] def addedToEngineInternal(engine: Engine): Unit = {
    _engine = Nullable(engine)
    addedToEngine(engine)
  }

  final private[ecs] def removedFromEngineInternal(engine: Engine): Unit = {
    _engine = Nullable.empty
    removedFromEngine(engine)
  }
}
