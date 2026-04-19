/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 66
 * Covenant-baseline-methods: BreadthFirstSearch,edgeIter,isFinished,outEdges,queue,step,update,v
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package algorithms

import scala.collection.mutable.ArrayDeque
import scala.util.boundary
import scala.util.boundary.break

import sge.graphs.utils.SearchProcessor

/** Breadth-first search algorithm. */
class BreadthFirstSearch[V] private[algorithms] (
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
        queue.append(w)
      }
    }
    isFinished
  }

  override def isFinished: Boolean = queue.isEmpty
}
