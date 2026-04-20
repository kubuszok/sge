/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class SelectTest extends munit.FunSuite {

  // Select.selectIndex casts items to Array[AnyRef] for the quickselect path,
  // so tests that exercise kthLowest != 1 and != size must use reference types.
  private val intOrd: Ordering[java.lang.Integer] =
    (a: java.lang.Integer, b: java.lang.Integer) => a.compareTo(b)

  private def boxed(values: Int*): Array[java.lang.Integer] =
    values.map(java.lang.Integer.valueOf).toArray

  test("select kthLowest=1 returns minimum") {
    val items  = boxed(5, 3, 8, 1, 9, 2)
    val result = Select.select(items, intOrd, 1, items.length)
    assertEquals(result.intValue(), 1)
  }

  test("select kthLowest=size returns maximum") {
    val items  = boxed(5, 3, 8, 1, 9, 2)
    val result = Select.select(items, intOrd, items.length, items.length)
    assertEquals(result.intValue(), 9)
  }

  test("select kthLowest=3 returns 3rd smallest") {
    val items = boxed(5, 3, 8, 1, 9, 2)
    // sorted: 1, 2, 3, 5, 8, 9 -> 3rd = 3
    val result = Select.select(items, intOrd, 3, items.length)
    assertEquals(result.intValue(), 3)
  }

  test("select median element") {
    val items = boxed(7, 2, 9, 4, 5, 1, 8)
    // sorted: 1, 2, 4, 5, 7, 8, 9 -> 4th = 5
    val result = Select.select(items, intOrd, 4, items.length)
    assertEquals(result.intValue(), 5)
  }

  test("selectIndex kthLowest=1 returns index of minimum") {
    val items = boxed(5, 3, 8, 1, 9, 2)
    val idx   = Select.selectIndex(items, intOrd, 1, items.length)
    assertEquals(items(idx).intValue(), 1)
  }

  test("selectIndex kthLowest=size returns index of maximum") {
    val items = boxed(5, 3, 8, 1, 9, 2)
    val idx   = Select.selectIndex(items, intOrd, items.length, items.length)
    assertEquals(items(idx).intValue(), 9)
  }

  test("select single element array") {
    val items = boxed(42)
    assertEquals(Select.select(items, intOrd, 1, 1).intValue(), 42)
  }

  test("select two element array min and max") {
    val items = boxed(10, 3)
    assertEquals(Select.select(items, intOrd, 1, 2).intValue(), 3)
    // Items may be rearranged by previous select, reset
    val items2 = boxed(10, 3)
    assertEquals(Select.select(items2, intOrd, 2, 2).intValue(), 10)
  }

  test("select with custom reverse ordering") {
    val items = boxed(5, 3, 8, 1, 9, 2)
    // With reverse ordering, kthLowest=1 should return the maximum
    val result = Select.select(items, intOrd.reverse, 1, items.length)
    assertEquals(result.intValue(), 9)
  }

  test("select throws on empty array (size < 1)") {
    val items = new Array[java.lang.Integer](0)
    intercept[SgeError.InvalidInput] {
      Select.select(items, intOrd, 1, 0)
    }
  }

  test("select throws when kthLowest > size") {
    val items = boxed(1, 2, 3)
    intercept[SgeError.InvalidInput] {
      Select.select(items, intOrd, 4, 3)
    }
  }

  test("select with subset size") {
    val items = boxed(5, 3, 8, 1, 9, 2, 7, 4)
    // Only consider first 4 elements: 5, 3, 8, 1
    // sorted subset: 1, 3, 5, 8 -> 2nd = 3
    val result = Select.select(items, intOrd, 2, 4)
    assertEquals(result.intValue(), 3)
  }

  test("select all equal elements") {
    val items = boxed(5, 5, 5, 5, 5)
    assertEquals(Select.select(items, intOrd, 1, 5).intValue(), 5)
    val items2 = boxed(5, 5, 5, 5, 5)
    assertEquals(Select.select(items2, intOrd, 3, 5).intValue(), 5)
    val items3 = boxed(5, 5, 5, 5, 5)
    assertEquals(Select.select(items3, intOrd, 5, 5).intValue(), 5)
  }

  test("select large array with quickselect path") {
    val rng    = java.util.Random(42)
    val n      = 200
    val items  = Array.fill(n)(java.lang.Integer.valueOf(rng.nextInt(10000)))
    val sorted = items.sorted(using intOrd)
    // Select the 50th smallest
    val result = Select.select(items, intOrd, 50, n)
    assertEquals(result.intValue(), sorted(49).intValue())
  }

  test("selectIndex returns valid index for middle elements") {
    val items = boxed(10, 20, 30, 40, 50)
    val idx   = Select.selectIndex(items, intOrd, 3, 5)
    assertEquals(items(idx).intValue(), 30)
  }

  test("select with string ordering") {
    val items  = Array("cherry", "apple", "banana", "date")
    val result = Select.select(items, Ordering.String, 1, items.length)
    assertEquals(result, "apple")
  }

  test("select with string ordering quickselect path") {
    val items = Array("cherry", "apple", "banana", "date", "elderberry")
    // sorted: apple, banana, cherry, date, elderberry -> 3rd = cherry
    val result = Select.select(items, Ordering.String, 3, items.length)
    assertEquals(result, "cherry")
  }
}
