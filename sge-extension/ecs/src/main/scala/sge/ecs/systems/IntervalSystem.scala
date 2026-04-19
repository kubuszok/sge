/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/systems/IntervalSystem.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.systems` -> `sge.ecs.systems`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: IntervalSystem,accumulator,update,updateInterval
 * Covenant-source-reference: com/badlogic/ashley/systems/IntervalSystem.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ecs
package systems

/** A simple [[EntitySystem]] that does not run its update logic every call to [[update]], but after a given interval. The actual logic should be placed in [[updateInterval]].
  *
  * @author
  *   David Saltares (original implementation)
  */
abstract class IntervalSystem(val interval: Float, priority: Int = 0) extends EntitySystem(priority) {

  private var accumulator: Float = 0f

  override def update(deltaTime: Float): Unit = {
    accumulator += deltaTime

    while (accumulator >= interval) {
      accumulator -= interval
      updateInterval()
    }
  }

  /** The processing logic of the system should be placed here. */
  protected def updateInterval(): Unit
}
