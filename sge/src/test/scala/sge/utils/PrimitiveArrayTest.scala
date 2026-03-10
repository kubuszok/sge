/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

/** Tests DynamicArray with primitive element types (Int, Float, Long),
  * verifying that the unified DynamicArray correctly replaces the
  * LibGDX IntArray, FloatArray, and LongArray classes.
  */
class PrimitiveArrayTest extends munit.FunSuite {

  // ==== IntArray (DynamicArray[Int]) ====

  test("Int: add, get, set, removeIndex") {
    val arr = DynamicArray[Int]()
    arr.add(10)
    arr.add(20)
    arr.add(30)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 10)
    assertEquals(arr(1), 20)
    assertEquals(arr(2), 30)

    arr(1) = 99
    assertEquals(arr(1), 99)

    val removed = arr.removeIndex(1)
    assertEquals(removed, 99)
    assertEquals(arr.size, 2)
    assertEquals(arr(0), 10)
    assertEquals(arr(1), 30)
  }

  test("Int: contains and indexOf") {
    val arr = DynamicArray[Int]()
    arr.add(5)
    arr.add(10)
    arr.add(15)
    arr.add(10)

    assert(arr.contains(10))
    assert(!arr.contains(42))

    assertEquals(arr.indexOf(10), 1)
    assertEquals(arr.indexOf(42), -1)
    assertEquals(arr.lastIndexOf(10), 3)
  }

  test("Int: sort") {
    val arr = DynamicArray[Int]()
    arr.add(30)
    arr.add(10)
    arr.add(20)
    arr.add(5)
    arr.sort()
    assertEquals(arr(0), 5)
    assertEquals(arr(1), 10)
    assertEquals(arr(2), 20)
    assertEquals(arr(3), 30)
  }

  test("Int: clear, size, isEmpty, toArray") {
    val arr = DynamicArray[Int]()
    assert(arr.isEmpty)
    assertEquals(arr.size, 0)

    arr.add(1)
    arr.add(2)
    arr.add(3)
    assert(!arr.isEmpty)
    assertEquals(arr.size, 3)

    val plain = arr.toArray
    assertEquals(plain.length, 3)
    assertEquals(plain(0), 1)
    assertEquals(plain(1), 2)
    assertEquals(plain(2), 3)

    arr.clear()
    assert(arr.isEmpty)
    assertEquals(arr.size, 0)
  }

  // ==== FloatArray (DynamicArray[Float]) ====

  test("Float: add, get, set, removeIndex") {
    val arr = DynamicArray[Float]()
    arr.add(1.5f)
    arr.add(2.5f)
    arr.add(3.5f)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1.5f)
    assertEquals(arr(1), 2.5f)
    assertEquals(arr(2), 3.5f)

    arr(1) = 9.9f
    assertEquals(arr(1), 9.9f)

    val removed = arr.removeIndex(0)
    assertEquals(removed, 1.5f)
    assertEquals(arr.size, 2)
    assertEquals(arr(0), 9.9f)
    assertEquals(arr(1), 3.5f)
  }

  test("Float: contains and indexOf") {
    val arr = DynamicArray[Float]()
    arr.add(0.1f)
    arr.add(0.2f)
    arr.add(0.3f)
    arr.add(0.2f)

    assert(arr.contains(0.2f))
    assert(!arr.contains(0.9f))

    assertEquals(arr.indexOf(0.2f), 1)
    assertEquals(arr.indexOf(0.9f), -1)
    assertEquals(arr.lastIndexOf(0.2f), 3)
  }

  test("Float: sort") {
    val arr = DynamicArray[Float]()
    arr.add(3.0f)
    arr.add(1.0f)
    arr.add(2.0f)
    arr.add(0.5f)
    arr.sort()
    assertEquals(arr(0), 0.5f)
    assertEquals(arr(1), 1.0f)
    assertEquals(arr(2), 2.0f)
    assertEquals(arr(3), 3.0f)
  }

  test("Float: clear, size, isEmpty, toArray") {
    val arr = DynamicArray[Float]()
    assert(arr.isEmpty)

    arr.add(1.1f)
    arr.add(2.2f)
    assert(!arr.isEmpty)
    assertEquals(arr.size, 2)

    val plain = arr.toArray
    assertEquals(plain.length, 2)
    assertEquals(plain(0), 1.1f)
    assertEquals(plain(1), 2.2f)

    arr.clear()
    assert(arr.isEmpty)
    assertEquals(arr.size, 0)
  }

  // ==== LongArray (DynamicArray[Long]) ====

  test("Long: add, get, set, removeIndex") {
    val arr = DynamicArray[Long]()
    arr.add(100L)
    arr.add(200L)
    arr.add(300L)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 100L)
    assertEquals(arr(1), 200L)
    assertEquals(arr(2), 300L)

    arr(2) = 999L
    assertEquals(arr(2), 999L)

    val removed = arr.removeIndex(0)
    assertEquals(removed, 100L)
    assertEquals(arr.size, 2)
    assertEquals(arr(0), 200L)
    assertEquals(arr(1), 999L)
  }

  test("Long: contains and indexOf") {
    val arr = DynamicArray[Long]()
    arr.add(Long.MaxValue)
    arr.add(0L)
    arr.add(Long.MinValue)
    arr.add(0L)

    assert(arr.contains(Long.MaxValue))
    assert(arr.contains(Long.MinValue))
    assert(!arr.contains(42L))

    assertEquals(arr.indexOf(0L), 1)
    assertEquals(arr.lastIndexOf(0L), 3)
    assertEquals(arr.indexOf(42L), -1)
  }

  test("Long: sort") {
    val arr = DynamicArray[Long]()
    arr.add(300L)
    arr.add(100L)
    arr.add(200L)
    arr.add(50L)
    arr.sort()
    assertEquals(arr(0), 50L)
    assertEquals(arr(1), 100L)
    assertEquals(arr(2), 200L)
    assertEquals(arr(3), 300L)
  }

  test("Long: clear, size, isEmpty, toArray") {
    val arr = DynamicArray[Long]()
    assert(arr.isEmpty)

    arr.add(10L)
    arr.add(20L)
    arr.add(30L)
    assert(!arr.isEmpty)
    assertEquals(arr.size, 3)

    val plain = arr.toArray
    assertEquals(plain.length, 3)
    assertEquals(plain(0), 10L)
    assertEquals(plain(1), 20L)
    assertEquals(plain(2), 30L)

    arr.clear()
    assert(arr.isEmpty)
    assertEquals(arr.size, 0)
  }
}
