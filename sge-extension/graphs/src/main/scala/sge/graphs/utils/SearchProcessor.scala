/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package utils

import sge.graphs.algorithms.SearchStep

/** Callback for processing each step during a graph search algorithm. */
trait SearchProcessor[V] {
  def accept(step: SearchStep[V]): Unit
}
