/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs

import munit.FunSuite

import sge.graphs.utils.SearchProcessor
import sge.graphs.algorithms.SearchStep

class DirectedGraphSuite extends FunSuite {

  test("add and remove vertices") {
    val graph = DirectedGraph[String]()
    assert(graph.addVertex("A"))
    assert(graph.addVertex("B"))
    assert(!graph.addVertex("A")) // duplicate
    assertEquals(graph.size, 2)
    assert(graph.contains("A"))
    assert(graph.removeVertex("A"))
    assertEquals(graph.size, 1)
    assert(!graph.contains("A"))
  }

  test("add and remove edges") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B", 2.0f)
    graph.addEdge("B", "C", 3.0f)
    assertEquals(graph.edgeCount, 2)
    assert(graph.edgeExists("A", "B"))
    assert(!graph.edgeExists("B", "A")) // directed
    assert(graph.removeEdge("A", "B"))
    assertEquals(graph.edgeCount, 1)
    assert(!graph.edgeExists("A", "B"))
  }

  test("shortest path with Dijkstra") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C", "D")
    graph.addEdge("A", "B", 1.0f)
    graph.addEdge("B", "C", 2.0f)
    graph.addEdge("A", "C", 10.0f)
    graph.addEdge("C", "D", 1.0f)

    val path = graph.algorithms.findShortestPath("A", "D")
    assertEquals(path.size, 4)
    assertEquals(path.first, "A")
    assertEquals(path.last, "D")
    assertEquals(path.length, 4.0f) // A->B(1) + B->C(2) + C->D(1) = 4
  }

  test("shortest path - unreachable") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B")
    // no edge from A to B
    val path = graph.algorithms.findShortestPath("A", "B")
    assert(path.isEmpty)
  }

  test("BFS visits all reachable vertices") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    graph.addEdge(1, 3)
    graph.addEdge(2, 4)

    var visited = List.empty[Int]
    graph.algorithms.breadthFirstSearch(1, new SearchProcessor[Int] {
      def accept(step: SearchStep[Int]): Unit = {
        visited = visited :+ step.vertex
      }
    })
    // BFS should visit 1 first (depth 0), then 2 and 3 (depth 1), then 4 (depth 2)
    assertEquals(visited.size, 4)
    assertEquals(visited.head, 1)
    assert(visited.contains(4))
  }

  test("DFS visits all reachable vertices") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    graph.addEdge(1, 3)
    graph.addEdge(2, 4)

    var visited = List.empty[Int]
    graph.algorithms.depthFirstSearch(1, new SearchProcessor[Int] {
      def accept(step: SearchStep[Int]): Unit = {
        visited = visited :+ step.vertex
      }
    })
    assertEquals(visited.size, 4)
    assertEquals(visited.head, 1)
  }

  test("cycle detection - has cycle") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    graph.addEdge("C", "A")
    assert(graph.algorithms.containsCycle())
  }

  test("cycle detection - no cycle") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    assert(!graph.algorithms.containsCycle())
  }

  test("topological sort") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C", "D")
    graph.addEdge("A", "B")
    graph.addEdge("A", "C")
    graph.addEdge("B", "D")
    graph.addEdge("C", "D")

    val success = graph.topologicalSort()
    assert(success)

    val verts = graph.vertices.toList
    // A must come before B, C; B and C must come before D
    assert(verts.indexOf("A") < verts.indexOf("B"))
    assert(verts.indexOf("A") < verts.indexOf("C"))
    assert(verts.indexOf("B") < verts.indexOf("D"))
    assert(verts.indexOf("C") < verts.indexOf("D"))
  }

  test("edge weight") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B")
    val edge = graph.addEdge("A", "B", 5.0f)
    assertEquals(edge.weight, 5.0f)
  }

  test("graph toString") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B")
    graph.addEdge("A", "B")
    assertEquals(graph.toString, "Directed graph with 2 vertices and 1 edges")
  }

  test("complete graph builder") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    GraphBuilder.buildCompleteGraph(graph)
    // Directed complete graph with 3 vertices has 3*2 = 6 edges
    assertEquals(graph.edgeCount, 6)
  }

  test("minimum distance") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B", 3.0f)
    graph.addEdge("B", "C", 4.0f)
    val dist = graph.algorithms.findMinimumDistance("A", "C")
    assertEquals(dist, 7.0f)
  }

  test("isConnected between vertices") {
    val graph = DirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B")
    assert(graph.algorithms.isConnected("A", "B"))
    assert(!graph.algorithms.isConnected("A", "C"))
  }
}
