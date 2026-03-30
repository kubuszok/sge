/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package internal

/** A binary heap that stores Nodes sorted lowest-first by heapValue.
  * Ported from simple-graphs BinaryHeap.java (which was adapted from libGDX BinaryHeap).
  */
class BinaryHeap[V](initialCapacity: Int = 16) {

  var size: Int = 0
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
  def isEmpty: Boolean = size == 0

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

  private def up(idx: Int): Unit = {
    var index = idx
    val node = nodes(index)
    val value = node.heapValue
    while (index > 0) {
      val parentIndex = (index - 1) >> 1
      val parent = nodes(parentIndex)
      if (value < parent.heapValue) {
        nodes(index) = parent
        parent.heapIndex = index
        index = parentIndex
      } else {
        // break
        nodes(index) = node
        node.heapIndex = index
        return // performance-critical inner loop; using return for clarity
      }
    }
    nodes(index) = node
    node.heapIndex = index
  }

  private def down(idx: Int): Unit = {
    var index = idx
    val node = nodes(index)
    val value = node.heapValue

    var continue = true
    while (continue) {
      val leftIndex = 1 + (index << 1)
      if (leftIndex >= size) {
        continue = false
      } else {
        val rightIndex = leftIndex + 1

        // Always has a left child.
        val leftNode = nodes(leftIndex)
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
}
