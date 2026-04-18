/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package internal

import scala.util.boundary
import scala.util.boundary.break

/** Resizable array, ported from simple-graphs Array.java. Used internally for edge lists and path storage.
  */
class InternalArray[T](initialCapacity: Int = 8, resizeToCapacity: Boolean = false) extends Iterable[T] {

  private var _items: Array[Any] = new Array[Any](initialCapacity)
  private var _size:  Int        = if (resizeToCapacity) initialCapacity else 0

  def items:         Array[Any] = _items
  override def size: Int        = _size

  def add(item: T): Boolean = {
    ensureCapacity(_size + 1)
    _items(_size) = item
    _size += 1
    true
  }

  def set(index: Int, item: T): T = {
    val oldVal = _items(index).asInstanceOf[T]
    _items(index) = item
    oldVal
  }

  def get(index: Int): T = _items(index).asInstanceOf[T]

  def indexOf(item: Any): Int = boundary {
    if (item == null) {
      throw IllegalArgumentException("No item can be null")
    }
    var i = _size - 1
    while (i >= 0) {
      if (item.equals(_items(i))) {
        break(i)
      }
      i -= 1
    }
    -1
  }

  def removeItem(item: Any): Boolean =
    if (item == null) {
      false
    } else {
      val idx = indexOf(item)
      if (idx >= 0) {
        removeAt(idx)
        true
      } else {
        false
      }
    }

  def removeAt(index: Int): T = {
    if (index < 0 || index >= _size) {
      throw IndexOutOfBoundsException()
    }
    val item = _items(index).asInstanceOf[T]
    _size -= 1
    var i = index
    while (i < _size) {
      _items(i) = _items(i + 1)
      i += 1
    }
    _items(_size) = null
    item
  }

  def addAll(collection: Iterable[T]): Boolean = {
    val arr    = collection.toArray[Any]
    val numNew = arr.length
    if (numNew == 0) {
      false
    } else {
      ensureCapacity(_size + numNew)
      System.arraycopy(arr, 0, _items, _size, numNew)
      _size += numNew
      true
    }
  }

  /** Returns a new array containing the elements of this array (up to size). */
  def toArray: Array[Any] = {
    val result = new Array[Any](_size)
    System.arraycopy(_items, 0, result, 0, _size)
    result
  }

  /** Ensures the backing array has at least the given capacity, growing if necessary. */
  private[internal] def resize(newSize: Int): Unit =
    if (newSize > _items.length) {
      strictResize(math.max(2 * _items.length, newSize))
    }

  private def ensureCapacity(newSize: Int): Unit =
    if (newSize > _items.length) {
      strictResize(math.max(2 * _items.length, newSize))
    }

  protected def strictResize(newSize: Int): Unit = {
    val newItems = new Array[Any](newSize)
    System.arraycopy(_items, 0, newItems, 0, _items.length.min(newSize))
    _items = newItems
  }

  def clear(): Unit = {
    java.util.Arrays.fill(_items.asInstanceOf[Array[AnyRef]], null)
    _size = 0
  }

  override def isEmpty: Boolean = _size == 0

  def containsAll(collection: Iterable[?]): Boolean = boundary {
    val iter = collection.iterator
    while (iter.hasNext)
      if (!contains(iter.next())) {
        break(false)
      }
    true
  }

  def contains(o: Any): Boolean = o != null && indexOf(o) >= 0

  override def iterator: Iterator[T] = new Iterator[T] {
    private var cursor = 0
    def hasNext: Boolean = cursor < _size
    def next():  T       = {
      if (cursor >= _size) {
        throw java.util.NoSuchElementException()
      }
      val item = _items(cursor).asInstanceOf[T]
      cursor += 1
      item
    }
  }

  override def knownSize: Int = _size
}
