/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 14
 * Covenant-baseline-methods: WeightFunction,getWeight
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package utils

/** A function that returns the weight of an edge between two vertices. */
trait WeightFunction[V] {
  def getWeight(a: V, b: V): Float
}
