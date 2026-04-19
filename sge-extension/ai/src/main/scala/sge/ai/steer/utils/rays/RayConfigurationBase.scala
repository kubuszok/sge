/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/rays/RayConfigurationBase.java
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
 * Covenant-baseline-loc: 44
 * Covenant-baseline-methods: RayConfigurationBase,arr,i,owner,rays
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package utils
package rays

import sge.ai.utils.Ray
import sge.math.Vector

/** `RayConfigurationBase` is the base class for concrete ray configurations having a fixed number of rays.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class RayConfigurationBase[T <: Vector[T]](
  var owner: Steerable[T],
  numRays:   Int
) extends RayConfiguration[T] {

  var rays: Array[Ray[T]] = {
    val arr = new Array[Ray[T]](numRays)
    var i   = 0
    while (i < numRays) {
      arr(i) = new Ray[T](owner.position.copy.setZero(), owner.position.copy.setZero())
      i += 1
    }
    arr
  }
}
