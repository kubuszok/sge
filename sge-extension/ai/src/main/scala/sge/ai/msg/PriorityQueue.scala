/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/msg/PriorityQueue.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.msg` -> `sge.ai.msg`
 *   Convention: split packages; `null` -> `Nullable`; `ObjectSet` -> `scala.collection.mutable.HashSet`
 *   Idiom: `getUniqueness`/`setUniqueness` -> `var uniqueness`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package msg

import sge.utils.Nullable

/** An unbounded priority queue based on a priority heap. The elements of the priority queue are ordered according to their natural ordering. A priority queue does not permit empty elements.
  *
  * The queue can be set to accept or reject the insertion of non unique elements through the [[uniqueness]] flag. Uniqueness is disabled by default.
  *
  * The head of this queue is the least element with respect to the specified ordering. If multiple elements are tied for least value (provided that uniqueness is set to false), the head is one of
  * those elements -- ties are broken arbitrarily.
  *
  * @tparam E
  *   the type of comparable elements held in this queue
  * @author
  *   davebaol (original implementation)
  */
class PriorityQueue[E <: Comparable[E]](initialCapacity: Int = PriorityQueue.DEFAULT_INITIAL_CAPACITY) {

  /** Priority queue represented as a balanced binary heap: the two children of queue(n) are queue(2*n+1) and queue(2*(n+1)). The priority queue is ordered by the elements' natural ordering: For each
    * node n in the heap and each descendant d of n, n <= d. The element with the lowest value is in queue(0), assuming the queue is nonempty.
    */
  private var queue: Array[AnyRef] = new Array[AnyRef](initialCapacity)

  /** A set used to check elements' uniqueness (if enabled). */
  private val set: scala.collection.mutable.HashSet[E] = scala.collection.mutable.HashSet.empty[E]

  /** A flag indicating whether elements inserted into the queue must be unique. */
  var uniqueness: Boolean = false

  /** The number of elements in the priority queue. */
  private var _size: Int = 0

  /** Returns the number of elements in this queue. */
  def size: Int = _size

  /** Inserts the specified element into this priority queue. If `uniqueness` is enabled and this priority queue already contains the element, the call leaves the queue unchanged and returns false.
    *
    * @return
    *   true if the element was added to this queue, else false
    */
  def add(e: E): Boolean =
    if (uniqueness && !set.add(e)) false
    else {
      val i = _size
      if (i >= queue.length) growToSize(i + 1)
      _size = i + 1
      if (i == 0)
        queue(0) = e.asInstanceOf[AnyRef]
      else
        siftUp(i, e)
      true
    }

  /** Retrieves, but does not remove, the head of this queue. If this queue is empty `Nullable.empty` is returned.
    *
    * @return
    *   the head of this queue
    */
  def peek(): Nullable[E] =
    if (_size == 0) Nullable.empty else Nullable(queue(0).asInstanceOf[E])

  /** Retrieves the element at the specified index. If such an element doesn't exist `Nullable.empty` is returned.
    *
    * Iterating the queue by index is not guaranteed to traverse the elements in any particular order.
    *
    * @return
    *   the element at the specified index in this queue.
    */
  def get(index: Int): Nullable[E] =
    if (index >= _size) Nullable.empty else Nullable(queue(index).asInstanceOf[E])

  /** Removes all of the elements from this priority queue. The queue will be empty after this call returns. */
  def clear(): Unit = {
    var i = 0
    while (i < _size) {
      queue(i) = null
      i += 1
    }
    _size = 0
    set.clear()
  }

  /** Retrieves and removes the head of this queue, or returns `Nullable.empty` if this queue is empty.
    *
    * @return
    *   the head of this queue, or `Nullable.empty` if this queue is empty.
    */
  def poll(): Nullable[E] =
    if (_size == 0) Nullable.empty
    else {
      val s = _size - 1
      _size = s
      val result = queue(0).asInstanceOf[E]
      val x      = queue(s).asInstanceOf[E]
      queue(s) = null
      if (s != 0) siftDown(0, x)
      if (uniqueness) set.remove(result)
      Nullable(result)
    }

  /** Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it is greater than or equal to its parent, or is the root.
    */
  private def siftUp(k: Int, x: E): Unit = {
    var pos = k
    while (pos > 0) {
      val parent = (pos - 1) >>> 1
      val e      = queue(parent).asInstanceOf[E]
      if (x.compareTo(e) >= 0) {
        queue(pos) = x.asInstanceOf[AnyRef]
        pos = -1 // signal done
      } else {
        queue(pos) = e.asInstanceOf[AnyRef]
        pos = parent
      }
    }
    if (pos >= 0) queue(pos) = x.asInstanceOf[AnyRef]
  }

  /** Inserts item x at position k, maintaining heap invariant by demoting x down the tree repeatedly until it is less than or equal to its children or is a leaf.
    */
  private def siftDown(k: Int, x: E): Unit = {
    val half = _size >>> 1
    var pos  = k
    while (pos < half) {
      var child = (pos << 1) + 1
      var c     = queue(child).asInstanceOf[E]
      val right = child + 1
      if (right < _size && c.compareTo(queue(right).asInstanceOf[E]) > 0) {
        child = right
        c = queue(right).asInstanceOf[E]
      }
      if (x.compareTo(c) <= 0) {
        queue(pos) = x.asInstanceOf[AnyRef]
        pos = half // signal done
      } else {
        queue(pos) = c.asInstanceOf[AnyRef]
        pos = child
      }
    }
    if (pos < half) queue(pos) = x.asInstanceOf[AnyRef]
    else if (pos == k || pos != half) ()
    else queue(pos) = x.asInstanceOf[AnyRef] // loop didn't break early; write final position
  }

  /** Increases the capacity of the array. */
  private def growToSize(minCapacity: Int): Unit = {
    if (minCapacity < 0) throw new RuntimeException("Capacity upper limit exceeded.")
    val oldCapacity = queue.length
    var newCapacity =
      if (oldCapacity < 64) ((oldCapacity + 1) * PriorityQueue.CAPACITY_RATIO_HI).toInt
      else (oldCapacity * PriorityQueue.CAPACITY_RATIO_LOW).toInt
    if (newCapacity < 0) newCapacity = Int.MaxValue
    if (newCapacity < minCapacity) newCapacity = minCapacity
    val newQueue = new Array[AnyRef](newCapacity)
    System.arraycopy(queue, 0, newQueue, 0, _size)
    queue = newQueue
  }
}

object PriorityQueue {
  private val DEFAULT_INITIAL_CAPACITY: Int    = 11
  private val CAPACITY_RATIO_LOW:       Double = 1.5
  private val CAPACITY_RATIO_HI:        Double = 2.0
}
