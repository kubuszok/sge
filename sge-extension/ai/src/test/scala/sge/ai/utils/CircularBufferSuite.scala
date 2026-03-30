package sge
package ai
package utils

class CircularBufferSuite extends munit.FunSuite {

  test("store and read basic") {
    val buf = new CircularBuffer[String](4)
    buf.store("a")
    buf.store("b")
    assertEquals(buf.size, 2)
    assertEquals(buf.read().get, "a")
    assertEquals(buf.read().get, "b")
    assert(buf.isEmpty, "buffer should be empty after reading all")
  }

  test("FIFO order") {
    val buf = new CircularBuffer[String](8)
    for (i <- 1 to 5) buf.store(s"item$i")
    for (i <- 1 to 5) assertEquals(buf.read().get, s"item$i")
  }

  test("wrap-around: store more than initial capacity with resize") {
    val buf = new CircularBuffer[String](2, resizable = true)
    assert(buf.store("a"), "store a")
    assert(buf.store("b"), "store b")
    assert(buf.store("c"), "store c triggers resize")
    assert(buf.store("d"), "store d")
    assertEquals(buf.size, 4)
    assertEquals(buf.read().get, "a")
    assertEquals(buf.read().get, "b")
    assertEquals(buf.read().get, "c")
    assertEquals(buf.read().get, "d")
  }

  test("fixed-size buffer returns false when full") {
    val buf = new CircularBuffer[String](2, resizable = false)
    assert(buf.store("a"), "store a")
    assert(buf.store("b"), "store b")
    assert(!buf.store("c"), "store c should fail when full")
    assertEquals(buf.size, 2)
    assert(buf.isFull, "buffer should be full")
  }

  test("clear resets") {
    val buf = new CircularBuffer[String](4)
    buf.store("a")
    buf.store("b")
    buf.clear()
    assert(buf.isEmpty, "buffer should be empty after clear")
    assertEquals(buf.size, 0)
    assert(buf.read().isEmpty, "read from cleared buffer should be empty")
  }

  test("read from empty returns empty Nullable") {
    val buf = new CircularBuffer[String](4)
    assert(buf.read().isEmpty, "read from empty buffer should be empty")
  }

  test("wrap-around internal state: read some, store more, read all") {
    val buf = new CircularBuffer[String](4, resizable = false)
    buf.store("a")
    buf.store("b")
    buf.store("c")
    buf.store("d")
    // Read 2 to advance head
    assertEquals(buf.read().get, "a")
    assertEquals(buf.read().get, "b")
    // Now store 2 more (wrapping the tail around)
    assert(buf.store("e"), "store e after wrapping")
    assert(buf.store("f"), "store f after wrapping")
    assertEquals(buf.size, 4)
    assertEquals(buf.read().get, "c")
    assertEquals(buf.read().get, "d")
    assertEquals(buf.read().get, "e")
    assertEquals(buf.read().get, "f")
    assert(buf.isEmpty, "buffer should be empty after reading all")
  }
}
