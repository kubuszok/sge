/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Wander.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `GdxAI.getTimepiece()` -> `(using Timepiece)`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package behaviors

import sge.ai.Timepiece
import sge.math.{ MathUtils, Vector }

/** `Wander` behavior is designed to produce a steering acceleration that will give the impression of a random walk through the agent's environment.
  *
  * There is a circle in front of the owner (where front is determined by its current facing direction) on which the target is constrained. Each time the behavior is run, we move the target around the
  * circle a little, by a random amount.
  *
  * This implementation tries to face the target in each frame, using the [[Face]] behavior to align to the target, and applies full linear acceleration in the direction of its current orientation.
  * However, if you manually align owner's orientation to its linear velocity on each time step, [[Face]] behavior should not be used (which is the default case). On the other hand, if the owner has
  * independent facing you should explicitly set `faceEnabled = true` before using Wander behavior.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Wander[T <: Vector[T]](
  owner: Steerable[T]
)(using timepiece: Timepiece)
    extends Face[T](owner) {

  /** The forward offset of the wander circle */
  var wanderOffset: Float = 0f

  /** The radius of the wander circle */
  var wanderRadius: Float = 0f

  /** The rate, expressed in radian per second, at which the wander orientation can change */
  var wanderRate: Float = 0f

  /** The last time the orientation of the wander target has been updated */
  var lastTime: Float = 0f

  /** The current orientation of the wander target */
  var wanderOrientation: Float = 0f

  /** The flag indicating whether to use [[Face]] behavior or not. This should be set to `true` when independent facing is used.
    */
  var faceEnabled: Boolean = false

  private val internalTargetPosition: T = newVector(owner)
  private val wanderCenter:           T = newVector(owner)

  /** Returns the current position of the wander target. This method is useful for debug purpose. */
  def getInternalTargetPosition: T = internalTargetPosition

  /** Returns the current center of the wander circle. This method is useful for debug purpose. */
  def getWanderCenter: T = wanderCenter

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // Update the wander orientation
    val now = timepiece.time
    if (lastTime > 0) {
      val delta = now - lastTime
      wanderOrientation += MathUtils.randomTriangular(wanderRate * delta)
    }
    lastTime = now

    // Calculate the combined target orientation
    val targetOrientation = wanderOrientation + owner.orientation

    // Calculate the center of the wander circle
    wanderCenter.set(owner.position).mulAdd(owner.angleToVector(steering.linear, owner.orientation), wanderOffset)

    // Calculate the target location
    // Notice that we're using steering.linear as temporary vector
    internalTargetPosition.set(wanderCenter).mulAdd(owner.angleToVector(steering.linear, targetOrientation), wanderRadius)

    val maxLinearAcceleration = getActualLimiter().maxLinearAcceleration

    if (faceEnabled) {
      // Delegate to face
      face(steering, internalTargetPosition)

      // Set the linear acceleration to be at full
      // acceleration in the direction of the orientation
      owner.angleToVector(steering.linear, owner.orientation).scale(maxLinearAcceleration)
    } else {
      // Seek the internal target position
      steering.linear.set(internalTargetPosition).-(owner.position).normalize().scale(maxLinearAcceleration)

      // No angular acceleration
      steering.angular = 0
    }

    steering
  }
}
