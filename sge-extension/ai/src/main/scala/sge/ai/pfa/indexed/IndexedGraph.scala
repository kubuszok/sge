/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/indexed/IndexedGraph.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa.indexed` -> `sge.ai.pfa.indexed`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: IndexedGraph,getIndex,getNodeCount
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package pfa
package indexed

/** A graph for the [[IndexedAStarPathFinder]].
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait IndexedGraph[N] extends Graph[N] {

  /** Returns the unique index of the given node.
    * @param node
    *   the node whose index will be returned
    * @return
    *   the unique index of the given node.
    */
  def getIndex(node: N): Int

  /** Returns the number of nodes in this graph. */
  def getNodeCount: Int
}
