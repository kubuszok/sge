/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

/** Algorithms specific to directed graphs. */
class DirectedGraphAlgorithms[V](graph: DirectedGraph[V]) extends Algorithms[V](graph)
