/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/collision/OrientedBoundingBox.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: Serializable dropped; serialVersionUID dropped;
 *     static arrays (tempAxes, tempVertices, tmpVectors) moved to companion object;
 *     Java init() replaced with inline field initializers + class body update() call
 *   Renames: Intersector.hasOverlap called via sge.math.Intersector.hasOverlap;
 *     getVertices -> vertices, getBounds -> bounds (public), getTransform -> deleted (transform already public val),
 *     getCorner* -> corner*
 *   Idiom: split packages
 *   Audited: 2026-03-04
 */
package sge
package math
package collision

import sge.math.{ Matrix4, Vector3 }

class OrientedBoundingBox() {

  /** Bounds used as size. */
  private val _bounds = BoundingBox()

  /** Transform matrix. */
  val transform                = Matrix4()
  private val inverseTransform = Matrix4()

  private val axes      = Array.fill(3)(Vector3())
  private val _vertices = Array.fill(8)(Vector3())

  // Initialize
  _bounds.clr()
  update()

  /** Constructs a new oriented bounding box from the given bounding box.
    *
    * @param bounds
    *   The bounding box to copy
    */
  def this(bounds: BoundingBox) = {
    this()
    this._bounds.set(bounds.min, bounds.max)
    update()
  }

  /** Constructs a new oriented bounding box from the given bounding box and transform.
    *
    * @param bounds
    *   The bounding box to copy
    * @param transform
    *   The transformation matrix to copy
    */
  def this(bounds: BoundingBox, transform: Matrix4) = {
    this()
    this._bounds.set(bounds.min, bounds.max)
    this.transform.set(transform)
    update()
  }

  def vertices: Array[Vector3] = _vertices

  /** Get the current bounds. Call {@link #update()} if you manually change this bounding box. */
  def bounds: BoundingBox = _bounds

  /** Sets the base bounds of the oriented bounding box as the bounds given, the transform is applied to the vertices.
    *
    * @param bounds
    *   The bounding box to copy
    */
  def setBounds(bounds: BoundingBox): Unit = {
    this._bounds.set(bounds)
    bounds.corner000(_vertices(0b000)).mul(transform)
    bounds.corner001(_vertices(0b001)).mul(transform)
    bounds.corner010(_vertices(0b010)).mul(transform)
    bounds.corner011(_vertices(0b011)).mul(transform)
    bounds.corner100(_vertices(0b100)).mul(transform)
    bounds.corner101(_vertices(0b101)).mul(transform)
    bounds.corner110(_vertices(0b110)).mul(transform)
    bounds.corner111(_vertices(0b111)).mul(transform)
  }

  def setTransform(transform: Matrix4): Unit = {
    this.transform.set(transform)
    update()
  }

  def set(bounds: BoundingBox, transform: Matrix4): OrientedBoundingBox = {
    setBounds(bounds)
    setTransform(transform)
    this
  }

  def corner000(out: Vector3): Vector3 = out.set(_vertices(0b000))

  def corner001(out: Vector3): Vector3 = out.set(_vertices(0b001))

  def corner010(out: Vector3): Vector3 = out.set(_vertices(0b010))

  def corner011(out: Vector3): Vector3 = out.set(_vertices(0b011))

  def corner100(out: Vector3): Vector3 = out.set(_vertices(0b100))

  def corner101(out: Vector3): Vector3 = out.set(_vertices(0b101))

  def corner110(out: Vector3): Vector3 = out.set(_vertices(0b110))

  def corner111(out: Vector3): Vector3 = out.set(_vertices(0b111))

  /** Returns whether the given vector is contained in this oriented bounding box.
    * @param v
    *   The vector
    * @return
    *   Whether the vector is contained or not.
    */
  def contains(v: Vector3): Boolean = contains(v, inverseTransform)

  private def contains(v: Vector3, invTransform: Matrix4): Boolean = {
    val localV = OrientedBoundingBox.tmpVectors(0).set(v).mul(invTransform)
    _bounds.contains(localV)
  }

  /** Returns whether the given bounding box is contained in this oriented bounding box.
    * @param b
    *   The bounding box
    * @return
    *   Whether the given bounding box is contained
    */
  def contains(b: BoundingBox): Boolean = {
    val tmpVector = OrientedBoundingBox.tmpVectors(0)
    contains(b.corner000(tmpVector), inverseTransform) && contains(b.corner001(tmpVector), inverseTransform) &&
    contains(b.corner010(tmpVector), inverseTransform) && contains(b.corner011(tmpVector), inverseTransform) &&
    contains(b.corner100(tmpVector), inverseTransform) && contains(b.corner101(tmpVector), inverseTransform) &&
    contains(b.corner110(tmpVector), inverseTransform) && contains(b.corner111(tmpVector), inverseTransform)
  }

  /** Returns whether the given oriented bounding box is contained in this oriented bounding box.
    * @param obb
    *   The oriented bounding box
    * @return
    *   Whether the given oriented bounding box is contained
    */
  def contains(obb: OrientedBoundingBox): Boolean =
    contains(obb.corner000(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner001(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner010(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner011(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner100(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner101(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner110(OrientedBoundingBox.tmpVectors(0)), inverseTransform) &&
      contains(obb.corner111(OrientedBoundingBox.tmpVectors(0)), inverseTransform)

  /** Returns whether the given bounding box is intersecting this oriented bounding box (at least one point in).
    * @param b
    *   The bounding box
    * @return
    *   Whether the given bounding box is intersected
    */
  def intersects(b: BoundingBox): Boolean = {
    val aAxes = axes

    OrientedBoundingBox.tempAxes(0) = aAxes(0)
    OrientedBoundingBox.tempAxes(1) = aAxes(1)
    OrientedBoundingBox.tempAxes(2) = aAxes(2)
    OrientedBoundingBox.tempAxes(3) = Vector3.X
    OrientedBoundingBox.tempAxes(4) = Vector3.Y
    OrientedBoundingBox.tempAxes(5) = Vector3.Z
    OrientedBoundingBox.tempAxes(6) = OrientedBoundingBox.tmpVectors(0).set(aAxes(0)).crs(Vector3.X)
    OrientedBoundingBox.tempAxes(7) = OrientedBoundingBox.tmpVectors(1).set(aAxes(0)).crs(Vector3.Y)
    OrientedBoundingBox.tempAxes(8) = OrientedBoundingBox.tmpVectors(2).set(aAxes(0)).crs(Vector3.Z)
    OrientedBoundingBox.tempAxes(9) = OrientedBoundingBox.tmpVectors(3).set(aAxes(1)).crs(Vector3.X)
    OrientedBoundingBox.tempAxes(10) = OrientedBoundingBox.tmpVectors(4).set(aAxes(1)).crs(Vector3.Y)
    OrientedBoundingBox.tempAxes(11) = OrientedBoundingBox.tmpVectors(5).set(aAxes(1)).crs(Vector3.Z)
    OrientedBoundingBox.tempAxes(12) = OrientedBoundingBox.tmpVectors(6).set(aAxes(2)).crs(Vector3.X)
    OrientedBoundingBox.tempAxes(13) = OrientedBoundingBox.tmpVectors(7).set(aAxes(2)).crs(Vector3.Y)
    OrientedBoundingBox.tempAxes(14) = OrientedBoundingBox.tmpVectors(8).set(aAxes(2)).crs(Vector3.Z)

    val aVertices = _vertices
    val bVertices = extractVertices(b)

    sge.math.Intersector.hasOverlap(OrientedBoundingBox.tempAxes, aVertices, bVertices)
  }

  /** Returns whether the given oriented bounding box is intersecting this oriented bounding box (at least one point in).
    * @param obb
    *   The oriented bounding box
    * @return
    *   Whether the given bounding box is intersected
    */
  def intersects(obb: OrientedBoundingBox): Boolean = {
    val aAxes = axes
    val bAxes = obb.axes

    OrientedBoundingBox.tempAxes(0) = aAxes(0)
    OrientedBoundingBox.tempAxes(1) = aAxes(1)
    OrientedBoundingBox.tempAxes(2) = aAxes(2)
    OrientedBoundingBox.tempAxes(3) = bAxes(0)
    OrientedBoundingBox.tempAxes(4) = bAxes(1)
    OrientedBoundingBox.tempAxes(5) = bAxes(2)
    OrientedBoundingBox.tempAxes(6) = OrientedBoundingBox.tmpVectors(0).set(aAxes(0)).crs(bAxes(0))
    OrientedBoundingBox.tempAxes(7) = OrientedBoundingBox.tmpVectors(1).set(aAxes(0)).crs(bAxes(1))
    OrientedBoundingBox.tempAxes(8) = OrientedBoundingBox.tmpVectors(2).set(aAxes(0)).crs(bAxes(2))
    OrientedBoundingBox.tempAxes(9) = OrientedBoundingBox.tmpVectors(3).set(aAxes(1)).crs(bAxes(0))
    OrientedBoundingBox.tempAxes(10) = OrientedBoundingBox.tmpVectors(4).set(aAxes(1)).crs(bAxes(1))
    OrientedBoundingBox.tempAxes(11) = OrientedBoundingBox.tmpVectors(5).set(aAxes(1)).crs(bAxes(2))
    OrientedBoundingBox.tempAxes(12) = OrientedBoundingBox.tmpVectors(6).set(aAxes(2)).crs(bAxes(0))
    OrientedBoundingBox.tempAxes(13) = OrientedBoundingBox.tmpVectors(7).set(aAxes(2)).crs(bAxes(1))
    OrientedBoundingBox.tempAxes(14) = OrientedBoundingBox.tmpVectors(8).set(aAxes(2)).crs(bAxes(2))

    sge.math.Intersector.hasOverlap(OrientedBoundingBox.tempAxes, _vertices, obb._vertices)
  }

  def mul(transform: Matrix4): Unit = {
    this.transform.mul(transform)
    update()
  }

  private def update(): Unit = {
    // Update vertices
    _bounds.corner000(_vertices(0b000)).mul(transform)
    _bounds.corner001(_vertices(0b001)).mul(transform)
    _bounds.corner010(_vertices(0b010)).mul(transform)
    _bounds.corner011(_vertices(0b011)).mul(transform)
    _bounds.corner100(_vertices(0b100)).mul(transform)
    _bounds.corner101(_vertices(0b101)).mul(transform)
    _bounds.corner110(_vertices(0b110)).mul(transform)
    _bounds.corner111(_vertices(0b111)).mul(transform)

    // Update axes by extracting matrix columns (not multiplying unit vectors, which includes translation)
    val v = transform.values
    axes(0).set(v(Matrix4.M00), v(Matrix4.M10), v(Matrix4.M20)).nor()
    axes(1).set(v(Matrix4.M01), v(Matrix4.M11), v(Matrix4.M21)).nor()
    axes(2).set(v(Matrix4.M02), v(Matrix4.M12), v(Matrix4.M22)).nor()

    inverseTransform.set(transform).inv()
  }

  private def extractVertices(b: BoundingBox): Array[Vector3] = {
    OrientedBoundingBox.tempVertices(0) = b.corner000(OrientedBoundingBox.tempVertices(0))
    OrientedBoundingBox.tempVertices(1) = b.corner001(OrientedBoundingBox.tempVertices(1))
    OrientedBoundingBox.tempVertices(2) = b.corner010(OrientedBoundingBox.tempVertices(2))
    OrientedBoundingBox.tempVertices(3) = b.corner011(OrientedBoundingBox.tempVertices(3))
    OrientedBoundingBox.tempVertices(4) = b.corner100(OrientedBoundingBox.tempVertices(4))
    OrientedBoundingBox.tempVertices(5) = b.corner101(OrientedBoundingBox.tempVertices(5))
    OrientedBoundingBox.tempVertices(6) = b.corner110(OrientedBoundingBox.tempVertices(6))
    OrientedBoundingBox.tempVertices(7) = b.corner111(OrientedBoundingBox.tempVertices(7))
    OrientedBoundingBox.tempVertices
  }
}

object OrientedBoundingBox {
  private val tempAxes     = Array.fill(15)(Vector3())
  private val tempVertices = Array.fill(8)(Vector3())
  private val tmpVectors   = Array.fill(9)(Vector3())
}
