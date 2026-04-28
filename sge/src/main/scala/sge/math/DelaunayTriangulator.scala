/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/DelaunayTriangulator.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: computeTriangles(4), trim. Uses DynamicArray
 * instead of ShortArray/IntArray. Private helpers: quicksortPairs, end, circumCircle.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 367
 * Covenant-baseline-methods: COMPLETE,DelaunayTriangulator,EPSILON,INCOMPLETE,INSIDE,centroid,circumCircle,complete,computeTriangles,down,edges,i,lower,originalIndices,pointCount,quicksortPartition,quicksortStack,rsqr,sort,sortedPoints,stack,superTriangle,triangles,trim,up,upper,x,y
 * Covenant-source-reference: com/badlogic/gdx/math/DelaunayTriangulator.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package math

import sge.utils.{ DynamicArray, Nullable }

/** Delaunay triangulation. Adapted from Paul Bourke's triangulate: http://paulbourke.net/papers/triangulate/
  * @author
  *   Nathan Sweet (original implementation)
  */
class DelaunayTriangulator {
  private val quicksortStack = DynamicArray[Int]()
  private var sortedPoints: Array[Float] = scala.compiletime.uninitialized
  private val triangles       = DynamicArray[Short]()
  private val originalIndices = DynamicArray[Short]()
  private val edges           = DynamicArray[Int]()
  private val complete        = DynamicArray[Boolean]()
  private val superTriangle   = new Array[Float](6)
  private val centroid        = Vector2()

  /** @see #computeTriangles(float[], int, int, boolean) */
  def computeTriangles(points: Array[Float], sorted: Boolean): DynamicArray[Short] =
    computeTriangles(points, 0, points.length, sorted)

  /** Triangulates the given point cloud to a list of triangle indices that make up the Delaunay triangulation.
    * @param points
    *   x,y pairs describing points. Duplicate points will result in undefined behavior.
    * @param sorted
    *   If false, the points will be sorted by the x coordinate, which is required by the triangulation algorithm. If sorting is done the input array is not modified, the returned indices are for the
    *   input array, and count*2 additional working memory is needed.
    * @return
    *   triples of indices into the points that describe the triangles in clockwise order. Note the returned array is reused for later calls to the same method.
    */
  def computeTriangles(points: Array[Float], offset: Int, count: Int, sorted: Boolean): DynamicArray[Short] = {
    if (count > 32767) throw new IllegalArgumentException("count must be <= " + 32767)
    val triangles = this.triangles
    triangles.clear()
    if (count < 6) triangles
    else {
      triangles.ensureCapacity(count)

      var pointsArray = points
      var offsetVar   = offset
      if (!sorted) {
        if (Nullable(sortedPoints).forall(_.length < count)) sortedPoints = new Array[Float](count)
        Array.copy(points, offset, sortedPoints, 0, count)
        pointsArray = sortedPoints
        offsetVar = 0
        sort(pointsArray, count)
      }

      val end = offsetVar + count

      // Determine bounds for super triangle.
      var xmin = pointsArray(0)
      var ymin = pointsArray(1)
      var xmax = xmin
      var ymax = ymin
      var i    = offsetVar + 2
      while (i < end) {
        val value = pointsArray(i)
        if (value < xmin) xmin = value
        if (value > xmax) xmax = value
        i += 1
        val value2 = pointsArray(i)
        if (value2 < ymin) ymin = value2
        if (value2 > ymax) ymax = value2
        i += 1
      }
      val dx   = xmax - xmin
      val dy   = ymax - ymin
      val dmax = (if (dx > dy) dx else dy) * 20f
      val xmid = (xmax + xmin) / 2f
      val ymid = (ymax + ymin) / 2f

      // Setup the super triangle, which contains all points.
      val superTriangle = this.superTriangle
      superTriangle(0) = xmid - dmax
      superTriangle(1) = ymid - dmax
      superTriangle(2) = xmid
      superTriangle(3) = ymid + dmax
      superTriangle(4) = xmid + dmax
      superTriangle(5) = ymid - dmax

      val edges = this.edges
      edges.ensureCapacity(count / 2)

      val complete = this.complete
      complete.clear()
      complete.ensureCapacity(count)

      // Add super triangle.
      triangles += end.toShort
      triangles += (end + 2).toShort
      triangles += (end + 4).toShort
      complete += false

      // Include each point one at a time into the existing mesh.
      var pointIndex = offsetVar
      while (pointIndex < end) {
        val x = pointsArray(pointIndex)
        val y = pointsArray(pointIndex + 1)

        // If x,y lies inside the circumcircle of a triangle, the edges are stored and the triangle removed.
        var triangleIndex = triangles.size - 1
        while (triangleIndex >= 0) {
          val completeIndex = triangleIndex / 3
          if (!complete(completeIndex)) {
            val p1       = triangles(triangleIndex - 2)
            val p2       = triangles(triangleIndex - 1)
            val p3       = triangles(triangleIndex)
            val (x1, y1) = if (p1 >= end) {
              val i = p1 - end
              (superTriangle(i), superTriangle(i + 1))
            } else {
              (pointsArray(p1), pointsArray(p1 + 1))
            }
            val (x2, y2) = if (p2 >= end) {
              val i = p2 - end
              (superTriangle(i), superTriangle(i + 1))
            } else {
              (pointsArray(p2), pointsArray(p2 + 1))
            }
            val (x3, y3) = if (p3 >= end) {
              val i = p3 - end
              (superTriangle(i), superTriangle(i + 1))
            } else {
              (pointsArray(p3), pointsArray(p3 + 1))
            }
            circumCircle(x, y, x1, y1, x2, y2, x3, y3) match {
              case DelaunayTriangulator.COMPLETE =>
                complete(completeIndex) = true
              case DelaunayTriangulator.INSIDE =>
                edges += p1
                edges += p2
                edges += p2
                edges += p3
                edges += p3
                edges += p1

                triangles.removeRange(triangleIndex - 2, triangleIndex + 1)
                complete.removeIndex(completeIndex)
              case _ => // INCOMPLETE — no action needed
            }
          }
          triangleIndex -= 3
        }

        var i = 0
        val n = edges.size
        while (i < n) {
          // Skip multiple edges. If all triangles are anticlockwise then all interior edges are opposite pointing in direction.
          val p1 = edges(i)
          if (p1 != -1) {
            val p2   = edges(i + 1)
            var skip = false
            var ii   = i + 2
            while (ii < n) {
              if (p1 == edges(ii + 1) && p2 == edges(ii)) {
                skip = true
                edges(ii) = -1
              }
              ii += 2
            }
            if (!skip) {
              // Form new triangles for the current point. Edges are arranged in clockwise order.
              triangles += p1.toShort
              triangles += edges(i + 1).toShort
              triangles += pointIndex.toShort
              complete += false
            }
          }
          i += 2
        }
        edges.clear()
        pointIndex += 2
      }

      // Remove triangles with super triangle vertices.
      var idx = triangles.size - 1
      while (idx >= 0) {
        if (triangles(idx) >= end || triangles(idx - 1) >= end || triangles(idx - 2) >= end) {
          triangles.removeRange(idx - 2, idx + 1)
        }
        idx -= 3
      }

      // Convert sorted to unsorted indices.
      if (!sorted) {
        var i = 0
        val n = triangles.size
        while (i < n) {
          triangles(i) = (originalIndices(triangles(i) / 2) * 2).toShort
          i += 1
        }
      }

      // Adjust triangles to start from zero and count by 1, not by vertex x,y coordinate pairs.
      if (offsetVar == 0) {
        var i = 0
        val n = triangles.size
        while (i < n) {
          triangles(i) = (triangles(i) / 2).toShort
          i += 1
        }
      } else {
        var i = 0
        val n = triangles.size
        while (i < n) {
          triangles(i) = ((triangles(i) - offsetVar) / 2).toShort
          i += 1
        }
      }

      triangles
    }
  }

  /** Returns INSIDE if point xp,yp is inside the circumcircle made up of the points x1,y1, x2,y2, x3,y3. Returns COMPLETE if xp is to the right of the entire circumcircle. Otherwise returns
    * INCOMPLETE. Note: a point on the circumcircle edge is considered inside.
    */
  private def circumCircle(xp: Float, yp: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Int = {
    val (xc, yc, drsqr) = if (scala.math.abs(y1 - y2) < DelaunayTriangulator.EPSILON) {
      val m2    = -(x3 - x2) / (y3 - y2)
      val mx2   = (x2 + x3) / 2f
      val my2   = (y2 + y3) / 2f
      val xc    = (x1 + x2) / 2f
      val yc    = m2 * (xc - mx2) + my2
      val drsqr = (x2 - xc) * (x2 - xc) + (y2 - yc) * (y2 - yc)
      (xc, yc, drsqr)
    } else if (scala.math.abs(y2 - y3) < DelaunayTriangulator.EPSILON) {
      val m1    = -(x2 - x1) / (y2 - y1)
      val mx1   = (x1 + x2) / 2f
      val my1   = (y1 + y2) / 2f
      val xc    = (x2 + x3) / 2f
      val yc    = m1 * (xc - mx1) + my1
      val drsqr = (x2 - xc) * (x2 - xc) + (y2 - yc) * (y2 - yc)
      (xc, yc, drsqr)
    } else {
      val m1    = -(x2 - x1) / (y2 - y1)
      val m2    = -(x3 - x2) / (y3 - y2)
      val mx1   = (x1 + x2) / 2f
      val mx2   = (x2 + x3) / 2f
      val my1   = (y1 + y2) / 2f
      val my2   = (y2 + y3) / 2f
      val xc    = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2)
      val yc    = m1 * (xc - mx1) + my1
      val drsqr = (x2 - xc) * (x2 - xc) + (y2 - yc) * (y2 - yc)
      (xc, yc, drsqr)
    }

    val rsqr = (xp - xc) * (xp - xc) + (yp - yc) * (yp - yc)

    if (rsqr - drsqr <= DelaunayTriangulator.EPSILON) DelaunayTriangulator.INSIDE
    else if (xp > xc && rsqr > drsqr) DelaunayTriangulator.COMPLETE
    else DelaunayTriangulator.INCOMPLETE
  }

  private def sort(values: Array[Float], count: Int): Unit = {
    val pointCount = count / 2
    originalIndices.clear()
    originalIndices.ensureCapacity(pointCount)
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
      upper = stack.pop()
      lower = stack.pop()
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

  def trim(triangles: DynamicArray[Short], points: Array[Float], hull: Array[Float], offset: Int, count: Int): Unit = {
    var i = triangles.size - 1
    while (i >= 0) {
      val p1 = triangles(i - 2) * 2
      val p2 = triangles(i - 1) * 2
      val p3 = triangles(i) * 2
      GeometryUtils.triangleCentroid(points(p1), points(p1 + 1), points(p2), points(p2 + 1), points(p3), points(p3 + 1), centroid)
      if (!GeometryUtils.isPointInPolygon(hull, offset, count, centroid.x, centroid.y)) {
        triangles.removeRange(i - 2, i + 1)
      }
      i -= 3
    }
  }
}

object DelaunayTriangulator {
  private val EPSILON    = 0.000001f
  private val INSIDE     = 0
  private val COMPLETE   = 1
  private val INCOMPLETE = 2
}
