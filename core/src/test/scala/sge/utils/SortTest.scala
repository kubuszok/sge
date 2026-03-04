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
}
