/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import munit.FunSuite

class UndirectedGraphSuite extends FunSuite {

  test("add and remove vertices") {
    val graph = UndirectedGraph[String]()
    assert(graph.addVertex("A"))
    assert(graph.addVertex("B"))
    assertEquals(graph.size, 2)
    assert(graph.removeVertex("A"))
    assertEquals(graph.size, 1)
  }

  test("undirected edges are bidirectional") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B")
    graph.addEdge("A", "B", 5.0f)
    assertEquals(graph.edgeCount, 1) // only one edge in the map
    assert(graph.edgeExists("A", "B"))
    assert(graph.edgeExists("B", "A")) // undirected: both directions
  }

  test("no duplicate edges") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B")
    graph.addEdge("A", "B")
    graph.addEdge("B", "A") // same edge, should update weight not add new
    assertEquals(graph.edgeCount, 1)
  }

  test("remove undirected edge") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    assertEquals(graph.edgeCount, 2)
    assert(graph.removeEdge("B", "A")) // remove via reverse direction
    assertEquals(graph.edgeCount, 1)
    assert(!graph.edgeExists("A", "B"))
    assert(!graph.edgeExists("B", "A"))
  }

  test("minimum spanning tree") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C", "D")
    graph.addEdge("A", "B", 1.0f)
    graph.addEdge("B", "C", 2.0f)
    graph.addEdge("C", "D", 3.0f)
    graph.addEdge("A", "D", 10.0f)
    graph.addEdge("A", "C", 5.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    // MST should have n-1 = 3 edges
    assertEquals(mst.edgeCount, 3)
    // Total weight should be 1 + 2 + 3 = 6 (not 10 or 5)
    val totalWeight = mst.edges.map(_.weight).sum
    assertEquals(totalWeight, 6.0f)
  }

  test("shortest path in undirected graph") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B", 1.0f)
    graph.addEdge("B", "C", 2.0f)
    graph.addEdge("A", "C", 10.0f)

    val path = graph.algorithms.findShortestPath("A", "C")
    assertEquals(path.size, 3)
    assertEquals(path.first, "A")
    assertEquals(path.last, "C")
    assertEquals(path.length, 3.0f) // A->B(1) + B->C(2) = 3
  }

  test("cycle detection in undirected graph") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    graph.addEdge("C", "A")
    assert(graph.algorithms.containsCycle())
  }

  test("no cycle in tree") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    assert(!graph.algorithms.containsCycle())
  }

  test("isDirected returns false") {
    val graph = UndirectedGraph[String]()
    assert(!graph.isDirected)
  }

  test("graph toString") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B")
    graph.addEdge("A", "B")
    assertEquals(graph.toString, "Undirected graph with 2 vertices and 1 edges")
  }

  test("degree") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("A", "C")
    assertEquals(graph.getDegree("A"), 2)
    assertEquals(graph.getDegree("B"), 1)
  }

  test("complete graph builder for undirected") {
    val graph = UndirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    GraphBuilder.buildCompleteGraph(graph)
    // Undirected complete graph with 3 vertices has 3 edges
    assertEquals(graph.edgeCount, 3)
  }
}
