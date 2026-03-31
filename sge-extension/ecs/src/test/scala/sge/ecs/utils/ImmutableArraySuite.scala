package sge
package ecs
package utils

import scala.collection.mutable.ArrayBuffer

class ImmutableArraySuite extends munit.FunSuite {

  test("size reflects backing array") {
    val backing   = ArrayBuffer[Int]()
    val immutable = new ImmutableArray[Int](backing)

    assertEquals(immutable.size, 0)

    for (i <- 0 until 10)
      backing += i

    assertEquals(immutable.size, 10)
  }

  test("get by index matches backing") {
    val backing   = ArrayBuffer[Int]()
    val immutable = new ImmutableArray[Int](backing)

    for (i <- 0 until 10)
      backing += i

    for (i <- 0 until 10)
      assertEquals(immutable(i), backing(i))
  }

  test("contains") {
    val backing   = ArrayBuffer("a", "b", "c")
    val immutable = new ImmutableArray[String](backing)

    assert(immutable.contains("a"))
    assert(immutable.contains("c"))
    assert(!immutable.contains("d"))
  }

  test("iterator matches backing order") {
    val backing   = ArrayBuffer[Int]()
    val immutable = new ImmutableArray[Int](backing)

    for (i <- 0 until 10)
      backing += i

    var expected = 0
    for (value <- immutable) {
      assertEquals(value, expected)
      expected += 1
    }
    assertEquals(expected, 10)
  }

  test("live view reflects changes to backing") {
    val backing   = ArrayBuffer[Int]()
    val immutable = new ImmutableArray[Int](backing)

    backing += 1
    backing += 2
    assertEquals(immutable.size, 2)

    backing += 3
    assertEquals(immutable.size, 3)
    assertEquals(immutable(2), 3)

    backing.remove(0)
    assertEquals(immutable.size, 2)
    assertEquals(immutable(0), 2)
  }

  test("indexOf and lastIndexOf") {
    val backing   = ArrayBuffer("a", "b", "a", "c")
    val immutable = new ImmutableArray[String](backing)

    assertEquals(immutable.indexOf("a"), 0)
    assertEquals(immutable.lastIndexOf("a"), 2)
    assertEquals(immutable.indexOf("c"), 3)
    assertEquals(immutable.indexOf("z"), -1)
  }

  test("first and peek") {
    val backing   = ArrayBuffer("x", "y", "z")
    val immutable = new ImmutableArray[String](backing)

    assertEquals(immutable.first, "x")
    assertEquals(immutable.peek, "z")
  }

  test("empty array default constructor") {
    val immutable = new ImmutableArray[String]()
    assertEquals(immutable.size, 0)
  }
}
