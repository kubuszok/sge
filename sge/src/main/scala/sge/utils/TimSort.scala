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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 767
 * Covenant-baseline-methods: DEBUG,INITIAL_TMP_STORAGE_LENGTH,MIN_GALLOP,MIN_MERGE,TimSort,a,base1,base2,binarySort,c,countRunAndMakeAscending,cursor1,cursor2,dest,doSort,done,ensureCapacity,gallopLeft,gallopRight,i,j,k,lastOfs,len1,len2,localMinGallop,mergeAt,mergeCollapse,mergeForceCollapse,mergeHi,mergeLo,minGallop,minRunLength,nRemaining,n_var,ofs,pushRun,r,rangeCheck,reverseRange,runBase,runHi,runLen,sort,stackSize,start_var,tmp,tmpCount
 * Covenant-source-reference: com/badlogic/gdx/utils/TimSort.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package utils

import java.util.Arrays
import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

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
final class TimSort[T] {

  /** The array being sorted. */
  private var a: Array[T] = scala.compiletime.uninitialized

  /** The comparator for this sort. */
  private var c: Ordering[T] = scala.compiletime.uninitialized

  /** This controls when we get *into* galloping mode. It is initialized to MIN_GALLOP. The mergeLo and mergeHi methods nudge it higher for random data, and lower for highly structured data.
    */
  private var minGallop: Int = TimSort.MIN_GALLOP

  /** Temp storage for merges. Allocated lazily to match the component type of the array being sorted, so that System.arraycopy works correctly for both primitive and reference arrays. */
  private var tmp:      Array[T] = scala.compiletime.uninitialized
  private var tmpCount: Int      = 0

  /** A stack of pending runs yet to be merged. Run i starts at address base[i] and extends for len[i] elements. It's always true (so long as the indices are in bounds) that:
    *
    * runBase[i] + runLen[i] == runBase[i + 1]
    *
    * so we could cut the storage for this, but it's a minor amount, and keeping all the info explicit simplifies the code.
    */
  private var stackSize: Int        = 0 // Number of pending runs on stack
  private val runBase:   Array[Int] = new Array[Int](40)
  private val runLen:    Array[Int] = new Array[Int](40)

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
      // Allocate tmp with same component type as source array so System.arraycopy works for primitives
      if (tmp == null || tmp.getClass.getComponentType != a.getClass.getComponentType)
        tmp = java.lang.reflect.Array.newInstance(a.getClass.getComponentType, TimSort.INITIAL_TMP_STORAGE_LENGTH).asInstanceOf[Array[T]]

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
      // Clear temp storage for GC — only needed for reference arrays (primitive arrays don't hold references)
      if (!this.tmp.getClass.getComponentType.isPrimitive) {
        val tmp = this.tmp
        for (i <- 0 until tmpCount)
          tmp(i) = null.asInstanceOf[T]
      }
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

  /** Ensures that the external array tmp has at least the specified number of elements, increasing its size if necessary. The size increases exponentially to ensure amortized linear time complexity.
    *
    * @param minCapacity
    *   the minimum required capacity of the tmp array
    * @return
    *   tmp, whether or not it grew
    */
  private def ensureCapacity(minCapacity: Int): Array[T] = {
    tmpCount = Math.max(tmpCount, minCapacity)
    if (tmp.length < minCapacity) {
      // Compute smallest power of 2 > minCapacity
      var newSize = minCapacity
      newSize |= newSize >> 1
      newSize |= newSize >> 2
      newSize |= newSize >> 4
      newSize |= newSize >> 8
      newSize |= newSize >> 16
      newSize += 1

      if (newSize < 0) // Not bloody likely!
        newSize = minCapacity
      else
        newSize = Math.min(newSize, a.length >>> 1)

      tmp = java.lang.reflect.Array.newInstance(a.getClass.getComponentType, newSize).asInstanceOf[Array[T]]
    }
    tmp
  }

  /** Merges two adjacent runs in place, in a stable fashion. The first element of the first run must be greater than the first element of the second run (a[base1] > a[base2]), and the last element of
    * the first run (a[base1 + len1-1]) must be greater than all elements of the second run.
    *
    * For performance, this method should be called only when len1 <= len2; its twin, mergeHi should be called if len1 >= len2. (Either method may be called if len1 == len2.)
    *
    * @param _base1
    *   index of first element in first run to be merged
    * @param _len1
    *   length of first run to be merged (must be > 0)
    * @param base2
    *   index of first element in second run to be merged (must be aBase + aLen)
    * @param _len2
    *   length of second run to be merged (must be > 0)
    */
  private def mergeLo(_base1: Int, _len1: Int, base2: Int, _len2: Int): Unit = boundary {
    if (TimSort.DEBUG) assert(_len1 > 0 && _len2 > 0 && _base1 + _len1 == base2)

    var len1 = _len1
    var len2 = _len2

    // Copy first run into temp array
    val a   = this.a // For performance
    val tmp = ensureCapacity(len1)
    System.arraycopy(a, _base1, tmp, 0, len1)

    var cursor1 = 0 // Indexes into tmp array
    var cursor2 = base2 // Indexes int a
    var dest    = _base1 // Indexes int a

    // Move first element of second run and deal with degenerate cases
    a(dest) = a(cursor2); dest += 1; cursor2 += 1
    len2 -= 1
    if (len2 == 0) {
      System.arraycopy(tmp, cursor1, a, dest, len1)
      break()
    }
    if (len1 == 1) {
      System.arraycopy(a, cursor2, a, dest, len2)
      a(dest + len2) = tmp(cursor1) // Last elt of run 1 to end of merge
      break()
    }

    val c              = this.c // Use local variable for performance
    var localMinGallop = this.minGallop // " " " " "
    var done           = false
    while (!done) {
      var count1 = 0 // Number of times in a row that first run won
      var count2 = 0 // Number of times in a row that second run won

      /*
       * Do the straightforward thing until (if ever) one run starts winning consistently.
       */
      var breakOuter = false
      var doContinue = true
      while (doContinue) {
        if (TimSort.DEBUG) assert(len1 > 1 && len2 > 0)
        if (c.compare(a(cursor2), tmp(cursor1)) < 0) {
          a(dest) = a(cursor2); dest += 1; cursor2 += 1
          count2 += 1
          count1 = 0
          len2 -= 1
          if (len2 == 0) { breakOuter = true; doContinue = false }
        } else {
          a(dest) = tmp(cursor1); dest += 1; cursor1 += 1
          count1 += 1
          count2 = 0
          len1 -= 1
          if (len1 == 1) { breakOuter = true; doContinue = false }
        }
        if (doContinue && !((count1 | count2) < localMinGallop)) doContinue = false
      }
      if (breakOuter) { done = true }

      /*
       * One run is winning so consistently that galloping may be a huge win. So try that, and continue galloping until (if
       * ever) neither run appears to be winning consistently anymore.
       */
      if (!done) {
        var gallopContinue = true
        while (gallopContinue) {
          if (TimSort.DEBUG) assert(len1 > 1 && len2 > 0)
          count1 = TimSort.gallopRight(a(cursor2), tmp, cursor1, len1, 0, c)
          if (count1 != 0) {
            System.arraycopy(tmp, cursor1, a, dest, count1)
            dest += count1
            cursor1 += count1
            len1 -= count1
            if (len1 <= 1) { // len1 == 1 || len1 == 0
              done = true; gallopContinue = false
            }
          }
          if (gallopContinue) {
            a(dest) = a(cursor2); dest += 1; cursor2 += 1
            len2 -= 1
            if (len2 == 0) { done = true; gallopContinue = false }
          }

          if (gallopContinue) {
            count2 = TimSort.gallopLeft(tmp(cursor1), a, cursor2, len2, 0, c)
            if (count2 != 0) {
              System.arraycopy(a, cursor2, a, dest, count2)
              dest += count2
              cursor2 += count2
              len2 -= count2
              if (len2 == 0) { done = true; gallopContinue = false }
            }
            if (gallopContinue) {
              a(dest) = tmp(cursor1); dest += 1; cursor1 += 1
              len1 -= 1
              if (len1 == 1) { done = true; gallopContinue = false }
              else localMinGallop -= 1
            }
          }

          if (gallopContinue && !(count1 >= TimSort.MIN_GALLOP || count2 >= TimSort.MIN_GALLOP))
            gallopContinue = false
        }
        if (!done) {
          if (localMinGallop < 0) localMinGallop = 0
          localMinGallop += 2 // Penalize for leaving gallop mode
        }
      }
    } // End of "outer" loop
    this.minGallop = if (localMinGallop < 1) 1 else localMinGallop // Write back to field

    if (len1 == 1) {
      if (TimSort.DEBUG) assert(len2 > 0)
      System.arraycopy(a, cursor2, a, dest, len2)
      a(dest + len2) = tmp(cursor1) // Last elt of run 1 to end of merge
    } else if (len1 == 0) {
      throw new IllegalArgumentException("Comparison method violates its general contract!")
    } else {
      if (TimSort.DEBUG) assert(len2 == 0)
      if (TimSort.DEBUG) assert(len1 > 1)
      System.arraycopy(tmp, cursor1, a, dest, len1)
    }
  }

  /** Like mergeLo, except that this method should be called only if len1 >= len2; mergeLo should be called if len1 <= len2. (Either method may be called if len1 == len2.)
    *
    * @param base1
    *   index of first element in first run to be merged
    * @param _len1
    *   length of first run to be merged (must be > 0)
    * @param base2
    *   index of first element in second run to be merged (must be aBase + aLen)
    * @param _len2
    *   length of second run to be merged (must be > 0)
    */
  private def mergeHi(base1: Int, _len1: Int, base2: Int, _len2: Int): Unit = boundary {
    if (TimSort.DEBUG) assert(_len1 > 0 && _len2 > 0 && base1 + _len1 == base2)

    var len1 = _len1
    var len2 = _len2

    // Copy second run into temp array
    val a   = this.a // For performance
    val tmp = ensureCapacity(len2)
    System.arraycopy(a, base2, tmp, 0, len2)

    var cursor1 = base1 + len1 - 1 // Indexes into a
    var cursor2 = len2 - 1 // Indexes into tmp array
    var dest    = base2 + len2 - 1 // Indexes into a

    // Move last element of first run and deal with degenerate cases
    a(dest) = a(cursor1); dest -= 1; cursor1 -= 1
    len1 -= 1
    if (len1 == 0) {
      System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2)
      break()
    }
    if (len2 == 1) {
      dest -= len1
      cursor1 -= len1
      System.arraycopy(a, cursor1 + 1, a, dest + 1, len1)
      a(dest) = tmp(cursor2)
      break()
    }

    val c              = this.c // Use local variable for performance
    var localMinGallop = this.minGallop // " " " " "
    var done           = false
    while (!done) {
      var count1 = 0 // Number of times in a row that first run won
      var count2 = 0 // Number of times in a row that second run won

      /*
       * Do the straightforward thing until (if ever) one run appears to win consistently.
       */
      var breakOuter = false
      var doContinue = true
      while (doContinue) {
        if (TimSort.DEBUG) assert(len1 > 0 && len2 > 1)
        if (c.compare(tmp(cursor2), a(cursor1)) < 0) {
          a(dest) = a(cursor1); dest -= 1; cursor1 -= 1
          count1 += 1
          count2 = 0
          len1 -= 1
          if (len1 == 0) { breakOuter = true; doContinue = false }
        } else {
          a(dest) = tmp(cursor2); dest -= 1; cursor2 -= 1
          count2 += 1
          count1 = 0
          len2 -= 1
          if (len2 == 1) { breakOuter = true; doContinue = false }
        }
        if (doContinue && !((count1 | count2) < localMinGallop)) doContinue = false
      }
      if (breakOuter) { done = true }

      /*
       * One run is winning so consistently that galloping may be a huge win. So try that, and continue galloping until (if
       * ever) neither run appears to be winning consistently anymore.
       */
      if (!done) {
        var gallopContinue = true
        while (gallopContinue) {
          if (TimSort.DEBUG) assert(len1 > 0 && len2 > 1)
          count1 = len1 - TimSort.gallopRight(tmp(cursor2), a, base1, len1, len1 - 1, c)
          if (count1 != 0) {
            dest -= count1
            cursor1 -= count1
            len1 -= count1
            System.arraycopy(a, cursor1 + 1, a, dest + 1, count1)
            if (len1 == 0) { done = true; gallopContinue = false }
          }
          if (gallopContinue) {
            a(dest) = tmp(cursor2); dest -= 1; cursor2 -= 1
            len2 -= 1
            if (len2 == 1) { done = true; gallopContinue = false }
          }

          if (gallopContinue) {
            count2 = len2 - TimSort.gallopLeft(a(cursor1), tmp, 0, len2, len2 - 1, c)
            if (count2 != 0) {
              dest -= count2
              cursor2 -= count2
              len2 -= count2
              System.arraycopy(tmp, cursor2 + 1, a, dest + 1, count2)
              if (len2 <= 1) { // len2 == 1 || len2 == 0
                done = true; gallopContinue = false
              }
            }
            if (gallopContinue) {
              a(dest) = a(cursor1); dest -= 1; cursor1 -= 1
              len1 -= 1
              if (len1 == 0) { done = true; gallopContinue = false }
              else localMinGallop -= 1
            }
          }

          if (gallopContinue && !(count1 >= TimSort.MIN_GALLOP || count2 >= TimSort.MIN_GALLOP))
            gallopContinue = false
        }
        if (!done) {
          if (localMinGallop < 0) localMinGallop = 0
          localMinGallop += 2 // Penalize for leaving gallop mode
        }
      }
    } // End of "outer" loop
    this.minGallop = if (localMinGallop < 1) 1 else localMinGallop // Write back to field

    if (len2 == 1) {
      if (TimSort.DEBUG) assert(len1 > 0)
      dest -= len1
      cursor1 -= len1
      System.arraycopy(a, cursor1 + 1, a, dest + 1, len1)
      a(dest) = tmp(cursor2) // Move first elt of run2 to front of merge
    } else if (len2 == 0) {
      throw new IllegalArgumentException("Comparison method violates its general contract!")
    } else {
      if (TimSort.DEBUG) assert(len1 == 0)
      if (TimSort.DEBUG) assert(len2 > 0)
      System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2)
    }
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

  /** Locates the position at which to insert the specified key into the specified sorted range; if the range contains an element equal to key, returns the index of the leftmost equal element.
    *
    * @param key
    *   the key whose insertion point to search for
    * @param a
    *   the array in which to search
    * @param base
    *   the index of the first element in the range
    * @param len
    *   the length of the range; must be > 0
    * @param hint
    *   the index at which to begin the search, 0 <= hint < n. The closer hint is to the result, the faster this method will run.
    * @param c
    *   the comparator used to order the range, and to search
    * @return
    *   the int k, 0 <= k <= n such that a[b + k - 1] < key <= a[b + k], pretending that a[b - 1] is minus infinity and a[b + n] is infinity. In other words, key belongs at index b + k; or in other
    *   words, the first k elements of a should precede key, and the last n - k should follow it.
    */
  private def gallopLeft[T](key: T, a: Array[T], base: Int, len: Int, hint: Int, c: Ordering[T]): Int = {
    if (DEBUG) assert(len > 0 && hint >= 0 && hint < len)

    var lastOfs = 0
    var ofs     = 1
    if (c.compare(key, a(base + hint)) > 0) {
      // Gallop right until a[base+hint+lastOfs] < key <= a[base+hint+ofs]
      val maxOfs = len - hint
      while (ofs < maxOfs && c.compare(key, a(base + hint + ofs)) > 0) {
        lastOfs = ofs
        ofs = (ofs << 1) + 1
        if (ofs <= 0) ofs = maxOfs // int overflow
      }
      if (ofs > maxOfs) ofs = maxOfs

      // Make offsets relative to base
      lastOfs += hint
      ofs += hint
    } else { // key <= a[base + hint]
      // Gallop left until a[base+hint-ofs] < key <= a[base+hint-lastOfs]
      val maxOfs = hint + 1
      while (ofs < maxOfs && c.compare(key, a(base + hint - ofs)) <= 0) {
        lastOfs = ofs
        ofs = (ofs << 1) + 1
        if (ofs <= 0) ofs = maxOfs // int overflow
      }
      if (ofs > maxOfs) ofs = maxOfs

      // Make offsets relative to base
      val tmp = lastOfs
      lastOfs = hint - ofs
      ofs = hint - tmp
    }
    if (DEBUG) assert(-1 <= lastOfs && lastOfs < ofs && ofs <= len)

    // Now a[base+lastOfs] < key <= a[base+ofs], so key belongs somewhere to the right of lastOfs but no farther right than ofs
    lastOfs += 1
    while (lastOfs < ofs) {
      val m = lastOfs + ((ofs - lastOfs) >>> 1)

      if (c.compare(key, a(base + m)) > 0) {
        lastOfs = m + 1 // a[base + m] < key
      } else {
        ofs = m // key <= a[base + m]
      }
    }
    if (DEBUG) assert(lastOfs == ofs) // so a[base + ofs - 1] < key <= a[base + ofs]
    ofs
  }

  /** Like gallopLeft, except that if the range contains an element equal to key, gallopRight returns the index after the rightmost equal element.
    *
    * @param key
    *   the key whose insertion point to search for
    * @param a
    *   the array in which to search
    * @param base
    *   the index of the first element in the range
    * @param len
    *   the length of the range; must be > 0
    * @param hint
    *   the index at which to begin the search, 0 <= hint < n. The closer hint is to the result, the faster this method will run.
    * @param c
    *   the comparator used to order the range, and to search
    * @return
    *   the int k, 0 <= k <= n such that a[b + k - 1] <= key < a[b + k]
    */
  private def gallopRight[T](key: T, a: Array[T], base: Int, len: Int, hint: Int, c: Ordering[T]): Int = {
    if (DEBUG) assert(len > 0 && hint >= 0 && hint < len)

    var ofs     = 1
    var lastOfs = 0
    if (c.compare(key, a(base + hint)) < 0) {
      // Gallop left until a[base+hint-ofs] <= key < a[base+hint-lastOfs]
      val maxOfs = hint + 1
      while (ofs < maxOfs && c.compare(key, a(base + hint - ofs)) < 0) {
        lastOfs = ofs
        ofs = (ofs << 1) + 1
        if (ofs <= 0) ofs = maxOfs // int overflow
      }
      if (ofs > maxOfs) ofs = maxOfs

      // Make offsets relative to base
      val tmp = lastOfs
      lastOfs = hint - ofs
      ofs = hint - tmp
    } else { // a[base + hint] <= key
      // Gallop right until a[base+hint+lastOfs] <= key < a[base+hint+ofs]
      val maxOfs = len - hint
      while (ofs < maxOfs && c.compare(key, a(base + hint + ofs)) >= 0) {
        lastOfs = ofs
        ofs = (ofs << 1) + 1
        if (ofs <= 0) ofs = maxOfs // int overflow
      }
      if (ofs > maxOfs) ofs = maxOfs

      // Make offsets relative to base
      lastOfs += hint
      ofs += hint
    }
    if (DEBUG) assert(-1 <= lastOfs && lastOfs < ofs && ofs <= len)

    // Now a[base+lastOfs] <= key < a[base+ofs], so key belongs somewhere to the right of lastOfs but no farther right than ofs
    lastOfs += 1
    while (lastOfs < ofs) {
      val m = lastOfs + ((ofs - lastOfs) >>> 1)

      if (c.compare(key, a(base + m)) < 0) {
        ofs = m // key < a[base + m]
      } else {
        lastOfs = m + 1 // a[base + m] <= key
      }
    }
    if (DEBUG) assert(lastOfs == ofs) // so a[base + ofs - 1] <= key < a[base + ofs]
    ofs
  }

  /** Checks that fromIndex and toIndex are in range, and throws an appropriate exception if they aren't. */
  private def rangeCheck(arrayLen: Int, fromIndex: Int, toIndex: Int): Unit = {
    if (fromIndex > toIndex) throw new IllegalArgumentException(s"fromIndex($fromIndex) > toIndex($toIndex)")
    if (fromIndex < 0) throw new ArrayIndexOutOfBoundsException(fromIndex)
    if (toIndex > arrayLen) throw new ArrayIndexOutOfBoundsException(toIndex)
  }
}
