/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import sge.graphs.algorithms.DirectedGraphAlgorithms

/** Directed graph implementation. */
class DirectedGraph[V] extends Graph[V] {

  private val _algorithms: DirectedGraphAlgorithms[V] = DirectedGraphAlgorithms[V](this)

  override private[graphs] def obtainEdge(): Connection[V] = DirectedConnection[V]()

  override def createNew(): DirectedGraph[V] = DirectedGraph[V]()

  override def algorithms: DirectedGraphAlgorithms[V] = _algorithms

  /** @return the out degree of this vertex, or -1 if it is not in the graph */
  def getOutDegree(v: V): Int = {
    val node = getNode(v)
    if (node == null) -1 else node.outDegree
  }

  /** @return the in degree of this vertex, or -1 if it is not in the graph */
  def getInDegree(v: V): Int = {
    val node = getNode(v)
    if (node == null) -1 else node.inDegree
  }

  /** Get a collection containing all the edges which have v as a head. That is, for every edge e in the collection, e = (u, v) for some vertex u.
    */
  def getInEdges(v: V): Iterable[Edge[V]] = {
    val node = getNode(v)
    if (node == null) null.asInstanceOf[Iterable[Edge[V]]] // @nowarn — null return matches original API
    else node.inEdges.asInstanceOf[Iterable[Edge[V]]]
  }

  /** Sort the vertices of this graph in topological order. */
  def topologicalSort(): Boolean = nodeMap.topologicalSort()
}

object DirectedGraph {
  def apply[V](): DirectedGraph[V] = new DirectedGraph[V]()

  def apply[V](vertices: Iterable[V]): DirectedGraph[V] = {
    val g = new DirectedGraph[V]()
    g.initVertices(vertices)
    g
  }

  def apply[V](graph: Graph[V]): DirectedGraph[V] = {
    val g = new DirectedGraph[V]()
    g.initFromGraph(graph)
    g
  }
}
