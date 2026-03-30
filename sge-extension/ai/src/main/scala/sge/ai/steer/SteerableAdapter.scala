/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/SteerableAdapter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null returns, def+setter pairs
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer

import sge.ai.utils.Location
import sge.math.Vector

/** An adapter class for [[Steerable]]. You can derive from this and only override what you are interested in. For example, this comes in handy when you have to create on the fly a target for a
  * particular behavior.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class SteerableAdapter[T <: Vector[T]] extends Steerable[T] {

  override def zeroLinearSpeedThreshold:                 Float = 0.001f
  override def zeroLinearSpeedThreshold_=(value: Float): Unit  = {}

  override def maxLinearSpeed:                 Float = 0f
  override def maxLinearSpeed_=(value: Float): Unit  = {}

  override def maxLinearAcceleration:                 Float = 0f
  override def maxLinearAcceleration_=(value: Float): Unit  = {}

  override def maxAngularSpeed:                 Float = 0f
  override def maxAngularSpeed_=(value: Float): Unit  = {}

  override def maxAngularAcceleration:                 Float = 0f
  override def maxAngularAcceleration_=(value: Float): Unit  = {}

  override def position:                          T     = throw new UnsupportedOperationException()
  override def orientation:                       Float = 0f
  override def orientation_=(orientation: Float): Unit  = {}

  override def linearVelocity:  T     = throw new UnsupportedOperationException()
  override def angularVelocity: Float = 0f
  override def boundingRadius:  Float = 0f

  override def tagged:                    Boolean = false
  override def tagged_=(tagged: Boolean): Unit    = {}

  override def newLocation():                             Location[T] = throw new UnsupportedOperationException()
  override def vectorToAngle(vector:    T):               Float       = 0f
  override def angleToVector(outVector: T, angle: Float): T           = throw new UnsupportedOperationException()
}
