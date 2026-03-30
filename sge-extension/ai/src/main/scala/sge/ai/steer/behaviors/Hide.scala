/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Hide.java
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

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.Nullable

import scala.compiletime.uninitialized

/** This behavior attempts to position a owner so that an obstacle is always between itself and the agent (the hunter) it's trying to hide from. First the distance to each of these obstacles is
  * determined. Then the owner uses the arrive behavior to steer toward the closest one. If no appropriate obstacles can be found, no steering is returned.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Hide[T <: Vector[T]](
  owner:  Steerable[T],
  target: Nullable[Location[T]] = Nullable.empty[Location[T]],
  /** The proximity to find nearby obstacles. */
  var proximity: Nullable[Proximity[T]] = Nullable.empty[Proximity[T]]
) extends Arrive[T](owner, target)
    with Proximity.ProximityCallback[T] {

  /** The distance from the boundary of the obstacle behind which to hide. */
  var distanceFromBoundary: Float = 0f

  private val bestHidingSpot: T = newVector(owner)
  // toObstacle is set to steering.linear during calculateRealSteering to reuse the vector
  private var toObstacle:         T     = uninitialized
  private var distance2ToClosest: Float = Float.PositiveInfinity

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // Initialize member variables used by the callback
    this.distance2ToClosest = Float.PositiveInfinity
    this.toObstacle = steering.linear

    val prox = proximity.getOrElse(throw new IllegalStateException("proximity not set"))

    // Find neighbors (the obstacles) using this behavior as callback
    val neighborsCount = prox.findNeighbors(this)

    // If no suitable obstacles found return no steering otherwise use Arrive on the hiding spot
    if (neighborsCount == 0) steering.setZero() else arrive(steering, bestHidingSpot)
  }

  override def reportNeighbor(neighbor: Steerable[T]): Boolean = {
    val tgt = target.getOrElse(throw new IllegalStateException("target not set"))
    // Calculate the position of the hiding spot for this obstacle
    val hidingSpot = getHidingPosition(neighbor.position, neighbor.boundingRadius, tgt.position)

    // Work in distance-squared space to find the closest hiding
    // spot to the owner
    val distance2 = hidingSpot.distanceSq(owner.position)
    if (distance2 < distance2ToClosest) {
      distance2ToClosest = distance2
      bestHidingSpot.set(hidingSpot)
      true
    } else {
      false
    }
  }

  /** Given the position of a target and the position and radius of an obstacle, this method calculates a position `distanceFromBoundary` away from the object's bounding radius and directly opposite
    * the target.
    */
  protected def getHidingPosition(obstaclePosition: T, obstacleRadius: Float, targetPosition: T): T = {
    // Calculate how far away the agent is to be from the chosen
    // obstacle's bounding radius
    val distanceAway = obstacleRadius + distanceFromBoundary

    // Calculate the normalized vector toward the obstacle from the target
    toObstacle.set(obstaclePosition).-(targetPosition).normalize()

    // Scale it to size and add to the obstacle's position to get
    // the hiding spot.
    toObstacle.scale(distanceAway).+(obstaclePosition)
  }
}
