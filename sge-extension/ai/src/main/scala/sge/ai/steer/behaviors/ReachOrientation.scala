/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/ReachOrientation.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package behaviors

import sge.ai.utils.{ ArithmeticUtils, Location }
import sge.math.Vector
import sge.utils.Nullable

/** `ReachOrientation` tries to align the owner to the target. It pays no attention to the position or velocity of the owner or target. This steering behavior does not produce any linear acceleration;
  * it only responds by turning.
  *
  * `ReachOrientation` behaves in a similar way to [[Arrive]] since it tries to reach the target orientation and tries to have zero rotation when it gets there.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class ReachOrientation[T <: Vector[T]](
  owner: Steerable[T],
  /** The target to align to. */
  var target: Nullable[Location[T]] = Nullable.empty[Location[T]]
) extends SteeringBehavior[T](owner) {

  /** The tolerance for aligning to the target without letting small errors keep the owner swinging. */
  var alignTolerance: Float = 0f

  /** The radius for beginning to slow down */
  var decelerationRadius: Float = 0f

  /** The time over which to achieve target rotation speed */
  var timeToTarget: Float = 0.1f

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] =
    reachOrientation(steering, target.getOrElse(throw new IllegalStateException("target not set")).orientation)

  /** Produces a steering that tries to align the owner to the target orientation. This method is called by subclasses that want to align to a certain orientation.
    * @param steering
    *   the steering to be calculated.
    * @param targetOrientation
    *   the target orientation you want to align to.
    * @return
    *   the calculated steering for chaining.
    */
  protected def reachOrientation(steering: SteeringAcceleration[T], targetOrientation: Float): SteeringAcceleration[T] = {
    // Get the rotation direction to the target wrapped to the range [-PI, PI]
    val rotation = ArithmeticUtils.wrapAngleAroundZero(targetOrientation - owner.orientation)

    // Absolute rotation
    val rotationSize = if (rotation < 0f) -rotation else rotation

    // Check if we are there, return no steering
    if (rotationSize <= alignTolerance) {
      steering.setZero()
    } else {
      val actualLimiter = getActualLimiter()

      // Use maximum rotation
      var targetRotation = actualLimiter.maxAngularSpeed

      // If we are inside the slow down radius, then calculate a scaled rotation
      if (rotationSize <= decelerationRadius) targetRotation *= rotationSize / decelerationRadius

      // The final target rotation combines
      // speed (already in the variable) and direction
      targetRotation *= rotation / rotationSize

      // Acceleration tries to get to the target rotation
      steering.angular = (targetRotation - owner.angularVelocity) / timeToTarget

      // Check if the absolute acceleration is too great
      val angularAcceleration = if (steering.angular < 0f) -steering.angular else steering.angular
      if (angularAcceleration > actualLimiter.maxAngularAcceleration) {
        steering.angular *= actualLimiter.maxAngularAcceleration / angularAcceleration
      }

      // No linear acceleration
      steering.linear.setZero()

      // Output the steering
      steering
    }
  }
}
