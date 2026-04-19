/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/Graph.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: Graph,getConnections
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package pfa

import sge.utils.DynamicArray

/** A graph is a collection of nodes, each one having a collection of outgoing [[Connection connections]].
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait Graph[N] {

  /** Returns the connections outgoing from the given node.
    * @param fromNode
    *   the node whose outgoing connections will be returned
    * @return
    *   the array of connections outgoing from the given node.
    */
  def getConnections(fromNode: N): DynamicArray[Connection[N]]
}
