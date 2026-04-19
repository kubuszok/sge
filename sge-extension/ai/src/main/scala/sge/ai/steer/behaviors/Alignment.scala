/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Alignment.java
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
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: Alignment,averageVelocity,calculateRealSteering,neighborCount,reportNeighbor
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector

import scala.compiletime.uninitialized

/** `Alignment` is a group behavior producing a linear acceleration that attempts to keep the owner aligned with the agents in its immediate area defined by the given [[Proximity]]. The acceleration
  * is calculated by first iterating through all the neighbors and averaging their linear velocity vectors.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Alignment[T <: Vector[T]](
  owner:     Steerable[T],
  proximity: Proximity[T]
) extends GroupBehavior[T](owner, proximity)
    with Proximity.ProximityCallback[T] {

  private var averageVelocity: T = uninitialized

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    steering.setZero()

    averageVelocity = steering.linear

    val neighborCount = proximity.findNeighbors(this)

    if (neighborCount > 0) {
      // Average the accumulated velocities
      averageVelocity.scale(1f / neighborCount)

      // Match the average velocity.
      // Notice that steering.linear and averageVelocity are the same vector here.
      averageVelocity.-(owner.linearVelocity).limit(getActualLimiter().maxLinearAcceleration)
    }

    steering
  }

  override def reportNeighbor(neighbor: Steerable[T]): Boolean = {
    // Accumulate neighbor velocity
    averageVelocity.+(neighbor.linearVelocity)
    true
  }
}
