/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original source: colorful/src/main/java/com/github/tommyettinger/colorful/ipt_hq/ColorfulSprite.java
 * Original authors: mzechner, Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: boundary/break for early returns; no null in API
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (x, y, width, height, originX, originY,
 *          scaleX, scaleY, rotation, vertices, boundingRectangle);
 *          setX/setY -> x_=/y_= Scala setters; getColor/getColorTweak -> color/colorTweak;
 *          setPackedColor delegates to setColor(float) matching original
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 702
 * Covenant-baseline-methods: ColorfulSprite,SPRITE_SIZE,VERTEX_SIZE,_height,_originX,_originY,_rotation,_scaleX,_scaleY,_vertices,_width,_x,_y,boundingRectangle,bounds,color,colorTweak,currentColor,dirty,draw,flip,height,maxx,maxy,minx,miny,oldAlpha,originX,originY,performX,performY,rotate,rotate90,rotation,rotation_,scale,scaleX,scaleY,scroll,set,setAlpha,setBounds,setCenter,setCenterX,setCenterY,setColor,setFlip,setOrigin,setOriginBasedPosition,setOriginCenter,setPackedColor,setPosition,setRegion,setScale,setSize,setTweak,setTweakedColor,this,translate,translateX,translateY,tweak,u2_,u_,v2_,v_,vertices,verts,width,x,x2,x_,y,y2,y_
 * Covenant-source-reference: colorful/src/main/java/com/github/tommyettinger/colorful/ipt_hq/ColorfulSprite.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package ipt_hq

import sge.colorful.FloatColors
import sge.colorful.ipt_hq.ColorfulBatch.*
import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.math.MathUtils
import sge.math.Rectangle
import sge.utils.Nullable
import scala.compiletime.uninitialized
import scala.util.boundary
import scala.util.boundary.break

/** Holds the geometry, color, and texture information for drawing 2D sprites using {@link ColorfulBatch} . A ColorfulSprite has a position and a size given as width and height. The position is
  * relative to the origin of the coordinate system specified via {@link ColorfulBatch#begin()} and the respective matrices. A ColorfulSprite is always rectangular and its position (x, y) are located
  * in the bottom left corner of that rectangle. A ColorfulSprite also has an origin around which rotations and scaling are performed (that is, the origin is not modified by rotation and scaling). The
  * origin is given relative to the bottom left corner of the ColorfulSprite, its position.
  *
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  * @author
  *   Tommy Ettinger
  */
class ColorfulSprite() extends TextureRegion {

  final private val _vertices: Array[Float] = Array.ofDim[Float](ColorfulSprite.SPRITE_SIZE)
  private var _x:              Float        = uninitialized
  private var _y:              Float        = uninitialized
  private var _width:          Float        = uninitialized
  private var _height:         Float        = uninitialized
  private var _originX:        Float        = uninitialized
  private var _originY:        Float        = uninitialized
  private var _rotation:       Float        = uninitialized
  private var _scaleX:         Float        = 1
  private var _scaleY:         Float        = 1
  private var dirty:           Boolean      = true
  private var bounds:          Rectangle    = uninitialized

  /** Creates an uninitialized sprite. The sprite will need a texture region and bounds set before it can be drawn. */
  setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)

  def this(texture: Texture) = {
    this()
    this.texture = texture
    super.setRegion(0, 0, texture.width.toInt, texture.height.toInt)
    setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)
    setSize(Math.abs(texture.width.toInt).toFloat, Math.abs(texture.height.toInt).toFloat)
    setOrigin(_width * 0.5f, _height * 0.5f)
  }

  def this(texture: Texture, srcWidth: Int, srcHeight: Int) = {
    this()
    this.texture = texture
    super.setRegion(0, 0, srcWidth, srcHeight): Unit
    setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)
    setSize(Math.abs(srcWidth).toFloat, Math.abs(srcHeight).toFloat)
    setOrigin(_width * 0.5f, _height * 0.5f)
  }

  def this(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) = {
    this()
    if (texture == null) throw new IllegalArgumentException("texture cannot be null.") // Java interop guard
    this.texture = texture
    super.setRegion(srcX, srcY, srcWidth, srcHeight)
    setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)
    setSize(Math.abs(srcWidth).toFloat, Math.abs(srcHeight).toFloat)
    setOrigin(_width * 0.5f, _height * 0.5f)
  }

  def this(region: TextureRegion) = {
    this()
    setRegion(region)
    setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)
    setSize(region.regionWidth.toFloat, region.regionHeight.toFloat)
    setOrigin(_width * 0.5f, _height * 0.5f)
  }

  def this(region: TextureRegion, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) = {
    this()
    setRegion(region, srcX, srcY, srcWidth, srcHeight)
    setTweakedColor(Palette.GRAY, ColorfulBatch.TWEAK_RESET)
    setSize(Math.abs(srcWidth).toFloat, Math.abs(srcHeight).toFloat)
    setOrigin(_width * 0.5f, _height * 0.5f)
  }

  def this(colorfulSprite: ColorfulSprite) = {
    this()
    set(colorfulSprite)
  }

  def set(colorfulSprite: ColorfulSprite): Unit = {
    System.arraycopy(colorfulSprite._vertices, 0, _vertices, 0, ColorfulSprite.SPRITE_SIZE)
    setRegion(colorfulSprite)
    _x = colorfulSprite._x
    _y = colorfulSprite._y
    _width = colorfulSprite._width
    _height = colorfulSprite._height
    regionWidth = colorfulSprite.regionWidth
    regionHeight = colorfulSprite.regionHeight
    _originX = colorfulSprite._originX
    _originY = colorfulSprite._originY
    _rotation = colorfulSprite._rotation
    _scaleX = colorfulSprite._scaleX
    _scaleY = colorfulSprite._scaleY
    setTweakedColor(colorfulSprite._vertices(C1), colorfulSprite._vertices(T1))
    if (Nullable(colorfulSprite.bounds).isDefined)
      bounds = Rectangle(colorfulSprite.bounds)
    dirty = colorfulSprite.dirty
  }

  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = boundary {
    this._x = x
    this._y = y
    this._width = width
    this._height = height

    if (dirty) break()

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

    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) dirty = true
  }

  def setSize(width: Float, height: Float): Unit = boundary {
    this._width = width
    this._height = height

    if (dirty) break()

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

    if (_rotation != 0 || _scaleX != 1 || _scaleY != 1) dirty = true
  }

  def setPosition(x: Float, y: Float): Unit =
    translate(x - this._x, y - this._y)

  def setOriginBasedPosition(x: Float, y: Float): Unit =
    setPosition(x - this._originX, y - this._originY)

  def x_=(x: Float): Unit =
    translateX(x - this._x)

  def y_=(y: Float): Unit =
    translateY(y - this._y)

  def setCenterX(x: Float): Unit =
    this.x = x - _width * 0.5f

  def setCenterY(y: Float): Unit =
    this.y = y - _height * 0.5f

  def setCenter(x: Float, y: Float): Unit = {
    setCenterX(x)
    setCenterY(y)
  }

  def translateX(xAmount: Float): Unit = boundary {
    this._x += xAmount

    if (dirty) break()

    val vertices = this._vertices
    vertices(X1) += xAmount
    vertices(X2) += xAmount
    vertices(X3) += xAmount
    vertices(X4) += xAmount
  }

  def translateY(yAmount: Float): Unit = boundary {
    _y += yAmount

    if (dirty) break()

    val vertices = this._vertices
    vertices(Y1) += yAmount
    vertices(Y2) += yAmount
    vertices(Y3) += yAmount
    vertices(Y4) += yAmount
  }

  def translate(xAmount: Float, yAmount: Float): Unit = boundary {
    _x += xAmount
    _y += yAmount

    if (dirty) break()

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

  /** Sets the color used to tint this sprite. Default is {@link Palette#GRAY} , which makes no changes to the color. Use {@link ColorTools#ipt(float, float, float, float)} or a predefined color from
    * {@link Palette} if you don't have a color currently.
    *
    * @param color
    *   the packed float color used to add intensity, protan, and tritan to the current sprite, as well as the multiplier for alpha
    */
  def setColor(color: Float): Unit = {
    val vertices = this._vertices
    vertices(C1) = color
    vertices(C2) = color
    vertices(C3) = color
    vertices(C4) = color
  }

  /** Sets the color used to tint this sprite and the tweak that affects how that color will be treated.
    *
    * @param color
    *   the packed float color used to add intensity, protan, and tritan to the current sprite, as well as the multiplier for alpha
    * @param tweak
    *   the packed float used to multiply intensity, protan, and tritan, as well as the setting for contrast
    */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    val vertices = this._vertices
    vertices(C1) = color
    vertices(C2) = color
    vertices(C3) = color
    vertices(C4) = color
    vertices(T1) = tweak
    vertices(T2) = tweak
    vertices(T3) = tweak
    vertices(T4) = tweak
  }

  /** Sets the color used to tint this sprite and the tweak that affects how that color will be treated.
    *
    * @param intensityAdd
    *   how much lightness/intensity to add; darkest is 0f, neutral is 0.5f, lightest is 1f
    * @param protanAdd
    *   how much to add on the cyan/red axis; more cyan is 0f, neutral is 0.5f, more red is 1f
    * @param tritanAdd
    *   how much to add on the blue/yellow axis; more blue is 0f, neutral is 0.5f, more yellow is 1f
    * @param alphaMul
    *   how much to multiply alpha by; fully transparent is 0f, neutral is 1f
    * @param intensityMul
    *   how much source lightness/intensity should be multiplied by; darkest is 0f, neutral is 0.5f, lightest is 1f
    * @param protanMul
    *   how much the source's cyan/red axis should be multiplied by; no cyan/red is 0f, neutral is 0.5f, max cyan/red is 1f
    * @param tritanMul
    *   how much the source's blue/yellow axis should be multiplied by; no blue/yellow is 0f, neutral is 0.5f, max blue/yellow is 1f
    * @param contrast
    *   how to affect the curvature of lightness in the source; 0f makes lightness very even, 0.5f doesn't change lightness, and 1f makes light colors lighter and dark colors darker
    */
  def setTweakedColor(intensityAdd: Float, protanAdd: Float, tritanAdd: Float, alphaMul: Float, intensityMul: Float, protanMul: Float, tritanMul: Float, contrast: Float): Unit = {
    val color    = ColorTools.ipt(intensityAdd, protanAdd, tritanAdd, alphaMul)
    val tweak    = ColorTools.ipt(intensityMul, protanMul, tritanMul, contrast)
    val vertices = this._vertices
    vertices(C1) = color
    vertices(C2) = color
    vertices(C3) = color
    vertices(C4) = color
    vertices(T1) = tweak
    vertices(T2) = tweak
    vertices(T3) = tweak
    vertices(T4) = tweak
  }

  /** Sets the tweak that affects how the rendered color will be treated.
    *
    * @param tweak
    *   the packed float used to multiply intensity, protan, and tritan, as well as the setting for contrast
    */
  def setTweak(tweak: Float): Unit = {
    val vertices = this._vertices
    vertices(T1) = tweak
    vertices(T2) = tweak
    vertices(T3) = tweak
    vertices(T4) = tweak
  }

  /** Given an RGBA8888 Color from libGDX, this converts that to an IPT color as a packed float and sets the color of the sprite using that.
    *
    * @param color
    *   a libGDX RGBA8888 Color
    */
  def setColor(color: Color): Unit =
    setColor(ColorTools.fromColor(color))

  /** Sets the alpha portion of the color used to tint this sprite. */
  def setAlpha(alpha: Float): Unit = {
    var currentColor = _vertices(C1)
    if (ColorTools.alpha(currentColor) != alpha) {
      currentColor = FloatColors.setAlpha(currentColor, alpha)
      val vertices = this._vertices
      vertices(C1) = currentColor
      vertices(C2) = currentColor
      vertices(C3) = currentColor
      vertices(C4) = currentColor
    }
  }

  /** @see #setColor(float) */
  def setColor(intensity: Float, protan: Float, tritan: Float, alpha: Float): Unit = {
    val color    = ColorTools.ipt(intensity, protan, tritan, alpha)
    val vertices = this._vertices
    vertices(C1) = color
    vertices(C2) = color
    vertices(C3) = color
    vertices(C4) = color
  }

  /** @see #setTweak(float) */
  def setTweak(intensity: Float, protan: Float, tritan: Float, contrast: Float): Unit = {
    val tweak    = ColorTools.ipt(intensity, protan, tritan, contrast)
    val vertices = this._vertices
    vertices(T1) = tweak
    vertices(T2) = tweak
    vertices(T3) = tweak
    vertices(T4) = tweak
  }

  /** Exactly the same as {@link #setColor(float)} .
    *
    * @see
    *   #setColor(float)
    */
  def setPackedColor(packedColor: Float): Unit = {
    val vertices = this._vertices
    vertices(C1) = packedColor
    vertices(C2) = packedColor
    vertices(C3) = packedColor
    vertices(C4) = packedColor
  }

  /** Returns the color of this sprite.
    *
    * @return
    *   a packed float color used to add intensity, protan, and tritan to the current sprite, as well as the multiplier for alpha
    */
  def color: Float =
    _vertices(C1)

  /** Returns the multiplicative color tweaks used by this sprite, as a packed float with the same format as a color.
    *
    * @return
    *   a packed float used to multiply intensity, protan, and tritan, as well as the setting for contrast
    */
  def colorTweak: Float =
    _vertices(T1)

  def setOrigin(originX: Float, originY: Float): Unit = {
    this._originX = originX
    this._originY = originY
    dirty = true
  }

  /** Place origin in the center of the sprite */
  def setOriginCenter(): Unit = {
    this._originX = _width * 0.5f
    this._originY = _height * 0.5f
    dirty = true
  }

  def rotation_=(degrees: Float): Unit = {
    this._rotation = degrees
    dirty = true
  }

  def rotation: Float =
    _rotation

  def rotate(degrees: Float): Unit = boundary {
    if (degrees == 0) break()
    _rotation += degrees
    dirty = true
  }

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

  def setScale(scaleXY: Float): Unit = {
    this._scaleX = scaleXY
    this._scaleY = scaleXY
    dirty = true
  }

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this._scaleX = scaleX
    this._scaleY = scaleY
    dirty = true
  }

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

  def boundingRectangle: Rectangle = {
    val verts = vertices

    var minx = verts(X1)
    var miny = verts(Y1)
    var maxx = verts(X1)
    var maxy = verts(Y1)

    minx = Math.min(minx, verts(X2))
    minx = Math.min(minx, verts(X3))
    minx = Math.min(minx, verts(X4))

    maxx = Math.max(maxx, verts(X2))
    maxx = Math.max(maxx, verts(X3))
    maxx = Math.max(maxx, verts(X4))

    miny = Math.min(miny, verts(Y2))
    miny = Math.min(miny, verts(Y3))
    miny = Math.min(miny, verts(Y4))

    maxy = Math.max(maxy, verts(Y2))
    maxy = Math.max(maxy, verts(Y3))
    maxy = Math.max(maxy, verts(Y4))

    if (Nullable(bounds).isEmpty) bounds = Rectangle()
    bounds.x = minx
    bounds.y = miny
    bounds.width = maxx - minx
    bounds.height = maxy - miny
    bounds
  }

  def draw(batch: ColorfulBatch): Unit =
    batch.drawExactly(texture, vertices, 0, ColorfulSprite.SPRITE_SIZE)

  def draw(batch: ColorfulBatch, alphaModulation: Float): Unit = {
    val oldAlpha = ColorTools.alpha(color)
    setAlpha(oldAlpha * alphaModulation)
    draw(batch)
    setAlpha(oldAlpha)
  }

  def x: Float = _x

  def y: Float = _y

  def width: Float = _width

  def height: Float = _height

  def originX: Float = _originX

  def originY: Float = _originY

  def scaleX: Float = _scaleX

  def scaleY: Float = _scaleY

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
      val newU2 = newU + _width / texture.width.toFloat
      this.u = newU
      this.u2 = newU2
      vertices(U1) = newU
      vertices(U2) = newU
      vertices(U3) = newU2
      vertices(U4) = newU2
    }
    if (yAmount != 0) {
      val newV  = (vertices(V2) + yAmount) % 1
      val newV2 = newV + _height / texture.height.toFloat
      this.v = newV
      this.v2 = newV2
      vertices(V1) = newV2
      vertices(V2) = newV
      vertices(V3) = newV
      vertices(V4) = newV2
    }
  }
}

object ColorfulSprite {
  final val VERTEX_SIZE = 2 + 1 + 2 + 1
  final val SPRITE_SIZE = 4 * VERTEX_SIZE
}
