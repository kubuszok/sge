/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original source: com/github/tommyettinger/colorful/oklab/TextureArrayColorfulBatch.java
 * Original authors: mzechner (Original SpriteBatch), Nathan Sweet (Original SpriteBatch),
 *                   VaTTeRGeR (TextureArray Extension), Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close(); null ShaderProgram -> Nullable[ShaderProgram]; getColor/setColor -> color/color_=
 *   Convention: Nullable for shader/texture; using Sge context parameter; AutoCloseable; createDefaultShader in companion
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (matching Batch trait)
 *   Notes: Does not extend ColorfulBatch because the Scala port's parent fields are private;
 *          instead implements Batch directly with all needed fields, preserving full original logic.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1752
 * Covenant-baseline-methods: SPRITE_FLOAT_SIZE,SPRITE_VERTEX_SIZE,TEXTURE_INDEX_ATTRIBUTE,TWEAK_ATTRIBUTE,TWEAK_RESET,TextureArrayColorfulBatch,_blendDstFunc,_blendDstFuncAlpha,_blendSrcFunc,_blendSrcFuncAlpha,_maxTextureUnits,_projectionMatrix,_shader,_transformMatrix,_tweak,activateTexture,activeShader,adjustedCount,appType,blendDstFunc,blendDstFuncAlpha,blendSrcFunc,blendSrcFuncAlpha,blendingDisabled,blendingEnabled,buf,builtFragmentShader,c,close,color,colorPacked,color_,combinedMatrix,copyCount,createDefaultShader,currentOffset,currentTextureLFUSize,currentTextureLFUSwaps,customShader,disableBlending,draw,drawExactly,drawing,enableBlending,flush,flushIfFull,fragmentShader,fx,fx2,fy,fy2,getMaxTextureUnits,gl,i,idx,indices,intBits,invTexHeight,invTexWidth,j,len,maxSpritesInBatch,mesh,ownsShader,p1x,p1y,p2x,p2y,p3x,p3y,p4x,p4y,packedColor,packedColor_,prependFragment,prependVertex,projectionMatrix,projectionMatrix_,remainingVertices,renderCalls,s,setBlendFunction,setBlendFunctionSeparate,setColor,setIntColor,setIntTweak,setTweak,setTweakedColor,setupMatrices,shader,shaderErrorLog,shader_,tempColor,textureHandle,textureLFUCapacity,textureLFUSize,textureLFUSwaps,textureUnitIndicesBuffer,ti,totalRenderCalls,transformMatrix,transformMatrix_,tweak,tweak_,u,u2,usedTextures,usedTexturesLFU,v,v2,vertexDataType,vertexShader,vertexShaderOklabWithRGBATint,vertices,verticesLength,vv,worldOriginX,worldOriginY,x1,x2,x3,x4,y1,y2,y3,y4
 * Covenant-source-reference: com/github/tommyettinger/colorful/oklab/TextureArrayColorfulBatch.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package oklab

import sge.Application
import sge.graphics.{ BlendFactor, Color, DataType, EnableCap, GL20, Mesh, PrimitiveMode, Texture, VertexAttribute, VertexAttributes }
import sge.graphics.Mesh.VertexDataType
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, MathUtils, Matrix4 }
import sge.utils.{ BufferUtils, Nullable, NumberUtils }

import scala.annotation.publicInBinary
import scala.compiletime.uninitialized
import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import java.nio.Buffer
import java.nio.IntBuffer
import java.util.Arrays

/** Draws batched quads using indices. Like {@link ColorfulBatch}, this adds another attribute to store an extra color's worth of channels, but also has optimizations when rendering from multiple
  * Textures. This maintains an LFU texture-cache to combine draw calls with different textures effectively. <br> Use this Batch if you frequently utilize more than a single texture between calling
  * {@link #begin()} and {@link #end()}. An example would be if your Atlas is spread over multiple Textures or if you draw with individual Textures. This version is compatible with OpenGL ES 2.0.
  *
  * @see
  *   Batch
  * @see
  *   sge.graphics.g2d.SpriteBatch
  * @see
  *   ColorfulBatch
  * @author
  *   mzechner (Original SpriteBatch)
  * @author
  *   Nathan Sweet (Original SpriteBatch)
  * @author
  *   VaTTeRGeR (TextureArray Extension)
  *
  * @param size
  *   The max number of sprites in a single batch. Max of 8191.
  * @param defaultShader
  *   The default shader to use. This is not owned by the TextureArrayColorfulBatch and must be disposed separately.
  * @throws IllegalStateException
  *   Thrown if the device does not support texture arrays. Make sure to implement a Fallback to {@link ColorfulBatch} in case Texture Arrays are not supported on a client's device.
  * @see
  *   TextureArrayColorfulBatch#createDefaultShader(int)
  * @see
  *   TextureArrayColorfulBatch#getMaxTextureUnits()
  */
class TextureArrayColorfulBatch(size: Int = 1000, defaultShader: Nullable[ShaderProgram] = Nullable.empty)(using Sge) extends Batch with AutoCloseable {

  import TextureArrayColorfulBatch.*

  // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
  if (size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size)

  private val _maxTextureUnits: Int = TextureArrayColorfulBatch.getMaxTextureUnits()

  if (_maxTextureUnits == 0) {
    throw new IllegalStateException(
      "Texture Arrays are not supported on this device:\n" + TextureArrayColorfulBatch.shaderErrorLog.getOrElse("")
    )
  }

  /** Internal; not intended for external usage and undocumented. */
  private val _shader: ShaderProgram = defaultShader.getOrElse(TextureArrayColorfulBatch.createDefaultShader(_maxTextureUnits))

  /** Internal; not intended for external usage and undocumented. */
  private val ownsShader: Boolean = defaultShader.isEmpty

  /** Textures in use (index: Texture Unit, value: Texture). This is managed internally, and should only be used by this class or carefully by subclasses.
    */
  protected val usedTextures: Array[Nullable[Texture]] = Array.fill[Nullable[Texture]](_maxTextureUnits)(Nullable.empty)

  /** LFU Array (index: Texture Unit Index - value: Access frequency). This is managed internally, and should only be used by this class or carefully by subclasses.
    */
  protected val usedTexturesLFU: Array[Int] = Array.ofDim[Int](_maxTextureUnits)

  /** Gets sent to the fragment shader as a uniform "uniform sampler2d[X] u_textures". This is managed internally, and should only be used by this class or carefully by subclasses.
    */
  protected val textureUnitIndicesBuffer: IntBuffer = {
    val buf = BufferUtils.newIntBuffer(_maxTextureUnits)
    var i   = 0
    while (i < _maxTextureUnits) {
      buf.put(i)
      i += 1
    }
    buf.flip()
    buf
  }

  /** Internal; not intended for external usage and undocumented. */
  protected var mesh: Mesh = uninitialized

  /** Internal; not intended for external usage and undocumented. */
  protected val vertices: Array[Float] = Array.ofDim[Float](size * SPRITE_FLOAT_SIZE)

  /** Internal; not intended for external usage and undocumented. */
  protected var idx: Int = 0

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
  private var customShader: Nullable[ShaderProgram] = Nullable.empty

  /** A packed float color added to the base color of a drawn pixel (which is typically from a Texture) after the tweak has been multiplied with that base color.
    */
  protected var colorPacked: Float = Palette.GRAY

  /** Internal; not intended for external usage and undocumented. */
  private val tempColor: Color = Color(0.5019608f, 0.49803922f, 0.49803922f, 1f) // LAB from Palette.GRAY

  /** A packed float color multiplied with 2.0 and the base color of a drawn pixel (which is typically from a Texture). This can be created with {@link ColorTools#oklab(float, float, float, float)}
    * like any other packed float color, but the LAB channels instead refer to multiplicative L, A, and B, and alpha used instead for contrast. This treats A and B as having been offset to match how
    * Oklab normally represents chroma, centered on 0.0 instead of 0.5, before the multiplication. Giving this a channel value of 0.5 has nearly no change and 0.0 or 1.0 has extreme changes.
    */
  protected var _tweak: Float = TWEAK_RESET

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  /** The maximum number of sprites rendered in one batch so far. * */
  var maxSpritesInBatch: Int = 0

  /** The current number of textures in the LFU cache. Gets reset when calling {@link #begin()} * */
  protected var currentTextureLFUSize: Int = 0

  /** The current number of texture swaps in the LFU cache. Gets reset when calling {@link #begin()} * */
  protected var currentTextureLFUSwaps: Int = 0

  // Constructor body
  val vertexDataType =
    if (Sge().graphics.gl30.isDefined) VertexDataType.VertexBufferObjectWithVAO else VertexDataType.VertexArray

  // The vertex data is extended with one float for the texture index and one float for the tweak.
  mesh = Mesh(
    vertexDataType,
    false,
    size * 4,
    size * 6,
    VertexAttributes(
      VertexAttribute(Usage.Position, 2, DataType.Float, false, ShaderProgram.POSITION_ATTRIBUTE),
      VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, ShaderProgram.COLOR_ATTRIBUTE),
      VertexAttribute(Usage.TextureCoordinates, 2, DataType.Float, false, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
      VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, TWEAK_ATTRIBUTE),
      VertexAttribute(Usage.Generic, 1, DataType.Float, false, TEXTURE_INDEX_ATTRIBUTE)
    )
  )

  _projectionMatrix.setToOrtho2D(0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)

  {
    val len     = size * 6
    val indices = Array.ofDim[Short](len)
    var j: Short = 0
    var i = 0
    while (i < len) {
      indices(i) = j
      indices(i + 1) = (j + 1).toShort
      indices(i + 2) = (j + 2).toShort
      indices(i + 3) = (j + 2).toShort
      indices(i + 4) = (j + 3).toShort
      indices(i + 5) = j
      j = (j + 4).toShort
      i += 6
    }
    mesh.setIndices(indices)
  }

  @publicInBinary override private[sge] def begin(): Unit = {
    if (drawing) throw new IllegalStateException("TextureArrayColorfulBatch.end must be called before begin.")

    renderCalls = 0

    currentTextureLFUSize = 0
    currentTextureLFUSwaps = 0

    Arrays.fill(usedTextures.asInstanceOf[Array[AnyRef]], null) // @nowarn — clearing Nullable array for LFU reset
    Arrays.fill(usedTexturesLFU, 0)

    Sge().graphics.gl.glDepthMask(false)
    customShader.getOrElse(_shader).bind()
    setupMatrices()

    drawing = true
  }

  @publicInBinary override private[sge] def end(): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before end.")

    if (idx > 0) flush()

    drawing = false

    val gl = Sge().graphics.gl
    gl.glDepthMask(true)
    if (blendingEnabled) gl.glDisable(EnableCap.Blend)
  }

  override def color_=(tint: Color): Unit =
    colorPacked = tint.toFloatBits()

  /** Sets the color to the result of {@link ColorTools#oklab(float, float, float, float)} on the same arguments. For the L/A/B parameters, 0.5f is a neutral value (causing no change), while for
    * alpha, 1.0f is a neutral value. For L/A/B, higher values will add to the corresponding channel, while lower values will subtract from it.
    * @see
    *   ColorTools#oklab(float, float, float, float)
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
    colorPacked = ColorTools.oklab(r, g, b, a)

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
    * @param L
    *   like lightness; multiplicative; ranges from 0 (sets lightness to 0) to 1 (doubles lightness)
    * @param A
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green and red are removed) to 1 (green-ness or red-ness is emphasized)
    * @param B
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue and yellow are removed) to 1 (blue-ness or yellow-ness is emphasized)
    * @param contrast
    *   affects how lightness changes; ranges from 0 (low contrast, cloudy look) to 1 (high contrast, sharpened look)
    */
  def setTweak(L: Float, A: Float, B: Float, contrast: Float): Unit =
    _tweak = ColorTools.oklab(L, A, B, contrast)

  /** Sets the tweak using a single packed Oklab float.
    * @see
    *   #setTweak(float, float, float, float)
    * @param tweak
    *   a packed Oklab float, with contrast instead of alpha
    */
  def tweak_=(tweak: Float): Unit =
    _tweak = tweak

  def tweak: Float =
    _tweak

  /** Expects an int color in the format (luma, 8 bits), (chroma A, 8 bits), (chroma B, 8 bits), (alpha, 7 bits), (ignored, 1 bit).
    * @param color
    *   an int color with alpha in the least significant byte and luma in the most significant
    */
  def setIntColor(color: Int): Unit =
    this.colorPacked = NumberUtils.intBitsToFloat(Integer.reverseBytes(color & -2))

  /** Sets the color with the given L/A/B and A parameters from 0 to 255. For the L/A/B parameters, 127 or 128 is a neutral value (causing no change), while for alpha, 255 is a neutral value. For
    * L/A/B, higher values will add to the corresponding channel, while lower values will subtract from it.
    * @see
    *   ColorTools#oklab(float, float, float, float)
    * @param L
    *   like lightness; additive; ranges from 0 (black) to 255 (white)
    * @param A
    *   cool-to-warm, roughly; additive; ranges from 0 (green/cyan/blue) to 255 (orange/red/purple)
    * @param B
    *   artificial-to-natural, very roughly; additive; ranges from 0 (blue/purple) to 255 (green/yellow/orange)
    * @param alpha
    *   opacity, from 0 to 255 (254 is equivalent, since the lowest bit is discarded); multiplicative
    */
  def setIntColor(L: Int, A: Int, B: Int, alpha: Int): Unit =
    colorPacked = NumberUtils.intBitsToFloat(
      (alpha << 24 & 0xfe000000)
        | (B << 16 & 0xff0000) | (A << 8 & 0xff00) | (L & 0xff)
    )

  /** Takes the tweak as an int in the format: L (8 bits), A (8 bits), B (8 bits), contrast (7 bits), (1 ignored bit at the end). An example would be 0x7820206E, which slightly darkens (with L 0x78,
    * slightly below the halfway point of 0x80), significantly reduces colorfulness with A and B multipliers of 0x20 (closer to 0, so most colors will be almost grayscale), and slightly reduces
    * contrast (with contrast and the ignored bit as 0x6E, which is less than the halfway point).
    * @param tweak
    *   the tweak to use as an integer, with L in the most significant bits and contrast in least
    */
  def setIntTweak(tweak: Int): Unit =
    _tweak = NumberUtils.intBitsToFloat(Integer.reverseBytes(tweak & -2))

  /** Sets the multiplicative and contrast parts of the shader's color changes. 127 is a neutral value that should have minimal effect on the image; using {@code (127, 127, 127, 127)} will effectively
    * remove the tweak.
    * @param L
    *   like lightness; multiplicative; ranges from 0 (black) to 255 (white)
    * @param A
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green and red are removed) to 255 (green-ness or red-ness is emphasized)
    * @param B
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue and yellow are removed) to 255 (blue-ness or yellow-ness is emphasized)
    * @param contrast
    *   affects how lightness changes; ranges from 0 (low contrast, cloudy look) to 255 (high contrast, sharpened look)
    */
  def setIntTweak(L: Int, A: Int, B: Int, contrast: Int): Unit =
    _tweak = NumberUtils.intBitsToFloat(
      (contrast << 24 & 0xfe000000)
        | (B << 16 & 0xff0000) | (A << 8 & 0xff00) | (L & 0xff)
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
    * @param addL
    *   like lightness; additive; ranges from 0 (black) to 1 (white)
    * @param addA
    *   cool-to-warm, roughly; additive; ranges from 0 (green/cyan/blue) to 1 (orange/red/purple)
    * @param addB
    *   artificial-to-natural, very roughly; additive; ranges from 0 (blue/purple) to 1 (green/yellow/orange)
    * @param mulAlpha
    *   opacity, from 0 to 1; multiplicative
    * @param mulL
    *   like lightness; multiplicative; ranges from 0 (sets all L to 0) to 1 (doubles the image's L)
    * @param mulA
    *   cool-to-warm, roughly; multiplicative; ranges from 0 (green/cyan/blue) to 1 (orange/red/purple)
    * @param mulB
    *   artificial-to-natural, very roughly; multiplicative; ranges from 0 (blue/purple) to 1 (green/yellow/orange)
    * @param contrast
    *   foggy-to-sharp lightness contrast; affects most other components; ranges from 0 (flat, foggy lightness) to 1 (sharp, crisp lightness)
    */
  def setTweakedColor(
    addL:     Float,
    addA:     Float,
    addB:     Float,
    mulAlpha: Float,
    mulL:     Float,
    mulA:     Float,
    mulB:     Float,
    contrast: Float
  ): Unit = {
    setColor(addL, addA, addB, mulAlpha)
    setTweak(mulL, mulA, mulB, contrast)
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
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(texture)

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

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x1; idx += 1
    vertices(idx) = y1; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x2; idx += 1
    vertices(idx) = y2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x3; idx += 1
    vertices(idx) = y3; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x4; idx += 1
    vertices(idx) = y4; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(texture)

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

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(texture)

    val u   = srcX * invTexWidth
    val v   = (srcY + srcHeight) * invTexHeight
    val u2  = (srcX + srcWidth) * invTexWidth
    val v2  = srcY * invTexHeight
    val fx2 = x + srcWidth
    val fy2 = y + srcHeight

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(texture)

    val fx2 = x + width
    val fy2 = y + height

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    draw(texture, x, y, texture.width.toFloat, texture.height.toFloat)

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(texture)

    val fx2 = x + width
    val fy2 = y + height
    val u   = 0f
    val v   = 1f
    val u2  = 1f
    val v2  = 0f

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  /** This is very different from the other overloads in this class; it assumes the float array it is given is in the format libGDX uses to give to SpriteBatch, that is, in groups of 20 floats per
    * sprite. TextureArrayColorfulBatch uses 28 floats per sprite (7 per vertex), to add tweak per vertex and texture index, so this does some conversion.
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
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    var adjustedCount     = (count / 5) * 7
    val verticesLength    = vertices.length
    val remainingVertices = verticesLength

    flushIfFull()

    // Assigns a texture unit to this texture, flushing if none is available
    val ti = activateTexture(texture)

    var copyCount = scala.math.min(remainingVertices, adjustedCount)
    val tweak     = this._tweak

    // new way, thanks mgsx
    var s  = offset
    var vv = idx
    var i  = 0
    while (i < copyCount) {
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = tweak; vv += 1
      vertices(vv) = ti; vv += 1
      i += 7
    }
    idx += copyCount
    adjustedCount -= copyCount
    var currentOffset = offset
    while (adjustedCount > 0) {
      currentOffset += (copyCount / 7) * 5
      flush()
      copyCount = scala.math.min(verticesLength, adjustedCount)
      // new way, thanks mgsx
      s = currentOffset
      vv = 0
      i = 0
      while (i < copyCount) {
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = tweak; vv += 1
        vertices(vv) = ti; vv += 1
        i += 7
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
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    var adjustedCount     = (count / 6) * 7
    val verticesLength    = vertices.length
    val remainingVertices = verticesLength

    flushIfFull()

    // Assigns a texture unit to this texture, flushing if none is available
    val ti = activateTexture(texture)

    var copyCount = scala.math.min(remainingVertices, adjustedCount)

    var s  = offset
    var vv = idx
    var i  = 0
    while (i < copyCount) {
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = spriteVertices(s); vv += 1; s += 1
      vertices(vv) = ti; vv += 1
      i += 7
    }
    idx += copyCount
    adjustedCount -= copyCount
    var currentOffset = offset
    while (adjustedCount > 0) {
      currentOffset += (copyCount / 7) * 6
      flush()
      copyCount = scala.math.min(verticesLength, adjustedCount)

      s = currentOffset
      vv = 0
      i = 0
      while (i < copyCount) {
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = spriteVertices(s); vv += 1; s += 1
        vertices(vv) = ti; vv += 1
        i += 7
      }
      idx += copyCount
      adjustedCount -= copyCount
    }
  }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    draw(region, x, y, region.regionWidth.toFloat, region.regionHeight.toFloat)

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(region.texture)

    val fx2 = x + width
    val fy2 = y + height
    val u   = region.u
    val v   = region.v2
    val u2  = region.u2
    val v2  = region.v

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = fy2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = fx2; idx += 1
    vertices(idx) = y; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(region.texture)

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

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x1; idx += 1
    vertices(idx) = y1; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x2; idx += 1
    vertices(idx) = y2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x3; idx += 1
    vertices(idx) = y3; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x4; idx += 1
    vertices(idx) = y4; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(region.texture)

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

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x1; idx += 1
    vertices(idx) = y1; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u1; idx += 1
    vertices(idx) = v1; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x2; idx += 1
    vertices(idx) = y2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x3; idx += 1
    vertices(idx) = y3; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u3; idx += 1
    vertices(idx) = v3; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x4; idx += 1
    vertices(idx) = y4; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u4; idx += 1
    vertices(idx) = v4; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!drawing) throw new IllegalStateException("TextureArrayColorfulBatch.begin must be called before draw.")

    val vertices = this.vertices

    flushIfFull()

    val ti = activateTexture(region.texture)

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

    val color = this.colorPacked
    val tweak = this._tweak

    vertices(idx) = x1; idx += 1
    vertices(idx) = y1; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x2; idx += 1
    vertices(idx) = y2; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x3; idx += 1
    vertices(idx) = y3; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v2; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1

    vertices(idx) = x4; idx += 1
    vertices(idx) = y4; idx += 1
    vertices(idx) = color; idx += 1
    vertices(idx) = u2; idx += 1
    vertices(idx) = v; idx += 1
    vertices(idx) = tweak; idx += 1
    vertices(idx) = ti; idx += 1
  }

  /** Flushes if the vertices array cannot hold an additional sprite ((spriteVertexSize + 1) * 4 vertices) anymore. */
  protected def flushIfFull(): Unit =
    // original Sprite attribute size plus two extra floats per sprite vertex
    if (vertices.length - idx < SPRITE_FLOAT_SIZE) {
      flush()
    }

  override def flush(): Unit =
    if (idx != 0) {
      renderCalls += 1
      totalRenderCalls += 1
      val spritesInBatch = idx / SPRITE_FLOAT_SIZE
      if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch
      val count = spritesInBatch * 6

      // Bind the textures
      var i = 0
      while (i < currentTextureLFUSize) {
        usedTextures(i).foreach(_.bind(i))
        i += 1
      }

      // Set TEXTURE0 as active again before drawing.
      Sge().graphics.gl.glActiveTexture(GL20.GL_TEXTURE0)

      val mesh = this.mesh
      mesh.setVertices(vertices, 0, idx)

      val indices = mesh.getIndicesBuffer(true).asInstanceOf[Buffer]
      indices.position(0)
      indices.limit(count)

      if (blendingDisabled) {
        Sge().graphics.gl.glDisable(EnableCap.Blend)
      } else {
        Sge().graphics.gl.glEnable(EnableCap.Blend)
        if (_blendSrcFunc != -1) Sge().graphics.gl.glBlendFuncSeparate(BlendFactor(_blendSrcFunc), BlendFactor(_blendDstFunc), BlendFactor(_blendSrcFuncAlpha), BlendFactor(_blendDstFuncAlpha))
      }

      mesh.render(customShader.getOrElse(_shader), PrimitiveMode.Triangles, 0, count)

      idx = 0
    }

  /** Assigns Texture units and manages the LFU cache.
    *
    * @param texture
    *   The texture that shall be loaded into the cache, if it is not already loaded.
    * @return
    *   The texture slot that has been allocated to the selected texture
    */
  protected def activateTexture(texture: Texture): Float = {
    invTexWidth = 1.0f / texture.width.toFloat
    invTexHeight = 1.0f / texture.height.toFloat

    // This is our identifier for the textures
    val textureHandle = texture.textureObjectHandle

    // First try to see if the texture is already cached
    boundary[Float] {
      var i = 0
      while (i < currentTextureLFUSize) {
        // textureObjectHandle just returns an int,
        // it's fine to call this method instead of caching the value.
        usedTextures(i).foreach { t =>
          if (textureHandle == t.textureObjectHandle) {
            // Increase the access counter.
            usedTexturesLFU(i) += 1
            break(i.toFloat)
          }
        }
        i += 1
      }

      // If a free texture unit is available we just use it
      // If not we have to flush and then throw out the least accessed one.
      if (currentTextureLFUSize < _maxTextureUnits) {
        // Put the texture into the next free slot
        usedTextures(currentTextureLFUSize) = texture
        // Increase the access counter.
        usedTexturesLFU(currentTextureLFUSize) += 1
        val result = currentTextureLFUSize
        currentTextureLFUSize += 1
        break(result.toFloat)
      }

      // We have to flush if there is something in the pipeline already,
      // otherwise the texture index of previously rendered sprites gets invalidated
      if (idx > 0) {
        flush()
      }

      var slot    = 0
      var slotVal = usedTexturesLFU(0)

      var max     = 0
      var average = 0

      // We search for the best candidate for a swap (least accessed) and collect some data
      i = 0
      while (i < _maxTextureUnits) {
        val v = usedTexturesLFU(i)
        max = scala.math.max(v, max)
        average += v
        if (v <= slotVal) {
          slot = i
          slotVal = v
        }
        i += 1
      }

      // The LFU weights will be normalized to the range 0...100
      val normalizeRange = 100

      i = 0
      while (i < _maxTextureUnits) {
        usedTexturesLFU(i) = usedTexturesLFU(i) * normalizeRange / max
        i += 1
      }

      average = (average * normalizeRange) / (max * _maxTextureUnits)

      // Give the new texture a fair (average) chance of staying.
      usedTexturesLFU(slot) = average

      usedTextures(slot) = texture

      // For statistics
      currentTextureLFUSwaps += 1

      slot.toFloat
    }
  }

  /** @return
    *   The number of texture swaps the LFU cache performed since calling {@link #begin()}.
    */
  def textureLFUSwaps: Int = currentTextureLFUSwaps

  /** @return
    *   The current number of textures in the LFU cache. Gets reset when calling {@link #begin()}.
    */
  def textureLFUSize: Int = currentTextureLFUSize

  /** @return
    *   The maximum number of textures that the LFU cache can hold. This limit is imposed by the driver.
    * @see
    *   TextureArrayColorfulBatch#getMaxTextureUnits()
    */
  def textureLFUCapacity: Int = TextureArrayColorfulBatch.getMaxTextureUnits()

  override def disableBlending(): Unit =
    if (blendingDisabled) {
      // already disabled
    } else {
      flush()
      blendingDisabled = true
    }

  override def enableBlending(): Unit =
    if (!blendingDisabled) {
      // already enabled
    } else {
      flush()
      blendingDisabled = false
    }

  override def setBlendFunction(srcFunc: Int, dstFunc: Int): Unit =
    setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)

  override def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit =
    if (_blendSrcFunc != srcFuncColor || _blendDstFunc != dstFuncColor || _blendSrcFuncAlpha != srcFuncAlpha || _blendDstFuncAlpha != dstFuncAlpha) {
      flush()
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
    Sge().graphics.gl20.glUniform1iv(activeShader.fetchUniformLocation("u_textures", true).toInt, _maxTextureUnits, textureUnitIndicesBuffer)
  }

  /** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture coordinates attribute is called "a_texCoord0", the color attribute is called
    * "a_color", texture unit index is called "a_texture_index", this needs to be converted to int with int(...) in the fragment shader. See {@link ShaderProgram#POSITION_ATTRIBUTE},
    * {@link ShaderProgram#COLOR_ATTRIBUTE} and {@link ShaderProgram#TEXCOORD_ATTRIBUTE} which gets "0" appended to indicate the use of the first texture unit. The combined transform and projection
    * matrix is uploaded via a mat4 uniform called "u_projTrans". The texture sampler array is passed via a uniform called "u_textures", see {@link TextureArrayColorfulBatch#createDefaultShader(int)}
    * for reference. <p> Call this method with a null argument to use the default shader. <p> This method will flush the batch before setting the new shader, you can call it in between
    * {@link #begin()} and {@link #end()}.
    *
    * @param shader
    *   the {@link ShaderProgram} or null to use the default shader.
    * @see
    *   #createDefaultShader(int)
    * @see
    *   TextureArrayColorfulBatch#getMaxTextureUnits()
    */
  override def shader_=(shader: Nullable[ShaderProgram]): Unit = {
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

object TextureArrayColorfulBatch {

  /** Size of a ColorfulSprite vertex in floats (position x,y + color + texCoord u,v + tweak + textureIndex). */
  val SPRITE_VERTEX_SIZE: Int = 7

  /** How many floats are used for one "sprite" (4 vertices * SPRITE_VERTEX_SIZE). */
  val SPRITE_FLOAT_SIZE: Int = SPRITE_VERTEX_SIZE * 4

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** The name of the attribute used for the texture index in GLSL shaders. */
  val TEXTURE_INDEX_ATTRIBUTE: String = "a_texture_index"

  /** A constant packed float that can be assigned to this ColorfulBatch's tweak with {@link TextureArrayColorfulBatch#tweak_=(Float)} to make all the tweak adjustments virtually imperceptible. When
    * this is set as the tweak, it won't change the lightness multiplier or lightness contrast, and it won't change either chromatic value multiplier.
    */
  val TWEAK_RESET: Float = ColorTools.oklab(0.5f, 0.5f, 0.5f, 0.5f)

  /** The maximum number of available texture units for the fragment shader. Set internally by {@link #getMaxTextureUnits()}.
    */
  private var _maxTextureUnits: Int = -1

  /** Used to capture the log of any errors during shader compilation. */
  private var shaderErrorLog: Nullable[String] = Nullable.empty

  /** Queries the number of supported textures in a texture array by trying to create the default shader. <br> The first call of this method is very expensive, after that it simply returns a cached
    * value.
    *
    * @return
    *   the number of supported textures in a texture array or zero if this feature is unsupported on this device.
    * @see
    *   #shader_=(ShaderProgram)
    */
  def getMaxTextureUnits()(using Sge): Int = {
    if (_maxTextureUnits == -1) {
      // Query the number of available texture units and decide on a safe number of texture units to use
      val texUnitsQueryBuffer = BufferUtils.newIntBuffer(32)

      Sge().graphics.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, texUnitsQueryBuffer)

      var maxTextureUnitsLocal = texUnitsQueryBuffer.get()

      // Some OpenGL drivers (I'm looking at you, Intel!) do not report the right values,
      // so we take caution and test it first, reducing the number of slots if needed.
      // Will try to find the maximum amount of texture units supported.
      boundary[Unit] {
        while (maxTextureUnitsLocal > 0)
          try {
            val tempProg = createDefaultShader(maxTextureUnitsLocal)
            tempProg.close()
            break(())
          } catch {
            case e: Exception =>
              maxTextureUnitsLocal /= 2
              shaderErrorLog = e.getMessage
          }
      }
      _maxTextureUnits = maxTextureUnitsLocal
    }
    _maxTextureUnits
  }

  /** Returns a new instance of the default shader used by TextureArrayColorfulBatch for GL2 when no shader is specified. This overload always uses {@link #vertexShader} and {@link #fragmentShader} to
    * make its ShaderProgram. This ignores {@link ShaderProgram#prependVertexCode} and {@link ShaderProgram#prependFragmentCode}. Instead, it sets the GLSL version of the shader code automatically to
    * 100 or 150, as appropriate.
    * @see
    *   #getMaxTextureUnits()
    * @param maxTextureUnits
    *   look this up once with {@link #getMaxTextureUnits()} for the current hardware
    * @return
    *   the default ShaderProgram for this Batch
    */
  def createDefaultShader(maxTextureUnits: Int)(using Sge): ShaderProgram =
    createDefaultShader(maxTextureUnits, vertexShader, fragmentShader)

  /** Returns a new instance of the default shader used by TextureArrayColorfulBatch for GL2 when no shader is specified. Does not have any {@code #version} specified in the shader source. This
    * expects an extra attribute (relative to a normal SpriteBatch) that is used for the tweak, and handles its own extra attribute internally for the current texture index. If the fragment shader
    * contains the String <code>@maxTextureUnits@</code>, that will be replaced by the value of the parameter {@code maxTextureUnits}, and this should be done at runtime by this class. This ignores
    * {@link ShaderProgram#prependVertexCode} and {@link ShaderProgram#prependFragmentCode}. Instead, it sets the GLSL version of the shader code automatically to 100 or 150, as appropriate.
    * @see
    *   #getMaxTextureUnits()
    * @param maxTextureUnits
    *   look this up once with {@link #getMaxTextureUnits()} for the current hardware
    * @param vertex
    *   typically {@link #vertexShader}, but can also be {@link #vertexShaderOklabWithRGBATint} or user-defined
    * @param fragment
    *   typically {@link #fragmentShader}, but can also be user-defined
    * @return
    *   the default ShaderProgram for this Batch
    */
  def createDefaultShader(maxTextureUnits: Int, vertex: String, fragment: String)(using Sge): ShaderProgram = {
    val appType         = Sge().application.applicationType
    val prependVertex   = ShaderProgram.prependVertexCode
    val prependFragment = ShaderProgram.prependFragmentCode
    ShaderProgram.prependVertexCode = ""
    ShaderProgram.prependFragmentCode = ""
    val builtFragmentShader =
      if (appType == Application.ApplicationType.Android || appType == Application.ApplicationType.iOS || appType == Application.ApplicationType.WebGL) {
        "#version 100\n" + fragment.replace("@maxTextureUnits@", String.valueOf(maxTextureUnits))
      } else {
        "#version 150\n" + fragment.replace("@maxTextureUnits@", String.valueOf(maxTextureUnits))
      }

    val shader = new ShaderProgram(vertex, builtFragmentShader)
    ShaderProgram.prependVertexCode = prependVertex
    ShaderProgram.prependFragmentCode = prependFragment

    if (!shader.compiled) {
      throw new IllegalArgumentException("Error compiling shader: " + shader.log)
    }

    shader
  }

  /** The default shader's vertex part. <br> This is meant to be used with {@link #fragmentShader}.
    */
  val vertexShader: String =
    "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "attribute float " + TEXTURE_INDEX_ATTRIBUTE + ";\n" +
      "attribute vec4 " + TWEAK_ATTRIBUTE + ";\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_texture_index;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   v_texture_index = " + TEXTURE_INDEX_ATTRIBUTE + ";\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The default shader's fragment part. <br> This must have the String <code>@maxTextureUnits@</code> replaced with the value of {@link #_maxTextureUnits} at runtime. This is done by
    * {@link #createDefaultShader(int, String, String)}, but not if you create a {@link ShaderProgram} yourself. <br> This is meant to be used with {@link #vertexShader} or
    * {@link #vertexShaderOklabWithRGBATint} and passed to {@link TextureArrayColorfulBatch}
    */
  val fragmentShader: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP\n" +
      "#endif\n" +
      "varying LOWP vec4 v_color;\n" +
      "varying LOWP vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      // Added for texture array support
      "varying float v_texture_index;\n" +
      "uniform sampler2D u_textures[@maxTextureUnits@];\n" +
      // End
      "const vec3 forward = vec3(1.0 / 3.0);\n" +
      "float toOklab(float L) {\n" +
      "  return pow(L, 1.5);\n" +
      "}\n" +
      "float fromOklab(float L) {\n" +
      "  return pow(L, 0.666666);\n" +
      "}\n" +
      "void main()\n" +
      "{\n" +
      // Changed for texture array support
      "  vec4 tgt = texture2D(u_textures[int(v_texture_index)], v_texCoords);\n" +
      // End
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.x = toOklab(lab.x);\n" +
      "  // At this point, lab has the value of the RGBA pixel in the texture at v_texCoords, converted to Oklab color space.\n" +
      "  // You can do the same for v_color and even v_tweak if they come in as RGBA values.\n" +
      "  lab.x = (lab.x - 0.5) * 2.0;\n" +
      "  float contrast = exp(v_tweak.w * (-2.0 * 255.0 / 254.0) + 1.0);\n" +
      "  lab.x = pow(abs(lab.x), contrast) * sign(lab.x);\n" +
      "  lab.x = fromOklab(clamp(lab.x * v_tweak.x + v_color.x, 0.0, 1.0));\n" +
      "  lab.yz = clamp((lab.yz * v_tweak.yz + v_color.yz - 0.5) * 2.0, -1.0, 1.0);\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** A special-purpose vertex shader meant for use only here in the Oklab TextureArrayColorfulBatch, this can be used to create a ShaderProgram and passed into the constructor or
    * {@link TextureArrayColorfulBatch#shader_=}. Using this vertex shader lets you specify the batch color (or the tint) as a "normal" RGBA color, while using Oklab for the tweak. The fragment shader
    * doesn't need to be modified here, so you can use {@link #fragmentShader} as normal.
    */
  val vertexShaderOklabWithRGBATint: String =
    "attribute vec4 a_position;\n" +
      "attribute vec4 a_color;\n" +
      "attribute vec2 a_texCoord0;\n" +
      "attribute float " + TEXTURE_INDEX_ATTRIBUTE + ";\n" +
      "attribute vec4 " + TWEAK_ATTRIBUTE + ";\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_texture_index;\n" +
      "\n" +
      "vec3 rgbToLab(vec3 start) {\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *\n" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (start.rgb * start.rgb), vec3(1.0/3.0));\n" +
      "  lab.x = pow(lab.x, 1.5);\n" +
      "  lab.yz = lab.yz * 0.5 + 0.5;\n" +
      "  return lab;\n" +
      "}\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "  v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "  v_color.w = v_color.w * (255.0/254.0);\n" +
      "  v_color.rgb = rgbToLab(v_color.rgb);\n" +
      "  v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "  v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   v_texture_index = " + TEXTURE_INDEX_ATTRIBUTE + ";\n" +
      "  gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}"
}
