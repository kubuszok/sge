/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Polygon.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Renames: getVertices -> vertices, getTransformedVertices -> transformedVertices,
 *     getVertexCount -> vertexCount, getVertex -> vertex, getCentroid -> centroid,
 *     getBoundingRectangle -> boundingRectangle, getX/Y -> x/y, getOriginX/Y -> originX/Y,
 *     getRotation -> rotation, getScaleX/Y -> scaleX/Y (all via _ backing fields)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: vertices, transformedVertices, setOrigin,
 * setPosition, setVertices, setVertex, translate, setRotation, rotate, setScale, scale,
 * dirty, area, vertexCount, vertex, centroid, boundingRectangle, contains(2),
 * x/y, originX/Y, rotation, scaleX/Y, resetTransformations.
 */
package sge
package math

import sge.utils.Nullable

/** Encapsulates a 2D polygon defined by it's vertices relative to an origin point (default of 0, 0). */
class Polygon() extends Shape2D {
  private var localVertices: Array[Float] = Array.empty[Float]
  private var worldVertices: Array[Float] = scala.compiletime.uninitialized
  private var _x:            Float        = 0f
  private var _y:            Float        = 0f
  private var _originX:      Float        = 0f
  private var _originY:      Float        = 0f
  private var _rotation:     Float        = 0f
  private var _scaleX:       Float        = 1f
  private var _scaleY:       Float        = 1f
  private var isDirty:       Boolean      = true
  private var _bounds:       Rectangle    = scala.compiletime.uninitialized

  /** Constructs a new polygon from a float array of parts of vertex points.
    *
    * @param vertices
    *   an array where every even element represents the horizontal part of a point, and the following element representing the vertical part
    *
    * @throws IllegalArgumentException
    *   if less than 6 elements, representing 3 points, are provided
    */
  def this(vertices: Array[Float]) = {
    this()
    if (vertices.length < 6) throw new IllegalArgumentException("polygons must contain at least 3 points.")
    this.localVertices = vertices
  }

  /** Returns the polygon's local vertices without scaling or rotation and without being offset by the polygon position. */
  def vertices: Array[Float] = localVertices

  /** Calculates and returns the vertices of the polygon after scaling, rotation, and positional translations have been applied, as they are position within the world.
    *
    * @return
    *   vertices scaled, rotated, and offset by the polygon position.
    */
  def transformedVertices: Array[Float] =
    if (!isDirty) worldVertices
    else {
      isDirty = false

      val localVertices = this.localVertices
      if (Nullable(worldVertices).forall(_.length != localVertices.length))
        worldVertices = Array.ofDim[Float](localVertices.length)
      val positionX = _x
      val positionY = _y
      val originX   = this._originX
      val originY   = this._originY
      val scaleX    = this._scaleX
      val scaleY    = this._scaleY
      val scale     = scaleX != 1 || scaleY != 1
      val rotation  = this._rotation
      val cos       = MathUtils.cosDeg(rotation)
      val sin       = MathUtils.sinDeg(rotation)

      var i = 0
      val n = localVertices.length
      while (i < n) {
        var x = localVertices(i) - originX
        var y = localVertices(i + 1) - originY

        // scale if needed
        if (scale) {
          x *= scaleX
          y *= scaleY
        }

        // rotate if needed
        if (rotation != 0) {
          val oldX = x
          x = cos * x - sin * y
          y = sin * oldX + cos * y
        }

        worldVertices(i) = positionX + x + originX
        worldVertices(i + 1) = positionY + y + originY
        i += 2
      }
      worldVertices
    }

  /** Sets the origin point to which all of the polygon's local vertices are relative to. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this._originX = originX
    this._originY = originY
    isDirty = true
  }

  /** Sets the polygon's position within the world. */
  def setPosition(x: Float, y: Float): Unit = {
    this._x = x
    this._y = y
    isDirty = true
  }

  /** Sets the polygon's local vertices relative to the origin point, without any scaling, rotating or translations being applied.
    *
    * @param vertices
    *   float array where every even element represents the x-coordinate of a vertex, and the proceeding element representing the y-coordinate.
    * @throws IllegalArgumentException
    *   if less than 6 elements, representing 3 points, are provided
    */
  def setVertices(vertices: Array[Float]): Unit = {
    if (vertices.length < 6) throw new IllegalArgumentException("polygons must contain at least 3 points.")
    localVertices = vertices
    isDirty = true
  }

  /** Set vertex position
    * @param vertexNum
    *   min=0, max=vertices.length/2-1
    * @throws IllegalArgumentException
    *   if vertex doesnt exist
    */
  def setVertex(vertexNum: Int, x: Float, y: Float): Unit = {
    if (vertexNum < 0 || vertexNum > localVertices.length / 2 - 1)
      throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist")
    localVertices(2 * vertexNum) = x
    localVertices(2 * vertexNum + 1) = y
    isDirty = true
  }

  /** Translates the polygon's position by the specified horizontal and vertical amounts. */
  def translate(x: Float, y: Float): Unit = {
    this._x += x
    this._y += y
    isDirty = true
  }

  /** Sets the polygon to be rotated by the supplied degrees. */
  def setRotation(degrees: Float): Unit = {
    this._rotation = degrees
    isDirty = true
  }

  /** Applies additional rotation to the polygon by the supplied degrees. */
  def rotate(degrees: Float): Unit = {
    _rotation += degrees
    isDirty = true
  }

  /** Sets the amount of scaling to be applied to the polygon. */
  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this._scaleX = scaleX
    this._scaleY = scaleY
    isDirty = true
  }

  /** Applies additional scaling to the polygon by the supplied amount. */
  def scale(amount: Float): Unit = {
    this._scaleX += amount
    this._scaleY += amount
    isDirty = true
  }

  /** Sets the polygon's world vertices to be recalculated when calling {@link #transformedVertices transformedVertices} .
    */
  def dirty(): Unit =
    isDirty = true

  /** Returns the area contained within the polygon. */
  def area(): Float = {
    val verts = transformedVertices
    GeometryUtils.polygonArea(verts, 0, verts.length)
  }

  def vertexCount: Int =
    this.localVertices.length / 2

  /** @return Position(transformed) of vertex */
  def vertex(vertexNum: Int, pos: Vector2): Vector2 = {
    if (vertexNum < 0 || vertexNum > vertexCount)
      throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist")
    val verts = this.transformedVertices
    pos.set(verts(2 * vertexNum), verts(2 * vertexNum + 1))
  }

  def centroid(centroid: Vector2): Vector2 = {
    val verts = transformedVertices
    GeometryUtils.polygonCentroid(verts, 0, verts.length, centroid)
  }

  /** Returns an axis-aligned bounding box of this polygon.
    *
    * Note the returned Rectangle is cached in this polygon, and will be reused if this Polygon is changed.
    *
    * @return
    *   this polygon's bounding box {@link Rectangle}
    */
  def boundingRectangle: Rectangle = {
    val verts = transformedVertices

    var minX = verts(0)
    var minY = verts(1)
    var maxX = verts(0)
    var maxY = verts(1)

    val numFloats = verts.length
    var i         = 2
    while (i < numFloats) {
      minX = if (minX > verts(i)) verts(i) else minX
      minY = if (minY > verts(i + 1)) verts(i + 1) else minY
      maxX = if (maxX < verts(i)) verts(i) else maxX
      maxY = if (maxY < verts(i + 1)) verts(i + 1) else maxY
      i += 2
    }

    if (Nullable(_bounds).isEmpty) _bounds = Rectangle()
    _bounds.x = minX
    _bounds.y = minY
    _bounds.width = maxX - minX
    _bounds.height = maxY - minY

    _bounds
  }

  /** Returns whether an x, y pair is contained within the polygon. */
  override def contains(x: Float, y: Float): Boolean = {
    val verts      = transformedVertices
    val numFloats  = verts.length
    var intersects = 0

    var i = 0
    while (i < numFloats) {
      val x1 = verts(i)
      val y1 = verts(i + 1)
      val x2 = verts((i + 2) % numFloats)
      val y2 = verts((i + 3) % numFloats)
      if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1))
        intersects += 1
      i += 2
    }
    (intersects & 1) == 1
  }

  override def contains(point: Vector2): Boolean =
    contains(point.x, point.y)

  /** Returns the x-coordinate of the polygon's position within the world. */
  def x: Float = _x

  /** Returns the y-coordinate of the polygon's position within the world. */
  def y: Float = _y

  /** Returns the x-coordinate of the polygon's origin point. */
  def originX: Float = _originX

  /** Returns the y-coordinate of the polygon's origin point. */
  def originY: Float = _originY

  /** Returns the total rotation applied to the polygon. */
  def rotation: Float = _rotation

  /** Returns the total horizontal scaling applied to the polygon. */
  def scaleX: Float = _scaleX

  /** Returns the total vertical scaling applied to the polygon. */
  def scaleY: Float = _scaleY

  def resetTransformations(): Unit = {
    _scaleX = 1
    _scaleY = 1
    _originX = 0
    _originY = 0
    _x = 0
    _y = 0
    _rotation = 0
    isDirty = true
  }
}
