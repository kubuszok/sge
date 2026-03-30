/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

/** Builder utility for constructing complete graphs. */
object GraphBuilder {

  /** Build a complete graph from the vertices already in the graph. */
  def buildCompleteGraph[V](graph: Graph[V]): Unit = {
    val nodesA = graph.nodeMap.nodeIterator
    while (nodesA.hasNext) {
      val a = nodesA.next()
      val nodesB = graph.nodeMap.nodeIterator
      while (nodesB.hasNext) {
        val b = nodesB.next()
        if (!(a eq b)) {
          val e = a.getEdge(b)
          if (e == null) {
            graph.addConnection(a, b)
          }
          if (graph.isDirected) {
            val e2 = b.getEdge(a)
            if (e2 == null) {
              graph.addConnection(b, a)
            }
          }
        }
      }
    }
  }
}
