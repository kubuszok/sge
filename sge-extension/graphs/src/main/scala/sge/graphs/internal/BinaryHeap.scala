/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 205
 * Covenant-baseline-methods: BinaryHeap,add,clear,contains,continue,down,equals,h,hashCode,i,index,isEmpty,node,nodes,notEmpty,oldValue,peek,pop,removed,setValue,size,toString,up,value
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package internal

import scala.util.boundary
import scala.util.boundary.break

/** A binary heap that stores Nodes sorted lowest-first by heapValue. Ported from simple-graphs BinaryHeap.java (which was adapted from libGDX BinaryHeap).
  */
class BinaryHeap[V](initialCapacity: Int = 16) {

  var size:          Int            = 0
  private var nodes: Array[Node[V]] = new Array[Node[V]](initialCapacity)

  /** Adds the node to the heap using its current heapValue. */
  def add(node: Node[V]): Node[V] = {
    // Expand if necessary.
    if (size == nodes.length) {
      val newNodes = new Array[Node[V]](size << 1)
      System.arraycopy(nodes, 0, newNodes, 0, size)
      nodes = newNodes
    }
    // Insert at end and bubble up.
    node.heapIndex = size
    nodes(size) = node
    up(size)
    size += 1
    node
  }

  /** Sets the node's value and adds it to the heap. */
  def add(node: Node[V], value: Float): Node[V] = {
    node.heapValue = value
    add(node)
  }

  /** Returns the first (smallest) item in the heap. */
  def peek: Node[V] = nodes(0)

  /** Removes and returns the first (smallest) item in the heap. */
  def pop(): Node[V] = {
    val removed = nodes(0)
    size -= 1
    nodes(0) = nodes(size)
    nodes(size) = null.asInstanceOf[Node[V]] // @nowarn — internal array cleanup
    if (size > 0) down(0)
    removed
  }

  def notEmpty: Boolean = size > 0
  def isEmpty:  Boolean = size == 0

  def clear(): Unit = {
    var i = 0
    while (i < size) {
      nodes(i) = null.asInstanceOf[Node[V]] // @nowarn — internal array cleanup
      i += 1
    }
    size = 0
  }

  /** Changes the value of the node, which should already be in the heap. */
  def setValue(node: Node[V], value: Float): Unit = {
    val oldValue = node.heapValue
    node.heapValue = value
    if (value < oldValue) up(node.heapIndex)
    else down(node.heapIndex)
  }

  private def up(idx: Int): Unit = boundary {
    var index = idx
    val node  = nodes(index)
    val value = node.heapValue
    while (index > 0) {
      val parentIndex = (index - 1) >> 1
      val parent      = nodes(parentIndex)
      if (value < parent.heapValue) {
        nodes(index) = parent
        parent.heapIndex = index
        index = parentIndex
      } else {
        // break
        nodes(index) = node
        node.heapIndex = index
        break(())
      }
    }
    nodes(index) = node
    node.heapIndex = index
  }

  /** Returns true if the heap contains the specified node.
    *
    * @param node
    *   the node to search for (identity comparison)
    */
  def contains(node: Node[V]): Boolean = boundary {
    var i = 0
    while (i < size) {
      if (nodes(i) eq node) {
        break(true)
      }
      i += 1
    }
    false
  }

  private def down(idx: Int): Unit = {
    var index = idx
    val node  = nodes(index)
    val value = node.heapValue

    var continue = true
    while (continue) {
      val leftIndex = 1 + (index << 1)
      if (leftIndex >= size) {
        continue = false
      } else {
        val rightIndex = leftIndex + 1

        // Always has a left child.
        val leftNode  = nodes(leftIndex)
        val leftValue = leftNode.heapValue

        // May have a right child.
        val rightValue = if (rightIndex >= size) Float.MaxValue else nodes(rightIndex).heapValue

        if (leftValue < rightValue) {
          if (leftValue >= value) {
            continue = false
          } else {
            nodes(index) = leftNode
            leftNode.heapIndex = index
            index = leftIndex
          }
        } else {
          if (rightValue >= value) {
            continue = false
          } else {
            val rightNode = nodes(rightIndex)
            nodes(index) = rightNode
            rightNode.heapIndex = index
            index = rightIndex
          }
        }
      }
    }

    nodes(index) = node
    node.heapIndex = index
  }

  override def equals(obj: Any): Boolean =
    if (!obj.isInstanceOf[BinaryHeap[?]]) {
      false
    } else {
      val other = obj.asInstanceOf[BinaryHeap[?]]
      if (other.size != size) {
        false
      } else {
        var i    = 0
        var same = true
        while (i < size && same) {
          if (nodes(i).heapValue != other.nodes(i).heapValue) {
            same = false
          }
          i += 1
        }
        same
      }
    }

  override def hashCode(): Int = {
    var h = 1
    var i = 0
    while (i < size) {
      h = h * 31 + java.lang.Float.floatToIntBits(nodes(i).heapValue)
      i += 1
    }
    h
  }

  override def toString(): String =
    if (size == 0) {
      "[]"
    } else {
      val sb = StringBuilder()
      sb.append('[')
      sb.append(nodes(0).heapValue)
      var i = 1
      while (i < size) {
        sb.append(", ")
        sb.append(nodes(i).heapValue)
        i += 1
      }
      sb.append(']')
      sb.toString()
    }
}
