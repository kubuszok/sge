/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphs
package algorithms

import java.util.concurrent.atomic.AtomicInteger

import sge.graphs.utils.{ Heuristic, SearchProcessor }

/** Factory/coordinator for graph algorithms. */
abstract class Algorithms[V](protected val graph: Graph[V]) {

  private val runID: AtomicInteger = AtomicInteger()

  def requestRunID(): Int = runID.getAndIncrement()

  // --------------------
  //  Shortest Path
  // --------------------

  /** Find a shortest path from the start vertex to the target vertex, using Dijkstra's algorithm. */
  def findShortestPath(start: V, target: V): Path[V] =
    findShortestPath(start, target, null.asInstanceOf[Heuristic[V]], null.asInstanceOf[SearchProcessor[V]]) // @nowarn — null means no heuristic/processor

  /** Find a shortest path with a search processor callback. */
  def findShortestPath(start: V, target: V, processor: SearchProcessor[V]): Path[V] =
    findShortestPath(start, target, null.asInstanceOf[Heuristic[V]], processor) // @nowarn — null means no heuristic

  /** Find a shortest path using A* search with the provided heuristic. */
  def findShortestPath(start: V, target: V, heuristic: Heuristic[V]): Path[V] =
    findShortestPath(start, target, heuristic, null.asInstanceOf[SearchProcessor[V]]) // @nowarn — null means no processor

  /** Find a shortest path using A* search with heuristic and processor. */
  def findShortestPath(start: V, target: V, heuristic: Heuristic[V], processor: SearchProcessor[V]): Path[V] = {
    val search = newAStarSearch(start, target, heuristic, processor)
    search.finish()
    search.getPath
  }

  /** Create a new A* search that can be stepped. */
  def newAStarSearch(start: V, target: V, heuristic: Heuristic[V], processor: SearchProcessor[V]): AStarSearch[V] = {
    val startNode  = graph.getNode(start)
    val targetNode = graph.getNode(target)
    if (startNode == null || targetNode == null) throw IllegalArgumentException("At least one vertex is not in the graph")
    AStarSearch[V](requestRunID(), startNode, targetNode, heuristic, processor)
  }

  /** Find the length of a shortest path from the start vertex to the target vertex. */
  def findMinimumDistance(start: V, target: V): Float =
    findMinimumDistance(start, target, null.asInstanceOf[Heuristic[V]]) // @nowarn — null means no heuristic

  /** Find the length of a shortest path using A* with the provided heuristic. */
  def findMinimumDistance(start: V, target: V, heuristic: Heuristic[V]): Float = {
    val search = newAStarSearch(start, target, heuristic, null.asInstanceOf[SearchProcessor[V]]) // @nowarn — null means no processor
    search.finish()
    if (search.getEnd == null) Float.MaxValue
    else search.getEnd.distance
  }

  /** Checks whether there exists a path from the start vertex to target vertex. */
  def isConnected(start: V, target: V): Boolean =
    findMinimumDistance(start, target) < Float.MaxValue

  // --------------------
  // Graph Searching
  // --------------------

  /** Perform a breadth first search starting from the specified vertex. */
  def breadthFirstSearch(v: V, processor: SearchProcessor[V]): Unit = {
    val node = graph.getNode(v)
    if (node == null) throw IllegalArgumentException("Vertex is not in the graph")
    BreadthFirstSearch[V](requestRunID(), node, processor).finish()
  }

  /** Perform a depth first search starting from the specified vertex. */
  def depthFirstSearch(v: V, processor: SearchProcessor[V]): Unit = {
    val node = graph.getNode(v)
    if (node == null) throw IllegalArgumentException("Vertex is not in the graph")
    DepthFirstSearch[V](requestRunID(), node, processor).finish()
  }

  /** Perform a search using Dijkstra's algorithm starting from the specified vertex. */
  def dijkstraSearch(v: V, processor: SearchProcessor[V]): Unit = {
    val node = graph.getNode(v)
    if (node == null) throw IllegalArgumentException("Vertex is not in the graph")
    AStarSearch[V](requestRunID(), node, null.asInstanceOf[Node[V]], null.asInstanceOf[Heuristic[V]], processor).finish() // @nowarn — null target/heuristic for open-ended search
  }

  // --------------------
  //  Structures
  // --------------------

  /** Checks whether there are any cycles in the graph using depth first searches. */
  def containsCycle(): Boolean =
    CycleDetector[V](requestRunID(), graph).containsCycle
}
