/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class ObjectSetTest extends munit.ScalaCheckSuite {

  test("empty set has size 0") {
    val set = ObjectSet[String]()
    assertEquals(set.size, 0)
    assert(set.isEmpty)
  }

  test("add and contains") {
    val set = ObjectSet[String]()
    assert(set.add("hello"))
    assert(set.contains("hello"))
    assertEquals(set.size, 1)
  }

  test("add duplicate returns false") {
    val set = ObjectSet[String]()
    assert(set.add("hello"))
    assert(!set.add("hello"))
    assertEquals(set.size, 1)
  }

  test("remove") {
    val set = ObjectSet[String]()
    set.add("a")
    set.add("b")
    assert(set.remove("a"))
    assert(!set.contains("a"))
    assert(set.contains("b"))
    assertEquals(set.size, 1)
  }

  test("remove non-existent returns false") {
    val set = ObjectSet[String]()
    assert(!set.remove("nothing"))
  }

  test("clear") {
    val set = ObjectSet[String]()
    set.add("a")
    set.add("b")
    set.add("c")
    set.clear()
    assertEquals(set.size, 0)
    assert(set.isEmpty)
    assert(!set.contains("a"))
  }

  test("first returns an element") {
    val set = ObjectSet[String]()
    set.add("only")
    assertEquals(set.first, "only")
  }

  test("first on empty set throws") {
    val set = ObjectSet[String]()
    intercept[IllegalStateException] {
      set.first
    }
  }

  test("add many elements and verify all present") {
    val set      = ObjectSet[String]()
    val elements = (0 until 100).map(i => s"item$i")
    elements.foreach(set.add)
    assertEquals(set.size, 100)
    elements.foreach { e =>
      assert(set.contains(e), s"missing $e")
    }
  }

  test("remove all elements leaves empty set") {
    val set      = ObjectSet[String]()
    val elements = (0 until 50).map(i => s"item$i")
    elements.foreach(set.add)
    elements.foreach(set.remove)
    assertEquals(set.size, 0)
    assert(set.isEmpty)
  }

  test("foreach visits all elements") {
    val set = ObjectSet[String]()
    set.add("a")
    set.add("b")
    set.add("c")
    var count   = 0
    val visited = scala.collection.mutable.Set[String]()
    set.foreach { e =>
      visited += e
      count += 1
    }
    assertEquals(count, 3)
    assert(visited.contains("a"))
    assert(visited.contains("b"))
    assert(visited.contains("c"))
  }

  test("ensureCapacity does not lose elements") {
    val set = ObjectSet[String]()
    set.add("x")
    set.add("y")
    set.ensureCapacity(1000)
    assert(set.contains("x"))
    assert(set.contains("y"))
    assertEquals(set.size, 2)
  }

  test("shrink does not lose elements") {
    val set = ObjectSet[String](200)
    set.add("x")
    set.add("y")
    set.shrink(4)
    assert(set.contains("x"))
    assert(set.contains("y"))
    assertEquals(set.size, 2)
  }

  test("addAll from another ObjectSet") {
    val set1 = ObjectSet[String]()
    set1.add("a")
    set1.add("b")
    val set2 = ObjectSet[String]()
    set2.add("c")
    set2.addAll(set1)
    assertEquals(set2.size, 3)
    assert(set2.contains("a"))
    assert(set2.contains("b"))
    assert(set2.contains("c"))
  }

  test("addAll from DynamicArray") {
    val set = ObjectSet[String]()
    val arr = DynamicArray[String]()
    arr += "x"
    arr += "y"
    set.addAll(arr)
    assertEquals(set.size, 2)
    assert(set.contains("x"))
    assert(set.contains("y"))
  }

  property("add N items then contains all N") {
    forAll(Gen.choose(1, 100)) { (n: Int) =>
      val set   = ObjectSet[java.lang.Integer]()
      val items = (0 until n).map(java.lang.Integer.valueOf)
      items.foreach(set.add)
      set.size == n && items.forall(set.contains)
    }
  }

  property("add then remove all leaves size 0") {
    forAll(Gen.choose(1, 50)) { (n: Int) =>
      val set   = ObjectSet[java.lang.Integer]()
      val items = (0 until n).map(java.lang.Integer.valueOf)
      items.foreach(set.add)
      items.foreach(set.remove)
      set.size == 0 && set.isEmpty
    }
  }

  property("foreach visits each element exactly once") {
    forAll(Gen.choose(1, 50)) { (n: Int) =>
      val set   = ObjectSet[java.lang.Integer]()
      val items = (0 until n).map(java.lang.Integer.valueOf)
      items.foreach(set.add)
      var count = 0
      set.foreach(_ => count += 1)
      count == n
    }
  }
}
