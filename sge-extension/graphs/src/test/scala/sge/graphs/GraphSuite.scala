/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Original test: space/earlygrey/simplegraphs/GraphTest.java
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import munit.FunSuite

/** Test helper: a simple 2D vector with value-based equality. */
final case class Vector2(x: Float, y: Float) {

  def dst(v: Vector2): Float = {
    val xd = v.x - x
    val yd = v.y - y
    Math.sqrt((xd * xd + yd * yd).toDouble).toFloat
  }

  override def toString: String = s"($x, $y)"
}

object GraphTestUtils {

  /** Creates an n x n grid graph. For directed graphs, edges are added in both directions. */
  def makeGridGraph[V >: Vector2](graph: Graph[V], n: Int): Graph[V] = {
    for {
      i <- 0 until n
      j <- 0 until n
    } graph.addVertex(Vector2(i.toFloat, j.toFloat))

    for {
      i <- 0 until n
      j <- 0 until n
    } {
      if (i < n - 1) {
        val v1 = Vector2(i.toFloat, j.toFloat)
        val v2 = Vector2((i + 1).toFloat, j.toFloat)
        val d  = v1.dst(v2)
        graph.addEdge(v1, v2, d)
        if (graph.isDirected) graph.addEdge(v2, v1, d)
      }
      if (j < n - 1) {
        val v1 = Vector2(i.toFloat, j.toFloat)
        val v2 = Vector2(i.toFloat, (j + 1).toFloat)
        val d  = v1.dst(v2)
        graph.addEdge(v1, v2, d)
        if (graph.isDirected) graph.addEdge(v2, v1, d)
      }
    }

    graph
  }
}

class GraphSuite extends FunSuite {

  test("vertices can be sorted") {
    val graph = UndirectedGraph[Int]()
    val list  = List(9, 4, 3, 2, 5, 7, 6, 0, 8, 1)
    list.foreach(graph.addVertex)
    graph.sortVertices(Ordering.Int)
    var i = 0
    for (vertex <- graph.vertices) {
      assertEquals(vertex, i)
      i += 1
    }
  }

  test("edges can be sorted") {
    val graph = DirectedGraph[Int]()
    val list  = List(9, 4, 3, 2, 5, 7, 6, 0, 8, 1)
    for (j <- list.indices) graph.addVertex(j)
    for (j <- list.indices) graph.addEdge(list(j), list(list.size - j - 1))
    graph.sortEdges(Ordering.by(_.a))
    var i = 0
    for (edge <- graph.edges) {
      assertEquals(edge.a, i)
      i += 1
    }
  }

  test("removeVertexIf removes matching vertices") {
    val n     = 16
    val graph = UndirectedGraph[Int]((0 until n).toList)
    graph.removeVertexIf(i => i % 2 == 0)

    for (i <- 0 until n by 2)
      assert(!graph.contains(i), s"Vertex $i should have been removed")
    for (i <- 1 until n by 2)
      assert(graph.contains(i), s"Vertex $i should still be present")
  }

  test("removeVertexIf with BadHashInteger") {
    val n        = 16
    val badGraph = UndirectedGraph[BadHashInteger]((0 until n).map(BadHashInteger(_)).toList)
    badGraph.removeVertexIf(i => i.i % 2 == 0)

    for (i <- 0 until n by 2)
      assert(!badGraph.contains(BadHashInteger(i)), s"Vertex $i should have been removed")
    for (i <- 1 until n by 2)
      assert(badGraph.contains(BadHashInteger(i)), s"Vertex $i should still be present")
  }

  test("removeEdgeIf on undirected graph") {
    val n                  = 5
    val undirectedGraph    = GraphTestUtils.makeGridGraph(UndirectedGraph[Vector2](), n)
    val expectedUndirected = 2 * n * (n - 1)
    assertEquals(undirectedGraph.edgeCount, expectedUndirected)

    val v1 = Vector2(1f, 1f)
    val v2 = Vector2(2f, 1f)
    undirectedGraph.removeEdgeIf(e => e.hasEndpoint(v1) || e.hasEndpoint(v2))

    assertEquals(undirectedGraph.edgeCount, expectedUndirected - 7)

    for (e <- undirectedGraph.edges)
      assert(!(e.hasEndpoint(v1) || e.hasEndpoint(v2)), s"Edge $e should have been removed")
  }

  test("removeEdgeIf on directed graph") {
    val n                = 5
    val diGraph          = GraphTestUtils.makeGridGraph(DirectedGraph[Vector2](), n)
    val expectedDirected = 2 * 2 * n * (n - 1)
    assertEquals(diGraph.edgeCount, expectedDirected)

    val v1 = Vector2(1f, 1f)
    val v2 = Vector2(2f, 1f)
    diGraph.removeEdgeIf(e => e.a.equals(v1) || e.a.equals(v2))

    assertEquals(diGraph.edgeCount, expectedDirected - 8)

    for (e <- diGraph.edges)
      assert(!(e.a.equals(v1) || e.a.equals(v2)), s"Edge $e should have been removed via removeEdgeIf")
  }
}
