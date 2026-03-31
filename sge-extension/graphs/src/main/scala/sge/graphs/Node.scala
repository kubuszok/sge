/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import scala.collection.mutable.LinkedHashMap
import sge.graphs.internal.InternalArray

/** Vertex node in the graph, wrapping a user object of type V. Also carries mutable algorithm state (distance, prev, etc.) for search algorithms.
  */
class Node[V](
  val obj:        V,
  trackInEdges:   Boolean,
  val objectHash: Int
) {

  // ================================================================================
  // Graph structure related members
  // ================================================================================

  val idHash: Int = System.identityHashCode(this)

  val neighbours: LinkedHashMap[Node[V], Connection[V]] = LinkedHashMap.empty
  var outEdges:   InternalArray[Connection[V]]          = InternalArray[Connection[V]]()
  var inEdges:    InternalArray[Connection[V]]          =
    if (trackInEdges) InternalArray[Connection[V]]() else null.asInstanceOf[InternalArray[Connection[V]]] // @nowarn — null used for optional in-edges tracking (matching original design)

  // ================================================================================
  // Node map fields
  // ================================================================================

  var mapHash:      Int     = 0
  var nextInOrder:  Node[V] = null.asInstanceOf[Node[V]] // @nowarn — linked list pointer
  var prevInOrder:  Node[V] = null.asInstanceOf[Node[V]] // @nowarn — linked list pointer
  var nextInBucket: Node[V] = null.asInstanceOf[Node[V]] // @nowarn — hash bucket chain pointer

  // ================================================================================
  // Internal methods
  // ================================================================================

  def getEdge(v: Node[V]): Connection[V] = neighbours.getOrElse(v, null.asInstanceOf[Connection[V]]) // @nowarn — null return matches original API contract

  def addEdge(edge: Connection[V]): Unit = {
    val to = edge.nodeB
    neighbours.put(to, edge)
    outEdges.add(edge)
    if (to.inEdges != null) to.inEdges.add(edge)
  }

  def removeEdge(v: Node[V]): Connection[V] =
    neighbours.remove(v) match {
      case Some(edge) =>
        outEdges.removeItem(edge)
        if (v.inEdges != null) v.inEdges.removeItem(edge)
        edge
      case None =>
        null.asInstanceOf[Connection[V]] // @nowarn — null return matches original API contract
    }

  def disconnect(): Unit = {
    neighbours.clear()
    outEdges.clear()
    if (inEdges != null) inEdges.clear()
  }

  // ================================================================================
  // Public Methods
  // ================================================================================

  def connections: InternalArray[Connection[V]] = outEdges

  def inDegree: Int = if (inEdges == null) outDegree else inEdges.size

  def outDegree: Int = outEdges.size

  // ================================================================================
  // Algorithm fields and methods
  // ================================================================================

  // util fields for algorithms, don't store data in them
  var processed:         Boolean       = false
  var seen:              Boolean       = false
  var distance:          Float         = Float.MaxValue
  var estimate:          Float         = 0f
  var prev:              Node[V]       = null.asInstanceOf[Node[V]] // @nowarn — algorithm state reset to null each run
  var connection:        Connection[V] = null.asInstanceOf[Connection[V]] // @nowarn — algorithm state reset to null each run
  var index:             Int           = 0
  private var lastRunID: Int           = -1

  def resetAlgorithmAttribs(runID: Int): Boolean =
    if (runID == lastRunID) {
      false
    } else {
      processed = false
      prev = null.asInstanceOf[Node[V]] // @nowarn — algorithm reset
      connection = null.asInstanceOf[Connection[V]] // @nowarn — algorithm reset
      distance = Float.MaxValue
      estimate = 0f
      index = 0
      seen = false
      lastRunID = runID
      true
    }

  // ================================================================================
  // Heap fields
  // ================================================================================

  var heapIndex: Int   = 0
  var heapValue: Float = 0f

  // ================================================================================
  // Misc
  // ================================================================================

  override def equals(o: Any): Boolean = o.asInstanceOf[AnyRef] eq this

  override def hashCode(): Int = idHash

  override def toString: String = s"[$obj]"
}
