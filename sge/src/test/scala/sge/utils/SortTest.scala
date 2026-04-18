/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original test: com/badlogic/gdx/utils/SortTest.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/*
 * This test class verifies the correctness of the sort functions in the Sort object.
 * Each test ensures that the corresponding sort function correctly sorts arrays
 * and DynamicArray objects according to the specified criteria.
 */
class SortTest extends munit.FunSuite {

  test("sort array comparable") {
    val array: Array[AnyRef] = Array[AnyRef](
      Int.box(3),
      Int.box(1),
      Int.box(4),
      Int.box(1),
      Int.box(5),
      Int.box(9),
      Int.box(2),
      Int.box(6),
      Int.box(5),
      Int.box(3),
      Int.box(5)
    )
    Sort.sort(array)
    val expected: Array[AnyRef] = Array[AnyRef](
      Int.box(1),
      Int.box(1),
      Int.box(2),
      Int.box(3),
      Int.box(3),
      Int.box(4),
      Int.box(5),
      Int.box(5),
      Int.box(5),
      Int.box(6),
      Int.box(9)
    )
    assertEquals(array.toSeq, expected.toSeq)
  }

  test("sort array with comparator") {
    val array = Array(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)
    val ordering: Ordering[Int] = Ordering.Int
    Sort.sort(array, ordering)
    assertEquals(array.toSeq, Seq(1, 1, 2, 3, 3, 4, 5, 5, 5, 6, 9))
  }

  test("sort array with comparator and range") {
    val array = Array(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)
    val ordering: Ordering[Int] = Ordering.Int
    Sort.sort(array, ordering, 2, 7)
    assertEquals(array.toSeq, Seq(3, 1, 1, 2, 4, 5, 9, 6, 5, 3, 5))
  }

  test("sort array range") {
    val array: Array[AnyRef] = Array[AnyRef](
      Int.box(3),
      Int.box(1),
      Int.box(4),
      Int.box(1),
      Int.box(5),
      Int.box(9),
      Int.box(2),
      Int.box(6),
      Int.box(5),
      Int.box(3),
      Int.box(5)
    )
    Sort.sort(array, 2, 7)
    val expected: Array[AnyRef] = Array[AnyRef](
      Int.box(3),
      Int.box(1),
      Int.box(1),
      Int.box(2),
      Int.box(4),
      Int.box(5),
      Int.box(9),
      Int.box(6),
      Int.box(5),
      Int.box(3),
      Int.box(5)
    )
    assertEquals(array.toSeq, expected.toSeq)
  }

  test("sort DynamicArray comparable") {
    val array = DynamicArray[java.lang.Integer]()
    Seq(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5).map(java.lang.Integer.valueOf).foreach(array.add)
    Sort.sort(array)
    assertEquals(
      (0 until array.size).map(i => array(i).intValue()).toSeq,
      Seq(1, 1, 2, 3, 3, 4, 5, 5, 5, 6, 9)
    )
  }

  test("sort array with nulls using NullsFirst ordering") {
    val arrayWithNulls: Array[AnyRef] =
      Array[AnyRef](Int.box(3), null, Int.box(1), Int.box(4), null, Int.box(2))
    val nullsFirstOrdering: Ordering[AnyRef] = (o1: AnyRef, o2: AnyRef) =>
      if (o1 == null && o2 == null) 0
      else if (o1 == null) -1
      else if (o2 == null) 1
      else o1.asInstanceOf[java.lang.Integer].compareTo(o2.asInstanceOf[java.lang.Integer])
    Sort.sort(arrayWithNulls, nullsFirstOrdering)
    val expected: Array[AnyRef] =
      Array[AnyRef](null, null, Int.box(1), Int.box(2), Int.box(3), Int.box(4))
    assertEquals(arrayWithNulls.toSeq, expected.toSeq)
  }

  test("sort array range with invalid indices throws") {
    val array: Array[AnyRef] = Array[AnyRef](
      Int.box(3),
      Int.box(1),
      Int.box(4),
      Int.box(1),
      Int.box(5),
      Int.box(9),
      Int.box(2),
      Int.box(6),
      Int.box(5),
      Int.box(3),
      Int.box(5)
    )
    intercept[ArrayIndexOutOfBoundsException] {
      Sort.sort(array, -1, 15)
    }
  }

  test("sort DynamicArray with custom comparator (reverse)") {
    val array = DynamicArray[Int]()
    Seq(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5).foreach(array.add)
    val reverseOrdering: Ordering[Int] = Ordering.Int.reverse
    Sort.sort(array, reverseOrdering)
    assertEquals((0 until array.size).map(array(_)).toSeq, Seq(9, 6, 5, 5, 5, 4, 3, 3, 2, 1, 1))
  }

  test("sort empty array") {
    val emptyArray: Array[AnyRef] = Array[AnyRef]()
    Sort.sort(emptyArray)
    assertEquals(emptyArray.toSeq, Seq.empty)
  }

  test("sort single element array") {
    val singleElementArray: Array[AnyRef] = Array[AnyRef](Int.box(1))
    Sort.sort(singleElementArray)
    assertEquals(singleElementArray.toSeq, Seq[AnyRef](Int.box(1)))
  }

  test("sort already sorted DynamicArray") {
    val sortedArray = DynamicArray[Int]()
    Seq(1, 2, 3, 4, 5).foreach(sortedArray.add)
    Sort.sort(sortedArray, Ordering.Int)
    assertEquals((0 until sortedArray.size).map(sortedArray(_)).toSeq, Seq(1, 2, 3, 4, 5))
  }

  test("sort DynamicArray with equal elements") {
    val equalElementsArray = DynamicArray[Int]()
    Seq(2, 2, 2, 2, 2).foreach(equalElementsArray.add)
    Sort.sort(equalElementsArray, Ordering.Int)
    assertEquals((0 until equalElementsArray.size).map(equalElementsArray(_)).toSeq, Seq(2, 2, 2, 2, 2))
  }

  test("sort single element DynamicArray") {
    val singleElementArray = DynamicArray[Int]()
    singleElementArray.add(1)
    Sort.sort(singleElementArray, Ordering.Int)
    assertEquals((0 until singleElementArray.size).map(singleElementArray(_)).toSeq, Seq(1))
  }

  test("sort empty DynamicArray") {
    val emptyArray = DynamicArray[Int]()
    Sort.sort(emptyArray, Ordering.Int)
    assertEquals(emptyArray.size, 0)
  }

  test("sort DynamicArray Comparable modifies original") {
    // Regression: sort[Comparable] sorted a copy but never wrote back to the DynamicArray
    val da = DynamicArray[java.lang.Integer]()
    Seq(5, 3, 1, 4, 2).map(java.lang.Integer.valueOf).foreach(da.add)
    Sort.sort(da)
    assertEquals((0 until da.size).map(i => da(i).intValue()).toSeq, Seq(1, 2, 3, 4, 5))
  }

  // Large array tests to exercise full TimSort merge/gallop paths (MIN_MERGE = 32)

  test("sort large random array via ComparableTimSort") {
    val rng   = java.util.Random(42)
    val n     = 500
    val array = Array.fill[AnyRef](n)(Int.box(rng.nextInt(1000)))
    Sort.sort(array)
    for (i <- 1 until n)
      assert(
        array(i - 1).asInstanceOf[java.lang.Integer].compareTo(array(i).asInstanceOf[java.lang.Integer]) <= 0,
        s"Out of order at $i: ${array(i - 1)} > ${array(i)}"
      )
  }

  test("sort large random array via TimSort with Ordering") {
    val rng   = java.util.Random(42)
    val n     = 500
    val array = Array.fill(n)(rng.nextInt(1000))
    Sort.sort(array, Ordering.Int)
    for (i <- 1 until n)
      assert(array(i - 1) <= array(i), s"Out of order at $i: ${array(i - 1)} > ${array(i)}")
  }

  test("sort large reverse-sorted array triggers merge") {
    val n     = 200
    val array = (n to 1 by -1).map(Int.box).toArray[AnyRef]
    Sort.sort(array)
    for (i <- 0 until n)
      assertEquals(array(i).asInstanceOf[java.lang.Integer].intValue(), i + 1)
  }

  test("sort large already-sorted array") {
    val n     = 200
    val array = (1 to n).map(Int.box).toArray[AnyRef]
    Sort.sort(array)
    for (i <- 0 until n)
      assertEquals(array(i).asInstanceOf[java.lang.Integer].intValue(), i + 1)
  }

  test("sort stability with equal keys") {
    // Pairs of (key, original-index) — stable sort should preserve original order for equal keys
    final case class Pair(key: Int, idx: Int) extends Comparable[Pair] {
      override def compareTo(o: Pair): Int = Integer.compare(key, o.key)
    }
    val n     = 100
    val array = Array.tabulate[AnyRef](n)(i => Pair(i % 10, i)) // 10 groups of 10
    Sort.sort(array)
    val sorted = array.map(_.asInstanceOf[Pair])
    for (i <- 1 until n) {
      assert(sorted(i - 1).key <= sorted(i).key, s"Out of order at $i")
      if (sorted(i - 1).key == sorted(i).key) {
        assert(sorted(i - 1).idx < sorted(i).idx, s"Stability violated at $i: idx ${sorted(i - 1).idx} >= ${sorted(i).idx}")
      }
    }
  }

  test("sort alternating runs pattern exercises gallop") {
    // Interleave two sorted runs to trigger gallop mode
    val n     = 200
    val run1  = (0 until n by 2).map(Int.box).toArray[AnyRef] // 0,2,4,...
    val run2  = (1 until n by 2).map(Int.box).toArray[AnyRef] // 1,3,5,...
    val array = run1 ++ run2 // Two sorted runs concatenated
    Sort.sort(array)
    for (i <- 0 until n)
      assertEquals(array(i).asInstanceOf[java.lang.Integer].intValue(), i)
  }

  test("sort large DynamicArray with Ordering") {
    val rng = java.util.Random(123)
    val n   = 300
    val da  = DynamicArray[Int]()
    (0 until n).foreach(_ => da.add(rng.nextInt(1000)))
    Sort.sort(da, Ordering.Int)
    for (i <- 1 until da.size)
      assert(da(i - 1) <= da(i), s"Out of order at $i: ${da(i - 1)} > ${da(i)}")
  }
}
