/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Cohesion.java
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
 * Covenant-baseline-loc: 61
 * Covenant-baseline-methods: Cohesion,calculateRealSteering,centerOfMass,neighborCount,reportNeighbor
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Cohesion.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 61
 * Covenant-baseline-methods: Cohesion,calculateRealSteering,centerOfMass,neighborCount,reportNeighbor
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector

import scala.compiletime.uninitialized

/** `Cohesion` is a group behavior producing a linear acceleration that attempts to move the agent towards the center of mass of the agents in its immediate area defined by the given [[Proximity]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Cohesion[T <: Vector[T]](
  owner:     Steerable[T],
  proximity: Proximity[T]
) extends GroupBehavior[T](owner, proximity)
    with Proximity.ProximityCallback[T] {

  private var centerOfMass: T = uninitialized

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    steering.setZero()

    centerOfMass = steering.linear

    val neighborCount = proximity.findNeighbors(this)

    if (neighborCount > 0) {
      // The center of mass is the average of the sum of positions
      centerOfMass.scale(1f / neighborCount)

      // Now seek towards that position.
      centerOfMass.-(owner.position).normalize().scale(getActualLimiter().maxLinearAcceleration)
    }

    steering
  }

  override def reportNeighbor(neighbor: Steerable[T]): Boolean = {
    // Accumulate neighbor position
    centerOfMass.+(neighbor.position)
    true
  }
}
