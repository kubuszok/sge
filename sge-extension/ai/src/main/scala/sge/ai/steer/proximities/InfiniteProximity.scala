/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/proximities/InfiniteProximity.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: InfiniteProximity,findNeighbors,neighborCount
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/proximities/InfiniteProximity.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: InfiniteProximity,findNeighbors,neighborCount
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package proximities

import sge.math.Vector

/** `InfiniteProximity` is likely the simplest type of Proximity one can imagine. All the agents contained in the specified list are considered neighbors of the owner, excluded the owner itself (if it
  * is part of the list).
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class InfiniteProximity[T <: Vector[T]](
  owner:  Steerable[T],
  agents: Iterable[? <: Steerable[T]]
) extends ProximityBase[T](owner, agents) {

  override def findNeighbors(callback: Proximity.ProximityCallback[T]): Int = {
    var neighborCount = 0
    for (currentAgent <- agents)
      // Make sure the agent being examined isn't the owner
      if (currentAgent ne owner) {
        if (callback.reportNeighbor(currentAgent)) {
          neighborCount += 1
        }
      }
    neighborCount
  }
}
