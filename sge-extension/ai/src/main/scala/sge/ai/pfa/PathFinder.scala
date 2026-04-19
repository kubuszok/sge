/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathFinder.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: PathFinder,search,searchConnectionPath,searchNodePath
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package pfa

/** A `PathFinder` that can find a [[GraphPath path]] from one node in an arbitrary [[Graph graph]] to a goal node based on information provided by that graph.
  *
  * A fully implemented path finder can perform both interruptible and non-interruptible searches. If a specific path finder is not able to perform one of the two type of search then the corresponding
  * method should throw an `UnsupportedOperationException`.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait PathFinder[N] {

  /** Performs a non-interruptible search, trying to find a path made up of connections from the start node to the goal node attempting to honor costs provided by the graph.
    *
    * @param startNode
    *   the start node
    * @param endNode
    *   the end node
    * @param heuristic
    *   the heuristic function
    * @param outPath
    *   the output path that will only be filled if a path is found, otherwise it won't get touched.
    * @return
    *   `true` if a path was found; `false` otherwise.
    */
  def searchConnectionPath(startNode: N, endNode: N, heuristic: Heuristic[N], outPath: GraphPath[Connection[N]]): Boolean

  /** Performs a non-interruptible search, trying to find a path made up of nodes from the start node to the goal node attempting to honor costs provided by the graph.
    *
    * @param startNode
    *   the start node
    * @param endNode
    *   the end node
    * @param heuristic
    *   the heuristic function
    * @param outPath
    *   the output path that will only be filled if a path is found, otherwise it won't get touched.
    * @return
    *   `true` if a path was found; `false` otherwise.
    */
  def searchNodePath(startNode: N, endNode: N, heuristic: Heuristic[N], outPath: GraphPath[N]): Boolean

  /** Performs an interruptible search, trying to find a path made up of nodes from the start node to the goal node attempting to honor costs provided by the graph.
    *
    * @param request
    *   the pathfinding request
    * @param timeToRun
    *   the time in nanoseconds that can be used to advance the search
    * @return
    *   `true` if the search has finished; `false` otherwise.
    */
  def search(request: PathFinderRequest[N], timeToRun: Long): Boolean
}
