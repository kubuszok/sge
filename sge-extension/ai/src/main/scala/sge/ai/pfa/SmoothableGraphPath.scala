/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/SmoothableGraphPath.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: SmoothableGraphPath,getNodePosition,swapNodes,truncatePath
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/SmoothableGraphPath.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: SmoothableGraphPath,getNodePosition,swapNodes,truncatePath
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

import sge.math.Vector

/** A path that can be smoothed by a [[PathSmoother]].
  *
  * @tparam N
  *   Type of node
  * @tparam V
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait SmoothableGraphPath[N, V <: Vector[V]] extends GraphPath[N] {

  /** Returns the position of the node at the given index.
    * @param index
    *   the index of the node you want to know the position
    */
  def getNodePosition(index: Int): V

  /** Swaps the specified nodes of this path.
    * @param index1
    *   index of the first node to swap
    * @param index2
    *   index of the second node to swap
    */
  def swapNodes(index1: Int, index2: Int): Unit

  /** Reduces the size of this path to the specified length (number of nodes). If the path is already smaller than the specified length, no action is taken.
    * @param newLength
    *   the new length
    */
  def truncatePath(newLength: Int): Unit
}
