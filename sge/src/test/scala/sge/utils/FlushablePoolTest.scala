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

  /** Structurally-equal-but-distinct pooled item: `equals` is deliberately degenerate (ALL instances are `==`) to expose value-equality removal evicting the WRONG instance from `obtained`. Upstream
    * FlushablePool.java removes with identity=true (`removeAll(objects, true)` at :58, `removeValue(object, true)` at :52) precisely because pooled instances can be structurally equal yet distinct.
    */
  final private class EqualItem {
    override def equals(other: Any): Boolean = other.isInstanceOf[EqualItem]
    override def hashCode:           Int     = 42
  }

  private class EqualItemPool extends Pool.Flushable[EqualItem] {
    override protected val max:             Int = Int.MaxValue
    override protected val initialCapacity: Int = 16

    override def newObject(): EqualItem = new EqualItem

    /** Public accessor for the protected `obtained` field. */
    def obtainedItems: DynamicArray[EqualItem] = obtained
  }

  test(
    "ISS-801 bounce#1: freeAll(DynamicArray) must remove from `obtained` by REFERENCE identity — value equality evicts the still-checked-out equal instance, leaking/double-freeing via flush()"
  ) {
    val pool = new EqualItemPool
    val a    = pool.obtain()
    val b    = pool.obtain()
    assert(a ne b, "precondition: two distinct instances")
    assert(a == b, "precondition: structurally equal by construction")

    // Free ONLY instance A through the DynamicArray overload.
    val batch = DynamicArray.createRef[EqualItem]()
    batch.add(a)
    pool.freeAll(batch)

    // A must be gone from `obtained` BY REFERENCE; B (a == b but b ne a) must
    // still be tracked. Value-equality removal (DynamicArray.removeAll) evicts
    // B too — the wrong instance — so flush() never frees it (leak), while the
    // symmetric free()-path bug double-frees (see next test).
    assert(!pool.obtainedItems.containsByRef(a), "freed instance A must be removed from `obtained` by reference")
    assert(
      pool.obtainedItems.containsByRef(b),
      "instance B is still checked out — removal from `obtained` must be identity-based (FlushablePool.java:58 removeAll(objects, true)), value equality evicted the WRONG instance"
    )

    // flush() must free exactly B, once. Afterwards the free list holds exactly
    // {A, B} as two DISTINCT instances.
    pool.flush()
    assertEquals(
      pool.free,
      2,
      "free list must hold exactly A (via freeAll) and B (via flush) — 1 means B leaked, 3+ means a double-free"
    )
    val first  = pool.obtain()
    val second = pool.obtain()
    assert(first ne second, "double-free: the same instance was handed out twice")
    assert((first eq a) || (first eq b))
    assert((second eq a) || (second eq b))
  }

  test(
    "ISS-801 bounce#1: free(obj) and freeAll(Iterable) must also remove from `obtained` by REFERENCE identity"
  ) {
    val pool = new EqualItemPool
    val a    = pool.obtain()
    val b    = pool.obtain()

    // free(B): value-equality removeValue evicts A (first structural match) —
    // the wrong instance — leaving B in `obtained` to be double-freed by a
    // later flush(). Upstream frees with identity=true (FlushablePool.java:52).
    pool.free(b)
    assert(!pool.obtainedItems.containsByRef(b), "freed instance B must be removed from `obtained` by reference")
    assert(pool.obtainedItems.containsByRef(a), "free(B) must not evict the still-checked-out A from `obtained`")

    // freeAll(Iterable) with A: same identity contract.
    pool.freeAll(Seq(a))
    assert(!pool.obtainedItems.containsByRef(a), "freeAll(Iterable) must remove freed items from `obtained` by reference")
    assertEquals(pool.obtainedItems.size, 0)
  }
}
