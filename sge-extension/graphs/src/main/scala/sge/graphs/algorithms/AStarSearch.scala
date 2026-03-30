/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

import sge.graphs.internal.BinaryHeap
import sge.graphs.utils.{Heuristic, SearchProcessor}

/** A* search / Dijkstra's algorithm for shortest path finding. */
class AStarSearch[V] private[algorithms] (
    id: Int,
    start: Node[V],
    target: Node[V],
    heuristic: Heuristic[V],
    processor: SearchProcessor[V]
) extends Algorithm[V](id) {

  private val step: SearchStep[V] = SearchStep[V]()
  private val heap: BinaryHeap[V] = BinaryHeap[V]()
  private var u: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — current node during search
  private var end: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — result node
  private var path: Path[V] = null.asInstanceOf[Path[V]] // @nowarn — lazily constructed

  // Initialize
  start.resetAlgorithmAttribs(id)
  start.distance = 0f
  heap.add(start)

  override def update(): Boolean = {
    if (isFinished) {
      return true
    }

    u = heap.pop()

    if (!u.processed) {
      if (processor != null && u.index > 0) {
        step.prepare(u)
        processor.accept(step)
        if (step.terminateFlag) {
          heap.clear()
          return true
        }
        if (step.ignoreFlag) {
          return isFinished
        }
      }
      u.processed = true

      if (u eq target) {
        heap.clear()
        end = u
        return true
      }

      val outEdges = u.outEdges
      val edgeIter = outEdges.iterator
      while (edgeIter.hasNext) {
        val e = edgeIter.next()
        val v = e.nodeB
        v.resetAlgorithmAttribs(id)
        if (!v.processed) {
          val newDistance = u.distance + e.weight
          if (newDistance < v.distance) {
            v.distance = newDistance
            v.prev = u
            v.connection = e
            if (heuristic != null && !v.seen) {
              v.estimate = heuristic.getEstimate(v.obj, target.obj)
            }
            if (!v.seen) {
              heap.add(v, v.distance + v.estimate)
            } else {
              heap.setValue(v, v.distance + v.estimate)
            }
            v.index = u.index + 1
            v.seen = true
          }
        }
      }
    }
    isFinished
  }

  override def isFinished: Boolean = heap.size == 0

  def getPath: Path[V] = {
    if (!isFinished) {
      return null.asInstanceOf[Path[V]] // @nowarn — not finished yet
    }
    if (path == null) {
      path = if (end != null) AlgorithmPath[V](end) else Path.emptyPath[V]
    }
    path
  }

  private[algorithms] def getEnd: Node[V] = end
}
