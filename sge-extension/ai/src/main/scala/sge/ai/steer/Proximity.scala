/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/Proximity.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, def+setter pairs instead of getX/setX
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: Proximity,ProximityCallback,findNeighbors,owner,owner_,reportNeighbor
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/Proximity.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, def+setter pairs instead of getX/setX
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: Proximity,ProximityCallback,findNeighbors,owner,owner_,reportNeighbor
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer

import sge.math.Vector

/** A `Proximity` defines an area that is used by group behaviors to find and process the owner's neighbors.
  *
  * Typically (but not necessarily) different group behaviors share the same `Proximity` for a given owner. This allows you to combine group behaviors so as to get a more complex behavior also known
  * as emergent behavior.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait Proximity[T <: Vector[T]] {

  /** Returns the owner of this proximity. */
  def owner: Steerable[T]

  /** Sets the owner of this proximity. */
  def owner_=(owner: Steerable[T]): Unit

  /** Finds the agents that are within the immediate area of the owner. Each of those agents is passed to the [[Proximity.ProximityCallback.reportNeighbor]] method of the specified callback.
    * @return
    *   the number of neighbors found.
    */
  def findNeighbors(callback: Proximity.ProximityCallback[T]): Int
}

object Proximity {

  /** The callback object used by a proximity to report the owner's neighbor.
    *
    * @tparam T
    *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
    *
    * @author
    *   davebaol (original implementation)
    */
  trait ProximityCallback[T <: Vector[T]] {

    /** The callback method used to report a neighbor.
      * @param neighbor
      *   the reported neighbor.
      * @return
      *   `true` if the given neighbor is valid; `false` otherwise.
      */
    def reportNeighbor(neighbor: Steerable[T]): Boolean
  }
}
