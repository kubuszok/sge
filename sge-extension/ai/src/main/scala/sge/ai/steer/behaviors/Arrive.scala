/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Arrive.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 82
 * Covenant-baseline-methods: Arrive,arrivalTolerance,arrive,calculateRealSteering,decelerationRadius,distance,target,timeToTarget,toTarget
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Arrive.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 82
 * Covenant-baseline-methods: Arrive,arrivalTolerance,arrive,calculateRealSteering,decelerationRadius,distance,target,timeToTarget,toTarget
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package behaviors

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.Nullable

/** `Arrive` behavior moves the agent towards a target position. It is similar to seek but it attempts to arrive at the target position with a zero velocity.
  *
  * `Arrive` behavior uses two radii. The `arrivalTolerance` lets the owner get near enough to the target without letting small errors keep it in motion. The `decelerationRadius`, usually much larger
  * than the previous one, specifies when the incoming character will begin to slow down.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Arrive[T <: Vector[T]](
  owner: Steerable[T],
  /** The target to arrive to. */
  var target: Nullable[Location[T]] = Nullable.empty[Location[T]]
) extends SteeringBehavior[T](owner) {

  /** The tolerance for arriving at the target. It lets the owner get near enough to the target without letting small errors keep it in motion.
    */
  var arrivalTolerance: Float = 0f

  /** The radius for beginning to slow down */
  var decelerationRadius: Float = 0f

  /** The time over which to achieve target speed */
  var timeToTarget: Float = 0.1f

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] =
    arrive(steering, target.getOrElse(throw new IllegalStateException("target not set")).position)

  protected def arrive(steering: SteeringAcceleration[T], targetPosition: T): SteeringAcceleration[T] = {
    // Get the direction and distance to the target
    val toTarget = steering.linear.set(targetPosition).-(owner.position)
    val distance = toTarget.length

    // Check if we are there, return no steering
    if (distance <= arrivalTolerance) {
      steering.setZero()
    } else {
      val actualLimiter = getActualLimiter()
      // Go max speed
      var targetSpeed = actualLimiter.maxLinearSpeed

      // If we are inside the slow down radius calculate a scaled speed
      if (distance <= decelerationRadius) targetSpeed *= distance / decelerationRadius

      // Target velocity combines speed and direction
      val targetVelocity = toTarget.scale(targetSpeed / distance) // Optimized code for: toTarget.normalize().scale(targetSpeed)

      // Acceleration tries to get to the target velocity without exceeding max acceleration
      // Notice that steering.linear and targetVelocity are the same vector
      targetVelocity.-(owner.linearVelocity).scale(1f / timeToTarget).limit(actualLimiter.maxLinearAcceleration)

      // No angular acceleration
      steering.angular = 0f

      // Output the steering
      steering
    }
  }
}
