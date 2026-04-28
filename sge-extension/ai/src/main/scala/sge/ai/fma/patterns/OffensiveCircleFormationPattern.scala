/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/patterns/OffensiveCircleFormationPattern.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma.patterns` -> `sge.ai.fma.patterns`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: OffensiveCircleFormationPattern,calculateSlotLocation
 * Covenant-source-reference: com/badlogic/gdx/ai/fma/patterns/OffensiveCircleFormationPattern.java
 *   Renames: `com.badlogic.gdx.ai.fma.patterns` -> `sge.ai.fma.patterns`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: OffensiveCircleFormationPattern,calculateSlotLocation
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fma
package patterns

import sge.ai.utils.Location
import sge.math.{ MathUtils, Vector }

/** The offensive circle posts members around the circumference of a circle, so their fronts are to the center of the circle. The circle can consist of any number of members. Although a huge number of
  * members might look silly, this implementation doesn't put any fixed limit.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class OffensiveCircleFormationPattern[T <: Vector[T]](memberRadius: Float) extends DefensiveCircleFormationPattern[T](memberRadius) {

  override def calculateSlotLocation(outLocation: Location[T], slotNumber: Int): Location[T] = {
    super.calculateSlotLocation(outLocation, slotNumber)
    outLocation.orientation = outLocation.orientation + MathUtils.PI
    outLocation
  }
}
