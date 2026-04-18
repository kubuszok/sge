/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Tests ported from libGDX LongArrayTest.java
 */
package sge
package utils

/** Tests for DynamicArray[Long], ported from libGDX LongArrayTest.java.
  *
  * LongArray-specific numeric operations (incr, mul) are tested via direct element access since SGE's unified DynamicArray does not have primitive-specific arithmetic methods.
  */
class DynamicArrayLongSuite extends munit.FunSuite {

  // ---- insert / insertRange ----

  test("Long: insert into ordered array") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(1L, 3L, 4L, 5L, 6L), 0, 5)
    arr.insert(1, 2L)
    assertEquals(arr.toArray.toSeq, Seq(1L, 2L, 3L, 4L, 5L, 6L))
  }

  test("Long: insertRange into ordered array") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(1L, 3L, 4L, 5L, 6L), 0, 5)
    arr.insert(1, 2L)
    // arr is now [1, 2, 3, 4, 5, 6]
    arr.insertRange(2, 3)
    // insertRange inserts 3 empty slots at index 2, shifting [3, 4, 5, 6] right
    // Slots 2..4 contain copies of the shifted values, arr size is now 9
    assertEquals(arr.size, 9)
    assertEquals(arr(0), 1L)
    assertEquals(arr(1), 2L)
    // The shifted elements land at indices 5..8
    assertEquals(arr(5), 3L)
    assertEquals(arr(6), 4L)
    assertEquals(arr(7), 5L)
    assertEquals(arr(8), 6L)
  }

  test("Long: insertRange out of bounds throws") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(1L, 3L, 4L, 5L, 6L), 0, 5)
    intercept[IndexOutOfBoundsException] {
      arr.insertRange(400, 4)
    }
  }

  test("Long: insert into unordered array") {
    val arr = DynamicArray[Long](false, 16)
    arr.addAll(Array(1L, 3L, 4L, 5L, 6L), 0, 5)
    arr.insert(1, 2L)
    // Unordered insert swaps displaced element to end
    assertEquals(arr.toArray.toSeq, Seq(1L, 2L, 4L, 5L, 6L, 3L))
  }

  test("Long: insert out of bounds in unordered throws") {
    val arr = DynamicArray[Long](false, 16)
    arr.addAll(Array(1L, 3L, 4L, 5L, 6L), 0, 5)
    intercept[IndexOutOfBoundsException] {
      arr.insert(2783, 3L)
    }
  }

  // ---- swap ----

  test("Long: swap elements") {
    val arr = DynamicArray.from(Array(1L, 3L, 4L, 5L, 6L))
    arr.swap(1, 4)
    assertEquals(arr.toArray.toSeq, Seq(1L, 6L, 4L, 5L, 3L))
  }

  test("Long: swap out of bounds first index throws") {
    val arr = DynamicArray.from(Array(1L, 3L, 4L, 5L, 6L))
    intercept[IndexOutOfBoundsException] {
      arr.swap(100, 3)
    }
  }

  test("Long: swap out of bounds second index throws") {
    val arr = DynamicArray.from(Array(1L, 3L, 4L, 5L, 6L))
    intercept[IndexOutOfBoundsException] {
      arr.swap(3, 100)
    }
  }

  // ---- pop / peek / first ----

  test("Long: first, peek, pop") {
    val arr = DynamicArray[Long]()
    for (i <- 1L to 10L) arr.add(i)
    assertEquals(arr.first, 1L)
    assertEquals(arr.peek, 10L)
    assertEquals(arr.pop(), 10L)
    assertEquals(arr.toArray.toSeq, (1L to 9L).toSeq)
  }

  test("Long: first on empty throws") {
    val arr = DynamicArray[Long]()
    intercept[IndexOutOfBoundsException] {
      arr.first
    }
  }

  test("Long: pop on empty throws") {
    val arr = DynamicArray[Long]()
    intercept[IndexOutOfBoundsException] {
      arr.pop()
    }
  }

  test("Long: peek on empty throws") {
    val arr = DynamicArray[Long]()
    intercept[IndexOutOfBoundsException] {
      arr.peek
    }
  }

  // ---- shrink ----

  test("Long: shrink trims backing array to size") {
    // Default DynamicArray will have backing array larger than 3 elements
    val arr = DynamicArray[Long]()
    arr.add(1L, 2L, 3L)
    arr.shrink()
    assertEquals(arr.toArray.toSeq, Seq(1L, 2L, 3L))
    assertEquals(arr.items.length, 3)
  }

  // ---- ensureCapacity ----

  test("Long: ensureCapacity grows backing array") {
    val arr1 = DynamicArray.from(
      Array(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L, 89L, 0L, 10L, 389L, 8L, 392L, 4L, 27346L, 2L, 234L, 12L)
    )
    val arr2 = DynamicArray.from(Array(1L, 2L, 3L))

    arr2.ensureCapacity(2)
    assert(arr2.items.length >= 5) // size(3) + additional(2)
    assertEquals(arr2.size, 3) // size unchanged

    arr1.ensureCapacity(18)
    assert(arr1.items.length >= 39) // size(21) + additional(18)
    assertEquals(arr1.size, 21) // size unchanged
  }

  // ---- setSize ----

  test("Long: setSize grows") {
    val arr = DynamicArray.from(
      Array(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L, 89L, 90L, 10L, 389L, 8L, 392L, 4L, 27346L, 2L, 234L, 12L)
    )
    arr.setSize(23)
    assertEquals(arr.size, 23)
  }

  test("Long: setSize truncates") {
    val arr = DynamicArray.from(
      Array(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L, 89L, 90L, 10L, 389L, 8L, 392L, 4L, 27346L, 2L, 234L, 12L)
    )
    arr.setSize(10)
    assertEquals(arr.toArray.toSeq, Seq(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L))
  }

  test("Long: setSize to larger then check items") {
    val arr = DynamicArray.from(Array(1L, 2L, 3L))
    arr.setSize(5)
    assertEquals(arr.size, 5)
    // First 3 elements preserved
    assertEquals(arr(0), 1L)
    assertEquals(arr(1), 2L)
    assertEquals(arr(2), 3L)
    // Expanded slots are zero-initialized for primitives
    assertEquals(arr(3), 0L)
    assertEquals(arr(4), 0L)
  }

  // ---- resize (via setSize, since resize is private in SGE) ----

  test("Long: resize equivalent via setSize") {
    val arr = DynamicArray.from(
      Array(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L, 89L, 90L, 10L, 389L, 8L, 392L, 4L, 27346L, 2L, 234L, 12L)
    )
    arr.setSize(23)
    assertEquals(arr.size, 23)
    // Original 21 elements preserved
    assertEquals(arr(0), 1L)
    assertEquals(arr(20), 12L)
    // Extended slots are zero-initialized
    assertEquals(arr(21), 0L)
    assertEquals(arr(22), 0L)
  }

  // ---- equals ----

  test("Long: equality for same content ordered arrays") {
    val arr1 = DynamicArray[Long]()
    val arr2 = DynamicArray[Long]()
    arr1.add(1L, 2L)
    arr2.add(1L, 2L)
    assert(arr1 == arr2)
  }

  test("Long: inequality with different class") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 2L)
    val other = ArrayMap[Int, Int]()
    assert(arr != other)
  }

  test("Long: unordered and ordered arrays are not equal") {
    val arr1 = DynamicArray[Long]()
    arr1.add(1L, 2L)
    val arr3 = DynamicArray[Long](false, 16)
    arr3.add(1L, 2L)
    assert(arr1 != arr3)
  }

  test("Long: capacity does not affect equality") {
    val arr1 = DynamicArray[Long]()
    arr1.add(1L, 2L)
    val arr4 = DynamicArray[Long](true, 12)
    arr4.add(1L, 2L)
    assert(arr1 == arr4)
  }

  test("Long: different content arrays are not equal") {
    val arr1 = DynamicArray[Long]()
    val arr2 = DynamicArray[Long]()
    arr1.add(1L, 2L)
    arr2.add(1L, 2L)
    arr1.add(3L)
    assert(arr1 != arr2)
  }

  // ---- remove (full) ----

  test("Long: removeValue") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 3L, 4L)
    arr.add(5L, 6L, 6L)
    arr.add(3L, 9L)
    assert(arr.removeValue(3L))
    assertEquals(arr.toArray.toSeq, Seq(1L, 4L, 5L, 6L, 6L, 3L, 9L))
    assertEquals(arr.size, 7)
    assert(!arr.removeValue(99L))
  }

  test("Long: removeIndex") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 4L, 5L)
    arr.add(6L, 6L, 3L)
    arr.add(9L)
    assertEquals(arr.removeIndex(1), 4L)
    assertEquals(arr.toArray.toSeq, Seq(1L, 5L, 6L, 6L, 3L, 9L))
    assertEquals(arr.size, 6)
  }

  test("Long: removeIndex out of bounds throws") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 5L, 6L)
    arr.add(6L, 3L, 9L)
    intercept[IndexOutOfBoundsException] {
      arr.removeIndex(56)
    }
  }

  test("Long: removeRange") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(1L, 10L, 25L, 2L, 23L, 345L), 0, 6)
    // LibGDX removeRange(2, 5) is inclusive [2..5], SGE removeRange is [start, end) exclusive
    // LibGDX: removes indices 2,3,4,5 -> leaves [1, 10]
    // SGE equivalent: removeRange(2, 6) removes indices 2,3,4,5
    arr.removeRange(2, 6)
    assertEquals(arr.toArray.toSeq, Seq(1L, 10L))
  }

  test("Long: removeRange out of bounds throws") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 10L)
    intercept[IndexOutOfBoundsException] {
      // end > size
      arr.removeRange(0, 5)
    }
  }

  test("Long: removeAll") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(1L, 10L, 25L, 35L, 50L, 40L), 0, 6)
    val toRemove = DynamicArray.from(Array(1L, 25L, 35L))
    assert(arr.removeAll(toRemove))
    assertEquals(arr.toArray.toSeq, Seq(10L, 50L, 40L))
    assert(!arr.removeAll(toRemove))
  }

  test("Long: removeAll partial match") {
    val arr = DynamicArray[Long]()
    arr.add(50L, 40L)
    // Only 10 exists, 30 and 22 do not
    val toRemove2 = DynamicArray.from(Array(10L, 30L, 22L))
    assert(!arr.removeAll(toRemove2))
    assertEquals(arr.toArray.toSeq, Seq(50L, 40L))
  }

  // ---- reverse ----

  test("Long: reverse") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 2L, 3L)
    arr.add(4L, 5L)
    arr.reverse()
    assertEquals(arr.toArray.toSeq, Seq(5L, 4L, 3L, 2L, 1L))
  }

  // ---- incr (element-wise increment, no dedicated method in SGE) ----

  test("Long: element-wise increment at index") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    arr(3) = arr(3) + 45L
    assertEquals(arr(3), 46L)
  }

  test("Long: element-wise increment all") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    // Increment element at index 3 by 45
    arr(3) = arr(3) + 45L
    // Then increment all by 3
    var i = 0
    while (i < arr.size) {
      arr(i) = arr(i) + 3L
      i += 1
    }
    assertEquals(arr.toArray.toSeq, Seq(6L, 7L, 8L, 49L, 59L, 35L))
  }

  test("Long: element access at out of bounds index throws") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    intercept[IndexOutOfBoundsException] {
      arr(28)
    }
  }

  // ---- mul (element-wise multiply, no dedicated method in SGE) ----

  test("Long: element-wise multiply at index") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    arr(1) = arr(1) * 3L
    assertEquals(arr(1), 12L)
  }

  test("Long: element-wise multiply all") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    // Multiply element at index 1 by 3
    arr(1) = arr(1) * 3L
    // Then multiply all by 2
    var i = 0
    while (i < arr.size) {
      arr(i) = arr(i) * 2L
      i += 1
    }
    assertEquals(arr.toArray.toSeq, Seq(6L, 24L, 10L, 2L, 112L, 64L))
  }

  test("Long: element update at out of bounds index throws") {
    val arr = DynamicArray.from(Array(3L, 4L, 5L, 1L, 56L, 32L))
    intercept[IndexOutOfBoundsException] {
      arr(17) = arr(17) * 8L
    }
  }

  // ---- addAll variants ----

  test("Long: add single element") {
    val arr = DynamicArray[Long](3)
    arr.add(3L)
    assertEquals(arr.toArray.toSeq, Seq(3L))
  }

  test("Long: add two elements") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 2L)
    assertEquals(arr.toArray.toSeq, Seq(1L, 2L))
  }

  test("Long: addAll from DynamicArray") {
    val arr2 = DynamicArray[Long]()
    arr2.add(1L, 2L)
    val arr3 = DynamicArray[Long]()
    arr3.addAll(arr2)
    assertEquals(arr3.toArray.toSeq, arr2.toArray.toSeq)
  }

  test("Long: addAll from DynamicArray chained") {
    val arr1 = DynamicArray[Long](3)
    arr1.add(3L)
    val arr2 = DynamicArray[Long]()
    arr2.add(1L, 2L)
    val arr3 = DynamicArray[Long]()
    arr3.addAll(arr2)
    arr3.addAll(arr1)
    assertEquals(arr3.toArray.toSeq, Seq(1L, 2L, 3L))
  }

  test("Long: addAll from plain array with many elements") {
    val arr = DynamicArray[Long]()
    arr.add(1L, 2L, 3L)
    arr.addAll(Array(4L, 5L, 6L, 2L, 8L, 10L, 1L, 6L, 2L, 3L, 30L, 31L, 25L, 20L), 0, 14)
    assertEquals(arr.size, 17)
    assertEquals(
      arr.toArray.toSeq,
      Seq(1L, 2L, 3L, 4L, 5L, 6L, 2L, 8L, 10L, 1L, 6L, 2L, 3L, 30L, 31L, 25L, 20L)
    )
  }

  test("Long: addAll from plain array with offset and length") {
    val arr = DynamicArray[Long]()
    arr.addAll(Array(4L, 5L, 6L, 2L, 21L, 45L, 78L), 3, 3)
    assertEquals(arr.toArray.toSeq, Seq(2L, 21L, 45L))
  }

  // ---- sort and reverse combined ----

  test("Long: sort then reverse") {
    val arr    = DynamicArray[Long]()
    val values = Array(1L, 2L, 4L, 6L, 32L, 53L, 564L, 53L, 2L, 1L, 89L, 90L, 10L, 389L, 8L, 392L, 4L, 27346L, 2L, 234L, 12L)
    arr.addAll(values, 0, values.length)
    arr.sort()
    assertEquals(
      arr.toArray.toSeq,
      Seq(1L, 1L, 2L, 2L, 2L, 4L, 4L, 6L, 8L, 10L, 12L, 32L, 53L, 53L, 89L, 90L, 234L, 389L, 392L, 564L, 27346L)
    )
    arr.reverse()
    assertEquals(
      arr.toArray.toSeq,
      Seq(27346L, 564L, 392L, 389L, 234L, 90L, 89L, 53L, 53L, 32L, 12L, 10L, 8L, 6L, 4L, 4L, 2L, 2L, 2L, 1L, 1L)
    )
  }
}
