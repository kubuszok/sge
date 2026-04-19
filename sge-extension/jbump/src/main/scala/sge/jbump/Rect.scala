/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 159
 * Covenant-baseline-methods: Rect,dx,dy,nx,nx1,nx2,ny,ny1,ny2,p,q,r,rect_containsPoint,rect_getDiff,rect_getNearestCorner,rect_getSegmentIntersectionIndices,rect_getSquareDistance,rect_isIntersecting,set,this,ti1,ti2
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

import sge.jbump.util.MathUtils.{ DELTA, nearest }

/** AABB rectangle (x, y, w, h) with static utility methods. */
class Rect(var x: Float, var y: Float, var w: Float, var h: Float) {

  def this() = this(0f, 0f, 0f, 0f)

  def set(x: Float, y: Float, w: Float, h: Float): Unit = {
    this.x = x
    this.y = y
    this.w = w
    this.h = h
  }
}

object Rect {

  def rect_getNearestCorner(x: Float, y: Float, w: Float, h: Float, px: Float, py: Float, result: Point): Unit =
    result.set(nearest(px, x, x + w), nearest(y, y, y + h))

  /** This is a generalized implementation of the liang-barsky algorithm, which also returns the normals of the sides where the segment intersects. Notice that normals are only guaranteed to be
    * accurate when initially ti1 == -Float.MaxValue, ti2 == Float.MaxValue
    * @return
    *   false if the segment never touches the rect
    */
  def rect_getSegmentIntersectionIndices(
    x:     Float,
    y:     Float,
    w:     Float,
    h:     Float,
    x1:    Float,
    y1:    Float,
    x2:    Float,
    y2:    Float,
    ti1In: Float,
    ti2In: Float,
    ti:    Point,
    n1:    IntPoint,
    n2:    IntPoint
  ): Boolean = {
    import scala.util.boundary, boundary.break

    val dx = x2 - x1
    val dy = y2 - y1

    var nx  = 0
    var ny  = 0
    var nx1 = 0
    var ny1 = 0
    var nx2 = 0
    var ny2 = 0
    var p   = 0f
    var q   = 0f
    var r   = 0f
    var ti1 = ti1In
    var ti2 = ti2In

    boundary {
      var side = 1
      while (side <= 4) {
        side match {
          case 1 => // left
            nx = -1; ny = 0; p = -dx; q = x1 - x
          case 2 => // right
            nx = 1; ny = 0; p = dx; q = x + w - x1
          case 3 => // top
            nx = 0; ny = -1; p = -dy; q = y1 - y
          case _ => // bottom
            nx = 0; ny = 1; p = dy; q = y + h - y1
        }

        if (p == 0) {
          if (q <= 0) {
            break(false)
          }
        } else {
          r = q / p
          if (p < 0) {
            if (r > ti2) {
              break(false)
            } else if (r > ti1) {
              ti1 = r
              nx1 = nx
              ny1 = ny
            }
          } else {
            if (r < ti1) {
              break(false)
            } else if (r < ti2) {
              ti2 = r
              nx2 = nx
              ny2 = ny
            }
          }
        }
        side += 1
      }
      ti.set(ti1, ti2)
      n1.set(nx1, ny1)
      n2.set(nx2, ny2)
      true
    }
  }

  /** Calculates the minkowsky difference between 2 rects, which is another rect. */
  def rect_getDiff(
    x1:     Float,
    y1:     Float,
    w1:     Float,
    h1:     Float,
    x2:     Float,
    y2:     Float,
    w2:     Float,
    h2:     Float,
    result: Rect
  ): Unit =
    result.set(x2 - x1 - w1, y2 - y1 - h1, w1 + w2, h1 + h2)

  def rect_containsPoint(x: Float, y: Float, w: Float, h: Float, px: Float, py: Float): Boolean =
    px - x > DELTA && py - y > DELTA &&
      x + w - px > DELTA && y + h - py > DELTA

  def rect_isIntersecting(
    x1: Float,
    y1: Float,
    w1: Float,
    h1: Float,
    x2: Float,
    y2: Float,
    w2: Float,
    h2: Float
  ): Boolean =
    x1 < x2 + w2 && x2 < x1 + w1 &&
      y1 < y2 + h2 && y2 < y1 + h1

  def rect_getSquareDistance(
    x1: Float,
    y1: Float,
    w1: Float,
    h1: Float,
    x2: Float,
    y2: Float,
    w2: Float,
    h2: Float
  ): Float = {
    val dx = x1 - x2 + (w1 - w2) / 2
    val dy = y1 - y2 + (h1 - h2) / 2
    dx * dx + dy * dy
  }
}
