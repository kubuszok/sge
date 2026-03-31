package sge
package ai
package pfa

import sge.ai.pfa.indexed.{ IndexedAStarPathFinder, IndexedGraph }
import sge.utils.DynamicArray

/** A simple grid node with (x, y) coordinates and a precomputed index. */
final class GridNode(val x: Int, val y: Int, val index: Int) {
  override def toString: String = s"($x,$y)"
}

/** A 2D grid graph implementing IndexedGraph for A* tests.
  *
  * Each node connects to its 4-directional neighbors (up/down/left/right) unless blocked. Connections have cost 1.
  */
final class GridGraph(val width: Int, val height: Int) extends IndexedGraph[GridNode] {

  val nodes: Array[GridNode] = {
    val arr = new Array[GridNode](width * height)
    for {
      y <- 0 until height
      x <- 0 until width
    }
      arr(y * width + x) = new GridNode(x, y, y * width + x)
    arr
  }

  private val connections: Array[DynamicArray[Connection[GridNode]]] = {
    val arr = new Array[DynamicArray[Connection[GridNode]]](width * height)
    for (i <- 0 until width * height)
      arr(i) = DynamicArray[Connection[GridNode]]()
    arr
  }

  /** Build default 4-directional connections for all nodes. */
  def buildConnections(): Unit = {
    val dirs = Array((0, 1), (0, -1), (1, 0), (-1, 0))
    for {
      y <- 0 until height
      x <- 0 until width
    } {
      val fromIdx = y * width + x
      val from    = nodes(fromIdx)
      for ((dx, dy) <- dirs) {
        val nx = x + dx
        val ny = y + dy
        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
          val to = nodes(ny * width + nx)
          connections(fromIdx).add(new DefaultConnection[GridNode](from, to))
        }
      }
    }
  }

  /** Remove all connections from a node (making it impassable as a source). */
  def blockNode(x: Int, y: Int): Unit = {
    val idx = y * width + x
    connections(idx).clear()
    // Also remove connections TO this node from neighbors
    val dirs = Array((0, 1), (0, -1), (1, 0), (-1, 0))
    for ((dx, dy) <- dirs) {
      val nx = x + dx
      val ny = y + dy
      if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
        val neighborIdx = ny * width + nx
        val conns       = connections(neighborIdx)
        // Remove connections that lead to the blocked node
        var i = conns.size - 1
        while (i >= 0) {
          if (conns(i).getToNode.index == idx) {
            conns.removeIndex(i)
          }
          i -= 1
        }
      }
    }
  }

  def node(x: Int, y: Int): GridNode = nodes(y * width + x)

  override def getConnections(fromNode: GridNode): DynamicArray[Connection[GridNode]] =
    connections(fromNode.index)

  override def getIndex(node: GridNode): Int = node.index

  override def getNodeCount: Int = width * height
}

/** Manhattan distance heuristic for grid pathfinding. */
class ManhattanHeuristic extends Heuristic[GridNode] {
  override def estimate(node: GridNode, endNode: GridNode): Float =
    (Math.abs(node.x - endNode.x) + Math.abs(node.y - endNode.y)).toFloat
}

class AStarSuite extends munit.FunSuite {

  test("find shortest path from (0,0) to (4,4) on 5x5 grid") {
    val graph = new GridGraph(5, 5)
    graph.buildConnections()

    val pathFinder = new IndexedAStarPathFinder[GridNode](graph)
    val path       = DefaultGraphPath[GridNode]()
    val heuristic  = new ManhattanHeuristic()

    val found = pathFinder.searchNodePath(graph.node(0, 0), graph.node(4, 4), heuristic, path)
    assert(found, "Path should be found")

    // Shortest path on a 5x5 grid from (0,0) to (4,4) is 8 steps (Manhattan distance)
    assertEquals(path.getCount, 9) // 9 nodes = 8 edges

    // First node should be start, last should be end
    assertEquals(path.get(0).x, 0)
    assertEquals(path.get(0).y, 0)
    assertEquals(path.get(path.getCount - 1).x, 4)
    assertEquals(path.get(path.getCount - 1).y, 4)

    // Verify each step is adjacent (Manhattan distance 1)
    var i = 0
    while (i < path.getCount - 1) {
      val a    = path.get(i)
      val b    = path.get(i + 1)
      val dist = Math.abs(a.x - b.x) + Math.abs(a.y - b.y)
      assertEquals(dist, 1, s"Step $i->${i + 1}: (${a.x},${a.y})->(${b.x},${b.y}) dist=$dist")
      i += 1
    }
  }

  test("path around obstacle") {
    val graph = new GridGraph(5, 5)
    graph.buildConnections()

    // Block a wall at x=2 from y=0 to y=3, forcing path to go around
    for (y <- 0 to 3)
      graph.blockNode(2, y)

    val pathFinder = new IndexedAStarPathFinder[GridNode](graph)
    val path       = DefaultGraphPath[GridNode]()
    val heuristic  = new ManhattanHeuristic()

    val found = pathFinder.searchNodePath(graph.node(0, 0), graph.node(4, 0), heuristic, path)
    assert(found, "Path should be found around obstacle")

    // Path must avoid blocked nodes
    var i = 0
    while (i < path.getCount) {
      val n = path.get(i)
      assert(!(n.x == 2 && n.y <= 3), s"Path should not go through blocked node (${n.x},${n.y})")
      i += 1
    }

    // Path is longer than the direct 4-step path
    assert(path.getCount > 5, s"Path should be longer than direct route, got ${path.getCount} nodes")

    // First and last nodes correct
    assertEquals(path.get(0).x, 0)
    assertEquals(path.get(0).y, 0)
    assertEquals(path.get(path.getCount - 1).x, 4)
    assertEquals(path.get(path.getCount - 1).y, 0)
  }

  test("no path exists for isolated nodes") {
    val graph = new GridGraph(5, 5)
    graph.buildConnections()

    // Block all neighbors of (4,4) to isolate it
    graph.blockNode(4, 4)
    // Also block (3,4) and (4,3) connections TO (4,4) already handled by blockNode

    val pathFinder = new IndexedAStarPathFinder[GridNode](graph)
    val path       = DefaultGraphPath[GridNode]()
    val heuristic  = new ManhattanHeuristic()

    val found = pathFinder.searchNodePath(graph.node(0, 0), graph.node(4, 4), heuristic, path)
    assert(!found, "No path should exist to isolated node")
  }

  test("path from node to itself") {
    val graph = new GridGraph(3, 3)
    graph.buildConnections()

    val pathFinder = new IndexedAStarPathFinder[GridNode](graph)
    val path       = DefaultGraphPath[GridNode]()
    val heuristic  = new ManhattanHeuristic()

    val start = graph.node(1, 1)
    val found = pathFinder.searchNodePath(start, start, heuristic, path)
    assert(found, "Path from node to itself should be found")
    assertEquals(path.getCount, 1)
  }

  test("connection path works") {
    val graph = new GridGraph(3, 3)
    graph.buildConnections()

    val pathFinder = new IndexedAStarPathFinder[GridNode](graph)
    val path       = DefaultGraphPath[Connection[GridNode]]()
    val heuristic  = new ManhattanHeuristic()

    val found = pathFinder.searchConnectionPath(graph.node(0, 0), graph.node(2, 2), heuristic, path)
    assert(found, "Connection path should be found")
    // 4 edges for Manhattan distance of 4 from (0,0) to (2,2)
    assertEquals(path.getCount, 4)
  }
}
