/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/Connection.java
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
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: Connection,getCost,getFromNode,getToNode
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/Connection.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: Connection,getCost,getFromNode,getToNode
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

/** A connection between two nodes of the [[Graph]]. The connection has a non-negative cost that often represents time or distance. However, the cost can be anything you want, for instance a
  * combination of time, distance, and other factors.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait Connection[N] {

  /** Returns the non-negative cost of this connection */
  def getCost: Float

  /** Returns the node that this connection came from */
  def getFromNode: N

  /** Returns the node that this connection leads to */
  def getToNode: N
}
