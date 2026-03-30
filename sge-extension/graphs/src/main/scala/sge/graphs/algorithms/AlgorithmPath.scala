/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

/** Path constructed by backtracking through algorithm node state. */
private[algorithms] class AlgorithmPath[V] private (nodeCount: Int) extends Path[V](nodeCount, true) {

  private def setByBacktracking(node: Node[V]): Unit = {
    val count = node.index + 1
    if (items.length < count) strictResize(count)

    var v: Node[V] = node
    while (v != null) {
      set(v.index, v.obj)
      v = v.prev
    }

    length = node.distance
  }
}

private[algorithms] object AlgorithmPath {
  def apply[V](node: Node[V]): AlgorithmPath[V] = {
    val path = new AlgorithmPath[V](node.index + 1)
    path.setByBacktracking(node)
    path
  }
}
