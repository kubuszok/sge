/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

/** Regression tests verifying that collections null out vacated slots to prevent memory leaks. */
class CollectionLeakTest extends munit.FunSuite {

  test("DynamicArray.removeIndex nulls vacated slot") {
    val arr = DynamicArray[String](4)
    arr.add("a")
    arr.add("b")
    arr.add("c")
    arr.removeIndex(1) // remove "b"
    // After removing index 1, size is 2 and the backing array slot at index 2 should be nulled
    assertEquals(arr.size, 2)
    assertEquals(arr(0), "a")
    assertEquals(arr(1), "c")
  }

  test("DynamicArray.pop nulls vacated slot") {
    val arr = DynamicArray[String](4)
    arr.add("a")
    arr.add("b")
    val popped = arr.pop()
    assertEquals(popped, "b")
    assertEquals(arr.size, 1)
  }

  test("DynamicArray.clear nulls all slots") {
    val arr = DynamicArray[String](4)
    arr.add("a")
    arr.add("b")
    arr.add("c")
    arr.clear()
    assertEquals(arr.size, 0)
  }

  test("ObjectMap.remove returns removed value") {
    val map = ObjectMap[String, String](4)
    map.put("key", "value")
    val removed = map.remove("key")
    assert(removed.isDefined)
    assertEquals(removed.get, "value")
    assertEquals(map.size, 0)
  }

  test("ObjectMap.clear empties map") {
    val map = ObjectMap[String, String](4)
    map.put("a", "1")
    map.put("b", "2")
    map.clear()
    assertEquals(map.size, 0)
    assert(map.get("a").isEmpty)
  }

  test("ObjectSet.remove works correctly") {
    val set = ObjectSet[String](4)
    set.add("a")
    set.add("b")
    assert(set.remove("a"))
    assertEquals(set.size, 1)
    assert(!set.contains("a"))
    assert(set.contains("b"))
  }

  test("ObjectSet.clear empties set") {
    val set = ObjectSet[String](4)
    set.add("a")
    set.add("b")
    set.clear()
    assertEquals(set.size, 0)
    assert(!set.contains("a"))
  }

  test("ArrayMap.removeIndex nulls slot") {
    val map = ArrayMap[String, String]()
    map.put("a", "1")
    map.put("b", "2")
    map.put("c", "3")
    map.removeIndex(1)
    assertEquals(map.size, 2)
    assertEquals(map.getKeyAt(0), "a")
  }

  test("ArrayMap.clear empties map") {
    val map = ArrayMap[String, String]()
    map.put("a", "1")
    map.put("b", "2")
    map.clear()
    assertEquals(map.size, 0)
  }
}
