/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 16
 * Covenant-baseline-methods: SearchProcessor,accept
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package utils

import sge.graphs.algorithms.SearchStep

/** Callback for processing each step during a graph search algorithm. */
trait SearchProcessor[V] {
  def accept(step: SearchStep[V]): Unit
}
