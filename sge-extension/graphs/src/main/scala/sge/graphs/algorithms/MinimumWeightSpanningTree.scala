/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

import scala.collection.mutable.ArrayDeque

/** Minimum (or maximum) weight spanning tree using Kruskal's algorithm. */
class MinimumWeightSpanningTree[V] private[algorithms] (
    id: Int,
    graph: UndirectedGraph[V],
    minSpanningTree: Boolean
) extends Algorithm[V](id) {

  private val spanningTree: UndirectedGraph[V] = graph.createNew()
  spanningTree.addVertices(graph.vertices)

  private val edgeQueue: ArrayDeque[Connection[V]] = {
    val sorted = graph.edgeMap.values.toSeq.sortWith { (a, b) =>
      if (minSpanningTree) a.weight < b.weight
      else a.weight > b.weight
    }
    val q = ArrayDeque[Connection[V]]()
    sorted.foreach(q.append)
    q
  }

  private val finishAt: Int = if (graph.isConnected) graph.size - 1 else -1

  override def update(): Boolean = {
    if (isFinished) {
      return true
    }

    val edge = edgeQueue.removeHead()

    if (doesEdgeCreateCycle(edge.nodeA, edge.nodeB, id)) {
      return false
    }
    spanningTree.addEdge(edge.a, edge.b, edge.weightFunction)

    isFinished
  }

  private def unionByRank(rootU: Node[V], rootV: Node[V]): Unit = {
    if (rootU.index < rootV.index) {
      rootU.prev = rootV
    } else {
      rootV.prev = rootU
      if (rootU.index == rootV.index) rootU.index = rootU.index + 1
    }
  }

  private def find(node: Node[V]): Node[V] = {
    if (node eq node.prev) node
    else find(node.prev)
  }

  private def pathCompressionFind(node: Node[V]): Node[V] = {
    if (node eq node.prev) {
      node
    } else {
      val parentNode = find(node.prev)
      node.prev = parentNode
      parentNode
    }
  }

  private def doesEdgeCreateCycle(u: Node[V], v: Node[V], runID: Int): Boolean = {
    if (u.resetAlgorithmAttribs(runID)) u.prev = u
    if (v.resetAlgorithmAttribs(runID)) v.prev = v
    val rootU = pathCompressionFind(u)
    val rootV = pathCompressionFind(v)
    if (rootU eq rootV) {
      return true
    }
    unionByRank(rootU, rootV)
    false
  }

  override def isFinished: Boolean = {
    if (finishAt < 0) edgeQueue.isEmpty
    else spanningTree.edgeCount == finishAt
  }

  def getSpanningTree: UndirectedGraph[V] = spanningTree
}
