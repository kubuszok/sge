/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: UndirectedGraphAlgorithms,algorithm,findMinimumWeightSpanningTree
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package algorithms

/** Algorithms specific to undirected graphs. */
class UndirectedGraphAlgorithms[V](graph: UndirectedGraph[V]) extends Algorithms[V](graph) {

  /** Find a minimum weight spanning tree using Kruskal's algorithm.
    * @return
    *   a Graph object containing a minimum weight spanning tree (if this graph is connected, in general a minimum weight spanning forest)
    */
  def findMinimumWeightSpanningTree(): UndirectedGraph[V] = {
    val algorithm = MinimumWeightSpanningTree[V](requestRunID(), graph, true)
    algorithm.finish()
    algorithm.getSpanningTree
  }
}
