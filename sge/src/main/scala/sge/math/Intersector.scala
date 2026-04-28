/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Intersector.java
 * Original authors: badlogicgames@gmail.com, jan.stria, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: isPointInTriangle(4), isPointInPolygon(2),
 * distanceLinePoint(2), distanceSegmentPoint(3), nearestSegmentPoint(2),
 * intersectSegmentPlane, intersectLinePlane, planeFrontFacing,
 * intersectPointLinePerpendicular, intersectSegments(3), nearestPoints,
 * intersectRayPlane, intersectRayTriangle(2), intersectRaySphere(2),
 * intersectRayBounds(2), intersectRayBoundsFast(4), intersectRayOrientedBoundsFast,
 * intersectSegmentRectangle(2), intersectSegmentCircle, isInsidePolygon(2),
 * intersectRectangles, intersectSegmentPolygon, overlapConvexPolygons(3),
 * splitTriangle(2), overlaps(Circle/Rect/2Circles), pointLineSide, pointLineSide2D.
 * Inner classes: SplitTriangle, MinimumTranslationVector.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1601
 * Covenant-baseline-methods: Intersector,MinimumTranslationVector,SplitTriangle,_intersection,add,back,backOffset,best,bounds,boundsIntersectsFrustum,closestX,closestY,d,d1xd2,d2sx,d2sy,delta,denom,depth,det,detd,difx,dify,dir,direction,distanceLinePoint,distanceSegmentPoint,divX,divY,divZ,dst2,e,edge1,edge2,edgeSplit,endA,endB,ep1,ep2,f,first,floatArray,floatArray2,front,frontCurrent,frontOffset,frustumIsInsideBounds,hasOverlap,hit,i,ii,intersectBoundsPlaneFast,intersectFrustumBounds,intersectLinePlane,intersectLinePolygon,intersectLines,intersectPlanes,intersectPolygonEdges,intersectPolygons,intersectRayBounds,intersectRayBoundsFast,intersectRayOrientedBounds,intersectRayOrientedBoundsFast,intersectRayPlane,intersectRayRay,intersectRaySphere,intersectRayTriangle,intersectRayTriangles,intersectRectangles,intersectSegmentCircle,intersectSegmentPlane,intersectSegmentPolygon,intersectSegmentRectangle,intersectSegments,ip,isPointInPolygon,isPointInTriangle,l,last,last1,len,length2,lowest,max,maxx,maxy,maxz,min,min_dist,minx,miny,minz,n,nearestSegmentPoint,normal,normalLength,numBack,numFront,oBBposition,oddNodes,origin,overlapConvexPolygons,overlaps,overlapsOnAxisOfShape,p,p1,pointLineSide,pvec,px1,py1,qvec,r1,r2,r3,radius,rectangleEndX,rectangleEndY,reset,s,second,side,side12,side_,splitEdge,splitTriangle,stride,sx,sy,t,t1,t2,tMax,tMin,tmp,tmp1,tmp2,tmp3,toString,total,transform,tvec,u,ua,ub,v,v0,v1,v2,v2a,v2b,v2c,v2d,vertices,vertices2,x,x1,x3,xDiff,xaxis,xd,y,y1,yDiff,yaxis,yd,yi,zaxis
 * Covenant-source-reference: com/badlogic/gdx/math/Intersector.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e61454599581bbeade4b6a2a1514f35d70bf6322
 */
package sge
package math

import sge.math.Plane.PlaneSide
import sge.math.collision.{ BoundingBox, OrientedBoundingBox, Ray }
import sge.utils.{ DynamicArray, Nullable }
import scala.util.boundary
import scala.util.boundary.break

/** Class offering various static methods for intersection testing between different geometric objects.
  * @author
  *   badlogicgames@gmail.com (original implementation)
  * @author
  *   jan.stria (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
object Intersector {
  private val v0          = Vector3()
  private val v1          = Vector3()
  private val v2          = Vector3()
  private val floatArray  = DynamicArray[Float]()
  private val floatArray2 = DynamicArray[Float]()

  private val ip  = Vector2()
  private val ep1 = Vector2()
  private val ep2 = Vector2()
  private val s   = Vector2()
  private val e   = Vector2()

  private val v2a = Vector2()
  private val v2b = Vector2()
  private val v2c = Vector2()
  private val v2d = Vector2()

  private val best = Vector3()
  private val tmp  = Vector3()
  private val tmp1 = Vector3()
  private val tmp2 = Vector3()
  private val tmp3 = Vector3()
  private val p    = Plane(Vector3(), 0)
  private val i    = Vector3()
  Vector3()
  Vector3()

  private val _intersection = Vector3()

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
  def isPointInTriangle(point: Vector3, t1: Vector3, t2: Vector3, t3: Vector3): Boolean = boundary {
    v0.set(t1).sub(point)
    v1.set(t2).sub(point)
    v2.set(t3).sub(point)

    v1.crs(v2)
    v2.crs(v0)

    if (v1.dot(v2) < 0f) break(false)
    v0.crs(v2.set(t2).sub(point))
    v1.dot(v0) >= 0f
  }

  /** Returns true if the given point is inside the triangle. */
  def isPointInTriangle(p: Vector2, a: Vector2, b: Vector2, c: Vector2): Boolean =
    isPointInTriangle(p.x, p.y, a.x, a.y, b.x, b.y, c.x, c.y)

  /** Returns true if the given point is inside the triangle. */
  def isPointInTriangle(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Boolean = boundary {
    val px1    = px - ax
    val py1    = py - ay
    val side12 = (bx - ax) * py1 - (by - ay) * px1 > 0
    if ((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) break(false)
    if ((cx - bx) * (py - by) - (cy - by) * (px - bx) > 0 != side12) break(false)
    true
  }

  def intersectSegmentPlane(start: Vector3, end: Vector3, plane: Plane, intersection: Vector3): Boolean = boundary {
    val dir   = v0.set(end).sub(start)
    val denom = dir.dot(plane.normal)
    if (denom == 0f) break(false)
    val t = -(start.dot(plane.normal) + plane.d) / denom
    if (t < 0 || t > 1) break(false)

    intersection.set(start).add(dir.scl(t))
    true
  }

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

  /** Intersects two convex polygons with clockwise vertices and sets the overlap polygon resulting from the intersection. Follows the Sutherland-Hodgman algorithm.
    * @param p1
    *   The polygon that is being clipped
    * @param p2
    *   The clip polygon
    * @param overlap
    *   The intersection of the two polygons (can be Nullable.empty, if an intersection polygon is not needed)
    * @return
    *   Whether the two polygons intersect.
    */
  def intersectPolygons(p1: Polygon, p2: Polygon, overlap: Nullable[Polygon]): Boolean = boundary {
    if (p1.vertices.length == 0 || p2.vertices.length == 0) {
      break(false)
    }
    val ip         = Intersector.ip; val ep1                 = Intersector.ep1; val ep2 = Intersector.ep2; val s = Intersector.s; val e = Intersector.e
    val floatArray = Intersector.floatArray; val floatArray2 = Intersector.floatArray2
    floatArray.clear()
    floatArray2.clear()
    floatArray2.addAll(p1.transformedVertices, 0, p1.transformedVertices.length)
    val vertices2 = p2.transformedVertices
    var i         = 0
    val last      = vertices2.length - 2
    while (i <= last) {
      ep1.set(vertices2(i), vertices2(i + 1))
      // wrap around to beginning of array if index points to end;
      if (i < last) ep2.set(vertices2(i + 2), vertices2(i + 3))
      else ep2.set(vertices2(0), vertices2(1))
      if (floatArray2.size == 0) break(false)
      s.set(floatArray2(floatArray2.size - 2), floatArray2(floatArray2.size - 1))
      var j = 0
      while (j < floatArray2.size) {
        e.set(floatArray2(j), floatArray2(j + 1))
        // determine if point is inside clip edge
        val side = Intersector.pointLineSide(ep2, ep1, s) > 0
        if (Intersector.pointLineSide(ep2, ep1, e) > 0) {
          if (!side) {
            Intersector.intersectLines(s, e, ep1, ep2, Nullable(ip))
            if (
              floatArray.size < 2 || floatArray(floatArray.size - 2) != ip.x
              || floatArray(floatArray.size - 1) != ip.y
            ) {
              floatArray.add(ip.x)
              floatArray.add(ip.y)
            }
          }
          floatArray.add(e.x)
          floatArray.add(e.y)
        } else if (side) {
          Intersector.intersectLines(s, e, ep1, ep2, Nullable(ip))
          floatArray.add(ip.x)
          floatArray.add(ip.y)
        }
        s.set(e.x, e.y)
        j += 2
      }
      floatArray2.clear()
      floatArray2.addAll(floatArray)
      floatArray.clear()
      i += 2
    }
    // Ensure first and last point are different
    if (
      floatArray2.size >= 6 && floatArray2(0) == floatArray2(floatArray2.size - 2)
      && floatArray2(1) == floatArray2(floatArray2.size - 1)
    ) floatArray2.setSize(floatArray2.size - 2)
    // Check for 3 or more vertices needed due to floating point precision errors
    if (floatArray2.size >= 6) {
      overlap.foreach { ovl =>
        ovl.resetTransformations()
        if (ovl.vertices.length == floatArray2.size)
          System.arraycopy(floatArray2.items, 0, ovl.vertices, 0, floatArray2.size)
        else
          ovl.setVertices(floatArray2.toArray)
      }
      break(true)
    }
    false
  }

  /** Returns true if the specified polygons intersect. */
  def intersectPolygons(polygon1: DynamicArray[Float], polygon2: DynamicArray[Float]): Boolean = boundary {
    if (Intersector.isPointInPolygon(polygon1.items, 0, polygon1.size, polygon2.items(0), polygon2.items(1))) break(true)
    if (Intersector.isPointInPolygon(polygon2.items, 0, polygon2.size, polygon1.items(0), polygon1.items(1))) break(true)
    intersectPolygonEdges(polygon1, polygon2)
  }

  /** Returns true if the lines of the specified polygons intersect. */
  def intersectPolygonEdges(polygon1: DynamicArray[Float], polygon2: DynamicArray[Float]): Boolean = boundary {
    val last1 = polygon1.size - 2; val last2 = polygon2.size - 2
    val p1    = polygon1.items; val p2       = polygon2.items
    var x1    = p1(last1); var y1            = p1(last1 + 1)
    var ii    = 0
    while (ii <= last1) {
      val x2 = p1(ii); val y2    = p1(ii + 1)
      var x3 = p2(last2); var y3 = p2(last2 + 1)
      var j  = 0
      while (j <= last2) {
        val x4 = p2(j); val y4 = p2(j + 1)
        if (intersectSegments(x1, y1, x2, y2, x3, y3, x4, y4, Nullable.empty[Vector2])) break(true)
        x3 = x4
        y3 = y4
        j += 2
      }
      x1 = x2
      y1 = y2
      ii += 2
    }
    false
  }

  /** Returns the distance between the given line and point. Note the specified line is not a line segment. */
  def distanceLinePoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float): Float = {
    val normalLength = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)).toFloat
    Math.abs((pointX - startX) * (endY - startY) - (pointY - startY) * (endX - startX)) / normalLength
  }

  /** Returns the distance between the given segment and point. */
  def distanceSegmentPoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float): Float =
    nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY, v2a).distance(Vector2(pointX, pointY))

  /** Returns the distance between the given segment and point. */
  def distanceSegmentPoint(start: Vector2, end: Vector2, point: Vector2): Float =
    nearestSegmentPoint(start, end, point, v2a).distance(point)

  /** Returns a point on the segment nearest to the specified point. */
  def nearestSegmentPoint(start: Vector2, end: Vector2, point: Vector2, nearest: Vector2): Vector2 = boundary {
    val length2 = start.distanceSq(end)
    if (length2 == 0) break(nearest.set(start))
    val t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / length2
    if (t <= 0) break(nearest.set(start))
    if (t >= 1) break(nearest.set(end))
    nearest.set(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y))
  }

  /** Returns a point on the segment nearest to the specified point. */
  def nearestSegmentPoint(startX: Float, startY: Float, endX: Float, endY: Float, pointX: Float, pointY: Float, nearest: Vector2): Vector2 = boundary {
    val xDiff   = endX - startX
    val yDiff   = endY - startY
    val length2 = xDiff * xDiff + yDiff * yDiff
    if (length2 == 0) break(nearest.set(startX, startY))
    val t = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY)) / length2
    if (t <= 0) break(nearest.set(startX, startY))
    if (t >= 1) break(nearest.set(endX, endY))
    nearest.set(startX + t * (endX - startX), startY + t * (endY - startY))
  }

  /** Returns whether the given line segment intersects the given circle.
    * @param start
    *   The start point of the line segment
    * @param end
    *   The end point of the line segment
    * @param center
    *   The center of the circle
    * @param squareRadius
    *   The squared radius of the circle
    * @return
    *   Whether the line segment and the circle intersect
    */
  def intersectSegmentCircle(start: Vector2, end: Vector2, center: Vector2, squareRadius: Float): Boolean = {
    tmp.set(end.x - start.x, end.y - start.y, 0)
    tmp1.set(center.x - start.x, center.y - start.y, 0)
    val l = tmp.length
    val u = tmp1.dot(tmp.nor())
    if (u <= 0) {
      tmp2.set(start.x, start.y, 0)
    } else if (u >= l) {
      tmp2.set(end.x, end.y, 0)
    } else {
      tmp3.set(tmp.scl(u)) // remember tmp is already normalized
      tmp2.set(tmp3.x + start.x, tmp3.y + start.y, 0)
    }

    val x = center.x - tmp2.x
    val y = center.y - tmp2.y

    x * x + y * y <= squareRadius
  }

  /** Returns whether the given line segment intersects the given circle.
    * @param start
    *   The start point of the line segment
    * @param end
    *   The end point of the line segment
    * @param circle
    *   The circle
    * @param mtv
    *   A Minimum Translation Vector to fill in the case of a collision, or Nullable.empty (optional).
    * @return
    *   Whether the line segment and the circle intersect
    */
  def intersectSegmentCircle(start: Vector2, end: Vector2, circle: Circle, mtv: Nullable[MinimumTranslationVector]): Boolean = {
    v2a.set(end).-(start)
    v2b.set(circle.x - start.x, circle.y - start.y)
    val len = v2a.length
    val u   = v2b.dot(v2a.normalize())
    if (u <= 0) {
      v2c.set(start)
    } else if (u >= len) {
      v2c.set(end)
    } else {
      v2d.set(v2a.scale(u)) // remember v2a is already normalized
      v2c.set(v2d).+(start)
    }

    v2a.set(v2c.x - circle.x, v2c.y - circle.y)

    mtv.foreach { m =>
      // Handle special case of segment containing circle center
      if (v2a.equals(Vector2(0, 0))) {
        v2d.set(end.y - start.y, start.x - end.x)
        m.normal.set(v2d).normalize()
        m.depth = circle.radius
      } else {
        m.normal.set(v2a).normalize()
        m.depth = circle.radius - v2a.length
      }
    }

    v2a.lengthSq <= circle.radius * circle.radius
  }

  /** Returns whether the given {@link Frustum} intersects a {@link BoundingBox}.
    * @param frustum
    *   The frustum
    * @param bounds
    *   The bounding box
    * @return
    *   Whether the frustum intersects the bounding box
    */
  def intersectFrustumBounds(frustum: Frustum, bounds: BoundingBox): Boolean = boundary {
    val boundsIntersectsFrustum = frustum.pointInFrustum(bounds.corner000(tmp)) ||
      frustum.pointInFrustum(bounds.corner001(tmp)) || frustum.pointInFrustum(bounds.corner010(tmp)) ||
      frustum.pointInFrustum(bounds.corner011(tmp)) || frustum.pointInFrustum(bounds.corner100(tmp)) ||
      frustum.pointInFrustum(bounds.corner101(tmp)) || frustum.pointInFrustum(bounds.corner110(tmp)) ||
      frustum.pointInFrustum(bounds.corner111(tmp))

    if (boundsIntersectsFrustum) {
      break(true)
    }

    var frustumIsInsideBounds = false
    for (point <- frustum.planePoints)
      frustumIsInsideBounds |= bounds.contains(point)
    frustumIsInsideBounds
  }

  /** Returns whether the given {@link Frustum} intersects a {@link OrientedBoundingBox}.
    * @param frustum
    *   The frustum
    * @param obb
    *   The oriented bounding box
    * @return
    *   Whether the frustum intersects the oriented bounding box
    */
  def intersectFrustumBounds(frustum: Frustum, obb: OrientedBoundingBox): Boolean = boundary {
    var boundsIntersectsFrustum = false

    for (v <- obb.vertices)
      boundsIntersectsFrustum |= frustum.pointInFrustum(v)

    if (boundsIntersectsFrustum) {
      break(true)
    }

    var frustumIsInsideBounds = false
    for (point <- frustum.planePoints)
      frustumIsInsideBounds |= obb.contains(point)

    frustumIsInsideBounds
  }

  /** Intersect two 2D Rays and return the scalar parameter of the first ray at the intersection point. You can get the intersection point by: Vector2 point(direction1).scl(scalar).add(start1); For
    * more information, check: http://stackoverflow.com/a/565282/1091440
    * @param start1
    *   Where the first ray start
    * @param direction1
    *   The direction the first ray is pointing
    * @param start2
    *   Where the second ray start
    * @param direction2
    *   The direction the second ray is pointing
    * @return
    *   scalar parameter on the first ray describing the point where the intersection happens. May be negative. In case the rays are collinear, Float.POSITIVE_INFINITY will be returned.
    */
  def intersectRayRay(start1: Vector2, direction1: Vector2, start2: Vector2, direction2: Vector2): Float = boundary {
    val difx  = start2.x - start1.x
    val dify  = start2.y - start1.y
    val d1xd2 = direction1.x * direction2.y - direction1.y * direction2.x
    if (d1xd2 == 0.0f) {
      break(Float.PositiveInfinity) // collinear
    }
    val d2sx = direction2.x / d1xd2
    val d2sy = direction2.y / d1xd2
    difx * d2sy - dify * d2sx
  }

  /** Intersects a {@link Ray} and a {@link Plane}. The intersection point is stored in intersection in case an intersection is present.
    * @param intersection
    *   The vector the intersection point is written to (optional)
    * @return
    *   Whether an intersection is present.
    */
  def intersectRayPlane(ray: Ray, plane: Plane, intersection: Nullable[Vector3]): Boolean = boundary {
    val denom = ray.direction.dot(plane.normal)
    if (denom != 0) {
      val t = -(ray.origin.dot(plane.normal) + plane.d) / denom
      if (t < 0) break(false)

      intersection.foreach(_.set(ray.origin).add(v0.set(ray.direction).scl(t)))
      true
    } else if (plane.testPoint(ray.origin) == PlaneSide.OnPlane) {
      intersection.foreach(_.set(ray.origin))
      true
    } else {
      false
    }
  }

  /** Intersects a line and a plane. The intersection is returned as the distance from the first point to the plane. In case an intersection happened, the return value is in the range [0,1]. The
    * intersection point can be recovered by point1 + t * (point2 - point1) where t is the return value of this method.
    */
  def intersectLinePlane(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, plane: Plane, intersection: Nullable[Vector3]): Float = {
    val direction = tmp.set(x2, y2, z2).sub(x, y, z)
    val origin    = tmp2.set(x, y, z)
    val denom     = direction.dot(plane.normal)
    if (denom != 0) {
      val t = -(origin.dot(plane.normal) + plane.d) / denom
      intersection.foreach(_.set(origin).add(direction.scl(t)))
      t
    } else if (plane.testPoint(origin) == PlaneSide.OnPlane) {
      intersection.foreach(_.set(origin))
      0
    } else {
      -1
    }
  }

  /** Returns true if the three {@link Plane planes} intersect, setting the point of intersection in {@code intersection}, if any.
    * @param intersection
    *   The point where the three planes intersect
    */
  def intersectPlanes(a: Plane, b: Plane, c: Plane, intersection: Vector3): Boolean = boundary {
    tmp1.set(a.normal).crs(b.normal)
    tmp2.set(b.normal).crs(c.normal)
    tmp3.set(c.normal).crs(a.normal)

    val f = -a.normal.dot(tmp2)
    if (Math.abs(f) < MathUtils.FLOAT_ROUNDING_ERROR) {
      break(false)
    }

    tmp1.scl(c.d)
    tmp2.scl(a.d)
    tmp3.scl(b.d)

    intersection.set(tmp1.x + tmp2.x + tmp3.x, tmp1.y + tmp2.y + tmp3.y, tmp1.z + tmp2.z + tmp3.z)
    intersection.scl(1 / f)
    true
  }

  /** Intersect a {@link Ray} and a triangle, returning the intersection point in intersection.
    * @param t1
    *   The first vertex of the triangle
    * @param t2
    *   The second vertex of the triangle
    * @param t3
    *   The third vertex of the triangle
    * @param intersection
    *   The intersection point (optional)
    * @return
    *   True in case an intersection is present.
    */
  def intersectRayTriangle(ray: Ray, t1: Vector3, t2: Vector3, t3: Vector3, intersection: Nullable[Vector3]): Boolean = boundary {
    val edge1 = v0.set(t2).sub(t1)
    val edge2 = v1.set(t3).sub(t1)

    val pvec = v2.set(ray.direction).crs(edge2)
    var det  = edge1.dot(pvec)
    if (MathUtils.isZero(det)) {
      p.set(t1, t2, t3)
      if (p.testPoint(ray.origin) == PlaneSide.OnPlane && Intersector.isPointInTriangle(ray.origin, t1, t2, t3)) {
        intersection.foreach(_.set(ray.origin))
        break(true)
      }
      break(false)
    }

    det = 1.0f / det

    val tvec = i.set(ray.origin).sub(t1)
    val u    = tvec.dot(pvec) * det
    if (u < 0.0f || u > 1.0f) break(false)

    val qvec = tvec.crs(edge1)
    val v    = ray.direction.dot(qvec) * det
    if (v < 0.0f || u + v > 1.0f) break(false)

    val t = edge2.dot(qvec) * det
    if (t < 0) break(false)

    intersection.foreach { isect =>
      if (t <= MathUtils.FLOAT_ROUNDING_ERROR) {
        isect.set(ray.origin)
      } else {
        ray.endPoint(isect, t)
      }
    }

    true
  }

  /** Intersects a {@link Ray} and a sphere, returning the intersection point in intersection.
    * @param ray
    *   The ray, the direction component must be normalized before calling this method
    * @param center
    *   The center of the sphere
    * @param radius
    *   The radius of the sphere
    * @param intersection
    *   The intersection point (optional, can be Nullable.empty)
    * @return
    *   Whether an intersection is present.
    */
  def intersectRaySphere(ray: Ray, center: Vector3, radius: Float, intersection: Nullable[Vector3]): Boolean = boundary {
    val len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z)
    if (len < 0f) // behind the ray
      break(false)
    val dst2 = center.distanceSq(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len, ray.origin.z + ray.direction.z * len)
    val r2   = radius * radius
    if (dst2 > r2) break(false)
    intersection.foreach(_.set(ray.direction).scl(len - Math.sqrt(r2 - dst2).toFloat).add(ray.origin))
    true
  }

  /** Intersects a {@link Ray} and a {@link BoundingBox}, returning the intersection point in intersection. This intersection is defined as the point on the ray closest to the origin which is within
    * the specified bounds.
    *
    * <p> The returned intersection (if any) is guaranteed to be within the bounds of the bounding box, but it can occasionally diverge slightly from ray, due to small floating-point errors. </p>
    *
    * <p> If the origin of the ray is inside the box, this method returns true and the intersection point is set to the origin of the ray, accordingly to the definition above. </p>
    * @param intersection
    *   The intersection point (optional)
    * @return
    *   Whether an intersection is present.
    */
  def intersectRayBounds(ray: Ray, box: BoundingBox, intersection: Nullable[Vector3]): Boolean = boundary {
    if (box.contains(ray.origin)) {
      intersection.foreach(_.set(ray.origin))
      break(true)
    }
    var lowest = 0f
    var t      = 0f
    var hit    = false

    // min x
    if (ray.origin.x <= box.min.x && ray.direction.x > 0) {
      t = (box.min.x - ray.origin.x) / ray.direction.x
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    // max x
    if (ray.origin.x >= box.max.x && ray.direction.x < 0) {
      t = (box.max.x - ray.origin.x) / ray.direction.x
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    // min y
    if (ray.origin.y <= box.min.y && ray.direction.y > 0) {
      t = (box.min.y - ray.origin.y) / ray.direction.y
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    // max y
    if (ray.origin.y >= box.max.y && ray.direction.y < 0) {
      t = (box.max.y - ray.origin.y) / ray.direction.y
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    // min z
    if (ray.origin.z <= box.min.z && ray.direction.z > 0) {
      t = (box.min.z - ray.origin.z) / ray.direction.z
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    // max z
    if (ray.origin.z >= box.max.z && ray.direction.z < 0) {
      t = (box.max.z - ray.origin.z) / ray.direction.z
      if (t >= 0) {
        v2.set(ray.direction).scl(t).add(ray.origin)
        if (v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)) {
          hit = true
          lowest = t
        }
      }
    }
    if (hit) {
      intersection.foreach { isect =>
        isect.set(ray.direction).scl(lowest).add(ray.origin)
        if (isect.x < box.min.x) isect.x = box.min.x
        else if (isect.x > box.max.x) isect.x = box.max.x
        if (isect.y < box.min.y) isect.y = box.min.y
        else if (isect.y > box.max.y) isect.y = box.max.y
        if (isect.z < box.min.z) isect.z = box.min.z
        else if (isect.z > box.max.z) isect.z = box.max.z
      }
    }
    hit
  }

  /** Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
    * @return
    *   Whether the ray and the bounding box intersect.
    */
  def intersectRayBoundsFast(ray: Ray, box: BoundingBox): Boolean =
    intersectRayBoundsFast(ray, box.center(tmp1), box.dimensions(tmp2))

  /** Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
    * @param center
    *   The center of the bounding box
    * @param dimensions
    *   The dimensions (width, height and depth) of the bounding box
    * @return
    *   Whether the ray and the bounding box intersect.
    */
  def intersectRayBoundsFast(ray: Ray, center: Vector3, dimensions: Vector3): Boolean = {
    val divX = 1f / ray.direction.x
    val divY = 1f / ray.direction.y
    val divZ = 1f / ray.direction.z

    var minx = ((center.x - dimensions.x * 0.5f) - ray.origin.x) * divX
    var maxx = ((center.x + dimensions.x * 0.5f) - ray.origin.x) * divX
    if (minx > maxx) { val t = minx; minx = maxx; maxx = t }

    var miny = ((center.y - dimensions.y * 0.5f) - ray.origin.y) * divY
    var maxy = ((center.y + dimensions.y * 0.5f) - ray.origin.y) * divY
    if (miny > maxy) { val t = miny; miny = maxy; maxy = t }

    var minz = ((center.z - dimensions.z * 0.5f) - ray.origin.z) * divZ
    var maxz = ((center.z + dimensions.z * 0.5f) - ray.origin.z) * divZ
    if (minz > maxz) { val t = minz; minz = maxz; maxz = t }

    val min = Math.max(Math.max(minx, miny), minz)
    val max = Math.min(Math.min(maxx, maxy), maxz)

    max >= 0 && max >= min
  }

  /** Check whether the given {@link Ray} and {@link OrientedBoundingBox} intersect.
    *
    * @return
    *   Whether the ray and the oriented bounding box intersect.
    */
  def intersectRayOrientedBoundsFast(ray: Ray, obb: OrientedBoundingBox): Boolean =
    intersectRayOrientedBounds(ray, obb, Nullable.empty[Vector3])

  /** Check whether the given {@link Ray} and Oriented {@link BoundingBox} intersect.
    * @param transform
    *   the BoundingBox transformation
    *
    * @return
    *   Whether the ray and the oriented bounding box intersect.
    */
  def intersectRayOrientedBoundsFast(ray: Ray, bounds: BoundingBox, transform: Matrix4): Boolean =
    intersectRayOrientedBounds(ray, bounds, transform, Nullable.empty[Vector3])

  /** Check whether the given {@link Ray} and {@link OrientedBoundingBox} intersect.
    *
    * @param intersection
    *   The intersection point (optional)
    * @return
    *   Whether an intersection is present.
    */
  def intersectRayOrientedBounds(ray: Ray, obb: OrientedBoundingBox, intersection: Nullable[Vector3]): Boolean = {
    val bounds    = obb.bounds
    val transform = obb.transform
    intersectRayOrientedBounds(ray, bounds, transform, intersection)
  }

  /** Check whether the given {@link Ray} and {@link OrientedBoundingBox} intersect.
    *
    * Based on code at: https://github.com/opengl-tutorials/ogl/blob/master/misc05_picking/misc05_picking_custom.cpp#L83
    * @param intersection
    *   The intersection point (optional)
    * @return
    *   Whether an intersection is present.
    */
  def intersectRayOrientedBounds(ray: Ray, bounds: BoundingBox, transform: Matrix4, intersection: Nullable[Vector3]): Boolean = boundary {
    var tMin = 0.0f
    var tMax = Float.MaxValue
    var t1   = 0f
    var t2   = 0f

    val oBBposition = transform.translation(tmp)
    val delta       = oBBposition.sub(ray.origin)

    // Test intersection with the 2 planes perpendicular to the OBB's X axis
    val xaxis = tmp1
    tmp1.set(transform.values(Matrix4.M00), transform.values(Matrix4.M10), transform.values(Matrix4.M20))
    var e = xaxis.dot(delta)
    var f = ray.direction.dot(xaxis)

    if (Math.abs(f) > MathUtils.FLOAT_ROUNDING_ERROR) { // Standard case
      t1 = (e + bounds.min.x) / f // Intersection with the "left" plane
      t2 = (e + bounds.max.x) / f // Intersection with the "right" plane
      if (t1 > t2) { val w = t1; t1 = t2; t2 = w }
      if (t2 < tMax) tMax = t2
      if (t1 > tMin) tMin = t1
      if (tMax < tMin) break(false)
    } else if (-e + bounds.min.x > 0.0f || -e + bounds.max.x < 0.0f) {
      break(false)
    }

    // Test intersection with the 2 planes perpendicular to the OBB's Y axis
    val yaxis = tmp2
    tmp2.set(transform.values(Matrix4.M01), transform.values(Matrix4.M11), transform.values(Matrix4.M21))

    e = yaxis.dot(delta)
    f = ray.direction.dot(yaxis)

    if (Math.abs(f) > MathUtils.FLOAT_ROUNDING_ERROR) {
      t1 = (e + bounds.min.y) / f
      t2 = (e + bounds.max.y) / f
      if (t1 > t2) { val w = t1; t1 = t2; t2 = w }
      if (t2 < tMax) tMax = t2
      if (t1 > tMin) tMin = t1
      if (tMin > tMax) break(false)
    } else if (-e + bounds.min.y > 0.0f || -e + bounds.max.y < 0.0f) {
      break(false)
    }

    // Test intersection with the 2 planes perpendicular to the OBB's Z axis
    val zaxis = tmp3
    tmp3.set(transform.values(Matrix4.M02), transform.values(Matrix4.M12), transform.values(Matrix4.M22))

    e = zaxis.dot(delta)
    f = ray.direction.dot(zaxis)

    if (Math.abs(f) > MathUtils.FLOAT_ROUNDING_ERROR) {
      t1 = (e + bounds.min.z) / f
      t2 = (e + bounds.max.z) / f
      if (t1 > t2) { val w = t1; t1 = t2; t2 = w }
      if (t2 < tMax) tMax = t2
      if (t1 > tMin) tMin = t1
      if (tMin > tMax) break(false)
    } else if (-e + bounds.min.z > 0.0f || -e + bounds.max.z < 0.0f) {
      break(false)
    }

    intersection.foreach { isect =>
      ray.endPoint(isect, tMin)
    }

    true
  }

  /** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
    * @param triangles
    *   The triangles, each successive 9 elements are the 3 vertices of a triangle, a vertex is made of 3 successive floats (XYZ)
    * @param intersection
    *   The nearest intersection point (optional)
    * @return
    *   Whether the ray and the triangles intersect.
    */
  def intersectRayTriangles(ray: Ray, triangles: Array[Float], intersection: Nullable[Vector3]): Boolean = {
    var min_dist = Float.MaxValue
    var hit      = false

    if (triangles.length % 9 != 0) throw new RuntimeException("triangles array size is not a multiple of 9")

    var ii = 0
    while (ii < triangles.length) {
      val result = intersectRayTriangle(
        ray,
        tmp1.set(triangles(ii), triangles(ii + 1), triangles(ii + 2)),
        tmp2.set(triangles(ii + 3), triangles(ii + 4), triangles(ii + 5)),
        tmp3.set(triangles(ii + 6), triangles(ii + 7), triangles(ii + 8)),
        Nullable(tmp)
      )

      if (result) {
        val dist = ray.origin.distanceSq(tmp)
        if (dist < min_dist) {
          min_dist = dist
          best.set(tmp)
          hit = true
        }
      }
      ii += 9
    }

    if (!hit) false
    else {
      intersection.foreach(_.set(best))
      true
    }
  }

  /** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
    * @param indices
    *   the indices, each successive 3 shorts index the 3 vertices of a triangle
    * @param vertexSize
    *   the size of a vertex in floats
    * @param intersection
    *   The nearest intersection point (optional)
    * @return
    *   Whether the ray and the triangles intersect.
    */
  def intersectRayTriangles(ray: Ray, vertices: Array[Float], indices: Array[Short], vertexSize: Int, intersection: Nullable[Vector3]): Boolean = {
    var min_dist = Float.MaxValue
    var hit      = false

    if (indices.length % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3")

    var ii = 0
    while (ii < indices.length) {
      val i1 = indices(ii) * vertexSize
      val i2 = indices(ii + 1) * vertexSize
      val i3 = indices(ii + 2) * vertexSize

      val result = intersectRayTriangle(
        ray,
        tmp1.set(vertices(i1), vertices(i1 + 1), vertices(i1 + 2)),
        tmp2.set(vertices(i2), vertices(i2 + 1), vertices(i2 + 2)),
        tmp3.set(vertices(i3), vertices(i3 + 1), vertices(i3 + 2)),
        Nullable(tmp)
      )

      if (result) {
        val dist = ray.origin.distanceSq(tmp)
        if (dist < min_dist) {
          min_dist = dist
          best.set(tmp)
          hit = true
        }
      }
      ii += 3
    }

    if (!hit) false
    else {
      intersection.foreach(_.set(best))
      true
    }
  }

  /** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
    * @param triangles
    *   The triangles, each successive 3 elements are the 3 vertices of a triangle
    * @param intersection
    *   The nearest intersection point (optional)
    * @return
    *   Whether the ray and the triangles intersect.
    */
  def intersectRayTriangles(ray: Ray, triangles: Seq[Vector3], intersection: Nullable[Vector3]): Boolean = {
    var min_dist = Float.MaxValue
    var hit      = false

    if (triangles.size % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3")

    var ii = 0
    while (ii < triangles.size) {
      val result = intersectRayTriangle(ray, triangles(ii), triangles(ii + 1), triangles(ii + 2), Nullable(tmp))

      if (result) {
        val dist = ray.origin.distanceSq(tmp)
        if (dist < min_dist) {
          min_dist = dist
          best.set(tmp)
          hit = true
        }
      }
      ii += 3
    }

    if (!hit) false
    else {
      intersection.foreach(_.set(best))
      true
    }
  }

  /** Quick check whether the given {@link BoundingBox} and {@link Plane} intersect.
    * @return
    *   Whether the bounding box and the plane intersect.
    */
  def intersectBoundsPlaneFast(box: BoundingBox, plane: Plane): Boolean =
    intersectBoundsPlaneFast(box.center(tmp1), box.dimensions(tmp2).scl(0.5f), plane.normal, plane.d)

  /** Quick check whether the given bounding box and a plane intersect. Code adapted from Christer Ericson's Real Time Collision
    * @param center
    *   The center of the bounding box
    * @param halfDimensions
    *   Half of the dimensions (width, height and depth) of the bounding box
    * @param normal
    *   The normal of the plane
    * @param distance
    *   The distance of the plane
    * @return
    *   Whether the bounding box and the plane intersect.
    */
  def intersectBoundsPlaneFast(center: Vector3, halfDimensions: Vector3, normal: Vector3, distance: Float): Boolean = {
    // Compute the projection interval radius of b onto L(t) = b.c + t * p.n
    val radius = halfDimensions.x * Math.abs(normal.x) + halfDimensions.y * Math.abs(normal.y) +
      halfDimensions.z * Math.abs(normal.z)

    // Compute distance of box center from plane
    val s = normal.dot(center) - distance

    // Intersection occurs when plane distance falls within [-r,+r] interval
    Math.abs(s) <= radius
  }

  /** Intersects the two lines and returns the intersection point in intersection.
    * @param p1
    *   The first point of the first line
    * @param p2
    *   The second point of the first line
    * @param p3
    *   The first point of the second line
    * @param p4
    *   The second point of the second line
    * @param intersection
    *   The intersection point. May be Nullable.empty.
    * @return
    *   Whether the two lines intersect
    */
  def intersectLines(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2, intersection: Nullable[Vector2]): Boolean =
    intersectLines(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, intersection)

  /** Intersects the two lines and returns the intersection point in intersection.
    * @param intersection
    *   The intersection point, or Nullable.empty.
    * @return
    *   Whether the two lines intersect
    */
  def intersectLines(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, intersection: Nullable[Vector2]): Boolean = boundary {
    val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
    if (d == 0) break(false)

    intersection.foreach { isect =>
      val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d
      isect.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua)
    }
    true
  }

  /** Check whether the given line and {@link Polygon} intersect.
    * @param p1
    *   The first point of the line
    * @param p2
    *   The second point of the line
    * @return
    *   Whether polygon and line intersects
    */
  def intersectLinePolygon(p1: Vector2, p2: Vector2, polygon: Polygon): Boolean = boundary {
    val vertices = polygon.transformedVertices
    val x1       = p1.x; val y1            = p1.y; val x2 = p2.x; val y2 = p2.y
    val n        = vertices.length
    var x3       = vertices(n - 2); var y3 = vertices(n - 1)
    var ii       = 0
    while (ii < n) {
      val x4 = vertices(ii); val y4 = vertices(ii + 1)
      val d  = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
      if (d != 0) {
        val yd = y1 - y3
        val xd = x1 - x3
        val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
        if (ua >= 0 && ua <= 1) {
          break(true)
        }
      }
      x3 = x4
      y3 = y4
      ii += 2
    }
    false
  }

  /** Determines whether the given rectangles intersect and, if they do, sets the supplied {@code intersection} rectangle to the area of overlap.
    * @return
    *   Whether the rectangles intersect
    */
  def intersectRectangles(rectangle1: Rectangle, rectangle2: Rectangle, intersection: Rectangle): Boolean = boundary {
    if (rectangle1.overlaps(rectangle2)) {
      intersection.x = Math.max(rectangle1.x, rectangle2.x)
      intersection.width = Math.min(rectangle1.x + rectangle1.width, rectangle2.x + rectangle2.width) - intersection.x
      intersection.y = Math.max(rectangle1.y, rectangle2.y)
      intersection.height = Math.min(rectangle1.y + rectangle1.height, rectangle2.y + rectangle2.height) - intersection.y
      break(true)
    }
    false
  }

  /** Determines whether the given rectangle and segment intersect
    * @param startX
    *   x-coordinate start of line segment
    * @param startY
    *   y-coordinate start of line segment
    * @param endX
    *   x-coordinate end of line segment
    * @param endY
    *   y-coordinate end of line segment
    * @param rectangle
    *   rectangle that is being tested for collision
    * @return
    *   whether the rectangle intersects with the line segment
    */
  def intersectSegmentRectangle(startX: Float, startY: Float, endX: Float, endY: Float, rectangle: Rectangle): Boolean = boundary {
    val rectangleEndX = rectangle.x + rectangle.width
    val rectangleEndY = rectangle.y + rectangle.height

    if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangle.x, rectangleEndY, Nullable.empty[Vector2])) break(true)
    if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangle.y, rectangleEndX, rectangle.y, Nullable.empty[Vector2])) break(true)
    if (intersectSegments(startX, startY, endX, endY, rectangleEndX, rectangle.y, rectangleEndX, rectangleEndY, Nullable.empty[Vector2])) break(true)
    if (intersectSegments(startX, startY, endX, endY, rectangle.x, rectangleEndY, rectangleEndX, rectangleEndY, Nullable.empty[Vector2])) break(true)

    rectangle.contains(startX, startY)
  }

  /** {@link #intersectSegmentRectangle(Float, Float, Float, Float, Rectangle)} */
  def intersectSegmentRectangle(start: Vector2, end: Vector2, rectangle: Rectangle): Boolean =
    intersectSegmentRectangle(start.x, start.y, end.x, end.y, rectangle)

  /** Check whether the given line segment and {@link Polygon} intersect.
    * @param p1
    *   The first point of the segment
    * @param p2
    *   The second point of the segment
    * @return
    *   Whether polygon and segment intersect
    */
  def intersectSegmentPolygon(p1: Vector2, p2: Vector2, polygon: Polygon): Boolean = boundary {
    val vertices = polygon.transformedVertices
    val x1       = p1.x; val y1            = p1.y; val x2 = p2.x; val y2 = p2.y
    val n        = vertices.length
    var x3       = vertices(n - 2); var y3 = vertices(n - 1)
    var ii       = 0
    while (ii < n) {
      val x4 = vertices(ii); val y4 = vertices(ii + 1)
      val d  = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
      if (d != 0) {
        val yd = y1 - y3
        val xd = x1 - x3
        val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
        if (ua >= 0 && ua <= 1) {
          val ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d
          if (ub >= 0 && ub <= 1) {
            break(true)
          }
        }
      }
      x3 = x4
      y3 = y4
      ii += 2
    }
    false
  }

  /** Intersects the two line segments and returns the intersection point in intersection.
    * @param p1
    *   The first point of the first line segment
    * @param p2
    *   The second point of the first line segment
    * @param p3
    *   The first point of the second line segment
    * @param p4
    *   The second point of the second line segment
    * @param intersection
    *   The intersection point. May be Nullable.empty.
    * @return
    *   Whether the two line segments intersect
    */
  def intersectSegments(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2, intersection: Nullable[Vector2]): Boolean =
    intersectSegments(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, intersection)

  /** @param intersection
    *   May be Nullable.empty.
    */
  def intersectSegments(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, intersection: Nullable[Vector2]): Boolean = boundary {
    val d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
    if (d == 0) break(false)

    val yd = y1 - y3
    val xd = x1 - x3
    val ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d
    if (ua < 0 || ua > 1) break(false)

    val ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d
    if (ub < 0 || ub > 1) break(false)

    intersection.foreach(_.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua))
    true
  }

  private[math] def det(a: Float, b: Float, c: Float, d: Float): Float =
    a * d - b * c

  private[math] def detd(a: Double, b: Double, c: Double, d: Double): Double =
    a * d - b * c

  def overlaps(c1: Circle, c2: Circle): Boolean =
    c1.overlaps(c2)

  def overlaps(r1: Rectangle, r2: Rectangle): Boolean =
    r1.overlaps(r2)

  def overlaps(c: Circle, r: Rectangle): Boolean = {
    var closestX = c.x
    var closestY = c.y

    if (c.x < r.x) closestX = r.x
    else if (c.x > r.x + r.width) closestX = r.x + r.width

    if (c.y < r.y) closestY = r.y
    else if (c.y > r.y + r.height) closestY = r.y + r.height

    closestX = closestX - c.x
    closestX *= closestX
    closestY = closestY - c.y
    closestY *= closestY

    closestX + closestY < c.radius * c.radius
  }

  /** Check whether specified convex polygons overlap (clockwise or counter-clockwise wound doesn't matter).
    * @param p1
    *   The first polygon.
    * @param p2
    *   The second polygon.
    * @return
    *   Whether polygons overlap.
    */
  def overlapConvexPolygons(p1: Polygon, p2: Polygon): Boolean =
    overlapConvexPolygons(p1, p2, Nullable.empty[MinimumTranslationVector])

  /** Check whether convex polygons overlap (clockwise or counter-clockwise wound doesn't matter). If they do, optionally obtain a Minimum Translation Vector indicating the minimum magnitude vector
    * required to push the polygon p1 out of collision with polygon p2.
    * @param p1
    *   The first polygon.
    * @param p2
    *   The second polygon.
    * @param mtv
    *   A Minimum Translation Vector to fill in the case of a collision, or Nullable.empty (optional).
    * @return
    *   Whether polygons overlap.
    */
  def overlapConvexPolygons(p1: Polygon, p2: Polygon, mtv: Nullable[MinimumTranslationVector]): Boolean =
    overlapConvexPolygons(p1.transformedVertices, p2.transformedVertices, mtv)

  /** @see #overlapConvexPolygons(Array[Float], Int, Int, Array[Float], Int, Int, Nullable[MinimumTranslationVector]) */
  def overlapConvexPolygons(verts1: Array[Float], verts2: Array[Float], mtv: Nullable[MinimumTranslationVector]): Boolean =
    overlapConvexPolygons(verts1, 0, verts1.length, verts2, 0, verts2.length, mtv)

  /** Check whether polygons defined by the given vertex arrays overlap (clockwise or counter-clockwise wound doesn't matter). If they do, optionally obtain a Minimum Translation Vector indicating the
    * minimum magnitude vector required to push the polygon defined by verts1 out of the collision with the polygon defined by verts2.
    * @param verts1
    *   Vertices of the first polygon.
    * @param offset1
    *   the offset of the verts1 array
    * @param count1
    *   the amount that is added to the offset1
    * @param verts2
    *   Vertices of the second polygon.
    * @param offset2
    *   the offset of the verts2 array
    * @param count2
    *   the amount that is added to the offset2
    * @param mtv
    *   A Minimum Translation Vector to fill in the case of a collision, or Nullable.empty (optional).
    * @return
    *   Whether polygons overlap.
    */
  def overlapConvexPolygons(verts1: Array[Float], offset1: Int, count1: Int, verts2: Array[Float], offset2: Int, count2: Int, mtv: Nullable[MinimumTranslationVector]): Boolean = boundary {
    var overlaps = false
    mtv.foreach { m =>
      m.depth = Float.MaxValue
      m.normal.setZero()
    }
    overlaps = overlapsOnAxisOfShape(verts2, offset2, count2, verts1, offset1, count1, mtv, true)
    if (overlaps) {
      overlaps = overlapsOnAxisOfShape(verts1, offset1, count1, verts2, offset2, count2, mtv, false)
    }

    if (!overlaps) {
      mtv.foreach { m =>
        m.depth = 0
        m.normal.setZero()
      }
      break(false)
    }
    true
  }

  /** Implementation of the separating axis theorem (SAT) algorithm
    * @param offset1
    *   offset of verts1
    * @param count1
    *   count of verts1
    * @param offset2
    *   offset of verts2
    * @param count2
    *   count of verts2
    * @param mtv
    *   the minimum translation vector
    * @param shapesShifted
    *   states if shape a and b are shifted. Important for calculating the axis translation for verts1.
    */
  private def overlapsOnAxisOfShape(
    verts1:        Array[Float],
    offset1:       Int,
    count1:        Int,
    verts2:        Array[Float],
    offset2:       Int,
    count2:        Int,
    mtv:           Nullable[MinimumTranslationVector],
    shapesShifted: Boolean
  ): Boolean = boundary {
    val endA = offset1 + count1
    val endB = offset2 + count2
    // get axis of polygon A
    var ii = offset1
    while (ii < endA) {
      val x1 = verts1(ii)
      val y1 = verts1(ii + 1)
      val x2 = verts1((ii + 2 - offset1) % count1 + offset1)
      val y2 = verts1((ii + 3 - offset1) % count1 + offset1)

      // Get the Axis for the 2 vertices
      var axisX = y1 - y2
      var axisY = -(x1 - x2)

      val len = Math.sqrt(axisX * axisX + axisY * axisY).toFloat
      // We got a normalized Vector
      axisX /= len
      axisY /= len
      var minA = Float.MaxValue
      var maxA = -Float.MaxValue
      // project shape a on axis
      var v = offset1
      while (v < endA) {
        val pv = verts1(v) * axisX + verts1(v + 1) * axisY
        minA = Math.min(minA, pv)
        maxA = Math.max(maxA, pv)
        v += 2
      }

      var minB = Float.MaxValue
      var maxB = -Float.MaxValue

      // project shape b on axis
      v = offset2
      while (v < endB) {
        val pv = verts2(v) * axisX + verts2(v + 1) * axisY
        minB = Math.min(minB, pv)
        maxB = Math.max(maxB, pv)
        v += 2
      }
      // There is a gap
      if (maxA < minB || maxB < minA) {
        break(false)
      } else {
        mtv.foreach { m =>
          val o          = Math.min(maxA, maxB) - Math.max(minA, minB)
          val aContainsB = minA < minB && maxA > maxB
          val bContainsA = minB < minA && maxB > maxA
          // if it contains one or another
          var mins      = 0f
          var maxs      = 0f
          var adjustedO = o
          if (aContainsB || bContainsA) {
            mins = Math.abs(minA - minB)
            maxs = Math.abs(maxA - maxB)
            adjustedO += Math.min(mins, maxs)
          }

          if (m.depth > adjustedO) {
            m.depth = adjustedO
            var condition = false
            if (shapesShifted) {
              condition = minA < minB
              axisX = if (condition) axisX else -axisX
              axisY = if (condition) axisY else -axisY
            } else {
              condition = minA > minB
              axisX = if (condition) axisX else -axisX
              axisY = if (condition) axisY else -axisY
            }

            if (aContainsB || bContainsA) {
              condition = mins > maxs
              axisX = if (condition) axisX else -axisX
              axisY = if (condition) axisY else -axisY
            }

            m.normal.set(axisX, axisY)
          }
        }
      }
      ii += 2
    }
    true
  }

  /** Splits the triangle by the plane. The result is stored in the SplitTriangle instance. Depending on where the triangle is relative to the plane, the result can be:
    *
    * <ul> <li>Triangle is fully in front/behind: {@link SplitTriangle#front} or {@link SplitTriangle#back} will contain the original triangle, {@link SplitTriangle#total} will be one.</li>
    * <li>Triangle has two vertices in front, one behind: {@link SplitTriangle#front} contains 2 triangles, {@link SplitTriangle#back} contains 1 triangles, {@link SplitTriangle#total} will be 3.</li>
    * <li>Triangle has one vertex in front, two behind: {@link SplitTriangle#front} contains 1 triangle, {@link SplitTriangle#back} contains 2 triangles, {@link SplitTriangle#total} will be 3.</li>
    * </ul>
    *
    * The input triangle should have the form: x, y, z, x2, y2, z2, x3, y3, z3. One can add additional attributes per vertex which will be interpolated if split, such as texture coordinates or
    * normals. Note that these additional attributes won't be normalized, as might be necessary in case of normals.
    * @param split
    *   output SplitTriangle
    */
  def splitTriangle(triangle: Array[Float], plane: Plane, split: SplitTriangle): Unit = boundary {
    val stride = triangle.length / 3
    val r1     = plane.testPoint(triangle(0), triangle(1), triangle(2)) == PlaneSide.Back
    val r2     = plane.testPoint(triangle(0 + stride), triangle(1 + stride), triangle(2 + stride)) == PlaneSide.Back
    val r3     = plane.testPoint(triangle(0 + stride * 2), triangle(1 + stride * 2), triangle(2 + stride * 2)) == PlaneSide.Back

    split.reset()

    // easy case, triangle is on one side (point on plane means front).
    if (r1 == r2 && r2 == r3) {
      split.total = 1
      if (r1) {
        split.numBack = 1
        System.arraycopy(triangle, 0, split.back, 0, triangle.length)
      } else {
        split.numFront = 1
        System.arraycopy(triangle, 0, split.front, 0, triangle.length)
      }
      break()
    }

    // set number of triangles
    split.total = 3
    split.numFront = (if (r1) 0 else 1) + (if (r2) 0 else 1) + (if (r3) 0 else 1)
    split.numBack = split.total - split.numFront

    // hard case, split the three edges on the plane
    // determine which array to fill first, front or back, flip if we
    // cross the plane
    split.side = !r1

    // split first edge
    var first  = 0
    var second = stride
    if (r1 != r2) {
      // split the edge
      splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

      // add first edge vertex and new vertex to current side
      split.add(triangle, first, stride)
      split.add(split.edgeSplit, 0, stride)

      // flip side and add new vertex and second edge vertex to current side
      split.side = !split.side
      split.add(split.edgeSplit, 0, stride)
    } else {
      // add both vertices
      split.add(triangle, first, stride)
    }

    // split second edge
    first = stride
    second = stride + stride
    if (r2 != r3) {
      // split the edge
      splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

      // add first edge vertex and new vertex to current side
      split.add(triangle, first, stride)
      split.add(split.edgeSplit, 0, stride)

      // flip side and add new vertex and second edge vertex to current side
      split.side = !split.side
      split.add(split.edgeSplit, 0, stride)
    } else {
      // add both vertices
      split.add(triangle, first, stride)
    }

    // split third edge
    first = stride + stride
    second = 0
    if (r3 != r1) {
      // split the edge
      splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0)

      // add first edge vertex and new vertex to current side
      split.add(triangle, first, stride)
      split.add(split.edgeSplit, 0, stride)

      // flip side and add new vertex and second edge vertex to current side
      split.side = !split.side
      split.add(split.edgeSplit, 0, stride)
    } else {
      // add both vertices
      split.add(triangle, first, stride)
    }

    // triangulate the side with 2 triangles
    if (split.numFront == 2) {
      System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2)
      System.arraycopy(split.front, 0, split.front, stride * 5, stride)
    } else {
      System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2)
      System.arraycopy(split.back, 0, split.back, stride * 5, stride)
    }
  }

  private def splitEdge(vertices: Array[Float], s: Int, e: Int, stride: Int, plane: Plane, split: Array[Float], offset: Int): Unit = {
    val t = Intersector.intersectLinePlane(vertices(s), vertices(s + 1), vertices(s + 2), vertices(e), vertices(e + 1), vertices(e + 2), plane, Nullable(_intersection))
    split(offset + 0) = _intersection.x
    split(offset + 1) = _intersection.y
    split(offset + 2) = _intersection.z
    var ii = 3
    while (ii < stride) {
      val a = vertices(s + ii)
      val b = vertices(e + ii)
      split(offset + ii) = a + t * (b - a)
      ii += 1
    }
  }

  /** Tests overlap between two convex shapes using the Separating Axis Theorem (SAT).
    * @param axes
    *   the axes to test
    * @param verticesA
    *   vertices of the first shape
    * @param verticesB
    *   vertices of the second shape
    * @return
    *   true if the projections of the two shapes overlap on all axes
    */
  def hasOverlap(axes: Array[Vector3], verticesA: Array[Vector3], verticesB: Array[Vector3]): Boolean =
    scala.util.boundary {
      for (axis <- axes) {
        var minA = Float.MaxValue; var maxA = -Float.MaxValue
        for (v <- verticesA) { val p = v.dot(axis); if (p < minA) minA = p; if (p > maxA) maxA = p }
        var minB = Float.MaxValue; var maxB = -Float.MaxValue
        for (v <- verticesB) { val p = v.dot(axis); if (p < minB) minB = p; if (p > maxB) maxB = p }
        if (maxA < minB || maxB < minA) scala.util.boundary.break(false)
      }
      true
    }

  /** Splits the triangle by the plane. The result is stored in the SplitTriangle instance. */
  class SplitTriangle(numAttributes: Int) {
    var front:     Array[Float] = new Array[Float](numAttributes * 3 * 2)
    var back:      Array[Float] = new Array[Float](numAttributes * 3 * 2)
    var edgeSplit: Array[Float] = new Array[Float](numAttributes)
    var numFront:  Int          = 0
    var numBack:   Int          = 0
    var total:     Int          = 0
    private var frontCurrent = false
    private var frontOffset  = 0
    private var backOffset   = 0

    override def toString: String =
      s"SplitTriangle [front=${java.util.Arrays.toString(front)}, back=${java.util.Arrays.toString(back)}, numFront=$numFront, numBack=$numBack, total=$total]"

    def side_=(front: Boolean): Unit =
      frontCurrent = front

    def side: Boolean = frontCurrent

    def add(vertex: Array[Float], offset: Int, stride: Int): Unit =
      if (frontCurrent) {
        System.arraycopy(vertex, offset, front, frontOffset, stride)
        frontOffset += stride
      } else {
        System.arraycopy(vertex, offset, back, backOffset, stride)
        backOffset += stride
      }

    def reset(): Unit = {
      frontCurrent = false
      frontOffset = 0
      backOffset = 0
      numFront = 0
      numBack = 0
      total = 0
    }
  }

  /** Minimum translation required to separate two polygons. */
  class MinimumTranslationVector {

    /** Unit length vector that indicates the direction for the separation */
    val normal: Vector2 = Vector2()

    /** Distance of the translation required for the separation */
    var depth: Float = 0
  }
}
