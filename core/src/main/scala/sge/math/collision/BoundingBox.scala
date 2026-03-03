/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/collision/BoundingBox.java
 * Original authors: badlogicgames@gmail.com, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math
package collision

import sge.math.{ Matrix4, Vector3 }

/** Encapsulates an axis aligned bounding box represented by a minimum and a maximum Vector. Additionally you can query for the bounding box's center, dimensions and corner points.
  *
  * @author
  *   badlogicgames@gmail.com, Xoppa (original implementation)
  */
class BoundingBox() {

  /** Minimum vector. All XYZ components should be inferior to corresponding {@link #max} components. Call {@link #update()} if you manually change this vector.
    */
  val min = new Vector3()

  /** Maximum vector. All XYZ components should be superior to corresponding {@link #min} components. Call {@link #update()} if you manually change this vector.
    */
  val max = new Vector3()

  private val cnt = new Vector3()
  private val dim = new Vector3()

  // Initialize
  clr()

  /** Constructs a new bounding box from the given bounding box.
    *
    * @param bounds
    *   The bounding box to copy
    */
  def this(bounds: BoundingBox) = {
    this()
    this.set(bounds)
  }

  /** Constructs the new bounding box using the given minimum and maximum vector.
    *
    * @param minimum
    *   The minimum vector
    * @param maximum
    *   The maximum vector
    */
  def this(minimum: Vector3, maximum: Vector3) = {
    this()
    this.set(minimum, maximum)
  }

  /** @param out
    *   The {@link Vector3} to receive the center of the bounding box.
    * @return
    *   The vector specified with the out argument.
    */
  def getCenter(out: Vector3): Vector3 = out.set(cnt)

  def getCenterX(): Float = cnt.x

  def getCenterY(): Float = cnt.y

  def getCenterZ(): Float = cnt.z

  def getCorner000(out: Vector3): Vector3 = out.set(min.x, min.y, min.z)

  def getCorner001(out: Vector3): Vector3 = out.set(min.x, min.y, max.z)

  def getCorner010(out: Vector3): Vector3 = out.set(min.x, max.y, min.z)

  def getCorner011(out: Vector3): Vector3 = out.set(min.x, max.y, max.z)

  def getCorner100(out: Vector3): Vector3 = out.set(max.x, min.y, min.z)

  def getCorner101(out: Vector3): Vector3 = out.set(max.x, min.y, max.z)

  def getCorner110(out: Vector3): Vector3 = out.set(max.x, max.y, min.z)

  def getCorner111(out: Vector3): Vector3 = out.set(max.x, max.y, max.z)

  /** @param out
    *   The {@link Vector3} to receive the dimensions of this bounding box on all three axis.
    * @return
    *   The vector specified with the out argument
    */
  def getDimensions(out: Vector3): Vector3 = out.set(dim)

  def getWidth(): Float = dim.x

  def getHeight(): Float = dim.y

  def getDepth(): Float = dim.z

  /** @param out
    *   The {@link Vector3} to receive the minimum values.
    * @return
    *   The vector specified with the out argument
    */
  def getMin(out: Vector3): Vector3 = out.set(min)

  /** @param out
    *   The {@link Vector3} to receive the maximum values.
    * @return
    *   The vector specified with the out argument
    */
  def getMax(out: Vector3): Vector3 = out.set(max)

  /** Sets the given bounding box.
    *
    * @param bounds
    *   The bounds.
    * @return
    *   This bounding box for chaining.
    */
  def set(bounds: BoundingBox): BoundingBox = this.set(bounds.min, bounds.max)

  /** Sets the given minimum and maximum vector.
    *
    * @param minimum
    *   The minimum vector
    * @param maximum
    *   The maximum vector
    * @return
    *   This bounding box for chaining.
    */
  def set(minimum: Vector3, maximum: Vector3): BoundingBox = {
    min.set(
      if (minimum.x < maximum.x) minimum.x else maximum.x,
      if (minimum.y < maximum.y) minimum.y else maximum.y,
      if (minimum.z < maximum.z) minimum.z else maximum.z
    )
    max.set(
      if (minimum.x > maximum.x) minimum.x else maximum.x,
      if (minimum.y > maximum.y) minimum.y else maximum.y,
      if (minimum.z > maximum.z) minimum.z else maximum.z
    )
    update()
    this
  }

  /** Should be called if you modify {@link #min} and/or {@link #max} vectors manually. */
  def update(): Unit = {
    cnt.set(min).+(max).scale(0.5f)
    dim.set(max).-(min)
  }

  /** Sets the bounding box minimum and maximum vector from the given points.
    *
    * @param points
    *   The points.
    * @return
    *   This bounding box for chaining.
    */
  def set(points: Array[Vector3]): BoundingBox = {
    this.inf()
    for (point <- points)
      this.ext(point)
    this
  }

  /** Sets the bounding box minimum and maximum vector from the given points.
    *
    * @param points
    *   The points.
    * @return
    *   This bounding box for chaining.
    */
  def set(points: List[Vector3]): BoundingBox = {
    this.inf()
    for (point <- points)
      this.ext(point)
    this
  }

  /** Sets the minimum and maximum vector to positive and negative infinity.
    *
    * @return
    *   This bounding box for chaining.
    */
  def inf(): BoundingBox = {
    min.set(Float.PositiveInfinity, Float.PositiveInfinity, Float.PositiveInfinity)
    max.set(Float.NegativeInfinity, Float.NegativeInfinity, Float.NegativeInfinity)
    cnt.set(0, 0, 0)
    dim.set(0, 0, 0)
    this
  }

  /** Extends the bounding box to incorporate the given {@link Vector3} .
    * @param point
    *   The vector
    * @return
    *   This bounding box for chaining.
    */
  def ext(point: Vector3): BoundingBox =
    this.set(
      min.set(Math.min(min.x, point.x), Math.min(min.y, point.y), Math.min(min.z, point.z)),
      max.set(Math.max(max.x, point.x), Math.max(max.y, point.y), Math.max(max.z, point.z))
    )

  /** Sets the minimum and maximum vector to zeros.
    * @return
    *   This bounding box for chaining.
    */
  def clr(): BoundingBox = this.set(min.set(0, 0, 0), max.set(0, 0, 0))

  /** Returns whether this bounding box is valid. This means that {@link #max} is greater than or equal to {@link #min} .
    * @return
    *   True in case the bounding box is valid, false otherwise
    */
  def isValid(): Boolean = min.x <= max.x && min.y <= max.y && min.z <= max.z

  /** Extends this bounding box by the given bounding box.
    *
    * @param bounds
    *   The bounding box
    * @return
    *   This bounding box for chaining.
    */
  def ext(bounds: BoundingBox): BoundingBox =
    this.set(
      min.set(Math.min(min.x, bounds.min.x), Math.min(min.y, bounds.min.y), Math.min(min.z, bounds.min.z)),
      max.set(Math.max(max.x, bounds.max.x), Math.max(max.y, bounds.max.y), Math.max(max.z, bounds.max.z))
    )

  /** Extends this bounding box by the given sphere.
    *
    * @param center
    *   Sphere center
    * @param radius
    *   Sphere radius
    * @return
    *   This bounding box for chaining.
    */
  def ext(center: Vector3, radius: Float): BoundingBox =
    this.set(
      min.set(Math.min(min.x, center.x - radius), Math.min(min.y, center.y - radius), Math.min(min.z, center.z - radius)),
      max.set(Math.max(max.x, center.x + radius), Math.max(max.y, center.y + radius), Math.max(max.z, center.z + radius))
    )

  /** Extends this bounding box by the given transformed bounding box.
    *
    * @param bounds
    *   The bounding box
    * @param transform
    *   The transformation matrix to apply to bounds, before using it to extend this bounding box.
    * @return
    *   This bounding box for chaining.
    */
  def ext(bounds: BoundingBox, transform: Matrix4): BoundingBox = {
    ext(BoundingBox.tmpVector.set(bounds.min.x, bounds.min.y, bounds.min.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.min.x, bounds.min.y, bounds.max.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.min.x, bounds.max.y, bounds.min.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.min.x, bounds.max.y, bounds.max.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.max.x, bounds.min.y, bounds.min.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.max.x, bounds.min.y, bounds.max.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.max.x, bounds.max.y, bounds.min.z).mul(transform))
    ext(BoundingBox.tmpVector.set(bounds.max.x, bounds.max.y, bounds.max.z).mul(transform))
    this
  }

  /** Multiplies the bounding box by the given matrix. This is achieved by multiplying the 8 corner points and then calculating the minimum and maximum vectors from the transformed points.
    *
    * @param transform
    *   The matrix
    * @return
    *   This bounding box for chaining.
    */
  def mul(transform: Matrix4): BoundingBox = {
    val x0 = min.x
    val y0 = min.y
    val z0 = min.z
    val x1 = max.x
    val y1 = max.y
    val z1 = max.z
    inf()
    ext(BoundingBox.tmpVector.set(x0, y0, z0).mul(transform))
    ext(BoundingBox.tmpVector.set(x0, y0, z1).mul(transform))
    ext(BoundingBox.tmpVector.set(x0, y1, z0).mul(transform))
    ext(BoundingBox.tmpVector.set(x0, y1, z1).mul(transform))
    ext(BoundingBox.tmpVector.set(x1, y0, z0).mul(transform))
    ext(BoundingBox.tmpVector.set(x1, y0, z1).mul(transform))
    ext(BoundingBox.tmpVector.set(x1, y1, z0).mul(transform))
    ext(BoundingBox.tmpVector.set(x1, y1, z1).mul(transform))
    this
  }

  /** Returns whether the given bounding box is contained in this bounding box.
    * @param b
    *   The bounding box
    * @return
    *   Whether the given bounding box is contained
    */
  def contains(b: BoundingBox): Boolean =
    (!isValid()) || (min.x <= b.min.x && min.y <= b.min.y && min.z <= b.min.z && max.x >= b.max.x && max.y >= b.max.y && max.z >= b.max.z)

  /** Returns whether the given vector is contained in this bounding box.
    * @param v
    *   The vector
    * @return
    *   Whether the vector is contained or not.
    */
  def contains(v: Vector3): Boolean = contains(v.x, v.y, v.z)

  /** Returns whether the given vector is contained in this bounding box.
    * @param x
    *   The x-component of the vector
    * @param y
    *   The y-component of the vector
    * @param z
    *   The z-component of the vector
    * @return
    *   Whether the vector is contained or not.
    */
  def contains(x: Float, y: Float, z: Float): Boolean =
    min.x <= x && max.x >= x && min.y <= y && max.y >= y && min.z <= z && max.z >= z

  /** Returns whether this bounding box intersects the given bounding box.
    * @param bounds
    *   The bounding box to test
    * @return
    *   Whether the two bounding boxes intersect
    */
  def intersects(bounds: BoundingBox): Boolean =
    if (!isValid() || !bounds.isValid()) false
    else
      !(min.x > bounds.max.x || max.x < bounds.min.x ||
        min.y > bounds.max.y || max.y < bounds.min.y ||
        min.z > bounds.max.z || max.z < bounds.min.z)

  override def toString(): String = s"[${min.toString()}|${max.toString()}]"

  /** Extends the bounding box by the given vector.
    *
    * @param x
    *   The x-component
    * @param y
    *   The y-component
    * @param z
    *   The z-component
    * @return
    *   This bounding box for chaining.
    */
  def ext(x: Float, y: Float, z: Float): BoundingBox =
    this.set(
      min.set(scala.math.min(min.x, x), scala.math.min(min.y, y), scala.math.min(min.z, z)),
      max.set(scala.math.max(max.x, x), scala.math.max(max.y, y), scala.math.max(max.z, z))
    )
}

object BoundingBox {
  private val tmpVector = new Vector3()
}
