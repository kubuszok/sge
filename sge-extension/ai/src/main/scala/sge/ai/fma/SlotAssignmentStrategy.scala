/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/SlotAssignmentStrategy.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package fma

import sge.math.Vector
import sge.utils.DynamicArray

/** This trait defines how each [[FormationMember]] is assigned to a slot in the [[Formation]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait SlotAssignmentStrategy[T <: Vector[T]] {

  /** Updates the assignment of members to slots */
  def updateSlotAssignments(assignments: DynamicArray[SlotAssignment[T]]): Unit

  /** Calculates the number of slots from the assignment data. */
  def calculateNumberOfSlots(assignments: DynamicArray[SlotAssignment[T]]): Int

  /** Removes the slot assignment at the specified index. */
  def removeSlotAssignment(assignments: DynamicArray[SlotAssignment[T]], index: Int): Unit
}
