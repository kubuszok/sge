/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/OrderedMap.java
 * Original authors: Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Array<K> keys` -> `DynamicArray[K] _keys`; accessor `orderedKeys()`
 *   Convention: delegates to `ObjectMap` + `DynamicArray` internally; `final class`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.compiletime.summonFrom

/** An `ObjectMap` that also stores keys in a `DynamicArray` using the insertion order. Null keys are not allowed. No allocation is done except when growing the table size.
  *
  * Iteration over the entries, keys, and values is ordered and faster than an unordered map. Keys can also be accessed and the order changed using `orderedKeys`. There is some additional overhead for
  * put and remove.
  *
  * @author
  *   Nathan Sweet, Tommy Ettinger (original implementation)
  */
final class OrderedMap[K, V] private (
  private val map:   ObjectMap[K, V],
  private val _keys: DynamicArray[K]
) {

  // --- Core ---

  /** The number of key-value pairs in this map. */
  def size: Int = map.size

  /** Returns true if the map is empty. */
  def isEmpty: Boolean = map.isEmpty

  /** Returns true if the map has one or more items. */
  def nonEmpty: Boolean = map.nonEmpty

  /** The load factor of the underlying hash map. */
  def loadFactor: Float = map.loadFactor

  // --- Access ---

  /** Returns the old value associated with the specified key, or `Nullable.empty` if the key was not already in the map.
    */
  def put(key: K, value: V): Nullable[V] = {
    val existing = map.get(key)
    map.put(key, value)
    if (existing.isEmpty) _keys.add(key) // Only add to ordered keys if this is a new key
    existing
  }

  /** Returns the value for the specified key, or `Nullable.empty` if the key is not in the map. */
  def get(key: K): Nullable[V] = map.get(key)

  /** Returns the value for the specified key, or the default value if the key is not in the map. */
  def get(key: K, defaultValue: V): V = map.get(key, defaultValue)

  /** Returns the value for the removed key, or `Nullable.empty` if the key is not in the map. */
  def remove(key: K): Nullable[V] = {
    _keys.removeValue(key)
    map.remove(key)
  }

  /** Removes the key-value pair at the given insertion-order index. Returns the removed value, or `Nullable.empty`. */
  def removeIndex(index: Int): Nullable[V] = {
    val key = _keys.removeIndex(index)
    map.remove(key)
  }

  /** Returns true if the specified key is in the map. */
  def containsKey(key: K): Boolean = map.containsKey(key)

  /** Returns true if the specified value is in the map. */
  def containsValue(value: V): Boolean = map.containsValue(value)

  /** Returns true if the specified value is in the map.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def containsValue(value: V, identity: Boolean): Boolean = map.containsValue(value, identity)

  /** Returns the key for the specified value, or `Nullable.empty` if it is not in the map. */
  def findKey(value: V): Nullable[K] = map.findKey(value)

  /** Returns the key for the specified value, or `Nullable.empty` if it is not in the map.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def findKey(value: V, identity: Boolean): Nullable[K] = map.findKey(value, identity)

  /** Changes the key `before` to `after` without changing its position in the order or its value. Returns true if `before` was removed and `after` was added, false otherwise (i.e. `after` is already
    * present or `before` is not present).
    */
  def alter(before: K, after: K): Boolean =
    if (map.containsKey(after)) false
    else {
      val idx = _keys.indexOf(before)
      if (idx < 0) false
      else {
        val value = map.remove(before)
        value.foreach { v =>
          map.put(after, v)
        }
        _keys.update(idx, after)
        true
      }
    }

  /** Changes the key at the given index in the order to `after`, without changing the ordering of other entries or any values. Returns true if `after` successfully replaced the key at `index`, false
    * otherwise.
    */
  def alterIndex(index: Int, after: K): Boolean =
    if (index < 0 || index >= map.size || map.containsKey(after)) false
    else {
      val before = _keys(index)
      val value  = map.remove(before)
      value.foreach { v =>
        map.put(after, v)
      }
      _keys.update(index, after)
      true
    }

  // --- Bulk ---

  /** Copies all key-value pairs from the other map into this map. */
  def putAll(other: OrderedMap[K, V]): Unit = {
    map.ensureCapacity(other.size)
    val otherKeys = other._keys
    var i         = 0
    while (i < otherKeys.size) {
      val key = otherKeys(i)
      put(key, other.map.getUnsafe(key))
      i += 1
    }
  }

  /** Clears the map. */
  def clear(): Unit = {
    _keys.clear()
    map.clear()
  }

  /** Clears the map and reduces the size of the backing arrays. */
  def clear(maximumCapacity: Int): Unit = {
    _keys.clear()
    map.clear(maximumCapacity)
  }

  /** Increases the size of the backing array to accommodate additional items. */
  def ensureCapacity(additionalCapacity: Int): Unit =
    map.ensureCapacity(additionalCapacity)

  /** Reduces the size of the backing arrays. */
  def shrink(maximumCapacity: Int): Unit = map.shrink(maximumCapacity)

  // --- Order ---

  /** Returns the `DynamicArray` of keys in insertion order. The array can be modified to change the order. */
  def orderedKeys: DynamicArray[K] = _keys

  // --- Iteration (ordered) ---
  // Architecture divergence: The original LibGDX OrderedMap uses mutable Java-style inner-class iterators
  // (OrderedMapEntries, OrderedMapKeys, OrderedMapValues). This port replaces them with functional foreach* methods,
  // which are idiomatic Scala and avoid iterator-pool allocation complexity. All iteration functionality is preserved.

  /** Calls the given function for each key-value pair in insertion order. */
  def foreachEntry(f: (K, V) => Unit): Unit = {
    var i = 0
    while (i < _keys.size) {
      val key = _keys(i)
      f(key, map.getUnsafe(key))
      i += 1
    }
  }

  /** Calls the given function for each key in insertion order. */
  def foreachKey(f: K => Unit): Unit = {
    var i = 0
    while (i < _keys.size) {
      f(_keys(i))
      i += 1
    }
  }

  /** Calls the given function for each value in insertion order. */
  def foreachValue(f: V => Unit): Unit = {
    var i = 0
    while (i < _keys.size) {
      f(map.getUnsafe(_keys(i)))
      i += 1
    }
  }

  // --- Standard ---

  override def hashCode(): Int = map.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OrderedMap[?, ?] => map.equals(other.map)
    case _ => false
  }

  /** Uses `eq` (reference identity) for comparison of each value. */
  def equalsIdentity(obj: Any): Boolean = obj match {
    case other: OrderedMap[?, ?] => map.equalsIdentity(other.map)
    case _ => false
  }

  /** Returns a string representation using the specified separator between entries. */
  def toString(separator: String): String =
    if (size == 0) ""
    else {
      val sb = new StringBuilder()
      var i  = 0
      while (i < _keys.size) {
        if (i > 0) sb.append(separator)
        val key = _keys(i)
        sb.append(key)
        sb.append('=')
        sb.append(map.getUnsafe(key))
        i += 1
      }
      sb.toString()
    }

  override def toString(): String =
    if (size == 0) "{}"
    else {
      val sb = new StringBuilder()
      sb.append('{')
      var i = 0
      while (i < _keys.size) {
        if (i > 0) sb.append(", ")
        val key = _keys(i)
        sb.append(key)
        sb.append('=')
        sb.append(map.getUnsafe(key))
        i += 1
      }
      sb.append('}')
      sb.toString()
    }
}

object OrderedMap {

  /** Creates an OrderedMap with default capacity 51 and load factor 0.8. */
  inline def apply[K, V](): OrderedMap[K, V] = apply[K, V](51, 0.8f)

  /** Creates an OrderedMap with the given capacity and default load factor 0.8. */
  inline def apply[K, V](capacity: Int): OrderedMap[K, V] = apply[K, V](capacity, 0.8f)

  /** Creates an OrderedMap with the given capacity and load factor. */
  inline def apply[K, V](capacity: Int, loadFactor: Float): OrderedMap[K, V] = {
    val mkK = summonMkArray[K]
    val mkV = summonMkArray[V]
    create(mkK, mkV, capacity, loadFactor)
  }

  /** Creates an OrderedMap that is a copy of the given map. */
  def from[K, V](other: OrderedMap[K, V]): OrderedMap[K, V] =
    new OrderedMap[K, V](
      ObjectMap.from(other.map),
      DynamicArray.from(other._keys)
    )

  private def create[K, V](
    mkK:        MkArray[K],
    mkV:        MkArray[V],
    capacity:   Int,
    loadFactor: Float
  ): OrderedMap[K, V] = {
    val map  = ObjectMap.createWithMk(mkK, mkV, capacity, loadFactor)
    val keys = DynamicArray.createWithMk(mkK, capacity, true)
    new OrderedMap[K, V](map, keys)
  }

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
