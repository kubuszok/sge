/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/NullLimiter.java
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

/** A `NullLimiter` always throws `UnsupportedOperationException`. Typically it's used as the base class of partial or immutable limiters.
  *
  * @author
  *   davebaol (original implementation)
  */
class NullLimiter extends Limiter {

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def zeroLinearSpeedThreshold: Float = 0.001f

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def zeroLinearSpeedThreshold_=(value: Float): Unit =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxLinearSpeed: Float =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxLinearSpeed_=(value: Float): Unit =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxLinearAcceleration: Float =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxLinearAcceleration_=(value: Float): Unit =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxAngularSpeed: Float =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxAngularSpeed_=(value: Float): Unit =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxAngularAcceleration: Float =
    throw new UnsupportedOperationException()

  /** Guaranteed to throw UnsupportedOperationException.
    * @throws UnsupportedOperationException
    *   always
    */
  override def maxAngularAcceleration_=(value: Float): Unit =
    throw new UnsupportedOperationException()
}

object NullLimiter {

  /** An immutable limiter whose getters return `Float.PositiveInfinity` and setters throw `UnsupportedOperationException`. */
  val NEUTRAL_LIMITER: NullLimiter = new NullLimiter() {
    override def maxLinearSpeed:         Float = Float.PositiveInfinity
    override def maxLinearAcceleration:  Float = Float.PositiveInfinity
    override def maxAngularSpeed:        Float = Float.PositiveInfinity
    override def maxAngularAcceleration: Float = Float.PositiveInfinity
  }
}
