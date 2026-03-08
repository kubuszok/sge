/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class ArrayMapTest extends munit.FunSuite {

  test("empty map") {
    val map = ArrayMap[String, Int]()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("put and get") {
    val map = ArrayMap[String, Int]()
    map.put("a", 1)
    assertEquals(map.get("a").getOrElse(fail("missing")), 1)
    assertEquals(map.size, 1)
  }

  test("put overwrites and returns index") {
    val map = ArrayMap[String, Int]()
    map.put("a", 1)
    val idx = map.put("a", 2)
    assert(idx >= 0)
    assertEquals(map.get("a").getOrElse(fail("missing")), 2)
    assertEquals(map.size, 1)
  }

  test("getKeyAt and getValueAt") {
    val map = ArrayMap[String, Int]()
    map.put("x", 10)
    map.put("y", 20)
    assertEquals(map.getKeyAt(0), "x")
    assertEquals(map.getValueAt(0), 10)
    assertEquals(map.getKeyAt(1), "y")
    assertEquals(map.getValueAt(1), 20)
  }

  test("removeKey") {
    val map = ArrayMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val removed = map.removeKey("a")
    assertEquals(removed.getOrElse(fail("expected")), 1)
    assert(!map.containsKey("a"))
  }

  test("removeIndex") {
    val map = ArrayMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    map.removeIndex(1)
    assertEquals(map.size, 2)
  }

  test("removeValue") {
    val map = ArrayMap[String, java.lang.Integer]()
    map.put("a", java.lang.Integer.valueOf(1))
    map.put("b", java.lang.Integer.valueOf(2))
    assert(map.removeValue(java.lang.Integer.valueOf(1)))
    assert(!map.containsValue(java.lang.Integer.valueOf(1)))
    assertEquals(map.size, 1)
  }

  test("containsKey and containsValue") {
    val map = ArrayMap[String, java.lang.Integer]()
    map.put("x", java.lang.Integer.valueOf(42))
    assert(map.containsKey("x"))
    assert(map.containsValue(java.lang.Integer.valueOf(42)))
    assert(!map.containsKey("y"))
  }

  test("clear") {
    val map = ArrayMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.clear()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("get with default") {
    val map = ArrayMap[String, Int]()
    assertEquals(map.get("missing", 99), 99)
  }

  test("putAll") {
    val map1 = ArrayMap[String, Int]()
    map1.put("a", 1)
    val map2 = ArrayMap[String, Int]()
    map2.put("b", 2)
    map2.putAll(map1)
    assertEquals(map2.size, 2)
    assert(map2.containsKey("a"))
    assert(map2.containsKey("b"))
  }

  test("preserves insertion order") {
    val map = ArrayMap[String, Int]()
    map.put("c", 3)
    map.put("a", 1)
    map.put("b", 2)
    assertEquals(map.getKeyAt(0), "c")
    assertEquals(map.getKeyAt(1), "a")
    assertEquals(map.getKeyAt(2), "b")
  }

  test("many elements") {
    val map = ArrayMap[java.lang.Integer, String]()
    for (i <- 0 until 100)
      map.put(java.lang.Integer.valueOf(i), s"val$i")
    assertEquals(map.size, 100)
    for (i <- 0 until 100)
      assert(map.containsKey(java.lang.Integer.valueOf(i)), s"missing key $i")
  }
}
