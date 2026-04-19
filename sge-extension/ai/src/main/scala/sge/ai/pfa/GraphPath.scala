/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/GraphPath.java
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
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: GraphPath,add,clear,get,getCount,reverse
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package pfa

/** A `GraphPath` represents a path in a [[Graph]]. Note that a path can be defined in terms of nodes or [[Connection connections]] so that multiple edges between the same pair of nodes can be
  * discriminated.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
trait GraphPath[N] extends Iterable[N] {

  /** Returns the number of items of this path. */
  def getCount: Int

  /** Returns the item of this path at the given index. */
  def get(index: Int): N

  /** Adds an item at the end of this path. */
  def add(node: N): Unit

  /** Clears this path. */
  def clear(): Unit

  /** Reverses this path. */
  def reverse(): Unit
}
