/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/FollowFlowField.java
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
 * Covenant-baseline-loc: 89
 * Covenant-baseline-methods: FlowField,FollowFlowField,calculateRealSteering,ff,flowField,flowVector,location,lookup,predictionTime
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector
import sge.utils.Nullable

/** The `FollowFlowField` behavior produces a linear acceleration that tries to align the motion of the owner with the local tangent of a flow field.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class FollowFlowField[T <: Vector[T]](
  owner: Steerable[T],
  /** The flow field to follow. */
  var flowField: Nullable[FollowFlowField.FlowField[T]] = Nullable.empty[FollowFlowField.FlowField[T]],
  /** The time in the future to predict the owner's position. Set it to 0 for non-predictive flow field following. */
  var predictionTime: Float = 0f
) extends SteeringBehavior[T](owner) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    val ff = flowField.getOrElse(throw new IllegalStateException("flowField not set"))

    // Predictive or non-predictive behavior?
    val location: T =
      if (predictionTime == 0) {
        // Use the current position of the owner
        owner.position
      } else {
        // Calculate the predicted future position of the owner. We're reusing steering.linear here.
        steering.linear.set(owner.position).mulAdd(owner.linearVelocity, predictionTime)
      }

    // Retrieve the flow vector at the specified location
    val flowVector: Nullable[T] = Nullable(ff.lookup(location))

    // Clear both linear and angular components
    steering.setZero()

    flowVector.foreach { fv =>
      if (!fv.isZero) {
        val actualLimiter = getActualLimiter()

        // Calculate linear acceleration
        steering.linear.mulAdd(fv, actualLimiter.maxLinearSpeed).-(owner.linearVelocity).limit(actualLimiter.maxLinearAcceleration)
      }
    }

    // Output steering
    steering
  }
}

object FollowFlowField {

  /** A `FlowField` defines a mapping from a location in space to a flow vector. Typically flow fields are implemented as a multidimensional array representing a grid of cells. In each cell of the
    * grid lives a flow vector.
    *
    * @tparam T
    *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
    *
    * @author
    *   davebaol (original implementation)
    */
  trait FlowField[T <: Vector[T]] {

    /** Returns the flow vector for the specified position in space.
      * @param position
      *   the position to map
      */
    def lookup(position: T): T
  }
}
