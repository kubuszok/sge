/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Face.java
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
 * Covenant-baseline-loc: 54
 * Covenant-baseline-methods: Face,calculateRealSteering,face,toTarget
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package behaviors

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.Nullable

/** `Face` behavior makes the owner look at its target. It delegates to the [[ReachOrientation]] behavior to perform the rotation but calculates the target orientation first based on target and owner
  * position.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Face[T <: Vector[T]](
  owner:  Steerable[T],
  target: Nullable[Location[T]] = Nullable.empty[Location[T]]
) extends ReachOrientation[T](owner, target) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] =
    face(steering, target.getOrElse(throw new IllegalStateException("target not set")).position)

  protected def face(steering: SteeringAcceleration[T], targetPosition: T): SteeringAcceleration[T] = {
    // Get the direction to target
    val toTarget = steering.linear.set(targetPosition).-(owner.position)

    // Check for a zero direction, and return no steering if so
    if (toTarget.isZero(getActualLimiter().zeroLinearSpeedThreshold)) {
      steering.setZero()
    } else {
      // Calculate the orientation to face the target
      val orientation = owner.vectorToAngle(toTarget)

      // Delegate to ReachOrientation
      reachOrientation(steering, orientation)
    }
  }
}
