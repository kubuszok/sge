/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/CollisionAvoidance.java
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
 * Covenant-baseline-loc: 120
 * Covenant-baseline-methods: CollisionAvoidance,calculateRealSteering,firstDistance,firstMinSeparation,firstNeighbor,firstRelativePosition,firstRelativeVelocity,neighborCount,relativePosition,relativeSpeed2,relativeVelocity,reportNeighbor,shortestTime
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector
import sge.utils.Nullable

import scala.compiletime.uninitialized

/** `CollisionAvoidance` behavior steers the owner to avoid obstacles lying in its path. An obstacle is any object that can be approximated by a circle (or sphere, if you are working in 3D).
  *
  * This implementation uses collision prediction working out the closest approach of two agents and determining if their distance at this point is less than the sum of their bounding radius.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class CollisionAvoidance[T <: Vector[T]](
  owner:     Steerable[T],
  proximity: Proximity[T]
) extends GroupBehavior[T](owner, proximity)
    with Proximity.ProximityCallback[T] {

  private var shortestTime:          Float                  = 0f
  private var firstNeighbor:         Nullable[Steerable[T]] = Nullable.empty
  private var firstMinSeparation:    Float                  = 0f
  private var firstDistance:         Float                  = 0f
  private val firstRelativePosition: T                      = newVector(owner)
  private val firstRelativeVelocity: T                      = newVector(owner)
  private var relativePosition:      T                      = uninitialized
  private val relativeVelocity:      T                      = newVector(owner)

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    shortestTime = Float.PositiveInfinity
    firstNeighbor = Nullable.empty
    firstMinSeparation = 0
    firstDistance = 0
    relativePosition = steering.linear

    // Take into consideration each neighbor to find the most imminent collision.
    val neighborCount = proximity.findNeighbors(this)

    // If we have no target, then return no steering acceleration
    if (neighborCount == 0 || firstNeighbor.isEmpty) {
      steering.setZero()
    } else {
      val fn = firstNeighbor.getOrElse(throw new IllegalStateException())

      // If we're going to hit exactly, or if we're already
      // colliding, then do the steering based on current position.
      if (firstMinSeparation <= 0 || firstDistance < owner.boundingRadius + fn.boundingRadius) {
        relativePosition.set(fn.position).-(owner.position)
      } else {
        // Otherwise calculate the future relative position
        relativePosition.set(firstRelativePosition).mulAdd(firstRelativeVelocity, shortestTime)
      }

      // Avoid the target
      // Notice that steering.linear and relativePosition are the same vector
      relativePosition.normalize().scale(-getActualLimiter().maxLinearAcceleration)

      // No angular acceleration
      steering.angular = 0f

      // Output the steering
      steering
    }
  }

  override def reportNeighbor(neighbor: Steerable[T]): Boolean = {
    // Calculate the time to collision
    relativePosition.set(neighbor.position).-(owner.position)
    relativeVelocity.set(neighbor.linearVelocity).-(owner.linearVelocity)
    val relativeSpeed2 = relativeVelocity.lengthSq

    // Collision can't happen when the agents have the same linear velocity.
    if (relativeSpeed2 == 0) {
      false
    } else {
      val timeToCollision = -relativePosition.dot(relativeVelocity) / relativeSpeed2

      // If timeToCollision is negative, i.e. the owner is already moving away from the neighbor,
      // or it's not the most imminent collision then no action needs to be taken.
      if (timeToCollision <= 0 || timeToCollision >= shortestTime) {
        false
      } else {
        // Check if it is going to be a collision at all
        val distance      = relativePosition.length
        val minSeparation = distance - Math.sqrt(relativeSpeed2).toFloat * timeToCollision
        if (minSeparation > owner.boundingRadius + neighbor.boundingRadius) {
          false
        } else {
          // Store most imminent collision data
          shortestTime = timeToCollision
          firstNeighbor = Nullable(neighbor)
          firstMinSeparation = minSeparation
          firstDistance = distance
          firstRelativePosition.set(relativePosition)
          firstRelativeVelocity.set(relativeVelocity)
          true
        }
      }
    }
  }
}
