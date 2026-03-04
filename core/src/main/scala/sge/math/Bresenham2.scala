/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Bresenham2.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: line(GridPoint2,GridPoint2), line(ints),
 * line(ints,Pool,DynamicArray). Uses DynamicArray instead of gdx Array.
 * Two-arg line returns Array[GridPoint2] via .toArray (Java returns Array<GridPoint2>).
 */
package sge
package math

import sge.utils.{ DynamicArray, Pool }

/** Returns a list of points at integer coordinates for a line on a 2D grid, using the Bresenham algorithm. <p>
  *
  * Instances of this class own the returned array of points and the points themselves to avoid garbage collection as much as possible. Calling any of the methods will result in the reuse of the
  * previously returned array and vectors, expect
  * @author
  *   badlogic (original implementation)
  */
final class Bresenham2 {

  private val points = DynamicArray[GridPoint2]()
  private val pool   = Pool.Default[GridPoint2](() => GridPoint2())

  /** Returns a list of {@link GridPoint2} instances along the given line, at integer coordinates.
    * @param start
    *   the start of the line
    * @param end
    *   the end of the line
    * @return
    *   the list of points on the line at integer coordinates
    */
  def line(start: GridPoint2, end: GridPoint2): Array[GridPoint2] =
    line(start.x, start.y, end.x, end.y)

  /** Returns a list of {@link GridPoint2} instances along the given line, at integer coordinates.
    * @param startX
    *   the start x coordinate of the line
    * @param startY
    *   the start y coordinate of the line
    * @param endX
    *   the end x coordinate of the line
    * @param endY
    *   the end y coordinate of the line
    * @return
    *   the list of points on the line at integer coordinates
    */
  def line(startX: Int, startY: Int, endX: Int, endY: Int): Array[GridPoint2] = {
    points.foreach(pool.free)
    points.clear()
    line(startX, startY, endX, endY, pool, points).toArray
  }

  /** Returns a list of {@link GridPoint2} instances along the given line, at integer coordinates.
    * @param startX
    *   the start x coordinate of the line
    * @param startY
    *   the start y coordinate of the line
    * @param endX
    *   the end x coordinate of the line
    * @param endY
    *   the end y coordinate of the line
    * @param pool
    *   the pool from which GridPoint2 instances are fetched
    * @param output
    *   the output array, will be cleared in this method
    * @return
    *   the list of points on the line at integer coordinates
    */
  def line(startX: Int, startY: Int, endX: Int, endY: Int, pool: Pool[GridPoint2], output: DynamicArray[GridPoint2]): DynamicArray[GridPoint2] = {
    val w   = endX - startX
    val h   = endY - startY
    var dx1 = 0
    var dy1 = 0
    var dx2 = 0
    var dy2 = 0

    if (w < 0) {
      dx1 = -1
      dx2 = -1
    } else if (w > 0) {
      dx1 = 1
      dx2 = 1
    }

    if (h < 0) {
      dy1 = -1
    } else if (h > 0) {
      dy1 = 1
    }

    var longest  = scala.math.abs(w)
    var shortest = scala.math.abs(h)

    if (longest < shortest) {
      longest = scala.math.abs(h)
      shortest = scala.math.abs(w)
      if (h < 0) {
        dy2 = -1
      } else if (h > 0) {
        dy2 = 1
      }
      dx2 = 0
    }

    val shortest2 = shortest << 1
    val longest2  = longest << 1
    var numerator = 0
    var currentX  = startX
    var currentY  = startY

    for (_ <- 0 to longest) {
      val point = pool.obtain()
      point.set(currentX, currentY)
      output += point
      numerator += shortest2
      if (numerator > longest) {
        numerator -= longest2
        currentX += dx1
        currentY += dy1
      } else {
        currentX += dx2
        currentY += dy2
      }
    }

    output
  }
}
