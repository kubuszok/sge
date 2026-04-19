/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/SpriteBatch.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close()
 *   Convention: Nullable for shader/texture; using Sge context parameter; AutoCloseable; createDefaultShader in companion
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters -> Scala property accessors (matching Batch trait)
 *   Improvement: typed GL enums -- BlendFactor, BlendEquation, EnableCap, ClearMask -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1038
 * Covenant-baseline-methods: SpriteBatch,_blendDstFunc,_blendDstFuncAlpha,_blendSrcFunc,_blendSrcFuncAlpha,_color,_projectionMatrix,_shader,_transformMatrix,activeShader,blendDstFunc,blendDstFuncAlpha,blendSrcFunc,blendSrcFuncAlpha,blendingDisabled,blendingEnabled,close,color,colorPacked,color_,combinedMatrix,copyCount,createDefaultShader,currentDataType,currentOffset,customShader,disableBlending,draw,drawing,enableBlending,flush,fragmentShader,fx,fx2,fy,fy2,gl,idx,indices,invTexHeight,invTexWidth,j,lastTexture,len,localIdx,maxSpritesInBatch,mesh,ownsShader,p1x,p1y,p2x,p2y,p3x,p3y,p4x,p4y,packedColor,packedColor_,projectionMatrix,projectionMatrix_,remainingCount,remainingVertices,renderCalls,setBlendFunction,setBlendFunctionSeparate,setColor,setupMatrices,shader,shader_,switchTexture,texture,totalRenderCalls,transformMatrix,transformMatrix_,u,u2,v,v2,vertexDataType,vertexShader,vertices,verticesLength,worldOriginX,worldOriginY,x1,x2,x3,x4,y1,y2,y3,y4
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/SpriteBatch.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g2d

import sge.graphics.Mesh.VertexDataType
import sge.graphics.VertexAttributes.Usage
import sge.graphics.glutils.ShaderProgram
import sge.math.Affine2
import sge.math.MathUtils
import sge.math.Matrix4
import sge.graphics.{ Color, GL20, VertexAttribute }
import sge.utils.Nullable

import scala.annotation.publicInBinary
import scala.compiletime.uninitialized
import scala.language.implicitConversions

import java.nio.Buffer

/** Draws batched quads using indices.
  * @see
  *   Batch
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class SpriteBatch(size: Int = 1000, defaultShader: Nullable[ShaderProgram] = Nullable.empty)(using Sge) extends Batch with AutoCloseable {

  private var currentDataType: VertexDataType = uninitialized

  private var mesh: Mesh = uninitialized

  final val vertices: Array[Float]      = Array.ofDim[Float](size * Sprite.SPRITE_SIZE)
  var idx:            Int               = 0
  var lastTexture:    Nullable[Texture] = Nullable.empty
  var invTexWidth:    Float             = 0
  var invTexHeight:   Float             = 0

  var drawing: Boolean = false

  private val _transformMatrix:  Matrix4 = Matrix4()
  private val _projectionMatrix: Matrix4 = Matrix4()
  private val combinedMatrix:    Matrix4 = Matrix4()

  private var blendingDisabled:   Boolean = false
  private var _blendSrcFunc:      Int     = GL20.GL_SRC_ALPHA
  private var _blendDstFunc:      Int     = GL20.GL_ONE_MINUS_SRC_ALPHA
  private var _blendSrcFuncAlpha: Int     = GL20.GL_SRC_ALPHA
  private var _blendDstFuncAlpha: Int     = GL20.GL_ONE_MINUS_SRC_ALPHA

  private val _shader:      ShaderProgram           = defaultShader.getOrElse(SpriteBatch.createDefaultShader())
  private var customShader: Nullable[ShaderProgram] = Nullable.empty
  private val ownsShader:   Boolean                 = defaultShader.isEmpty

  private val _color: Color = Color(1, 1, 1, 1)
  var colorPacked:    Float = Color.WHITE_FLOAT_BITS

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  /** The maximum number of sprites rendered in one batch so far. * */
  var maxSpritesInBatch: Int = 0

  // Constructor body
  // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
  if (size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size)

  val vertexDataType =
    if (Sge().graphics.gl30.isDefined) VertexDataType.VertexBufferObjectWithVAO else VertexDataType.VertexBufferObject

  currentDataType = vertexDataType

  mesh = Mesh(
    currentDataType,
    false,
    size * 4,
    size * 6,
    VertexAttributes(
      VertexAttribute(Usage.Position, 2, DataType.Float, false, "a_position"),
      VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, "a_color"),
      VertexAttribute(Usage.TextureCoordinates, 2, DataType.Float, false, "a_texCoord0")
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
    if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.")
    renderCalls = 0

    Sge().graphics.gl.glDepthMask(false)
    customShader.getOrElse(_shader).bind()
    setupMatrices()

    drawing = true
  }

  @publicInBinary override private[sge] def end(): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before end.")
    if (idx > 0) flush()
    lastTexture = Nullable.empty
    drawing = false

    val gl = Sge().graphics.gl
    gl.glDepthMask(true)
    if (blendingEnabled) gl.glDisable(EnableCap.Blend)
  }

  override def color_=(tint: Color): Unit = {
    _color.set(tint)
    colorPacked = tint.toFloatBits()
  }

  override def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    _color.set(r, g, b, a)
    colorPacked = _color.toFloatBits()
  }

  override def color: Color =
    _color

  override def packedColor_=(packedColor: Float): Unit = {
    Color.abgr8888ToColor(_color, packedColor)
    this.colorPacked = packedColor
  }

  override def packedColor: Float =
    colorPacked

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
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x2
    vertices(localIdx + 6) = y2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = x3
    vertices(localIdx + 11) = y3
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = x4
    vertices(localIdx + 16) = y4
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x
    vertices(localIdx + 6) = fy2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = fx2
    vertices(localIdx + 11) = fy2
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = fx2
    vertices(localIdx + 16) = y
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x
    vertices(localIdx + 6) = fy2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = fx2
    vertices(localIdx + 11) = fy2
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = fx2
    vertices(localIdx + 16) = y
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

    val vertices = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (idx == vertices.length)
      flush()

    val fx2 = x + width
    val fy2 = y + height

    val color    = this.colorPacked
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x
    vertices(localIdx + 6) = fy2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = fx2
    vertices(localIdx + 11) = fy2
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = fx2
    vertices(localIdx + 16) = y
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    draw(texture, x, y, texture.width.toFloat, texture.height.toFloat)

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x
    vertices(localIdx + 6) = fy2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = fx2
    vertices(localIdx + 11) = fy2
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = fx2
    vertices(localIdx + 16) = y
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    var copyCount = scala.math.min(remainingVertices, count)

    System.arraycopy(spriteVertices, offset, vertices, idx, copyCount)
    idx += copyCount
    var remainingCount = count - copyCount
    var currentOffset  = offset
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
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x
    vertices(localIdx + 1) = y
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x
    vertices(localIdx + 6) = fy2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = fx2
    vertices(localIdx + 11) = fy2
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = fx2
    vertices(localIdx + 16) = y
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x2
    vertices(localIdx + 6) = y2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = x3
    vertices(localIdx + 11) = y3
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = x4
    vertices(localIdx + 16) = y4
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u1
    vertices(localIdx + 4) = v1

    vertices(localIdx + 5) = x2
    vertices(localIdx + 6) = y2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u2
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = x3
    vertices(localIdx + 11) = y3
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u3
    vertices(localIdx + 14) = v3

    vertices(localIdx + 15) = x4
    vertices(localIdx + 16) = y4
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u4
    vertices(localIdx + 19) = v4
    this.idx = localIdx + 20
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.")

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
    val localIdx = this.idx
    vertices(localIdx) = x1
    vertices(localIdx + 1) = y1
    vertices(localIdx + 2) = color
    vertices(localIdx + 3) = u
    vertices(localIdx + 4) = v

    vertices(localIdx + 5) = x2
    vertices(localIdx + 6) = y2
    vertices(localIdx + 7) = color
    vertices(localIdx + 8) = u
    vertices(localIdx + 9) = v2

    vertices(localIdx + 10) = x3
    vertices(localIdx + 11) = y3
    vertices(localIdx + 12) = color
    vertices(localIdx + 13) = u2
    vertices(localIdx + 14) = v2

    vertices(localIdx + 15) = x4
    vertices(localIdx + 16) = y4
    vertices(localIdx + 17) = color
    vertices(localIdx + 18) = u2
    vertices(localIdx + 19) = v
    this.idx = localIdx + 20
  }

  override def flush(): Unit =
    if (idx != 0) {
      renderCalls += 1
      totalRenderCalls += 1
      val spritesInBatch = idx / 20
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
}

object SpriteBatch {

  /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
  def createDefaultShader()(using Sge): ShaderProgram = {
    val vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"
    val fragmentShader = "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying LOWP vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
      "}"

    val shader = ShaderProgram(vertexShader, fragmentShader)
    if (!shader.compiled) throw new IllegalArgumentException("Error compiling shader: " + shader.log)
    shader
  }
}
