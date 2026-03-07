/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Sprite.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: boundary/break for early returns; no null in API
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (x, y, width, height, originX, originY,
 *          scaleX, scaleY, rotation, color, packedColor, vertices, boundingRectangle);
 *          overrides u_=, v_=, u2_=, v2_= from TextureRegion; removed getTexture()/setTexture() calls
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
import scala.util.boundary
import scala.util.boundary.break

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

  final val _vertices:      Array[Float] = Array.ofDim[Float](20) // 4 * (2 + 1 + 2) = 4 * 5 = 20
  final private val _color: Color        = Color(1, 1, 1, 1)
  private var _packedColor: Float        = Color.WHITE_FLOAT_BITS
  private var _x:           Float        = uninitialized
  private var _y:           Float        = uninitialized
  private var _width:       Float        = uninitialized
  private var _height:      Float        = uninitialized
  private var _originX:     Float        = uninitialized
  private var _originY:     Float        = uninitialized
  private var _rotation:    Float        = uninitialized
  private var _scaleX:      Float        = 1
  private var _scaleY:      Float        = 1
  private var dirty:        Boolean      = true
  private var bounds:       Rectangle    = uninitialized

  /** Creates an uninitialized sprite. The sprite will need a texture region and bounds set before it can be drawn. */
  setColor(1, 1, 1, 1)

  /** Creates a sprite with width, height, and texture region equal to the size of the texture. */
  def this(texture: Texture) = {
    this()
    this.texture = texture
    super.setRegion(0, 0, texture.getWidth.toInt, texture.getHeight.toInt)
    setColor(1, 1, 1, 1)
    setSize(texture.getWidth.toFloat, texture.getHeight.toFloat)
    setOrigin(_width / 2, _height / 2)
  }

  /** Creates a sprite with width, height, and texture region equal to the specified size. The texture region's upper left corner will be 0,0.
    * @param srcWidth
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param srcHeight
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, srcWidth: Int, srcHeight: Int) = {
    this()
    this.texture = texture
    super.setRegion(0, 0, srcWidth, srcHeight): Unit
    setColor(1, 1, 1, 1)
    setSize(scala.math.abs(srcWidth).toFloat, scala.math.abs(srcHeight).toFloat)
    setOrigin(_width / 2, _height / 2)
  }

  /** Creates a sprite with width, height, and texture region equal to the specified size.
    * @param srcWidth
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param srcHeight
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) = {
    this()
    this.texture = texture
    super.setRegion(srcX, srcY, srcWidth, srcHeight)
    setColor(1, 1, 1, 1)
    setSize(scala.math.abs(srcWidth).toFloat, scala.math.abs(srcHeight).toFloat)
    setOrigin(_width / 2, _height / 2)
  }

  // Note the region is copied.
  /** Creates a sprite based on a specific TextureRegion, the new sprite's region is a copy of the parameter region - altering one does not affect the other
    */
  def this(region: TextureRegion) = {
    this()
    setRegion(region)
    setColor(1, 1, 1, 1)
    setSize(region.regionWidth.toFloat, region.regionHeight.toFloat)
    setOrigin(_width / 2, _height / 2)
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
    setOrigin(_width / 2, _height / 2)
  }

  /** Creates a sprite that is a copy in every way of the specified sprite. */
  def this(sprite: Sprite) = {
    this()
    set(sprite)
  }

  /** Make this sprite a copy in every way of the specified sprite */
  def set(sprite: Sprite): Unit = {
    // sprite: Sprite is non-nullable in Scala
    System.arraycopy(sprite._vertices, 0, _vertices, 0, Sprite.SPRITE_SIZE)
    texture = sprite.texture
    u = sprite.u
    v = sprite.v
    u2 = sprite.u2
    v2 = sprite.v2
    _x = sprite._x
    _y = sprite._y
    _width = sprite._width
    _height = sprite._height
    regionWidth = sprite.regionWidth
    regionHeight = sprite.regionHeight
    _originX = sprite._originX
    _originY = sprite._originY
    _rotation = sprite._rotation
    _scaleX = sprite._scaleX
    _scaleY = sprite._scaleY
    _color.set(sprite._color)
    dirty = sprite.dirty
  }

  /** Sets the position and size of the sprite when drawn, before scaling and rotation are applied. If origin, rotation, or scale are changed, it is slightly more efficient to set the bounds after
    * those operations.
    */
  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = boundary {
    this._x = x
    this._y = y
    this._width = width
    this._height = height

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val x2       = x + width
    val y2       = y + height
    val vertices = this._vertices
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
  def setSize(width: Float, height: Float): Unit = boundary {
    this._width = width
    this._height = height

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val x2       = _x + width
    val y2       = _y + height
    val vertices = this._vertices
    vertices(X1) = _x
    vertices(Y1) = _y

    vertices(X2) = _x
    vertices(Y2) = y2

    vertices(X3) = x2
    vertices(Y3) = y2

    vertices(X4) = x2
    vertices(Y4) = _y
  }

  /** Sets the position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def setPosition(x: Float, y: Float): Unit = boundary {
    this._x = x
    this._y = y

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val x2       = x + _width
    val y2       = y + _height
    val vertices = this._vertices
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
    setPosition(x - this._originX, y - this._originY)

  /** Sets the x position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def x_=(x: Float): Unit = boundary {
    this._x = x

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val x2       = x + _width
    val vertices = this._vertices
    vertices(X1) = x
    vertices(X2) = x
    vertices(X3) = x2
    vertices(X4) = x2
  }

  /** Sets the y position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to set the position after those operations. If both position and size
    * are to be changed, it is better to use {@link #setBounds(float, float, float, float)} .
    */
  def y_=(y: Float): Unit = boundary {
    this._y = y

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val y2       = y + _height
    val vertices = this._vertices
    vertices(Y1) = y
    vertices(Y2) = y2
    vertices(Y3) = y2
    vertices(Y4) = y
  }

  def x: Float = _x

  def y: Float = _y

  /** Sets the x position so that it is centered on the given x parameter */
  def setCenterX(x: Float): Unit =
    this.x = x - _width / 2

  /** Sets the y position so that it is centered on the given y parameter */
  def setCenterY(y: Float): Unit =
    this.y = y - _height / 2

  /** Sets the position so that the sprite is centered on (x, y) */
  def setCenter(x: Float, y: Float): Unit =
    setPosition(x - _width / 2, y - _height / 2)

  /** Sets the x position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateX(xAmount: Float): Unit = boundary {
    this._x += xAmount

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val vertices = this._vertices
    vertices(X1) += xAmount
    vertices(X2) += xAmount
    vertices(X3) += xAmount
    vertices(X4) += xAmount
  }

  /** Sets the y position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translateY(yAmount: Float): Unit = boundary {
    _y += yAmount

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val vertices = this._vertices
    vertices(Y1) += yAmount
    vertices(Y2) += yAmount
    vertices(Y3) += yAmount
    vertices(Y4) += yAmount
  }

  /** Sets the position relative to the current position where the sprite will be drawn. If origin, rotation, or scale are changed, it is slightly more efficient to translate after those operations.
    */
  def translate(xAmount: Float, yAmount: Float): Unit = boundary {
    _x += xAmount
    _y += yAmount

    if (dirty) break()
    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) {
      dirty = true
      break()
    }

    val vertices = this._vertices
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
  def color_=(tint: Color): Unit = {
    _color.set(tint)
    _packedColor = tint.toFloatBits()
    val vertices = this._vertices
    vertices(C1) = _packedColor
    vertices(C2) = _packedColor
    vertices(C3) = _packedColor
    vertices(C4) = _packedColor
  }

  /** Sets the alpha portion of the color used to tint this sprite. */
  def setAlpha(a: Float): Unit =
    if (_color.a != a) {
      _color.a = a
      _packedColor = _color.toFloatBits()
      _vertices(C1) = _packedColor
      _vertices(C2) = _packedColor
      _vertices(C3) = _packedColor
      _vertices(C4) = _packedColor
    }

  /** @see #setColor(Color) */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    _color.set(r, g, b, a)
    _packedColor = _color.toFloatBits()
    val vertices = this._vertices
    vertices(C1) = _packedColor
    vertices(C2) = _packedColor
    vertices(C3) = _packedColor
    vertices(C4) = _packedColor
  }

  /** Sets the packed color used to tint this sprite.
    * @see
    *   #setColor(Color)
    * @see
    *   Color#toFloatBits()
    */
  def packedColor_=(packedColor: Float): Unit =
    // Handle 0f/-0f special case
    if (
      packedColor != this._packedColor || (packedColor == 0f && this._packedColor == 0f
        && java.lang.Float.floatToIntBits(packedColor) != java.lang.Float.floatToIntBits(this._packedColor))
    ) {
      this._packedColor = packedColor
      Color.abgr8888ToColor(_color, packedColor)
      val vertices = this._vertices
      vertices(C1) = packedColor
      vertices(C2) = packedColor
      vertices(C3) = packedColor
      vertices(C4) = packedColor
    }

  /** Returns the color of this sprite. If the returned instance is manipulated, {@link #setColor(Color)} must be called afterward.
    */
  def color: Color =
    _color

  /** Returns the packed color of this sprite. */
  def packedColor: Float =
    _packedColor

  /** Sets the origin in relation to the sprite's position for scaling and rotation. */
  def setOrigin(originX: Float, originY: Float): Unit = {
    this._originX = originX
    this._originY = originY
    dirty = true
  }

  /** Place origin in the center of the sprite */
  def setOriginCenter(): Unit = {
    this._originX = _width / 2
    this._originY = _height / 2
    dirty = true
  }

  /** Sets the rotation of the sprite in degrees. Rotation is centered on the origin set in {@link #setOrigin(float, float)} */
  def rotation_=(degrees: Float): Unit = {
    this._rotation = degrees
    dirty = true
  }

  /** @return the rotation of the sprite in degrees */
  def rotation: Float =
    _rotation

  /** The origin influences {@link #setPosition(float, float)} , {@link #setRotation(float)} and the expansion direction of scaling {@link #setScale(float, float)}
    */
  def originX: Float =
    _originX

  /** The origin influences {@link #setPosition(float, float)} , {@link #setRotation(float)} and the expansion direction of scaling {@link #setScale(float, float)}
    */
  def originY: Float =
    _originY

  /** X scale of the sprite, independent of size set by {@link #setSize(float, float)} */
  def scaleX: Float =
    _scaleX

  /** Y scale of the sprite, independent of size set by {@link #setSize(float, float)} */
  def scaleY: Float =
    _scaleY

  /** @return the width of the sprite, not accounting for scale. */
  def width: Float =
    _width

  /** @return the height of the sprite, not accounting for scale. */
  def height: Float =
    _height

  /** Sets the sprite's rotation in degrees relative to the current rotation. Rotation is centered on the origin set in {@link #setOrigin(float, float)}
    */
  def rotate(degrees: Float): Unit = boundary {
    if (degrees == 0) break()
    _rotation += degrees
    dirty = true
  }

  /** Rotates this sprite 90 degrees in-place by rotating the texture coordinates. This rotation is unaffected by {@link #setRotation(float)} and {@link #rotate(float)} .
    */
  def rotate90(clockwise: Boolean): Unit = {
    val vertices = this._vertices

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
    this._scaleX = scaleXY
    this._scaleY = scaleXY
    dirty = true
  }

  /** Sets the sprite's scale for both X and Y. The sprite scales out from the origin. This will not affect the values returned by {@link #getWidth()} and {@link #getHeight()}
    */
  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this._scaleX = scaleX
    this._scaleY = scaleY
    dirty = true
  }

  /** Sets the sprite's scale relative to the current scale. for example: original scale 2 -> sprite.scale(4) -> final scale 6. The sprite scales out from the origin. This will not affect the values
    * returned by {@link #getWidth()} and {@link #getHeight()}
    */
  def scale(amount: Float): Unit = {
    this._scaleX += amount
    this._scaleY += amount
    dirty = true
  }

  /** Returns the packed vertices, colors, and texture coordinates for this sprite. */
  def vertices: Array[Float] = {
    if (dirty) {
      dirty = false

      val vertices     = this._vertices
      var localX       = -_originX
      var localY       = -_originY
      var localX2      = localX + _width
      var localY2      = localY + _height
      val worldOriginX = this._x - localX
      val worldOriginY = this._y - localY
      if (_scaleX != 1 || _scaleY != 1) {
        localX *= _scaleX
        localY *= _scaleY
        localX2 *= _scaleX
        localY2 *= _scaleY
      }
      if (_rotation != 0) {
        val cos        = MathUtils.cosDeg(_rotation)
        val sin        = MathUtils.sinDeg(_rotation)
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
    _vertices
  }

  /** Returns the bounding axis aligned {@link Rectangle} that bounds this sprite. The rectangles x and y coordinates describe its bottom left corner. If you change the position or size of the sprite,
    * you have to fetch the triangle again for it to be recomputed.
    *
    * @return
    *   the bounding Rectangle
    */
  def boundingRectangle: Rectangle = {
    val verts = vertices

    var minx = verts(X1)
    var miny = verts(Y1)
    var maxx = verts(X1)
    var maxy = verts(Y1)

    minx = if (minx > verts(X2)) verts(X2) else minx
    minx = if (minx > verts(X3)) verts(X3) else minx
    minx = if (minx > verts(X4)) verts(X4) else minx

    maxx = if (maxx < verts(X2)) verts(X2) else maxx
    maxx = if (maxx < verts(X3)) verts(X3) else maxx
    maxx = if (maxx < verts(X4)) verts(X4) else maxx

    miny = if (miny > verts(Y2)) verts(Y2) else miny
    miny = if (miny > verts(Y3)) verts(Y3) else miny
    miny = if (miny > verts(Y4)) verts(Y4) else miny

    maxy = if (maxy < verts(Y2)) verts(Y2) else maxy
    maxy = if (maxy < verts(Y3)) verts(Y3) else maxy
    maxy = if (maxy < verts(Y4)) verts(Y4) else maxy

    if (Nullable(bounds).isEmpty) bounds = Rectangle()
    bounds.x = minx
    bounds.y = miny
    bounds.width = maxx - minx
    bounds.height = maxy - miny
    bounds
  }

  def draw(batch: Batch): Unit =
    batch.draw(texture, vertices, 0, Sprite.SPRITE_SIZE)

  def draw(batch: Batch, alphaModulation: Float): Unit = {
    val oldAlpha = color.a
    setAlpha(oldAlpha * alphaModulation)
    draw(batch)
    setAlpha(oldAlpha)
  }

  override def setRegion(u: Float, v: Float, u2: Float, v2: Float): Unit = {
    super.setRegion(u, v, u2, v2)

    val vertices = this._vertices
    vertices(U1) = u
    vertices(V1) = v2

    vertices(U2) = u
    vertices(V2) = v

    vertices(U3) = u2
    vertices(V3) = v

    vertices(U4) = u2
    vertices(V4) = v2
  }

  override def u_=(u: Float): Unit = {
    super.u_=(u)
    _vertices(U1) = u
    _vertices(U2) = u
  }

  override def v_=(v: Float): Unit = {
    super.v_=(v)
    _vertices(V2) = v
    _vertices(V3) = v
  }

  override def u2_=(u2: Float): Unit = {
    super.u2_=(u2)
    _vertices(U3) = u2
    _vertices(U4) = u2
  }

  override def v2_=(v2: Float): Unit = {
    super.v2_=(v2)
    _vertices(V1) = v2
    _vertices(V4) = v2
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
    if (flipX != x) {
      performX = true
    }
    if (flipY != y) {
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
    val vertices = this._vertices
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
    val vertices = this._vertices
    if (xAmount != 0) {
      val newU  = (vertices(U1) + xAmount) % 1
      val newU2 = newU + _width / texture.getWidth.toFloat
      this.u = newU
      this.u2 = newU2
      vertices(U1) = newU
      vertices(U2) = newU
      vertices(U3) = newU2
      vertices(U4) = newU2
    }
    if (yAmount != 0) {
      val newV  = (vertices(V2) + yAmount) % 1
      val newV2 = newV + _height / texture.getHeight.toFloat
      this.v = newV
      this.v2 = newV2
      vertices(V1) = newV2
      vertices(V2) = newV
      vertices(V3) = newV
      vertices(V4) = newV2
    }
  }

  def setU(u: Float, width: Float): Unit = {
    val newU2 = u + width / texture.getWidth.toFloat
    this.u = u
    this.u2 = newU2
  }

  def setV(v: Float, height: Float): Unit = {
    val newV2 = v + height / texture.getHeight.toFloat
    this.v = v
    this.v2 = newV2
  }
}

object Sprite {
  final val VERTEX_SIZE = 2 + 1 + 2
  final val SPRITE_SIZE = 4 * VERTEX_SIZE
}
