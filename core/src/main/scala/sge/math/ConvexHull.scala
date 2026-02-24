/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/ConvexHull.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

import scala.collection.mutable.ArrayBuffer

/** Computes the convex hull of a set of points using the monotone chain convex hull algorithm (aka Andrew's algorithm).
  * @author
  *   Nathan Sweet (original implementation)
  */
class ConvexHull {
  private val quicksortStack = ArrayBuffer[Int]()
  private var sortedPoints: Array[Float] = scala.compiletime.uninitialized
  private val hull            = ArrayBuffer[Float]()
  private val indices         = ArrayBuffer[Int]()
  private val originalIndices = ArrayBuffer[Short]()

  /** @see #computePolygon(float[], int, int, boolean) */
  def computePolygon(points: Array[Float], sorted: Boolean): ArrayBuffer[Float] =
    computePolygon(points, 0, points.length, sorted)

  /** Returns a list of points on the convex hull in counter-clockwise order. Note: the last point in the returned list is the same as the first one.
    */
  /** Returns the convex hull polygon for the given point cloud.
    * @param points
    *   x,y pairs describing points in counter-clockwise order. Duplicate points will result in undefined behavior.
    * @param sorted
    *   If false, the points will be sorted by the x coordinate then the y coordinate, which is required by the convex hull algorithm. If sorting is done the input array is not modified and count
    *   additional working memory is needed.
    * @return
    *   pairs of coordinates that describe the convex hull polygon in counterclockwise order. Note the returned array is reused for later calls to the same method.
    */
  def computePolygon(points: Array[Float], offset: Int, count: Int, sorted: Boolean): ArrayBuffer[Float] = {
    var pointsArray = points
    var offsetVar   = offset
    var end         = offset + count

    if (!sorted) {
      if (sortedPoints == null || sortedPoints.length < count) sortedPoints = new Array[Float](count)
      Array.copy(points, offset, sortedPoints, 0, count)
      pointsArray = sortedPoints
      offsetVar = 0
      end = count
      sort(pointsArray, count)
    }

    val hull = this.hull
    hull.clear()

    // Lower hull.
    var i = offsetVar
    while (i < end) {
      val x = pointsArray(i)
      val y = pointsArray(i + 1)
      while (hull.size >= 4 && ccw(x, y) <= 0)
        hull.dropRightInPlace(2)
      hull += x
      hull += y
      i += 2
    }

    // Upper hull.
    i = end - 4
    val t = hull.size + 2
    while (i >= offsetVar) {
      val x = pointsArray(i)
      val y = pointsArray(i + 1)
      while (hull.size >= t && ccw(x, y) <= 0)
        hull.dropRightInPlace(2)
      hull += x
      hull += y
      i -= 2
    }

    hull
  }

  /** @see #computeIndices(float[], int, int, boolean, boolean) */
  def computeIndices(points: Array[Float], sorted: Boolean, yDown: Boolean): ArrayBuffer[Int] =
    computeIndices(points, 0, points.length, sorted, yDown)

  /** Computes a hull the same as {@link #computePolygon(float[], int, int, boolean)} but returns indices of the specified points.
    */
  def computeIndices(points: Array[Float], offset: Int, count: Int, sorted: Boolean, yDown: Boolean): ArrayBuffer[Int] = {
    if (count > 32767) throw new IllegalArgumentException("count must be <= " + 32767)
    var pointsArray = points
    var offsetVar   = offset
    var end         = offset + count

    if (!sorted) {
      if (sortedPoints == null || sortedPoints.length < count) sortedPoints = new Array[Float](count)
      Array.copy(points, offset, sortedPoints, 0, count)
      pointsArray = sortedPoints
      offsetVar = 0
      end = count
      sortWithIndices(pointsArray, count, yDown)
    }

    val indices = this.indices
    indices.clear()

    val hull = this.hull
    hull.clear()

    // Lower hull.
    var i     = offsetVar
    var index = i / 2
    while (i < end) {
      val x = pointsArray(i)
      val y = pointsArray(i + 1)
      while (hull.size >= 4 && ccw(x, y) <= 0) {
        hull.dropRightInPlace(2)
        indices.dropRightInPlace(1)
      }
      hull += x
      hull += y
      indices += index
      i += 2
      index += 1
    }

    // Upper hull.
    i = end - 4
    index = i / 2
    val t = hull.size + 2
    while (i >= offsetVar) {
      val x = pointsArray(i)
      val y = pointsArray(i + 1)
      while (hull.size >= t && ccw(x, y) <= 0) {
        hull.dropRightInPlace(2)
        indices.dropRightInPlace(1)
      }
      hull += x
      hull += y
      indices += index
      i -= 2
      index -= 1
    }

    // Convert sorted to unsorted indices.
    if (!sorted) {
      var i = 0
      val n = indices.size
      while (i < n) {
        indices(i) = originalIndices(indices(i))
        i += 1
      }
    }

    indices
  }

  /** Returns > 0 if the points are a counterclockwise turn, < 0 if clockwise, and 0 if colinear. */
  private def ccw(p3x: Float, p3y: Float): Float = {
    val hull = this.hull
    val size = hull.size
    val p1x  = hull(size - 4)
    val p1y  = hull(size - 3)
    val p2x  = hull(size - 2)
    val p2y  = hull(size - 1)
    (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x)
  }

  /** Sorts x,y pairs of values by the x value, then the y value.
    * @param count
    *   Number of indices, must be even.
    */
  private def sort(values: Array[Float], count: Int): Unit = {
    val pointCount = count / 2
    originalIndices.clear()
    originalIndices.sizeHint(pointCount)
    var i: Short = 0
    while (i < pointCount) {
      originalIndices += i
      i = (i + 1).toShort
    }

    var lower = 0
    var upper = count - 1
    val stack = quicksortStack
    stack += lower
    stack += upper - 1
    while (stack.nonEmpty) {
      upper = stack.remove(stack.size - 1)
      lower = stack.remove(stack.size - 1)
      if (upper <= lower) {
        // continue
      } else {
        val i = quicksortPartition(values, lower, upper, originalIndices.toArray)
        if (i - lower > upper - i) {
          stack += lower
          stack += i - 2
        }
        stack += i + 2
        stack += upper
        if (upper - i >= i - lower) {
          stack += lower
          stack += i - 2
        }
      }
    }
  }

  private def quicksortPartition(values: Array[Float], lower: Int, upper: Int, originalIndices: Array[Short]): Int = {
    val x    = values(lower)
    val y    = values(lower + 1)
    var up   = upper
    var down = lower
    while (down < up) {
      while (down < up && values(down) <= x)
        down = down + 2
      while (values(up) > x || (values(up) == x && values(up + 1) < y))
        up = up - 2
      if (down < up) {
        val temp = values(down)
        values(down) = values(up)
        values(up) = temp

        val temp2 = values(down + 1)
        values(down + 1) = values(up + 1)
        values(up + 1) = temp2

        val tempIndex = originalIndices(down / 2)
        originalIndices(down / 2) = originalIndices(up / 2)
        originalIndices(up / 2) = tempIndex
      }
    }
    if (x > values(up) || (x == values(up) && y < values(up + 1))) {
      values(lower) = values(up)
      values(up) = x

      values(lower + 1) = values(up + 1)
      values(up + 1) = y

      val tempIndex = originalIndices(lower / 2)
      originalIndices(lower / 2) = originalIndices(up / 2)
      originalIndices(up / 2) = tempIndex
    }
    up
  }

  /** Sorts x,y pairs of values by the x value, then the y value and stores unsorted original indices.
    * @param count
    *   Number of indices, must be even.
    */
  private def sortWithIndices(values: Array[Float], count: Int, yDown: Boolean): Unit = {
    val pointCount = count / 2
    originalIndices.clear()
    originalIndices.sizeHint(pointCount)
    var i: Short = 0
    while (i < pointCount) {
      originalIndices += i
      i = (i + 1).toShort
    }

    var lower = 0
    var upper = count - 1
    val stack = quicksortStack
    stack += lower
    stack += upper - 1
    while (stack.nonEmpty) {
      upper = stack.remove(stack.size - 1)
      lower = stack.remove(stack.size - 1)
      if (upper <= lower) {
        // continue
      } else {
        val i = quicksortPartitionWithIndices(values, lower, upper, originalIndices.toArray, yDown)
        if (i - lower > upper - i) {
          stack += lower
          stack += i - 2
        }
        stack += i + 2
        stack += upper
        if (upper - i >= i - lower) {
          stack += lower
          stack += i - 2
        }
      }
    }
  }

  private def quicksortPartitionWithIndices(values: Array[Float], lower: Int, upper: Int, originalIndices: Array[Short], yDown: Boolean): Int = {
    val x    = values(lower)
    val y    = values(lower + 1)
    var up   = upper
    var down = lower
    while (down < up) {
      while (down < up && (values(down) < x || (values(down) == x && (if (yDown) values(down + 1) < y else values(down + 1) > y))))
        down = down + 2
      while (values(up) > x || (values(up) == x && (if (yDown) values(up + 1) > y else values(up + 1) < y)))
        up = up - 2
      if (down < up) {
        val temp = values(down)
        values(down) = values(up)
        values(up) = temp

        val temp2 = values(down + 1)
        values(down + 1) = values(up + 1)
        values(up + 1) = temp2

        val tempIndex = originalIndices(down / 2)
        originalIndices(down / 2) = originalIndices(up / 2)
        originalIndices(up / 2) = tempIndex
      }
    }
    if (x > values(up) || (x == values(up) && (if (yDown) y > values(up + 1) else y < values(up + 1)))) {
      values(lower) = values(up)
      values(up) = x

      values(lower + 1) = values(up + 1)
      values(up + 1) = y

      val tempIndex = originalIndices(lower / 2)
      originalIndices(lower / 2) = originalIndices(up / 2)
      originalIndices(up / 2) = tempIndex
    }
    up
  }
}
