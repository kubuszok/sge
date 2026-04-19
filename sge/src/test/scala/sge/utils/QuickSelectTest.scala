/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class QuickSelectTest extends munit.FunSuite {

  test("select finds kth smallest element") {
    val qs = QuickSelect[Int]()
    val items = Array(7, 2, 9, 4, 5)
    // sorted: 2, 4, 5, 7, 9 -> k=1 is index of smallest
    val idx = qs.select(items, Ordering.Int, 1, items.length)
    assertEquals(items(idx), 2)
  }

  test("select finds 3rd smallest") {
    val qs = QuickSelect[Int]()
    val items = Array(7, 2, 9, 4, 5)
    // sorted: 2, 4, 5, 7, 9 -> k=3 is 5
    val idx = qs.select(items, Ordering.Int, 3, items.length)
    assertEquals(items(idx), 5)
  }

  test("select finds largest") {
    val qs = QuickSelect[Int]()
    val items = Array(7, 2, 9, 4, 5)
    val idx = qs.select(items, Ordering.Int, items.length, items.length)
    assertEquals(items(idx), 9)
  }

  test("select single element") {
    val qs = QuickSelect[Int]()
    val items = Array(42)
    val idx = qs.select(items, Ordering.Int, 1, 1)
    assertEquals(items(idx), 42)
  }

  test("select two elements") {
    val qs = QuickSelect[Int]()
    val items = Array(10, 3)
    val idx = qs.select(items, Ordering.Int, 1, 2)
    assertEquals(items(idx), 3)
  }

  test("select equal elements") {
    val qs = QuickSelect[Int]()
    val items = Array(5, 5, 5, 5)
    val idx = qs.select(items, Ordering.Int, 2, items.length)
    assertEquals(items(idx), 5)
  }

  test("select already sorted array") {
    val qs = QuickSelect[Int]()
    val items = Array(1, 2, 3, 4, 5)
    val idx = qs.select(items, Ordering.Int, 4, items.length)
    assertEquals(items(idx), 4)
  }

  test("select reverse sorted array") {
    val qs = QuickSelect[Int]()
    val items = Array(5, 4, 3, 2, 1)
    val idx = qs.select(items, Ordering.Int, 2, items.length)
    assertEquals(items(idx), 2)
  }

  test("select large random array") {
    val qs = QuickSelect[Int]()
    val rng = java.util.Random(123)
    val n = 500
    val items = Array.fill(n)(rng.nextInt(10000))
    val sorted = items.sorted
    // Select the 100th smallest
    val idx = qs.select(items, Ordering.Int, 100, n)
    assertEquals(items(idx), sorted(99))
  }

  test("select with reverse ordering") {
    val qs = QuickSelect[Int]()
    val items = Array(7, 2, 9, 4, 5)
    // reverse ordering: k=1 should be the largest in normal order
    val idx = qs.select(items, Ordering.Int.reverse, 1, items.length)
    assertEquals(items(idx), 9)
  }

  test("select with subset of array") {
    val qs = QuickSelect[Int]()
    val items = Array(5, 3, 8, 1, 9, 2, 7, 4)
    // Only consider first 4 elements: 5, 3, 8, 1
    // sorted subset: 1, 3, 5, 8 -> k=2 = 3
    val idx = qs.select(items, Ordering.Int, 2, 4)
    assertEquals(items(idx), 3)
  }

  test("select can be reused across multiple calls") {
    val qs = QuickSelect[Int]()
    val items1 = Array(10, 20, 30)
    val idx1 = qs.select(items1, Ordering.Int, 1, items1.length)
    assertEquals(items1(idx1), 10)

    val items2 = Array(50, 40, 60)
    val idx2 = qs.select(items2, Ordering.Int, 3, items2.length)
    assertEquals(items2(idx2), 60)
  }
}
