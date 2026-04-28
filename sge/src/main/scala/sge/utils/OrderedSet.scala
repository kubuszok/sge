/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/OrderedSet.java
 * Original authors: Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Array<T> items` -> `DynamicArray[A] _items`; accessor `orderedItems()`
 *   Convention: delegates to `ObjectSet` + `DynamicArray` internally; `final class`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 275
 * Covenant-baseline-methods: OrderedSet,_items,add,addAll,alter,alterIndex,apply,clear,contains,create,ensureCapacity,equals,first,foreach,from,get,h,hashCode,i,isEmpty,items,key,loadFactor,mk,n,nonEmpty,orderedItems,remove,removeIndex,set,shrink,size,summonMkArray,toArray,toString
 * Covenant-source-reference: com/badlogic/gdx/utils/OrderedSet.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 1039c144ae1a226a2235b8a9e9614ea46f3a84ad
 */
package sge
package utils

import scala.compiletime.summonFrom

/** An `ObjectSet` that also stores keys in a `DynamicArray` using the insertion order. Null keys are not allowed. No allocation is done except when growing the table size.
  *
  * Iteration is ordered and faster than an unordered set. Keys can also be accessed and the order changed using `orderedItems`. There is some additional overhead for add and remove.
  *
  * @author
  *   Nathan Sweet, Tommy Ettinger (original implementation)
  */
final class OrderedSet[A] private (
  private val set:    ObjectSet[A],
  private val _items: DynamicArray[A]
) {

  // --- Core ---

  /** The number of elements in this set. */
  def size: Int = set.size

  /** Returns true if the set is empty. */
  def isEmpty: Boolean = set.isEmpty

  /** Returns true if the set has one or more items. */
  def nonEmpty: Boolean = set.nonEmpty

  /** The load factor of the underlying hash set. */
  def loadFactor: Float = set.loadFactor

  // --- Access ---

  /** Returns true if the key was added to the set or false if it was already in the set. */
  def add(key: A): Boolean =
    if (!set.add(key)) false
    else {
      _items.add(key)
      true
    }

  /** Sets the key at the specified index. Returns true if the key was added to the set or false if it was already in the set. If this set already contains the key, the existing key's index is changed
    * if needed and false is returned.
    */
  def add(key: A, index: Int): Boolean =
    if (!set.add(key)) {
      val oldIndex = _items.indexOf(key)
      if (oldIndex != index) _items.insert(index, _items.removeIndex(oldIndex))
      false
    } else {
      _items.insert(index, key)
      true
    }

  /** Adds all elements from another OrderedSet. */
  def addAll(other: OrderedSet[A]): Unit = {
    ensureCapacity(other.size)
    val items = other._items
    var i     = 0
    while (i < items.size) {
      add(items(i))
      i += 1
    }
  }

  /** Adds all elements from a DynamicArray. */
  def addAll(array: DynamicArray[? <: A]): Unit = {
    ensureCapacity(array.size)
    val items = array.items
    val n     = array.size
    var i     = 0
    while (i < n) {
      add(items(i).asInstanceOf[A])
      i += 1
    }
  }

  /** Returns true if the key was removed. */
  def remove(key: A): Boolean =
    if (!set.remove(key)) false
    else {
      _items.removeValue(key)
      true
    }

  /** Removes the element at the given insertion-order index. Returns the removed element. */
  def removeIndex(index: Int): A = {
    val key = _items.removeIndex(index)
    set.remove(key)
    key
  }

  /** Returns true if the specified key is in the set. */
  def contains(key: A): Boolean = set.contains(key)

  /** Returns the stored instance for the given key, or `Nullable.empty`. */
  def get(key: A): Nullable[A] = set.get(key)

  /** Returns the first element in insertion order. Throws if empty. */
  def first: A = _items.first

  /** Changes the item `before` to `after` without changing its position in the order. Returns true if `before` was removed and `after` was added, false otherwise.
    */
  def alter(before: A, after: A): Boolean =
    if (set.contains(after)) false
    else if (!set.remove(before)) false
    else {
      set.add(after)
      _items.update(_items.indexOf(before), after)
      true
    }

  /** Changes the item at the given index in the order to `after`, without changing the ordering of other items. Returns true if `after` successfully replaced the contents at `index`, false otherwise.
    */
  def alterIndex(index: Int, after: A): Boolean =
    if (index < 0 || index >= set.size || set.contains(after)) false
    else {
      set.remove(_items(index))
      set.add(after)
      _items.update(index, after)
      true
    }

  // --- Bulk ---

  /** Clears the set. */
  def clear(): Unit = {
    _items.clear()
    set.clear()
  }

  /** Clears the set and reduces the size of the backing arrays. */
  def clear(maximumCapacity: Int): Unit = {
    _items.clear()
    set.clear(maximumCapacity)
  }

  /** Increases the size of the backing array to accommodate additional items. */
  def ensureCapacity(additionalCapacity: Int): Unit = {
    set.ensureCapacity(additionalCapacity)
    _items.ensureCapacity(additionalCapacity)
  }

  /** Reduces the size of the backing arrays. */
  def shrink(maximumCapacity: Int): Unit = set.shrink(maximumCapacity)

  // --- Order ---

  /** Returns the `DynamicArray` of items in insertion order. The array can be modified to change the order. */
  def orderedItems: DynamicArray[A] = _items

  // --- Iteration (ordered) ---
  // Architecture divergence: The original LibGDX OrderedSet uses a mutable Java-style inner-class iterator
  // (OrderedSetIterator). This port replaces it with a functional foreach method, which is idiomatic Scala and avoids
  // iterator-pool allocation complexity. All iteration functionality is preserved.

  /** Calls the given function for each element in insertion order. */
  def foreach(f: A => Unit): Unit = {
    var i = 0
    while (i < _items.size) {
      f(_items(i))
      i += 1
    }
  }

  /** Returns a new DynamicArray containing all elements in insertion order. */
  def toArray: DynamicArray[A] = DynamicArray.from(_items)

  // --- Standard ---

  override def hashCode(): Int = {
    var h     = size
    val items = _items.items
    var i     = 0
    while (i < _items.size) {
      h += items(i).hashCode()
      i += 1
    }
    h
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: ObjectSet[?] =>
      if (other.size != size) false
      else {
        val otherSet = other.asInstanceOf[ObjectSet[A]]
        var equal    = true
        var i        = 0
        while (i < _items.size && equal) {
          if (!otherSet.contains(_items(i))) equal = false
          i += 1
        }
        equal
      }
    case other: OrderedSet[?] =>
      if (other.size != size) false
      else {
        val otherSet = other.set.asInstanceOf[ObjectSet[A]]
        var equal    = true
        var i        = 0
        while (i < _items.size && equal) {
          if (!otherSet.contains(_items(i))) equal = false
          i += 1
        }
        equal
      }
    case _ => false
  }

  /** Returns a string representation using the specified separator between elements. */
  def toString(separator: String): String = _items.toString(separator)

  override def toString(): String =
    if (size == 0) "{}"
    else {
      val sb = new StringBuilder()
      sb.append('{')
      sb.append(_items(0))
      var i = 1
      while (i < _items.size) {
        sb.append(", ")
        sb.append(_items(i))
        i += 1
      }
      sb.append('}')
      sb.toString()
    }
}

object OrderedSet {

  /** Creates an OrderedSet with default capacity 51 and load factor 0.8. */
  inline def apply[A](): OrderedSet[A] = apply[A](51, 0.8f)

  /** Creates an OrderedSet with the given capacity and default load factor 0.8. */
  inline def apply[A](capacity: Int): OrderedSet[A] = apply[A](capacity, 0.8f)

  /** Creates an OrderedSet with the given capacity and load factor. */
  inline def apply[A](capacity: Int, loadFactor: Float): OrderedSet[A] = {
    val mk = summonMkArray[A]
    create(mk, capacity, loadFactor)
  }

  /** Creates an OrderedSet that is a copy of the given set. */
  def from[A](other: OrderedSet[A]): OrderedSet[A] =
    new OrderedSet[A](
      ObjectSet.from(other.set),
      DynamicArray.from(other._items)
    )

  private def create[A](mk: MkArray[A], capacity: Int, loadFactor: Float): OrderedSet[A] = {
    val set   = ObjectSet.createWithMk(mk, capacity, loadFactor)
    val items = DynamicArray.createWithMk(mk, capacity, true)
    new OrderedSet[A](set, items)
  }

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
