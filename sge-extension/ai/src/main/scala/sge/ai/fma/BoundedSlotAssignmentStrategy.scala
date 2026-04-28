/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/BoundedSlotAssignmentStrategy.java
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
 * Covenant-baseline-loc: 59
 * Covenant-baseline-methods: BoundedSlotAssignmentStrategy,calculateNumberOfSlots,filledSlots,i,removeSlotAssignment,sn
 * Covenant-source-reference: com/badlogic/gdx/ai/fma/BoundedSlotAssignmentStrategy.java
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 59
 * Covenant-baseline-methods: BoundedSlotAssignmentStrategy,calculateNumberOfSlots,filledSlots,i,removeSlotAssignment,sn
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fma

import sge.math.Vector
import sge.utils.DynamicArray

/** `BoundedSlotAssignmentStrategy` is an abstract implementation of [[SlotAssignmentStrategy]] that supports roles. Generally speaking, there are hard and soft roles. Hard roles cannot be broken,
  * soft roles can.
  *
  * This abstract class provides an implementation of the [[calculateNumberOfSlots]] method that is more general (and costly) than the simplified implementation in [[FreeSlotAssignmentStrategy]]. It
  * scans the assignment list to find the number of filled slots, which is the highest slot number in the assignments.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class BoundedSlotAssignmentStrategy[T <: Vector[T]] extends SlotAssignmentStrategy[T] {

  override def calculateNumberOfSlots(assignments: DynamicArray[SlotAssignment[T]]): Int = {
    // Find the number of filled slots: it will be the
    // highest slot number in the assignments
    var filledSlots = -1
    var i           = 0
    while (i < assignments.size) {
      val assignment = assignments(i)
      if (assignment.slotNumber >= filledSlots) filledSlots = assignment.slotNumber
      i += 1
    }

    // Add one to go from the index of the highest slot to the number of slots needed.
    filledSlots + 1
  }

  override def removeSlotAssignment(assignments: DynamicArray[SlotAssignment[T]], index: Int): Unit = {
    val sn = assignments(index).slotNumber
    var i  = 0
    while (i < assignments.size) {
      val sa = assignments(i)
      if (sa.slotNumber >= sn) sa.slotNumber -= 1
      i += 1
    }
    assignments.removeIndex(index)
  }
}
