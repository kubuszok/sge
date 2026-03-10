/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/collision/Ray.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: Serializable dropped; serialVersionUID dropped;
 *     primary constructor copies Vector3 values and normalizes direction (matches Java);
 *     static tmp Vector3 replaced with local allocation in mul();
 *     added 3-arg constructor (originVec, directionVec, normalize) not in Java
 *   Idiom: split packages
 *   Audited: 2026-03-04
 */
package sge
package math
package collision

/** Encapsulates a ray having a starting position and a unit length direction.
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class Ray() {
  val origin:    Vector3 = Vector3()
  val direction: Vector3 = Vector3()

  /** Constructor, sets the starting position of the ray and the direction.
    * @param origin
    *   The starting position
    * @param direction
    *   The direction
    */
  def this(origin: Vector3, direction: Vector3) = {
    this()
    this.origin.set(origin)
    this.direction.set(direction).nor()
  }

  /** Alternative constructor with different parameter names to avoid conflicts.
    * @param originVec
    *   The starting position
    * @param directionVec
    *   The direction
    */
  def this(originVec: Vector3, directionVec: Vector3, normalize: Boolean) = {
    this()
    origin.set(originVec)
    direction.set(directionVec)
    if (normalize) direction.nor()
  }

  /** @return a copy of this ray. */
  def cpy(): Ray =
    Ray(Vector3(origin.x, origin.y, origin.z), Vector3(direction.x, direction.y, direction.z))

  /** Returns the endpoint given the distance. This is calculated as startpoint + distance * direction.
    * @param out
    *   The vector to set to the result
    * @param distance
    *   The distance from the end point to the start point.
    * @return
    *   The out param
    */
  def getEndPoint(out: Vector3, distance: Float): Vector3 =
    out.set(direction).scl(distance).add(origin)

  /** Multiplies the ray by the given matrix. Use this to transform a ray into another coordinate system.
    *
    * @param matrix
    *   The matrix
    * @return
    *   This ray for chaining.
    */
  def mul(matrix: Matrix4): Ray = {
    val tmp = Vector3(origin.x + direction.x, origin.y + direction.y, origin.z + direction.z)
    tmp.mul(matrix)
    origin.mul(matrix)
    direction.set(tmp.x - origin.x, tmp.y - origin.y, tmp.z - origin.z).nor()
    this
  }

  /** {@inheritDoc} */
  override def toString: String =
    s"ray [$origin:$direction]"

  /** Sets the starting position and the direction of this ray.
    *
    * @param origin
    *   The starting position
    * @param direction
    *   The direction
    * @return
    *   this ray for chaining
    */
  def set(originVec: Vector3, directionVec: Vector3): Ray = {
    origin.set(originVec)
    direction.set(directionVec).nor()
    this
  }

  /** Sets this ray from the given starting position and direction.
    *
    * @param x
    *   The x-component of the starting position
    * @param y
    *   The y-component of the starting position
    * @param z
    *   The z-component of the starting position
    * @param dx
    *   The x-component of the direction
    * @param dy
    *   The y-component of the direction
    * @param dz
    *   The z-component of the direction
    * @return
    *   this ray for chaining
    */
  def set(x: Float, y: Float, z: Float, dx: Float, dy: Float, dz: Float): Ray = {
    origin.set(x, y, z)
    direction.set(dx, dy, dz).nor()
    this
  }

  /** Sets the starting position and direction from the given ray
    *
    * @param ray
    *   The ray
    * @return
    *   This ray for chaining
    */
  def set(ray: Ray): Ray = {
    origin.set(ray.origin)
    direction.set(ray.direction).nor()
    this
  }

  override def equals(obj: Any): Boolean = obj match {
    case r: Ray => direction.equals(r.direction) && origin.equals(r.origin)
    case _ => false
  }

  override def hashCode(): Int = {
    val prime  = 73
    var result = 1
    result = prime * result + direction.hashCode()
    result = prime * result + origin.hashCode()
    result
  }
}
