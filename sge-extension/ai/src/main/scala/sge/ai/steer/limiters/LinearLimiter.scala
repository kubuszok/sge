/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/LinearLimiter.java
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
 * Covenant-baseline-methods: LinearLimiter,_maxLinearAcceleration,_maxLinearSpeed,maxLinearAcceleration,maxLinearAcceleration_,maxLinearSpeed,maxLinearSpeed_
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/limiters/LinearLimiter.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: LinearLimiter,_maxLinearAcceleration,_maxLinearSpeed,maxLinearAcceleration,maxLinearAcceleration_,maxLinearSpeed,maxLinearSpeed_
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package limiters

/** A `LinearLimiter` provides the maximum magnitudes of linear speed and linear acceleration. Angular methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class LinearLimiter(
  private var _maxLinearAcceleration: Float,
  private var _maxLinearSpeed:        Float
) extends NullLimiter {

  /** Returns the maximum linear speed. */
  override def maxLinearSpeed: Float = _maxLinearSpeed

  /** Sets the maximum linear speed. */
  override def maxLinearSpeed_=(value: Float): Unit = _maxLinearSpeed = value

  /** Returns the maximum linear acceleration. */
  override def maxLinearAcceleration: Float = _maxLinearAcceleration

  /** Sets the maximum linear acceleration. */
  override def maxLinearAcceleration_=(value: Float): Unit = _maxLinearAcceleration = value
}
