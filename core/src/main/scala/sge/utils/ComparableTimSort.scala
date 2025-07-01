package sge
package utils

/** This is a near duplicate of TimSort, modified for use with arrays of objects that implement Comparable, instead of using explicit comparators.
  *
  * If you are using an optimizing VM, you may find that ComparableTimSort offers no performance benefit over TimSort in conjunction with a comparator that simply returns
  * ((Comparable)first).compareTo(Second). If this is the case, you are better off deleting ComparableTimSort to eliminate the code duplication. (See Arrays.java for details.)
  */
class ComparableTimSort {

  /** The array being sorted. */
  private var a: Array[AnyRef] = scala.compiletime.uninitialized

  /** This controls when we get *into* galloping mode. It is initialized to MIN_GALLOP. The mergeLo and mergeHi methods nudge it higher for random data, and lower for highly structured data.
    */
  private var minGallop: Int = ComparableTimSort.MIN_GALLOP

  /** Temp storage for merges. */
  private var tmp:      Array[AnyRef] = new Array[AnyRef](ComparableTimSort.INITIAL_TMP_STORAGE_LENGTH)
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

  def doSort(a: Array[AnyRef], lo: Int, hi: Int): Unit = {
    stackSize = 0
    ComparableTimSort.rangeCheck(a.length, lo, hi)
    val nRemaining = hi - lo
    if (nRemaining < 2) return // Arrays of size 0 and 1 are always sorted

    // If array is small, do a "mini-TimSort" with no merges
    if (nRemaining < ComparableTimSort.MIN_MERGE) {
      val initRunLen = ComparableTimSort.countRunAndMakeAscending(a, lo, hi)
      ComparableTimSort.binarySort(a, lo, hi, lo + initRunLen)
      return
    }

    this.a = a
    tmpCount = 0

    // March over the array once, left to right, finding natural runs, extending short natural runs to minRun elements, and
    // merging runs to maintain stack invariant.
    val minRun    = ComparableTimSort.minRunLength(nRemaining)
    var currentLo = lo
    var remaining = nRemaining

    while (remaining != 0) {
      // Identify next run
      var runLen = ComparableTimSort.countRunAndMakeAscending(a, currentLo, hi)

      // If run is short, extend to min(minRun, remaining)
      if (runLen < minRun) {
        val force = if (remaining <= minRun) remaining else minRun
        ComparableTimSort.binarySort(a, currentLo, currentLo + force, currentLo + runLen)
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
    if (ComparableTimSort.DEBUG) assert(currentLo == hi)
    mergeForceCollapse()
    if (ComparableTimSort.DEBUG) assert(stackSize == 1)

    this.a = null
    val tmp = this.tmp
    for (i <- 0 until tmpCount)
      tmp(i) = null
  }

  private def pushRun(runBase: Int, runLen: Int): Unit = {
    this.runBase(stackSize) = runBase
    this.runLen(stackSize) = runLen
    stackSize += 1
  }

  private def mergeCollapse(): Unit =
    while (stackSize > 1) {
      var n = stackSize - 2
      if (n > 0 && runLen(n - 1) <= runLen(n) + runLen(n + 1)) {
        if (runLen(n - 1) < runLen(n + 1)) n -= 1
        mergeAt(n)
      } else if (runLen(n) <= runLen(n + 1)) {
        mergeAt(n)
      } else {
        return
      }
    }

  private def mergeForceCollapse(): Unit =
    while (stackSize > 1) {
      var n = stackSize - 2
      if (n > 0 && runLen(n - 1) < runLen(n + 1)) n -= 1
      mergeAt(n)
    }

  private def mergeAt(i: Int): Unit = {
    if (ComparableTimSort.DEBUG) assert(stackSize >= 2)
    if (ComparableTimSort.DEBUG) assert(i >= 0)
    if (ComparableTimSort.DEBUG) assert(i == stackSize - 2 || i == stackSize - 3)

    val base1 = runBase(i)
    var len1  = runLen(i)
    val base2 = runBase(i + 1)
    var len2  = runLen(i + 1)
    if (ComparableTimSort.DEBUG) assert(len1 > 0 && len2 > 0)
    if (ComparableTimSort.DEBUG) assert(base1 + len1 == base2)

    // Record the length of the combined runs
    runLen(i) = len1 + len2
    if (i == stackSize - 3) {
      runBase(i + 1) = runBase(i + 2)
      runLen(i + 1) = runLen(i + 2)
    }
    stackSize -= 1

    // Find where the first element of run2 goes in run1
    val k = ComparableTimSort.gallopRight(a(base2).asInstanceOf[Comparable[AnyRef]], a, base1, len1, 0)
    if (ComparableTimSort.DEBUG) assert(k >= 0)
    base1 + k
    len1 -= k
    if (len1 == 0) return

    // Find where the last element of run1 goes in run2
    len2 = ComparableTimSort.gallopLeft(a(base1 + len1 - 1).asInstanceOf[Comparable[AnyRef]], a, base2, len2, len2 - 1)
    if (ComparableTimSort.DEBUG) assert(len2 >= 0)
    if (len2 == 0) return

    // Merge remaining runs, using tmp array with min(len1, len2) elements
    if (len1 <= len2) {
      mergeLo(base1, len1, base2, len2)
    } else {
      mergeHi(base1, len1, base2, len2)
    }
  }

  // Placeholder for mergeLo and mergeHi methods - these need proper implementation
  private def mergeLo(base1: Int, len1: Int, base2: Int, len2: Int): Unit = {
    // TODO: Implement merge logic
  }

  private def mergeHi(base1: Int, len1: Int, base2: Int, len2: Int): Unit = {
    // TODO: Implement merge logic
  }
}

object ComparableTimSort {

  /** This is the minimum sized sequence that will be merged. Shorter sequences will be lengthened by calling binarySort. If the entire array is less than this length, no merges will be performed.
    *
    * This constant should be a power of two. It was 64 in Tim Peter's C implementation, but 32 was empirically determined to work better in this implementation. In the unlikely event that you set
    * this constant to be a number that's not a power of two, you'll need to change the minRunLength computation.
    *
    * If you decrease this constant, you must change the stackLen computation in the TimSort constructor, or you risk an ArrayOutOfBounds exception. See listsort.txt for a discussion of the minimum
    * stack length required as a function of the length of the array being sorted and the minimum merge sequence length.
    */
  private val MIN_MERGE = 32

  /** When we get into galloping mode, we stay there until both runs win less often than MIN_GALLOP consecutive times. */
  private val MIN_GALLOP = 7

  /** Maximum initial size of tmp array, which is used for merging. The array can grow to accommodate demand.
    *
    * Unlike Tim's original C version, we do not allocate this much storage when sorting smaller arrays. This change was required for performance.
    */
  private val INITIAL_TMP_STORAGE_LENGTH = 256

  /** Asserts have been placed in if-statements for performance. To enable them, set this field to true and enable them in VM with a command line flag. If you modify this class, please do test the
    * asserts!
    */
  private val DEBUG = false

  def sort(a: Array[AnyRef]): Unit =
    sort(a, 0, a.length)

  def sort(a: Array[AnyRef], lo: Int, hi: Int): Unit = {
    rangeCheck(a.length, lo, hi)
    val nRemaining = hi - lo
    if (nRemaining < 2) return // Arrays of size 0 and 1 are always sorted

    // If array is small, do a "mini-TimSort" with no merges
    if (nRemaining < MIN_MERGE) {
      val initRunLen = countRunAndMakeAscending(a, lo, hi)
      binarySort(a, lo, hi, lo + initRunLen)
      return
    }

    // March over the array once, left to right, finding natural runs, extending short natural runs to minRun elements, and
    // merging runs to maintain stack invariant.
    val ts        = new ComparableTimSort()
    val minRun    = minRunLength(nRemaining)
    var currentLo = lo
    var remaining = nRemaining

    while (remaining != 0) {
      // Identify next run
      var runLen = countRunAndMakeAscending(a, currentLo, hi)

      // If run is short, extend to min(minRun, remaining)
      if (runLen < minRun) {
        val force = if (remaining <= minRun) remaining else minRun
        binarySort(a, currentLo, currentLo + force, currentLo + runLen)
        runLen = force
      }

      // Push run onto pending-run stack, and maybe merge
      ts.pushRun(currentLo, runLen)
      ts.mergeCollapse()

      // Advance to find next run
      currentLo += runLen
      remaining -= runLen
    }

    // Merge all remaining runs to complete sort
    if (DEBUG) assert(currentLo == hi)
    ts.mergeForceCollapse()
    if (DEBUG) assert(ts.stackSize == 1)
  }

  private def rangeCheck(arrayLen: Int, fromIndex: Int, toIndex: Int): Unit = {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException(s"fromIndex($fromIndex) > toIndex($toIndex)")
    }
    if (fromIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(fromIndex)
    }
    if (toIndex > arrayLen) {
      throw new ArrayIndexOutOfBoundsException(toIndex)
    }
  }

  private def countRunAndMakeAscending(a: Array[AnyRef], lo: Int, hi: Int): Int = {
    if (DEBUG) assert(lo < hi)

    var runHi = lo + 1
    if (runHi == hi) return 1

    // Find end of run, and reverse range if descending
    if (a(runHi).asInstanceOf[Comparable[AnyRef]].compareTo(a(lo)) < 0) { // Descending
      while (runHi < hi && a(runHi).asInstanceOf[Comparable[AnyRef]].compareTo(a(runHi - 1)) < 0)
        runHi += 1
      reverseRange(a, lo, runHi)
    } else { // Ascending
      while (runHi < hi && a(runHi).asInstanceOf[Comparable[AnyRef]].compareTo(a(runHi - 1)) >= 0)
        runHi += 1
    }

    runHi - lo
  }

  private def reverseRange(a: Array[AnyRef], lo: Int, hi: Int): Unit = {
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

  private def binarySort(a: Array[AnyRef], lo: Int, hi: Int, start: Int): Unit = {
    if (DEBUG) assert(lo <= start && start <= hi)
    var start_var = start
    if (start_var == lo) start_var += 1

    while (start_var < hi) {
      val pivot = a(start_var)

      // Set left (and right) to the index where a[start] (pivot) belongs
      var left  = lo
      var right = start_var
      if (DEBUG) assert(left <= right)

      // The invariants a[lo .. left) <= pivot < a[right .. start_var) hold
      while (left < right) {
        val mid = (left + right) >>> 1
        if (pivot.asInstanceOf[Comparable[AnyRef]].compareTo(a(mid)) < 0) {
          right = mid
        } else {
          left = mid + 1
        }
      }
      if (DEBUG) assert(left == right)

      // The invariants hold: a[lo .. left) <= pivot < a[left .. start_var), so pivot belongs at left
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

  private def gallopLeft(key: Comparable[AnyRef], a: Array[AnyRef], base: Int, len: Int, hint: Int): Int = {
    if (DEBUG) assert(len > 0 && hint >= 0 && hint < len)

    var lastOfs = 0
    var ofs     = 1
    if (key.compareTo(a(base + hint)) > 0) {
      // Gallop right until a[base+hint+lastOfs] < key <= a[base+hint+ofs]
      val maxOfs = len - hint
      while (ofs < maxOfs && key.compareTo(a(base + hint + ofs)) > 0) {
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
      while (ofs < maxOfs && key.compareTo(a(base + hint - ofs)) <= 0) {
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

      if (key.compareTo(a(base + m)) > 0) {
        lastOfs = m + 1 // a[base + m] < key
      } else {
        ofs = m // key <= a[base + m]
      }
    }
    if (DEBUG) assert(lastOfs == ofs) // so a[base + ofs - 1] < key <= a[base + ofs]
    ofs
  }

  private def gallopRight(key: Comparable[AnyRef], a: Array[AnyRef], base: Int, len: Int, hint: Int): Int = {
    if (DEBUG) assert(len > 0 && hint >= 0 && hint < len)

    var ofs     = 1
    var lastOfs = 0
    if (key.compareTo(a(base + hint)) < 0) {
      // Gallop left until a[base+hint-ofs] <= key < a[base+hint-lastOfs]
      val maxOfs = hint + 1
      while (ofs < maxOfs && key.compareTo(a(base + hint - ofs)) < 0) {
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
      while (ofs < maxOfs && key.compareTo(a(base + hint + ofs)) >= 0) {
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

      if (key.compareTo(a(base + m)) < 0) {
        ofs = m // key < a[base + m]
      } else {
        lastOfs = m + 1 // a[base + m] <= key
      }
    }
    if (DEBUG) assert(lastOfs == ofs) // so a[base + ofs - 1] <= key < a[base + ofs]
    ofs
  }
}
