/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/LinearAccelerationLimiter.java
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
 * Covenant-baseline-methods: LinearAccelerationLimiter,_maxLinearAcceleration,maxLinearAcceleration,maxLinearAcceleration_
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/limiters/LinearAccelerationLimiter.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: LinearAccelerationLimiter,_maxLinearAcceleration,maxLinearAcceleration,maxLinearAcceleration_
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package limiters

/** A `LinearAccelerationLimiter` provides the maximum magnitude of linear acceleration. All other methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class LinearAccelerationLimiter(
  private var _maxLinearAcceleration: Float
) extends NullLimiter {

  /** Returns the maximum linear acceleration. */
  override def maxLinearAcceleration: Float = _maxLinearAcceleration

  /** Sets the maximum linear acceleration. */
  override def maxLinearAcceleration_=(value: Float): Unit = _maxLinearAcceleration = value
}
