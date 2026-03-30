/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/proximities/RadiusProximity.java
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

/** A `RadiusProximity` elaborates any agents contained in the specified list that are within the radius of the owner.
  *
  * Note that this implementation checks the AI time of the current frame through the `Timepiece.time` method in order to calculate neighbors only once per frame (assuming delta time is always greater
  * than 0, if time has changed the frame has changed too).
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class RadiusProximity[T <: Vector[T]](
  owner:  Steerable[T],
  agents: Iterable[? <: Steerable[T]],
  /** The radius of this proximity. */
  var radius: Float
)(using timepiece: Timepiece)
    extends ProximityBase[T](owner, agents) {

  private var lastTime: Float = 0f

  override def findNeighbors(callback: Proximity.ProximityCallback[T]): Int = {
    var neighborCount = 0

    // If the frame is new then avoid repeating calculations
    // when this proximity is used by multiple group behaviors.
    val currentTime = timepiece.time
    if (this.lastTime != currentTime) {
      // Save the current time
      this.lastTime = currentTime

      val ownerPosition = owner.position

      // Scan the agents searching for neighbors
      for (currentAgent <- agents)
        // Make sure the agent being examined isn't the owner
        if (currentAgent ne owner) {
          val squareDistance = ownerPosition.distanceSq(currentAgent.position)

          // The bounding radius of the current agent is taken into account
          // by adding it to the range
          val range = radius + currentAgent.boundingRadius

          // If the current agent is within the range, report it to the callback
          // and tag it for further consideration.
          if (squareDistance < range * range) {
            if (callback.reportNeighbor(currentAgent)) {
              currentAgent.tagged = true
              neighborCount += 1
            } else {
              currentAgent.tagged = false
            }
          } else {
            // Clear the tag
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
