package sge
package graphs

import munit.FunSuite

class MinimumWeightSpanningTreeSuite extends FunSuite {

  test("MST of simple triangle") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C")
    graph.addEdge("A", "B", 1.0f)
    graph.addEdge("B", "C", 2.0f)
    graph.addEdge("A", "C", 3.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    // MST should have n-1 = 2 edges
    assertEquals(mst.edgeCount, 2)
    // Total weight = 1 + 2 = 3 (not the 3-weight edge)
    val totalWeight = mst.edges.map(_.weight).sum
    assertEquals(totalWeight, 3.0f)
  }

  test("MST of path graph returns same graph") {
    val graph = UndirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2, 1.0f)
    graph.addEdge(2, 3, 1.0f)
    graph.addEdge(3, 4, 1.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 3)
    val totalWeight = mst.edges.map(_.weight).sum
    assertEquals(totalWeight, 3.0f)
  }

  test("MST picks lighter edges") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C", "D")
    graph.addEdge("A", "B", 1.0f)
    graph.addEdge("A", "C", 5.0f)
    graph.addEdge("B", "C", 2.0f)
    graph.addEdge("B", "D", 3.0f)
    graph.addEdge("C", "D", 4.0f)
    graph.addEdge("A", "D", 10.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 3)
    val totalWeight = mst.edges.map(_.weight).sum
    // Optimal MST: A-B(1) + B-C(2) + B-D(3) = 6
    assertEquals(totalWeight, 6.0f)
  }

  test("MST of single vertex graph has no edges") {
    val graph = UndirectedGraph[Int]()
    graph.addVertex(1)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 0)
    assertEquals(mst.size, 1)
  }

  test("MST of two vertices with one edge") {
    val graph = UndirectedGraph[Int]()
    graph.addVertices(1, 2)
    graph.addEdge(1, 2, 5.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 1)
    val totalWeight = mst.edges.map(_.weight).sum
    assertEquals(totalWeight, 5.0f)
  }

  test("MST preserves all vertices") {
    val graph = UndirectedGraph[Int]()
    for (i <- 1 to 6) graph.addVertex(i)
    graph.addEdge(1, 2, 1.0f)
    graph.addEdge(2, 3, 2.0f)
    graph.addEdge(3, 4, 3.0f)
    graph.addEdge(4, 5, 4.0f)
    graph.addEdge(5, 6, 5.0f)
    graph.addEdge(1, 6, 100.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.size, 6) // all vertices present
    assertEquals(mst.edgeCount, 5)
  }

  test("MST with equal weight edges") {
    val graph = UndirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    graph.addEdge(1, 2, 1.0f)
    graph.addEdge(2, 3, 1.0f)
    graph.addEdge(1, 3, 1.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 2)
    val totalWeight = mst.edges.map(_.weight).sum
    assertEquals(totalWeight, 2.0f)
  }

  test("MST on grid graph") {
    // 3x3 grid with unit weights
    val n     = 3
    val graph = UndirectedGraph[Vector2]()
    GraphTestUtils.makeGridGraph(graph, n)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    // n*n vertices, MST should have n*n-1 edges
    assertEquals(mst.edgeCount, n * n - 1)
  }

  test("MST on complete graph") {
    val graph = UndirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    GraphBuilder.buildCompleteGraph(graph)

    // Set specific weights
    graph.addEdge(1, 2, 1.0f)
    graph.addEdge(1, 3, 4.0f)
    graph.addEdge(1, 4, 3.0f)
    graph.addEdge(2, 3, 2.0f)
    graph.addEdge(2, 4, 5.0f)
    graph.addEdge(3, 4, 6.0f)

    val mst = graph.algorithms.findMinimumWeightSpanningTree()
    assertEquals(mst.edgeCount, 3)
    val totalWeight = mst.edges.map(_.weight).sum
    // Optimal: 1-2(1) + 2-3(2) + 1-4(3) = 6
    assertEquals(totalWeight, 6.0f)
  }
}
