/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/ArrayMap.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Array<>` -> `DynamicArray`; null keys -> `Nullable`; `Comparator` -> `Ordering`
 *   Convention: private constructor with `MkArray`-based factory methods; `final class`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 516
 * Covenant-baseline-methods: ArrayMap,_size,apply,clear,containsKey,containsValue,create,ensureCapacity,equals,equalsIdentity,existingIdx,firstKey,firstValue,foreachEntry,foreachKey,foreachValue,from,get,getKey,getKeyAt,getValueAt,grow,h,hashCode,i,indexOfKey,indexOfValue,insert,isEmpty,j,keyArray,mkK,mkV,needed,newCapacity,nonEmpty,peekKey,peekValue,preserveOrder,put,putAll,removeIndex,removeKey,removeValue,reverse,setKeyAt,setValueAt,shrink,shuffle,size,sizeNeeded,summonMkArray,toString,truncate,valArray
 * Covenant-source-reference: com/badlogic/gdx/utils/ArrayMap.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: ff659249e292141692fb7b0b2b30c69ac38e5e0d
 */
package sge
package utils

import scala.compiletime.summonFrom
import scala.util.boundary

/** An ordered or unordered map of objects. This implementation uses arrays to store the keys and values, which means gets do a comparison for each key in the map. This is slower than a typical hash
  * map implementation, but may be acceptable for small maps and has the benefits that keys and values can be accessed by index, which makes iteration fast. Like `DynamicArray`, if `preserveOrder` is
  * false, this class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
final class ArrayMap[K, V] private (
  private val mkK:      MkArray[K],
  private val mkV:      MkArray[V],
  private var keyArray: Array[K],
  private var valArray: Array[V],
  private var _size:    Int,
  val preserveOrder:    Boolean
) {

  // --- Core ---

  /** The number of key-value pairs in this map. */
  def size: Int = _size

  /** Returns true if the map is empty. */
  def isEmpty: Boolean = _size == 0

  /** Returns true if the map has one or more items. */
  def nonEmpty: Boolean = _size > 0

  // --- Access ---

  /** Puts a key-value pair into the map. If the key already exists, the value is replaced and the index is returned. If the key is new, it is appended and the new index is returned.
    */
  def put(key: K, value: V): Int = {
    val existingIdx = indexOfKey(key)
    if (existingIdx >= 0) {
      valArray(existingIdx) = value
      existingIdx
    } else {
      if (_size == keyArray.length) grow()
      keyArray(_size) = key
      valArray(_size) = value
      _size += 1
      _size - 1
    }
  }

  /** Puts a key-value pair at the specified index. If the key already exists, it is first removed, then inserted at the given index.
    */
  def put(key: K, value: V, index: Int): Int = {
    val existingIdx = indexOfKey(key)
    if (existingIdx != -1) {
      removeIndex(existingIdx)
    } else if (_size == keyArray.length) {
      grow()
    }
    System.arraycopy(keyArray, index, keyArray, index + 1, _size - index)
    System.arraycopy(valArray, index, valArray, index + 1, _size - index)
    keyArray(index) = key
    valArray(index) = value
    _size += 1
    index
  }

  /** Returns the value for the specified key, or `Nullable.empty` if not found. */
  def get(key: K): Nullable[V] = {
    val i = indexOfKey(key)
    if (i < 0) Nullable.empty[V] else Nullable(valArray(i))
  }

  /** Returns the value for the specified key, or the default value if not found. */
  def get(key: K, defaultValue: V): V = {
    val i = indexOfKey(key)
    if (i < 0) defaultValue else valArray(i)
  }

  /** Returns the key at the given index. */
  def getKeyAt(index: Int): K = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    keyArray(index)
  }

  /** Returns the value at the given index. */
  def getValueAt(index: Int): V = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    valArray(index)
  }

  /** Returns the first key. Throws if the map is empty. */
  def firstKey: K = {
    if (_size == 0) throw new IllegalStateException("Map is empty.")
    keyArray(0)
  }

  /** Returns the first value. Throws if the map is empty. */
  def firstValue: V = {
    if (_size == 0) throw new IllegalStateException("Map is empty.")
    valArray(0)
  }

  /** Returns the key for the specified value. Note this does a comparison of each value in reverse order until the specified value is found.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def getKey(value: V, identity: Boolean): Nullable[K] = boundary {
    var i = _size - 1
    if (identity) {
      while (i >= 0) {
        if (valArray(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) boundary.break(Nullable(keyArray(i)))
        i -= 1
      }
    } else {
      while (i >= 0) {
        if (value == valArray(i)) boundary.break(Nullable(keyArray(i)))
        i -= 1
      }
    }
    Nullable.empty[K]
  }

  /** Replaces the key at the given index, keeping the same value. */
  def setKeyAt(index: Int, key: K): Unit = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    keyArray(index) = key
  }

  /** Replaces the value at the given index, keeping the same key. */
  def setValueAt(index: Int, value: V): Unit = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    valArray(index) = value
  }

  /** Inserts a key-value pair at the given index. If `preserveOrder` is true, existing elements are shifted; otherwise the element at the given index is moved to the end.
    */
  def insert(index: Int, key: K, value: V): Unit = {
    if (index > _size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + _size)
    if (_size == keyArray.length) grow()
    if (preserveOrder) {
      System.arraycopy(keyArray, index, keyArray, index + 1, _size - index)
      System.arraycopy(valArray, index, valArray, index + 1, _size - index)
    } else {
      keyArray(_size) = keyArray(index)
      valArray(_size) = valArray(index)
    }
    _size += 1
    keyArray(index) = key
    valArray(index) = value
  }

  /** Removes the key-value pair for the specified key, returning the value or `Nullable.empty`. */
  def removeKey(key: K): Nullable[V] = {
    val i = indexOfKey(key)
    if (i < 0) Nullable.empty[V]
    else {
      val value = valArray(i)
      removeIndex(i)
      Nullable(value)
    }
  }

  /** Removes the key-value pair at the specified index. */
  def removeIndex(index: Int): Unit = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    _size -= 1
    if (preserveOrder) {
      System.arraycopy(keyArray, index + 1, keyArray, index, _size - index)
      System.arraycopy(valArray, index + 1, valArray, index, _size - index)
    } else {
      keyArray(index) = keyArray(_size)
      valArray(index) = valArray(_size)
    }
    // Null the vacated last slot to allow GC
    if (keyArray.isInstanceOf[Array[AnyRef]]) keyArray.asInstanceOf[Array[AnyRef]](_size) = null
    if (valArray.isInstanceOf[Array[AnyRef]]) valArray.asInstanceOf[Array[AnyRef]](_size) = null
  }

  /** Removes the first key-value pair with the specified value. Returns true if found.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def removeValue(value: V, identity: Boolean): Boolean = {
    val i = indexOfValue(value, identity)
    if (i < 0) false
    else {
      removeIndex(i)
      true
    }
  }

  /** Returns the index of the specified key, or -1 if not found. */
  def indexOfKey(key: K): Int = boundary {
    var i = 0
    while (i < _size) {
      if (keyArray(i) == key) boundary.break(i)
      i += 1
    }
    -1
  }

  /** Returns the index of the specified value, or -1 if not found. Uses `==` (equals) comparison. */
  def indexOfValue(value: V): Int = indexOfValue(value, identity = false)

  /** Returns the index of the specified value, or -1 if not found.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def indexOfValue(value: V, identity: Boolean): Int = boundary {
    var i = 0
    if (identity) {
      while (i < _size) {
        if (valArray(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) boundary.break(i)
        i += 1
      }
    } else {
      while (i < _size) {
        if (value == valArray(i)) boundary.break(i)
        i += 1
      }
    }
    -1
  }

  /** Returns true if the specified key is in the map. */
  def containsKey(key: K): Boolean = indexOfKey(key) >= 0

  /** Returns true if the specified value is in the map. Uses `==` (equals) comparison.
    * @param identity
    *   If true, `eq` (reference identity) comparison will be used. If false, `==` (equals) comparison will be used.
    */
  def containsValue(value: V, identity: Boolean): Boolean = indexOfValue(value, identity) >= 0

  /** Returns true if the specified value is in the map. Uses `==` (equals) comparison. */
  def containsValue(value: V): Boolean = indexOfValue(value) >= 0

  // --- Bulk ---

  /** Copies all key-value pairs from the other map into this map. */
  def putAll(other: ArrayMap[K, V]): Unit = putAll(other, 0, other._size)

  /** Copies key-value pairs from the other map into this map, starting at `offset` for `length` elements. */
  def putAll(other: ArrayMap[K, V], offset: Int, length: Int): Unit = {
    if (offset + length > other._size)
      throw new IllegalArgumentException(
        "offset + length must be <= size: " + offset + " + " + length + " <= " + other._size
      )
    val sizeNeeded = _size + length - offset
    if (sizeNeeded >= keyArray.length) {
      val newCap = Math.max(8, (sizeNeeded * 1.75).toInt)
      keyArray = mkK.copyOf(keyArray, newCap)
      valArray = mkV.copyOf(valArray, newCap)
    }
    System.arraycopy(other.keyArray, offset, keyArray, _size, length)
    System.arraycopy(other.valArray, offset, valArray, _size, length)
    _size += length
  }

  /** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
  def clear(maximumCapacity: Int): Unit =
    if (keyArray.length <= maximumCapacity) {
      clear()
    } else {
      _size = 0
      keyArray = mkK.create(maximumCapacity)
      valArray = mkV.create(maximumCapacity)
    }

  /** Removes all key-value pairs. */
  def clear(): Unit = {
    // Null reference-type arrays to allow GC before resetting size
    if (keyArray.isInstanceOf[Array[AnyRef]])
      java.util.Arrays.fill(keyArray.asInstanceOf[Array[AnyRef]], 0, _size, null)
    if (valArray.isInstanceOf[Array[AnyRef]])
      java.util.Arrays.fill(valArray.asInstanceOf[Array[AnyRef]], 0, _size, null)
    _size = 0
  }

  /** Ensures capacity for at least `additional` more elements beyond current size. */
  def ensureCapacity(additional: Int): Unit = {
    val needed = _size + additional
    if (needed > keyArray.length) {
      keyArray = mkK.copyOf(keyArray, needed)
      valArray = mkV.copyOf(valArray, needed)
    }
  }

  /** Trims the backing arrays to exactly `size` elements. */
  def shrink(): Unit =
    if (keyArray.length != _size) {
      keyArray = mkK.copyOf(keyArray, _size)
      valArray = mkV.copyOf(valArray, _size)
    }

  // --- Stack-like access ---

  /** Returns the last key. Throws if empty. */
  def peekKey: K = {
    if (_size == 0) throw new IndexOutOfBoundsException("ArrayMap is empty.")
    keyArray(_size - 1)
  }

  /** Returns the last value. Throws if empty. */
  def peekValue: V = {
    if (_size == 0) throw new IndexOutOfBoundsException("ArrayMap is empty.")
    valArray(_size - 1)
  }

  // --- Reorder ---

  /** Reverses the order of all key-value pairs. */
  def reverse(): Unit = {
    var i = 0
    var j = _size - 1
    while (i < j) {
      val tmpK = keyArray(i)
      keyArray(i) = keyArray(j)
      keyArray(j) = tmpK
      val tmpV = valArray(i)
      valArray(i) = valArray(j)
      valArray(j) = tmpV
      i += 1
      j -= 1
    }
  }

  /** Shuffles key-value pairs randomly using `MathUtils.random`. */
  def shuffle(): Unit = {
    var i = _size - 1
    while (i > 0) {
      val ii   = math.MathUtils.random(i)
      val tmpK = keyArray(i)
      keyArray(i) = keyArray(ii)
      keyArray(ii) = tmpK
      val tmpV = valArray(i)
      valArray(i) = valArray(ii)
      valArray(ii) = tmpV
      i -= 1
    }
  }

  /** Reduces the size to at most `newSize`, discarding trailing pairs. */
  def truncate(newSize: Int): Unit =
    if (newSize < _size) {
      // Null vacated reference-type slots to allow GC
      if (keyArray.isInstanceOf[Array[AnyRef]])
        java.util.Arrays.fill(keyArray.asInstanceOf[Array[AnyRef]], newSize, _size, null)
      if (valArray.isInstanceOf[Array[AnyRef]])
        java.util.Arrays.fill(valArray.asInstanceOf[Array[AnyRef]], newSize, _size, null)
      _size = newSize
    }

  // --- Iteration ---
  // Architecture divergence: The original LibGDX ArrayMap uses mutable Java-style inner-class iterators (Entries, Keys, Values)
  // with pooling via Collections.allocateIterators. This port replaces them with functional foreach* methods, which are idiomatic
  // Scala, avoid iterator-pool allocation complexity, and eliminate the nested-iterator misuse bugs that the original guards against.
  // All iteration functionality (entries, keys, values) is preserved; only the mechanism differs.

  /** Calls the given function for each key-value pair. */
  def foreachEntry(f: (K, V) => Unit): Unit = {
    var i = 0
    while (i < _size) {
      f(keyArray(i), valArray(i))
      i += 1
    }
  }

  /** Calls the given function for each key. */
  def foreachKey(f: K => Unit): Unit = {
    var i = 0
    while (i < _size) {
      f(keyArray(i))
      i += 1
    }
  }

  /** Calls the given function for each value. */
  def foreachValue(f: V => Unit): Unit = {
    var i = 0
    while (i < _size) {
      f(valArray(i))
      i += 1
    }
  }

  // --- Standard ---

  override def hashCode(): Int = {
    var h = 1
    var i = 0
    while (i < _size) {
      h = 31 * h + keyArray(i).hashCode()
      h = 31 * h + valArray(i).hashCode()
      i += 1
    }
    h
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: ArrayMap[?, ?] =>
      if (other eq this) true
      else if (other._size != _size) false
      else {
        val otherMap = other.asInstanceOf[ArrayMap[K, V]]
        var equal    = true
        var i        = 0
        while (i < _size && equal) {
          if (keyArray(i) != otherMap.keyArray(i) || valArray(i) != otherMap.valArray(i)) equal = false
          i += 1
        }
        equal
      }
    case _ => false
  }

  /** Uses `eq` (reference identity) for comparison of each value. */
  def equalsIdentity(obj: Any): Boolean = obj match {
    case other: ArrayMap[?, ?] =>
      if (other eq this) true
      else if (other._size != _size) false
      else {
        val otherMap = other.asInstanceOf[ArrayMap[K, V]]
        var equal    = true
        var i        = 0
        while (i < _size && equal) {
          val otherVal = otherMap.get(keyArray(i))
          if (otherVal.isEmpty || !(valArray(i).asInstanceOf[AnyRef] eq otherVal.get.asInstanceOf[AnyRef]))
            equal = false
          i += 1
        }
        equal
      }
    case _ => false
  }

  override def toString(): String =
    if (_size == 0) "{}"
    else {
      val sb = new StringBuilder()
      sb.append('{')
      var i = 0
      while (i < _size) {
        if (i > 0) sb.append(", ")
        sb.append(keyArray(i))
        sb.append('=')
        sb.append(valArray(i))
        i += 1
      }
      sb.append('}')
      sb.toString()
    }

  // --- Internal ---

  private def grow(): Unit = {
    val newCapacity = Math.max(8, (_size * 1.75).toInt)
    keyArray = mkK.copyOf(keyArray, newCapacity)
    valArray = mkV.copyOf(valArray, newCapacity)
  }
}

object ArrayMap {

  /** Creates an ordered ArrayMap with default capacity 16. */
  inline def apply[K, V](): ArrayMap[K, V] = apply[K, V](true, 16)

  /** Creates an ordered ArrayMap with the given capacity. */
  inline def apply[K, V](capacity: Int): ArrayMap[K, V] = apply[K, V](true, capacity)

  /** Creates an ArrayMap with the given order mode and capacity. */
  inline def apply[K, V](preserveOrder: Boolean, capacity: Int): ArrayMap[K, V] = {
    val mkK = summonMkArray[K]
    val mkV = summonMkArray[V]
    create(mkK, mkV, capacity, preserveOrder)
  }

  /** Creates an ArrayMap that is a copy of the given map. */
  def from[K, V](other: ArrayMap[K, V]): ArrayMap[K, V] =
    new ArrayMap[K, V](
      other.mkK,
      other.mkV,
      other.mkK.copyOf(other.keyArray, other.keyArray.length),
      other.mkV.copyOf(other.valArray, other.valArray.length),
      other._size,
      other.preserveOrder
    )

  private def create[K, V](
    mkK:           MkArray[K],
    mkV:           MkArray[V],
    capacity:      Int,
    preserveOrder: Boolean
  ): ArrayMap[K, V] =
    new ArrayMap[K, V](mkK, mkV, mkK.create(capacity), mkV.create(capacity), 0, preserveOrder)

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
