package sge
package graphs

import munit.FunSuite

import sge.graphs.internal.BinaryHeap

class BinaryHeapSuite extends FunSuite {

  private def makeNode(value: Float): Node[String] = {
    val node = new Node[String]("v", false, 0)
    node.heapValue = value
    node
  }

  test("add and peek") {
    val heap = BinaryHeap[String]()
    val n1   = makeNode(5.0f)
    val n2   = makeNode(3.0f)
    val n3   = makeNode(7.0f)

    heap.add(n1)
    heap.add(n2)
    heap.add(n3)

    assertEquals(heap.size, 3)
    assertEquals(heap.peek.heapValue, 3.0f)
  }

  test("pop removes smallest") {
    val heap = BinaryHeap[String]()
    heap.add(makeNode(5.0f))
    heap.add(makeNode(1.0f))
    heap.add(makeNode(3.0f))

    val first = heap.pop()
    assertEquals(first.heapValue, 1.0f)
    assertEquals(heap.size, 2)

    val second = heap.pop()
    assertEquals(second.heapValue, 3.0f)
    assertEquals(heap.size, 1)

    val third = heap.pop()
    assertEquals(third.heapValue, 5.0f)
    assertEquals(heap.size, 0)
  }

  test("isEmpty and notEmpty") {
    val heap = BinaryHeap[String]()
    assert(heap.isEmpty)
    assert(!heap.notEmpty)

    heap.add(makeNode(1.0f))
    assert(!heap.isEmpty)
    assert(heap.notEmpty)

    heap.pop()
    assert(heap.isEmpty)
    assert(!heap.notEmpty)
  }

  test("clear empties the heap") {
    val heap = BinaryHeap[String]()
    heap.add(makeNode(1.0f))
    heap.add(makeNode(2.0f))
    heap.add(makeNode(3.0f))

    assertEquals(heap.size, 3)
    heap.clear()
    assertEquals(heap.size, 0)
    assert(heap.isEmpty)
  }

  test("add with explicit value") {
    val heap = BinaryHeap[String]()
    val node = new Node[String]("v", false, 0)
    heap.add(node, 42.0f)
    assertEquals(node.heapValue, 42.0f)
    assertEquals(heap.peek.heapValue, 42.0f)
  }

  test("setValue moves node up") {
    val heap = BinaryHeap[String]()
    val n1   = makeNode(10.0f)
    val n2   = makeNode(20.0f)
    val n3   = makeNode(30.0f)

    heap.add(n1)
    heap.add(n2)
    heap.add(n3)

    assertEquals(heap.peek.heapValue, 10.0f)

    // Lower n3's value to make it the smallest
    heap.setValue(n3, 1.0f)
    assertEquals(heap.peek.heapValue, 1.0f)
  }

  test("setValue moves node down") {
    val heap = BinaryHeap[String]()
    val n1   = makeNode(1.0f)
    val n2   = makeNode(2.0f)
    val n3   = makeNode(3.0f)

    heap.add(n1)
    heap.add(n2)
    heap.add(n3)

    assertEquals(heap.peek.heapValue, 1.0f)

    // Raise n1's value above all others
    heap.setValue(n1, 100.0f)
    assertEquals(heap.peek.heapValue, 2.0f)
  }

  test("contains finds node by identity") {
    val heap = BinaryHeap[String]()
    val n1   = makeNode(1.0f)
    val n2   = makeNode(2.0f)
    val n3   = makeNode(3.0f)

    heap.add(n1)
    heap.add(n2)

    assert(heap.contains(n1))
    assert(heap.contains(n2))
    assert(!heap.contains(n3))
  }

  test("contains returns false for empty heap") {
    val heap = BinaryHeap[String]()
    val node = makeNode(1.0f)
    assert(!heap.contains(node))
  }

  test("contains after pop") {
    val heap = BinaryHeap[String]()
    val n1   = makeNode(1.0f)
    val n2   = makeNode(2.0f)

    heap.add(n1)
    heap.add(n2)

    assert(heap.contains(n1))
    heap.pop() // removes n1
    assert(!heap.contains(n1))
    assert(heap.contains(n2))
  }

  test("equals with same values") {
    val heap1 = BinaryHeap[String]()
    val heap2 = BinaryHeap[String]()

    heap1.add(makeNode(1.0f))
    heap1.add(makeNode(2.0f))

    heap2.add(makeNode(1.0f))
    heap2.add(makeNode(2.0f))

    assert(heap1.equals(heap2))
    assert(heap2.equals(heap1))
  }

  test("equals with different values") {
    val heap1 = BinaryHeap[String]()
    val heap2 = BinaryHeap[String]()

    heap1.add(makeNode(1.0f))
    heap1.add(makeNode(2.0f))

    heap2.add(makeNode(1.0f))
    heap2.add(makeNode(3.0f))

    assert(!heap1.equals(heap2))
  }

  test("equals with different sizes") {
    val heap1 = BinaryHeap[String]()
    val heap2 = BinaryHeap[String]()

    heap1.add(makeNode(1.0f))
    heap2.add(makeNode(1.0f))
    heap2.add(makeNode(2.0f))

    assert(!heap1.equals(heap2))
  }

  test("equals with non-BinaryHeap returns false") {
    val heap = BinaryHeap[String]()
    assert(!heap.equals("not a heap"))
    assert(!heap.equals(42))
    assert(!heap.equals(null))
  }

  test("equals empty heaps") {
    val heap1 = BinaryHeap[String]()
    val heap2 = BinaryHeap[String]()
    assert(heap1.equals(heap2))
  }

  test("hashCode consistent with equals") {
    val heap1 = BinaryHeap[String]()
    val heap2 = BinaryHeap[String]()

    heap1.add(makeNode(1.0f))
    heap1.add(makeNode(2.0f))

    heap2.add(makeNode(1.0f))
    heap2.add(makeNode(2.0f))

    assertEquals(heap1.hashCode(), heap2.hashCode())
  }

  test("hashCode changes when elements change") {
    val heap = BinaryHeap[String]()
    val h1   = heap.hashCode()
    heap.add(makeNode(5.0f))
    val h2 = heap.hashCode()
    assert(h1 != h2, "Hash should change after adding element")
  }

  test("toString empty heap") {
    val heap = BinaryHeap[String]()
    assertEquals(heap.toString(), "[]")
  }

  test("toString single element") {
    val heap = BinaryHeap[String]()
    heap.add(makeNode(3.0f))
    val s = heap.toString()
    assert(s == "[3.0]" || s == "[3]", s"Expected [3.0] or [3], got $s")
  }

  test("toString multiple elements") {
    val heap = BinaryHeap[String]()
    heap.add(makeNode(1.0f))
    heap.add(makeNode(2.0f))
    heap.add(makeNode(3.0f))
    // Heap invariant: smallest is first, but order of rest depends on insertion
    val str = heap.toString()
    assert(str.startsWith("[1.0") || str.startsWith("[1,") || str.startsWith("[1]"))
    assert(str.endsWith("]"))
  }

  test("heap grows beyond initial capacity") {
    val heap = BinaryHeap[String](2)
    for (i <- 0 until 20)
      heap.add(makeNode(i.toFloat))

    assertEquals(heap.size, 20)
    // Pop all and verify sorted order
    var prev = -1.0f
    while (heap.notEmpty) {
      val v = heap.pop().heapValue
      assert(v >= prev, s"Expected sorted order, got $v after $prev")
      prev = v
    }
  }

  test("pop and re-add maintains heap property") {
    val heap = BinaryHeap[String]()
    heap.add(makeNode(5.0f))
    heap.add(makeNode(3.0f))
    heap.add(makeNode(7.0f))

    val removed = heap.pop()
    assertEquals(removed.heapValue, 3.0f)

    heap.add(makeNode(1.0f))
    assertEquals(heap.peek.heapValue, 1.0f)
  }
}
