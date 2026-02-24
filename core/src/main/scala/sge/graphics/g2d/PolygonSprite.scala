/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonSprite.java
 * Original authors: Stefan Bachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.MathUtils
import sge.math.Rectangle
import sge.graphics.Color
import sge.utils.SgeError
import scala.compiletime.uninitialized

/** @author
  *   Stefan Bachmann
  * @author
  *   Nathan Sweet
  */
class PolygonSprite {
  private var region:           PolygonRegion = uninitialized
  private var x, y:             Float         = 0f
  private var width, height:    Float         = 0f
  private var scaleX:           Float         = 1f
  private var scaleY:           Float         = 1f
  private var rotation:         Float         = 0f
  private var originX, originY: Float         = 0f
  private var vertices:         Array[Float]  = uninitialized
  private var dirty:            Boolean       = false
  private val bounds = new Rectangle()
  private val color  = new Color(1f, 1f, 1f, 1f)

  def this(region: PolygonRegion) = {
    this()
    setRegion(region)
    setSize(region.getRegion().regionWidth.toFloat, region.getRegion().regionHeight.toFloat)
    setOrigin(width / 2, height / 2)
  }

  /** Creates a sprite that is a copy in every way of the specified sprite. */
  def this(sprite: PolygonSprite) = {
    this()
    set(sprite)
  }

  def set(sprite: PolygonSprite): Unit = {
    if (sprite == null) throw SgeError.InvalidInput("sprite cannot be null.")

    setRegion(sprite.region)

    x = sprite.x
    y = sprite.y
    width = sprite.width
    height = sprite.height
    originX = sprite.originX
    originY = sprite.originY
    rotation = sprite.rotation
    scaleX = sprite.scaleX
    scaleY = sprite.scaleY
    color.set(sprite.color)
  }

  /** Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the bounds after
    * those operations.
    */
  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;

    dirty = true;
  }

  /** Sets the size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the size after those operations.
    * If both position and size are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setSize(width: Float, height: Float): Unit = {
    this.width = width;
    this.height = height;

    dirty = true;
  }

  /** Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setPosition(x: Float, y: Float): Unit =
    translate(x - this.x, y - this.y);

  /** Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setX(x: Float): Unit =
    translateX(x - this.x);

  /** Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setY(y: Float): Unit =
    translateY(y - this.y);

  /** Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateX(xAmount: Float): Unit = {
    this.x += xAmount;

    if (dirty) return;

    val vertices = this.vertices;
    for (i <- 0 until vertices.length by Sprite.VERTEX_SIZE)
      vertices(i) += xAmount;
  }

  /** Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateY(yAmount: Float): Unit = {
    y += yAmount;

    if (dirty) return;

    val vertices = this.vertices;
    for (i <- 1 until vertices.length by Sprite.VERTEX_SIZE)
      vertices(i) += yAmount;
  }

  /** Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translate(xAmount: Float, yAmount: Float): Unit = {
    x += xAmount;
    y += yAmount;

    if (dirty) return;

    val vertices = this.vertices;
    for (i <- 0 until vertices.length by Sprite.VERTEX_SIZE) {
      vertices(i) += xAmount;
      vertices(i + 1) += yAmount;
    }
  }

  def setColor(tint: Color): Unit = {
    color.set(tint);
    val colorFloat = tint.toFloatBits();

    val vertices = this.vertices;
    for (i <- 2 until vertices.length by Sprite.VERTEX_SIZE)
      vertices(i) = colorFloat;
  }

  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    color.set(r, g, b, a);
    val packedColor = color.toFloatBits();
    val vertices    = this.vertices;
    for (i <- 2 until vertices.length by Sprite.VERTEX_SIZE)
      vertices(i) = packedColor;
  }

  /** Sets the origin in relation to the sprite's position for scaling and rotation. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this.originX = originX;
    this.originY = originY;
    dirty = true;
  }

  def setRotation(degrees: Float): Unit = {
    this.rotation = degrees;
    dirty = true;
  }

  /** Sets the sprite's rotation relative to the current rotation. */
  def rotate(degrees: Float): Unit = {
    rotation += degrees;
    dirty = true;
  }

  def setScale(scaleXY: Float): Unit = {
    this.scaleX = scaleXY;
    this.scaleY = scaleXY;
    dirty = true;
  }

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this.scaleX = scaleX;
    this.scaleY = scaleY;
    dirty = true;
  }

  /** Sets the sprite's scale relative to the current scale. */
  def scale(amount: Float): Unit = {
    this.scaleX += amount;
    this.scaleY += amount;
    dirty = true;
  }

  /** Returns the packed vertices, colors, and texture coordinates for this sprite. */
  def getVertices(): Array[Float] = {
    if (!dirty) return this.vertices
    dirty = false

    val originX        = this.originX
    val originY        = this.originY
    val scaleX         = this.scaleX
    val scaleY         = this.scaleY
    val region         = this.region
    val spriteVertices = this.vertices
    val regionVertices = region.getVertices()

    val worldOriginX = x + originX
    val worldOriginY = y + originY
    val sX           = width / region.getRegion().getRegionWidth()
    val sY           = height / region.getRegion().getRegionHeight()
    val cos          = MathUtils.cosDeg(rotation)
    val sin          = MathUtils.sinDeg(rotation)

    var fx: Float = 0f
    var fy: Float = 0f
    var v = 0
    for (i <- 0 until regionVertices.length by 2) {
      fx = (regionVertices(i) * sX - originX) * scaleX
      fy = (regionVertices(i + 1) * sY - originY) * scaleY
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
  def getBoundingRectangle(): Rectangle = {
    val vertices = getVertices()

    var minx: Float = vertices(0)
    var miny: Float = vertices(1)
    var maxx: Float = vertices(0)
    var maxy: Float = vertices(1)

    for (i <- 5 until vertices.length by 5) {
      val x = vertices(i)
      val y = vertices(i + 1)
      minx = if (minx > x) x else minx
      maxx = if (maxx < x) x else maxx
      miny = if (miny > y) y else miny
      maxy = if (maxy < y) y else maxy
    }

    bounds.x = minx
    bounds.y = miny
    bounds.width = maxx - minx
    bounds.height = maxy - miny
    bounds
  }

  def draw(spriteBatch: PolygonBatch): Unit = {
    val region = this.region
    spriteBatch.draw(region.getRegion().texture, getVertices(), 0, vertices.length, region.getTriangles(), 0, region.getTriangles().length)
  }

  def draw(spriteBatch: PolygonBatch, alphaModulation: Float): Unit = {
    val color    = getColor()
    val oldAlpha = color.a
    color.a *= alphaModulation
    setColor(color)
    draw(spriteBatch)
    color.a = oldAlpha
    setColor(color)
  }

  def getX(): Float = x

  def getY(): Float = y

  def getWidth(): Float = width

  def getHeight(): Float = height

  def getOriginX(): Float = originX

  def getOriginY(): Float = originY

  def getRotation(): Float = rotation

  def getScaleX(): Float = scaleX

  def getScaleY(): Float = scaleY

  /** Returns the color of this sprite. Modifying the returned color will have unexpected effects unless {@link #setColor(Color)} or {@link #setColor(float, float, float, float)} is subsequently
    * called before drawing this sprite.
    */
  def getColor(): Color = color

  /** Returns the actual color used in the vertices of this sprite. Modifying the returned color will have unexpected effects unless {@link #setColor(Color)} or
    * {@link #setColor(float, float, float, float)} is subsequently called before drawing this sprite.
    */
  def getPackedColor(): Color = {
    Color.abgr8888ToColor(color, vertices(2))
    color
  }

  def setRegion(region: PolygonRegion): Unit = {
    this.region = region

    val regionVertices = region.getVertices()
    val textureCoords  = region.getTextureCoords()

    val verticesLength = (regionVertices.length / 2) * 5
    if (vertices == null || vertices.length != verticesLength) vertices = new Array[Float](verticesLength)

    // Set the color and UVs in this sprite's vertices.
    val floatColor      = color.toFloatBits()
    val currentVertices = this.vertices
    var v               = 2
    for (i <- 0 until regionVertices.length by 2) {
      currentVertices(v) = floatColor
      currentVertices(v + 1) = textureCoords(i)
      currentVertices(v + 2) = textureCoords(i + 1)
      v += 5
    }

    dirty = true
  }

  def getRegion(): PolygonRegion = region
}
