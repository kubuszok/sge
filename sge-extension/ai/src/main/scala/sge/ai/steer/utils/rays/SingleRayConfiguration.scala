/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/rays/SingleRayConfiguration.java
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
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: SingleRayConfiguration,length,updateRays
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/utils/rays/SingleRayConfiguration.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: SingleRayConfiguration,length,updateRays
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
import sge.math.Vector

/** As the name suggests, a `SingleRayConfiguration` uses just one ray cast.
  *
  * This configuration is useful in concave environments but grazes convex obstacles. It is not susceptible to the corner trap, though.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class SingleRayConfiguration[T <: Vector[T]](
  owner: Steerable[T],
  /** The length of the ray. */
  var length: Float
) extends RayConfigurationBase[T](owner, 1) {

  override def updateRays(): Array[Ray[T]] = {
    rays(0).start.set(owner.position)
    rays(0).end.set(owner.linearVelocity).normalize().scale(length).+(rays(0).start)
    rays
  }
}
