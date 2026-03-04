/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Polygon.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: getVertices, getTransformedVertices, setOrigin,
 * setPosition, setVertices, setVertex, translate, setRotation, rotate, setScale, scale,
 * dirty, area, getVertexCount, getVertex, getCentroid, getBoundingRectangle, contains(2),
 * getX/Y, getOriginX/Y, getRotation, getScaleX/Y, resetTransformations.
 */
package sge
package math

import sge.utils.Nullable

/** Encapsulates a 2D polygon defined by it's vertices relative to an origin point (default of 0, 0). */
class Polygon() extends Shape2D {
  private var localVertices: Array[Float] = Array.empty[Float]
  private var worldVertices: Array[Float] = scala.compiletime.uninitialized
  private var x:             Float        = 0f
  private var y:             Float        = 0f
  private var originX:       Float        = 0f
  private var originY:       Float        = 0f
  private var rotation:      Float        = 0f
  private var scaleX:        Float        = 1f
  private var scaleY:        Float        = 1f
  private var isDirty:       Boolean      = true
  private var bounds:        Rectangle    = scala.compiletime.uninitialized

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
  def getVertices: Array[Float] = localVertices

  /** Calculates and returns the vertices of the polygon after scaling, rotation, and positional translations have been applied, as they are position within the world.
    *
    * @return
    *   vertices scaled, rotated, and offset by the polygon position.
    */
  def getTransformedVertices: Array[Float] =
    if (!isDirty) worldVertices
    else {
      isDirty = false

      val localVertices = this.localVertices
      if (Nullable(worldVertices).fold(true)(_.length != localVertices.length))
        worldVertices = Array.ofDim[Float](localVertices.length)
      val positionX = x
      val positionY = y
      val originX   = this.originX
      val originY   = this.originY
      val scaleX    = this.scaleX
      val scaleY    = this.scaleY
      val scale     = scaleX != 1 || scaleY != 1
      val rotation  = this.rotation
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
    this.originX = originX
    this.originY = originY
    isDirty = true
  }

  /** Sets the polygon's position within the world. */
  def setPosition(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y
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
    this.x += x
    this.y += y
    isDirty = true
  }

  /** Sets the polygon to be rotated by the supplied degrees. */
  def setRotation(degrees: Float): Unit = {
    this.rotation = degrees
    isDirty = true
  }

  /** Applies additional rotation to the polygon by the supplied degrees. */
  def rotate(degrees: Float): Unit = {
    rotation += degrees
    isDirty = true
  }

  /** Sets the amount of scaling to be applied to the polygon. */
  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this.scaleX = scaleX
    this.scaleY = scaleY
    isDirty = true
  }

  /** Applies additional scaling to the polygon by the supplied amount. */
  def scale(amount: Float): Unit = {
    this.scaleX += amount
    this.scaleY += amount
    isDirty = true
  }

  /** Sets the polygon's world vertices to be recalculated when calling {@link #getTransformedVertices() getTransformedVertices} .
    */
  def dirty(): Unit =
    isDirty = true

  /** Returns the area contained within the polygon. */
  def area(): Float = {
    val vertices = getTransformedVertices
    GeometryUtils.polygonArea(vertices, 0, vertices.length)
  }

  def getVertexCount: Int =
    this.localVertices.length / 2

  /** @return Position(transformed) of vertex */
  def getVertex(vertexNum: Int, pos: Vector2): Vector2 = {
    if (vertexNum < 0 || vertexNum > getVertexCount)
      throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist")
    val vertices = this.getTransformedVertices
    pos.set(vertices(2 * vertexNum), vertices(2 * vertexNum + 1))
  }

  def getCentroid(centroid: Vector2): Vector2 = {
    val vertices = getTransformedVertices
    GeometryUtils.polygonCentroid(vertices, 0, vertices.length, centroid)
  }

  /** Returns an axis-aligned bounding box of this polygon.
    *
    * Note the returned Rectangle is cached in this polygon, and will be reused if this Polygon is changed.
    *
    * @return
    *   this polygon's bounding box {@link Rectangle}
    */
  def getBoundingRectangle: Rectangle = {
    val vertices = getTransformedVertices

    var minX = vertices(0)
    var minY = vertices(1)
    var maxX = vertices(0)
    var maxY = vertices(1)

    val numFloats = vertices.length
    var i         = 2
    while (i < numFloats) {
      minX = if (minX > vertices(i)) vertices(i) else minX
      minY = if (minY > vertices(i + 1)) vertices(i + 1) else minY
      maxX = if (maxX < vertices(i)) vertices(i) else maxX
      maxY = if (maxY < vertices(i + 1)) vertices(i + 1) else maxY
      i += 2
    }

    if (Nullable(bounds).isEmpty) bounds = new Rectangle()
    bounds.x = minX
    bounds.y = minY
    bounds.width = maxX - minX
    bounds.height = maxY - minY

    bounds
  }

  /** Returns whether an x, y pair is contained within the polygon. */
  override def contains(x: Float, y: Float): Boolean = {
    val vertices   = getTransformedVertices
    val numFloats  = vertices.length
    var intersects = 0

    var i = 0
    while (i < numFloats) {
      val x1 = vertices(i)
      val y1 = vertices(i + 1)
      val x2 = vertices((i + 2) % numFloats)
      val y2 = vertices((i + 3) % numFloats)
      if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1))
        intersects += 1
      i += 2
    }
    (intersects & 1) == 1
  }

  override def contains(point: Vector2): Boolean =
    contains(point.x, point.y)

  /** Returns the x-coordinate of the polygon's position within the world. */
  def getX: Float = x

  /** Returns the y-coordinate of the polygon's position within the world. */
  def getY: Float = y

  /** Returns the x-coordinate of the polygon's origin point. */
  def getOriginX: Float = originX

  /** Returns the y-coordinate of the polygon's origin point. */
  def getOriginY: Float = originY

  /** Returns the total rotation applied to the polygon. */
  def getRotation: Float = rotation

  /** Returns the total horizontal scaling applied to the polygon. */
  def getScaleX: Float = scaleX

  /** Returns the total vertical scaling applied to the polygon. */
  def getScaleY: Float = scaleY

  def resetTransformations(): Unit = {
    scaleX = 1
    scaleY = 1
    originX = 0
    originY = 0
    x = 0
    y = 0
    rotation = 0
    isDirty = true
  }
}
