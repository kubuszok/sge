/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/utils/Bag.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.utils` -> `sge.ecs.utils`
 *   Convention: split packages
 *   Idiom: raw null OK in internal sparse array
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 138
 * Covenant-baseline-methods: Bag,_size,add,clear,contains,data,e,get,getCapacity,grow,i,isEmpty,isIndexWithinBounds,newCapacity,oldData,remove,removeLast,set,size
 * Covenant-source-reference: com/badlogic/ashley/utils/Bag.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs
package utils

import scala.util.boundary
import scala.util.boundary.break

/** Fast sparse collection similar to Array that grows on demand as elements are accessed. It does not preserve order of elements. Inspired by Artemis Bag.
  *
  * This is an internal type not exposed in public API. Raw null is used intentionally for sparse storage gaps.
  */
final private[ecs] class Bag[E >: Null <: AnyRef](initialCapacity: Int = 64) {

  private var data:  Array[AnyRef] = new Array[AnyRef](initialCapacity)
  private var _size: Int           = 0

  /** Removes the element at the specified position. Order of elements is not preserved.
    * @return
    *   element that was removed from the Bag.
    */
  def remove(index: Int): E = {
    val e = data(index).asInstanceOf[E]
    _size -= 1
    data(index) = data(_size) // overwrite item to remove with last element
    data(_size) = null // null last element, so gc can do its work
    e
  }

  /** Removes and returns the last object in the bag, or null if empty. */
  def removeLast(): E =
    if (_size > 0) {
      _size -= 1
      val e = data(_size).asInstanceOf[E]
      data(_size) = null
      e
    } else {
      null
    }

  /** Removes the first occurrence of the specified element. Does not preserve order.
    * @return
    *   true if the element was removed.
    */
  def remove(e: E): Boolean = boundary {
    var i = 0
    while (i < _size) {
      if (e eq data(i).asInstanceOf[AnyRef]) {
        _size -= 1
        data(i) = data(_size) // overwrite item to remove with last element
        data(_size) = null // null last element, so gc can do its work
        break(true)
      }
      i += 1
    }
    false
  }

  /** Check if bag contains this element. Uses reference equality (eq). */
  def contains(e: E): Boolean = boundary {
    var i = 0
    while (i < _size) {
      if (e eq data(i).asInstanceOf[AnyRef]) {
        break(true)
      }
      i += 1
    }
    false
  }

  /** @return the element at the specified position in Bag, or null if index is beyond capacity. */
  def get(index: Int): E =
    if (index >= data.length) null
    else data(index).asInstanceOf[E]

  /** @return the number of elements in this bag. */
  def size: Int = _size

  /** @return the number of elements the bag can hold without growing. */
  def getCapacity: Int = data.length

  /** @return whether or not the index is within the bounds of the collection */
  def isIndexWithinBounds(index: Int): Boolean = index < getCapacity

  /** @return true if this bag contains no elements */
  def isEmpty: Boolean = _size == 0

  /** Adds the specified element to the end of this bag. Grows capacity if needed. */
  def add(e: E): Unit = {
    if (_size == data.length) {
      grow()
    }
    data(_size) = e
    _size += 1
  }

  /** Set element at specified index in the bag. Grows if needed. */
  def set(index: Int, e: E): Unit = {
    if (index >= data.length) {
      grow(index * 2)
    }
    _size = scala.math.max(_size, index + 1)
    data(index) = e
  }

  /** Removes all elements from this bag. */
  def clear(): Unit = {
    var i = 0
    while (i < _size) {
      data(i) = null
      i += 1
    }
    _size = 0
  }

  private def grow(): Unit = {
    val newCapacity = (data.length * 3) / 2 + 1
    grow(newCapacity)
  }

  private def grow(newCapacity: Int): Unit = {
    val oldData = data
    data = new Array[AnyRef](newCapacity)
    System.arraycopy(oldData, 0, data, 0, oldData.length)
  }
}
