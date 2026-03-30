/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

/** An object representing a "step" in a search algorithm. Usually this corresponds to visiting or processing a vertex. */
class SearchStep[V] {

  private[algorithms] var terminateFlag: Boolean = false
  private[algorithms] var ignoreFlag: Boolean = false
  private var node: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — set via prepare() before use
  private var _count: Int = -1

  private[algorithms] def prepare(node: Node[V]): Unit = {
    this.node = node
    terminateFlag = false
    ignoreFlag = false
    _count += 1
  }

  /** Immediately terminate the search. */
  def terminate(): Unit = {
    terminateFlag = true
  }

  /** Ignore the current vertex, and do not check its neighbours in this step. */
  def ignore(): Unit = {
    ignoreFlag = true
  }

  /** @return the vertex being currently processed. */
  def vertex: V = node.obj

  /** @return the edge from which the current vertex was found. */
  def edge: Edge[V] = node.connection

  /** @return the vertex from which the current vertex was found. */
  def previous: V = node.connection.a

  /** @return the number of vertices traversed in order to find the current vertex, not including the initial vertex. */
  def depth: Int = node.index

  /** @return the sum of edge weights on a path from the initial vertex to the current, along the search path taken. */
  def distance: Float = node.distance

  /** @return the number of processing steps so far. */
  def count: Int = _count

  /** Reconstruct the path from the initial vertex to the current vertex that the search algorithm took. */
  def createPath(): Path[V] = AlgorithmPath[V](node)
}
