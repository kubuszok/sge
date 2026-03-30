/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/proximities/FieldOfViewProximity.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `GdxAI.getTimepiece()` -> `(using Timepiece)`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package proximities

import sge.ai.Timepiece
import sge.math.Vector

/** `FieldOfViewProximity` emulates the peripheral vision of the owner as if it had eyes. Any agents contained in the specified list that are within the field of view of the owner are considered
  * owner's neighbors. The field of view is determined by a radius and an angle in radians.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class FieldOfViewProximity[T <: Vector[T]](
  owner:  Steerable[T],
  agents: Iterable[? <: Steerable[T]],
  /** The radius of this proximity. */
  var radius: Float,
  angle0:     Float
)(using timepiece: Timepiece)
    extends ProximityBase[T](owner, agents) {

  /** The angle in radians of this proximity. */
  private var _angle:           Float = angle0
  private var coneThreshold:    Float = Math.cos(angle0 * 0.5f).toFloat
  private var lastTime:         Float = 0f
  private val ownerOrientation: T     = owner.position.copy.setZero()
  private val toAgent:          T     = owner.position.copy.setZero()

  /** Returns the angle of this proximity in radians. */
  def angle: Float = _angle

  /** Sets the angle of this proximity in radians. */
  def angle_=(angle: Float): Unit = {
    this._angle = angle
    this.coneThreshold = Math.cos(angle * 0.5f).toFloat
  }

  override def findNeighbors(callback: Proximity.ProximityCallback[T]): Int = {
    var neighborCount = 0

    // If the frame is new then avoid repeating calculations
    // when this proximity is used by multiple group behaviors.
    val currentTime = timepiece.time
    if (this.lastTime != currentTime) {
      // Save the current time
      this.lastTime = currentTime

      val ownerPosition = owner.position

      // Transform owner orientation to a Vector
      owner.angleToVector(ownerOrientation, owner.orientation)

      // Scan the agents searching for neighbors
      for (currentAgent <- agents)
        // Make sure the agent being examined isn't the owner
        if (currentAgent ne owner) {
          toAgent.set(currentAgent.position).-(ownerPosition)

          // The bounding radius of the current agent is taken into account
          // by adding it to the radius proximity
          val range = radius + currentAgent.boundingRadius

          val toAgentLen2 = toAgent.lengthSq

          // Make sure the current agent is within the range.
          // Notice we're working in distance-squared space to avoid square root.
          if (toAgentLen2 < range * range) {
            // If the current agent is within the field of view of the owner,
            // report it to the callback and tag it for further consideration.
            if (ownerOrientation.dot(toAgent) > coneThreshold) {
              if (callback.reportNeighbor(currentAgent)) {
                currentAgent.tagged = true
                neighborCount += 1
              } else {
                currentAgent.tagged = false
              }
            } else {
              currentAgent.tagged = false
            }
          } else {
            currentAgent.tagged = false
          }
        } else {
          // Clear the tag
          currentAgent.tagged = false
        }
    } else {
      // Scan the agents searching for tagged neighbors
      for (currentAgent <- agents)
        // Make sure the agent being examined isn't the owner and that
        // it's tagged.
        if ((currentAgent ne owner) && currentAgent.tagged) {
          if (callback.reportNeighbor(currentAgent)) {
            neighborCount += 1
          }
        }
    }

    neighborCount
  }
}
