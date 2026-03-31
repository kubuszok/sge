/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import sge.graphs.utils.WeightFunction

/** Abstract edge representation with two endpoints and a weight. */
abstract class Edge[V] {

  def a:                        V
  def b:                        V
  def hasEndpoints(u: V, v: V): Boolean

  def hasEndpoint(u: V): Boolean = a == u || b == u

  def weight:                                       Float
  def weight_=(w:               Float):             Unit
  def weightFunction:                               WeightFunction[V]
  def setWeight(weightFunction: WeightFunction[V]): Unit

  private[graphs] def internalNodeA:                                                  Node[V]
  private[graphs] def internalNodeB:                                                  Node[V]
  private[graphs] def set(a: Node[V], b: Node[V], weightFunction: WeightFunction[V]): Unit
}
