/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import sge.jbump.util.Nullable

/**
 * Item wrapper with identity-based hashing.
 *
 * The `userData` field is not ever read by JBump, so this can be anything user code needs it to be.
 * It is not considered by `equals` or `hashCode`.
 */
class Item[E](var userData: Nullable[E]) {

  /**
   * Constructs an Item with no userData.
   */
  def this() = this(Nullable.Null)

  protected val identityHash: Int = System.identityHashCode(this)

  override def equals(o: Any): Boolean = this eq o.asInstanceOf[AnyRef]

  override def hashCode(): Int = identityHash
}
