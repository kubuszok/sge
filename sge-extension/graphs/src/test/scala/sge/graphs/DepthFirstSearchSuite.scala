package sge
package graphs

import scala.collection.mutable.ArrayBuffer

import munit.FunSuite

import sge.graphs.algorithms.SearchStep
import sge.graphs.utils.SearchProcessor

class DepthFirstSearchSuite extends FunSuite {

  test("DFS visits all reachable vertices in directed graph") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4, 5)
    graph.addEdge(1, 2)
    graph.addEdge(1, 3)
    graph.addEdge(2, 4)
    graph.addEdge(3, 5)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          visited += step.vertex
                                      }
    )

    assertEquals(visited.size, 5)
    assertEquals(visited.head, 1)
    assert(visited.contains(2))
    assert(visited.contains(3))
    assert(visited.contains(4))
    assert(visited.contains(5))
  }

  test("DFS only visits reachable vertices") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    // 3 and 4 are isolated
    graph.addEdge(3, 4)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          visited += step.vertex
                                      }
    )

    assertEquals(visited.size, 2)
    assert(visited.contains(1))
    assert(visited.contains(2))
    assert(!visited.contains(3))
    assert(!visited.contains(4))
  }

  test("DFS visits in depth-first order") {
    // Build a linear chain: 1 -> 2 -> 3 -> 4
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)
    graph.addEdge(3, 4)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          visited += step.vertex
                                      }
    )

    // In a linear chain, DFS should visit 1, 2, 3, 4 in order
    assertEquals(visited.toList, List(1, 2, 3, 4))
  }

  test("DFS tracks depth correctly") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)

    val depths = ArrayBuffer.empty[(Int, Int)]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          depths += ((step.vertex, step.depth))
                                      }
    )

    assertEquals(depths.size, 3)
    assertEquals(depths.find(_._1 == 1).get._2, 0) // root depth = 0
    assertEquals(depths.find(_._1 == 2).get._2, 1)
    assertEquals(depths.find(_._1 == 3).get._2, 2)
  }

  test("DFS with terminate stops early") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)
    graph.addEdge(3, 4)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(
      1,
      new SearchProcessor[Int] {
        def accept(step: SearchStep[Int]): Unit = {
          visited += step.vertex
          if (step.vertex == 2) step.terminate()
        }
      }
    )

    // Should stop after visiting vertex 2
    assert(visited.contains(1))
    assert(visited.contains(2))
    assert(!visited.contains(3))
    assert(!visited.contains(4))
  }

  test("DFS with ignore skips neighbours") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3, 4)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)
    graph.addEdge(1, 4)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit = {
                                          visited += step.vertex
                                          if (step.vertex == 2) step.ignore()
                                        }
                                      }
    )

    // Ignoring vertex 2 should skip its neighbour 3 but still visit 4
    assert(visited.contains(1))
    assert(visited.contains(2))
    assert(!visited.contains(3), "Vertex 3 should not be visited when vertex 2 is ignored")
    assert(visited.contains(4))
  }

  test("DFS on undirected graph visits all connected vertices") {
    val graph = UndirectedGraph[String]()
    graph.addVertices("A", "B", "C", "D")
    graph.addEdge("A", "B")
    graph.addEdge("B", "C")
    graph.addEdge("C", "D")

    val visited = ArrayBuffer.empty[String]
    graph.algorithms.depthFirstSearch("A",
                                      new SearchProcessor[String] {
                                        def accept(step: SearchStep[String]): Unit =
                                          visited += step.vertex
                                      }
    )

    assertEquals(visited.size, 4)
  }

  test("DFS on single vertex graph") {
    val graph = DirectedGraph[Int]()
    graph.addVertex(42)

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(42,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          visited += step.vertex
                                      }
    )

    assertEquals(visited.toList, List(42))
  }

  test("DFS handles cycles without infinite loop") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)
    graph.addEdge(3, 1) // cycle

    val visited = ArrayBuffer.empty[Int]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          visited += step.vertex
                                      }
    )

    assertEquals(visited.size, 3)
  }

  test("DFS tracks count correctly") {
    val graph = DirectedGraph[Int]()
    graph.addVertices(1, 2, 3)
    graph.addEdge(1, 2)
    graph.addEdge(2, 3)

    val counts = ArrayBuffer.empty[(Int, Int)]
    graph.algorithms.depthFirstSearch(1,
                                      new SearchProcessor[Int] {
                                        def accept(step: SearchStep[Int]): Unit =
                                          counts += ((step.vertex, step.count))
                                      }
    )

    // Count increments with each step processed
    assertEquals(counts.find(_._1 == 1).get._2, 0) // first step
    assertEquals(counts.find(_._1 == 2).get._2, 1) // second step
    assertEquals(counts.find(_._1 == 3).get._2, 2) // third step
  }
}
