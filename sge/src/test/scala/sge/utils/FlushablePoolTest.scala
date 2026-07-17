/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original test: com/badlogic/gdx/utils/FlushablePoolTest.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import lowlevel.util.DynamicArray

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
    assertEquals(flushablePool.free, 0)
  }

  test("initialize flushable pool with initial capacity") {
    val flushablePool = TestPool(10)
    assertEquals(flushablePool.free, 0)
  }

  test("initialize flushable pool with initial capacity and max") {
    val flushablePool = TestPool(10, 10)
    assertEquals(flushablePool.free, 0)
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

  test(
    "ISS-801: freeAll(DynamicArray) must clear `obtained` so a later flush() does not double-free the same instances"
  ) {
    // Upstream FlushablePool overrides its ONLY freeAll overload (freeAll(Array<T>)),
    // where it removes the freed items from `obtained` before delegating to
    // super.freeAll. SGE split freeAll into freeAll(Iterable) + freeAll(DynamicArray)
    // and (before ISS-801) only overrode the Iterable one, so freeing through the
    // DynamicArray overload left the items in `obtained`; the next flush() then
    // freed them a SECOND time (double-free = same instance in the free list twice).
    val flushablePool = TestPool(8, 8)

    // Obtain two elements (tracked in `obtained`).
    val element1 = flushablePool.obtain()
    val element2 = flushablePool.obtain()

    // Free them through the DynamicArray overload — the one upstream's
    // freeAll(Array<T>) actually maps to. DynamicArray is not scala Iterable,
    // so this resolves to freeAll(DynamicArray), not freeAll(Iterable).
    val batch = DynamicArray.createRef[String]()
    batch.add(element1)
    batch.add(element2)
    flushablePool.freeAll(batch)

    // Exactly like the freeAll(Iterable) contract above, the freed instances
    // must have been removed from `obtained`.
    assert(!flushablePool.obtainedItems.contains(element1), "freeAll(DynamicArray) must remove freed items from `obtained`")
    assert(!flushablePool.obtainedItems.contains(element2), "freeAll(DynamicArray) must remove freed items from `obtained`")

    // flush() frees whatever is STILL tracked in `obtained`. If freeAll left
    // element1/element2 there, they are freed a second time — the free list then
    // holds each instance twice, so two callers can obtain the SAME instance.
    flushablePool.flush()

    val freeSlots  = flushablePool.free
    val handed     = (0 until freeSlots).map(_ => flushablePool.obtain())
    val seen       = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap[String, java.lang.Boolean])
    val duplicates = handed.count(item => !seen.add(item))
    assertEquals(
      duplicates,
      0,
      s"double-free via freeAll(DynamicArray)+flush(): $duplicates of $freeSlots free slots are the same instance handed out twice"
    )
  }
}
