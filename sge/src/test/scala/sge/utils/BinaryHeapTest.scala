/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class BinaryHeapTest extends munit.FunSuite {

  test("add and peek") {
    val heap = BinaryHeap[BinaryHeap.Node]()
    heap.add(BinaryHeap.Node(3f))
    heap.add(BinaryHeap.Node(1f))
    heap.add(BinaryHeap.Node(2f))
    assertEqualsFloat(heap.peek().value, 1f, 0f)
  }

  test("pop returns min") {
    val heap = BinaryHeap[BinaryHeap.Node]()
    heap.add(BinaryHeap.Node(5f))
    heap.add(BinaryHeap.Node(2f))
    heap.add(BinaryHeap.Node(8f))
    assertEqualsFloat(heap.pop().value, 2f, 0f)
    assertEqualsFloat(heap.pop().value, 5f, 0f)
    assertEqualsFloat(heap.pop().value, 8f, 0f)
  }

  test("max heap") {
    val heap = BinaryHeap[BinaryHeap.Node](16, true)
    heap.add(BinaryHeap.Node(1f))
    heap.add(BinaryHeap.Node(5f))
    heap.add(BinaryHeap.Node(3f))
    assertEqualsFloat(heap.peek().value, 5f, 0f)
  }

  test("contains with identity uses reference equality") {
    // Regression: contains(_, true) used == instead of eq
    val n1   = BinaryHeap.Node(1f)
    val n2   = BinaryHeap.Node(1f)
    val heap = BinaryHeap[BinaryHeap.Node]()
    heap.add(n1)
    assert(heap.contains(n1, identity = true))
    assert(!heap.contains(n2, identity = true)) // same value but different instance
    // Node doesn't override equals, so identity=false also uses reference equality
    assert(!heap.contains(n2, identity = false))
    assert(heap.contains(n1, identity = false))
  }

  test("clear empties heap") {
    val heap = BinaryHeap[BinaryHeap.Node]()
    heap.add(BinaryHeap.Node(1f))
    heap.add(BinaryHeap.Node(2f))
    assertEquals(heap.size, 2)
    heap.clear()
    assertEquals(heap.size, 0)
    assert(heap.isEmpty)
  }
}
