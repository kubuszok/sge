/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class DynamicArrayTest extends munit.FunSuite {

  // ---- Construction ----

  test("default construction creates empty array") {
    val arr = DynamicArray[Int]()
    assertEquals(arr.size, 0)
    assert(arr.isEmpty)
  }

  test("construction with capacity") {
    val arr = DynamicArray[String](32)
    assertEquals(arr.size, 0)
    assert(arr.isEmpty)
  }

  test("from plain array") {
    val arr = DynamicArray.from(Array(1, 2, 3))
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 3)
  }

  test("wrap plain array (no copy)") {
    val raw = Array(10, 20, 30)
    val arr = DynamicArray.wrap(raw)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 10)
    // Modifying through the wrapper should be visible in the original
    arr(0) = 99
    assertEquals(raw(0), 99)
  }

  test("from DynamicArray creates copy") {
    val original = DynamicArray[String]()
    original.add("a")
    original.add("b")
    val copy = DynamicArray.from(original)
    assertEquals(copy.size, 2)
    assertEquals(copy(0), "a")
    // Modifying copy does not affect original
    copy(0) = "z"
    assertEquals(original(0), "a")
  }

  // ---- Adding ----

  test("add single element") {
    val arr = DynamicArray[Int]()
    arr.add(42)
    assertEquals(arr.size, 1)
    assertEquals(arr(0), 42)
  }

  test("add two elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2)
    assertEquals(arr.size, 2)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
  }

  test("add three elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 3)
  }

  test("+= operator alias") {
    val arr = DynamicArray[String]()
    arr += "hello"
    assertEquals(arr.size, 1)
    assertEquals(arr(0), "hello")
  }

  test("addAll from another DynamicArray") {
    val arr1 = DynamicArray[Int]()
    arr1.add(1, 2)
    val arr2 = DynamicArray[Int]()
    arr2.add(3, 4)
    arr1.addAll(arr2)
    assertEquals(arr1.size, 4)
    assertEquals(arr1(2), 3)
    assertEquals(arr1(3), 4)
  }

  test("addAll from iterable") {
    val arr = DynamicArray[Int]()
    arr.addAll(Seq(10, 20, 30))
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 10)
    assertEquals(arr(1), 20)
    assertEquals(arr(2), 30)
  }

  test("addAll from plain array with start and count") {
    val arr = DynamicArray[Int]()
    arr.addAll(Array(1, 2, 3, 4, 5), 1, 3)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 2)
    assertEquals(arr(1), 3)
    assertEquals(arr(2), 4)
  }

  test("insert at beginning (ordered)") {
    val arr = DynamicArray[Int]()
    arr.add(2, 3)
    arr.insert(0, 1)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 3)
  }

  test("insert at middle (ordered)") {
    val arr = DynamicArray[Int]()
    arr.add(1, 3)
    arr.insert(1, 2)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 3)
  }

  test("insert at end") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2)
    arr.insert(2, 3)
    assertEquals(arr.size, 3)
    assertEquals(arr(2), 3)
  }

  // ---- Removing ----

  test("removeIndex (ordered) shifts elements") {
    val arr = DynamicArray[String]()
    arr.add("a")
    arr.add("b")
    arr.add("c")
    val removed = arr.removeIndex(1)
    assertEquals(removed, "b")
    assertEquals(arr.size, 2)
    assertEquals(arr(0), "a")
    assertEquals(arr(1), "c")
  }

  test("removeIndex (unordered) swaps with last") {
    val arr = DynamicArray[String](false, 16)
    arr.add("a")
    arr.add("b")
    arr.add("c")
    val removed = arr.removeIndex(0)
    assertEquals(removed, "a")
    assertEquals(arr.size, 2)
    // "c" should have been swapped into position 0
    assertEquals(arr(0), "c")
    assertEquals(arr(1), "b")
  }

  test("removeValue returns true if found") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assert(arr.removeValue(2))
    assertEquals(arr.size, 2)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 3)
  }

  test("removeValue returns false if not found") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assert(!arr.removeValue(99))
    assertEquals(arr.size, 3)
  }

  test("-= operator alias") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr -= 2
    assertEquals(arr.size, 2)
  }

  test("removeAll removes matching elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(4, 5)
    val toRemove = DynamicArray[Int]()
    toRemove.add(2, 4)
    assert(arr.removeAll(toRemove))
    assertEquals(arr.size, 3)
    assert(!arr.contains(2))
    assert(!arr.contains(4))
  }

  test("--= operator alias") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val other = DynamicArray[Int]()
    other.add(2)
    arr --= other
    assertEquals(arr.size, 2)
  }

  test("removeRange removes slice") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(4, 5)
    arr.removeRange(1, 3)
    assertEquals(arr.size, 3)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 4)
    assertEquals(arr(2), 5)
  }

  test("pop removes and returns last element") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val popped = arr.pop()
    assertEquals(popped, 3)
    assertEquals(arr.size, 2)
  }

  test("pop on empty array throws") {
    val arr = DynamicArray[Int]()
    intercept[IndexOutOfBoundsException] {
      arr.pop()
    }
  }

  test("clear removes all elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.clear()
    assertEquals(arr.size, 0)
    assert(arr.isEmpty)
  }

  test("truncate reduces size") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(4, 5)
    arr.truncate(3)
    assertEquals(arr.size, 3)
    assertEquals(arr(2), 3)
  }

  test("truncate with larger value is no-op") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.truncate(10)
    assertEquals(arr.size, 3)
  }

  // ---- Access ----

  test("apply bounds check") {
    val arr = DynamicArray[Int]()
    arr.add(1)
    intercept[IndexOutOfBoundsException] {
      arr(1)
    }
  }

  test("update bounds check") {
    val arr = DynamicArray[Int]()
    arr.add(1)
    intercept[IndexOutOfBoundsException] {
      arr(1) = 99
    }
  }

  test("first returns first element") {
    val arr = DynamicArray[Int]()
    arr.add(10, 20, 30)
    assertEquals(arr.first, 10)
  }

  test("first on empty throws") {
    val arr = DynamicArray[Int]()
    intercept[IndexOutOfBoundsException] {
      arr.first
    }
  }

  test("last returns last element") {
    val arr = DynamicArray[Int]()
    arr.add(10, 20, 30)
    assertEquals(arr.last, 30)
  }

  test("last on empty throws") {
    val arr = DynamicArray[Int]()
    intercept[IndexOutOfBoundsException] {
      arr.last
    }
  }

  test("peek is alias for last") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assertEquals(arr.peek, arr.last)
  }

  test("random on empty returns Nullable.empty") {
    val arr = DynamicArray[Int]()
    assert(arr.random().isEmpty)
  }

  test("random on non-empty returns a valid element") {
    val arr = DynamicArray[Int]()
    arr.add(42)
    val r = arr.random()
    assert(r.isDefined)
    assertEquals(r.orNull, 42)
  }

  // ---- Search ----

  test("contains") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assert(arr.contains(2))
    assert(!arr.contains(99))
  }

  test("indexOf") {
    val arr = DynamicArray[String]()
    arr.add("a")
    arr.add("b")
    arr.add("c")
    assertEquals(arr.indexOf("b"), 1)
    assertEquals(arr.indexOf("z"), -1)
  }

  test("lastIndexOf") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(2)
    assertEquals(arr.lastIndexOf(2), 3)
    assertEquals(arr.lastIndexOf(99), -1)
  }

  test("containsAll") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val subset = DynamicArray[Int]()
    subset.add(1, 3)
    assert(arr.containsAll(subset))
    subset.add(99)
    assert(!arr.containsAll(subset))
  }

  test("containsAny") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val other = DynamicArray[Int]()
    other.add(99, 3)
    assert(arr.containsAny(other))
    val none = DynamicArray[Int]()
    none.add(88, 99)
    assert(!arr.containsAny(none))
  }

  // ---- Iteration ----

  test("foreach visits all elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    var sum = 0
    arr.foreach(sum += _)
    assertEquals(sum, 6)
  }

  test("iterator produces correct sequence") {
    val arr = DynamicArray[String]()
    arr.add("a")
    arr.add("b")
    arr.add("c")
    assertEquals(arr.iterator.toList, List("a", "b", "c"))
  }

  test("exists") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assert(arr.exists(_ > 2))
    assert(!arr.exists(_ > 10))
  }

  test("find") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val found = arr.find(_ == 2)
    assert(found.isDefined)
    assertEquals(found.orNull, 2)
    assert(arr.find(_ == 99).isEmpty)
  }

  test("count") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(4, 5)
    assertEquals(arr.count(_ > 2), 3)
    assertEquals(arr.count(_ > 100), 0)
  }

  test("forall") {
    val arr = DynamicArray[Int]()
    arr.add(2, 4, 6)
    assert(arr.forall(_ % 2 == 0))
    arr.add(3)
    assert(!arr.forall(_ % 2 == 0))
  }

  test("forall on empty returns true") {
    val arr = DynamicArray[Int]()
    assert(arr.forall(_ > 0))
  }

  test("indexWhere") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assertEquals(arr.indexWhere(_ > 1), 1)
    assertEquals(arr.indexWhere(_ > 100), -1)
  }

  // ---- Snapshot (copy-on-write) ----

  test("begin/end without mutation does not copy") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val snapshot = arr.begin()
    assertEquals(snapshot(0), 1)
    arr.end()
    // After end, array should be unchanged
    assertEquals(arr(0), 1)
  }

  test("mutation during snapshot triggers copy") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val snapshot = arr.begin()
    // Mutate during snapshot
    arr.add(4)
    // Snapshot should still see old size via the returned array
    assertEquals(snapshot.length >= 3, true)
    arr.end()
    // Array should have the new element
    assertEquals(arr.size, 4)
    assertEquals(arr(3), 4)
  }

  // ---- Transform ----

  test("sort with ordering") {
    val arr = DynamicArray[Int]()
    arr.add(3, 1, 2)
    arr.sort()(using Ordering.Int)
    assertEquals(arr(0), 1)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 3)
  }

  test("sort with explicit ordering") {
    val arr = DynamicArray[Int]()
    arr.add(3, 1, 2)
    arr.sort(Ordering.Int.reverse)
    assertEquals(arr(0), 3)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 1)
  }

  test("reverse") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.reverse()
    assertEquals(arr(0), 3)
    assertEquals(arr(1), 2)
    assertEquals(arr(2), 1)
  }

  test("swap") {
    val arr = DynamicArray[String]()
    arr.add("a")
    arr.add("b")
    arr.add("c")
    arr.swap(0, 2)
    assertEquals(arr(0), "c")
    assertEquals(arr(2), "a")
  }

  test("swap bounds check") {
    val arr = DynamicArray[Int]()
    arr.add(1)
    intercept[IndexOutOfBoundsException] {
      arr.swap(0, 1)
    }
  }

  test("shuffle does not lose elements") {
    val arr = DynamicArray[Int]()
    (0 until 100).foreach(arr.add)
    arr.shuffle()
    assertEquals(arr.size, 100)
    // All elements should still be present
    val sorted = (0 until arr.size).map(arr(_)).sorted
    assertEquals(sorted, (0 until 100).toSeq)
  }

  // ---- Size management ----

  test("ensureCapacity grows backing array") {
    val arr = DynamicArray[Int]()
    arr.ensureCapacity(1000)
    // Should not change size
    assertEquals(arr.size, 0)
    // But backing array should be large enough
    assert(arr.items.length >= 1000)
  }

  test("setSize grows or truncates") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.setSize(5)
    assertEquals(arr.size, 5)
    arr.setSize(1)
    assertEquals(arr.size, 1)
  }

  test("shrink trims backing array") {
    val arr = DynamicArray[Int]()
    arr.ensureCapacity(100)
    arr.add(1, 2, 3)
    arr.shrink()
    assertEquals(arr.items.length, 3)
    assertEquals(arr.size, 3)
  }

  test("isEmpty and nonEmpty") {
    val arr = DynamicArray[Int]()
    assert(arr.isEmpty)
    assert(!arr.nonEmpty)
    arr.add(1)
    assert(!arr.isEmpty)
    assert(arr.nonEmpty)
  }

  // ---- Conversion ----

  test("toArray creates a copy") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    val plain = arr.toArray
    assertEquals(plain.toSeq, Seq(1, 2, 3))
    // Modifying copy does not affect original
    plain(0) = 99
    assertEquals(arr(0), 1)
  }

  test("toString formats correctly") {
    val arr = DynamicArray[Int]()
    assertEquals(arr.toString, "[]")
    arr.add(1, 2, 3)
    assertEquals(arr.toString, "[1, 2, 3]")
  }

  test("toString with custom separator") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    assertEquals(arr.toString("; "), "[1; 2; 3]")
  }

  // ---- Edge cases ----

  test("single element array operations") {
    val arr = DynamicArray[Int]()
    arr.add(42)
    assertEquals(arr.first, 42)
    assertEquals(arr.last, 42)
    assertEquals(arr.peek, 42)
    assertEquals(arr.indexOf(42), 0)
    assertEquals(arr.lastIndexOf(42), 0)
    assert(arr.contains(42))
    arr.reverse()
    assertEquals(arr(0), 42)
  }

  test("large array stress test") {
    val arr = DynamicArray[Int]()
    val n   = 10000
    for (i <- 0 until n)
      arr.add(i)
    assertEquals(arr.size, n)
    assertEquals(arr.first, 0)
    assertEquals(arr.last, n - 1)

    // Remove from middle
    arr.removeIndex(n / 2)
    assertEquals(arr.size, n - 1)
  }

  // ---- Equality and hashCode ----

  test("equality for ordered arrays with same elements") {
    val a = DynamicArray[Int]()
    a.add(1, 2, 3)
    val b = DynamicArray[Int]()
    b.add(1, 2, 3)
    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }

  test("inequality for different elements") {
    val a = DynamicArray[Int]()
    a.add(1, 2, 3)
    val b = DynamicArray[Int]()
    b.add(1, 2, 4)
    assert(a != b)
  }

  test("inequality for different sizes") {
    val a = DynamicArray[Int]()
    a.add(1, 2)
    val b = DynamicArray[Int]()
    b.add(1, 2, 3)
    assert(a != b)
  }

  test("unordered arrays equality is reference equality") {
    val a = DynamicArray[Int](false, 16)
    a.add(1, 2, 3)
    val b = DynamicArray[Int](false, 16)
    b.add(1, 2, 3)
    // Unordered arrays use reference equality
    assert(a != b)
    assert(a == a)
  }

  test("replaceFirst replaces matching element") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(2)
    assert(arr.replaceFirst(2, 99))
    assertEquals(arr(1), 99)
    assertEquals(arr(3), 2) // only first replaced
  }

  test("replaceAll replaces all matching elements") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.add(2)
    assertEquals(arr.replaceAll(2, 99), 2)
    assertEquals(arr(1), 99)
    assertEquals(arr(3), 99)
  }

  test("insertRange inserts empty slots") {
    val arr = DynamicArray[Int]()
    arr.add(1, 2, 3)
    arr.insertRange(1, 2)
    assertEquals(arr.size, 5)
    assertEquals(arr(0), 1)
    assertEquals(arr(3), 2)
    assertEquals(arr(4), 3)
  }
}
