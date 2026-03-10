/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import scala.collection.mutable

/** Tests for stdlib replacements of LibGDX Queue (ArrayDeque) and Bits (BitSet).
  * Queue.java -> skipped (use stdlib ArrayDeque)
  * Bits.java  -> skipped (use stdlib BitSet) */
class QueueBitsTest extends munit.FunSuite {

  // ---------------------------------------------------------------------------
  // Queue (ArrayDeque) tests
  // ---------------------------------------------------------------------------

  test("Queue: addFirst and addLast maintain order") {
    val q = mutable.ArrayDeque[String]()
    q.addOne("B")
    q.prepend("A")
    q.addOne("C")
    assertEquals(q.toList, List("A", "B", "C"))
  }

  test("Queue: removeFirst and removeLast") {
    val q = mutable.ArrayDeque(1, 2, 3, 4)
    assertEquals(q.removeHead(), 1)
    assertEquals(q.removeLast(), 4)
    assertEquals(q.toList, List(2, 3))
  }

  test("Queue: size and isEmpty") {
    val q = mutable.ArrayDeque[Int]()
    assert(q.isEmpty)
    assertEquals(q.size, 0)
    q.addOne(42)
    assert(q.nonEmpty)
    assertEquals(q.size, 1)
  }

  test("Queue: clear removes all elements") {
    val q = mutable.ArrayDeque(1, 2, 3)
    q.clear()
    assert(q.isEmpty)
    assertEquals(q.size, 0)
  }

  test("Queue: head and last peek without removal") {
    val q = mutable.ArrayDeque("first", "middle", "last")
    assertEquals(q.head, "first")
    assertEquals(q.last, "last")
    assertEquals(q.size, 3)
  }

  test("Queue: iterator preserves insertion order") {
    val q = mutable.ArrayDeque[Int]()
    q.prepend(1)
    q.addOne(2)
    q.addOne(3)
    q.prepend(0)
    assertEquals(q.iterator.toList, List(0, 1, 2, 3))
  }

  // ---------------------------------------------------------------------------
  // Bits (BitSet) tests
  // ---------------------------------------------------------------------------

  test("Bits: set, get, and clear individual bits") {
    val bits = mutable.BitSet()
    bits += 5
    bits += 10
    assert(bits.contains(5))
    assert(bits.contains(10))
    assert(!bits.contains(0))
    bits -= 5
    assert(!bits.contains(5))
    assert(bits.contains(10))
  }

  test("Bits: and, or, xor operations") {
    val a = mutable.BitSet(1, 2, 3, 4)
    val b = mutable.BitSet(3, 4, 5, 6)
    assertEquals((a & b), mutable.BitSet(3, 4))
    assertEquals((a | b), mutable.BitSet(1, 2, 3, 4, 5, 6))
    assertEquals((a ^ b), mutable.BitSet(1, 2, 5, 6))
  }

  test("Bits: nextSetBit equivalent via iterator") {
    val bits = mutable.BitSet(3, 7, 15, 100)
    val fromSeven = bits.iteratorFrom(7).toList
    assertEquals(fromSeven, List(7, 15, 100))
  }

  test("Bits: nextClearBit equivalent") {
    val bits = mutable.BitSet(0, 1, 2, 4)
    // first clear bit starting from 0 is 3
    var i = 0
    while (bits.contains(i)) { i += 1 }
    assertEquals(i, 3)
  }

  test("Bits: isEmpty") {
    val bits = mutable.BitSet()
    assert(bits.isEmpty)
    bits += 42
    assert(bits.nonEmpty)
    bits -= 42
    assert(bits.isEmpty)
  }

  test("Bits: containsAll (subsetOf)") {
    val superset = mutable.BitSet(1, 2, 3, 4, 5)
    val subset = mutable.BitSet(2, 4)
    val notSubset = mutable.BitSet(2, 6)
    assert(subset.subsetOf(superset))
    assert(!notSubset.subsetOf(superset))
  }
}
