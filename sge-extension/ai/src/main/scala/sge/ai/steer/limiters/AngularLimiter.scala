/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/AngularLimiter.java
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
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: AngularLimiter,_maxAngularAcceleration,_maxAngularSpeed,maxAngularAcceleration,maxAngularAcceleration_,maxAngularSpeed,maxAngularSpeed_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package limiters

/** An `AngularLimiter` provides the maximum magnitudes of angular speed and angular acceleration. Linear methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class AngularLimiter(
  private var _maxAngularAcceleration: Float,
  private var _maxAngularSpeed:        Float
) extends NullLimiter {

  /** Returns the maximum angular speed. */
  override def maxAngularSpeed: Float = _maxAngularSpeed

  /** Sets the maximum angular speed. */
  override def maxAngularSpeed_=(value: Float): Unit = _maxAngularSpeed = value

  /** Returns the maximum angular acceleration. */
  override def maxAngularAcceleration: Float = _maxAngularAcceleration

  /** Sets the maximum angular acceleration. */
  override def maxAngularAcceleration_=(value: Float): Unit = _maxAngularAcceleration = value
}
