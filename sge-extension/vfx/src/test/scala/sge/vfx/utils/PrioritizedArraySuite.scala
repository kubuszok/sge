package sge
package vfx
package utils

class PrioritizedArraySuite extends munit.FunSuite {

  test("add items and iterate in priority order") {
    val arr = new PrioritizedArray[String]()
    arr.add("low", 10)
    arr.add("high", 1)
    arr.add("mid", 5)
    assertEquals(arr.toList, List("high", "mid", "low"))
  }

  test("add with default priority 0") {
    val arr = new PrioritizedArray[String]()
    arr.add("first")
    arr.add("second")
    assertEquals(arr.size, 2)
    assertEquals(arr.toList, List("first", "second"))
  }

  test("remove by item identity") {
    val arr  = new PrioritizedArray[String]()
    val item = "hello"
    arr.add(item, 1)
    arr.add("world", 2)
    assertEquals(arr.size, 2)
    arr.remove(item)
    assertEquals(arr.size, 1)
    assertEquals(arr.toList, List("world"))
  }

  test("remove by index") {
    val arr = new PrioritizedArray[String]()
    arr.add("a", 1)
    arr.add("b", 2)
    arr.add("c", 3)
    arr.remove(1) // removes "b" (index 1 in priority-sorted order)
    assertEquals(arr.size, 2)
  }

  test("contains checks identity") {
    val arr  = new PrioritizedArray[String]()
    val item = "hello"
    arr.add(item)
    assert(arr.contains(item))
    // Note: contains uses identity (eq), not equality
  }

  test("clear removes all items") {
    val arr = new PrioritizedArray[String]()
    arr.add("a")
    arr.add("b")
    arr.clear()
    assertEquals(arr.size, 0)
    assertEquals(arr.toList, Nil)
  }

  test("get retrieves item at index") {
    val arr = new PrioritizedArray[String]()
    arr.add("low", 10)
    arr.add("high", 1)
    assertEquals(arr.get(0), "high")
    assertEquals(arr.get(1), "low")
  }

  test("setPriority re-sorts items") {
    val arr  = new PrioritizedArray[String]()
    val item = "movable"
    arr.add(item, 10)
    arr.add("fixed", 5)
    // initially: fixed(5), movable(10)
    assertEquals(arr.get(0), "fixed")
    arr.setPriority(item, 1)
    // now: movable(1), fixed(5)
    assertEquals(arr.get(0), "movable")
    assertEquals(arr.get(1), "fixed")
  }

  test("size is accurate") {
    val arr = new PrioritizedArray[Int]()
    assertEquals(arr.size, 0)
    arr.add(1)
    assertEquals(arr.size, 1)
    arr.add(2)
    assertEquals(arr.size, 2)
    arr.remove(1: Int)
    assertEquals(arr.size, 1)
  }

  test("stable sort preserves insertion order for equal priorities") {
    val arr = new PrioritizedArray[String]()
    arr.add("first", 0)
    arr.add("second", 0)
    arr.add("third", 0)
    assertEquals(arr.toList, List("first", "second", "third"))
  }
}
