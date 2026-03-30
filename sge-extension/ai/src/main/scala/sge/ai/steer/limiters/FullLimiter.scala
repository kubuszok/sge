/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/FullLimiter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, public vars instead of getX/setX
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package limiters

/** A `FullLimiter` provides the maximum magnitudes of speed and acceleration for both linear and angular components.
  *
  * @author
  *   davebaol (original implementation)
  */
class FullLimiter(
  private var _maxLinearAcceleration:  Float,
  private var _maxLinearSpeed:         Float,
  private var _maxAngularAcceleration: Float,
  private var _maxAngularSpeed:        Float
) extends Limiter {

  private var _zeroLinearSpeedThreshold: Float = 0f

  override def maxLinearSpeed:                 Float = _maxLinearSpeed
  override def maxLinearSpeed_=(value: Float): Unit  = _maxLinearSpeed = value

  override def maxLinearAcceleration:                 Float = _maxLinearAcceleration
  override def maxLinearAcceleration_=(value: Float): Unit  = _maxLinearAcceleration = value

  override def maxAngularSpeed:                 Float = _maxAngularSpeed
  override def maxAngularSpeed_=(value: Float): Unit  = _maxAngularSpeed = value

  override def maxAngularAcceleration:                 Float = _maxAngularAcceleration
  override def maxAngularAcceleration_=(value: Float): Unit  = _maxAngularAcceleration = value

  override def zeroLinearSpeedThreshold:                 Float = _zeroLinearSpeedThreshold
  override def zeroLinearSpeedThreshold_=(value: Float): Unit  = _zeroLinearSpeedThreshold = value
}
