/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Separation.java
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
 * Covenant-baseline-loc: 75
 * Covenant-baseline-methods: Separation,calculateRealSteering,decayCoefficient,distanceSqr,linear,reportNeighbor,toAgent
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Separation.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 75
 * Covenant-baseline-methods: Separation,calculateRealSteering,decayCoefficient,distanceSqr,linear,reportNeighbor,toAgent
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

/** `Separation` is a group behavior producing a steering acceleration repelling from the other neighbors which are the agents in the immediate area defined by the given [[Proximity]]. The
  * acceleration is calculated by iterating through all the neighbors, examining each one. The vector to each agent under consideration is normalized, multiplied by a strength decreasing according to
  * the inverse square law in relation to distance, and accumulated.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Separation[T <: Vector[T]](
  owner:     Steerable[T],
  proximity: Proximity[T]
) extends GroupBehavior[T](owner, proximity)
    with Proximity.ProximityCallback[T] {

  /** The constant coefficient of decay for the inverse square law force. It controls how fast the separation strength decays with distance.
    */
  var decayCoefficient: Float = 1f

  private val toAgent: T = newVector(owner)
  private var linear:  T = uninitialized

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    steering.setZero()

    linear = steering.linear

    proximity.findNeighbors(this)

    steering
  }

  override def reportNeighbor(neighbor: Steerable[T]): Boolean = {
    toAgent.set(owner.position).-(neighbor.position)
    val distanceSqr = toAgent.lengthSq

    if (distanceSqr == 0) {
      true
    } else {
      val maxAcceleration = getActualLimiter().maxLinearAcceleration

      // Calculate the strength of repulsion through inverse square law decay
      var strength = decayCoefficient / distanceSqr
      if (strength > maxAcceleration) strength = maxAcceleration

      // Add the acceleration
      // Optimized code for linear.mulAdd(toAgent.normalize(), strength);
      linear.mulAdd(toAgent, strength / Math.sqrt(distanceSqr).toFloat)

      true
    }
  }
}
