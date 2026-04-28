/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/rays/ParallelSideRayConfiguration.java
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
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: HALF_PI,ParallelSideRayConfiguration,length,sideOffset,updateRays,velocityAngle
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/utils/rays/ParallelSideRayConfiguration.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: HALF_PI,ParallelSideRayConfiguration,length,sideOffset,updateRays,velocityAngle
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package utils
package rays

import sge.ai.utils.Ray
import sge.math.{ MathUtils, Vector }

/** A `ParallelSideRayConfiguration` uses two rays parallel to the direction of motion. The rays have the same length and opposite side offset.
  *
  * The parallel configuration works well in areas where corners are highly obtuse but is very susceptible to the corner trap.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class ParallelSideRayConfiguration[T <: Vector[T]](
  owner: Steerable[T],
  /** The length of the rays. */
  var length: Float,
  /** The side offset of the rays. */
  var sideOffset: Float
) extends RayConfigurationBase[T](owner, 2) {

  private val HALF_PI: Float = MathUtils.PI * 0.5f

  override def updateRays(): Array[Ray[T]] = {
    val velocityAngle = owner.vectorToAngle(owner.linearVelocity)

    // Update ray 0
    owner.angleToVector(rays(0).start, velocityAngle - HALF_PI).scale(sideOffset).+(owner.position)
    rays(0).end.set(owner.linearVelocity).normalize().scale(length) // later we'll add rays(0).start

    // Update ray 1
    owner.angleToVector(rays(1).start, velocityAngle + HALF_PI).scale(sideOffset).+(owner.position)
    rays(1).end.set(rays(0).end).+(rays(1).start)

    // add start position to ray 0
    rays(0).end.+(rays(0).start)

    rays
  }
}
