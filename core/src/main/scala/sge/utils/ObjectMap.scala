/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/ObjectMap.java
 * Original authors: Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.compiletime.summonFrom
import scala.util.boundary

/** An unordered map where the keys and values are objects. Null keys are not allowed. No allocation is done except when growing the table size.
  *
  * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to
  * reduce collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT size.
  *
  * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci hashing, instead of the more common power-of-two mask, to better
  * distribute poor hashCodes.
  *
  * Uses `filled: Array[Boolean]` for occupancy tracking instead of null-key checks. This means the same implementation works uniformly for both reference and primitive key types (e.g. `Int`, `Long`)
  * without special-casing for key=0.
  *
  * @author
  *   Nathan Sweet, Tommy Ettinger (original implementation)
  */
final class ObjectMap[K, V] private (
  private val mkK:        MkArray[K],
  private val mkV:        MkArray[V],
  private var keyTable:   Array[K],
  private var valueTable: Array[V],
  private var filled:     Array[Boolean],
  private var _size:      Int,
  private var mask:       Int,
  private var shift:      Int,
  val loadFactor:         Float,
  private var threshold:  Int
) {

  // --- Core ---

  /** The number of key-value pairs in this map. */
  def size: Int = _size

  /** Returns true if the map is empty. */
  def isEmpty: Boolean = _size == 0

  /** Returns true if the map has one or more items. */
  def nonEmpty: Boolean = _size > 0

  // --- Hash table internals ---

  /** Returns an index >= 0 and <= mask for the specified item using Fibonacci hashing. */
  private def place(key: K): Int =
    (key.hashCode().toLong * 0x9e3779b97f4a7c15L >>> shift).toInt

  /** Returns the index of the key if already present, else -(index + 1) for the next empty index. */
  private def locateKey(key: K): Int = boundary {
    var i = place(key)
    while (true) {
      if (!filled(i)) boundary.break(-(i + 1)) // Empty space is available.
      if (keyTable(i) == key) boundary.break(i) // Same key was found.
      i = (i + 1) & mask
    }
    -1 // unreachable
  }

  // --- Access ---

  /** Returns the old value associated with the specified key, or `Nullable.empty` if the key was not already in the map.
    */
  def put(key: K, value: V): Nullable[V] = {
    val i = locateKey(key)
    if (i >= 0) { // Existing key was found.
      val oldValue = valueTable(i)
      valueTable(i) = value
      Nullable(oldValue)
    } else {
      val slot = -(i + 1) // Empty space was found.
      keyTable(slot) = key
      valueTable(slot) = value
      filled(slot) = true
      _size += 1
      if (_size >= threshold) resize(keyTable.length << 1)
      Nullable.empty[V]
    }
  }

  /** Returns the value for the specified key, or `Nullable.empty` if the key is not in the map. */
  def get(key: K): Nullable[V] = {
    val i = locateKey(key)
    if (i < 0) Nullable.empty[V] else Nullable(valueTable(i))
  }

  /** Returns the value for the specified key, or the default value if the key is not in the map. */
  def get(key: K, defaultValue: V): V = {
    val i = locateKey(key)
    if (i < 0) defaultValue else valueTable(i)
  }

  /** Returns the value for the removed key, or `Nullable.empty` if the key is not in the map. Uses backward-shift deletion to maintain probe sequences without tombstones.
    */
  def remove(key: K): Nullable[V] = {
    var i = locateKey(key)
    if (i < 0) Nullable.empty[V]
    else {
      val oldValue = valueTable(i)
      // Backward-shift deletion
      var next = (i + 1) & mask
      while (filled(next)) {
        val k         = keyTable(next)
        val placement = place(k)
        if (((next - placement) & mask) > ((i - placement) & mask)) {
          keyTable(i) = k
          valueTable(i) = valueTable(next)
          filled(i) = true
          i = next
        }
        next = (next + 1) & mask
      }
      filled(i) = false
      _size -= 1
      Nullable(oldValue)
    }
  }

  /** Returns true if the specified key is in the map. */
  def containsKey(key: K): Boolean = locateKey(key) >= 0

  /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def containsValue(value: V): Boolean = boundary {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i) && valueTable(i) == value) boundary.break(true)
      i += 1
    }
    false
  }

  /** Returns the key for the specified value, or `Nullable.empty` if it is not in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def findKey(value: V): Nullable[K] = boundary {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i) && valueTable(i) == value) boundary.break(Nullable(keyTable(i)))
      i += 1
    }
    Nullable.empty[K]
  }

  // --- Bulk ---

  /** Copies all key-value pairs from the other map into this map. */
  def putAll(other: ObjectMap[K, V]): Unit = {
    ensureCapacity(other._size)
    val otherKeys   = other.keyTable
    val otherValues = other.valueTable
    val otherFilled = other.filled
    val len         = otherKeys.length
    var i           = 0
    while (i < len) {
      if (otherFilled(i)) put(otherKeys(i), otherValues(i))
      i += 1
    }
  }

  /** Clears the map. */
  def clear(): Unit =
    if (_size == 0) ()
    else {
      _size = 0
      java.util.Arrays.fill(filled, false)
    }

  /** Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
    */
  def clear(maximumCapacity: Int): Unit = {
    val tableSize = ObjectMap.tableSize(maximumCapacity, loadFactor)
    if (keyTable.length <= tableSize) clear()
    else {
      _size = 0
      resize(tableSize)
    }
  }

  /** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before adding many items to avoid multiple backing array resizes.
    */
  def ensureCapacity(additionalCapacity: Int): Unit = {
    val tableSize = ObjectMap.tableSize(_size + additionalCapacity, loadFactor)
    if (keyTable.length < tableSize) resize(tableSize)
  }

  /** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less, nothing is done. If the map contains more items than the specified
    * capacity, the next highest power of two capacity is used instead.
    */
  def shrink(maximumCapacity: Int): Unit = {
    if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity)
    val tableSize = ObjectMap.tableSize(maximumCapacity, loadFactor)
    if (keyTable.length > tableSize) resize(tableSize)
  }

  /** Internal resize. Rehashes all entries into new arrays. */
  private[utils] def resize(newSize: Int): Unit = {
    val oldCapacity = keyTable.length
    val oldKeyTable = keyTable
    val oldValTable = valueTable
    val oldFilled   = filled

    threshold = (newSize * loadFactor).toInt
    mask = newSize - 1
    shift = java.lang.Long.numberOfLeadingZeros(mask.toLong)

    keyTable = mkK.create(newSize)
    valueTable = mkV.create(newSize)
    filled = new Array[Boolean](newSize)

    if (_size > 0) {
      var i = 0
      while (i < oldCapacity) {
        if (oldFilled(i)) putResize(oldKeyTable(i), oldValTable(i))
        i += 1
      }
    }
  }

  /** Skips checks for existing keys, doesn't increment size. Used during resize. */
  private def putResize(key: K, value: V): Unit = boundary {
    var i = place(key)
    while (true) {
      if (!filled(i)) {
        keyTable(i) = key
        valueTable(i) = value
        filled(i) = true
        boundary.break(())
      }
      i = (i + 1) & mask
    }
  }

  // --- Iteration ---

  /** Calls the given function for each key-value pair in the map. Iteration order is not guaranteed. */
  def foreachEntry(f: (K, V) => Unit): Unit = {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) f(keyTable(i), valueTable(i))
      i += 1
    }
  }

  /** Calls the given function for each key in the map. Iteration order is not guaranteed. */
  def foreachKey(f: K => Unit): Unit = {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) f(keyTable(i))
      i += 1
    }
  }

  /** Calls the given function for each value in the map. Iteration order is not guaranteed. */
  def foreachValue(f: V => Unit): Unit = {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) f(valueTable(i))
      i += 1
    }
  }

  // --- Standard ---

  override def hashCode(): Int = {
    var h   = _size
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) {
        h += keyTable(i).hashCode()
        h += valueTable(i).hashCode()
      }
      i += 1
    }
    h
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: ObjectMap[?, ?] =>
      if (other eq this) true
      else if (other._size != _size) false
      else {
        val otherMap = other.asInstanceOf[ObjectMap[K, V]]
        var equal    = true
        val len      = keyTable.length
        var i        = 0
        while (i < len && equal) {
          if (filled(i)) {
            val value    = valueTable(i)
            val otherVal = otherMap.get(keyTable(i))
            if (otherVal.isEmpty || otherVal.getOrElse(value) != value) equal = false
          }
          i += 1
        }
        equal
      }
    case _ => false
  }

  override def toString(): String =
    if (_size == 0) "{}"
    else {
      val sb  = new StringBuilder()
      val len = keyTable.length
      sb.append('{')
      var first = true
      var i     = 0
      while (i < len) {
        if (filled(i)) {
          if (!first) sb.append(", ")
          sb.append(keyTable(i))
          sb.append('=')
          sb.append(valueTable(i))
          first = false
        }
        i += 1
      }
      sb.append('}')
      sb.toString()
    }

  // --- Internal accessors for OrderedMap ---

  private[utils] def internalKeyTable:   Array[K]       = keyTable
  private[utils] def internalValueTable: Array[V]       = valueTable
  private[utils] def internalFilled:     Array[Boolean] = filled
  private[utils] def internalMask:       Int            = mask
  private[utils] def internalShift:      Int            = shift
  private[utils] def internalThreshold:  Int            = threshold
}

object ObjectMap {

  /** Creates an ObjectMap with default capacity 51 and load factor 0.8. */
  inline def apply[K, V](): ObjectMap[K, V] = apply[K, V](51, 0.8f)

  /** Creates an ObjectMap with the given capacity and default load factor 0.8. */
  inline def apply[K, V](capacity: Int): ObjectMap[K, V] = apply[K, V](capacity, 0.8f)

  /** Creates an ObjectMap with the given capacity and load factor. */
  inline def apply[K, V](capacity: Int, loadFactor: Float): ObjectMap[K, V] = {
    val mkK = summonMkArray[K]
    val mkV = summonMkArray[V]
    create(mkK, mkV, capacity, loadFactor)
  }

  /** Creates an ObjectMap that is a copy of the given map. */
  def from[K, V](other: ObjectMap[K, V]): ObjectMap[K, V] = {
    val map = new ObjectMap[K, V](
      other.mkK,
      other.mkV,
      other.mkK.copyOf(other.keyTable, other.keyTable.length),
      other.mkV.copyOf(other.valueTable, other.valueTable.length),
      java.util.Arrays.copyOf(other.filled, other.filled.length),
      other._size,
      other.mask,
      other.shift,
      other.loadFactor,
      other.threshold
    )
    map
  }

  private def create[K, V](mkK: MkArray[K], mkV: MkArray[V], capacity: Int, loadFactor: Float): ObjectMap[K, V] = {
    if (loadFactor <= 0f || loadFactor >= 1f)
      throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor)

    val ts        = tableSize(capacity, loadFactor)
    val threshold = (ts * loadFactor).toInt
    val mask      = ts - 1
    val shift     = java.lang.Long.numberOfLeadingZeros(mask.toLong).toInt

    new ObjectMap[K, V](
      mkK,
      mkV,
      mkK.create(ts),
      mkV.create(ts),
      new Array[Boolean](ts),
      0,
      mask,
      shift,
      loadFactor,
      threshold
    )
  }

  /** Creates an ObjectMap with explicit MkArray instances. For use by OrderedMap and other internal code. */
  private[utils] def createWithMk[K, V](
    mkK:        MkArray[K],
    mkV:        MkArray[V],
    capacity:   Int,
    loadFactor: Float
  ): ObjectMap[K, V] = create(mkK, mkV, capacity, loadFactor)

  /** Computes the table size (next power of two) for the given capacity and load factor. */
  private[utils] def tableSize(capacity: Int, loadFactor: Float): Int = {
    if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0: " + capacity)
    val ts = math.MathUtils.nextPowerOfTwo(Math.max(2, Math.ceil(capacity.toDouble / loadFactor).toInt))
    if (ts > (1 << 30)) throw new IllegalArgumentException("The required capacity is too large: " + capacity)
    ts
  }

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
