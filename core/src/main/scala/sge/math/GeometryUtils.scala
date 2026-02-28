/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/GeometryUtils.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** @author Nathan Sweet (original implementation) */
object GeometryUtils {

  private val tmp1 = Vector2()
  private val tmp2 = Vector2()
  private val tmp3 = Vector2()

  /** Computes the barycentric coordinates v,w for the specified point in the triangle. <p> If barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1 then the point is inside
    * the triangle. <p> If vertices a,b,c have values aa,bb,cc then to get an interpolated value at point p:
    *
    * {{{
    * GeometryUtils.toBarycoord(p, a, b, c, barycentric);
    * // THEN:
    * float u = 1f - barycentric.x - barycentric.y;
    * float x = u * aa.x + barycentric.x * bb.x + barycentric.y * cc.x;
    * float y = u * aa.y + barycentric.x * bb.y + barycentric.y * cc.y;
    * // OR:
    * GeometryUtils.fromBarycoord(barycentric, aa, bb, cc, out);
    * }}}
    *
    * @return
    *   barycentricOut
    */
  def toBarycoord(p: Vector2, a: Vector2, b: Vector2, c: Vector2, barycentricOut: Vector2): Vector2 = {
    val v0    = tmp1.set(b).-(a)
    val v1    = tmp2.set(c).-(a)
    val v2    = tmp3.set(p).-(a)
    val d00   = v0.dot(v0)
    val d01   = v0.dot(v1)
    val d11   = v1.dot(v1)
    val d20   = v2.dot(v0)
    val d21   = v2.dot(v1)
    val denom = d00 * d11 - d01 * d01
    barycentricOut.x = (d11 * d20 - d01 * d21) / denom
    barycentricOut.y = (d00 * d21 - d01 * d20) / denom
    barycentricOut
  }

  /** Returns true if the barycentric coordinates are inside the triangle. */
  def barycoordInsideTriangle(barycentric: Vector2): Boolean =
    barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1

  /** Returns interpolated values given the barycentric coordinates of a point in a triangle and the values at each vertex.
    * @return
    *   interpolatedOut
    */
  def fromBarycoord(barycentric: Vector2, a: Vector2, b: Vector2, c: Vector2, interpolatedOut: Vector2): Vector2 = {
    val u = 1 - barycentric.x - barycentric.y
    interpolatedOut.x = u * a.x + barycentric.x * b.x + barycentric.y * c.x
    interpolatedOut.y = u * a.y + barycentric.x * b.y + barycentric.y * c.y
    interpolatedOut
  }

  /** Returns an interpolated value given the barycentric coordinates of a point in a triangle and the values at each vertex.
    * @return
    *   interpolatedOut
    */
  def fromBarycoord(barycentric: Vector2, a: Float, b: Float, c: Float): Float = {
    val u = 1 - barycentric.x - barycentric.y
    u * a + barycentric.x * b + barycentric.y * c
  }

  /** Returns the lowest positive root of the quadric equation given by a * x * x + b * x + c = 0. If no solution is given, Float.NaN is returned.
    * @param a
    *   the first coefficient of the quadric equation
    * @param b
    *   the second coefficient of the quadric equation
    * @param c
    *   the third coefficient of the quadric equation
    * @return
    *   the lowest positive root or Float.Nan
    */
  def lowestPositiveRoot(a: Float, b: Float, c: Float): Float = {
    val det = b * b - 4 * a * c
    if (det < 0) Float.NaN
    else {
      val sqrtD = scala.math.sqrt(det).toFloat
      val invA  = 1 / (2 * a)
      var r1    = (-b - sqrtD) * invA
      var r2    = (-b + sqrtD) * invA

      if (r1 > r2) {
        val tmp = r2
        r2 = r1
        r1 = tmp
      }

      if (r1 > 0) r1
      else if (r2 > 0) r2
      else Float.NaN
    }
  }

  def colinear(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Boolean = {
    val dx21 = x2 - x1
    val dy21 = y2 - y1
    val dx32 = x3 - x2
    val dy32 = y3 - y2
    val det  = dx32 * dy21 - dx21 * dy32
    scala.math.abs(det) < MathUtils.FLOAT_ROUNDING_ERROR
  }

  def triangleCentroid(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, centroid: Vector2): Vector2 = {
    centroid.x = (x1 + x2 + x3) / 3
    centroid.y = (y1 + y2 + y3) / 3
    centroid
  }

  /** Returns the circumcenter of the triangle. The input points must not be colinear. */
  def triangleCircumcenter(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, circumcenter: Vector2): Vector2 = {
    val dx21 = x2 - x1
    val dy21 = y2 - y1
    val dx32 = x3 - x2
    val dy32 = y3 - y2
    val dx13 = x1 - x3
    val dy13 = y1 - y3
    var det  = dx32 * dy21 - dx21 * dy32
    if (scala.math.abs(det) < MathUtils.FLOAT_ROUNDING_ERROR)
      throw new IllegalArgumentException("Triangle points must not be colinear.")
    det *= 2
    val sqr1 = x1 * x1 + y1 * y1
    val sqr2 = x2 * x2 + y2 * y2
    val sqr3 = x3 * x3 + y3 * y3
    circumcenter.set((sqr1 * dy32 + sqr2 * dy13 + sqr3 * dy21) / det, -(sqr1 * dx32 + sqr2 * dx13 + sqr3 * dx21) / det)
    circumcenter
  }

  def triangleCircumradius(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float = {
    var m1  = 0f
    var m2  = 0f
    var mx1 = 0f
    var mx2 = 0f
    var my1 = 0f
    var my2 = 0f
    var x   = 0f
    var y   = 0f
    if (scala.math.abs(y2 - y1) < MathUtils.FLOAT_ROUNDING_ERROR) {
      m2 = -(x3 - x2) / (y3 - y2)
      mx2 = (x2 + x3) / 2
      my2 = (y2 + y3) / 2
      x = (x2 + x1) / 2
      y = m2 * (x - mx2) + my2
    } else if (scala.math.abs(y3 - y2) < MathUtils.FLOAT_ROUNDING_ERROR) {
      m1 = -(x2 - x1) / (y2 - y1)
      mx1 = (x1 + x2) / 2
      my1 = (y1 + y2) / 2
      x = (x3 + x2) / 2
      y = m1 * (x - mx1) + my1
    } else {
      m1 = -(x2 - x1) / (y2 - y1)
      m2 = -(x3 - x2) / (y3 - y2)
      mx1 = (x1 + x2) / 2
      mx2 = (x2 + x3) / 2
      my1 = (y1 + y2) / 2
      my2 = (y2 + y3) / 2
      x = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2)
      y = m1 * (x - mx1) + my1
    }
    val dx = x1 - x
    val dy = y1 - y
    scala.math.sqrt(dx * dx + dy * dy).toFloat
  }

  /** Ratio of circumradius to shortest edge as a measure of triangle quality. <p> Gary L. Miller, Dafna Talmor, Shang-Hua Teng, and Noel Walkington. A Delaunay Based Numerical Method for Three
    * Dimensions: Generation, Formulation, and Partition.
    */
  def triangleQuality(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float = {
    val sqLength1 = x1 * x1 + y1 * y1
    val sqLength2 = x2 * x2 + y2 * y2
    val sqLength3 = x3 * x3 + y3 * y3
    scala.math.sqrt(scala.math.min(sqLength1, scala.math.min(sqLength2, sqLength3))).toFloat / triangleCircumradius(x1, y1, x2, y2, x3, y3)
  }

  def triangleArea(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float =
    scala.math.abs((x1 - x3) * (y2 - y1) - (x1 - x2) * (y3 - y1)).toFloat * 0.5f

  def quadrilateralCentroid(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, centroid: Vector2): Vector2 = {
    val avgX1 = (x1 + x2 + x3) / 3
    val avgY1 = (y1 + y2 + y3) / 3
    val avgX2 = (x1 + x4 + x3) / 3
    val avgY2 = (y1 + y4 + y3) / 3
    centroid.x = avgX1 - (avgX1 - avgX2) / 2
    centroid.y = avgY1 - (avgY1 - avgY2) / 2
    centroid
  }

  /** Returns the centroid for the specified non-self-intersecting polygon. */
  def polygonCentroid(polygon: Array[Float], offset: Int, count: Int, centroid: Vector2): Vector2 = {
    if (count < 6) throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.")

    var area = 0f
    var x    = 0f
    var y    = 0f
    val last = offset + count - 2
    var x1   = polygon(last)
    var y1   = polygon(last + 1)
    var i    = offset
    while (i <= last) {
      val x2 = polygon(i)
      val y2 = polygon(i + 1)
      val a  = x1 * y2 - x2 * y1
      area += a
      x += (x1 + x2) * a
      y += (y1 + y2) * a
      x1 = x2
      y1 = y2
      i += 2
    }
    if (area == 0) {
      centroid.x = 0
      centroid.y = 0
    } else {
      area *= 0.5f
      centroid.x = x / (6 * area)
      centroid.y = y / (6 * area)
    }
    centroid
  }

  /** Computes the area for a convex polygon. */
  def polygonArea(polygon: Array[Float], offset: Int, count: Int): Float = {
    var area = 0f
    val last = offset + count - 2
    var x1   = polygon(last)
    var y1   = polygon(last + 1)
    var i    = offset
    while (i <= last) {
      val x2 = polygon(i)
      val y2 = polygon(i + 1)
      area += x1 * y2 - x2 * y1
      x1 = x2
      y1 = y2
      i += 2
    }
    area * 0.5f
  }

  def ensureCCW(polygon: Array[Float], offset: Int, count: Int): Unit =
    if (isClockwise(polygon, offset, count)) {
      val lastX = offset + count - 2
      var i     = offset
      val n     = offset + count / 2
      while (i < n) {
        val other = lastX - i
        val x     = polygon(i)
        val y     = polygon(i + 1)
        polygon(i) = polygon(other)
        polygon(i + 1) = polygon(other + 1)
        polygon(other) = x
        polygon(other + 1) = y
        i += 2
      }
    }

  def isClockwise(polygon: Array[Float], offset: Int, count: Int): Boolean =
    if (count <= 2) false
    else {
      var area = 0f
      val last = offset + count - 2
      var x1   = polygon(last)
      var y1   = polygon(last + 1)
      var i    = offset
      while (i <= last) {
        val x2 = polygon(i)
        val y2 = polygon(i + 1)
        area += x1 * y2 - x2 * y1
        x1 = x2
        y1 = y2
        i += 2
      }
      area < 0
    }

  /** Returns true if the point is inside the polygon using the ray casting algorithm.
    * @param polygon
    *   pairs of x,y coordinates defining the polygon vertices
    * @param offset
    *   starting index in the polygon array
    * @param count
    *   number of floats (vertices * 2) in the polygon
    * @param x
    *   the x coordinate of the point to test
    * @param y
    *   the y coordinate of the point to test
    * @return
    *   true if the point is inside the polygon
    */
  def isPointInPolygon(polygon: Array[Float], offset: Int, count: Int, x: Float, y: Float): Boolean = {
    var inside = false
    val end    = offset + count
    var i      = offset
    var j      = end - 2
    while (i < end) {
      val xi = polygon(i)
      val yi = polygon(i + 1)
      val xj = polygon(j)
      val yj = polygon(j + 1)

      if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
        inside = !inside
      }
      j = i
      i += 2
    }
    inside
  }
}
