/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/MatchVelocity.java
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
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: MatchVelocity,calculateRealSteering,target,tgt,timeToTarget
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/MatchVelocity.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: MatchVelocity,calculateRealSteering,target,tgt,timeToTarget
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
import sge.utils.Nullable

/** This steering behavior produces a linear acceleration trying to match target's velocity. It does not produce any angular acceleration.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class MatchVelocity[T <: Vector[T]](
  owner: Steerable[T],
  /** The target of this behavior */
  var target: Nullable[Steerable[T]] = Nullable.empty[Steerable[T]],
  /** The time over which to achieve target speed */
  var timeToTarget: Float = 0.1f
) extends SteeringBehavior[T](owner) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    val tgt = target.getOrElse(throw new IllegalStateException("target not set"))

    // Acceleration tries to get to the target velocity without exceeding max acceleration
    steering.linear.set(tgt.linearVelocity).-(owner.linearVelocity).scale(1f / timeToTarget).limit(getActualLimiter().maxLinearAcceleration)

    // No angular acceleration
    steering.angular = 0

    // Output steering acceleration
    steering
  }
}
