/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Polyline.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: replaced getX/Y, getOriginX/Y, getRotation, getScaleX/Y with public read accessors over private _x/_y fields;
 *     getVertices -> vertices, getTransformedVertices -> transformedVertices,
 *     getLength -> length, getScaledLength -> scaledLength, getBoundingRectangle -> boundingRectangle
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: getVertices, getTransformedVertices, getLength,
 * getScaledLength, x/y/originX/Y/rotation/scaleX/Y (read accessors), setOrigin, setPosition,
 * setVertices, setRotation, rotate, setScale, scale, calculateLength, calculateScaledLength,
 * dirty, translate, getBoundingRectangle, contains(2). Implements Shape2D (always false).
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 249
 * Covenant-baseline-methods: Polyline,_length,_originX,_originY,_rotation,_scaleX,_scaleY,_scaledLength,_x,_y,boundingRectangle,bounds,calculateLength,calculateScaledLength,contains,dirty,i,isDirty,length,localVertices,maxX,maxY,minX,minY,numFloats,originX,originY,rotate,rotation,scale,scaleX,scaleY,scaledLength,setOrigin,setPosition,setRotation,setScale,setVertices,shouldCalculateLength,shouldCalculateScaledLength,this,transformedVertices,translate,vertices,worldVertices,x,y
 * Covenant-source-reference: com/badlogic/gdx/math/Polyline.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

import sge.utils.Nullable

class Polyline() extends Shape2D {
  private var localVertices:               Array[Float] = Array.empty[Float]
  private var worldVertices:               Array[Float] = scala.compiletime.uninitialized
  private var _x:                          Float        = 0f
  private var _y:                          Float        = 0f
  private var _originX:                    Float        = 0f
  private var _originY:                    Float        = 0f
  private var _rotation:                   Float        = 0f
  private var _scaleX:                     Float        = 1f
  private var _scaleY:                     Float        = 1f
  private var _length:                     Float        = 0f
  private var _scaledLength:               Float        = 0f
  private var shouldCalculateScaledLength: Boolean      = true
  private var shouldCalculateLength:       Boolean      = true
  private var isDirty:                     Boolean      = true
  private var bounds:                      Rectangle    = scala.compiletime.uninitialized

  def this(vertices: Array[Float]) = {
    this()
    if (vertices.length < 4) throw new IllegalArgumentException("polylines must contain at least 2 points.")
    this.localVertices = vertices
  }

  /** Returns vertices without scaling or rotation and without being offset by the polyline position. */
  def vertices: Array[Float] =
    localVertices

  /** Returns vertices scaled, rotated, and offset by the polygon position. */
  def transformedVertices: Array[Float] =
    if (!isDirty) this.worldVertices
    else {
      isDirty = false

      val localVertices = this.localVertices
      if (Nullable(this.worldVertices).forall(_.length < localVertices.length))
        this.worldVertices = new Array[Float](localVertices.length)

      val worldVertices = this.worldVertices
      val positionX     = _x
      val positionY     = _y
      val originX       = this._originX
      val originY       = this._originY
      val scaleX        = this._scaleX
      val scaleY        = this._scaleY
      val scale         = scaleX != 1 || scaleY != 1
      val rotation      = this._rotation
      val cos           = MathUtils.cosDeg(rotation)
      val sin           = MathUtils.sinDeg(rotation)

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

  /** Returns the euclidean length of the polyline without scaling */
  def length: Float =
    if (!shouldCalculateLength) _length
    else {
      shouldCalculateLength = false

      _length = 0
      var i = 0
      val n = localVertices.length - 2
      while (i < n) {
        val x = localVertices(i + 2) - localVertices(i)
        val y = localVertices(i + 1) - localVertices(i + 3)
        _length += Math.sqrt(x * x + y * y).toFloat
        i += 2
      }

      _length
    }

  /** Returns the euclidean length of the polyline */
  def scaledLength: Float =
    if (!shouldCalculateScaledLength) _scaledLength
    else {
      shouldCalculateScaledLength = false

      _scaledLength = 0
      var i = 0
      val n = localVertices.length - 2
      while (i < n) {
        val x = localVertices(i + 2) * _scaleX - localVertices(i) * _scaleX
        val y = localVertices(i + 1) * _scaleY - localVertices(i + 3) * _scaleY
        _scaledLength += Math.sqrt(x * x + y * y).toFloat
        i += 2
      }

      _scaledLength
    }

  def x: Float = _x

  def y: Float = _y

  def originX: Float = _originX

  def originY: Float = _originY

  def rotation: Float = _rotation

  def scaleX: Float = _scaleX

  def scaleY: Float = _scaleY

  def setOrigin(originX: Float, originY: Float): Unit = {
    this._originX = originX
    this._originY = originY
    isDirty = true
  }

  def setPosition(x: Float, y: Float): Unit = {
    this._x = x
    this._y = y
    isDirty = true
  }

  def setVertices(vertices: Array[Float]): Unit = {
    if (vertices.length < 4) throw new IllegalArgumentException("polylines must contain at least 2 points.")
    this.localVertices = vertices
    isDirty = true
  }

  def setRotation(degrees: Float): Unit = {
    this._rotation = degrees
    isDirty = true
  }

  def rotate(degrees: Float): Unit = {
    _rotation += degrees
    isDirty = true
  }

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this._scaleX = scaleX
    this._scaleY = scaleY
    isDirty = true
    shouldCalculateScaledLength = true
  }

  def scale(amount: Float): Unit = {
    this._scaleX += amount
    this._scaleY += amount
    isDirty = true
    shouldCalculateScaledLength = true
  }

  def calculateLength(): Unit =
    shouldCalculateLength = true

  def calculateScaledLength(): Unit =
    shouldCalculateScaledLength = true

  def dirty(): Unit =
    isDirty = true

  def translate(x: Float, y: Float): Unit = {
    this._x += x
    this._y += y
    isDirty = true
  }

  /** Returns an axis-aligned bounding box of this polyline.
    *
    * Note the returned Rectangle is cached in this polyline, and will be reused if this Polyline is changed.
    *
    * @return
    *   this polyline's bounding box {@link Rectangle}
    */
  def boundingRectangle: Rectangle = {
    val vertices = transformedVertices

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

    if (Nullable(bounds).isEmpty) bounds = Rectangle()
    bounds.x = minX
    bounds.y = minY
    bounds.width = maxX - minX
    bounds.height = maxY - minY

    bounds
  }

  override def contains(point: Vector2): Boolean =
    false

  override def contains(x: Float, y: Float): Boolean =
    false
}
