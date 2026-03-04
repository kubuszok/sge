/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Polyline.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getX/Y, getOriginX/Y, getRotation, getScaleX/Y and setters with dirty flags
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: getVertices, getTransformedVertices, getLength,
 * getScaledLength, getX/Y, getOriginX/Y, getRotation, getScaleX/Y, setOrigin, setPosition,
 * setVertices, setRotation, rotate, setScale, scale, calculateLength, calculateScaledLength,
 * dirty, translate, getBoundingRectangle, contains(2). Implements Shape2D (always false).
 */
package sge
package math

import sge.utils.Nullable

class Polyline() extends Shape2D {
  private var localVertices:               Array[Float] = Array.empty[Float]
  private var worldVertices:               Array[Float] = scala.compiletime.uninitialized
  private var x:                           Float        = 0f
  private var y:                           Float        = 0f
  private var originX:                     Float        = 0f
  private var originY:                     Float        = 0f
  private var rotation:                    Float        = 0f
  private var scaleX:                      Float        = 1f
  private var scaleY:                      Float        = 1f
  private var length:                      Float        = 0f
  private var scaledLength:                Float        = 0f
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
  def getVertices(): Array[Float] =
    localVertices

  /** Returns vertices scaled, rotated, and offset by the polygon position. */
  def getTransformedVertices(): Array[Float] =
    if (!isDirty) this.worldVertices
    else {
      isDirty = false

      val localVertices = this.localVertices
      if (Nullable(this.worldVertices).fold(true)(_.length < localVertices.length))
        this.worldVertices = new Array[Float](localVertices.length)

      val worldVertices = this.worldVertices
      val positionX     = x
      val positionY     = y
      val originX       = this.originX
      val originY       = this.originY
      val scaleX        = this.scaleX
      val scaleY        = this.scaleY
      val scale         = scaleX != 1 || scaleY != 1
      val rotation      = this.rotation
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
  def getLength(): Float =
    if (!shouldCalculateLength) length
    else {
      shouldCalculateLength = false

      length = 0
      var i = 0
      val n = localVertices.length - 2
      while (i < n) {
        val x = localVertices(i + 2) - localVertices(i)
        val y = localVertices(i + 1) - localVertices(i + 3)
        length += Math.sqrt(x * x + y * y).toFloat
        i += 2
      }

      length
    }

  /** Returns the euclidean length of the polyline */
  def getScaledLength(): Float =
    if (!shouldCalculateScaledLength) scaledLength
    else {
      shouldCalculateScaledLength = false

      scaledLength = 0
      var i = 0
      val n = localVertices.length - 2
      while (i < n) {
        val x = localVertices(i + 2) * scaleX - localVertices(i) * scaleX
        val y = localVertices(i + 1) * scaleY - localVertices(i + 3) * scaleY
        scaledLength += Math.sqrt(x * x + y * y).toFloat
        i += 2
      }

      scaledLength
    }

  def getX(): Float = x

  def getY(): Float = y

  def getOriginX(): Float = originX

  def getOriginY(): Float = originY

  def getRotation(): Float = rotation

  def getScaleX(): Float = scaleX

  def getScaleY(): Float = scaleY

  def setOrigin(originX: Float, originY: Float): Unit = {
    this.originX = originX
    this.originY = originY
    isDirty = true
  }

  def setPosition(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y
    isDirty = true
  }

  def setVertices(vertices: Array[Float]): Unit = {
    if (vertices.length < 4) throw new IllegalArgumentException("polylines must contain at least 2 points.")
    this.localVertices = vertices
    isDirty = true
  }

  def setRotation(degrees: Float): Unit = {
    this.rotation = degrees
    isDirty = true
  }

  def rotate(degrees: Float): Unit = {
    rotation += degrees
    isDirty = true
  }

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this.scaleX = scaleX
    this.scaleY = scaleY
    isDirty = true
    shouldCalculateScaledLength = true
  }

  def scale(amount: Float): Unit = {
    this.scaleX += amount
    this.scaleY += amount
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
    this.x += x
    this.y += y
    isDirty = true
  }

  /** Returns an axis-aligned bounding box of this polyline.
    *
    * Note the returned Rectangle is cached in this polyline, and will be reused if this Polyline is changed.
    *
    * @return
    *   this polyline's bounding box {@link Rectangle}
    */
  def getBoundingRectangle(): Rectangle = {
    val vertices = getTransformedVertices()

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

  override def contains(point: Vector2): Boolean =
    false

  override def contains(x: Float, y: Float): Boolean =
    false
}
