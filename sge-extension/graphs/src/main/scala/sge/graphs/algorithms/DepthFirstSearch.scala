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
import scala.util.boundary
import scala.util.boundary.break

import sge.graphs.utils.SearchProcessor

/** Depth-first search algorithm. */
class DepthFirstSearch[V] private[algorithms] (
  id:        Int,
  start:     Node[V],
  processor: SearchProcessor[V]
) extends Algorithm[V](id) {

  private val step:  SearchStep[V]       = SearchStep[V]()
  private val queue: ArrayDeque[Node[V]] = ArrayDeque[Node[V]]()

  start.resetAlgorithmAttribs(id)
  queue.append(start)
  start.seen = true

  override def update(): Boolean = boundary {
    if (isFinished) {
      break(true)
    }

    val v = queue.removeHead()
    if (processor != null) {
      step.prepare(v)
      processor.accept(step)
      if (step.terminateFlag) {
        queue.clear()
        break(true)
      }
      if (step.ignoreFlag) {
        break(isFinished)
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

object DepthFirstSearch {

  /** Perform a recursive depth first search starting from vertex v. */
  private[algorithms] def depthFirstSearch[V](id: Int, v: Node[V], processor: SearchProcessor[V]): Unit = {
    v.resetAlgorithmAttribs(id)
    v.distance = 0
    val step = if (processor != null) SearchStep[V]() else null.asInstanceOf[SearchStep[V]] // @nowarn — null when no processor, matching original pattern
    recursiveDepthFirstSearch(id, v, processor, 0, step)
  }

  private def recursiveDepthFirstSearch[V](
    id:        Int,
    v:         Node[V],
    processor: SearchProcessor[V],
    depth:     Int,
    step:      SearchStep[V]
  ): Boolean = boundary {
    if (processor != null) {
      step.prepare(v)
      processor.accept(step)
      if (step.terminateFlag) break(true)
      if (step.ignoreFlag) break(false)
    }
    v.processed = true
    val n = v.outEdges.size
    var i = 0
    while (i < n) {
      val e = v.outEdges.get(i)
      val w = e.nodeB
      w.resetAlgorithmAttribs(id)
      if (!w.processed) {
        w.index = depth + 1
        w.distance = v.distance + e.weight
        w.connection = e
        if (recursiveDepthFirstSearch(id, w, processor, depth + 1, step)) {
          break(true)
        }
      }
      i += 1
    }
    false
  }
}
