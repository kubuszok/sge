/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/FreeSlotAssignmentStrategy.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 48
 * Covenant-baseline-methods: FreeSlotAssignmentStrategy,calculateNumberOfSlots,i,removeSlotAssignment,updateSlotAssignments
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package fma

import sge.math.Vector
import sge.utils.DynamicArray

/** `FreeSlotAssignmentStrategy` is the simplest implementation of [[SlotAssignmentStrategy]]. It simply goes through each assignment in the list and assigns sequential slot numbers. The number of
  * slots is just the length of the list.
  *
  * Because each member can occupy any slot this implementation does not support roles.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class FreeSlotAssignmentStrategy[T <: Vector[T]] extends SlotAssignmentStrategy[T] {

  override def updateSlotAssignments(assignments: DynamicArray[SlotAssignment[T]]): Unit = {
    // A very simple assignment algorithm: we simply go through
    // each assignment in the list and assign sequential slot numbers
    var i = 0
    while (i < assignments.size) {
      assignments(i).slotNumber = i
      i += 1
    }
  }

  override def calculateNumberOfSlots(assignments: DynamicArray[SlotAssignment[T]]): Int =
    assignments.size

  override def removeSlotAssignment(assignments: DynamicArray[SlotAssignment[T]], index: Int): Unit =
    assignments.removeIndex(index)
}
