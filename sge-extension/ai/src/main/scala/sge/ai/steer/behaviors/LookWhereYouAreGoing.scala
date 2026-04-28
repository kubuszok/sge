/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/LookWhereYouAreGoing.java
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
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: LookWhereYouAreGoing,calculateRealSteering
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/LookWhereYouAreGoing.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: LookWhereYouAreGoing,calculateRealSteering
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

/** `LookWhereYouAreGoing` behavior gives the owner angular acceleration to make it face in the direction it is moving. In this way the owner changes facing gradually, which can look more natural,
  * especially for aerial vehicles such as helicopters or for human characters that can move sideways.
  *
  * This is a process similar to the `Face` behavior. The target orientation is calculated using the current velocity of the owner. If there is no velocity, then the target orientation is set to the
  * current orientation.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class LookWhereYouAreGoing[T <: Vector[T]](
  owner: Steerable[T]
) extends ReachOrientation[T](owner) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] =
    // Check for a zero direction, and return no steering if so
    if (owner.linearVelocity.isZero(getActualLimiter().zeroLinearSpeedThreshold)) {
      steering.setZero()
    } else {
      // Calculate the orientation based on the velocity of the owner
      val orientation = owner.vectorToAngle(owner.linearVelocity)

      // Delegate to ReachOrientation
      reachOrientation(steering, orientation)
    }
}
