/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Pursue.java
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
 * Covenant-baseline-loc: 71
 * Covenant-baseline-methods: Pursue,calculateRealSteering,getActualMaxLinearAcceleration,maxPredictionTime,predictionTime,squareDistance,squareSpeed,target,targetPosition,tgt
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector
import sge.utils.Nullable

/** `Pursue` behavior produces a force that steers the agent towards the evader (the target). Actually it predicts where an agent will be in time `t` and seeks towards that point to intercept it.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Pursue[T <: Vector[T]](
  owner: Steerable[T],
  /** The target */
  var target: Nullable[Steerable[T]] = Nullable.empty[Steerable[T]],
  /** The maximum prediction time */
  var maxPredictionTime: Float = 1f
) extends SteeringBehavior[T](owner) {

  /** Returns the actual linear acceleration to be applied. This method is overridden by the [[Evade]] behavior to invert the maximum linear acceleration in order to evade the target.
    */
  protected def getActualMaxLinearAcceleration(): Float =
    getActualLimiter().maxLinearAcceleration

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    val tgt            = target.getOrElse(throw new IllegalStateException("target not set"))
    val targetPosition = tgt.position

    // Get the square distance to the evader (the target)
    val squareDistance = steering.linear.set(targetPosition).-(owner.position).lengthSq

    // Work out our current square speed
    val squareSpeed = owner.linearVelocity.lengthSq

    var predictionTime = maxPredictionTime

    if (squareSpeed > 0) {
      // Calculate prediction time if speed is not too small to give a reasonable value
      val squarePredictionTime = squareDistance / squareSpeed
      if (squarePredictionTime < maxPredictionTime * maxPredictionTime) {
        predictionTime = Math.sqrt(squarePredictionTime).toFloat
      }
    }

    // Calculate and seek/flee the predicted position of the target
    steering.linear.set(targetPosition).mulAdd(tgt.linearVelocity, predictionTime).-(owner.position).normalize().scale(getActualMaxLinearAcceleration())

    // No angular acceleration
    steering.angular = 0

    // Output steering acceleration
    steering
  }
}
