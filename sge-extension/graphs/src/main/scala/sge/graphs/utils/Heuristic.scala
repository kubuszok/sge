/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package utils

/** A function which estimates the distance of a shortest path between two vertices. A heuristic h should be admissible, that is, for any two vertices x and y, h(x,y) <= d(x,y), where d(x,y) is the
  * actual distance of a shortest path from x to y.
  */
trait Heuristic[V] {

  /** @return
    *   an estimation of the distance from u to v. This value should always be at most the actual distance of a shortest path from x to y.
    */
  def getEstimate(u: V, v: V): Float
}
