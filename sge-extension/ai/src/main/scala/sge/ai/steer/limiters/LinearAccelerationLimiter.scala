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
