/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package utils

/** A function that returns the weight of an edge between two vertices. */
trait WeightFunction[V] {
  def getWeight(a: V, b: V): Float
}
