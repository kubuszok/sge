/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Sprite.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.g2d.Batch.*
import sge.math.MathUtils
import sge.math.Rectangle
import sge.utils.Nullable
import scala.compiletime.uninitialized

/** Holds the geometry, color, and texture information for drawing 2D sprites using {@link Batch} . A Sprite has a position and a size given as width and height. The position is relative to the origin
  * of the coordinate system specified via {@link Batch#begin()} and the respective matrices. A Sprite is always rectangular and its position (x, y) are located in the bottom left corner of that
  * rectangle. A Sprite also has an origin around which rotations and scaling are performed (that is, the origin is not modified by rotation and scaling). The origin is given relative to the bottom
  * left corner of the Sprite, its position.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Sprite() extends TextureRegion {

  final val vertices:      Array[Float] = Array.ofDim[Float](20) // 4 * (2 + 1 + 2) = 4 * 5 = 20
  final private val color: Color        = new Color(1, 1, 1, 1)
  private var packedColor: Float        = Color.WHITE_FLOAT_BITS
  private var x:           Float        = uninitialized
  private var y:           Float        = uninitialized
  var width:               Float        = uninitialized
  var height:              Float        = uninitialized
  private var originX:     Float        = uninitialized
  private var originY:     Float        = uninitialized
  private var rotation:    Float        = uninitialized
  private var scaleX:      Float        = 1
  private var scaleY:      Float        = 1
  private var dirty:       Boolean      = true
  private var bounds:      Rectangle    = uninitialized

  /** Creates an uninitialized sprite. The sprite will need a texture region and bounds set before it can be drawn. */
  setColor(1, 1, 1, 1)

  /** Creates a sprite with width, height, and texture region equal to the size of the texture. */
  def this(texture: Texture) = {
    this()
    setTexture(texture)
    super.setRegion(0, 0, texture.getWidth, texture.getHeight)
    setColor(1, 1, 1, 1)
    setSize(texture.getWidth.toFloat, texture.getHeight.toFloat)
    setOrigin(width / 2, height / 2)
  }

  /** Creates a sprite with width, height, and texture region equal to the specified size. The texture region's upper left corner will be 0,0.
    * @param srcWidth
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param srcHeight
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, srcWidth: Int, srcHeight: Int) = {
    this()
    setTexture(texture)
    super.setRegion(0, 0, srcWidth, srcHeight): Unit
    setColor(1, 1, 1, 1)
    setSize(scala.math.abs(srcWidth).toFloat, scala.math.abs(srcHeight).toFloat)
    setOrigin(width / 2, height / 2)
  }

  /** Creates a sprite with width, height, and texture region equal to the specified size.
    * @param srcWidth
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param srcHeight
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) = {
    this()
    setTexture(texture)
    super.setRegion(srcX, srcY, srcWidth, srcHeight)
    setColor(1, 1, 1, 1)
    setSize(scala.math.abs(srcWidth).toFloat, scala.math.abs(srcHeight).toFloat)
    setOrigin(width / 2, height / 2)
  }

  // Note the region is copied.
  /** Creates a sprite based on a specific TextureRegion, the new sprite's region is a copy of the parameter region - altering one does not affect the other
    */
  def this(region: TextureRegion) = {
    this()
    setRegion(region)
    setColor(1, 1, 1, 1)
    setSize(region.getRegionWidth().toFloat, region.getRegionHeight().toFloat)
    setOrigin(width / 2, height / 2)
  }

  /** Creates a sprite with width, height, and texture region equal to the specified size, relative to specified sprite's texture region.
    * @param srcWidth
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param srcHeight
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(region: TextureRegion, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) = {
    this()
    setRegion(region, srcX, srcY, srcWidth, srcHeight)
    setColor(1, 1, 1, 1)
    setSize(scala.math.abs(srcWidth).toFloat, scala.math.abs(srcHeight).toFloat)
    setOrigin(width / 2, height / 2)
  }

  /** Creates a sprite that is a copy in every way of the specified sprite. */
  def this(sprite: Sprite) = {
    this()
    set(sprite)
  }

  /** Make this sprite a copy in every way of the specified sprite */
  def set(sprite: Sprite): Unit = {
    // sprite: Sprite is non-nullable in Scala
    System.arraycopy(sprite.vertices, 0, vertices, 0, Sprite.SPRITE_SIZE)
    setTexture(sprite.getTexture())
    setU(sprite.getU())
    setV(sprite.getV())
    setU2(sprite.getU2())
    setV2(sprite.getV2())
    x = sprite.x
    y = sprite.y
    width = sprite.width
    height = sprite.height
    setRegionWidth(sprite.getRegionWidth())
    setRegionHeight(sprite.getRegionHeight())
    originX = sprite.originX
    originY = sprite.originY
    rotation = sprite.rotation
    scaleX = sprite.scaleX
    scaleY = sprite.scaleY
    color.set(sprite.color)
    dirty = sprite.dirty
  }

  /** Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the bounds after
    * those operations.
    */
  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    this.x = x
    this.y = y
    this.width = width
    this.height = height

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val x2       = x + width
    val y2       = y + height
    val vertices = this.vertices
    vertices(X1) = x
    vertices(Y1) = y

    vertices(X2) = x
    vertices(Y2) = y2

    vertices(X3) = x2
    vertices(Y3) = y2

    vertices(X4) = x2
    vertices(Y4) = y
  }

  /** Sets the size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the size after those operations.
    * If both position and size are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setSize(width: Float, height: Float): Unit = {
    this.width = width
    this.height = height

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val x2       = x + width
    val y2       = y + height
    val vertices = this.vertices
    vertices(X1) = x
    vertices(Y1) = y

    vertices(X2) = x
    vertices(Y2) = y2

    vertices(X3) = x2
    vertices(Y3) = y2

    vertices(X4) = x2
    vertices(Y4) = y
  }

  /** Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setPosition(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val x2       = x + width
    val y2       = y + height
    val vertices = this.vertices
    vertices(X1) = x
    vertices(Y1) = y

    vertices(X2) = x
    vertices(Y2) = y2

    vertices(X3) = x2
    vertices(Y3) = y2

    vertices(X4) = x2
    vertices(Y4) = y
  }

  /** Sets the position where the sprite will be drawn, relative to its current origin. */
  def setOriginBasedPosition(x: Float, y: Float): Unit =
    setPosition(x - this.originX, y - this.originY)

  /** Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setX(x: Float): Unit = {
    this.x = x

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val x2       = x + width
    val vertices = this.vertices
    vertices(X1) = x
    vertices(X2) = x
    vertices(X3) = x2
    vertices(X4) = x2
  }

  /** Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setY(y: Float): Unit = {
    this.y = y

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val y2       = y + height
    val vertices = this.vertices
    vertices(Y1) = y
    vertices(Y2) = y2
    vertices(Y3) = y2
    vertices(Y4) = y
  }

  /** Sets the x position so that it is centered on the given x parameter */
  def setCenterX(x: Float): Unit =
    setX(x - width / 2)

  /** Sets the y position so that it is centered on the given y parameter */
  def setCenterY(y: Float): Unit =
    setY(y - height / 2)

  /** Sets the position so that the sprite is centered on (x, y) */
  def setCenter(x: Float, y: Float): Unit =
    setPosition(x - width / 2, y - height / 2)

  /** Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateX(xAmount: Float): Unit = {
    this.x += xAmount

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val vertices = this.vertices
    vertices(X1) += xAmount
    vertices(X2) += xAmount
    vertices(X3) += xAmount
    vertices(X4) += xAmount
  }

  /** Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateY(yAmount: Float): Unit = {
    y += yAmount

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val vertices = this.vertices
    vertices(Y1) += yAmount
    vertices(Y2) += yAmount
    vertices(Y3) += yAmount
    vertices(Y4) += yAmount
  }

  /** Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translate(xAmount: Float, yAmount: Float): Unit = {
    x += xAmount
    y += yAmount

    if (dirty) return
    if (rotation != 0 || scaleX != 1 || scaleY != 1) {
      dirty = true
      return
    }

    val vertices = this.vertices
    vertices(X1) += xAmount
    vertices(Y1) += yAmount

    vertices(X2) += xAmount
    vertices(Y2) += yAmount

    vertices(X3) += xAmount
    vertices(Y3) += yAmount

    vertices(X4) += xAmount
    vertices(Y4) += yAmount
  }

  /** Sets the color used to tint this sprite. Default is {@link Color#WHITE}. */
  def setColor(tint: Color): Unit = {
    color.set(tint)
    packedColor = tint.toFloatBits()
    val vertices = this.vertices
    vertices(C1) = packedColor
    vertices(C2) = packedColor
    vertices(C3) = packedColor
    vertices(C4) = packedColor
  }

  /** Sets the alpha portion of the color used to tint this sprite. */
  def setAlpha(a: Float): Unit =
    if (color.a != a) {
      color.a = a
      packedColor = color.toFloatBits()
      vertices(C1) = packedColor
      vertices(C2) = packedColor
      vertices(C3) = packedColor
      vertices(C4) = packedColor
    }

  /** @see #setColor(Color) */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    color.set(r, g, b, a)
    packedColor = color.toFloatBits()
    val vertices = this.vertices
    vertices(C1) = packedColor
    vertices(C2) = packedColor
    vertices(C3) = packedColor
    vertices(C4) = packedColor
  }

  /** Sets the packed color used to tint this sprite.
    * @see
    *   #setColor(Color)
    * @see
    *   Color#toFloatBits()
    */
  def setPackedColor(packedColor: Float): Unit =
    // Handle 0f/-0f special case
    if (
      packedColor != this.packedColor || (packedColor == 0f && this.packedColor == 0f
        && java.lang.Float.floatToIntBits(packedColor) != java.lang.Float.floatToIntBits(this.packedColor))
    ) {
      this.packedColor = packedColor
      Color.abgr8888ToColor(color, packedColor)
      val vertices = this.vertices
      vertices(C1) = packedColor
      vertices(C2) = packedColor
      vertices(C3) = packedColor
      vertices(C4) = packedColor
    }

  /** Sets the origin in relation to the sprite's position for scaling and rotation. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this.originX = originX
    this.originY = originY
    dirty = true
  }

  /** Place origin in the center of the sprite */
  def setOriginCenter(): Unit = {
    this.originX = width / 2
    this.originY = height / 2
    dirty = true
  }

  /** Sets the rotation of the sprite in degrees. Rotation is centered on the origin set in {@link #setOrigin(float, float)} */
  def setRotation(degrees: Float): Unit = {
    this.rotation = degrees
    dirty = true
  }

  /** @return the rotation of the sprite in degrees */
  def getRotation(): Float =
    rotation

  /** Sets the sprite's rotation in degrees relative to the current rotation. Rotation is centered on the origin set in {@link #setOrigin(float, float)}
    */
  def rotate(degrees: Float): Unit = {
    if (degrees == 0) return
    rotation += degrees
    dirty = true
  }

  /** Rotates this sprite 90 degrees in-place by rotating the texture coordinates. This rotation is unaffected by {@link #setRotation(float)} and {@link #rotate(float)} .
    */
  def rotate90(clockwise: Boolean): Unit = {
    val vertices = this.vertices

    if (clockwise) {
      var temp = vertices(V1)
      vertices(V1) = vertices(V4)
      vertices(V4) = vertices(V3)
      vertices(V3) = vertices(V2)
      vertices(V2) = temp

      temp = vertices(U1)
      vertices(U1) = vertices(U4)
      vertices(U4) = vertices(U3)
      vertices(U3) = vertices(U2)
      vertices(U2) = temp
    } else {
      var temp = vertices(V1)
      vertices(V1) = vertices(V2)
      vertices(V2) = vertices(V3)
      vertices(V3) = vertices(V4)
      vertices(V4) = temp

      temp = vertices(U1)
      vertices(U1) = vertices(U2)
      vertices(U2) = vertices(U3)
      vertices(U3) = vertices(U4)
      vertices(U4) = temp
    }
  }

  /** Sets the sprite's scale for both X and Y uniformly. The sprite scales out from the origin. This will not affect the values returned by {@link #getWidth()} and {@link #getHeight()}
    */
  def setScale(scaleXY: Float): Unit = {
    this.scaleX = scaleXY
    this.scaleY = scaleXY
    dirty = true
  }

  /** Sets the sprite's scale for both X and Y. The sprite scales out from the origin. This will not affect the values returned by {@link #getWidth()} and {@link #getHeight()}
    */
  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this.scaleX = scaleX
    this.scaleY = scaleY
    dirty = true
  }

  /** Sets the sprite's scale relative to the current scale. for example: original scale 2 -> sprite.scale(4) -> final scale 6. The sprite scales out from the origin. This will not affect the values
    * returned by {@link #getWidth()} and {@link #getHeight()}
    */
  def scale(amount: Float): Unit = {
    this.scaleX += amount
    this.scaleY += amount
    dirty = true
  }

  /** Returns the packed vertices, colors, and texture coordinates for this sprite. */
  def getVertices(): Array[Float] = {
    if (dirty) {
      dirty = false

      val vertices     = this.vertices
      var localX       = -originX
      var localY       = -originY
      var localX2      = localX + width
      var localY2      = localY + height
      val worldOriginX = this.x - localX
      val worldOriginY = this.y - localY
      if (scaleX != 1 || scaleY != 1) {
        localX *= scaleX
        localY *= scaleY
        localX2 *= scaleX
        localY2 *= scaleY
      }
      if (rotation != 0) {
        val cos        = MathUtils.cosDeg(rotation)
        val sin        = MathUtils.sinDeg(rotation)
        val localXCos  = localX * cos
        val localXSin  = localX * sin
        val localYCos  = localY * cos
        val localYSin  = localY * sin
        val localX2Cos = localX2 * cos
        val localX2Sin = localX2 * sin
        val localY2Cos = localY2 * cos
        val localY2Sin = localY2 * sin

        val x1 = localXCos - localYSin + worldOriginX
        val y1 = localYCos + localXSin + worldOriginY
        vertices(X1) = x1
        vertices(Y1) = y1

        val x2 = localXCos - localY2Sin + worldOriginX
        val y2 = localY2Cos + localXSin + worldOriginY
        vertices(X2) = x2
        vertices(Y2) = y2

        val x3 = localX2Cos - localY2Sin + worldOriginX
        val y3 = localY2Cos + localX2Sin + worldOriginY
        vertices(X3) = x3
        vertices(Y3) = y3

        vertices(X4) = x1 + (x3 - x2)
        vertices(Y4) = y3 - (y2 - y1)
      } else {
        val x1 = localX + worldOriginX
        val y1 = localY + worldOriginY
        val x2 = localX2 + worldOriginX
        val y2 = localY2 + worldOriginY

        vertices(X1) = x1
        vertices(Y1) = y1

        vertices(X2) = x1
        vertices(Y2) = y2

        vertices(X3) = x2
        vertices(Y3) = y2

        vertices(X4) = x2
        vertices(Y4) = y1
      }
    }
    vertices
  }

  /** Returns the bounding axis aligned {@link Rectangle} that bounds this sprite. The rectangles x and y coordinates describe its bottom left corner. If you change the position or size of the sprite,
    * you have to fetch the triangle again for it to be recomputed.
    *
    * @return
    *   the bounding Rectangle
    */
  def getBoundingRectangle(): Rectangle = {
    val vertices = getVertices()

    var minx = vertices(X1)
    var miny = vertices(Y1)
    var maxx = vertices(X1)
    var maxy = vertices(Y1)

    minx = if (minx > vertices(X2)) vertices(X2) else minx
    minx = if (minx > vertices(X3)) vertices(X3) else minx
    minx = if (minx > vertices(X4)) vertices(X4) else minx

    maxx = if (maxx < vertices(X2)) vertices(X2) else maxx
    maxx = if (maxx < vertices(X3)) vertices(X3) else maxx
    maxx = if (maxx < vertices(X4)) vertices(X4) else maxx

    miny = if (miny > vertices(Y2)) vertices(Y2) else miny
    miny = if (miny > vertices(Y3)) vertices(Y3) else miny
    miny = if (miny > vertices(Y4)) vertices(Y4) else miny

    maxy = if (maxy < vertices(Y2)) vertices(Y2) else maxy
    maxy = if (maxy < vertices(Y3)) vertices(Y3) else maxy
    maxy = if (maxy < vertices(Y4)) vertices(Y4) else maxy

    if (Nullable(bounds).isEmpty) bounds = new Rectangle()
    bounds.x = minx
    bounds.y = miny
    bounds.width = maxx - minx
    bounds.height = maxy - miny
    bounds
  }

  def draw(batch: Batch): Unit =
    batch.draw(getTexture(), getVertices(), 0, Sprite.SPRITE_SIZE)

  def draw(batch: Batch, alphaModulation: Float): Unit = {
    val oldAlpha = getColor().a
    setAlpha(oldAlpha * alphaModulation)
    draw(batch)
    setAlpha(oldAlpha)
  }

  def getX(): Float =
    x

  def getY(): Float =
    y

  /** @return the width of the sprite, not accounting for scale. */
  def getWidth(): Float =
    width

  /** @return the height of the sprite, not accounting for scale. */
  def getHeight(): Float =
    height

  /** The origin influences {@link #setPosition(float, float)} , {@link #setRotation(float)} and the expansion direction of scaling {@link #setScale(float, float)}
    */
  def getOriginX(): Float =
    originX

  /** The origin influences {@link #setPosition(float, float)} , {@link #setRotation(float)} and the expansion direction of scaling {@link #setScale(float, float)}
    */
  def getOriginY(): Float =
    originY

  /** X scale of the sprite, independent of size set by {@link #setSize(float, float)} */
  def getScaleX(): Float =
    scaleX

  /** Y scale of the sprite, independent of size set by {@link #setSize(float, float)} */
  def getScaleY(): Float =
    scaleY

  /** Returns the color of this sprite. If the returned instance is manipulated, {@link #setColor(Color)} must be called afterward.
    */
  def getColor(): Color =
    color

  /** Returns the packed color of this sprite. */
  def getPackedColor(): Float =
    packedColor

  override def setRegion(u: Float, v: Float, u2: Float, v2: Float): Unit = {
    super.setRegion(u, v, u2, v2)

    val vertices = this.vertices
    vertices(U1) = u
    vertices(V1) = v2

    vertices(U2) = u
    vertices(V2) = v

    vertices(U3) = u2
    vertices(V3) = v

    vertices(U4) = u2
    vertices(V4) = v2
  }

  override def setU(u: Float): Unit = {
    super.setU(u)
    vertices(U1) = u
    vertices(U2) = u
  }

  override def setV(v: Float): Unit = {
    super.setV(v)
    vertices(V2) = v
    vertices(V3) = v
  }

  override def setU2(u2: Float): Unit = {
    super.setU2(u2)
    vertices(U3) = u2
    vertices(U4) = u2
  }

  override def setV2(v2: Float): Unit = {
    super.setV2(v2)
    vertices(V1) = v2
    vertices(V4) = v2
  }

  /** Set the sprite's flip state regardless of current condition
    * @param x
    *   the desired horizontal flip state
    * @param y
    *   the desired vertical flip state
    */
  def setFlip(x: Boolean, y: Boolean): Unit = {
    var performX = false
    var performY = false
    if (isFlipX() != x) {
      performX = true
    }
    if (isFlipY() != y) {
      performY = true
    }
    flip(performX, performY)
  }

  /** boolean parameters x,y are not setting a state, but performing a flip
    * @param x
    *   perform horizontal flip
    * @param y
    *   perform vertical flip
    */
  override def flip(x: Boolean, y: Boolean): Unit = {
    super.flip(x, y)
    val vertices = this.vertices
    if (x) {
      var temp = vertices(U1)
      vertices(U1) = vertices(U3)
      vertices(U3) = temp
      temp = vertices(U2)
      vertices(U2) = vertices(U4)
      vertices(U4) = temp
    }
    if (y) {
      var temp = vertices(V1)
      vertices(V1) = vertices(V3)
      vertices(V3) = temp
      temp = vertices(V2)
      vertices(V2) = vertices(V4)
      vertices(V4) = temp
    }
  }

  override def scroll(xAmount: Float, yAmount: Float): Unit = {
    val vertices = this.vertices
    if (xAmount != 0) {
      val u  = (vertices(U1) + xAmount) % 1
      val u2 = u + width / getTexture().getWidth
      setU(u)
      setU2(u2)
      vertices(U1) = u
      vertices(U2) = u
      vertices(U3) = u2
      vertices(U4) = u2
    }
    if (yAmount != 0) {
      val v  = (vertices(V2) + yAmount) % 1
      val v2 = v + height / getTexture().getHeight
      setV(v)
      setV2(v2)
      vertices(V1) = v2
      vertices(V2) = v
      vertices(V3) = v
      vertices(V4) = v2
    }
  }

  def setU(u: Float, width: Float): Unit = {
    val u2 = u + width / getTexture().getWidth
    setU(u)
    setU2(u2)
  }

  def setV(v: Float, height: Float): Unit = {
    val v2 = v + height / getTexture().getHeight
    setV(v)
    setV2(v2)
  }
}

object Sprite {
  final val VERTEX_SIZE = 2 + 1 + 2
  final val SPRITE_SIZE = 4 * VERTEX_SIZE
}
