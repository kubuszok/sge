/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original source: com/github/tommyettinger/colorful/ipt/ColorfulBatch.java
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close(); null ShaderProgram -> Nullable[ShaderProgram]; getColor/setColor -> color/color_=
 *   Convention: Nullable for shader/texture; using Sge context parameter; AutoCloseable; createDefaultShader in companion
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (matching Batch trait)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1413
 * Covenant-baseline-methods: C1,C2,C3,C4,ColorfulBatch,SPRITE_SIZE,T1,T2,T3,T4,TWEAK_ATTRIBUTE,TWEAK_RESET,U1,U2,U3,U4,V1,V2,V3,V4,X1,X2,X3,X4,Y1,Y2,Y3,Y4,_blendDstFunc,_blendDstFuncAlpha,_blendSrcFunc,_blendSrcFuncAlpha,_projectionMatrix,_shader,_transformMatrix,_tweak,activeShader,adjustedCount,blendDstFunc,blendDstFuncAlpha,blendSrcFunc,blendSrcFuncAlpha,blendingDisabled,blendingEnabled,c,close,color,colorPacked,color_,combinedMatrix,copyCount,createDefaultShader,currentDataType,currentOffset,customShader,disableBlending,draw,drawExactly,drawing,enableBlending,flush,fragmentShader,fx,fx2,fy,fy2,gl,i,idx,indices,intBits,invTexHeight,invTexWidth,j,lastTexture,len,localIdx,maxSpritesInBatch,mesh,ownsShader,p1x,p1y,p2x,p2y,p3x,p3y,p4x,p4y,packedColor,packedColor_,projectionMatrix,projectionMatrix_,remainingCount,remainingVertices,renderCalls,s,setBlendFunction,setBlendFunctionSeparate,setColor,setIntColor,setIntTweak,setTweak,setTweakedColor,setupMatrices,shader,shader_,switchTexture,tempColor,texture,totalRenderCalls,transformMatrix,transformMatrix_,tweak,tweak_,u,u2,v,v2,vertexDataType,vertexShader,vertices,verticesLength,vv,worldOriginX,worldOriginY,x1,x2,x3,x4,y1,y2,y3,y4
 * Covenant-source-reference: com/github/tommyettinger/colorful/ipt/ColorfulBatch.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package ipt

import sge.graphics.{ BlendFactor, Color, DataType, EnableCap, GL20, Mesh, PrimitiveMode, Texture, VertexAttribute, VertexAttributes }
import sge.graphics.Mesh.VertexDataType
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, MathUtils, Matrix4 }
import sge.utils.{ Nullable, NumberUtils }

import scala.annotation.publicInBinary
import scala.compiletime.uninitialized
import scala.language.implicitConversions

import java.nio.Buffer

/** A substitute for {@link sge.graphics.g2d.SpriteBatch} that adds an additional attribute to store an extra color's worth of channels, used to modify the intensity and chromatic channels of a color
  * by multiplication (called the "tweak") while the primary color affects the color additively (just called the color, often drawn from {@link Palette} or generated with {@link ColorTools}). This
  * ColorfulBatch uses the IPT color space where SpriteBatch normally uses RGBA, which means that in the batch color, red maps to additive intensity, green maps to additive protan, blue maps to
  * additive tritan, and alpha continues to be multiplicative alpha. Additive values cause no change when they are 0.5, but cause a sharp increase at 1.0 and a sharp decrease at 0.0. In the tweak, red
  * maps to multiplicative intensity, green maps to multiplicative protan, blue maps to multiplicative tritan, and alpha maps to "exponential-like" contrast. Like with the additive values, 0.5 causes
  * no change for multiplicative values, but you can think of the existing color as being temporarily shifted down by 0.5 for each of the IPT channels, so each IPT channel's range is -0.5 to 0.5
  * before being multiplied by the corresponding tweak value (times 2.0, which means multiplicative tweak values less than 0.5 make colors closer to grayscale and values greater than 0.5 make colors
  * more vibrant) and then shifted back to its prior range. Contrast is a special value; 0.5 still means "no change," but lower values will make intensity changes less distinct between similar colors,
  * while higher values will "sharpen" the changes in intensity. <br> Created by Tommy Ettinger on 12/14/2019.
  *
  * @param size
  *   The max number of sprites in a single batch. Max of 16383.
  * @param defaultShader
  *   The default shader to use. This is not owned by the ColorfulBatch and must be disposed separately.
  */
class ColorfulBatch(size: Int = 1000, defaultShader: Nullable[ShaderProgram] = Nullable.empty)(using Sge) extends Batch with AutoCloseable {

  import ColorfulBatch.SPRITE_SIZE

  // 65535 is max vertex index, so 65535 / 4 vertices per sprite = 16383 sprites max.
  if (size > 16383) throw new IllegalArgumentException("Can't have more than 16383 sprites per batch: " + size)

  private var currentDataType: VertexDataType = uninitialized

  /** Internal; not intended for external usage and undocumented. */
  protected var mesh: Mesh = uninitialized

  /** Internal; not intended for external usage and undocumented. */
  protected val vertices: Array[Float] = Array.ofDim[Float](size * SPRITE_SIZE)

  /** Internal; not intended for external usage and undocumented. */
  protected var idx: Int = 0

  /** Internal; not intended for external usage and undocumented. */
  protected var lastTexture: Nullable[Texture] = Nullable.empty

  /** Internal; not intended for external usage and undocumented. */
  protected var invTexWidth: Float = 0

  /** Internal; not intended for external usage and undocumented. */
  protected var invTexHeight: Float = 0

  /** Internal; not intended for external usage and undocumented. */
  var drawing: Boolean = false

  /** Internal; not intended for external usage and undocumented. */
  private val _transformMatrix: Matrix4 = Matrix4()

  /** Internal; not intended for external usage and undocumented. */
  private val _projectionMatrix: Matrix4 = Matrix4()

  /** Internal; not intended for external usage and undocumented. */
  private val combinedMatrix: Matrix4 = Matrix4()

  /** Internal; not intended for external usage and undocumented. */
  private var blendingDisabled: Boolean = false

  /** Internal; not intended for external usage and undocumented. */
  private var _blendSrcFunc: Int = GL20.GL_SRC_ALPHA

  /** Internal; not intended for external usage and undocumented. */
  private var _blendDstFunc: Int = GL20.GL_ONE_MINUS_SRC_ALPHA

  /** Internal; not intended for external usage and undocumented. */
  private var _blendSrcFuncAlpha: Int = GL20.GL_SRC_ALPHA

  /** Internal; not intended for external usage and undocumented. */
  private var _blendDstFuncAlpha: Int = GL20.GL_ONE_MINUS_SRC_ALPHA

  /** Internal; not intended for external usage and undocumented. */
  private val _shader: ShaderProgram = defaultShader.getOrElse(ColorfulBatch.createDefaultShader())

  /** Internal; not intended for external usage and undocumented. */
  private var customShader: Nullable[ShaderProgram] = Nullable.empty

  /** Internal; not intended for external usage and undocumented. */
  private val ownsShader: Boolean = defaultShader.isEmpty

  /** A packed float color added to the base color of a drawn pixel (which is typically from a Texture) after the tweak has been multiplied with that base color.
    */
  protected var colorPacked: Float = Palette.GRAY

  /** Internal; not intended for external usage and undocumented. */
  private val tempColor: Color = Color(0.5f, 0.5f, 0.5f, 1f) // IPT from Palette.GRAY

  /** A packed float color multiplied with 2.0 and the base color of a drawn pixel (which is typically from a Texture). This can be created with {@link ColorTools#ipt(float, float, float, float)} like
    * any other packed float color, but the IPT channels instead refer to multiplicative I, P, and T, and alpha used instead for contrast. This treats P and T as having been offset to match how IPT
    * normally represents chroma, centered on 0.0 instead of 0.5, before the multiplication. Giving this a channel value of 0.5 has nearly no change and 0.0 or 1.0 has extreme changes.
    */
  protected var _tweak: Float = ColorfulBatch.TWEAK_RESET

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  /** The maximum number of sprites rendered in one batch so far. * */
  var maxSpritesInBatch: Int = 0

  // Constructor body
  val vertexDataType =
    if (Sge().graphics.gl30.isDefined) VertexDataType.VertexBufferObjectWithVAO else VertexDataType.VertexBufferObject

  currentDataType = vertexDataType

  mesh = Mesh(
    currentDataType,
    false,
    size * 4,
    size * 6,
    VertexAttributes(
      VertexAttribute(Usage.Position, 2, DataType.Float, false, ShaderProgram.POSITION_ATTRIBUTE),
      VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, ShaderProgram.COLOR_ATTRIBUTE),
      VertexAttribute(Usage.TextureCoordinates, 2, DataType.Float, false, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
      VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, ColorfulBatch.TWEAK_ATTRIBUTE)
    )
  )

  _projectionMatrix.setToOrtho2D(0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)

  val len     = size * 6
  val indices = Array.ofDim[Short](len)
  var j: Short = 0
  for (i <- 0 until len by 6) {
    indices(i) = j
    indices(i + 1) = (j + 1).toShort
    indices(i + 2) = (j + 2).toShort
    indices(i + 3) = (j + 2).toShort
    indices(i + 4) = (j + 3).toShort
    indices(i + 5) = j
    j = (j + 4).toShort
  }
  mesh.setIndices(indices)

  // Pre bind the mesh to force the upload of indices data.
  if (currentDataType != VertexDataType.VertexArray) {
    mesh.indexData.bind()
    mesh.indexData.unbind()
  }

  @publicInBinary override private[sge] def begin(): Unit = {
    if (drawing) throw new IllegalStateException("ColorfulBatch.end must be called before begin.")
    renderCalls = 0

    Sge().graphics.gl.glDepthMask(false)
    customShader.getOrElse(_shader).bind()
    setupMatrices()

    drawing = true
  }

  @publicInBinary override private[sge] def end(): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before end.")
    if (idx > 0) flush()
    lastTexture = Nullable.empty
    drawing = false

    val gl = Sge().graphics.gl
    gl.glDepthMask(true)
    if (blendingEnabled) gl.glDisable(EnableCap.Blend)
  }

  override def color_=(tint: Color): Unit =
    colorPacked = tint.toFloatBits()

  /** Sets the color to the result of {@link ColorTools#ipt(float, float, float, float)} on the same arguments. For the IPT parameters, 0.5f is a neutral value (causing no change), while for alpha,
    * 1.0f is a neutral value. For IPT, higher values will add to the corresponding channel, while lower values will subtract from it.
    * @see
    *   ColorTools#ipt(float, float, float, float)
    * @param r
    *   like lightness; additive; ranges from 0 (black) to 1 (white)
    * @param g
    *   cool-to-warm, roughly; additive; ranges from 0 (green/cyan/blue) to 1 (orange/red/purple)
    * @param b
    *   artificial-to-natural, very roughly; additive; ranges from 0 (blue/purple) to 1 (green/yellow/orange)
    * @param a
    *   opacity, from 0 to 1; multiplicative
    */
  override def setColor(r: Float, g: Float, b: Float, a: Float): Unit =
    colorPacked = ColorTools.ipt(r, g, b, a)

  override def color: Color = {
    val intBits = NumberUtils.floatToRawIntBits(colorPacked)
    val c       = tempColor
    c.r = (intBits & 0xff) / 255f
    c.g = ((intBits >>> 8) & 0xff) / 255f
    c.b = ((intBits >>> 16) & 0xff) / 255f
    c.a = (intBits >>> 25) / 127f
    c
  }

  override def packedColor_=(packed: Float): Unit =
    this.colorPacked = packed

  override def packedColor: Float =
    colorPacked

  /** Sets the multiplicative and contrast parts of the shader's color changes. 0.5 is a neutral value that should have minimal effect on the image; using {@code (0.5f, 0.5f, 0.5f, 0.5f)} will
    * effectively remove the tweak.
    * @param intensity
    *   like lightness; multiplicative; ranges from 0 (black) to 1 (white)
    * @param protan
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green and red are removed) to 1 (green-ness or red-ness is emphasized)
    * @param tritan
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue and yellow are removed) to 1 (blue-ness or yellow-ness is emphasized)
    * @param contrast
    *   affects how lightness changes; ranges from 0 (low contrast, cloudy look) to 1 (high contrast, sharpened look)
    */
  def setTweak(intensity: Float, protan: Float, tritan: Float, contrast: Float): Unit =
    _tweak = ColorTools.ipt(intensity, protan, tritan, contrast)

  /** Sets the tweak using a single packed IPT float.
    * @see
    *   #setTweak(float, float, float, float)
    * @param tweak
    *   a packed IPT float, with contrast instead of alpha
    */
  def tweak_=(tweak: Float): Unit =
    _tweak = tweak

  def tweak: Float =
    _tweak

  /** Expects an int color in the format (intensity, 8 bits), (protan, 8 bits), (tritan, 8 bits), (alpha, 7 bits), (ignored, 1 bit).
    * @param color
    *   an int color with alpha in the least significant byte and intensity in the most significant
    */
  def setIntColor(color: Int): Unit =
    this.colorPacked = NumberUtils.intBitsToFloat(Integer.reverseBytes(color & -2))

  /** Sets the color with the given IPT and A parameters from 0 to 255. For the IPT parameters, 127 or 128 is a neutral value (causing no change), while for alpha, 255 is a neutral value. For IPT,
    * higher values will add to the corresponding channel, while lower values will subtract from it.
    * @see
    *   ColorTools#ipt(float, float, float, float)
    * @param intensity
    *   like lightness; additive; ranges from 0 (black) to 255 (white)
    * @param protan
    *   cool-to-warm, roughly; additive; ranges from 0 (green/cyan/blue) to 255 (orange/red/purple)
    * @param tritan
    *   artificial-to-natural, very roughly; additive; ranges from 0 (blue/purple) to 255 (green/yellow/orange)
    * @param alpha
    *   opacity, from 0 to 255 (254 is equivalent, since the lowest bit is discarded); multiplicative
    */
  def setIntColor(intensity: Int, protan: Int, tritan: Int, alpha: Int): Unit =
    colorPacked = NumberUtils.intBitsToFloat(
      (alpha << 24 & 0xfe000000)
        | (tritan << 16 & 0xff0000) | (protan << 8 & 0xff00) | (intensity & 0xff)
    )

  /** Takes the tweak as an int in the format: intensity (8 bits), protan (8 bits), tritan (8 bits), contrast (7 bits), (1 ignored bit at the end). An example would be 0x7820206E, which slightly
    * darkens (with intensity 0x78, slightly below the halfway point of 0x80), significantly reduces colorfulness with protan and tritan multipliers of 0x20 (closer to 0, so most colors will be almost
    * grayscale), and slightly reduces contrast (with contrast and the ignored bit as 0x6E, which is less than the halfway point).
    * @param tweak
    *   the tweak to use as an integer, with intensity in the most significant bits and contrast in least
    */
  def setIntTweak(tweak: Int): Unit =
    _tweak = NumberUtils.intBitsToFloat(Integer.reverseBytes(tweak & -2))

  /** Sets the multiplicative and contrast parts of the shader's color changes. 127 is a neutral value that should have minimal effect on the image; using {@code (127, 127, 127, 127)} will effectively
    * remove the tweak.
    * @param intensity
    *   like lightness; multiplicative; ranges from 0 (black) to 255 (white)
    * @param protan
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green and red are removed) to 255 (green-ness or red-ness is emphasized)
    * @param tritan
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue and yellow are removed) to 255 (blue-ness or yellow-ness is emphasized)
    * @param contrast
    *   affects how lightness changes; ranges from 0 (low contrast, cloudy look) to 255 (high contrast, sharpened look)
    */
  def setIntTweak(intensity: Int, protan: Int, tritan: Int, contrast: Int): Unit =
    _tweak = NumberUtils.intBitsToFloat(
      (contrast << 24 & 0xfe000000)
        | (tritan << 16 & 0xff0000) | (protan << 8 & 0xff00) | (intensity & 0xff)
    )

  /** A convenience method that sets both the color (with {@link #packedColor_=(Float)}) and the tweak (with {@link #tweak_=(Float)}) at the same time, using two packed floats.
    * @param color
    *   the additive components and alpha, as a packed float
    * @param tweak
    *   the multiplicative components and contrast, as a packed float
    */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.colorPacked = color
    this._tweak = tweak
  }

  /** A convenience method that sets both the color (with {@link #setColor(float, float, float, float)}) and the tweak (with {@link #setTweak(float, float, float, float)}) at the same time.
    * @param addIntensity
    *   like lightness; additive; ranges from 0 (black) to 1 (white)
    * @param addProtan
    *   cool-to-warm, roughly; additive; ranges from 0 (green/cyan/blue) to 1 (orange/red/purple)
    * @param addTritan
    *   artificial-to-natural, very roughly; additive; ranges from 0 (blue/purple) to 1 (green/yellow/orange)
    * @param mulAlpha
    *   opacity, from 0 to 1; multiplicative
    * @param mulIntensity
    *   like lightness; multiplicative; ranges from 0 (sets all intensity to 0) to 1 (doubles the image's intensity)
    * @param mulProtan
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green/cyan/blue) to 1 (orange/red/purple)
    * @param mulTritan
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue/purple) to 1 (green/yellow/orange)
    * @param contrast
    *   foggy-to-sharp lightness contrast; affects most other components; ranges from 0 (flat, foggy lightness) to 1 (sharp, crisp lightness)
    */
  def setTweakedColor(
    addIntensity: Float,
    addProtan:    Float,
    addTritan:    Float,
    mulAlpha:     Float,
    mulIntensity: Float,
    mulProtan:    Float,
    mulTritan:    Float,
    contrast:     Float
  ): Unit = {
    setColor(addIntensity, addProtan, addTritan, mulAlpha)
    setTweak(mulIntensity, mulProtan, mulTritan, contrast)
  }

  override def draw(
    texture:   Texture,
    x:         Float,
    y:         Float,
    originX:   Float,
    originY:   Float,
    width:     Float,
    height:    Float,
    scaleX:    Float,
    scaleY:    Float,
    rotation:  Float,
    srcX:      Int,
    srcY:      Int,
    srcWidth:  Int,
    srcHeight: Int,
    flipX:     Boolean,
    flipY:     Boolean
  ): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX
      fy *= scaleY
      fx2 *= scaleX
      fy2 *= scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx
    val p1y = fy
    val p2x = fx
    val p2y = fy2
    val p3x = fx2
    val p3y = fy2
    val p4x = fx2
    val p4y = fy

    var x1: Float = 0
    var y1: Float = 0
    var x2: Float = 0
    var y2: Float = 0
    var x3: Float = 0
    var y3: Float = 0
    var x4: Float = 0
    var y4: Float = 0

    // rotate
    if (rotation != 0) {
      val cos = MathUtils.cosDeg(rotation)
      val sin = MathUtils.sinDeg(rotation)

      x1 = cos * p1x - sin * p1y
      y1 = sin * p1x + cos * p1y

      x2 = cos * p2x - sin * p2y
      y2 = sin * p2x + cos * p2y

      x3 = cos * p3x - sin * p3y
      y3 = sin * p3x + cos * p3y

      x4 = x1 + (x3 - x2)
      y4 = y3 - (y2 - y1)
    } else {
      x1 = p1x
      y1 = p1y

      x2 = p2x
      y2 = p2y

      x3 = p3x
      y3 = p3y

      x4 = p4x
      y4 = p4y
    }

    x1 += worldOriginX
    y1 += worldOriginY
    x2 += worldOriginX
    y2 += worldOriginY
    x3 += worldOriginX
    y3 += worldOriginY
    x4 += worldOriginX
    y4 += worldOriginY

    var u  = srcX * invTexWidth
    var v  = (srcY + srcHeight) * invTexHeight
    var u2 = (srcX + srcWidth) * invTexWidth
    var v2 = srcY * invTexHeight

    if (flipX) {
      val tmp = u
      u = u2
      u2 = tmp
    }

    if (flipY) {
      val tmp = v
      v = v2
      v2 = tmp
    }

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x2
    vertices(localIdx + 7) = y2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = x3
    vertices(localIdx + 13) = y3
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = x4
    vertices(localIdx + 19) = y4
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    var u   = srcX * invTexWidth
    var v   = (srcY + srcHeight) * invTexHeight
    var u2  = (srcX + srcWidth) * invTexWidth
    var v2  = srcY * invTexHeight
    val fx2 = x + width
    val fy2 = y + height

    if (flipX) {
      val tmp = u
      u = u2
      u2 = tmp
    }

    if (flipY) {
      val tmp = v
      v = v2
      v2 = tmp
    }

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x
    vertices(localIdx + 7) = fy2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = fx2
    vertices(localIdx + 13) = fy2
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = fx2
    vertices(localIdx + 19) = y
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    val u   = srcX * invTexWidth
    val v   = (srcY + srcHeight) * invTexHeight
    val u2  = (srcX + srcWidth) * invTexWidth
    val v2  = srcY * invTexHeight
    val fx2 = x + srcWidth
    val fy2 = y + srcHeight

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x
    vertices(localIdx + 7) = fy2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = fx2
    vertices(localIdx + 13) = fy2
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = fx2
    vertices(localIdx + 19) = y
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    val fx2 = x + width
    val fy2 = y + height

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x
    vertices(localIdx + 7) = fy2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = fx2
    vertices(localIdx + 13) = fy2
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = fx2
    vertices(localIdx + 19) = y
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    draw(texture, x, y, texture.width.toFloat, texture.height.toFloat)

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    val fx2 = x + width
    val fy2 = y + height
    val u   = 0f
    val v   = 1f
    val u2  = 1f
    val v2  = 0f

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x
    vertices(localIdx + 7) = fy2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = fx2
    vertices(localIdx + 13) = fy2
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = fx2
    vertices(localIdx + 19) = y
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  /** This is very different from the other overloads in this class; it assumes the float array it is given is in the format libGDX uses to give to SpriteBatch, that is, in groups of 20 floats per
    * sprite. ColorfulBatch uses 24 floats per sprite, to add tweak per color, so this does some conversion.
    * @param texture
    *   the Texture being drawn from; usually an atlas or some parent Texture with lots of TextureRegions
    * @param spriteVertices
    *   not the same format as {@link #vertices} in this class; should have a length that's a multiple of 20
    * @param offset
    *   where to start drawing vertices from {@code spriteVertices}
    * @param count
    *   how many vertices to draw from {@code spriteVertices} (20 vertices is one sprite)
    */
  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    var adjustedCount     = (count / 5) * 6
    val verticesLength    = vertices.length
    var remainingVertices = verticesLength
    if (texture != lastTexture)
      switchTexture(texture)
    else {
      remainingVertices -= idx
      if (remainingVertices == 0) {
        flush()
        remainingVertices = verticesLength
      }
    }
    var copyCount = scala.math.min(remainingVertices, adjustedCount)
    val tweak     = this._tweak

    // new way, thanks mgsx
    var s  = offset
    var vv = idx
    var i  = 0
    while (i < copyCount) {
      vertices(vv) = spriteVertices(s)
      vv += 1; s += 1
      vertices(vv) = spriteVertices(s)
      vv += 1; s += 1
      vertices(vv) = spriteVertices(s)
      vv += 1; s += 1
      vertices(vv) = spriteVertices(s)
      vv += 1; s += 1
      vertices(vv) = spriteVertices(s)
      vv += 1; s += 1
      vertices(vv) = tweak
      vv += 1
      i += 6
    }
    idx += copyCount
    adjustedCount -= copyCount
    var currentOffset = offset
    while (adjustedCount > 0) {
      currentOffset += (copyCount / 6) * 5
      flush()
      copyCount = scala.math.min(verticesLength, adjustedCount)
      // new way, thanks mgsx
      s = currentOffset
      vv = 0
      i = 0
      while (i < copyCount) {
        vertices(vv) = spriteVertices(s)
        vv += 1; s += 1
        vertices(vv) = spriteVertices(s)
        vv += 1; s += 1
        vertices(vv) = spriteVertices(s)
        vv += 1; s += 1
        vertices(vv) = spriteVertices(s)
        vv += 1; s += 1
        vertices(vv) = spriteVertices(s)
        vv += 1; s += 1
        vertices(vv) = tweak
        vv += 1
        i += 6
      }
      idx += copyCount
      adjustedCount -= copyCount
    }
  }

  /** Meant for code that uses ColorfulBatch specifically and can set an extra float (for the color tweak) per vertex, this is just like {@link #draw(Texture, Array[Float], Int, Int)} when used in
    * other Batch implementations, but expects {@code spriteVertices} to have a length that is a multiple of 24 instead of 20.
    * @param texture
    *   the Texture being drawn from; usually an atlas or some parent Texture with lots of TextureRegions
    * @param spriteVertices
    *   vertices formatted as this class uses them; length should be a multiple of 24
    * @param offset
    *   where to start drawing vertices from {@code spriteVertices}
    * @param count
    *   how many vertices to draw from {@code spriteVertices} (24 vertices is one sprite)
    */
  def drawExactly(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    var remainingCount    = count
    val verticesLength    = vertices.length
    var remainingVertices = verticesLength
    if (texture != lastTexture)
      switchTexture(texture)
    else {
      remainingVertices -= idx
      if (remainingVertices == 0) {
        flush()
        remainingVertices = verticesLength
      }
    }
    var copyCount     = scala.math.min(remainingVertices, remainingCount)
    var currentOffset = offset

    System.arraycopy(spriteVertices, currentOffset, vertices, idx, copyCount)
    idx += copyCount
    remainingCount -= copyCount
    while (remainingCount > 0) {
      currentOffset += copyCount
      flush()
      copyCount = scala.math.min(verticesLength, remainingCount)
      System.arraycopy(spriteVertices, currentOffset, vertices, 0, copyCount)
      idx += copyCount
      remainingCount -= copyCount
    }
  }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    draw(region, x, y, region.regionWidth.toFloat, region.regionHeight.toFloat)

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    val texture = region.texture
    if (texture != lastTexture) {
      switchTexture(texture)
    } else if (idx == vertices.length) {
      flush()
    }

    val fx2 = x + width
    val fy2 = y + height
    val u   = region.u
    val v   = region.v2
    val u2  = region.u2
    val v2  = region.v

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x
    vertices(localIdx + 7) = fy2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = fx2
    vertices(localIdx + 13) = fy2
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = fx2
    vertices(localIdx + 19) = y
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    val texture = region.texture
    if (texture != lastTexture) {
      switchTexture(texture)
    } else if (idx == vertices.length) {
      flush()
    }

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX
      fy *= scaleY
      fx2 *= scaleX
      fy2 *= scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx
    val p1y = fy
    val p2x = fx
    val p2y = fy2
    val p3x = fx2
    val p3y = fy2
    val p4x = fx2
    val p4y = fy

    var x1: Float = 0
    var y1: Float = 0
    var x2: Float = 0
    var y2: Float = 0
    var x3: Float = 0
    var y3: Float = 0
    var x4: Float = 0
    var y4: Float = 0

    // rotate
    if (rotation != 0) {
      val cos = MathUtils.cosDeg(rotation)
      val sin = MathUtils.sinDeg(rotation)

      x1 = cos * p1x - sin * p1y
      y1 = sin * p1x + cos * p1y

      x2 = cos * p2x - sin * p2y
      y2 = sin * p2x + cos * p2y

      x3 = cos * p3x - sin * p3y
      y3 = sin * p3x + cos * p3y

      x4 = x1 + (x3 - x2)
      y4 = y3 - (y2 - y1)
    } else {
      x1 = p1x
      y1 = p1y

      x2 = p2x
      y2 = p2y

      x3 = p3x
      y3 = p3y

      x4 = p4x
      y4 = p4y
    }

    x1 += worldOriginX
    y1 += worldOriginY
    x2 += worldOriginX
    y2 += worldOriginY
    x3 += worldOriginX
    y3 += worldOriginY
    x4 += worldOriginX
    y4 += worldOriginY

    val u  = region.u
    val v  = region.v2
    val u2 = region.u2
    val v2 = region.v

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x2
    vertices(localIdx + 7) = y2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = x3
    vertices(localIdx + 13) = y3
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = x4
    vertices(localIdx + 19) = y4
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    val texture = region.texture
    if (texture != lastTexture) {
      switchTexture(texture)
    } else if (idx == vertices.length) {
      flush()
    }

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX
      fy *= scaleY
      fx2 *= scaleX
      fy2 *= scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx
    val p1y = fy
    val p2x = fx
    val p2y = fy2
    val p3x = fx2
    val p3y = fy2
    val p4x = fx2
    val p4y = fy

    var x1: Float = 0
    var y1: Float = 0
    var x2: Float = 0
    var y2: Float = 0
    var x3: Float = 0
    var y3: Float = 0
    var x4: Float = 0
    var y4: Float = 0

    // rotate
    if (rotation != 0) {
      val cos = MathUtils.cosDeg(rotation)
      val sin = MathUtils.sinDeg(rotation)

      x1 = cos * p1x - sin * p1y
      y1 = sin * p1x + cos * p1y

      x2 = cos * p2x - sin * p2y
      y2 = sin * p2x + cos * p2y

      x3 = cos * p3x - sin * p3y
      y3 = sin * p3x + cos * p3y

      x4 = x1 + (x3 - x2)
      y4 = y3 - (y2 - y1)
    } else {
      x1 = p1x
      y1 = p1y

      x2 = p2x
      y2 = p2y

      x3 = p3x
      y3 = p3y

      x4 = p4x
      y4 = p4y
    }

    x1 += worldOriginX
    y1 += worldOriginY
    x2 += worldOriginX
    y2 += worldOriginY
    x3 += worldOriginX
    y3 += worldOriginY
    x4 += worldOriginX
    y4 += worldOriginY

    val (u1, v1, u2, v2, u3, v3, u4, v4) = if (clockwise) {
      (region.u2, region.v2, region.u, region.v2, region.u, region.v, region.u2, region.v)
    } else {
      (region.u, region.v, region.u2, region.v, region.u2, region.v2, region.u, region.v2)
    }

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u1
    vertices(localIdx + 4) = v1
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x2
    vertices(localIdx + 7) = y2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u2
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = x3
    vertices(localIdx + 13) = y3
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u3
    vertices(localIdx + 16) = v3
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = x4
    vertices(localIdx + 19) = y4
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u4
    vertices(localIdx + 22) = v4
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!drawing) throw new IllegalStateException("ColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    val texture = region.texture
    if (texture != lastTexture) {
      switchTexture(texture)
    } else if (idx == vertices.length) {
      flush()
    }

    // construct corner points
    val x1 = transform.m02
    val y1 = transform.m12
    val x2 = transform.m01 * height + transform.m02
    val y2 = transform.m11 * height + transform.m12
    val x3 = transform.m00 * width + transform.m01 * height + transform.m02
    val y3 = transform.m10 * width + transform.m11 * height + transform.m12
    val x4 = transform.m00 * width + transform.m02
    val y4 = transform.m10 * width + transform.m12

    val u  = region.u
    val v  = region.v2
    val u2 = region.u2
    val v2 = region.v

    val color    = this.colorPacked
    val tweak    = this._tweak
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v
    vertices(localIdx + 5) = tweak

    vertices(localIdx + 6) = x2
    vertices(localIdx + 7) = y2
    vertices(localIdx + 8) = color
    vertices(localIdx + 9) = u
    vertices(localIdx + 10) = v2
    vertices(localIdx + 11) = tweak

    vertices(localIdx + 12) = x3
    vertices(localIdx + 13) = y3
    vertices(localIdx + 14) = color
    vertices(localIdx + 15) = u2
    vertices(localIdx + 16) = v2
    vertices(localIdx + 17) = tweak

    vertices(localIdx + 18) = x4
    vertices(localIdx + 19) = y4
    vertices(localIdx + 20) = color
    vertices(localIdx + 21) = u2
    vertices(localIdx + 22) = v
    vertices(localIdx + 23) = tweak
    this.idx = localIdx + 24
  }

  override def flush(): Unit =
    if (idx != 0) {
      renderCalls += 1
      totalRenderCalls += 1
      val spritesInBatch = idx / SPRITE_SIZE
      if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch
      val count = spritesInBatch * 6

      lastTexture.foreach(_.bind())
      val mesh = this.mesh
      mesh.setVertices(vertices, 0, idx)

      // Only upload indices for the vertex array type
      if (currentDataType == VertexDataType.VertexArray) {
        val indicesBuffer = mesh.getIndicesBuffer(true).asInstanceOf[Buffer]
        indicesBuffer.position(0)
        indicesBuffer.limit(count)
      }

      if (blendingDisabled) {
        Sge().graphics.gl.glDisable(EnableCap.Blend)
      } else {
        Sge().graphics.gl.glEnable(EnableCap.Blend)
        if (_blendSrcFunc != -1) Sge().graphics.gl.glBlendFuncSeparate(BlendFactor(_blendSrcFunc), BlendFactor(_blendDstFunc), BlendFactor(_blendSrcFuncAlpha), BlendFactor(_blendDstFuncAlpha))
      }

      mesh.render(customShader.getOrElse(_shader), PrimitiveMode.Triangles, 0, count)

      idx = 0
    }

  override def disableBlending(): Unit = {
    if (drawing) flush()
    blendingDisabled = true
  }

  override def enableBlending(): Unit = {
    if (drawing) flush()
    blendingDisabled = false
  }

  override def setBlendFunction(srcFunc: Int, dstFunc: Int): Unit =
    setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)

  override def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit =
    if (_blendSrcFunc != srcFuncColor || _blendDstFunc != dstFuncColor || _blendSrcFuncAlpha != srcFuncAlpha || _blendDstFuncAlpha != dstFuncAlpha) {
      if (drawing) flush()
      _blendSrcFunc = srcFuncColor
      _blendDstFunc = dstFuncColor
      _blendSrcFuncAlpha = srcFuncAlpha
      _blendDstFuncAlpha = dstFuncAlpha
    }

  override def blendSrcFunc: Int =
    _blendSrcFunc

  override def blendDstFunc: Int =
    _blendDstFunc

  override def blendSrcFuncAlpha: Int =
    _blendSrcFuncAlpha

  override def blendDstFuncAlpha: Int =
    _blendDstFuncAlpha

  override def close(): Unit = {
    mesh.close()
    if (ownsShader) _shader.close()
  }

  override def projectionMatrix: Matrix4 =
    _projectionMatrix

  override def transformMatrix: Matrix4 =
    _transformMatrix

  override def projectionMatrix_=(projection: Matrix4): Unit = {
    if (drawing) flush()
    _projectionMatrix.set(projection)
    if (drawing) setupMatrices()
  }

  override def transformMatrix_=(transform: Matrix4): Unit = {
    if (drawing) flush()
    _transformMatrix.set(transform)
    if (drawing) setupMatrices()
  }

  protected def setupMatrices(): Unit = {
    combinedMatrix.set(_projectionMatrix).mul(_transformMatrix)
    val activeShader = customShader.getOrElse(_shader)
    activeShader.setUniformMatrix("u_projTrans", combinedMatrix)
    activeShader.setUniformi("u_texture", 0)
  }

  protected def switchTexture(texture: Texture): Unit = {
    flush()
    lastTexture = texture
    invTexWidth = 1.0f / texture.width.toFloat
    invTexHeight = 1.0f / texture.height.toFloat
  }

  override def shader_=(shader: Nullable[ShaderProgram]): Unit =
    if (shader != customShader) { // avoid unnecessary flushing in case we are drawing
      if (drawing) {
        flush()
      }
      customShader = shader
      if (drawing) {
        customShader.getOrElse(this._shader).bind()
        setupMatrices()
      }
    }

  override def shader: ShaderProgram =
    customShader.getOrElse(_shader)

  override def blendingEnabled: Boolean =
    !blendingDisabled
}

object ColorfulBatch {

  /** How many floats are used for one "sprite" (position, color, texcoord, tweak). */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A constant packed float that can be assigned to the tweak to make all the tweak adjustments virtually imperceptible. When set, it won't change intensity, protan, or tritan multipliers or
    * contrast.
    */
  val TWEAK_RESET: Float = ColorTools.ipt(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the IPT ColorfulBatch. */
  val vertexShader: String =
    "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "attribute vec4 " + TWEAK_ATTRIBUTE + ";\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_lightFix;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "   v_tweak.a = pow(v_tweak.a * (255.0/254.0) + 0.5, 1.709);\n" +
      "   v_lightFix = 1.0 + pow(v_tweak.a, 1.41421356);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The default fragment shader for the IPT ColorfulBatch. */
  val fragmentShader: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "varying LOWP vec4 v_tweak;\n" +
      "varying float v_lightFix;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 ipt = mat3(0.189786, 0.669665 , 0.286498, 0.576951, -0.73741 , 0.655205, 0.233221, 0.0681367, -0.941748)\n" +
      "             * tgt.rgb;\n" +
      "  ipt.x = pow(ipt.x, v_tweak.a) * v_lightFix * v_tweak.r + v_color.r - 0.5;\n" +
      "  ipt.yz = (ipt.yz * v_tweak.gb + v_color.gb - 0.5) * 2.0;\n" +
      "  vec3 back = mat3(0.999779, 1.00015, 0.999769, 1.07094, -0.377744, 0.0629496, 0.324891, 0.220439, -0.809638) * ipt;\n" +
      "  gl_FragColor = clamp(vec4(back, v_color.a * tgt.a), 0.0, 1.0);\n" +
      "}"

  /** Creates the default ShaderProgram for this ColorfulBatch. */
  def createDefaultShader()(using sge.Sge): ShaderProgram = {
    val shader = new ShaderProgram(vertexShader, fragmentShader)
    if (!shader.compiled) {
      throw new IllegalArgumentException("Error compiling shader: " + shader.log)
    }
    shader
  }

  final val X1 = 0
  final val Y1 = 1
  final val C1 = 2
  final val U1 = 3
  final val V1 = 4
  final val T1 = 5
  final val X2 = 6
  final val Y2 = 7
  final val C2 = 8
  final val U2 = 9
  final val V2 = 10
  final val T2 = 11
  final val X3 = 12
  final val Y3 = 13
  final val C3 = 14
  final val U3 = 15
  final val V3 = 16
  final val T3 = 17
  final val X4 = 18
  final val Y4 = 19
  final val C4 = 20
  final val U4 = 21
  final val V4 = 22
  final val T4 = 23
}
