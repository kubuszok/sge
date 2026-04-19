/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/Heuristic.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: Heuristic,estimate
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package pfa

/** A `Heuristic` generates estimates of the cost to move from a given node to the goal.
  *
  * With a heuristic function pathfinding algorithms can choose the node that is most likely to lead to the optimal path. The notion of "most likely" is controlled by a heuristic. If the heuristic is
  * accurate, then the algorithm will be efficient. If the heuristic is terrible, then it can perform even worse than other algorithms that don't use any heuristic function such as Dijkstra.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait Heuristic[N] {

  /** Calculates an estimated cost to reach the goal node from the given node.
    * @param node
    *   the start node
    * @param endNode
    *   the end node
    * @return
    *   the estimated cost
    */
  def estimate(node: N, endNode: N): Float
}
