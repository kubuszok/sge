/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/patterns/DefensiveCircleFormationPattern.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma.patterns` -> `sge.ai.fma.patterns`; `.scl` -> `.scale`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: DefensiveCircleFormationPattern,_numberOfSlots,calculateSlotLocation,memberRadius,numberOfSlots,numberOfSlots_,supportsSlots
 * Covenant-source-reference: com/badlogic/gdx/ai/fma/patterns/DefensiveCircleFormationPattern.java
 *   Renames: `com.badlogic.gdx.ai.fma.patterns` -> `sge.ai.fma.patterns`; `.scl` -> `.scale`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: DefensiveCircleFormationPattern,_numberOfSlots,calculateSlotLocation,memberRadius,numberOfSlots,numberOfSlots_,supportsSlots
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

/** The defensive circle posts members around the circumference of a circle, so their backs are to the center of the circle. The circle can consist of any number of members. Although a huge number of
  * members might look silly, this implementation doesn't put any fixed limit.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class DefensiveCircleFormationPattern[T <: Vector[T]](
  /** The radius of one member. This is needed to determine how close we can pack a given number of members around circle. */
  val memberRadius: Float
) extends FormationPattern[T] {

  /** The number of slots currently in the pattern. */
  protected var _numberOfSlots: Int = 0

  override def numberOfSlots: Int = _numberOfSlots

  override def numberOfSlots_=(numberOfSlots: Int): Unit =
    _numberOfSlots = numberOfSlots

  override def calculateSlotLocation(outLocation: Location[T], slotNumber: Int): Location[T] = {
    if (_numberOfSlots > 1) {
      // Place the slot around the circle based on its slot number
      val angleAroundCircle = (MathUtils.PI2 * slotNumber) / _numberOfSlots

      // The radius depends on the radius of the member,
      // and the number of members in the circle:
      // we want there to be no gap between member's shoulders.
      val radius = memberRadius / Math.sin(Math.PI / _numberOfSlots).toFloat

      // Fill location components based on the angle around circle.
      outLocation.angleToVector(outLocation.position, angleAroundCircle).scale(radius)

      // The members should be facing out
      outLocation.orientation = angleAroundCircle
    } else {
      outLocation.position.setZero()
      outLocation.orientation = MathUtils.PI2 * slotNumber
    }

    // Return the slot location
    outLocation
  }

  override def supportsSlots(slotCount: Int): Boolean =
    // In this case we support any number of slots.
    true
}
