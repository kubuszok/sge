/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: IntPoint,equals,hashCode,set,this,toString
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

/** A 2D point with integer x and y. */
class IntPoint(var x: Int, var y: Int) {

  def this() = this(0, 0)

  def set(x: Int, y: Int): Unit = {
    this.x = x
    this.y = y
  }

  override def equals(o: Any): Boolean =
    o match {
      case that: IntPoint =>
        (this eq that) || (x == that.x && y == that.y)
      case _ => false
    }

  override def hashCode(): Int =
    // Rosenberg-Strong pairing function, only works for non-negative coordinates
    if (x >= y) x * (x + 2) - y else y * y + x

  override def toString: String = s"($x, $y)"
}
