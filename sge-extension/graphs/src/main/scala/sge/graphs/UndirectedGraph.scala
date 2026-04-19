/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: UndirectedGraph,_algorithms,addConnection,algorithms,apply,createNew,e,edge,existing,g,getConnection,getDegree,isDirected,node,obtainEdge,removeConnection
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs

import sge.graphs.algorithms.UndirectedGraphAlgorithms
import sge.graphs.utils.WeightFunction

/** Undirected graph implementation. Edges are bidirectional with linked twin connections. */
class UndirectedGraph[V] extends Graph[V] {

  private val _algorithms: UndirectedGraphAlgorithms[V] = UndirectedGraphAlgorithms[V](this)

  // ================================================================================
  // Graph building
  // ================================================================================

  override private[graphs] def obtainEdge(): Connection[V] = UndirectedConnection[V]()

  override private[graphs] def addConnection(a: Node[V], b: Node[V], weight: WeightFunction[V]): Connection[V] = {
    val existing = a.getEdge(b)
    if (existing == null) {
      val e1 = obtainEdge().asInstanceOf[UndirectedConnection[V]]
      val e2 = obtainEdge().asInstanceOf[UndirectedConnection[V]]
      e1.link(e2)
      e2.link(e1)
      e1.set(a, b, weight)
      e2.set(b, a, weight)
      a.addEdge(e1)
      b.addEdge(e2)
      edgeMap.put(e1, e1)
      e1
    } else {
      existing.setWeight(weight)
      existing
    }
  }

  override private[graphs] def addConnection(a: Node[V], b: Node[V]): Connection[V] = {
    val e = a.getEdge(b)
    if (e != null) edgeMap.getOrElse(e, e) else addConnection(a, b, defaultEdgeWeightFunction)
  }

  override private[graphs] def removeConnection(a: Node[V], b: Node[V]): Boolean = {
    val e = a.removeEdge(b)
    if (e == null) {
      false
    } else {
      b.removeEdge(a)
      edgeMap.remove(e)
      true
    }
  }

  override private[graphs] def getConnection(a: Node[V], b: Node[V]): Connection[V] = {
    val edge = a.getEdge(b)
    if (edge == null) null.asInstanceOf[Connection[V]] // @nowarn — null return matches original API
    else edgeMap.getOrElse(edge, edge) // get from map to ensure consistent instance is returned
  }

  // ================================================================================
  // Superclass implementations
  // ================================================================================

  override def isDirected: Boolean = false

  override def createNew(): UndirectedGraph[V] = UndirectedGraph[V]()

  override def algorithms: UndirectedGraphAlgorithms[V] = _algorithms

  /** @return the degree of this vertex, or -1 if it is not in the graph */
  def getDegree(v: V): Int = {
    val node = getNode(v)
    if (node == null) -1 else node.outDegree
  }
}

object UndirectedGraph {
  def apply[V](): UndirectedGraph[V] = new UndirectedGraph[V]()

  def apply[V](vertices: Iterable[V]): UndirectedGraph[V] = {
    val g = new UndirectedGraph[V]()
    g.initVertices(vertices)
    g
  }

  def apply[V](graph: Graph[V]): UndirectedGraph[V] = {
    val g = new UndirectedGraph[V]()
    g.initFromGraph(graph)
    g
  }
}
