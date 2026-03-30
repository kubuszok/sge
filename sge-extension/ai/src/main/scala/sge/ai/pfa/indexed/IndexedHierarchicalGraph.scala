/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/indexed/IndexedHierarchicalGraph.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa.indexed` -> `sge.ai.pfa.indexed`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa
package indexed

/** A hierarchical graph for the [[IndexedAStarPathFinder]].
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class IndexedHierarchicalGraph[N](
  protected var levelCount: Int
) extends IndexedGraph[N]
    with HierarchicalGraph[N] {

  protected var level: Int = 0

  /** Creates an `IndexedHierarchicalGraph` with the given number of levels. */

  override def getLevelCount: Int = levelCount

  override def setLevel(level: Int): Unit =
    this.level = level

  override def convertNodeBetweenLevels(inputLevel: Int, node: N, outputLevel: Int): N
}
