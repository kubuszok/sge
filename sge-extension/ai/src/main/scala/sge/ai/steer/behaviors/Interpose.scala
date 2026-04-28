/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Interpose.java
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
 * Covenant-baseline-methods: Interpose,agentA,agentB,calculateRealSteering,getInternalTargetPosition,internalTargetPosition,interpositionRatio,timeToTargetPosition
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Interpose.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: Interpose,agentA,agentB,calculateRealSteering,getInternalTargetPosition,internalTargetPosition,interpositionRatio,timeToTargetPosition
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

/** `Interpose` behavior produces a steering force that moves the owner to a point along the imaginary line connecting two other agents. A bodyguard taking a bullet for his employer or a soccer player
  * intercepting a pass are examples of this type of behavior.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Interpose[T <: Vector[T]](
  owner:                  Steerable[T],
  var agentA:             Steerable[T],
  var agentB:             Steerable[T],
  var interpositionRatio: Float = 0.5f
) extends Arrive[T](owner) {

  private val internalTargetPosition: T = newVector(owner)

  /** Returns the current position of the internal target. This method is useful for debug purpose. */
  def getInternalTargetPosition: T = internalTargetPosition

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // First we need to figure out where the two agents are going to be at
    // time T in the future. This is approximated by determining the time
    // taken by the owner to reach the desired point between the 2 agents
    // at the current time at the max speed.
    internalTargetPosition.set(agentB.position).-(agentA.position).scale(interpositionRatio).+(agentA.position)

    val timeToTargetPosition = owner.position.distance(internalTargetPosition) / getActualLimiter().maxLinearSpeed

    // Now we have the time, we assume that agent A and agent B will continue on a
    // straight trajectory and extrapolate to get their future positions.
    // Note that here we are reusing steering.linear vector as agentA future position
    // and internalTargetPosition as agentB future position.
    steering.linear.set(agentA.position).mulAdd(agentA.linearVelocity, timeToTargetPosition)
    internalTargetPosition.set(agentB.position).mulAdd(agentB.linearVelocity, timeToTargetPosition)

    // Calculate the target position between these predicted positions
    internalTargetPosition.-(steering.linear).scale(interpositionRatio).+(steering.linear)

    // Finally delegate to Arrive
    arrive(steering, internalTargetPosition)
  }
}
