/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/DefaultGraphPath.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa

import sge.utils.DynamicArray

/** Default implementation of a [[GraphPath]] that uses an internal [[DynamicArray]] to store nodes or connections.
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class DefaultGraphPath[N](val nodes: DynamicArray[N]) extends GraphPath[N] {

  override def clear(): Unit = nodes.clear()

  override def getCount: Int = nodes.size

  override def add(node: N): Unit = nodes.add(node)

  override def get(index: Int): N = nodes(index)

  override def reverse(): Unit = nodes.reverse()

  override def iterator: Iterator[N] = nodes.iterator
}

object DefaultGraphPath {

  /** Creates a `DefaultGraphPath` with no nodes. */
  inline def apply[N](): DefaultGraphPath[N] = new DefaultGraphPath[N](DynamicArray[N]())

  /** Creates a `DefaultGraphPath` with the given capacity and no nodes. */
  inline def apply[N](capacity: Int): DefaultGraphPath[N] = new DefaultGraphPath[N](DynamicArray[N](capacity))
}
