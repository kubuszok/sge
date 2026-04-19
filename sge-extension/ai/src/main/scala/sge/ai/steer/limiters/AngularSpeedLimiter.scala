/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/AngularSpeedLimiter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: AngularSpeedLimiter,_maxAngularSpeed,maxAngularSpeed,maxAngularSpeed_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package limiters

/** An `AngularSpeedLimiter` provides the maximum magnitudes of angular speed. All other methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class AngularSpeedLimiter(
  private var _maxAngularSpeed: Float
) extends NullLimiter {

  /** Returns the maximum angular speed. */
  override def maxAngularSpeed: Float = _maxAngularSpeed

  /** Sets the maximum angular speed. */
  override def maxAngularSpeed_=(value: Float): Unit = _maxAngularSpeed = value
}
