/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.{ MathUtils, Nullable }

/** Rectangle collision helper -- detects collisions between two AABBs. */
class RectHelper {

  private val rect_detectCollision_diff                              = Rect()
  private val rect_detectCollision_nearestCorner                     = Point()
  private val rect_detectCollision_getSegmentIntersectionIndices_ti  = Point()
  private val rect_detectCollision_getSegmentIntersectionIndices_n1  = IntPoint()
  private val rect_detectCollision_getSegmentIntersectionIndices_n2  = IntPoint()
  private val rect_detectCollision_getSegmentIntersectionIndices_col = Collision()

  def rect_detectCollision(
    x1:    Float,
    y1:    Float,
    w1:    Float,
    h1:    Float,
    x2:    Float,
    y2:    Float,
    w2:    Float,
    h2:    Float,
    goalX: Float,
    goalY: Float
  ): Nullable[Collision] = {
    import scala.util.boundary, boundary.break

    val col = rect_detectCollision_getSegmentIntersectionIndices_col
    val dx  = goalX - x1
    val dy  = goalY - y1

    Rect.rect_getDiff(x1, y1, w1, h1, x2, y2, w2, h2, rect_detectCollision_diff)
    val x = rect_detectCollision_diff.x
    val y = rect_detectCollision_diff.y
    val w = rect_detectCollision_diff.w
    val h = rect_detectCollision_diff.h

    var overlaps = false
    var hasTi    = false
    var ti       = 0f
    var nx       = 0
    var ny       = 0

    if (Rect.rect_containsPoint(x, y, w, h, 0, 0)) {
      // item was intersecting other
      Rect.rect_getNearestCorner(x, y, w, h, 0, 0, rect_detectCollision_nearestCorner)
      val px = rect_detectCollision_nearestCorner.x
      val py = rect_detectCollision_nearestCorner.y

      // area of intersection
      val wi = Math.min(w1, Math.abs(px))
      val hi = Math.min(h1, Math.abs(py))
      ti = -wi * hi // ti is the negative area of intersection
      hasTi = true
      overlaps = true
    } else {
      val intersect = Rect.rect_getSegmentIntersectionIndices(
        x,
        y,
        w,
        h,
        0,
        0,
        dx,
        dy,
        -Float.MaxValue,
        Float.MaxValue,
        rect_detectCollision_getSegmentIntersectionIndices_ti,
        rect_detectCollision_getSegmentIntersectionIndices_n1,
        rect_detectCollision_getSegmentIntersectionIndices_n2
      )
      val ti1 = rect_detectCollision_getSegmentIntersectionIndices_ti.x
      val ti2 = rect_detectCollision_getSegmentIntersectionIndices_ti.y
      val nx1 = rect_detectCollision_getSegmentIntersectionIndices_n1.x
      val ny1 = rect_detectCollision_getSegmentIntersectionIndices_n1.y

      // item tunnels into other
      if (
        intersect && ti1 < 1 && Math.abs(ti1 - ti2) >= MathUtils.DELTA // special case for rect going through another rect's corner
        && (0 < ti1 + MathUtils.DELTA || (0 == ti1 && ti2 > 0))
      ) {
        ti = ti1
        nx = nx1
        ny = ny1
        hasTi = true
        overlaps = false
      }
    }

    if (!hasTi) {
      Nullable.Null
    } else {
      boundary[Nullable[Collision]] {
        var tx = 0f
        var ty = 0f

        if (overlaps) {
          if (dx == 0 && dy == 0) {
            // intersecting and not moving - use minimum displacement vector
            Rect.rect_getNearestCorner(x, y, w, h, 0, 0, rect_detectCollision_nearestCorner)
            var px = rect_detectCollision_nearestCorner.x
            var py = rect_detectCollision_nearestCorner.y
            if (Math.abs(px) < Math.abs(py)) {
              py = 0
            } else {
              px = 0
            }
            nx = MathUtils.sign(px)
            ny = MathUtils.sign(py)
            tx = x1 + px
            ty = y1 + py
          } else {
            // intersecting and moving - move in the opposite direction
            val intersect = Rect.rect_getSegmentIntersectionIndices(
              x,
              y,
              w,
              h,
              0,
              0,
              dx,
              dy,
              -Float.MaxValue,
              1,
              rect_detectCollision_getSegmentIntersectionIndices_ti,
              rect_detectCollision_getSegmentIntersectionIndices_n1,
              rect_detectCollision_getSegmentIntersectionIndices_n2
            )
            val ti1 = rect_detectCollision_getSegmentIntersectionIndices_ti.x
            nx = rect_detectCollision_getSegmentIntersectionIndices_n1.x
            ny = rect_detectCollision_getSegmentIntersectionIndices_n1.y
            if (!intersect) {
              break(Nullable.Null)
            }
            tx = x1 + dx * ti1
            ty = y1 + dy * ti1
          }
        } else {
          // tunnel
          tx = x1 + dx * ti
          ty = y1 + dy * ti
        }

        col.set(overlaps, ti, dx, dy, nx, ny, tx, ty, x1, y1, w1, h1, x2, y2, w2, h2)
        col
      }
    }
  }
}
