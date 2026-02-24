/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/BinaryHeap.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.util.Arrays
import scala.collection.mutable.ArrayBuffer
import scala.util.boundary, boundary.break

/** A binary heap that stores nodes which each have a float value and are sorted either lowest first or highest first. The Node class can be extended to store additional information.
  * @author
  *   Nathan Sweet
  */
class BinaryHeap[T <: BinaryHeap.Node](capacity: Int = 16, val isMaxHeap: Boolean = false) {
  var size: Int = 0

  private var nodes: Array[BinaryHeap.Node] = new Array[BinaryHeap.Node](capacity)

  /** Adds the node to the heap using its current value. The node should not already be in the heap.
    * @return
    *   The specified node.
    */
  def add(node: T): T = {
    // Expand if necessary.
    if (size == nodes.length) {
      val newNodes = new Array[BinaryHeap.Node](size << 1)
      System.arraycopy(nodes, 0, newNodes, 0, size)
      nodes = newNodes
    }
    // Insert at end and bubble up.
    node.index = size
    nodes(size) = node
    up(size)
    size += 1
    node
  }

  /** Sets the node's value and adds it to the heap. The node should not already be in the heap.
    * @return
    *   The specified node.
    */
  def add(node: T, value: Float): T = {
    node.value = value
    add(node)
  }

  /** Returns true if the heap contains the specified node.
    * @param identity
    *   If true, == comparison will be used. If false, .equals() comparison will be used.
    */
  def contains(node: T, identity: Boolean): Boolean = {
    if (node == null) throw new IllegalArgumentException("node cannot be null.")
    if (identity) {
      nodes.take(size).exists(_ == node)
    } else {
      nodes.take(size).exists(_.equals(node))
    }
  }

  /** Returns the first item in the heap. This is the item with the lowest value (or highest value if this heap is configured as a max heap).
    */
  def peek(): T = {
    if (size == 0) throw new IllegalStateException("The heap is empty.")
    nodes(0).asInstanceOf[T]
  }

  /** Removes the first item in the heap and returns it. This is the item with the lowest value (or highest value if this heap is configured as a max heap).
    */
  def pop(): T = {
    val removed = nodes(0)
    size -= 1
    if (size > 0) {
      nodes(0) = nodes(size)
      nodes(size) = null
      down(0)
    } else {
      nodes(0) = null
    }
    removed.asInstanceOf[T]
  }

  /** @return The specified node. */
  def remove(node: T): T = {
    size -= 1
    if (size > 0) {
      val moved = nodes(size)
      nodes(size) = null
      nodes(node.index) = moved
      moved.index = node.index
      if (moved.value < node.value ^ isMaxHeap) {
        up(node.index)
      } else {
        down(node.index)
      }
    } else {
      nodes(0) = null
    }
    node
  }

  /** Returns true if the heap has one or more items. */
  def notEmpty(): Boolean = size > 0

  /** Returns true if the heap is empty. */
  def isEmpty: Boolean = size == 0

  def clear(): Unit = {
    Arrays.fill(nodes.asInstanceOf[Array[Object]], 0, size, null)
    size = 0
  }

  /** Changes the value of the node, which should already be in the heap. */
  def setValue(node: T, value: Float): Unit = {
    val oldValue = node.value
    node.value = value
    if (value < oldValue ^ isMaxHeap) {
      up(node.index)
    } else {
      down(node.index)
    }
  }

  private def up(index: Int): Unit = {
    val nodes        = this.nodes
    val node         = nodes(index)
    val value        = node.value
    var currentIndex = index

    boundary {
      while (currentIndex > 0) {
        val parentIndex = (currentIndex - 1) >> 1
        val parent      = nodes(parentIndex)
        if (value < parent.value ^ isMaxHeap) {
          nodes(currentIndex) = parent
          parent.index = currentIndex
          currentIndex = parentIndex
        } else {
          break()
        }
      }
    }

    nodes(currentIndex) = node
    node.index = currentIndex
  }

  private def down(index: Int): Unit = {
    val nodes = this.nodes
    val size  = this.size

    val node         = nodes(index)
    val value        = node.value
    var currentIndex = index

    boundary {
      while (true) {
        val leftIndex = 1 + (currentIndex << 1)
        if (leftIndex >= size) break()

        val rightIndex = leftIndex + 1

        // Always has a left child.
        val leftNode  = nodes(leftIndex)
        val leftValue = leftNode.value

        // May have a right child.
        val (rightNode, rightValue) = if (rightIndex >= size) {
          (null, if (isMaxHeap) -Float.MaxValue else Float.MaxValue)
        } else {
          val rNode = nodes(rightIndex)
          (rNode, rNode.value)
        }

        // The smallest of the three values is the parent.
        if (leftValue < rightValue ^ isMaxHeap) {
          if (leftValue == value || (leftValue > value ^ isMaxHeap)) break()
          nodes(currentIndex) = leftNode
          leftNode.index = currentIndex
          currentIndex = leftIndex
        } else {
          if (rightValue == value || (rightValue > value ^ isMaxHeap)) break()
          nodes(currentIndex) = rightNode
          if (rightNode != null) rightNode.index = currentIndex
          currentIndex = rightIndex
        }
      }
    }

    nodes(currentIndex) = node
    node.index = currentIndex
  }

  override def equals(obj: Any): Boolean =
    obj match {
      case other: BinaryHeap[_] =>
        if (other.size != size) return false
        val nodes1 = this.nodes
        val nodes2 = other.nodes
        boundary {
          for (i <- 0 until size)
            if (nodes1(i).value != nodes2(i).value) break(false)
          true
        }
      case _ => false
    }

  override def hashCode(): Int = {
    var h     = 1
    val nodes = this.nodes
    for (i <- 0 until size)
      h = h * 31 + java.lang.Float.floatToIntBits(nodes(i).value)
    h
  }

  override def toString: String = {
    if (size == 0) return "[]"
    val nodes  = this.nodes
    val buffer = new StringBuilder(32)
    buffer.append('[')
    buffer.append(nodes(0).value)
    for (i <- 1 until size) {
      buffer.append(", ")
      buffer.append(nodes(i).value)
    }
    buffer.append(']')
    buffer.toString
  }
}

object BinaryHeap {

  /** A binary heap node.
    * @author
    *   Nathan Sweet
    * @param value
    *   The initial value for the node. To change the value, use BinaryHeap#add(Node, float) if the node is not in the heap, or BinaryHeap#setValue(Node, float) if the node is in the heap.
    */
  class Node(var value: Float) {
    var index: Int = scala.compiletime.uninitialized

    def getValue: Float = value

    override def toString: String = value.toString
  }
}
