/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/indexed/IndexedAStarPathFinder.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa.indexed` -> `sge.ai.pfa.indexed`; `Array` -> `DynamicArray`;
 *     `BinaryHeap` -> `sge.utils.BinaryHeap`; `TimeUtils` -> `sge.utils.TimeUtils`
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`;
 *     `= _` -> `scala.compiletime.uninitialized`; inner static classes -> companion/private classes
 *   Idiom: unchecked array cast replaced with Array[AnyRef] approach
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa
package indexed

import sge.utils.BinaryHeap
import sge.utils.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** A fully implemented [[PathFinder]] that can perform both interruptible and non-interruptible pathfinding.
  *
  * This implementation is a common variation of the A* algorithm that is faster than the general A*.
  *
  * In the general A* implementation, data are held for each node in the open or closed lists, and these data are held as a NodeRecord instance. Records are created when a node is first considered and
  * then moved between the open and closed lists, as required. There is a key step in the algorithm where the lists are searched for a node record corresponding to a particular node. This operation is
  * something time-consuming.
  *
  * The indexed A* algorithm improves execution speed by using an array of all the node records for every node in the graph. Nodes must be numbered using sequential integers (see
  * [[IndexedGraph.getIndex]]), so we don't need to search for a node in the two lists at all. We can simply use the node index to look up its record in the array (creating it if it is missing). This
  * means that the close list is no longer needed. To know whether a node is open or closed, we use the [[IndexedAStarPathFinder.NodeRecord.category category]] of the node record. This makes the
  * search step very fast indeed (in fact, there is no search, and we can go straight to the information we need). Unfortunately, we can't get rid of the open list because we still need to be able to
  * retrieve the element with the lowest cost. However, we use a [[BinaryHeap]] for the open list in order to keep performance as high as possible.
  *
  * This class defaults to comparing `N` nodes by reference equality, but you can customize this behavior by passing a [[StopCondition]] to the constructor, such as an [[EqualsMethodStopCondition]] to
  * use the `equals()` method on each node for comparisons. You can also set [[stopCondition]] after construction, though typically before finding a path. More elaborate implementations are possible
  * that can stop pathfinding under specific conditions, like a path moving off-screen.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class IndexedAStarPathFinder[N](
  val graph:         IndexedGraph[N],
  calculateMetrics:  Boolean,
  var stopCondition: StopCondition[N]
) extends PathFinder[N] {

  private val nodeRecords: Array[NodeRecord[N]]      = new Array[NodeRecord[N]](graph.getNodeCount)
  private val openList:    BinaryHeap[NodeRecord[N]] = BinaryHeap[NodeRecord[N]]()
  private var current:     Nullable[NodeRecord[N]]   = Nullable.empty

  /** Optional metrics for the search. Only populated when `calculateMetrics` is `true`. */
  val metrics: Nullable[IndexedAStarPathFinder.Metrics] =
    if (calculateMetrics) Nullable(IndexedAStarPathFinder.Metrics()) else Nullable.empty

  /** The unique ID for each search run. Used to mark nodes. */
  private var searchId: Int = 0

  def this(graph: IndexedGraph[N]) =
    this(graph, false, EqualsByReferenceStopCondition[N]())

  def this(graph: IndexedGraph[N], calculateMetrics: Boolean) =
    this(graph, calculateMetrics, EqualsByReferenceStopCondition[N]())

  override def searchConnectionPath(
    startNode: N,
    endNode:   N,
    heuristic: Heuristic[N],
    outPath:   GraphPath[Connection[N]]
  ): Boolean = {
    // Perform AStar
    val found = searchInternal(startNode, endNode, heuristic)

    if (found) {
      // Create a path made of connections
      generateConnectionPath(startNode, outPath)
    }

    found
  }

  override def searchNodePath(startNode: N, endNode: N, heuristic: Heuristic[N], outPath: GraphPath[N]): Boolean = {
    // Perform AStar
    val found = searchInternal(startNode, endNode, heuristic)

    if (found) {
      // Create a path made of nodes
      generateNodePath(startNode, outPath)
    }

    found
  }

  protected def searchInternal(startNode: N, endNode: N, heuristic: Heuristic[N]): Boolean = {

    initSearch(startNode, endNode, heuristic)

    // Iterate through processing each node
    boundary {
      var done = false
      while (!done) {
        // Retrieve the node with smallest estimated total cost from the open list
        val cur = openList.pop()
        current = Nullable(cur)
        cur.category = NodeRecord.CLOSED

        // Terminate if we reached the stop condition
        if (stopCondition.shouldStopSearch(cur.node, endNode)) break(true)

        visitChildren(endNode, heuristic)

        if (openList.size <= 0) done = true
      }

      // We've run out of nodes without finding the goal, so there's no solution
      false
    }
  }

  override def search(request: PathFinderRequest[N], timeToRun: Long): Boolean = {

    var lastTime      = TimeUtils.nanoTime().toLong
    var remainingTime = timeToRun

    // We have to initialize the search if the status has just changed
    if (request.statusChanged) {
      initSearch(request.startNode, request.endNode, request.heuristic)
      request.statusChanged = false
    }

    // Iterate through processing each node
    boundary {
      var done = false
      while (!done) {

        // Check the available time
        val currentTime = TimeUtils.nanoTime().toLong
        remainingTime -= currentTime - lastTime
        if (remainingTime <= PathFinderQueue.TIME_TOLERANCE) break(false)

        // Retrieve the node with smallest estimated total cost from the open list
        val cur = openList.pop()
        current = Nullable(cur)
        cur.category = NodeRecord.CLOSED

        // Terminate if we reached the stop condition; we've found a path.
        if (stopCondition.shouldStopSearch(cur.node, request.endNode)) {
          request.pathFound = true
          generateNodePath(request.startNode, request.resultPath)
          break(true)
        }

        // Visit current node's children
        visitChildren(request.endNode, request.heuristic)

        // Store the current time
        lastTime = currentTime

        if (openList.size <= 0) done = true
      }

      // The open list is empty and we've not found a path.
      request.pathFound = false
      true
    }
  }

  protected def initSearch(startNode: N, endNode: N, heuristic: Heuristic[N]): Unit = {
    metrics.foreach(_.reset())

    // Increment the search id
    searchId += 1
    if (searchId < 0) searchId = 1

    // Initialize the open list
    openList.clear()

    // Initialize the record for the start node and add it to the open list
    val startRecord = getNodeRecord(startNode)
    startRecord.node = startNode
    startRecord.connection = Nullable.empty
    startRecord.costSoFar = 0
    addToOpenList(startRecord, heuristic.estimate(startNode, endNode))

    current = Nullable.empty
  }

  protected def visitChildren(endNode: N, heuristic: Heuristic[N]): Unit = {
    val cur = current.get
    // Get current node's outgoing connections
    val connections = graph.getConnections(cur.node)

    // Loop through each connection in turn
    var i = 0
    while (i < connections.size) {
      metrics.foreach(_.visitedNodes += 1)

      val connection = connections(i)

      // Get the cost estimate for the node
      val node     = connection.getToNode
      val nodeCost = cur.costSoFar + connection.getCost

      var nodeHeuristic: Float = 0f
      val nodeRecord = getNodeRecord(node)
      if (nodeRecord.category == NodeRecord.CLOSED) {
        // The node is closed

        // If we didn't find a shorter route, skip
        if (nodeRecord.costSoFar <= nodeCost) {
          // skip - do nothing
        } else {
          // We can use the node's old cost values to calculate its heuristic
          // without calling the possibly expensive heuristic function
          nodeHeuristic = nodeRecord.getEstimatedTotalCost - nodeRecord.costSoFar

          // Update node record's cost and connection
          nodeRecord.costSoFar = nodeCost
          nodeRecord.connection = Nullable(connection)

          // Add it to the open list with the estimated total cost
          addToOpenList(nodeRecord, nodeCost + nodeHeuristic)
        }
      } else if (nodeRecord.category == NodeRecord.OPEN) {
        // The node is open

        // If our route is no better, then skip
        if (nodeRecord.costSoFar <= nodeCost) {
          // skip - do nothing
        } else {
          // Remove it from the open list (it will be re-added with the new cost)
          openList.remove(nodeRecord)

          // We can use the node's old cost values to calculate its heuristic
          // without calling the possibly expensive heuristic function
          nodeHeuristic = nodeRecord.getEstimatedTotalCost - nodeRecord.costSoFar

          // Update node record's cost and connection
          nodeRecord.costSoFar = nodeCost
          nodeRecord.connection = Nullable(connection)

          // Add it to the open list with the estimated total cost
          addToOpenList(nodeRecord, nodeCost + nodeHeuristic)
        }
      } else {
        // the node is unvisited

        // We'll need to calculate the heuristic value using the function,
        // since we don't have a node record with a previously calculated value
        nodeHeuristic = heuristic.estimate(node, endNode)

        // Update node record's cost and connection
        nodeRecord.costSoFar = nodeCost
        nodeRecord.connection = Nullable(connection)

        // Add it to the open list with the estimated total cost
        addToOpenList(nodeRecord, nodeCost + nodeHeuristic)
      }

      i += 1
    }
  }

  protected def generateConnectionPath(startNode: N, outPath: GraphPath[Connection[N]]): Unit = {
    // Work back along the path, accumulating connections
    var cur = current.get
    while (cur.node != startNode) {
      val conn = cur.connection.get
      outPath.add(conn)
      cur = nodeRecords(graph.getIndex(conn.getFromNode))
    }

    // Reverse the path
    outPath.reverse()
  }

  protected def generateNodePath(startNode: N, outPath: GraphPath[N]): Unit = {
    // Work back along the path, accumulating nodes
    var cur = current.get
    while (cur.connection.isDefined) {
      outPath.add(cur.node)
      val conn = cur.connection.get
      cur = nodeRecords(graph.getIndex(conn.getFromNode))
    }
    outPath.add(startNode)

    // Reverse the path
    outPath.reverse()
  }

  protected def addToOpenList(nodeRecord: NodeRecord[N], estimatedTotalCost: Float): Unit = {
    openList.add(nodeRecord, estimatedTotalCost)
    nodeRecord.category = NodeRecord.OPEN
    metrics.foreach { m =>
      m.openListAdditions += 1
      m.openListPeak = Math.max(m.openListPeak, openList.size)
    }
  }

  protected def getNodeRecord(node: N): NodeRecord[N] = {
    val index = graph.getIndex(node)
    var nr    = nodeRecords(index)
    if (nr != null) {
      if (nr.searchId != searchId) {
        nr.category = NodeRecord.UNVISITED
        nr.searchId = searchId
      }
      nr
    } else {
      nr = NodeRecord[N]()
      nodeRecords(index) = nr
      nr.node = node
      nr.searchId = searchId
      nr
    }
  }
}

object IndexedAStarPathFinder {

  /** A class used by [[IndexedAStarPathFinder]] to collect search metrics.
    *
    * @author
    *   davebaol (original implementation)
    */
  class Metrics {
    var visitedNodes:      Int = 0
    var openListAdditions: Int = 0
    var openListPeak:      Int = 0

    def reset(): Unit = {
      visitedNodes = 0
      openListAdditions = 0
      openListPeak = 0
    }
  }
}

/** This class is used to keep track of the information we need for each node during the search.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
private[indexed] class NodeRecord[N] extends BinaryHeap.Node(0f) {

  /** The reference to the node. */
  var node: N = scala.compiletime.uninitialized

  /** The incoming connection to the node */
  var connection: Nullable[Connection[N]] = Nullable.empty

  /** The actual cost from the start node. */
  var costSoFar: Float = 0f

  /** The node category: UNVISITED, OPEN or CLOSED. */
  var category: Int = NodeRecord.UNVISITED

  /** ID of the current search. */
  var searchId: Int = 0

  /** Returns the estimated total cost. */
  def getEstimatedTotalCost: Float = value
}

private[indexed] object NodeRecord {
  val UNVISITED: Int = 0
  val OPEN:      Int = 1
  val CLOSED:    Int = 2
}

/** This trait is used to define criteria to interrupt the search. Normally, equality is fine as a criterion, but some situations may use additional or different criteria, such as a maximum distance
  * from a point being reached, or the path moving off-screen.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   niemandkun (original implementation)
  */
trait StopCondition[N] {
  def shouldStopSearch(currentNode: N, endNode: N): Boolean
}

/** Default implementation of [[StopCondition]], which compares two given nodes by reference. User code typically doesn't need to create one of these StopConditions, because [[IndexedAStarPathFinder]]
  * creates one by default.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   niemandkun (original implementation)
  */
class EqualsByReferenceStopCondition[N] extends StopCondition[N] {
  override def shouldStopSearch(currentNode: N, endNode: N): Boolean =
    (currentNode.asInstanceOf[AnyRef]) eq (endNode.asInstanceOf[AnyRef])
}

/** A [[StopCondition]] which compares two given nodes by calling `currentNode.equals(endNode)` if currentNode is not null. If currentNode is null, then this always returns false (meaning a null node
  * is not equal to anything, including a null endNode).
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   tommyettinger (original implementation)
  */
class EqualsMethodStopCondition[N] extends StopCondition[N] {
  override def shouldStopSearch(currentNode: N, endNode: N): Boolean =
    currentNode != null && currentNode.equals(endNode)
}
