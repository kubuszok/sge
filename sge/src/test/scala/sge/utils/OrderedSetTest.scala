/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class OrderedSetTest extends munit.FunSuite {

  test("empty set") {
    val set = OrderedSet[String]()
    assertEquals(set.size, 0)
    assert(set.isEmpty)
  }

  test("add and contains") {
    val set = OrderedSet[String]()
    assert(set.add("hello"))
    assert(set.contains("hello"))
    assertEquals(set.size, 1)
  }

  test("add duplicate returns false") {
    val set = OrderedSet[String]()
    assert(set.add("hello"))
    assert(!set.add("hello"))
    assertEquals(set.size, 1)
  }

  test("remove") {
    val set = OrderedSet[String]()
    set.add("a")
    set.add("b")
    assert(set.remove("a"))
    assert(!set.contains("a"))
    assert(set.contains("b"))
    assertEquals(set.size, 1)
  }

  test("clear") {
    val set = OrderedSet[String]()
    set.add("a")
    set.add("b")
    set.clear()
    assertEquals(set.size, 0)
    assert(set.isEmpty)
  }

  test("first") {
    val set = OrderedSet[String]()
    set.add("only")
    assertEquals(set.first, "only")
  }

  test("insertion order preserved") {
    val set = OrderedSet[String]()
    set.add("c")
    set.add("a")
    set.add("b")
    val items = set.orderedItems
    assertEquals(items(0), "c")
    assertEquals(items(1), "a")
    assertEquals(items(2), "b")
  }

  test("insertion order preserved after remove") {
    val set = OrderedSet[String]()
    set.add("a")
    set.add("b")
    set.add("c")
    set.remove("b")
    val items = set.orderedItems
    assertEquals(items.size, 2)
    assertEquals(items(0), "a")
    assertEquals(items(1), "c")
  }

  test("foreach visits in order") {
    val set = OrderedSet[String]()
    set.add("x")
    set.add("y")
    set.add("z")
    val visited = scala.collection.mutable.ArrayBuffer[String]()
    set.foreach(visited += _)
    assertEquals(visited.toList, List("x", "y", "z"))
  }

  test("many elements maintain order") {
    val set = OrderedSet[java.lang.Integer]()
    for (i <- 0 until 100) set.add(java.lang.Integer.valueOf(i))
    val items = set.orderedItems
    for (i <- 0 until 100)
      assertEquals(items(i), java.lang.Integer.valueOf(i))
  }

  test("add all from another set") {
    val set1 = OrderedSet[String]()
    set1.add("a")
    set1.add("b")
    val set2 = OrderedSet[String]()
    set2.add("c")
    set2.addAll(set1)
    assertEquals(set2.size, 3)
    assert(set2.contains("a"))
    assert(set2.contains("b"))
    assert(set2.contains("c"))
  }
}
