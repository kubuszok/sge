/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/Limiter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, def+setter pairs instead of getX/setX
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: Limiter,maxAngularAcceleration,maxAngularAcceleration_,maxAngularSpeed,maxAngularSpeed_,maxLinearAcceleration,maxLinearAcceleration_,maxLinearSpeed,maxLinearSpeed_,zeroLinearSpeedThreshold,zeroLinearSpeedThreshold_
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/Limiter.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, def+setter pairs instead of getX/setX
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: Limiter,maxAngularAcceleration,maxAngularAcceleration_,maxAngularSpeed,maxAngularSpeed_,maxLinearAcceleration,maxLinearAcceleration_,maxLinearSpeed,maxLinearSpeed_,zeroLinearSpeedThreshold,zeroLinearSpeedThreshold_
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer

/** A `Limiter` provides the maximum magnitudes of speed and acceleration for both linear and angular components.
  *
  * @author
  *   davebaol (original implementation)
  */
trait Limiter {

  /** Returns the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero. Usually it is used to avoid updating the orientation when the velocity
    * vector has a negligible length.
    */
  def zeroLinearSpeedThreshold: Float

  /** Sets the threshold below which the linear speed can be considered zero. It must be a small positive value near to zero. Usually it is used to avoid updating the orientation when the velocity
    * vector has a negligible length.
    */
  def zeroLinearSpeedThreshold_=(value: Float): Unit

  /** Returns the maximum linear speed. */
  def maxLinearSpeed: Float

  /** Sets the maximum linear speed. */
  def maxLinearSpeed_=(value: Float): Unit

  /** Returns the maximum linear acceleration. */
  def maxLinearAcceleration: Float

  /** Sets the maximum linear acceleration. */
  def maxLinearAcceleration_=(value: Float): Unit

  /** Returns the maximum angular speed. */
  def maxAngularSpeed: Float

  /** Sets the maximum angular speed. */
  def maxAngularSpeed_=(value: Float): Unit

  /** Returns the maximum angular acceleration. */
  def maxAngularAcceleration: Float

  /** Sets the maximum angular acceleration. */
  def maxAngularAcceleration_=(value: Float): Unit
}
