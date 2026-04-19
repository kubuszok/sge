/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Frustum.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: update, pointInFrustum(2), sphereInFrustum(2),
 * sphereInFrustumWithoutNearFar(2), boundsInFrustum(BoundingBox), boundsInFrustum(center,dim),
 * boundsInFrustum(x,y,z,hw,hh,hd), boundsInFrustum(OrientedBoundingBox).
 * Static: clipSpacePlanePoints, clipSpacePlanePointsArray, tmpV.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 262
 * Covenant-baseline-methods: Frustum,array,boundsInFrustum,clipSpacePlanePoints,clipSpacePlanePointsArray,j,planePoints,planePointsArray,planes,pointInFrustum,sphereInFrustum,sphereInFrustumWithoutNearFar,tmpV,update
 * Covenant-source-reference: com/badlogic/gdx/math/Frustum.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

import sge.math.Plane.PlaneSide
import sge.math.collision.BoundingBox
import sge.math.collision.OrientedBoundingBox

/** A truncated rectangular pyramid. Used to define the viewable region and its projection onto the screen.
  * @see
  *   Camera#frustum
  */
class Frustum {
  import Frustum._

  /** the six clipping planes, near, far, left, right, top, bottom * */
  val planes: Array[Plane] = Array.fill(6)(Plane(Vector3(), 0))

  /** eight points making up the near and far clipping "rectangles". order is counter clockwise, starting at bottom left * */
  val planePoints:                Array[Vector3] = Array.fill(8)(Vector3())
  protected val planePointsArray: Array[Float]   = Array.ofDim[Float](8 * 3)

  /** Updates the clipping plane's based on the given inverse combined projection and view matrix, e.g. from an {@link OrthographicCamera} or {@link PerspectiveCamera} .
    * @param inverseProjectionView
    *   the combined projection and view matrices.
    */
  def update(inverseProjectionView: Matrix4): Unit = {
    System.arraycopy(clipSpacePlanePointsArray, 0, planePointsArray, 0, clipSpacePlanePointsArray.length)
    Matrix4.prj(inverseProjectionView.values, planePointsArray, 0, 8, 3)
    for (i <- 0 until 8) {
      val j = i * 3
      val v = planePoints(i)
      v.x = planePointsArray(j)
      v.y = planePointsArray(j + 1)
      v.z = planePointsArray(j + 2)
    }

    planes(0).set(planePoints(1), planePoints(0), planePoints(2))
    planes(1).set(planePoints(4), planePoints(5), planePoints(7))
    planes(2).set(planePoints(0), planePoints(4), planePoints(3))
    planes(3).set(planePoints(5), planePoints(1), planePoints(6))
    planes(4).set(planePoints(2), planePoints(3), planePoints(6))
    planes(5).set(planePoints(4), planePoints(0), planePoints(1))
  }

  /** Returns whether the point is in the frustum.
    *
    * @param point
    *   The point
    * @return
    *   Whether the point is in the frustum.
    */
  def pointInFrustum(point: Vector3): Boolean = scala.util.boundary {
    for (i <- planes.indices) {
      val result = planes(i).testPoint(point)
      if (result == PlaneSide.Back) scala.util.boundary.break(false)
    }
    true
  }

  /** Returns whether the point is in the frustum.
    *
    * @param x
    *   The X coordinate of the point
    * @param y
    *   The Y coordinate of the point
    * @param z
    *   The Z coordinate of the point
    * @return
    *   Whether the point is in the frustum.
    */
  def pointInFrustum(x: Float, y: Float, z: Float): Boolean = scala.util.boundary {
    for (i <- planes.indices) {
      val result = planes(i).testPoint(x, y, z)
      if (result == PlaneSide.Back) scala.util.boundary.break(false)
    }
    true
  }

  /** Returns whether the given sphere is in the frustum.
    *
    * @param center
    *   The center of the sphere
    * @param radius
    *   The radius of the sphere
    * @return
    *   Whether the sphere is in the frustum
    */
  def sphereInFrustum(center: Vector3, radius: Float): Boolean = scala.util.boundary {
    for (i <- 0 until 6)
      if ((planes(i).normal.x * center.x + planes(i).normal.y * center.y + planes(i).normal.z * center.z) < (-radius - planes(i).d))
        scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given sphere is in the frustum.
    *
    * @param x
    *   The X coordinate of the center of the sphere
    * @param y
    *   The Y coordinate of the center of the sphere
    * @param z
    *   The Z coordinate of the center of the sphere
    * @param radius
    *   The radius of the sphere
    * @return
    *   Whether the sphere is in the frustum
    */
  def sphereInFrustum(x: Float, y: Float, z: Float, radius: Float): Boolean = scala.util.boundary {
    for (i <- 0 until 6)
      if ((planes(i).normal.x * x + planes(i).normal.y * y + planes(i).normal.z * z) < (-radius - planes(i).d))
        scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
    *
    * @param center
    *   The center of the sphere
    * @param radius
    *   The radius of the sphere
    * @return
    *   Whether the sphere is in the frustum
    */
  def sphereInFrustumWithoutNearFar(center: Vector3, radius: Float): Boolean = scala.util.boundary {
    for (i <- 2 until 6)
      if ((planes(i).normal.x * center.x + planes(i).normal.y * center.y + planes(i).normal.z * center.z) < (-radius - planes(i).d))
        scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
    *
    * @param x
    *   The X coordinate of the center of the sphere
    * @param y
    *   The Y coordinate of the center of the sphere
    * @param z
    *   The Z coordinate of the center of the sphere
    * @param radius
    *   The radius of the sphere
    * @return
    *   Whether the sphere is in the frustum
    */
  def sphereInFrustumWithoutNearFar(x: Float, y: Float, z: Float, radius: Float): Boolean = scala.util.boundary {
    for (i <- 2 until 6)
      if ((planes(i).normal.x * x + planes(i).normal.y * y + planes(i).normal.z * z) < (-radius - planes(i).d))
        scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given {@link BoundingBox} is in the frustum.
    *
    * @param bounds
    *   The bounding box
    * @return
    *   Whether the bounding box is in the frustum
    */
  def boundsInFrustum(bounds: BoundingBox): Boolean = scala.util.boundary {
    for (i <- planes.indices)
      if (planes(i).testPoint(bounds.corner000(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner001(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner010(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner011(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner100(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner101(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner110(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(bounds.corner111(tmpV)) != PlaneSide.Back) {}
      else
        scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given bounding box is in the frustum.
    * @return
    *   Whether the bounding box is in the frustum
    */
  def boundsInFrustum(center: Vector3, dimensions: Vector3): Boolean =
    boundsInFrustum(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2)

  /** Returns whether the given bounding box is in the frustum.
    * @return
    *   Whether the bounding box is in the frustum
    */
  def boundsInFrustum(x: Float, y: Float, z: Float, halfWidth: Float, halfHeight: Float, halfDepth: Float): Boolean = scala.util.boundary {
    for (i <- planes.indices)
      if (planes(i).testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.Back) {}
      else if (planes(i).testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.Back) {}
      else scala.util.boundary.break(false)
    true
  }

  /** Returns whether the given {@link OrientedBoundingBox} is in the frustum.
    *
    * @param obb
    *   The oriented bounding box
    * @return
    *   Whether the oriented bounding box is in the frustum
    */
  def boundsInFrustum(obb: OrientedBoundingBox): Boolean = scala.util.boundary {
    for (i <- planes.indices)
      if (planes(i).testPoint(obb.corner000(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner001(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner010(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner011(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner100(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner101(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner110(tmpV)) != PlaneSide.Back) {}
      else if (planes(i).testPoint(obb.corner111(tmpV)) != PlaneSide.Back) {}
      else scala.util.boundary.break(false)
    true
  }
}

object Frustum {
  protected val clipSpacePlanePoints: Array[Vector3] = Array(
    Vector3(-1, -1, -1),
    Vector3(1, -1, -1),
    Vector3(1, 1, -1),
    Vector3(-1, 1, -1), // near clip
    Vector3(-1, -1, 1),
    Vector3(1, -1, 1),
    Vector3(1, 1, 1),
    Vector3(-1, 1, 1) // far clip
  )

  protected val clipSpacePlanePointsArray: Array[Float] = {
    val array = Array.ofDim[Float](8 * 3)
    var j     = 0
    for (v <- clipSpacePlanePoints) {
      array(j) = v.x
      array(j + 1) = v.y
      array(j + 2) = v.z
      j += 3
    }
    array
  }

  private val tmpV = Vector3()
}
