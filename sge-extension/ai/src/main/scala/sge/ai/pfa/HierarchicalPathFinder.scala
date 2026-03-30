/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/HierarchicalPathFinder.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *     `GdxAI.getLogger()` -> logging removed
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`;
 *     `== null` / `!= null` -> Nullable checks
 *   Idiom: inner static class `LevelPathFinderRequest` -> private class
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa

import sge.utils.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** A `HierarchicalPathFinder` can find a path in an arbitrary [[HierarchicalGraph]] using the given [[PathFinder]], known as level path finder, on each level of the hierarchy.
  *
  * Pathfinding on a hierarchical graph applies the level path finder algorithm several times, starting at a high level of the hierarchy and working down. The results at high levels are used to limit
  * the work it needs to do at lower levels.
  *
  * Note that the hierarchical path finder calls the [[HierarchicalGraph.setLevel]] method to switch the graph into a particular level. All future calls to the
  * [[HierarchicalGraph.getConnections getConnections]] method of the hierarchical graph then act as if the graph was just a simple, non-hierarchical graph at that level. This way, the level path
  * finder has no way of telling that it is working with a hierarchical graph and it doesn't need to, meaning that you can use any path finder implementation for the level path finder.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class HierarchicalPathFinder[N](
  val graph:           HierarchicalGraph[N],
  val levelPathFinder: PathFinder[N]
) extends PathFinder[N] {

  private var levelRequest:        Nullable[LevelPathFinderRequest[N]]   = Nullable.empty
  private var levelRequestControl: Nullable[PathFinderRequestControl[N]] = Nullable.empty

  override def searchNodePath(startNode: N, endNode: N, heuristic: Heuristic[N], outPath: GraphPath[N]): Boolean =
    // Check if we have no path to find
    if (startNode == endNode) {
      true
    } else {
      // Set up our initial pair of nodes
      var currentStartNode = startNode
      var currentEndNode   = endNode
      var levelOfNodes     = 0

      // Descend through levels of the graph
      var currentLevel = graph.getLevelCount - 1
      boundary {
        while (currentLevel >= 0) {
          // Find the start node at current level
          currentStartNode = graph.convertNodeBetweenLevels(0, startNode, currentLevel)

          // Find the end node at current level
          // Note that if we're examining level 0 and the current end node, the end node and the
          // start node have the same parent at level 1 then we can use the end node directly.
          currentEndNode = graph.convertNodeBetweenLevels(levelOfNodes, currentEndNode, currentLevel)
          if (currentLevel == 0) {
            val currentEndNodeParent = graph.convertNodeBetweenLevels(0, currentEndNode, 1)
            if (
              currentEndNodeParent == graph.convertNodeBetweenLevels(0, endNode, 1)
              && currentEndNodeParent == graph.convertNodeBetweenLevels(0, startNode, 1)
            ) {
              currentEndNode = endNode
            }
          }

          // Decrease current level and skip it if start and end node are the same
          levelOfNodes = currentLevel
          currentLevel -= 1
          if (currentStartNode != currentEndNode) {
            // Otherwise we can perform the plan
            graph.setLevel(levelOfNodes)
            outPath.clear()
            val pathFound = levelPathFinder.searchNodePath(currentStartNode, currentEndNode, heuristic, outPath)

            if (!pathFound) break(false)

            // Now take the first move of this plan and use it for the next run through
            currentEndNode = outPath.get(1)
          }
        }

        // Return success.
        // Note that outPath contains the last path we considered which is at level zero
        true
      }
    }

  override def searchConnectionPath(
    startNode: N,
    endNode:   N,
    heuristic: Heuristic[N],
    outPath:   GraphPath[Connection[N]]
  ): Boolean =
    // Check if we have no path to find
    if (startNode == endNode) {
      true
    } else {
      // Set up our initial pair of nodes
      var currentStartNode = startNode
      var currentEndNode   = endNode
      var levelOfNodes     = 0

      // Descend through levels of the graph
      var currentLevel = graph.getLevelCount - 1
      boundary {
        while (currentLevel >= 0) {
          // Find the start node at current level
          currentStartNode = graph.convertNodeBetweenLevels(0, startNode, currentLevel)

          // Find the end node at current level
          // Note that if we're examining level 0 and the current end node, the end node and the
          // start node have the same parent at level 1 then we can use the end node directly.
          currentEndNode = graph.convertNodeBetweenLevels(levelOfNodes, currentEndNode, currentLevel)
          if (currentLevel == 0) {
            val currentEndNodeParent = graph.convertNodeBetweenLevels(0, currentEndNode, 1)
            if (
              currentEndNodeParent == graph.convertNodeBetweenLevels(0, endNode, 1)
              && currentEndNodeParent == graph.convertNodeBetweenLevels(0, startNode, 1)
            ) {
              currentEndNode = endNode
            }
          }

          // Decrease current level and skip it if start and end node are the same
          levelOfNodes = currentLevel
          currentLevel -= 1
          if (currentStartNode != currentEndNode) {
            // Otherwise we can perform the plan
            graph.setLevel(levelOfNodes)
            outPath.clear()
            val pathFound = levelPathFinder.searchConnectionPath(currentStartNode, currentEndNode, heuristic, outPath)

            if (!pathFound) break(false)

            // Now take the first move of this plan and use it for the next run through
            currentEndNode = outPath.get(0).getToNode
          }
        }

        // Return success.
        // Note that outPath contains the last path we considered which is at level zero
        true
      }
    }

  override def search(request: PathFinderRequest[N], timeToRun: Long): Boolean = {
    // Make sure the level request and its control are instantiated
    if (levelRequest.isEmpty) {
      levelRequest = Nullable(LevelPathFinderRequest[N]())
      levelRequestControl = Nullable(PathFinderRequestControl[N]())
    }

    val lvlReq  = levelRequest.get
    val lvlCtrl = levelRequestControl.get

    // We have to initialize the search if the status has just changed
    if (request.statusChanged) {
      // Check if we have no path to find
      if (request.startNode == request.endNode) {
        true
      } else {
        // Prepare the level request control
        lvlCtrl.lastTime = TimeUtils.nanoTime().toLong // Keep track of the current time
        lvlCtrl.timeToRun = timeToRun
        lvlCtrl.timeTolerance = PathFinderQueue.TIME_TOLERANCE
        lvlCtrl.server = Nullable.empty
        lvlCtrl.pathFinder = levelPathFinder

        // Prepare the level request
        lvlReq.hpf = this
        lvlReq.hpfRequest = request
        lvlReq.status = PathFinderRequest.SEARCH_NEW
        lvlReq.statusChanged = true
        lvlReq.heuristic = request.heuristic
        lvlReq.resultPath = request.resultPath
        lvlReq.startNode = request.startNode
        lvlReq.endNode = request.endNode
        lvlReq.levelOfNodes = 0
        lvlReq.currentLevel = graph.getLevelCount - 1

        searchLoop(lvlReq, lvlCtrl)
      }
    } else {
      searchLoop(lvlReq, lvlCtrl)
    }
  }

  private def searchLoop(lvlReq: LevelPathFinderRequest[N], lvlCtrl: PathFinderRequestControl[N]): Boolean =
    boundary {
      while (lvlReq.currentLevel >= 0) {
        val finished = lvlCtrl.execute(lvlReq)

        if (!finished) {
          break(false)
        } else {
          lvlReq.executionFrames = 0
          lvlReq.status = PathFinderRequest.SEARCH_NEW
          lvlReq.statusChanged = true

          if (!lvlReq.pathFound) break(true)
        }
      }

      // If we're here we have finished
      true
    }
}

private class LevelPathFinderRequest[N] extends PathFinderRequest[N] {
  var hpf:        HierarchicalPathFinder[N] = scala.compiletime.uninitialized
  var hpfRequest: PathFinderRequest[N]      = scala.compiletime.uninitialized

  var levelOfNodes: Int = 0
  var currentLevel: Int = 0

  override def initializeSearch(timeToRun: Long): Boolean = {

    // Reset the status
    // We can do it here because we know this method completes during this frame,
    // meaning that it is executed once per request
    this.executionFrames = 0
    this.pathFound = false
    this.status = PathFinderRequest.SEARCH_NEW
    this.statusChanged = false

    var continue = true
    while (continue && currentLevel >= 0) {
      // Find the start node at current level
      startNode = hpf.graph.convertNodeBetweenLevels(0, hpfRequest.startNode, currentLevel)

      // Find the end node at current level
      // Note that if we're examining level 0 and the current end node, the end node and the
      // start node have the same parent at level 1 then we can use the end node directly.
      endNode = hpf.graph.convertNodeBetweenLevels(levelOfNodes, endNode, currentLevel)
      if (currentLevel == 0) {
        val currentEndNodeParent = hpf.graph.convertNodeBetweenLevels(0, endNode, 1)
        if (
          currentEndNodeParent == hpf.graph.convertNodeBetweenLevels(0, hpfRequest.endNode, 1)
          && currentEndNodeParent == hpf.graph.convertNodeBetweenLevels(0, hpfRequest.startNode, 1)
        ) {
          endNode = hpfRequest.endNode
        }
      }

      // Decrease current level and skip it if start and end node are the same
      // FIXME the break below is wrong
      levelOfNodes = currentLevel
      currentLevel -= 1
      if (startNode != endNode) continue = false
    }

    // Otherwise we can perform the plan
    hpf.graph.setLevel(levelOfNodes)
    resultPath.clear()
    true
  }

  override def search(pathFinder: PathFinder[N], timeToRun: Long): Boolean =
    super.search(pathFinder, timeToRun)

  override def finalizeSearch(timeToRun: Long): Boolean = {
    hpfRequest.pathFound = pathFound
    if (pathFound) {
      // Take the first move of this plan and use it for the next run through
      endNode = resultPath.get(1)
    }
    true
  }
}
