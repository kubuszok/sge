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

import sge.graphs.utils.SearchProcessor

/** Depth-first search algorithm. */
class DepthFirstSearch[V] private[algorithms] (
    id: Int,
    start: Node[V],
    processor: SearchProcessor[V]
) extends Algorithm[V](id) {

  private val step: SearchStep[V] = SearchStep[V]()
  private val queue: ArrayDeque[Node[V]] = ArrayDeque[Node[V]]()

  start.resetAlgorithmAttribs(id)
  queue.append(start)
  start.seen = true

  override def update(): Boolean = {
    if (isFinished) {
      return true
    }

    val v = queue.removeHead()
    if (processor != null) {
      step.prepare(v)
      processor.accept(step)
      if (step.terminateFlag) {
        queue.clear()
        return true
      }
      if (step.ignoreFlag) {
        return isFinished
      }
    }
    val outEdges = v.outEdges
    val edgeIter = outEdges.iterator
    while (edgeIter.hasNext) {
      val e = edgeIter.next()
      val w = e.nodeB
      w.resetAlgorithmAttribs(id)
      if (!w.seen) {
        w.index = v.index + 1
        w.distance = v.distance + e.weight
        w.connection = e
        w.seen = true
        queue.prepend(w)
      }
    }
    isFinished
  }

  override def isFinished: Boolean = queue.isEmpty
}
