/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import sge.jbump.util.Nullable

/** Collision result. */
class Collision {
  var overlaps: Boolean = false
  var ti: Float = 0f
  val move: Point = Point()
  val normal: IntPoint = IntPoint()
  val touch: Point = Point()
  val itemRect: Rect = Rect()
  val otherRect: Rect = Rect()
  var item: Nullable[Item[?]] = Nullable.Null
  var other: Nullable[Item[?]] = Nullable.Null
  var `type`: Nullable[Response] = Nullable.Null

  def set(
      overlaps: Boolean,
      ti: Float,
      moveX: Float,
      moveY: Float,
      normalX: Int,
      normalY: Int,
      touchX: Float,
      touchY: Float,
      x1: Float,
      y1: Float,
      w1: Float,
      h1: Float,
      x2: Float,
      y2: Float,
      w2: Float,
      h2: Float
  ): Unit = {
    this.overlaps = overlaps
    this.ti = ti
    this.move.set(moveX, moveY)
    this.normal.set(normalX, normalY)
    this.touch.set(touchX, touchY)
    this.itemRect.set(x1, y1, w1, h1)
    this.otherRect.set(x2, y2, w2, h2)
  }
}
