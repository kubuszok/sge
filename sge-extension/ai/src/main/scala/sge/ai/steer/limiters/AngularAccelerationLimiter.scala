/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/AngularAccelerationLimiter.java
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
 * Covenant-baseline-methods: AngularAccelerationLimiter,_maxAngularAcceleration,maxAngularAcceleration,maxAngularAcceleration_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package limiters

/** An `AngularAccelerationLimiter` provides the maximum magnitude of angular acceleration. All other methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class AngularAccelerationLimiter(
  private var _maxAngularAcceleration: Float
) extends NullLimiter {

  /** Returns the maximum angular acceleration. */
  override def maxAngularAcceleration: Float = _maxAngularAcceleration

  /** Sets the maximum angular acceleration. */
  override def maxAngularAcceleration_=(value: Float): Unit = _maxAngularAcceleration = value
}
