/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Sort.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Array<T>` -> `DynamicArray[T]`; `Comparator` -> `Ordering`
 *   Convention: Java instance class with lazy singleton -> Scala `object` with direct methods
 *   Idiom: split packages
 *   Issues: missing `instance()` method (minor — Scala object serves as singleton); `sort(DynamicArray, Ordering)` copies array out and back instead of sorting `items` in-place
 *   TODO: ComparableTimSort uses asInstanceOf — sort[T: Comparable] overloads should use Ordering derived from Comparable, eliminating ComparableTimSort or merging it with TimSort
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: Sort,comparableTimSort,sort
 * Covenant-source-reference: com/badlogic/gdx/utils/Sort.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package utils

import scala.language.implicitConversions

/** Provides methods to sort arrays of objects. Sorting requires working memory and this class allows that memory to be reused to avoid allocation. The sorting is otherwise identical to the
  * Arrays.sort methods (uses timsort).<br> <br> Note that sorting primitive arrays with the Arrays.sort methods does not allocate memory (unless sorting large arrays of char, short, or byte).
  * @author
  *   Nathan Sweet (original implementation)
  */
object Sort {
  private val comparableTimSort = ComparableTimSort()

  def sort[T <: Comparable[T]](a: DynamicArray[T]): Unit =
    comparableTimSort.doSort(a.items.asInstanceOf[Array[AnyRef]], 0, a.size)

  /** The specified objects must implement {@link Comparable}. */
  def sort(a: Array[AnyRef]): Unit =
    comparableTimSort.doSort(a, 0, a.length)

  /** The specified objects must implement {@link Comparable}. */
  def sort(a: Array[AnyRef], fromIndex: Int, toIndex: Int): Unit =
    comparableTimSort.doSort(a, fromIndex, toIndex)

  def sort[T](a: DynamicArray[T], c: Ordering[T]): Unit =
    TimSort.sort(a.items, 0, a.size, c)

  def sort[T](a: Array[T], c: Ordering[T]): Unit =
    TimSort.sort(a, c)

  def sort[T](a: Array[T], c: Ordering[T], fromIndex: Int, toIndex: Int): Unit =
    TimSort.sort(a, fromIndex, toIndex, c)
}
