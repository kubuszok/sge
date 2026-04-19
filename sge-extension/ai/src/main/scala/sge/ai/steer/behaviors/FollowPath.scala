/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/FollowPath.java
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
 * Covenant-baseline-loc: 100
 * Covenant-baseline-methods: FollowPath,arriveEnabled,calculateRealSteering,distance,getInternalTargetPosition,internalTargetPosition,location,path,pathOffset,pathParam,predictionTime,targetDistance
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.ai.steer.utils.Path
import sge.ai.steer.utils.Path.PathParam
import sge.math.Vector

import scala.util.boundary
import boundary.break

/** `FollowPath` behavior produces a linear acceleration that moves the agent along the given path. First it calculates the agent location based on the specified prediction time. Then it works out the
  * position of the internal target based on the location just calculated and the shape of the path. It finally uses seek/arrive behavior to move the owner towards the internal target position.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  * @tparam P
  *   Type of path parameter implementing the [[PathParam]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class FollowPath[T <: Vector[T], P <: PathParam](
  owner: Steerable[T],
  /** The path to follow */
  var path: Path[T, P],
  /** The distance along the path to generate the target. Can be negative if the owner has to move along the reverse direction. */
  var pathOffset: Float = 0f,
  /** The time in the future to predict the owner's position. Set it to 0 for non-predictive path following. */
  var predictionTime: Float = 0f
) extends Arrive[T](owner) {

  /** The current position on the path */
  val pathParam: P = path.createParam()

  /** The flag indicating whether to use [[Arrive]] behavior to approach the end of an open path. It defaults to `true`. */
  var arriveEnabled: Boolean = true

  private val internalTargetPosition: T = newVector(owner)

  /** Returns the current position of the internal target. This method is useful for debug purpose. */
  def getInternalTargetPosition: T = internalTargetPosition

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = boundary {
    // Predictive or non-predictive behavior?
    val location: T =
      if (predictionTime == 0) {
        // Use the current position of the owner
        owner.position
      } else {
        // Calculate the predicted future position of the owner. We're reusing steering.linear here.
        steering.linear.set(owner.position).mulAdd(owner.linearVelocity, predictionTime)
      }

    // Find the distance from the start of the path
    val distance = path.calculateDistance(location, pathParam)

    // Offset it
    val targetDistance = distance + pathOffset

    // Calculate the target position
    path.calculateTargetPosition(internalTargetPosition, pathParam, targetDistance)

    if (arriveEnabled && path.isOpen) {
      if (pathOffset >= 0) {
        // Use Arrive to approach the last point of the path
        if (targetDistance > path.length - decelerationRadius) {
          break(arrive(steering, internalTargetPosition))
        }
      } else {
        // Use Arrive to approach the first point of the path
        if (targetDistance < decelerationRadius) {
          break(arrive(steering, internalTargetPosition))
        }
      }
    }

    // Seek the target position
    steering.linear.set(internalTargetPosition).-(owner.position).normalize().scale(getActualLimiter().maxLinearAcceleration)

    // No angular acceleration
    steering.angular = 0

    // Output steering acceleration
    steering
  }
}
