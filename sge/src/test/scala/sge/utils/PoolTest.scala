/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class PoolTest extends munit.FunSuite {

  private class StringPool(override protected val max: Int = 100, override protected val initialCapacity: Int = 16) extends Pool[String] {
    var created:                        Int    = 0
    override protected def newObject(): String = {
      created += 1
      s"obj-$created"
    }
  }

  test("obtain creates new objects") {
    val pool = StringPool()
    val obj  = pool.obtain()
    assert(obj.startsWith("obj-"))
    assertEquals(pool.created, 1)
  }

  test("free and obtain reuses objects") {
    val pool = StringPool()
    val obj  = pool.obtain()
    pool.free(obj)
    val reused = pool.obtain()
    assert(reused eq obj)
  }

  test("fill pre-allocates objects and updates peak") {
    // Regression: Pool.fill() had peak update outside method body
    val pool = StringPool()
    assertEquals(pool.peak, 0)
    pool.fill(5)
    assertEquals(pool.peak, 5)
    assertEquals(pool.created, 5)
  }

  test("fill respects max") {
    val pool = StringPool(max = 3)
    pool.fill(10)
    // Should only fill up to max
    assertEquals(pool.created, 3)
    assertEquals(pool.peak, 3)
  }

  test("free does not exceed max") {
    val pool = StringPool(max = 2)
    pool.free("a")
    pool.free("b")
    pool.free("c") // should be discarded
    assertEquals(pool.peak, 2)
    pool.obtain()
    pool.obtain()
    val o3 = pool.obtain() // new object — pool exhausted
    assert(o3.startsWith("obj-"))
  }
}
