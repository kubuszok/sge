/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class OrderedMapTest extends munit.FunSuite {

  test("empty map") {
    val map = OrderedMap[String, Int]()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("put and get") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    assertEquals(map.get("a").getOrElse(fail("missing")), 1)
    assertEquals(map.size, 1)
  }

  test("put overwrites previous value") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    val old = map.put("a", 2)
    assertEquals(old.getOrElse(fail("expected old")), 1)
    assertEquals(map.get("a").getOrElse(fail("missing")), 2)
    assertEquals(map.size, 1)
  }

  test("remove") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val removed = map.remove("a")
    assertEquals(removed.getOrElse(fail("expected")), 1)
    assert(!map.containsKey("a"))
    assertEquals(map.size, 1)
  }

  test("containsKey and containsValue") {
    val map = OrderedMap[String, Int]()
    map.put("x", 42)
    assert(map.containsKey("x"))
    assert(map.containsValue(42))
    assert(!map.containsKey("y"))
    assert(!map.containsValue(99))
  }

  test("clear") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.clear()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("insertion order preserved") {
    val map = OrderedMap[String, Int]()
    map.put("c", 3)
    map.put("a", 1)
    map.put("b", 2)
    val keys = map.orderedKeys
    assertEquals(keys(0), "c")
    assertEquals(keys(1), "a")
    assertEquals(keys(2), "b")
  }

  test("insertion order preserved after remove") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    map.remove("b")
    val keys = map.orderedKeys
    assertEquals(keys.size, 2)
    assertEquals(keys(0), "a")
    assertEquals(keys(1), "c")
  }

  test("removeIndex") {
    val map = OrderedMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    map.removeIndex(1) // remove "b"
    assertEquals(map.size, 2)
    assert(!map.containsKey("b"))
  }

  test("get with default") {
    val map = OrderedMap[String, Int]()
    assertEquals(map.get("missing", 99), 99)
  }

  test("putAll from another OrderedMap") {
    val map1 = OrderedMap[String, Int]()
    map1.put("a", 1)
    map1.put("b", 2)
    val map2 = OrderedMap[String, Int]()
    map2.put("c", 3)
    map2.putAll(map1)
    assertEquals(map2.size, 3)
    assert(map2.containsKey("a"))
    assert(map2.containsKey("b"))
    assert(map2.containsKey("c"))
  }

  test("many elements maintain order") {
    val map = OrderedMap[java.lang.Integer, String]()
    for (i <- 0 until 100)
      map.put(java.lang.Integer.valueOf(i), s"val$i")
    val keys = map.orderedKeys
    for (i <- 0 until 100)
      assertEquals(keys(i), java.lang.Integer.valueOf(i))
  }
}
