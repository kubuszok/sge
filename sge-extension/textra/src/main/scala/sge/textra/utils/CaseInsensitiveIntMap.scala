/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/CaseInsensitiveIntMap.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `GdxRuntimeException` -> `SgeError.InvalidInput`; `Array` -> `DynamicArray`;
 *     `IntArray` -> `DynamicArray[Int]`; `Category.caseUp` -> `Character.toUpperCase`;
 *     `Compatibility.imul` -> `*` (regular int multiplication)
 *   Convention: `return` -> `boundary`/`break`; internal null usage for
 *     low-level open-addressing table; braces required; split packages
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 725
 * Covenant-baseline-methods: CaseInsensitiveIntMap,Entries,Entry,Keys,MapIterator,Values,_hasNext,_keyTable,_size,_valueTable,buffer,clear,containsKey,containsValue,currentIndex,ensureCapacity,entries,entry,equals,findKey,findNextIndex,found,get,getAndIncrement,h,hasNext,hashCode,hashCodeIgnoreCase,i,i0,isEmpty,iterator,key,keys,kt,len,loadFactor,locateKey,mask,n,next,nextIndex,notEmpty,oldCapacity,oldKeyTable,oldValueTable,place,put,putAll,putResize,remove,reset,resize,shrink,size,tableSize,this,threshold,toArray,toString,ts,valid,value,values,vt
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/CaseInsensitiveIntMap.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package utils

import scala.util.boundary
import scala.util.boundary.break

import sge.utils.{ DynamicArray, SgeError }

/** An unordered map where the keys are case-insensitive Strings and the values are unboxed ints. Null keys are not allowed. No allocation is done except when growing the table size.
  *
  * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to
  * reduce collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT size.
  *
  * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using a form of random hashing; the hash seed changes every time `resize` gets called,
  * which means if resize() has to be called early due to frequent collisions, the hashes will change when the multiplier does, and that may help alleviate the collisions. Linear probing continues to
  * work even when all hashCodes collide, just more slowly.
  *
  * This implementation is closely based on `ObjectIntMap` from libGDX, but also uses ideas from jdkgdxds, such as the randomized hashing (and the case-insensitive matching in general).
  *
  * @author
  *   Nathan Sweet, Tommy Ettinger
  */
class CaseInsensitiveIntMap private (
  private[utils] var _keyTable:   Array[String],
  private[utils] var _valueTable: Array[Int],
  private[utils] var _size:       Int,
  val loadFactor:                 Float,
  private var threshold:          Int,
  private[utils] var mask:        Int
) {

  @transient private var entries1: CaseInsensitiveIntMap.Entries = null
  @transient private var entries2: CaseInsensitiveIntMap.Entries = null
  @transient private var values1:  CaseInsensitiveIntMap.Values  = null
  @transient private var values2:  CaseInsensitiveIntMap.Values  = null
  @transient private var keys1:    CaseInsensitiveIntMap.Keys    = null
  @transient private var keys2:    CaseInsensitiveIntMap.Keys    = null

  /** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before growing the backing table.
    * @param initialCapacity
    *   The backing array size is initialCapacity / loadFactor, increased to the next power of two.
    */
  def this(initialCapacity: Int, loadFactor: Float) = {
    this(
      _keyTable = null,
      _valueTable = null,
      _size = 0,
      loadFactor = {
        if (loadFactor <= 0f || loadFactor >= 1f)
          throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor)
        loadFactor
      },
      threshold = 0,
      mask = 0
    )
    val ts = CaseInsensitiveIntMap.tableSize(initialCapacity, loadFactor)
    threshold = (ts * loadFactor).toInt
    mask = ts - 1
    _keyTable = new Array[String](ts)
    _valueTable = new Array[Int](ts)
  }

  /** Creates a new map with an initial capacity of 51 and a load factor of 0.6. */
  def this() = this(51, 0.6f)

  /** Creates a new map with a load factor of 0.6.
    * @param initialCapacity
    *   The backing array size is initialCapacity / loadFactor, increased to the next power of two.
    */
  def this(initialCapacity: Int) = this(initialCapacity, 0.6f)

  /** Creates a new map and puts key-value pairs sequentially from the two given arrays until either array is exhausted. The initial capacity will be the length of the shorter of the two arrays, and
    * the load factor will be 0.6.
    */
  def this(keys: Array[String], values: Array[Int]) = {
    this(
      _keyTable = null,
      _valueTable = null,
      _size = 0,
      loadFactor = 0.6f,
      threshold = 0,
      mask = 0
    )
    val len = Math.min(keys.length, values.length)
    val ts  = CaseInsensitiveIntMap.tableSize(len, loadFactor)
    threshold = (ts * loadFactor).toInt
    mask = ts - 1
    _keyTable = new Array[String](ts)
    _valueTable = new Array[Int](ts)
    var i = 0
    while (i < len) {
      val key = keys(i)
      if (key != null) put(key, values(i))
      i += 1
    }
  }

  /** Creates a new map identical to the specified map. */
  def this(map: CaseInsensitiveIntMap) = {
    this((map._keyTable.length * map.loadFactor).toInt, map.loadFactor)
    System.arraycopy(map._keyTable, 0, _keyTable, 0, map._keyTable.length)
    System.arraycopy(map._valueTable, 0, _valueTable, 0, map._valueTable.length)
    _size = map._size
  }

  /** Returns the number of key-value pairs in this map. */
  def size: Int = _size

  /** Returns an index >= 0 and <= `mask` for the specified `item`. */
  protected def place(item: String): Int =
    CaseInsensitiveIntMap.hashCodeIgnoreCase(item, mask) & mask

  /** Returns the index of the key if already present, else ~index for the next empty index. This can be overridden in this package to compare for equality differently than `Object.equals`.
    */
  private[utils] def locateKey(key: String): Int = boundary {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null.")
    val kt = _keyTable
    var i  = place(key)
    while (true) {
      val other = kt(i)
      if (other == null) break(~i) // Empty space is available.
      if (other.equalsIgnoreCase(key)) break(i) // Same key was found.
      i = (i + 1) & mask
    }
    -1 // unreachable
  }

  def put(key: String, value: Int): Unit = {
    var i = locateKey(key)
    if (i >= 0) { // Existing key was found.
      _valueTable(i) = value
    } else {
      i = ~i // Empty space was found.
      _keyTable(i) = key
      _valueTable(i) = value
      _size += 1
      if (_size >= threshold) resize(_keyTable.length << 1)
    }
  }

  /** Returns the old value associated with the specified key, or the specified default value. */
  def put(key: String, value: Int, defaultValue: Int): Int = {
    var i = locateKey(key)
    if (i >= 0) { // Existing key was found.
      val oldValue = _valueTable(i)
      _valueTable(i) = value
      oldValue
    } else {
      i = ~i // Empty space was found.
      _keyTable(i) = key
      _valueTable(i) = value
      _size += 1
      if (_size >= threshold) resize(_keyTable.length << 1)
      defaultValue
    }
  }

  /** Puts keys with values in sequential pairs from the two arrays given, until either array is exhausted. */
  def putAll(keys: Array[String], values: Array[Int]): Unit = {
    val len = Math.min(keys.length, values.length)
    ensureCapacity(len)
    var i = 0
    while (i < len) {
      val key = keys(i)
      if (key != null) put(key, values(i))
      i += 1
    }
  }

  def putAll(map: CaseInsensitiveIntMap): Unit = {
    ensureCapacity(map._size)
    val kt = map._keyTable
    val vt = map._valueTable
    var i  = 0
    val n  = kt.length
    while (i < n) {
      val key = kt(i)
      if (key != null) put(key, vt(i))
      i += 1
    }
  }

  /** Skips checks for existing keys, doesn't increment size. */
  protected def putResize(key: String, value: Int): Unit = boundary {
    val kt = _keyTable
    var i  = place(key)
    while (true) {
      if (kt(i) == null) {
        kt(i) = key
        _valueTable(i) = value
        break(())
      }
      i = (i + 1) & mask
    }
  }

  /** Returns the value for the specified key, or the default value if the key is not in the map. */
  def get(key: String, defaultValue: Int): Int = {
    val i = locateKey(key)
    if (i < 0) defaultValue else _valueTable(i)
  }

  /** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is put into the map and defaultValue is returned.
    */
  def getAndIncrement(key: String, defaultValue: Int, increment: Int): Int = {
    var i = locateKey(key)
    if (i >= 0) { // Existing key was found.
      val oldValue = _valueTable(i)
      _valueTable(i) += increment
      oldValue
    } else {
      i = ~i // Empty space was found.
      _keyTable(i) = key
      _valueTable(i) = defaultValue + increment
      _size += 1
      if (_size >= threshold) resize(_keyTable.length << 1)
      defaultValue
    }
  }

  /** Returns the value for the removed key, or the default value if the key is not in the map. */
  def remove(key: String, defaultValue: Int): Int = {
    val i0 = locateKey(key)
    if (i0 < 0) {
      defaultValue
    } else {
      val kt       = _keyTable
      val vt       = _valueTable
      val oldValue = vt(i0)
      val m        = this.mask
      var i        = i0
      var next     = (i + 1) & m
      var k        = kt(next)
      while (k != null) {
        val placement = place(k)
        if (((next - placement) & m) > ((i - placement) & m)) {
          kt(i) = k
          vt(i) = vt(next)
          i = next
        }
        next = (next + 1) & m
        k = kt(next)
      }
      kt(i) = null
      _size -= 1
      oldValue
    }
  }

  /** Returns true if the map has one or more items. */
  def notEmpty: Boolean = _size > 0

  /** Returns true if the map is empty. */
  def isEmpty: Boolean = _size == 0

  /** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less, nothing is done. If the map contains more items than the specified
    * capacity, the next highest power of two capacity is used instead.
    */
  def shrink(maximumCapacity: Int): Unit = {
    if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity)
    val ts = CaseInsensitiveIntMap.tableSize(maximumCapacity, loadFactor)
    if (_keyTable.length > ts) resize(ts)
  }

  /** Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
    */
  def clear(maximumCapacity: Int): Unit = {
    val ts = CaseInsensitiveIntMap.tableSize(maximumCapacity, loadFactor)
    if (_keyTable.length <= ts) {
      clear()
    } else {
      _size = 0
      resize(ts)
    }
  }

  def clear(): Unit =
    if (_size == 0) {
      // nothing to do
    } else {
      _size = 0
      java.util.Arrays.fill(_keyTable.asInstanceOf[Array[Object]], null)
    }

  /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def containsValue(value: Int): Boolean = boundary {
    val kt = _keyTable
    val vt = _valueTable
    var i  = vt.length - 1
    while (i >= 0) {
      if (kt(i) != null && vt(i) == value) break(true)
      i -= 1
    }
    false
  }

  def containsKey(key: String): Boolean =
    locateKey(key) >= 0

  /** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def findKey(value: Int): String = boundary {
    val kt = _keyTable
    val vt = _valueTable
    var i  = vt.length - 1
    while (i >= 0) {
      val key = kt(i)
      if (key != null && vt(i) == value) break(key)
      i -= 1
    }
    null // matches Java API returning null when not found
  }

  /** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before adding many items to avoid multiple backing array resizes.
    */
  def ensureCapacity(additionalCapacity: Int): Unit = {
    val ts = CaseInsensitiveIntMap.tableSize(_size + additionalCapacity, loadFactor)
    if (_keyTable.length < ts) resize(ts)
  }

  final private[utils] def resize(newSize: Int): Unit = {
    val oldCapacity = _keyTable.length
    threshold = (newSize * loadFactor).toInt
    mask = newSize - 1

    val oldKeyTable   = _keyTable
    val oldValueTable = _valueTable

    _keyTable = new Array[String](newSize)
    _valueTable = new Array[Int](newSize)

    if (_size > 0) {
      var i = 0
      while (i < oldCapacity) {
        val key = oldKeyTable(i)
        if (key != null) putResize(key, oldValueTable(i))
        i += 1
      }
    }
  }

  override def hashCode(): Int = {
    var h  = _size
    val kt = _keyTable
    val vt = _valueTable
    var i  = 0
    val n  = kt.length
    while (i < n) {
      val key = kt(i)
      if (key != null) h ^= CaseInsensitiveIntMap.hashCodeIgnoreCase(key) ^ vt(i)
      i += 1
    }
    h
  }

  override def equals(obj: Any): Boolean = boundary {
    if (obj.asInstanceOf[AnyRef] eq this) break(true)
    obj match {
      case other: CaseInsensitiveIntMap =>
        if (other._size != _size) break(false)
        val kt = _keyTable
        val vt = _valueTable
        var i  = 0
        val n  = kt.length
        while (i < n) {
          val key = kt(i)
          if (key != null) {
            val otherValue = other.get(key, -1)
            if (otherValue == -1 && !other.containsKey(key)) break(false)
            if (otherValue != vt(i)) break(false)
          }
          i += 1
        }
        true
      case _ => false
    }
  }

  def toString(separator: String): String = toString(separator, braces = false)

  override def toString(): String = toString(", ", braces = true)

  def toString(separator: String, braces: Boolean): String = boundary {
    if (_size == 0) break(if (braces) "{}" else "")
    val buffer = new StringBuilder(32)
    if (braces) buffer.append('{')
    val kt = _keyTable
    val vt = _valueTable
    var i  = kt.length
    // Find first non-null entry
    var found = false
    while (!found && i > 0) {
      i -= 1
      val key = kt(i)
      if (key != null) {
        buffer.append(key)
        buffer.append('=')
        buffer.append(vt(i))
        found = true
      }
    }
    // Append remaining entries
    while (i > 0) {
      i -= 1
      val key = kt(i)
      if (key != null) {
        buffer.append(separator)
        buffer.append(key)
        buffer.append('=')
        buffer.append(vt(i))
      }
    }
    if (braces) buffer.append('}')
    buffer.toString
  }

  def iterator(): CaseInsensitiveIntMap.Entries = entries()

  /** Returns an iterator for the entries in the map. Remove is supported. Use the `Entries` constructor for nested or multithreaded iteration.
    */
  def entries(): CaseInsensitiveIntMap.Entries = {
    if (entries1 == null) {
      entries1 = new CaseInsensitiveIntMap.Entries(this)
      entries2 = new CaseInsensitiveIntMap.Entries(this)
    }
    if (!entries1.valid) {
      entries1.reset()
      entries1.valid = true
      entries2.valid = false
      entries1
    } else {
      entries2.reset()
      entries2.valid = true
      entries1.valid = false
      entries2
    }
  }

  /** Returns an iterator for the values in the map. Remove is supported. */
  def values(): CaseInsensitiveIntMap.Values = {
    if (values1 == null) {
      values1 = new CaseInsensitiveIntMap.Values(this)
      values2 = new CaseInsensitiveIntMap.Values(this)
    }
    if (!values1.valid) {
      values1.reset()
      values1.valid = true
      values2.valid = false
      values1
    } else {
      values2.reset()
      values2.valid = true
      values1.valid = false
      values2
    }
  }

  /** Returns an iterator for the keys in the map. Remove is supported. */
  def keys(): CaseInsensitiveIntMap.Keys = {
    if (keys1 == null) {
      keys1 = new CaseInsensitiveIntMap.Keys(this)
      keys2 = new CaseInsensitiveIntMap.Keys(this)
    }
    if (!keys1.valid) {
      keys1.reset()
      keys1.valid = true
      keys2.valid = false
      keys1
    } else {
      keys2.reset()
      keys2.valid = true
      keys1.valid = false
      keys2
    }
  }
}

object CaseInsensitiveIntMap {

  /** Used to establish the size of a hash table. The table size will always be a power of two, and should be the next power of two that is at least equal to `capacity / loadFactor`.
    *
    * @param capacity
    *   the amount of items the hash table should be able to hold
    * @param loadFactor
    *   between 0.0 (exclusive) and 1.0 (inclusive); the fraction of how much of the table can be filled
    * @return
    *   the size of a hash table that can handle the specified capacity with the given loadFactor
    */
  def tableSize(capacity: Int, loadFactor: Float): Int = {
    if (capacity < 0)
      throw new IllegalArgumentException("capacity must be >= 0: " + capacity)
    val ts = 1 << (-Integer.numberOfLeadingZeros(Math.max(2, Math.ceil(capacity.toDouble / loadFactor).toInt) - 1))
    if (ts > (1 << 30) || ts < 0)
      throw new IllegalArgumentException("The required capacity is too large: " + capacity)
    ts
  }

  class Entry {
    var key:   String = null
    var value: Int    = 0

    override def toString: String = key + "=" + value
  }

  private[utils] class MapIterator(val map: CaseInsensitiveIntMap) {

    /** Whether more entries remain. Subclasses check this; do not rename. */
    var _hasNext:     Boolean = false
    var nextIndex:    Int     = -1
    var currentIndex: Int     = -1
    var valid:        Boolean = true

    reset()

    def reset(): Unit = {
      currentIndex = -1
      nextIndex = -1
      findNextIndex()
    }

    def findNextIndex(): Unit = boundary {
      val kt = map._keyTable
      val n  = kt.length
      nextIndex += 1
      while (nextIndex < n) {
        if (kt(nextIndex) != null) {
          _hasNext = true
          break(())
        }
        nextIndex += 1
      }
      _hasNext = false
    }

    def remove(): Unit = {
      var i = currentIndex
      if (i < 0) throw new IllegalStateException("next must be called before remove.")
      val kt   = map._keyTable
      val vt   = map._valueTable
      val m    = map.mask
      var next = (i + 1) & m
      var key  = kt(next)
      while (key != null) {
        val placement = map.place(key)
        if (((next - placement) & m) > ((i - placement) & m)) {
          kt(i) = key
          vt(i) = vt(next)
          i = next
        }
        next = (next + 1) & m
        key = kt(next)
      }
      kt(i) = null
      map._size -= 1
      if (i != currentIndex) nextIndex -= 1
      currentIndex = -1
    }
  }

  class Entries(map: CaseInsensitiveIntMap) extends MapIterator(map) with scala.collection.Iterator[Entry] {
    private val entry = new Entry()

    /** Note the same entry instance is returned each time this method is called. */
    override def next(): Entry = {
      if (!_hasNext) throw new NoSuchElementException()
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      val kt = map._keyTable
      entry.key = kt(nextIndex)
      entry.value = map._valueTable(nextIndex)
      currentIndex = nextIndex
      findNextIndex()
      entry
    }

    override def hasNext: Boolean = {
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      _hasNext
    }
  }

  class Values(map: CaseInsensitiveIntMap) extends MapIterator(map) {

    def hasNext: Boolean = {
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      _hasNext
    }

    def next(): Int = {
      if (!_hasNext) throw new NoSuchElementException()
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      val value = map._valueTable(nextIndex)
      currentIndex = nextIndex
      findNextIndex()
      value
    }

    /** Returns a new array containing the remaining values. */
    def toArray(): DynamicArray[Int] = {
      val array = DynamicArray[Int](true, map._size)
      while (_hasNext)
        array.add(next())
      array
    }

    /** Adds the remaining values to the specified array. */
    def toArray(array: DynamicArray[Int]): DynamicArray[Int] = {
      while (_hasNext)
        array.add(next())
      array
    }
  }

  class Keys(map: CaseInsensitiveIntMap) extends MapIterator(map) with scala.collection.Iterator[String] {

    override def hasNext: Boolean = {
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      _hasNext
    }

    override def next(): String = {
      if (!_hasNext) throw new NoSuchElementException()
      if (!valid) throw SgeError.InvalidInput("#iterator() cannot be used nested.")
      val key = map._keyTable(nextIndex)
      currentIndex = nextIndex
      findNextIndex()
      key
    }

    /** Returns a new array containing the remaining keys. */
    def toArray(): DynamicArray[String] = {
      val array = DynamicArray[String](true, map._size)
      while (_hasNext)
        array.add(next())
      array
    }

    /** Adds the remaining keys to the array. */
    def toArray(array: DynamicArray[String]): DynamicArray[String] = {
      while (_hasNext)
        array.add(next())
      array
    }
  }

  /** Simple 32-bit multiplicative hashing with a tiny mix at the end. This gets the hash as if all cased letters have been converted to upper case by `Character.toUpperCase`; this should be correct
    * for all alphabets in Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing would use this with `(hashCodeIgnoreCase(text) >>> shift)` or
    * `(hashCodeIgnoreCase(text) & mask)`.
    *
    * @param data
    *   a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
    * @return
    *   an int hashCode; quality should be similarly good across any bits
    */
  def hashCodeIgnoreCase(data: CharSequence): Int = hashCodeIgnoreCase(data, 908697017)

  /** Simple 32-bit multiplicative hashing with a tiny mix at the end. This gets the hash as if all cased letters have been converted to upper case by `Character.toUpperCase`; this should be correct
    * for all alphabets in Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing would use this with `(hashCodeIgnoreCase(text, seed) >>> shift)`
    * or `(hashCodeIgnoreCase(text, seed) & mask)`.
    *
    * @param data
    *   a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
    * @param seed
    *   any int; must be the same between calls if two equivalent values for `data` must be the same
    * @return
    *   an int hashCode; quality should be similarly good across any bits
    */
  def hashCodeIgnoreCase(data: CharSequence, seedIn: Int): Int =
    if (data == null) 0
    else {
      val len  = data.length()
      var seed = seedIn ^ len
      var p    = 0
      while (p < len) {
        seed = -594347645 * (seed + Character.toUpperCase(data.charAt(p)))
        p += 1
      }
      seed ^ (seed << 27 | seed >>> 5) ^ (seed << 9 | seed >>> 23)
    }
}
