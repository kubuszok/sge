/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/CircularBuffer.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`
 *   Idiom: `ArrayReflection.newInstance` -> `new Array[AnyRef]` cast; `null` -> `Nullable`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package utils

import sge.utils.Nullable

/** A circular buffer, possibly resizable.
  *
  * @author
  *   davebaol (original implementation)
  */
class CircularBuffer[T <: AnyRef](initialCapacity: Int = 16, var resizable: Boolean = true) {

  private var items: Array[AnyRef] = new Array[AnyRef](initialCapacity)
  private var head:  Int           = 0
  private var tail:  Int           = 0
  private var _size: Int           = 0

  /** Adds the given item to the tail of this circular buffer.
    * @param item
    *   the item to add
    * @return
    *   `true` if the item has been successfully added to this circular buffer; `false` otherwise.
    */
  def store(item: T): Boolean =
    if (_size == items.length && !resizable) {
      false
    } else {
      if (_size == items.length) {
        // Resize this queue
        resize(Math.max(8, (items.length * 1.75f).toInt))
      }
      _size += 1
      items(tail) = item
      tail += 1
      if (tail == items.length) tail = 0
      true
    }

  /** Removes and returns the item at the head of this circular buffer (if any).
    * @return
    *   the item just removed or `Nullable.empty` if this circular buffer is empty.
    */
  def read(): Nullable[T] =
    if (_size > 0) {
      _size -= 1
      val item = items(head).asInstanceOf[T]
      items(head) = null // Avoid keeping useless references
      head += 1
      if (head == items.length) head = 0
      Nullable(item)
    } else {
      Nullable.empty[T]
    }

  /** Removes all items from this circular buffer. */
  def clear(): Unit = {
    if (tail > head) {
      var i = head
      while (i < tail) {
        items(i) = null
        i += 1
      }
    } else if (_size > 0) { // NOTE: when head == tail the buffer can be empty or full
      var i = head
      while (i < items.length) {
        items(i) = null
        i += 1
      }
      i = 0
      while (i < tail) {
        items(i) = null
        i += 1
      }
    }
    head = 0
    tail = 0
    _size = 0
  }

  /** Returns `true` if this circular buffer is empty; `false` otherwise. */
  def isEmpty: Boolean = _size == 0

  /** Returns `true` if this circular buffer contains as many items as its capacity; `false` otherwise. */
  def isFull: Boolean = _size == items.length

  /** Returns the number of elements in this circular buffer. */
  def size: Int = _size

  /** Increases the size of the backing array (if necessary) to accommodate the specified number of additional items. Useful before adding many items to avoid multiple backing array resizes.
    * @param additionalCapacity
    *   the number of additional items
    */
  def ensureCapacity(additionalCapacity: Int): Unit = {
    val newCapacity = _size + additionalCapacity
    if (items.length < newCapacity) resize(newCapacity)
  }

  /** Creates a new backing array with the specified capacity containing the current items.
    * @param newCapacity
    *   the new capacity
    */
  protected def resize(newCapacity: Int): Unit = {
    val newItems = new Array[AnyRef](newCapacity)
    if (tail > head) {
      System.arraycopy(items, head, newItems, 0, _size)
    } else if (_size > 0) { // NOTE: when head == tail the buffer can be empty or full
      System.arraycopy(items, head, newItems, 0, items.length - head)
      System.arraycopy(items, 0, newItems, items.length - head, tail)
    }
    head = 0
    tail = _size
    items = newItems
  }
}
