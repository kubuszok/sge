/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original test: com/badlogic/gdx/utils/FlushablePoolTest.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

class FlushablePoolTest extends munit.FunSuite {

  /** Test implementation of a flushable pool. Exposes `obtained` via a public accessor since the trait declares it as `protected`.
    */
  class TestPool(cap: Int, mx: Int) extends Pool.Default[String](() => "UNUSED", cap, mx) with Pool.Flushable[String] {
    // Track call count for newObject
    private var counter: Int = 0

    override def newObject(): String = {
      counter += 1
      counter.toString
    }

    /** Public accessor for the protected `obtained` field. */
    def obtainedItems: DynamicArray[String] = obtained
  }

  object TestPool {
    def apply():                               TestPool = new TestPool(16, Int.MaxValue)
    def apply(initialCapacity: Int):           TestPool = new TestPool(initialCapacity, Int.MaxValue)
    def apply(initialCapacity: Int, max: Int): TestPool = new TestPool(initialCapacity, max)
  }

  test("initialize flushable pool default") {
    val flushablePool = TestPool()
    assertEquals(flushablePool.getFree, 0)
  }

  test("initialize flushable pool with initial capacity") {
    val flushablePool = TestPool(10)
    assertEquals(flushablePool.getFree, 0)
  }

  test("initialize flushable pool with initial capacity and max") {
    val flushablePool = TestPool(10, 10)
    assertEquals(flushablePool.getFree, 0)
  }

  test("obtain") {
    val flushablePool = TestPool(10, 10)
    assertEquals(flushablePool.obtainedItems.size, 0)
    flushablePool.obtain()
    assertEquals(flushablePool.obtainedItems.size, 1)
    flushablePool.flush()
    assertEquals(flushablePool.obtainedItems.size, 0)
  }

  test("flush") {
    val flushablePool = TestPool(10, 10)
    flushablePool.obtain()
    assertEquals(flushablePool.obtainedItems.size, 1)
    flushablePool.flush()
    assertEquals(flushablePool.obtainedItems.size, 0)
  }

  test("free") {
    // Create the flushable pool.
    val flushablePool = TestPool(10, 10)

    // Obtain the elements.
    val element1 = flushablePool.obtain()
    val element2 = flushablePool.obtain()

    // Test preconditions.
    assert(flushablePool.obtainedItems.contains(element1))
    assert(flushablePool.obtainedItems.contains(element2))

    // Free element and check containment.
    flushablePool.free(element2)
    assert(flushablePool.obtainedItems.contains(element1))
    assert(!flushablePool.obtainedItems.contains(element2))
  }

  test("freeAll") {
    // Create the flushable pool.
    val flushablePool = TestPool(5, 5)

    // Obtain the elements.
    val element1 = flushablePool.obtain()
    val element2 = flushablePool.obtain()

    // Create iterable with elements.
    val elements = Seq(element1, element2)

    // Test preconditions.
    assert(flushablePool.obtainedItems.contains(element1))
    assert(flushablePool.obtainedItems.contains(element2))

    // Free elements and check containment.
    flushablePool.freeAll(elements)
    assert(!flushablePool.obtainedItems.contains(element1))
    assert(!flushablePool.obtainedItems.contains(element2))
  }
}
