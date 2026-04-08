/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/SoftRoleSlotAssignmentStrategy.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`; `BooleanArray` -> `Array[Boolean]`
 *   Convention: split packages; `continue LABEL` -> `boundary`/`break`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-ai/gdx-ai/src/com/badlogic/gdx/ai/fma/SoftRoleSlotAssignmentStrategy.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Inherited gdx-ai TODO at line 133 (no description in upstream source).
 */
package sge
package ai
package fma

import scala.util.boundary
import scala.util.boundary.break

import sge.math.Vector
import sge.utils.DynamicArray

/** `SoftRoleSlotAssignmentStrategy` is a concrete implementation of [[BoundedSlotAssignmentStrategy]] that supports soft roles, i.e. roles that can be broken. Rather than a member having a list of
  * roles it can fulfill, it has a set of values representing how difficult it would find it to fulfill every role. The value is known as the slot cost. To make a slot impossible for a member to fill,
  * its slot cost should be infinite (you can even set a threshold to ignore all slots whose cost is too high; this will reduce computation time when several costs are exceeding). To make a slot ideal
  * for a member, its slot cost should be zero. We can have different levels of unsuitable assignment for one member.
  *
  * Slot costs do not necessarily have to depend only on the member and the slot roles. They can be generalized to include any difficulty a member might have in taking up a slot. If a formation is
  * spread out, for example, a member may choose a slot that is close by over a more distant slot. Distance can be directly used as a slot cost.
  *
  * '''IMPORTANT NOTES:'''
  *   - In order for the algorithm to work properly the slot costs can not be negative.
  *   - This algorithm is often not fast enough to be used regularly. However, slot assignment happens relatively seldom (when the player selects a new pattern, for example, or adds a member to the
  *     formation, or a member is removed from the formation).
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class SoftRoleSlotAssignmentStrategy[T <: Vector[T]](
  protected val slotCostProvider: SlotCostProvider[T],
  protected val costThreshold:    Float = Float.PositiveInfinity
) extends BoundedSlotAssignmentStrategy[T] {

  private var filledSlots: Array[Boolean] = Array.empty[Boolean]

  override def updateSlotAssignments(assignments: DynamicArray[SlotAssignment[T]]): Unit = {

    // Holds a list of member and slot data for each member.
    val memberData = DynamicArray[MemberAndSlots[T]]()

    // Compile the member data
    val numberOfAssignments = assignments.size
    var i                   = 0
    while (i < numberOfAssignments) {
      val assignment = assignments(i)

      // Create a new member datum, and fill it
      val datum = MemberAndSlots[T](assignment.member)

      // Add each valid slot to it
      var j = 0
      while (j < numberOfAssignments) {

        // Get the cost of the slot
        val cost = slotCostProvider.getCost(assignment.member, j)

        // Make sure the slot is valid
        if (cost < costThreshold) {
          val slot = assignments(j)

          // Store the slot information
          val slotDatum = CostAndSlot[T](cost, slot.slotNumber)
          datum.costAndSlots.add(slotDatum)

          // Add it to the member's ease of assignment
          datum.assignmentEase += 1f / (1f + cost)
        }
        j += 1
      }

      // Add member datum
      memberData.add(datum)
      i += 1
    }

    // Reset the array to keep track of which slots we have already filled.
    if (numberOfAssignments > filledSlots.length) {
      filledSlots = new Array[Boolean](numberOfAssignments)
    }
    i = 0
    while (i < numberOfAssignments) {
      filledSlots(i) = false
      i += 1
    }

    // Arrange members in order of ease of assignment, with the least easy first.
    memberData.sort()
    i = 0
    while (i < memberData.size) {
      val memberDatum = memberData(i)

      // Choose the first slot in the list that is still empty (non-filled)
      memberDatum.costAndSlots.sort()
      val assigned = boundary[Boolean] {
        val m = memberDatum.costAndSlots.size
        var j = 0
        while (j < m) {
          val slotNumber = memberDatum.costAndSlots(j).slotNumber

          // Check if this slot is valid
          if (!filledSlots(slotNumber)) {
            // Fill this slot
            val slot = assignments(slotNumber)
            slot.member = memberDatum.member
            slot.slotNumber = slotNumber

            // Reserve the slot
            filledSlots(slotNumber) = true

            // Go to the next member
            break(true)
          }
          j += 1
        }
        false
      }

      // If we reach here without assignment, it's because a member has no valid assignment.
      //
      // TODO
      // Some sensible action should be taken, such as reporting to the player.
      if (!assigned) {
        throw new RuntimeException(
          "SoftRoleSlotAssignmentStrategy cannot find valid slot assignment for member " + memberDatum.member
        )
      }
      i += 1
    }
  }
}

private[fma] class CostAndSlot[T <: Vector[T]](val cost: Float, val slotNumber: Int) extends Comparable[CostAndSlot[T]] {
  override def compareTo(other: CostAndSlot[T]): Int =
    if (cost < other.cost) -1 else if (cost > other.cost) 1 else 0
}

private[fma] class MemberAndSlots[T <: Vector[T]](val member: FormationMember[T]) extends Comparable[MemberAndSlots[T]] {
  var assignmentEase: Float                        = 0f
  val costAndSlots:   DynamicArray[CostAndSlot[T]] = DynamicArray[CostAndSlot[T]]()

  override def compareTo(other: MemberAndSlots[T]): Int =
    if (assignmentEase < other.assignmentEase) -1 else if (assignmentEase > other.assignmentEase) 1 else 0
}

/** Provider of slot costs used by [[SoftRoleSlotAssignmentStrategy]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  */
trait SlotCostProvider[T <: Vector[T]] {
  def getCost(member: FormationMember[T], slotNumber: Int): Float
}
