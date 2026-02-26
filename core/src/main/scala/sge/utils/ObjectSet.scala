/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/ObjectSet.java
 * Original authors: Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.compiletime.summonFrom
import scala.util.boundary

/** An unordered set where the keys are objects. Null keys are not allowed. No allocation is done except when growing the table size.
  *
  * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to
  * reduce collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT size.
  *
  * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci hashing.
  *
  * Uses `filled: Array[Boolean]` for occupancy tracking instead of null-key checks, so the same implementation works uniformly for both reference and primitive key types.
  *
  * @author
  *   Nathan Sweet, Tommy Ettinger (original implementation)
  */
final class ObjectSet[A] private (
  private val mk:        MkArray[A],
  private var keyTable:  Array[A],
  private var filled:    Array[Boolean],
  private var _size:     Int,
  private var mask:      Int,
  private var shift:     Int,
  val loadFactor:        Float,
  private var threshold: Int
) {

  // --- Core ---

  /** The number of elements in this set. */
  def size: Int = _size

  /** Returns true if the set is empty. */
  def isEmpty: Boolean = _size == 0

  /** Returns true if the set has one or more items. */
  def nonEmpty: Boolean = _size > 0

  // --- Hash table internals ---

  /** Returns an index >= 0 and <= mask for the specified item using Fibonacci hashing. */
  private def place(key: A): Int =
    (key.hashCode().toLong * 0x9e3779b97f4a7c15L >>> shift).toInt

  /** Returns the index of the key if already present, else -(index + 1) for the next empty index. */
  private def locateKey(key: A): Int = boundary {
    var i = place(key)
    while (true) {
      if (!filled(i)) boundary.break(-(i + 1)) // Empty space is available.
      if (keyTable(i) == key) boundary.break(i) // Same key was found.
      i = (i + 1) & mask
    }
    -1 // unreachable
  }

  // --- Access ---

  /** Returns true if the key was added to the set or false if it was already in the set. */
  def add(key: A): Boolean = {
    val i = locateKey(key)
    if (i >= 0) false // Existing key was found.
    else {
      val slot = -(i + 1) // Empty space was found.
      keyTable(slot) = key
      filled(slot) = true
      _size += 1
      if (_size >= threshold) resize(keyTable.length << 1)
      true
    }
  }

  /** Adds all elements from another ObjectSet. */
  def addAll(other: ObjectSet[A]): Unit = {
    ensureCapacity(other._size)
    val otherKeys   = other.keyTable
    val otherFilled = other.filled
    val len         = otherKeys.length
    var i           = 0
    while (i < len) {
      if (otherFilled(i)) add(otherKeys(i))
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

  /** Returns true if the key was removed. Uses backward-shift deletion. */
  def remove(key: A): Boolean = {
    var i = locateKey(key)
    if (i < 0) false
    else {
      // Backward-shift deletion
      var next = (i + 1) & mask
      while (filled(next)) {
        val k         = keyTable(next)
        val placement = place(k)
        if (((next - placement) & mask) > ((i - placement) & mask)) {
          keyTable(i) = k
          filled(i) = true
          i = next
        }
        next = (next + 1) & mask
      }
      filled(i) = false
      _size -= 1
      true
    }
  }

  /** Returns true if the specified key is in the set. */
  def contains(key: A): Boolean = locateKey(key) >= 0

  /** Returns the stored instance for the given key, or `Nullable.empty` if not present. Useful when the set contains canonical instances and you want to retrieve the stored one.
    */
  def get(key: A): Nullable[A] = {
    val i = locateKey(key)
    if (i < 0) Nullable.empty[A] else Nullable(keyTable(i))
  }

  /** Returns the first non-empty element in the backing table. Throws if the set is empty. */
  def first: A = boundary {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) boundary.break(keyTable(i))
      i += 1
    }
    throw new IllegalStateException("ObjectSet is empty.")
  }

  // --- Bulk ---

  /** Clears the set. */
  def clear(): Unit =
    if (_size == 0) ()
    else {
      _size = 0
      java.util.Arrays.fill(filled, false)
    }

  /** Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
    */
  def clear(maximumCapacity: Int): Unit = {
    val tableSize = ObjectMap.tableSize(maximumCapacity, loadFactor)
    if (keyTable.length <= tableSize) clear()
    else {
      _size = 0
      resize(tableSize)
    }
  }

  /** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. */
  def ensureCapacity(additionalCapacity: Int): Unit = {
    val tableSize = ObjectMap.tableSize(_size + additionalCapacity, loadFactor)
    if (keyTable.length < tableSize) resize(tableSize)
  }

  /** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. */
  def shrink(maximumCapacity: Int): Unit = {
    if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity)
    val tableSize = ObjectMap.tableSize(maximumCapacity, loadFactor)
    if (keyTable.length > tableSize) resize(tableSize)
  }

  /** Internal resize. Rehashes all entries into new arrays. */
  private[utils] def resize(newSize: Int): Unit = {
    val oldCapacity = keyTable.length
    val oldKeyTable = keyTable
    val oldFilled   = filled

    threshold = (newSize * loadFactor).toInt
    mask = newSize - 1
    shift = java.lang.Long.numberOfLeadingZeros(mask.toLong).toInt

    keyTable = mk.create(newSize)
    filled = new Array[Boolean](newSize)

    if (_size > 0) {
      var i = 0
      while (i < oldCapacity) {
        if (oldFilled(i)) addResize(oldKeyTable(i))
        i += 1
      }
    }
  }

  /** Skips checks for existing keys, doesn't increment size. Used during resize. */
  private def addResize(key: A): Unit = boundary {
    var i = place(key)
    while (true) {
      if (!filled(i)) {
        keyTable(i) = key
        filled(i) = true
        boundary.break(())
      }
      i = (i + 1) & mask
    }
  }

  // --- Iteration ---

  /** Calls the given function for each element in the set. Iteration order is not guaranteed. */
  def foreach(f: A => Unit): Unit = {
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) f(keyTable(i))
      i += 1
    }
  }

  /** Returns a new DynamicArray containing all elements in the set. */
  def toArray: DynamicArray[A] = {
    val array = DynamicArray.createWithMk(mk, _size, true)
    val len   = keyTable.length
    var i     = 0
    while (i < len) {
      if (filled(i)) array.add(keyTable(i))
      i += 1
    }
    array
  }

  // --- Standard ---

  override def hashCode(): Int = {
    var h   = _size
    val len = keyTable.length
    var i   = 0
    while (i < len) {
      if (filled(i)) h += keyTable(i).hashCode()
      i += 1
    }
    h
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: ObjectSet[?] =>
      if (other eq this) true
      else if (other._size != _size) false
      else {
        val otherSet = other.asInstanceOf[ObjectSet[A]]
        val len      = keyTable.length
        var equal    = true
        var i        = 0
        while (i < len && equal) {
          if (filled(i) && !otherSet.contains(keyTable(i))) equal = false
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
          first = false
        }
        i += 1
      }
      sb.append('}')
      sb.toString()
    }

  // --- Internal accessors for OrderedSet ---

  private[utils] def internalKeyTable:  Array[A]       = keyTable
  private[utils] def internalFilled:    Array[Boolean] = filled
  private[utils] def internalMask:      Int            = mask
  private[utils] def internalShift:     Int            = shift
  private[utils] def internalThreshold: Int            = threshold
  private[utils] def internalMk:        MkArray[A]     = mk
}

object ObjectSet {

  /** Creates an ObjectSet with default capacity 51 and load factor 0.8. */
  inline def apply[A](): ObjectSet[A] = apply[A](51, 0.8f)

  /** Creates an ObjectSet with the given capacity and default load factor 0.8. */
  inline def apply[A](capacity: Int): ObjectSet[A] = apply[A](capacity, 0.8f)

  /** Creates an ObjectSet with the given capacity and load factor. */
  inline def apply[A](capacity: Int, loadFactor: Float): ObjectSet[A] = {
    val mk = summonMkArray[A]
    create(mk, capacity, loadFactor)
  }

  /** Creates an ObjectSet that is a copy of the given set. */
  def from[A](other: ObjectSet[A]): ObjectSet[A] =
    new ObjectSet[A](
      other.mk,
      other.mk.copyOf(other.keyTable, other.keyTable.length),
      java.util.Arrays.copyOf(other.filled, other.filled.length),
      other._size,
      other.mask,
      other.shift,
      other.loadFactor,
      other.threshold
    )

  private def create[A](mk: MkArray[A], capacity: Int, loadFactor: Float): ObjectSet[A] = {
    if (loadFactor <= 0f || loadFactor >= 1f)
      throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor)

    val ts        = ObjectMap.tableSize(capacity, loadFactor)
    val threshold = (ts * loadFactor).toInt
    val mask      = ts - 1
    val shift     = java.lang.Long.numberOfLeadingZeros(mask.toLong).toInt

    new ObjectSet[A](
      mk,
      mk.create(ts),
      new Array[Boolean](ts),
      0,
      mask,
      shift,
      loadFactor,
      threshold
    )
  }

  /** Creates an ObjectSet with explicit MkArray instance. For use by OrderedSet and other internal code. */
  private[utils] def createWithMk[A](mk: MkArray[A], capacity: Int, loadFactor: Float): ObjectSet[A] =
    create(mk, capacity, loadFactor)

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
