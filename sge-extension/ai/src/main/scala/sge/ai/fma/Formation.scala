/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/Formation.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`; `Array` -> `DynamicArray`;
 *     `.add` -> `.+`; `.sub` -> `.-`; `.scl` -> `.scale`; `.cpy` -> `.copy`
 *   Convention: split packages; `null` -> `Nullable`; getters/setters -> `var`
 *   Idiom: Vector2/Vector3 `mul(Matrix3)` -> type match with `rotateRad` for Vector2
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-ai/gdx-ai/src/com/badlogic/gdx/ai/fma/Formation.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Inherited gdx-ai TODO: "Consider the possibility of declaring mul(orientationMatrix)
 *     in Vector". Pending upstream design decision.
 */
package sge
package ai
package fma

import scala.util.boundary
import scala.util.boundary.break

import sge.ai.utils.Location
import sge.math.{ Matrix3, Vector, Vector2, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

/** A `Formation` coordinates the movement of a group of characters so that they retain some group organization. Characters belonging to a formation must implement the [[FormationMember]] trait. At
  * its simplest, a formation can consist of moving in a fixed geometric pattern such as a V or line abreast, but it is not limited to that. Formations can also make use of the environment. Squads of
  * characters can move between cover points using formation steering with only minor modifications, for example.
  *
  * Formation motion is used in team sports games, squad-based games, real-time strategy games, and sometimes in first-person shooters, driving games, and action adventures too. It is a simple and
  * flexible technique that is much quicker to write and execute and can produce much more stable behavior than collaborative tactical decision making.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Formation[T <: Vector[T]](
  /** The anchor point of this formation. */
  var anchor: Location[T],
  /** The formation pattern */
  var pattern: FormationPattern[T],
  /** The strategy used to assign a member to his slot */
  var slotAssignmentStrategy: SlotAssignmentStrategy[T] = FreeSlotAssignmentStrategy[T](),
  /** The formation motion moderator */
  var motionModerator: Nullable[FormationMotionModerator[T]] = Nullable.empty[FormationMotionModerator[T]]
) {
  require(anchor != null, "The anchor point cannot be null") // @nowarn - Java interop boundary check

  /** A list of slot assignments. */
  val slotAssignments: DynamicArray[SlotAssignment[T]] = DynamicArray[SlotAssignment[T]]()

  private val positionOffset:    T       = anchor.position.copy
  private val orientationMatrix: Matrix3 = Matrix3()

  /** The location representing the drift offset for the currently filled slots. */
  private val driftOffset: Location[T] = anchor.newLocation()

  /** Updates the assignment of members to slots */
  def updateSlotAssignments(): Unit = {
    // Apply the strategy to update slot assignments
    slotAssignmentStrategy.updateSlotAssignments(slotAssignments)

    // Set the newly calculated number of slots
    pattern.numberOfSlots = slotAssignmentStrategy.calculateNumberOfSlots(slotAssignments)

    // Update the drift offset if a motion moderator is set
    motionModerator.foreach(_.calculateDriftOffset(driftOffset, slotAssignments, pattern))
  }

  /** Changes the pattern of this formation and updates slot assignments if the number of members is supported by the given pattern.
    * @param pattern
    *   the pattern to set
    * @return
    *   `true` if the pattern has effectively changed; `false` otherwise.
    */
  def changePattern(pattern: FormationPattern[T]): Boolean = {
    // Find out how many slots we have occupied
    val occupiedSlots = slotAssignments.size

    // Check if the pattern supports one more slot
    if (pattern.supportsSlots(occupiedSlots)) {
      this.pattern = pattern

      // Update the slot assignments and return success
      updateSlotAssignments()

      true
    } else {
      false
    }
  }

  /** Adds a new member to the first available slot and updates slot assignments if the number of members is supported by the current pattern.
    * @param member
    *   the member to add
    * @return
    *   `false` if no more slots are available; `true` otherwise.
    */
  def addMember(member: FormationMember[T]): Boolean = {
    // Find out how many slots we have occupied
    val occupiedSlots = slotAssignments.size

    // Check if the pattern supports one more slot
    if (pattern.supportsSlots(occupiedSlots + 1)) {
      // Add a new slot assignment
      slotAssignments.add(SlotAssignment[T](member, occupiedSlots))

      // Update the slot assignments and return success
      updateSlotAssignments()
      true
    } else {
      false
    }
  }

  /** Removes a member from its slot and updates slot assignments.
    * @param member
    *   the member to remove
    */
  def removeMember(member: FormationMember[T]): Unit = {
    // Find the member's slot
    val slot = findMemberSlot(member)

    // Make sure we've found a valid result
    if (slot >= 0) {
      // Remove the slot
      slotAssignmentStrategy.removeSlotAssignment(slotAssignments, slot)

      // Update the assignments
      updateSlotAssignments()
    }
  }

  private def findMemberSlot(member: FormationMember[T]): Int =
    boundary[Int] {
      var i = 0
      while (i < slotAssignments.size) {
        if (slotAssignments(i).member eq member) break(i)
        i += 1
      }
      -1
    }

  // debug
  def slotAssignmentAt(index: Int): SlotAssignment[T] =
    slotAssignments(index)

  // debug
  def slotAssignmentCount: Int =
    slotAssignments.size

  /** Writes new slot locations to each member */
  def updateSlots(): Unit = {
    // Find the anchor point
    val currentAnchor = anchor

    positionOffset.set(currentAnchor.position)
    var orientationOffset = currentAnchor.orientation
    motionModerator.foreach { mod =>
      positionOffset.-(driftOffset.position)
      orientationOffset -= driftOffset.orientation
    }

    // Get the orientation of the anchor point as a matrix
    orientationMatrix.idt().rotateRad(currentAnchor.orientation)

    // Go through each member in turn
    var i = 0
    while (i < slotAssignments.size) {
      val slotAssignment = slotAssignments(i)

      // Retrieve the location reference of the formation member to calculate the new value
      val relativeLoc = slotAssignment.member.targetLocation

      // Ask for the location of the slot relative to the anchor point
      pattern.calculateSlotLocation(relativeLoc, slotAssignment.slotNumber)

      val relativeLocPosition = relativeLoc.position

      // TODO Consider the possibility of declaring mul(orientationMatrix) in Vector
      // Transform it by the anchor point's position and orientation.
      // Architecture divergence: Vector2.mul(Matrix3) is not exposed in SGE. The
      // orientationMatrix is always `identity.rotateRad(orientation)`, i.e. a pure
      // rotation, so `rotateRad(orientation)` is mathematically equivalent to
      // `mul(orientationMatrix)` for Vector2.
      relativeLocPosition match {
        case v2: Vector2 => v2.rotateRad(currentAnchor.orientation)
        case v3: Vector3 => v3.mul(orientationMatrix)
        case _ => // unsupported vector type, skip matrix transform
      }

      // Add the anchor and drift components
      relativeLocPosition.+(positionOffset)
      relativeLoc.orientation = relativeLoc.orientation + orientationOffset
      i += 1
    }

    // Possibly reset the anchor point if a moderator is set
    motionModerator.foreach(_.updateAnchorPoint(currentAnchor))
  }
}
