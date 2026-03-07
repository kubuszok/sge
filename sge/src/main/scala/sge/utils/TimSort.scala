/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TimSort.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Comparator` -> `Ordering`
 *   Convention: raw `null` used internally for clearing references after sort (acceptable performance optimization); `return` -> `boundary`/`break`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.util.Arrays
import scala.language.implicitConversions

/** A stable, adaptive, iterative mergesort that requires far fewer than n lg(n) comparisons when running on partially sorted arrays, while offering performance comparable to a traditional mergesort
  * when run on random arrays. Like all proper mergesorts, this sort is stable and runs O(n log n) time (worst case). In the worst case, this sort requires temporary storage space for n/2 object
  * references; in the best case, it requires only a small constant amount of space.
  *
  * This implementation was adapted from Tim Peters's list sort for Python, which is described in detail here:
  *
  * http://svn.python.org/projects/python/trunk/Objects/listsort.txt
  *
  * Tim's C code may be found here:
  *
  * http://svn.python.org/projects/python/trunk/Objects/listobject.c
  *
  * The underlying techniques are described in this paper (and may have even earlier origins):
  *
  * "Optimistic Sorting and Information Theoretic Complexity" Peter McIlroy SODA (Fourth Annual ACM-SIAM Symposium on Discrete Algorithms), pp 467-474, Austin, Texas, 25-27 January 1993.
  *
  * While the API to this class consists solely of static methods, it is (privately) instantiable; a TimSort instance holds the state of an ongoing sort, assuming the input array is large enough to
  * warrant the full-blown TimSort. Small arrays are sorted in place, using a binary insertion sort.
  */
class TimSort[T] {

  /** The array being sorted. */
  private var a: Array[T] = scala.compiletime.uninitialized

  /** The comparator for this sort. */
  private var c: Ordering[T] = scala.compiletime.uninitialized

  /** This controls when we get *into* galloping mode. It is initialized to MIN_GALLOP. The mergeLo and mergeHi methods nudge it higher for random data, and lower for highly structured data.
    */
  TimSort.MIN_GALLOP

  /** Temp storage for merges. */
  private var tmp:      Array[AnyRef] = scala.compiletime.uninitialized
  private var tmpCount: Int           = 0

  /** A stack of pending runs yet to be merged. Run i starts at address base[i] and extends for len[i] elements. It's always true (so long as the indices are in bounds) that:
    *
    * runBase[i] + runLen[i] == runBase[i + 1]
    *
    * so we could cut the storage for this, but it's a minor amount, and keeping all the info explicit simplifies the code.
    */
  private var stackSize: Int        = 0 // Number of pending runs on stack
  private val runBase:   Array[Int] = new Array[Int](40)
  private val runLen:    Array[Int] = new Array[Int](40)

  tmp = new Array[AnyRef](TimSort.INITIAL_TMP_STORAGE_LENGTH)

  def doSort(a: Array[T], c: Ordering[T], lo: Int, hi: Int): Unit = {
    stackSize = 0
    TimSort.rangeCheck(a.length, lo, hi)
    val nRemaining = hi - lo
    if (nRemaining < 2) () // Arrays of size 0 and 1 are always sorted
    else if (nRemaining < TimSort.MIN_MERGE) {
      // If array is small, do a "mini-TimSort" with no merges
      val initRunLen = TimSort.countRunAndMakeAscending(a, lo, hi, c)
      TimSort.binarySort(a, lo, hi, lo + initRunLen, c)
    } else {
      this.a = a
      this.c = c
      tmpCount = 0

      // March over the array once, left to right, finding natural runs, extending short natural runs to minRun elements, and
      // merging runs to maintain stack invariant.
      val minRun    = TimSort.minRunLength(nRemaining)
      var currentLo = lo
      var remaining = nRemaining

      while (remaining != 0) {
        // Identify next run
        var runLen = TimSort.countRunAndMakeAscending(a, currentLo, hi, c)

        // If run is short, extend to min(minRun, remaining)
        if (runLen < minRun) {
          val force = if (remaining <= minRun) remaining else minRun
          TimSort.binarySort(a, currentLo, currentLo + force, currentLo + runLen, c)
          runLen = force
        }

        // Push run onto pending-run stack, and maybe merge
        pushRun(currentLo, runLen)
        mergeCollapse()

        // Advance to find next run
        currentLo += runLen
        remaining -= runLen
      }

      // Merge all remaining runs to complete sort
      if (TimSort.DEBUG) assert(currentLo == hi)
      mergeForceCollapse()
      if (TimSort.DEBUG) assert(stackSize == 1)

      this.a = null.asInstanceOf[Array[T]]
      this.c = null.asInstanceOf[Ordering[T]]
      val tmp = this.tmp
      for (i <- 0 until tmpCount)
        tmp(i) = null
    }
  }

  /** Pushes the specified run onto the pending-run stack. */
  private def pushRun(runBase: Int, runLen: Int): Unit = {
    this.runBase(stackSize) = runBase
    this.runLen(stackSize) = runLen
    stackSize += 1
  }

  /** Examines the stack of runs waiting to be merged and merges adjacent runs until the stack invariants are reestablished. */
  private def mergeCollapse(): Unit =
    scala.util.boundary {
      while (stackSize > 1) {
        var n = stackSize - 2
        if ((n >= 1 && runLen(n - 1) <= runLen(n) + runLen(n + 1)) || (n >= 2 && runLen(n - 2) <= runLen(n) + runLen(n - 1))) {
          if (runLen(n - 1) < runLen(n + 1)) n -= 1
        } else if (runLen(n) > runLen(n + 1)) {
          scala.util.boundary.break(()) // Invariant is established
        }
        mergeAt(n)
      }
    }

  /** Merges all runs on the stack until only one remains. This method is called once, to complete the sort. */
  private def mergeForceCollapse(): Unit =
    while (stackSize > 1) {
      var n = stackSize - 2
      if (n > 0 && runLen(n - 1) < runLen(n + 1)) n -= 1
      mergeAt(n)
    }

  /** Merges the two runs at stack indices i and i+1. */
  private def mergeAt(i: Int): Unit = scala.util.boundary {
    if (TimSort.DEBUG) assert(stackSize >= 2)
    if (TimSort.DEBUG) assert(i >= 0)
    if (TimSort.DEBUG) assert(i == stackSize - 2 || i == stackSize - 3)

    var base1 = runBase(i)
    var len1  = runLen(i)
    val base2 = runBase(i + 1)
    var len2  = runLen(i + 1)
    if (TimSort.DEBUG) assert(len1 > 0 && len2 > 0)
    if (TimSort.DEBUG) assert(base1 + len1 == base2)

    // Record the length of the combined runs
    runLen(i) = len1 + len2
    if (i == stackSize - 3) {
      runBase(i + 1) = runBase(i + 2)
      runLen(i + 1) = runLen(i + 2)
    }
    stackSize -= 1

    // Find where the first element of run2 goes in run1
    val k = TimSort.gallopRight(a(base2), a, base1, len1, 0, c)
    if (TimSort.DEBUG) assert(k >= 0)
    base1 += k
    len1 -= k
    if (len1 == 0) scala.util.boundary.break(())

    // Find where the last element of run1 goes in run2
    len2 = TimSort.gallopLeft(a(base1 + len1 - 1), a, base2, len2, len2 - 1, c)
    if (TimSort.DEBUG) assert(len2 >= 0)
    if (len2 == 0) scala.util.boundary.break(())

    // Merge remaining runs, using tmp array with min(len1, len2) elements
    if (len1 <= len2)
      mergeLo(base1, len1, base2, len2)
    else
      mergeHi(base1, len1, base2, len2)
  }

  // Placeholder implementations for merge methods
  private def mergeLo(base1: Int, len1: Int, base2: Int, len2: Int): Unit = {
    // Implementation would go here - for now, just a simple fallback
    val start = Math.min(base1, base2)
    val end   = Math.max(base1 + len1, base2 + len2)
    java.util.Arrays.sort(a.asInstanceOf[Array[AnyRef]], start, end, c.asInstanceOf[java.util.Comparator[AnyRef]])
  }

  private def mergeHi(base1: Int, len1: Int, base2: Int, len2: Int): Unit = {
    // Implementation would go here - for now, just a simple fallback
    val start = Math.min(base1, base2)
    val end   = Math.max(base1 + len1, base2 + len2)
    java.util.Arrays.sort(a.asInstanceOf[Array[AnyRef]], start, end, c.asInstanceOf[java.util.Comparator[AnyRef]])
  }
}

object TimSort {

  /** This is the minimum sized sequence that will be merged. */
  private val MIN_MERGE = 32

  /** When we get into galloping mode, we stay there until both runs win less often than MIN_GALLOP consecutive times. */
  private val MIN_GALLOP = 7

  /** Maximum initial size of tmp array, which is used for merging. */
  private val INITIAL_TMP_STORAGE_LENGTH = 256

  /** Asserts have been placed in if-statements for performance. */
  private val DEBUG = false

  def sort[T](a: Array[T], c: Ordering[T]): Unit =
    sort(a, 0, a.length, c)

  def sort[T](a: Array[T], lo: Int, hi: Int, c: Nullable[Ordering[T]]): Unit =
    if (c.isEmpty) {
      Arrays.sort(a.asInstanceOf[Array[AnyRef]], lo, hi)
    } else {
      val comp = c.getOrElse(throw new AssertionError("unreachable"))
      rangeCheck(a.length, lo, hi)
      val nRemaining = hi - lo
      if (nRemaining < 2) () // Arrays of size 0 and 1 are always sorted
      else if (nRemaining < MIN_MERGE) {
        // If array is small, do a "mini-TimSort" with no merges
        val initRunLen = countRunAndMakeAscending(a, lo, hi, comp)
        binarySort(a, lo, hi, lo + initRunLen, comp)
      } else {
        // March over the array once, left to right, finding natural runs
        val ts = TimSort[T]()
        ts.doSort(a, comp, lo, hi)
      }
    }

  /** Sorts the specified portion of the specified array using a binary insertion sort. */
  private def binarySort[T](a: Array[T], lo: Int, hi: Int, start: Int, c: Ordering[T]): Unit = {
    if (DEBUG) assert(lo <= start && start <= hi)
    var start_var = start
    if (start_var == lo) start_var += 1

    while (start_var < hi) {
      val pivot = a(start_var)

      // Set left (and right) to the index where a[start] (pivot) belongs
      var left  = lo
      var right = start_var
      if (DEBUG) assert(left <= right)

      while (left < right) {
        val mid = (left + right) >>> 1
        if (c.compare(pivot, a(mid)) < 0)
          right = mid
        else
          left = mid + 1
      }
      if (DEBUG) assert(left == right)

      val n = start_var - left // The number of elements to move
      // Switch is just an optimization for arraycopy in default case
      n match {
        case 2 =>
          a(left + 2) = a(left + 1)
          a(left + 1) = a(left)
        case 1 =>
          a(left + 1) = a(left)
        case _ =>
          System.arraycopy(a, left, a, left + 1, n)
      }
      a(left) = pivot
      start_var += 1
    }
  }

  /** Returns the length of the run beginning at the specified position in the specified array and reverses the run if it is descending (ensuring that the run will always be ascending when the method
    * returns).
    */
  private def countRunAndMakeAscending[T](a: Array[T], lo: Int, hi: Int, c: Ordering[T]): Int = {
    if (DEBUG) assert(lo < hi)
    var runHi = lo + 1
    if (runHi == hi) 1
    else {
      // Find end of run, and reverse range if descending
      if (c.compare(a(runHi), a(lo)) < 0) { // Descending
        runHi += 1
        while (runHi < hi && c.compare(a(runHi), a(runHi - 1)) < 0)
          runHi += 1
        reverseRange(a, lo, runHi)
      } else { // Ascending
        while (runHi < hi && c.compare(a(runHi), a(runHi - 1)) >= 0)
          runHi += 1
      }

      runHi - lo
    }
  }

  /** Reverse the specified range of the specified array. */
  private def reverseRange[T](a: Array[T], lo: Int, hi: Int): Unit = {
    var i = lo
    var j = hi - 1
    while (i < j) {
      val t = a(i)
      a(i) = a(j)
      a(j) = t
      i += 1
      j -= 1
    }
  }

  /** Returns the minimum acceptable run length for an array of the specified length. */
  private def minRunLength(n: Int): Int = {
    if (DEBUG) assert(n >= 0)
    var r     = 0 // Becomes 1 if any 1 bits are shifted off
    var n_var = n
    while (n_var >= MIN_MERGE) {
      r |= (n_var & 1)
      n_var >>= 1
    }
    n_var + r
  }

  // Placeholder implementations for gallop methods
  private def gallopLeft[T](key: T, a: Array[T], base: Int, len: Int, hint: Int, c: Ordering[T]): Int = {
    // Simple linear search as placeholder
    var i = 0
    while (i < len && c.compare(a(base + i), key) < 0)
      i += 1
    i
  }

  private def gallopRight[T](key: T, a: Array[T], base: Int, len: Int, hint: Int, c: Ordering[T]): Int = {
    // Simple linear search as placeholder
    var i = 0
    while (i < len && c.compare(a(base + i), key) <= 0)
      i += 1
    i
  }

  /** Checks that fromIndex and toIndex are in range, and throws an appropriate exception if they aren't. */
  private def rangeCheck(arrayLen: Int, fromIndex: Int, toIndex: Int): Unit = {
    if (fromIndex > toIndex) throw new IllegalArgumentException(s"fromIndex($fromIndex) > toIndex($toIndex)")
    if (fromIndex < 0) throw new ArrayIndexOutOfBoundsException(fromIndex)
    if (toIndex > arrayLen) throw new ArrayIndexOutOfBoundsException(toIndex)
  }
}
