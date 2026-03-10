/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package utils

/** Tests for PoolManager: type-keyed pool registry, obtain/free lifecycle, and Poolable reset integration. */
class PoolManagerTest extends munit.FunSuite {

  // A simple Poolable class to verify reset() is actually called
  private class Tracker extends Pool.Poolable {
    var value:      Int  = 0
    var resetCalls: Int  = 0
    def reset():    Unit = {
      value = 0
      resetCalls += 1
    }
  }

  test("addPool and obtain") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())

    val t = pm.obtain[Tracker]
    assert(t.isInstanceOf[Tracker])
  }

  test("obtain from empty pool creates new object") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())

    val a = pm.obtain[Tracker]
    val b = pm.obtain[Tracker]
    assert(!(a eq b), "two obtains from empty pool should create distinct objects")
  }

  test("free returns object to pool for reuse") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())

    val t = pm.obtain[Tracker]
    pm.free(t)
    val reused = pm.obtain[Tracker]
    assert(reused eq t)
  }

  // Regression: PoolManager.addPool resolved Poolable[T] to noop because T had no
  // Pool.Poolable bound — pool-freed Pool.Poolable objects were never reset.
  test("free calls reset on Pool.Poolable subtypes") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())

    val t = pm.obtain[Tracker]
    t.value = 42
    assertEquals(t.resetCalls, 0)

    pm.free(t)
    assertEquals(t.resetCalls, 1, "reset() should be called on free")
    assertEquals(t.value, 0, "reset() should clear state")
  }

  test("pool-obtained object has clean state after previous free") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())

    val t = pm.obtain[Tracker]
    t.value = 99
    pm.free(t)

    val reused = pm.obtain[Tracker]
    assert(reused eq t, "should reuse same object")
    assertEquals(reused.value, 0, "state should be reset from previous use")
  }

  test("duplicate addPool throws") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())
    intercept[SgeError.InvalidInput] {
      pm.addPool[Tracker](() => Tracker())
    }
  }

  test("obtain for unregistered type throws") {
    val pm = PoolManager()
    intercept[SgeError.InvalidInput] {
      pm.obtain[Tracker]
    }
  }

  test("getPoolOrNull returns empty for unregistered type") {
    val pm = PoolManager()
    assert(pm.getPoolOrNull[Tracker].isEmpty)
  }

  test("hasPool checks registration") {
    val pm = PoolManager()
    assert(!pm.hasPool(classOf[Tracker]))
    pm.addPool[Tracker](() => Tracker())
    assert(pm.hasPool(classOf[Tracker]))
  }

  test("clear empties all pools") {
    val pm = PoolManager()
    pm.addPool[Tracker](() => Tracker())
    pm.free(pm.obtain[Tracker])
    pm.clear()
    // After clear, pool is empty — obtain creates new
    val fresh = pm.obtain[Tracker]
    assertEquals(fresh.resetCalls, 0, "fresh object should not have been reset")
  }
}
