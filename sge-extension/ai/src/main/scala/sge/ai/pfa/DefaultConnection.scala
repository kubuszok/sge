/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/DefaultConnection.java
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
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: DefaultConnection,fromNode,getCost,getFromNode,getToNode,toNode
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/DefaultConnection.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: DefaultConnection,fromNode,getCost,getFromNode,getToNode,toNode
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

/** A `DefaultConnection` is a [[Connection]] whose cost is 1.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class DefaultConnection[N](
  protected var fromNode: N,
  protected var toNode:   N
) extends Connection[N] {

  override def getCost: Float = 1f

  override def getFromNode: N = fromNode

  override def getToNode: N = toNode
}
