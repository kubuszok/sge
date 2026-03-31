/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

/** A 2D point with float x and y. */
class Point(var x: Float, var y: Float) {

  def this() = this(0f, 0f)

  def set(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y
  }

  override def equals(o: Any): Boolean =
    o match {
      case that: Point =>
        (this eq that) ||
        (java.lang.Float.compare(that.x, x) == 0 && java.lang.Float.compare(that.y, y) == 0)
      case _ => false
    }

  override def hashCode(): Int =
    (java.lang.Float.floatToIntBits(x) * 0xc13fa9a902a6328fL +
      java.lang.Float.floatToIntBits(y) * 0x91e10da5c79e7b1dL >>> 32).toInt

  override def toString: String = s"($x, $y)"
}
