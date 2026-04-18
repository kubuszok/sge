/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Original test: space/earlygrey/simplegraphs/StructuresTest.java
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import munit.FunSuite

/** A type whose hashCode always returns 1, forcing hash collisions in NodeMap. Ported from simple-graphs BadHashInteger.
  */
final case class BadHashInteger(i: Int) {

  override def hashCode(): Int = 1

  override def toString: String = i.toString
}

class StructuresSuite extends FunSuite {

  test("nodeMap should work") {
    val graph   = UndirectedGraph[Int]()
    val nodeMap = graph.nodeMap
    val n       = 16

    // NodeMap uses MinTableLength=32, ResizeThreshold=0.7f so threshold = (0.7*32).toInt = 22
    val threshold = 22

    for (i <- 0 until threshold)
      assert(nodeMap.put(i) != null, s"Put did not return a node for $i")

    // Adding one more element past the threshold should still succeed and trigger resize
    val minTableLength = 32
    assert(nodeMap.put(minTableLength) != null, "Put did not return a node")
    assert(nodeMap.contains(minTableLength), "Object not contained in map")
    assertEquals(nodeMap.size, threshold + 1, "Map is not correct size")

    val removed = nodeMap.remove(2)
    assert(removed != null, "Removal did not return node")

    // test via graph object with bad hash collisions
    val badGraph = UndirectedGraph[BadHashInteger]()
    for (i <- 0 until n)
      assert(badGraph.nodeMap.put(BadHashInteger(i)) != null, "Put did not return a node")

    assert(badGraph.size == n, "Graph is not correct size")

    badGraph.removeVertex(BadHashInteger(2))

    assert(badGraph.size == n - 1)

    badGraph.nodeMap.clear()

    for (i <- 0 until n)
      assert(badGraph.nodeMap.put(BadHashInteger(i)) != null)
  }
}
