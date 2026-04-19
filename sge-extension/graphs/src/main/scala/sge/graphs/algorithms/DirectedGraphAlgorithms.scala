/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 12
 * Covenant-baseline-methods: DirectedGraphAlgorithms
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package algorithms

/** Algorithms specific to directed graphs. */
class DirectedGraphAlgorithms[V](graph: DirectedGraph[V]) extends Algorithms[V](graph)
