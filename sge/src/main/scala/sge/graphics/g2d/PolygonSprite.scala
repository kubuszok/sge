/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonSprite.java
 * Original authors: Stefan Bachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: draw(PolygonSpriteBatch) -> draw(PolygonBatch) to use trait instead of concrete class
 *   Convention: if (!dirty) guard instead of if (dirty) return; Nullable for setRegion null check
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (x, y, width, height,
 *     originX, originY, scaleX, scaleY, rotation, color, region, vertices, boundingRectangle,
 *     packedColor); backing fields use _ prefix
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.MathUtils
import sge.math.Rectangle
import sge.graphics.Color
import sge.utils.Nullable
import scala.compiletime.uninitialized

/** @author
  *   Stefan Bachmann
  * @author
  *   Nathan Sweet
  */
class PolygonSprite {
  private var _region:            PolygonRegion = uninitialized
  private var _x, _y:             Float         = 0f
  private var _width, _height:    Float         = 0f
  private var _scaleX:            Float         = 1f
  private var _scaleY:            Float         = 1f
  private var _rotation:          Float         = 0f
  private var _originX, _originY: Float         = 0f
  private var _vertices:          Array[Float]  = uninitialized
  private var dirty:              Boolean       = false
  private val bounds = Rectangle()
  private val _color = Color(1f, 1f, 1f, 1f)

  def this(region: PolygonRegion) = {
    this()
    this.region = region
    setSize(region.region.regionWidth.toFloat, region.region.regionHeight.toFloat)
    setOrigin(_width / 2, _height / 2)
  }

  /** Creates a sprite that is a copy in every way of the specified sprite. */
  def this(sprite: PolygonSprite) = {
    this()
    set(sprite)
  }

  def set(sprite: PolygonSprite): Unit = {
    // sprite: PolygonSprite is non-nullable in Scala

    this.region = sprite._region

    _x = sprite._x
    _y = sprite._y
    _width = sprite._width
    _height = sprite._height
    _originX = sprite._originX
    _originY = sprite._originY
    _rotation = sprite._rotation
    _scaleX = sprite._scaleX
    _scaleY = sprite._scaleY
    _color.set(sprite._color)
  }

  /** Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the bounds after
    * those operations.
    */
  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    this._x = x;
    this._y = y;
    this._width = width;
    this._height = height;

    dirty = true;
  }

  /** Sets the size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the size after those operations.
    * If both position and size are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setSize(width: Float, height: Float): Unit = {
    this._width = width;
    this._height = height;

    dirty = true;
  }

  /** Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setPosition(x: Float, y: Float): Unit =
    translate(x - this._x, y - this._y);

  /** Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def x_=(x: Float): Unit =
    translateX(x - this._x);

  /** Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def y_=(y: Float): Unit =
    translateY(y - this._y);

  /** Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateX(xAmount: Float): Unit = {
    this._x += xAmount;

    if (!dirty) {
      val verts = this._vertices;
      for (i <- 0 until verts.length by Sprite.VERTEX_SIZE)
        verts(i) += xAmount;
    }
  }

  /** Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateY(yAmount: Float): Unit = {
    _y += yAmount;

    if (!dirty) {
      val verts = this._vertices;
      for (i <- 1 until verts.length by Sprite.VERTEX_SIZE)
        verts(i) += yAmount;
    }
  }

  /** Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translate(xAmount: Float, yAmount: Float): Unit = {
    _x += xAmount;
    _y += yAmount;

    if (!dirty) {
      val verts = this._vertices;
      for (i <- 0 until verts.length by Sprite.VERTEX_SIZE) {
        verts(i) += xAmount;
        verts(i + 1) += yAmount;
      }
    }
  }

  def color_=(tint: Color): Unit = {
    _color.set(tint);
    val colorFloat = tint.toFloatBits();

    val verts = this._vertices;
    for (i <- 2 until verts.length by Sprite.VERTEX_SIZE)
      verts(i) = colorFloat;
  }

  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    _color.set(r, g, b, a);
    val pc    = _color.toFloatBits();
    val verts = this._vertices;
    for (i <- 2 until verts.length by Sprite.VERTEX_SIZE)
      verts(i) = pc;
  }

  /** Sets the origin in relation to the sprite's position for scaling and rotation. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this._originX = originX;
    this._originY = originY;
    dirty = true;
  }

  def rotation_=(degrees: Float): Unit = {
    this._rotation = degrees;
    dirty = true;
  }

  /** Sets the sprite's rotation relative to the current rotation. */
  def rotate(degrees: Float): Unit = {
    _rotation += degrees;
    dirty = true;
  }

  def setScale(scaleXY: Float): Unit = {
    this._scaleX = scaleXY;
    this._scaleY = scaleXY;
    dirty = true;
  }

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this._scaleX = scaleX;
    this._scaleY = scaleY;
    dirty = true;
  }

  /** Sets the sprite's scale relative to the current scale. */
  def scale(amount: Float): Unit = {
    this._scaleX += amount;
    this._scaleY += amount;
    dirty = true;
  }

  /** Returns the packed vertices, colors, and texture coordinates for this sprite. */
  def vertices: Array[Float] =
    if (!dirty) this._vertices
    else {
      dirty = false

      val oX             = this._originX
      val oY             = this._originY
      val sX2            = this._scaleX
      val sY2            = this._scaleY
      val reg            = this._region
      val spriteVertices = this._vertices
      val regionVertices = reg.vertices

      val worldOriginX = _x + oX
      val worldOriginY = _y + oY
      val sX           = _width / reg.region.regionWidth
      val sY           = _height / reg.region.regionHeight
      val cos          = MathUtils.cosDeg(_rotation)
      val sin          = MathUtils.sinDeg(_rotation)

      var fx: Float = 0f
      var fy: Float = 0f
      var v = 0
      for (i <- 0 until regionVertices.length by 2) {
        fx = (regionVertices(i) * sX - oX) * sX2
        fy = (regionVertices(i + 1) * sY - oY) * sY2
        spriteVertices(v) = cos * fx - sin * fy + worldOriginX
        spriteVertices(v + 1) = sin * fx + cos * fy + worldOriginY
        v += 5
      }
      spriteVertices
    }

  /** Returns the bounding axis aligned {@link Rectangle} that bounds this sprite. The rectangles x and y coordinates describe its bottom left corner. If you change the position or size of the sprite,
    * you have to fetch the triangle again for it to be recomputed.
    * @return
    *   the bounding Rectangle
    */
  def boundingRectangle: Rectangle = {
    val verts = vertices

    var minx: Float = verts(0)
    var miny: Float = verts(1)
    var maxx: Float = verts(0)
    var maxy: Float = verts(1)

    for (i <- 5 until verts.length by 5) {
      val vx = verts(i)
      val vy = verts(i + 1)
      minx = if (minx > vx) vx else minx
      maxx = if (maxx < vx) vx else maxx
      miny = if (miny > vy) vy else miny
      maxy = if (maxy < vy) vy else maxy
    }

    bounds.x = minx
    bounds.y = miny
    bounds.width = maxx - minx
    bounds.height = maxy - miny
    bounds
  }

  def draw(spriteBatch: PolygonBatch): Unit = {
    val reg = this._region
    spriteBatch.draw(reg.region.texture, vertices, 0, _vertices.length, reg.triangles, 0, reg.triangles.length)
  }

  def draw(spriteBatch: PolygonBatch, alphaModulation: Float): Unit = {
    val c        = color
    val oldAlpha = c.a
    c.a *= alphaModulation
    this.color = c
    draw(spriteBatch)
    c.a = oldAlpha
    this.color = c
  }

  def x: Float = _x

  def y: Float = _y

  def width: Float = _width

  def height: Float = _height

  def originX: Float = _originX

  def originY: Float = _originY

  def rotation: Float = _rotation

  def scaleX: Float = _scaleX

  def scaleY: Float = _scaleY

  /** Returns the color of this sprite. Modifying the returned color will have unexpected effects unless {@link #setColor(Color)} or {@link #setColor(float, float, float, float)} is subsequently
    * called before drawing this sprite.
    */
  def color: Color = _color

  /** Returns the actual color used in the vertices of this sprite. Modifying the returned color will have unexpected effects unless {@link #setColor(Color)} or
    * {@link #setColor(float, float, float, float)} is subsequently called before drawing this sprite.
    */
  def packedColor: Color = {
    Color.abgr8888ToColor(_color, _vertices(2))
    _color
  }

  def region_=(region: PolygonRegion): Unit = {
    this._region = region

    val regionVertices = region.vertices
    val textureCoords  = region.textureCoords

    val verticesLength = (regionVertices.length / 2) * 5
    if (Nullable(_vertices).isEmpty || _vertices.length != verticesLength) _vertices = new Array[Float](verticesLength)

    // Set the color and UVs in this sprite's vertices.
    val floatColor      = _color.toFloatBits()
    val currentVertices = this._vertices
    var v               = 2
    for (i <- 0 until regionVertices.length by 2) {
      currentVertices(v) = floatColor
      currentVertices(v + 1) = textureCoords(i)
      currentVertices(v + 2) = textureCoords(i + 1)
      v += 5
    }

    dirty = true
  }

  def region: PolygonRegion = _region
}
