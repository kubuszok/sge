/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Array.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import scala.annotation.targetName
import scala.compiletime.summonFrom
import scala.util.boundary

/** Resizable array that avoids boxing for primitive types via the `MkArray` type class.
  *
  * Replaces all libGDX Array variants (`Array`, `IntArray`, `FloatArray`, `CharArray`, etc.) and includes built-in snapshot (copy-on-write) semantics for safe iteration during mutation.
  *
  * `preserveOrder` controls removal behavior:
  *   - `true` (default): removals shift elements left (O(n)), maintaining insertion order
  *   - `false`: removals swap the removed element with the last element (O(1)), changing order
  *
  * @param mk
  *   The `MkArray` type class instance, stored for runtime array operations
  * @param _items
  *   The backing array
  * @param _size
  *   The number of elements currently stored
  * @param preserveOrder
  *   Whether to preserve element ordering on removal
  */
final class DynamicArray[A] private (
  private val mk:     MkArray[A],
  private var _items: Array[A],
  private var _size:  Int,
  val preserveOrder:  Boolean
) {

  // Snapshot state — raw null for internal perf; never exposed via public API.
  // NOTE: begin()/end() is a resource-scoping pattern that could benefit from
  // capture calculus in the future (Scala capture checking / CC). Worth revisiting
  // when CC matures — it would allow the compiler to statically ensure that the
  // snapshot scope is properly closed.
  private var snapshot:  Array[A] = null.asInstanceOf[Array[A]]
  private var recycled:  Array[A] = null.asInstanceOf[Array[A]]
  private var snapshots: Int      = 0

  // --- Fields (direct access for zero-boxing hot paths) ---

  /** The backing array. Use `items(i)` for indexed access. May be longer than `size`. */
  def items: Array[A] = _items

  /** The number of elements currently stored. */
  def size: Int = _size

  // --- Element access ---

  /** Returns the element at the given index. Bounds-checked. */
  def apply(index: Int): A = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    _items(index)
  }

  /** Sets the element at the given index. Bounds-checked. */
  def update(index: Int, value: A): Unit = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    modified()
    _items(index) = value
  }

  // --- Adding ---

  /** Appends a single element. */
  def add(value: A): Unit = {
    modified()
    if (_size == _items.length) grow(1)
    _items(_size) = value
    _size += 1
  }

  /** Appends two elements, avoiding varargs boxing. */
  def add(v1: A, v2: A): Unit = {
    modified()
    if (_size + 2 > _items.length) grow(2)
    _items(_size) = v1
    _items(_size + 1) = v2
    _size += 2
  }

  /** Appends three elements, avoiding varargs boxing. */
  def add(v1: A, v2: A, v3: A): Unit = {
    modified()
    if (_size + 3 > _items.length) grow(3)
    _items(_size) = v1
    _items(_size + 1) = v2
    _items(_size + 2) = v3
    _size += 3
  }

  /** Appends all elements from another DynamicArray. */
  def addAll(other: DynamicArray[? <: A]): Unit = {
    val count = other._size
    modified()
    if (_size + count > _items.length) grow(count)
    // System.arraycopy takes Object, so variance is not an issue at runtime
    System.arraycopy(other._items, 0, _items, _size, count)
    _size += count
  }

  /** Appends elements from a plain array. */
  def addAll(array: Array[A], start: Int, count: Int): Unit = {
    modified()
    if (_size + count > _items.length) grow(count)
    System.arraycopy(array, start, _items, _size, count)
    _size += count
  }

  /** Inserts an element at the given index. Behavior depends on `preserveOrder`. */
  def insert(index: Int, value: A): Unit = {
    if (index > _size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + _size)
    modified()
    if (_size == _items.length) grow(1)
    if (preserveOrder) {
      System.arraycopy(_items, index, _items, index + 1, _size - index)
    } else {
      _items(_size) = _items(index)
    }
    _items(index) = value
    _size += 1
  }

  /** Inserts empty slots at the given index, shifting elements right. */
  def insertRange(index: Int, count: Int): Unit = {
    if (index > _size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + _size)
    modified()
    if (_size + count > _items.length) grow(count)
    System.arraycopy(_items, index, _items, index + count, _size - index)
    _size += count
  }

  // --- Removing ---

  /** Removes the element at the given index and returns it. If `preserveOrder`, shifts elements left (O(n)). Otherwise swaps with last (O(1)).
    */
  def removeIndex(index: Int): A = {
    if (index >= _size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + _size)
    modified()
    val value = _items(index)
    _size -= 1
    if (preserveOrder) {
      System.arraycopy(_items, index + 1, _items, index, _size - index)
    } else {
      _items(index) = _items(_size)
    }
    value
  }

  /** Removes the first element matching `value` using `==`. Returns true if found. */
  def removeValue(value: A): Boolean = boundary {
    var i = 0
    while (i < _size) {
      if (_items(i) == value) {
        removeIndex(i)
        boundary.break(true)
      }
      i += 1
    }
    false
  }

  /** Removes the first element matching `value` using reference identity (`eq`). Returns true if found. */
  def removeValueByRef(value: A): Boolean = boundary {
    var i = 0
    while (i < _size) {
      if (_items(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) {
        removeIndex(i)
        boundary.break(true)
      }
      i += 1
    }
    false
  }

  /** Removes elements in the range [start, end). */
  def removeRange(start: Int, end: Int): Unit = {
    val n = end - start
    if (n > 0) {
      if (end > _size) throw new IndexOutOfBoundsException("end can't be > size: " + end + " > " + _size)
      modified()
      if (preserveOrder) {
        System.arraycopy(_items, end, _items, start, _size - end)
      } else {
        val remaining = _size - end
        val copyCount = Math.min(remaining, n)
        System.arraycopy(_items, _size - copyCount, _items, start, copyCount)
      }
      _size -= n
    }
  }

  /** Removes all elements found in `other` using `==`. Returns true if any were removed. */
  def removeAll(other: DynamicArray[? <: A]): Boolean = {
    // Safe cast: we only read from other for equality comparison via ==
    val check   = other.asInstanceOf[DynamicArray[A]]
    var changed = false
    var i       = _size - 1
    while (i >= 0) {
      if (check.contains(_items(i))) {
        removeIndex(i)
        changed = true
      }
      i -= 1
    }
    changed
  }

  /** Removes all elements found in `other` using reference identity (`eq`). Returns true if any were removed. */
  def removeAllByRef(other: DynamicArray[? <: A]): Boolean = {
    // Safe cast: we only read from other for reference comparison via eq
    val check   = other.asInstanceOf[DynamicArray[A]]
    var changed = false
    var i       = _size - 1
    while (i >= 0) {
      if (check.containsByRef(_items(i))) {
        removeIndex(i)
        changed = true
      }
      i -= 1
    }
    changed
  }

  /** Removes and returns the last element. */
  def pop(): A = {
    if (_size == 0) throw new IndexOutOfBoundsException("Array is empty.")
    modified()
    _size -= 1
    _items(_size)
  }

  /** Removes all elements. */
  def clear(): Unit = {
    modified()
    _size = 0
  }

  /** Reduces the size to at most `newSize`, discarding trailing elements. */
  def truncate(newSize: Int): Unit =
    if (newSize < _size) {
      modified()
      _size = newSize
    }

  // --- Search ---

  /** Returns true if `value` is found using `==`. */
  def contains(value: A): Boolean = indexOf(value) >= 0

  /** Returns true if `value` is found using reference identity (`eq`). */
  def containsByRef(value: A): Boolean = indexOfByRef(value) >= 0

  /** Returns true if all elements in `values` are found using `==`. */
  def containsAll(values: DynamicArray[? <: A]): Boolean = boundary {
    var i = values._size - 1
    while (i >= 0) {
      if (!contains(values._items(i).asInstanceOf[A])) boundary.break(false)
      i -= 1
    }
    true
  }

  /** Returns true if all elements in `values` are found using reference identity (`eq`). */
  def containsAllByRef(values: DynamicArray[? <: A]): Boolean = boundary {
    var i = values._size - 1
    while (i >= 0) {
      if (!containsByRef(values._items(i).asInstanceOf[A])) boundary.break(false)
      i -= 1
    }
    true
  }

  /** Returns true if any element in `values` is found using `==`. */
  def containsAny(values: DynamicArray[? <: A]): Boolean = boundary {
    var i = values._size - 1
    while (i >= 0) {
      if (contains(values._items(i).asInstanceOf[A])) boundary.break(true)
      i -= 1
    }
    false
  }

  /** Returns true if any element in `values` is found using reference identity (`eq`). */
  def containsAnyByRef(values: DynamicArray[? <: A]): Boolean = boundary {
    var i = values._size - 1
    while (i >= 0) {
      if (containsByRef(values._items(i).asInstanceOf[A])) boundary.break(true)
      i -= 1
    }
    false
  }

  /** Returns the index of the first element matching `value` using `==`, or -1 if not found. */
  def indexOf(value: A): Int = boundary {
    var i = 0
    while (i < _size) {
      if (_items(i) == value) boundary.break(i)
      i += 1
    }
    -1
  }

  /** Returns the index of the first element matching `value` using reference identity (`eq`), or -1. */
  def indexOfByRef(value: A): Int = boundary {
    var i = 0
    while (i < _size) {
      if (_items(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) boundary.break(i)
      i += 1
    }
    -1
  }

  /** Returns the index of the last element matching `value` using `==`, or -1 if not found. */
  def lastIndexOf(value: A): Int = boundary {
    var i = _size - 1
    while (i >= 0) {
      if (_items(i) == value) boundary.break(i)
      i -= 1
    }
    -1
  }

  /** Returns the index of the last element matching `value` using reference identity (`eq`), or -1. */
  def lastIndexOfByRef(value: A): Int = boundary {
    var i = _size - 1
    while (i >= 0) {
      if (_items(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) boundary.break(i)
      i -= 1
    }
    -1
  }

  // --- Replace ---

  /** Replaces the first occurrence of `value` with `replacement` using `==`. Returns true if replaced. */
  def replaceFirst(value: A, replacement: A): Boolean = {
    val i = indexOf(value)
    if (i >= 0) {
      modified()
      _items(i) = replacement
      true
    } else {
      false
    }
  }

  /** Replaces the first occurrence of `value` with `replacement` using `eq`. Returns true if replaced. */
  def replaceFirstByRef(value: A, replacement: A): Boolean = {
    val i = indexOfByRef(value)
    if (i >= 0) {
      modified()
      _items(i) = replacement
      true
    } else {
      false
    }
  }

  /** Replaces all occurrences of `value` with `replacement` using `==`. Returns count of replacements. */
  def replaceAll(value: A, replacement: A): Int = {
    var count = 0
    var i     = 0
    while (i < _size) {
      if (_items(i) == value) {
        if (count == 0) modified()
        _items(i) = replacement
        count += 1
      }
      i += 1
    }
    count
  }

  /** Replaces all occurrences of `value` with `replacement` using `eq`. Returns count of replacements. */
  def replaceAllByRef(value: A, replacement: A): Int = {
    var count = 0
    var i     = 0
    while (i < _size) {
      if (_items(i).asInstanceOf[AnyRef] eq value.asInstanceOf[AnyRef]) {
        if (count == 0) modified()
        _items(i) = replacement
        count += 1
      }
      i += 1
    }
    count
  }

  // --- Reorder/Transform ---

  /** Swaps the elements at the two given indices. */
  def swap(first: Int, second: Int): Unit = {
    if (first >= _size) throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + _size)
    if (second >= _size) throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + _size)
    modified()
    val tmp = _items(first)
    _items(first) = _items(second)
    _items(second) = tmp
  }

  /** Reverses the order of all elements. */
  def reverse(): Unit = {
    modified()
    var i = 0
    var j = _size - 1
    while (i < j) {
      val tmp = _items(i)
      _items(i) = _items(j)
      _items(j) = tmp
      i += 1
      j -= 1
    }
  }

  /** Shuffles elements randomly using `MathUtils.random`. */
  def shuffle(): Unit = {
    modified()
    var i = _size - 1
    while (i > 0) {
      val ii  = math.MathUtils.random(i)
      val tmp = _items(i)
      _items(i) = _items(ii)
      _items(ii) = tmp
      i -= 1
    }
  }

  /** Sorts elements using the provided `Ordering`. Delegates to `Sort.sort`. */
  def sort()(using ordering: Ordering[A]): Unit = {
    modified()
    Sort.sort(_items, ordering, 0, _size)
  }

  /** Sorts elements using an explicit `Ordering`. */
  @targetName("sortWith")
  def sort(ordering: Ordering[A]): Unit = {
    modified()
    Sort.sort(_items, ordering, 0, _size)
  }

  // --- Size management ---

  /** Ensures capacity for at least `additional` more elements beyond current size. */
  def ensureCapacity(additional: Int): Unit = {
    val needed = _size + additional
    if (needed > _items.length) {
      _items = mk.copyOf(_items, needed)
    }
  }

  /** Sets the size to `newSize`. If larger than current, expands the backing array. */
  def setSize(newSize: Int): Unit = {
    modified()
    if (newSize > _items.length) {
      _items = mk.copyOf(_items, Math.max(8, newSize))
    }
    _size = newSize
  }

  /** Trims the backing array to exactly `size` elements. */
  def shrink(): Unit =
    if (_items.length != _size) {
      _items = mk.copyOf(_items, _size)
    }

  // --- Snapshot (copy-on-write for safe iteration during mutation) ---

  /** Starts a snapshot. Returns the current backing array for iteration.
    *
    * Callers must pair each `begin()` with a corresponding `end()` call.
    *
    * NOTE: This begin()/end() scoping pattern is a candidate for capture calculus (Scala CC) once it matures — the compiler could statically ensure proper scoping.
    */
  def begin(): Array[A] = {
    snapshot = _items
    snapshots += 1
    _items
  }

  /** Ends a snapshot. If items were copied during the snapshot, the old array is recycled. */
  def end(): Unit = {
    snapshots -= 1
    if (snapshot == null) {
      // No snapshot was active — nothing to do
    } else if (snapshot ne _items) {
      // Items were copied — recycle the old snapshot array
      if (recycled == null || recycled.length < snapshot.length) {
        recycled = snapshot
      }
      snapshot = null.asInstanceOf[Array[A]]
    } else if (snapshots == 0) {
      snapshot = null.asInstanceOf[Array[A]]
    }
  }

  // --- Conversion ---

  /** Returns a fresh copy of the elements as a plain array. */
  def toArray: Array[A] = mk.copyOfRange(_items, 0, _size)

  /** Returns the first element. Throws if empty. */
  def first: A = {
    if (_size == 0) throw new IndexOutOfBoundsException("Array is empty.")
    _items(0)
  }

  /** Returns the last element. Throws if empty. */
  def last: A = {
    if (_size == 0) throw new IndexOutOfBoundsException("Array is empty.")
    _items(_size - 1)
  }

  /** Alias for `last`. Returns the last element. */
  def peek: A = last

  /** Returns a random element, or `Nullable.empty` if the array is empty. */
  def random(): Nullable[A] =
    if (_size == 0) Nullable.empty[A]
    else Nullable(_items(math.MathUtils.random(_size - 1)))

  def isEmpty: Boolean = _size == 0

  def nonEmpty: Boolean = _size != 0

  // --- Standard ---

  override def hashCode(): Int =
    if (!preserveOrder) super.hashCode()
    else {
      var h = 1
      var i = 0
      while (i < _size) {
        val elem = _items(i)
        h = 31 * h + (if (elem.asInstanceOf[AnyRef] == null) 0 else elem.hashCode())
        i += 1
      }
      h
    }

  override def equals(obj: Any): Boolean = obj match {
    case other: DynamicArray[?] =>
      if (!preserveOrder || !other.preserveOrder) this eq other
      else if (_size != other._size) false
      else {
        var i     = 0
        var equal = true
        while (i < _size && equal) {
          if (_items(i) != other._items(i)) equal = false
          i += 1
        }
        equal
      }
    case _ => false
  }

  override def toString(): String = toString(", ")

  def toString(separator: String): String =
    if (_size == 0) "[]"
    else {
      val sb = new StringBuilder()
      sb.append('[')
      sb.append(_items(0))
      var i = 1
      while (i < _size) {
        sb.append(separator)
        sb.append(_items(i))
        i += 1
      }
      sb.append(']')
      sb.toString()
    }

  // --- Internal ---

  /** Called before any mutation. If a snapshot is active and the items array is the snapshot, copies the items array so the snapshot remains untouched.
    */
  private def modified(): Unit =
    if (snapshot == null || (snapshot ne _items)) {
      // No snapshot active, or items were already copied — nothing to do
    } else {
      // Items array is the snapshot — must copy before mutating
      if (recycled != null && recycled.length >= _size) {
        System.arraycopy(_items, 0, recycled, 0, _size)
        _items = recycled
        recycled = null.asInstanceOf[Array[A]]
      } else {
        _items = mk.copyOf(_items, _items.length)
      }
    }

  private def grow(additional: Int): Unit = {
    val needed      = _size + additional
    val newCapacity = Math.max(8, Math.max(needed, (_size * 1.75).toInt))
    _items = mk.copyOf(_items, newCapacity)
  }
}

object DynamicArray {

  /** Creates an ordered DynamicArray with default capacity 16. */
  inline def apply[A](): DynamicArray[A] = apply[A](true, 16)

  /** Creates an ordered DynamicArray with the given capacity. */
  inline def apply[A](capacity: Int): DynamicArray[A] = apply[A](true, capacity)

  /** Creates a DynamicArray with the given order mode and capacity. */
  inline def apply[A](preserveOrder: Boolean, capacity: Int): DynamicArray[A] = {
    val mk = summonMkArray[A]
    create(mk, capacity, preserveOrder)
  }

  /** Creates a DynamicArray by copying elements from a plain array. */
  inline def from[A](array: Array[A]): DynamicArray[A] = {
    val mk = summonMkArray[A]
    copyFrom(mk, array)
  }

  /** Creates a DynamicArray by copying elements from another DynamicArray. */
  def from[A](other: DynamicArray[A]): DynamicArray[A] = {
    val mk    = other.mk
    val items = mk.copyOf(other._items, other._items.length)
    new DynamicArray[A](mk, items, other._size, other.preserveOrder)
  }

  /** Creates a DynamicArray that wraps an existing array (no copy). */
  inline def wrap[A](array: Array[A]): DynamicArray[A] = {
    val mk = summonMkArray[A]
    wrapWith(mk, array)
  }

  /** Creates a DynamicArray with an explicit MkArray instance. For use by collection internals. */
  private[utils] def createWithMk[A](mk: MkArray[A], capacity: Int, preserveOrder: Boolean): DynamicArray[A] =
    create(mk, capacity, preserveOrder)

  // Non-inline helpers to avoid @publicInBinary requirement on the private constructor
  private def create[A](mk: MkArray[A], capacity: Int, preserveOrder: Boolean): DynamicArray[A] =
    new DynamicArray[A](mk, mk.create(capacity), 0, preserveOrder)

  private def copyFrom[A](mk: MkArray[A], array: Array[A]): DynamicArray[A] = {
    val items = mk.copyOf(array, array.length)
    new DynamicArray[A](mk, items, array.length, true)
  }

  private def wrapWith[A](mk: MkArray[A], array: Array[A]): DynamicArray[A] =
    new DynamicArray[A](mk, array, array.length, true)

  /** Resolves MkArray at compile time using summonFrom. */
  private inline def summonMkArray[A]: MkArray[A] = summonFrom { case mk: MkArray[A] =>
    mk
  }
}
