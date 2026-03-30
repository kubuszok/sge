/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

import scala.collection.mutable

/** Detects cycles in a graph using depth-first search. */
class CycleDetector[V] private[algorithms] (id: Int, graph: Graph[V]) extends Algorithm[V](id) {

  private val _containsCycle: Boolean = findCycle(graph)

  override def update(): Boolean = true

  override def isFinished: Boolean = true

  private def findCycle(graph: Graph[V]): Boolean = {
    if (graph.size < 3 || graph.edgeCount < 3) {
      return false
    }
    val runID = graph.algorithms.requestRunID()
    val nodeIter = graph.getNodes.iterator
    while (nodeIter.hasNext) {
      val v = nodeIter.next()
      v.resetAlgorithmAttribs(runID)
      if (detectCycleDFS(v, null.asInstanceOf[Node[V]], mutable.HashSet[Node[V]](), runID, graph)) { // @nowarn — null parent for root
        return true
      }
    }
    false
  }

  private def detectCycleDFS(
      v: Node[V],
      parent: Node[V],
      recursiveStack: mutable.HashSet[Node[V]],
      runID: Int,
      graph: Graph[V]
  ): Boolean = {
    v.processed = true
    recursiveStack.add(v)
    val outEdges = v.outEdges
    val edgeIter = outEdges.iterator
    while (edgeIter.hasNext) {
      val e = edgeIter.next()
      val u = e.nodeB
      if (!graph.isDirected && (u eq parent)) {
        // skip — undirected back-edge to parent
      } else {
        u.resetAlgorithmAttribs(runID)
        if (recursiveStack.contains(u)) {
          return true
        }
        if (!u.processed) {
          if (detectCycleDFS(u, v, recursiveStack, runID, graph)) {
            return true
          }
        }
      }
    }
    recursiveStack.remove(v)
    false
  }

  def containsCycle: Boolean = _containsCycle
}
