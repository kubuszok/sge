package sge
package ecs
package utils

class BagSuite extends munit.FunSuite {

  test("add and get") {
    val bag = new Bag[String]()
    bag.add("a")
    bag.add("b")
    bag.add("c")
    assertEquals(bag.size, 3)
    assertEquals(bag.get(0), "a")
    assertEquals(bag.get(1), "b")
    assertEquals(bag.get(2), "c")
  }

  test("set specific index") {
    val bag = new Bag[String]()
    bag.add("a")
    bag.add("b")
    bag.add("c")
    assertEquals(bag.size, 3)

    bag.set(1, "d")
    assertEquals(bag.size, 3)
    assertEquals(bag.get(1), "d")
  }

  test("set beyond capacity auto-grows") {
    val bag = new Bag[String](4)
    bag.set(10, "x")
    assertEquals(bag.get(10), "x")
    assert(bag.size >= 11)
    assert(bag.getCapacity > 10)
  }

  test("remove by index swaps with last") {
    val bag = new Bag[String]()
    bag.add("a")
    bag.add("b")
    bag.add("c")

    val removed = bag.remove(0)
    assertEquals(removed, "a")
    assertEquals(bag.size, 2)
    // Last element "c" was swapped into index 0
    assertEquals(bag.get(0), "c")
    assertEquals(bag.get(1), "b")
  }

  test("remove by reference") {
    val s1  = new String("hello")
    val s2  = new String("world")
    val bag = new Bag[String]()
    bag.add(s1)
    bag.add(s2)

    assert(bag.remove(s1))
    assertEquals(bag.size, 1)
    assert(!bag.remove(s1))
  }

  test("removeLast") {
    val bag = new Bag[String]()
    bag.add("a")
    bag.add("b")

    assertEquals(bag.removeLast(), "b")
    assertEquals(bag.size, 1)
    assertEquals(bag.removeLast(), "a")
    assertEquals(bag.size, 0)
    assertEquals(bag.removeLast(), null)
  }

  test("contains uses reference equality") {
    class Obj(val name: String)
    val s1  = new Obj("hello")
    val s2  = new Obj("hello") // same content, different reference
    val bag = new Bag[Obj]()
    bag.add(s1)

    assert(bag.contains(s1))
    assert(!bag.contains(s2))
  }

  test("clear resets size") {
    val bag = new Bag[String]()
    bag.add("a")
    bag.add("b")
    bag.add("c")
    assertEquals(bag.size, 3)

    bag.clear()
    assertEquals(bag.size, 0)
  }

  test("get out of bounds returns null") {
    val bag = new Bag[String](4)
    assertEquals(bag.get(0), null)
    assertEquals(bag.get(100), null) // beyond capacity
  }

  test("isEmpty") {
    val bag = new Bag[String]()
    assert(bag.isEmpty)
    bag.add("a")
    assert(!bag.isEmpty)
  }

  test("isIndexWithinBounds") {
    val bag = new Bag[String](8)
    assert(bag.isIndexWithinBounds(0))
    assert(bag.isIndexWithinBounds(7))
    assert(!bag.isIndexWithinBounds(8))
  }

  test("auto-grow on add") {
    val bag = new Bag[String](2)
    assertEquals(bag.getCapacity, 2)
    bag.add("a")
    bag.add("b")
    bag.add("c") // should trigger grow
    assertEquals(bag.size, 3)
    assertEquals(bag.get(2), "c")
    assert(bag.getCapacity > 2)
  }
}
