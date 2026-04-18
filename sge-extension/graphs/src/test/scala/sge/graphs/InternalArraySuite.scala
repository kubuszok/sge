/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Original test: space/earlygrey/simplegraphs/ArrayTest.java
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import munit.FunSuite

import sge.graphs.internal.InternalArray

class InternalArraySuite extends FunSuite {

  test("addAll should add all items from source to target and resize target") {
    val target = InternalArray[Int](initialCapacity = 0)
    val source = InternalArray[Int]()
    source.add(3)
    target.add(1)
    target.add(2)
    target.addAll(source)
    assertEquals(target.size, 3, "Target InternalArray has wrong size.")
    assertEquals(target.get(0), 1, "Item 0 of Target InternalArray was overwritten.")
    assertEquals(target.get(1), 2, "Item 1 of Target InternalArray was overwritten.")
    assertEquals(target.get(2), 3, "Item 0 of Source InternalArray was not copied.")
  }

  test("addAll should add all items from source to target and update target size") {
    val target = InternalArray[Int]()
    val source = InternalArray[Int]()
    target.add(0)
    source.add(1)
    target.addAll(source)
    assertEquals(target.size, 2, "Target InternalArray has wrong size.")
    assertEquals(target.get(0), 0, "Item of Target InternalArray was overwritten.")
    assertEquals(target.get(1), source.get(0), "Item of Source InternalArray was not copied.")
  }
}
