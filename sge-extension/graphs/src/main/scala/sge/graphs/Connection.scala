/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import sge.graphs.utils.WeightFunction

/** Weighted edge (connection) in the graph with internal node references. */
abstract class Connection[V] extends Edge[V] {

  // ================================================================================
  // Fields
  // ================================================================================

  private[graphs] var nodeA:    Node[V]           = null.asInstanceOf[Node[V]] // @nowarn — set via set() before use
  private[graphs] var nodeB:    Node[V]           = null.asInstanceOf[Node[V]] // @nowarn — set via set() before use
  private[graphs] var weightFn: WeightFunction[V] = null.asInstanceOf[WeightFunction[V]] // @nowarn — set via set() before use

  // ================================================================================
  // Internal methods
  // ================================================================================

  override private[graphs] def set(a: Node[V], b: Node[V], weightFunction: WeightFunction[V]): Unit = {
    nodeA = a
    nodeB = b
    setWeight(weightFunction)
  }

  override private[graphs] def internalNodeA: Node[V] = nodeA
  override private[graphs] def internalNodeB: Node[V] = nodeB

  // ================================================================================
  // Public methods
  // ================================================================================

  override def a: V = nodeA.obj
  override def b: V = nodeB.obj

  override def weight: Float = weightFn.getWeight(a, b)

  override def weight_=(w: Float): Unit = {
    val fixed = w
    setWeight(new WeightFunction[V] { def getWeight(a: V, b: V): Float = fixed })
  }

  override def setWeight(weightFunction: WeightFunction[V]): Unit =
    weightFn = weightFunction

  override def weightFunction: WeightFunction[V] = weightFn
}

/** Directed connection: equality based on ordered (a, b) pair. */
class DirectedConnection[V] extends Connection[V] {

  override def hasEndpoints(u: V, v: V): Boolean = a == u && b == v

  override def equals(o: Any): Boolean =
    if (this eq o.asInstanceOf[AnyRef]) {
      true
    } else {
      o match {
        case edge: Connection[?] =>
          nodeA.equals(edge.nodeA) && nodeB.equals(edge.nodeB)
        case _ => false
      }
    }

  override def hashCode(): Int =
    (nodeA.hashCode().toLong * 0xc13fa9a902a6328fL + (nodeB.hashCode().toLong * 0x91e10da5c79e7b1dL) >>> 32).toInt

  override def toString: String = s"{$nodeA -> $nodeB, $weight}"
}

/** Undirected connection: equality ignores order of (a, b). Has a linked twin for the reverse direction. */
class UndirectedConnection[V] extends Connection[V] {

  private var linked: UndirectedConnection[V] = null.asInstanceOf[UndirectedConnection[V]] // @nowarn — set via link() before use

  def link(other: UndirectedConnection[V]): Unit =
    linked = other

  override def hasEndpoints(u: V, v: V): Boolean = hasEndpoint(u) && hasEndpoint(v)

  override def equals(o: Any): Boolean =
    if (this eq o.asInstanceOf[AnyRef]) {
      true
    } else {
      o match {
        case edge: Connection[?] =>
          (nodeA.equals(edge.nodeA) && nodeB.equals(edge.nodeB)) ||
          (nodeA.equals(edge.nodeB) && nodeB.equals(edge.nodeA))
        case _ => false
      }
    }

  override def setWeight(weightFunction: WeightFunction[V]): Unit = {
    weightFn = weightFunction
    if (linked != null) linked.weightFn = weightFn
  }

  override def hashCode(): Int = nodeA.hashCode() ^ (nodeB.hashCode() >>> 32)

  override def toString: String = s"{$nodeA <> $nodeB, $weight}"
}
