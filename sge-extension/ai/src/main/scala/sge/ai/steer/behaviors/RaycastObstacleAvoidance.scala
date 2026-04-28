/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/RaycastObstacleAvoidance.java
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
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: RaycastObstacleAvoidance,calculateRealSteering,detector,distanceFromBoundary,i,inputRays,minDistanceSquare,minOutputCollision,outputCollision,ownerPosition,rayCfg,rayConfiguration,raycastCollisionDetector
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/RaycastObstacleAvoidance.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: RaycastObstacleAvoidance,calculateRealSteering,detector,distanceFromBoundary,i,inputRays,minDistanceSquare,minOutputCollision,outputCollision,ownerPosition,rayCfg,rayConfiguration,raycastCollisionDetector
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package behaviors

import sge.ai.steer.utils.RayConfiguration
import sge.ai.utils.{ Collision, RaycastCollisionDetector }
import sge.math.Vector
import sge.utils.Nullable

/** With the `RaycastObstacleAvoidance` the moving agent (the owner) casts one or more rays out in the direction of its motion. If these rays collide with an obstacle, then a target is created that
  * will avoid the collision, and the owner does a basic seek on this target. Typically, the rays extend a short distance ahead of the character (usually a distance corresponding to a few seconds of
  * movement).
  *
  * This behavior is especially suitable for large-scale obstacles like walls.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class RaycastObstacleAvoidance[T <: Vector[T]](
  owner: Steerable[T],
  /** The ray configuration */
  var rayConfiguration: Nullable[RayConfiguration[T]] = Nullable.empty[RayConfiguration[T]],
  /** The collision detector */
  var raycastCollisionDetector: Nullable[RaycastCollisionDetector[T]] = Nullable.empty[RaycastCollisionDetector[T]],
  /** The minimum distance to a wall, i.e. how far to avoid collision. */
  var distanceFromBoundary: Float = 0f
) extends SteeringBehavior[T](owner) {

  private var outputCollision:    Collision[T] = new Collision[T](newVector(owner), newVector(owner))
  private var minOutputCollision: Collision[T] = new Collision[T](newVector(owner), newVector(owner))

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    val ownerPosition     = owner.position
    var minDistanceSquare = Float.PositiveInfinity

    val rayCfg   = rayConfiguration.getOrElse(throw new IllegalStateException("rayConfiguration not set"))
    val detector = raycastCollisionDetector.getOrElse(throw new IllegalStateException("raycastCollisionDetector not set"))

    // Get the updated rays
    val inputRays = rayCfg.updateRays()

    // Process rays
    var i = 0
    while (i < inputRays.length) {
      // Find the collision with current ray
      val collided = detector.findCollision(outputCollision, inputRays(i))

      if (collided) {
        val distanceSquare = ownerPosition.distanceSq(outputCollision.point)
        if (distanceSquare < minDistanceSquare) {
          minDistanceSquare = distanceSquare
          // Swap collisions
          val tmpCollision = outputCollision
          outputCollision = minOutputCollision
          minOutputCollision = tmpCollision
        }
      }
      i += 1
    }

    // Return zero steering if no collision has occurred
    if (minDistanceSquare == Float.PositiveInfinity) {
      steering.setZero()
    } else {
      // Calculate and seek the target position
      steering.linear
        .set(minOutputCollision.point)
        .mulAdd(minOutputCollision.normal, owner.boundingRadius + distanceFromBoundary)
        .-(owner.position)
        .normalize()
        .scale(getActualLimiter().maxLinearAcceleration)

      // No angular acceleration
      steering.angular = 0

      // Output steering acceleration
      steering
    }
  }
}
