/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/HierarchicalGraph.java
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
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: HierarchicalGraph,convertNodeBetweenLevels,getLevelCount,setLevel
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/HierarchicalGraph.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: HierarchicalGraph,convertNodeBetweenLevels,getLevelCount,setLevel
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

/** A `HierarchicalGraph` is a multilevel graph that can be traversed by a [[HierarchicalPathFinder]] at any level of its hierarchy.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait HierarchicalGraph[N] extends Graph[N] {

  /** Returns the number of levels in this hierarchical graph. */
  def getLevelCount: Int

  /** Switches the graph into the given level so all future calls to the [[Graph.getConnections getConnections]] methods act as if the graph was just a simple, non-hierarchical graph at that level.
    * @param level
    *   the level to set
    */
  def setLevel(level: Int): Unit

  /** Converts the node at the input level into a node at the output level.
    * @param inputLevel
    *   the input level
    * @param node
    *   the node at the input level
    * @param outputLevel
    *   the output level
    * @return
    *   the node at the output level.
    */
  def convertNodeBetweenLevels(inputLevel: Int, node: N, outputLevel: Int): N
}
