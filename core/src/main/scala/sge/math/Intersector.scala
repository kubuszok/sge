/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Intersector.java
 * Original authors: badlogicgames@gmail.com, jan.stria, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

import sge.math.{ Vector2, Vector3 }
import scala.collection.mutable.ArrayBuffer

/** Class offering various static methods for intersection testing between different geometric objects.
  * @author
  *   badlogicgames@gmail.com (original implementation)
  * @author
  *   jan.stria (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
object Intersector {
  private val v0          = new Vector3()
  private val v1          = new Vector3()
  private val v2          = new Vector3()
  private val floatArray  = new ArrayBuffer[Float]()
  private val floatArray2 = new ArrayBuffer[Float]()

  /** Returns whether the given point is inside the triangle. This assumes that the point is on the plane of the triangle. No check is performed that this is the case. <br> If the Vector3 parameters
    * contain both small and large values, such as one that contains 0.0001 and one that contains 10000000.0, this can fail due to floating-point imprecision.
    *
    * @param t1
    *   the first vertex of the triangle
    * @param t2
    *   the second vertex of the triangle
    * @param t3
    *   the third vertex of the triangle
    * @return
    *   whether the point is in the triangle
    */
  def isPointInTriangle(point: Vector3, t1: Vector3, t2: Vector3, t3: Vector3): Boolean = {
    v0.set(t1).sub(point)
    v1.set(t2).sub(point)
    v2.set(t3).sub(point)

    v1.crs(v2)
    v2.crs(v0)

    if (v1.dot(v2) < 0f) return false
    v0.crs(v2.set(t2).sub(point))
    v1.dot(v0) >= 0f
  }

  /** Returns true if the given point is inside the triangle. */
  def isPointInTriangle(p: Vector2, a: Vector2, b: Vector2, c: Vector2): Boolean =
    isPointInTriangle(p.x, p.y, a.x, a.y, b.x, b.y, c.x, c.y)

  /** Returns true if the given point is inside the triangle. */
  def isPointInTriangle(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Boolean = {
    val px1    = px - ax
    val py1    = py - ay
    val side12 = (bx - ax) * py1 - (by - ay) * px1 > 0
    if ((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) return false
    if ((cx - bx) * (py - by) - (cy - by) * (px - bx) > 0 != side12) return false
    true
  }

  // TODO: Implement intersectSegmentPlane when Plane class is available
  // def intersectSegmentPlane(start: Vector3, end: Vector3, plane: Plane, intersection: Vector3): Boolean = {
  //   val dir = v0.set(end).sub(start)
  //   val denom = dir.dot(plane.getNormal())
  //   if (denom == 0f) return false
  //   val t = -(start.dot(plane.getNormal()) + plane.getD()) / denom
  //   if (t < 0 || t > 1) return false
  //
  //   intersection.set(start).add(dir.scale(t))
  //   true
  // }

  /** Determines on which side of the given line the point is. Returns 1 if the point is on the left side of the line, 0 if the point is on the line and -1 if the point is on the right side of the
    * line. Left and right are relative to the lines direction which is linePoint1 to linePoint2.
    */
  def pointLineSide(linePoint1: Vector2, linePoint2: Vector2, point: Vector2): Int =
    Math.signum((linePoint2.x - linePoint1.x) * (point.y - linePoint1.y) - (linePoint2.y - linePoint1.y) * (point.x - linePoint1.x)).toInt

  def pointLineSide(linePoint1X: Float, linePoint1Y: Float, linePoint2X: Float, linePoint2Y: Float, pointX: Float, pointY: Float): Int =
    Math.signum((linePoint2X - linePoint1X) * (pointY - linePoint1Y) - (linePoint2Y - linePoint1Y) * (pointX - linePoint1X)).toInt

  /** Checks whether the given point is in the polygon.
    * @param polygon
    *   The polygon vertices passed as an array
    * @return
    *   true if the point is in the polygon
    */
  def isPointInPolygon(polygon: Array[Vector2], point: Vector2): Boolean = {
    var last     = polygon.last
    val x        = point.x
    val y        = point.y
    var oddNodes = false
    for (i <- polygon.indices) {
      val vertex = polygon(i)
      if ((vertex.y < y && last.y >= y) || (last.y < y && vertex.y >= y)) {
        if (vertex.x + (y - vertex.y) / (last.y - vertex.y) * (last.x - vertex.x) < x) oddNodes = !oddNodes
      }
      last = vertex
    }
    oddNodes
  }

  /** Returns true if the specified point is in the polygon.
    * @param offset
    *   Starting polygon index.
    * @param count
    *   Number of array indices to use after offset.
    */
  def isPointInPolygon(polygon: Array[Float], offset: Int, count: Int, x: Float, y: Float): Boolean = {
    var oddNodes = false
    val sx       = polygon(offset)
    val sy       = polygon(offset + 1)
    var y1       = sy
    var yi       = offset + 3
    val n        = offset + count
    while (yi < n) {
      val y2 = polygon(yi)
      if ((y2 < y && y1 >= y) || (y1 < y && y2 >= y)) {
        val x2 = polygon(yi - 1)
        if (x2 + (y - y2) / (y1 - y2) * (polygon(yi - 3) - x2) < x) oddNodes = !oddNodes
      }
      y1 = y2
      yi += 2
    }
    if ((sy < y && y1 >= y) || (y1 < y && sy >= y)) {
      if (sx + (y - sy) / (y1 - sy) * (polygon(yi - 3) - sx) < x) oddNodes = !oddNodes
    }
    oddNodes
  }

  private val ip  = new Vector2()
  private val ep1 = new Vector2()
  private val ep2 = new Vector2()
  private val s   = new Vector2()
  private val e   = new Vector2()

  // Add placeholder for hasOverlap method that's referenced by OrientedBoundingBox
  def hasOverlap(axes: Array[Vector3], verticesA: Array[Vector3], verticesB: Array[Vector3]): Boolean =
    // TODO: Implement proper SAT overlap test
    // This is a placeholder to resolve compilation issues
    false

  // TODO: Convert remaining 1400+ lines of Java methods to Scala
  // For now, keeping this object structure to resolve immediate compilation issues
}
