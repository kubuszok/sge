/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/QuickSelect.java
 * Original authors: Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Comparator<? super T>` -> `Ordering[T]`
 *   Convention: `return` eliminated in `recursiveSelect` via expression-based code
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Implementation of Tony Hoare's quickselect algorithm. Running time is generally O(n), but worst case is O(n^2) Pivot choice is median of three method, providing better performance than a random
  * pivot for partially sorted data. http://en.wikipedia.org/wiki/Quickselect
  * @author
  *   Jon Renner (original implementation)
  */
final class QuickSelect[T] {
  private var array: Array[T]    = scala.compiletime.uninitialized
  private var comp:  Ordering[T] = scala.compiletime.uninitialized

  def select(items: Array[T], comp: Ordering[T], n: Int, size: Int): Int = {
    this.array = items
    this.comp = comp
    recursiveSelect(0, size - 1, n)
  }

  private def partition(left: Int, right: Int, pivot: Int): Int = {
    val pivotValue = array(pivot)
    swap(right, pivot)
    var storage = left
    for (i <- left until right)
      if (comp.compare(array(i), pivotValue) < 0) {
        swap(storage, i)
        storage += 1
      }
    swap(right, storage)
    storage
  }

  private def recursiveSelect(left: Int, right: Int, k: Int): Int =
    if (left == right) left
    else {
      val pivotIndex    = medianOfThreePivot(left, right)
      val pivotNewIndex = partition(left, right, pivotIndex)
      val pivotDist     = (pivotNewIndex - left) + 1
      if (pivotDist == k) {
        pivotNewIndex
      } else if (k < pivotDist) {
        recursiveSelect(left, pivotNewIndex - 1, k)
      } else {
        recursiveSelect(pivotNewIndex + 1, right, k - pivotDist)
      }
    }

  /** Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays */
  private def medianOfThreePivot(leftIdx: Int, rightIdx: Int): Int = {
    val left   = array(leftIdx)
    val midIdx = (leftIdx + rightIdx) / 2
    val mid    = array(midIdx)
    val right  = array(rightIdx)

    // spaghetti median of three algorithm
    // does at most 3 comparisons
    if (comp.compare(left, mid) > 0) {
      if (comp.compare(mid, right) > 0) {
        midIdx
      } else if (comp.compare(left, right) > 0) {
        rightIdx
      } else {
        leftIdx
      }
    } else {
      if (comp.compare(left, right) > 0) {
        leftIdx
      } else if (comp.compare(mid, right) > 0) {
        rightIdx
      } else {
        midIdx
      }
    }
  }

  private def swap(left: Int, right: Int): Unit = {
    val tmp = array(left)
    array(left) = array(right)
    array(right) = tmp
  }
}
