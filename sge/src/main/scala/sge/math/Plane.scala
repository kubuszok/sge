/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Plane.java
 * Original authors: badlogicgames@gmail.com, mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: PlaneSide converted from scala.Enumeration to Scala 3 enum extends java.lang.Enum
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 197
 * Covenant-baseline-methods: Plane,PlaneSide,d,dist,distance,dot,isFrontFacing,normal,set,testPoint,this,toString
 * Covenant-source-reference: com/badlogic/gdx/math/Plane.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

/** A plane defined via a unit length normal and the distance from the origin, as you learned in your math class.
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class Plane {

  import Plane.PlaneSide

  val normal = Vector3()
  var d      = 0f

  /** Constructs a new plane based on the normal and distance to the origin.
    *
    * @param normal
    *   The plane normal
    * @param d
    *   The distance to the origin
    */
  def this(normal: Vector3, d: Float) = {
    this()
    this.normal.set(normal.x, normal.y, normal.z).normalize()
    this.d = d
  }

  /** Constructs a new plane based on the normal and a point on the plane.
    *
    * @param normal
    *   The normal
    * @param point
    *   The point on the plane
    */
  def this(normal: Vector3, point: Vector3) = {
    this()
    this.normal.set(normal.x, normal.y, normal.z).normalize()
    this.d = -this.normal.dot(point)
  }

  /** Constructs a new plane out of the three given points that are considered to be on the plane. The normal is calculated via a cross product between (point1-point2)x(point2-point3)
    *
    * @param point1
    *   The first point
    * @param point2
    *   The second point
    * @param point3
    *   The third point
    */
  def this(point1: Vector3, point2: Vector3, point3: Vector3) = {
    this()
    set(point1, point2, point3)
  }

  /** Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane. The normal is calculated via a cross product between
    * (point1-point2)x(point2-point3)
    *
    * @param point1
    * @param point2
    * @param point3
    */
  def set(point1: Vector3, point2: Vector3, point3: Vector3): Unit = {
    normal.set(point1.x, point1.y, point1.z).-(point2).cross(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).normalize()
    d = -point1.dot(normal)
  }

  /** Sets the plane normal and distance
    *
    * @param nx
    *   normal x-component
    * @param ny
    *   normal y-component
    * @param nz
    *   normal z-component
    * @param d
    *   distance to origin
    */
  def set(nx: Float, ny: Float, nz: Float, d: Float): Unit = {
    normal.set(nx, ny, nz)
    this.d = d
  }

  /** Calculates the shortest signed distance between the plane and the given point.
    *
    * @param point
    *   The point
    * @return
    *   the shortest signed distance between the plane and the point
    */
  def distance(point: Vector3): Float =
    normal.dot(point) + d

  /** Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the plane normal points to.
    *
    * @param point
    *   The point
    * @return
    *   The side the point lies relative to the plane
    */
  def testPoint(point: Vector3): PlaneSide = {
    val dist = normal.dot(point) + d

    if (dist == 0)
      PlaneSide.OnPlane
    else if (dist < 0)
      PlaneSide.Back
    else
      PlaneSide.Front
  }

  /** Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the plane normal points to.
    *
    * @param x
    * @param y
    * @param z
    * @return
    *   The side the point lies relative to the plane
    */
  def testPoint(x: Float, y: Float, z: Float): PlaneSide = {
    val dist = normal.dot(x, y, z) + d

    if (dist == 0)
      PlaneSide.OnPlane
    else if (dist < 0)
      PlaneSide.Back
    else
      PlaneSide.Front
  }

  /** Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in. This method will return true if the front side of the plane determined
    * by its normal faces the camera.
    *
    * @param direction
    *   the direction
    * @return
    *   whether the plane is front facing
    */
  def isFrontFacing(direction: Vector3): Boolean = {
    val dot = normal.dot(direction)
    dot <= 0
  }

  /** Sets the plane to the given point and normal.
    *
    * @param point
    *   the point on the plane
    * @param normal
    *   the normal of the plane
    */
  def set(point: Vector3, normal: Vector3): Unit = {
    this.normal.set(normal.x, normal.y, normal.z)
    d = -point.dot(normal)
  }

  def set(pointX: Float, pointY: Float, pointZ: Float, norX: Float, norY: Float, norZ: Float): Unit = {
    this.normal.set(norX, norY, norZ)
    d = -(pointX * norX + pointY * norY + pointZ * norZ)
  }

  /** Sets this plane from the given plane
    *
    * @param plane
    *   the plane
    */
  def set(plane: Plane): Unit = {
    this.normal.set(plane.normal.x, plane.normal.y, plane.normal.z)
    this.d = plane.d
  }

  override def toString: String =
    normal.toString + ", " + d
}
object Plane {

  /** Enum specifying on which side a point lies respective to the plane and it's normal. {@link PlaneSide#Front} is the side to which the normal points.
    *
    * @author
    *   mzechner
    */
  enum PlaneSide extends java.lang.Enum[PlaneSide] {
    case OnPlane, Back, Front
  }
}
