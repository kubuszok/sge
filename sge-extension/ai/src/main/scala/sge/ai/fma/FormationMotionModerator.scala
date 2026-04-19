/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/FormationMotionModerator.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`
 *   Convention: split packages; `null` -> `Nullable`; `.add` -> `.+`; `.scl` -> `.scale`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: FormationMotionModerator,calculateDriftOffset,centerOfMassOrientation,centerOfMassPos,i,numberOfAssignments,tempLocation,tempLocationPos,tl,updateAnchorPoint
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package fma

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.{ DynamicArray, Nullable }

/** A `FormationMotionModerator` moderates the movement of the formation based on the current positions of the members in its slots: in effect to keep the anchor point on a leash. If the members in
  * the slots are having trouble reaching their targets, then the formation as a whole should be held back to give them a chance to catch up.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class FormationMotionModerator[T <: Vector[T]] {

  private var tempLocation: Nullable[Location[T]] = Nullable.empty

  /** Update the anchor point to moderate formation motion. This method is called at each frame.
    * @param anchor
    *   the anchor point
    */
  def updateAnchorPoint(anchor: Location[T]): Unit

  /** Calculates the drift offset when members are in the given set of slots for the specified pattern.
    * @param centerOfMass
    *   the output location set to the calculated drift offset
    * @param slotAssignments
    *   the set of slots
    * @param pattern
    *   the pattern
    * @return
    *   the given location for chaining.
    */
  def calculateDriftOffset(
    centerOfMass:    Location[T],
    slotAssignments: DynamicArray[SlotAssignment[T]],
    pattern:         FormationPattern[T]
  ): Location[T] = {

    // Clear the center of mass
    centerOfMass.position.setZero()
    var centerOfMassOrientation = 0f

    // Make sure tempLocation is instantiated
    if (tempLocation.isEmpty) tempLocation = Nullable(centerOfMass.newLocation())

    val tl              = tempLocation.get
    val centerOfMassPos = centerOfMass.position
    val tempLocationPos = tl.position

    // Go through each assignment and add its contribution to the center
    val numberOfAssignments = slotAssignments.size
    var i                   = 0
    while (i < numberOfAssignments) {
      pattern.calculateSlotLocation(tl, slotAssignments(i).slotNumber)
      centerOfMassPos.+(tempLocationPos)
      centerOfMassOrientation += tl.orientation
      i += 1
    }

    // Divide through to get the drift offset.
    centerOfMassPos.scale(1f / numberOfAssignments)
    centerOfMassOrientation /= numberOfAssignments
    centerOfMass.orientation = centerOfMassOrientation

    centerOfMass
  }
}
