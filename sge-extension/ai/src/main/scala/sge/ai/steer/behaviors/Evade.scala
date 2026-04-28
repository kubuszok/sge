/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Evade.java
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
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: Evade,getActualMaxLinearAcceleration
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Evade.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: Evade,getActualMaxLinearAcceleration
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

/** `Evade` behavior is almost the same as [[Pursue]] except that the agent flees from the estimated future position of the pursuer. Indeed, reversing the acceleration is all we have to do.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Evade[T <: Vector[T]](
  owner:             Steerable[T],
  target:            Nullable[Steerable[T]] = Nullable.empty[Steerable[T]],
  maxPredictionTime: Float = 1f
) extends Pursue[T](owner, target, maxPredictionTime) {

  override protected def getActualMaxLinearAcceleration(): Float =
    // Simply return the opposite of the max linear acceleration so to evade the target
    -getActualLimiter().maxLinearAcceleration
}
