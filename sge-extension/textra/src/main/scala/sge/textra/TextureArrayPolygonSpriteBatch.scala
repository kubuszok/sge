/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextureArrayPolygonSpriteBatch.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Gdx.gl -> Sge().graphics.gl; Gdx.gl20 -> Sge().graphics.gl;
 *     Gdx.gl30 -> Sge().graphics.gl30; Gdx.graphics -> Sge().graphics;
 *     GdxRuntimeException -> SgeError; dispose() -> close();
 *     getPackedColor -> packedColor; isBlendingEnabled -> blendingEnabled;
 *     isDrawing -> drawing; getShader -> shader; setShader -> shader_=;
 *     getProjectionMatrix -> projectionMatrix; getTransformMatrix -> transformMatrix;
 *     setProjectionMatrix -> projectionMatrix_=; setTransformMatrix -> transformMatrix_=;
 *     getBlendSrcFunc -> blendSrcFunc; etc.; texture.getWidth -> texture.width.toInt;
 *     texture.getTextureObjectHandle -> texture.textureObjectHandle.toInt;
 *     region.getTexture -> region.texture; region.getU -> region.u;
 *     region.getRegionWidth -> region.regionWidth; etc.
 *   Convention: extends PolygonBatch trait directly (not PolygonSpriteBatch class);
 *     (using Sge) propagation; Nullable for customShader;
 *     typed GL enums: EnableCap, PrimitiveMode, BlendFactor
 *   Idiom: split packages; boundary/break; Nullable
 *
 * Originally from Hyperlap2D's GitHub repo.
 * Originally licensed under Apache 2.0, like TextraTypist and libGDX.
 *
 * @author mzechner (Original SpriteBatch)
 * @author Nathan Sweet (Original SpriteBatch)
 * @author VaTTeRGeR (TextureArray Extension)
 * @author fgnm (PolygonBatch Extension)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1557
 * Covenant-baseline-methods: SPRITE_SIZE,TEXTURE_INDEX_ATTRIBUTE,TextureArrayPolygonSpriteBatch,VERTEX_SIZE,_blendDstFunc,_blendDstFuncAlpha,_blendSrcFunc,_blendSrcFuncAlpha,_color,_drawing,_projectionMatrix,_shader,_transformMatrix,activateTexture,activeShader,blendDstFunc,blendDstFuncAlpha,blendSrcFunc,blendSrcFuncAlpha,blendingDisabled,blendingEnabled,close,color,colorPacked,color_,combinedMatrix,cos,createDefaultShader,currentTextureLFUSize,currentTextureLFUSwaps,customShader,dirtyTextureArray,disableBlending,draw,drawing,enableBlending,ensureMaxTextureUnits,flush,fx,fx2,fy,fy2,getMaxTextureUnits,gl,i,idx,invTexHeight,invTexWidth,localVertices,maxTextureUnits,maxTrianglesInBatch,mesh,n,newShader,offsetIn,ownsShader,p1x,p1y,p2x,p2y,p3x,p3y,p4x,p4y,packedColor,packedColor_,projectionMatrix,projectionMatrix_,regionTriangles,regionTrianglesLength,regionVertices,regionVerticesLength,renderCalls,sX,sY,setBlendFunction,setBlendFunctionSeparate,setColor,setupMatrices,shader,shaderErrorLog,shader_,sin,startVertex,switchTexture,texture,textureCoords,textureHandle,textureIndex,textureLFUCapacity,textureLFUSize,textureLFUSwaps,textureRegion,textureUnitIndicesBuffer,this,totalRenderCalls,transformMatrix,transformMatrix_,triIdx,triangleCount,triangleIndex,triangles,u,u1,u2,u3,usedTextures,usedTexturesLFU,v,v2,vCount,vIn,vertex,vertexDataType,vertexIndex,vertices,verticesCount,vtxIdx,worldOriginX,worldOriginY,x1,x2,x3,x4,y1,y2,y3,y4
 * Covenant-source-reference: com/github/tommyettinger/textra/TextureArrayPolygonSpriteBatch.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import java.nio.IntBuffer
import java.util.Arrays

import sge.graphics.{ BlendFactor, Color, EnableCap, GL20, Mesh, PrimitiveMode, Texture, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g2d.{ PolygonBatch, PolygonRegion, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, MathUtils, Matrix4 }
import sge.utils.{ BufferUtils, Nullable, SgeError }

import scala.annotation.publicInBinary
import scala.language.implicitConversions

/** TextureArrayPolygonSpriteBatch behaves like a SpriteBatch with the polygon drawing features of a PolygonSpriteBatch and optimizations for Batches that switch between Textures frequently. This can
  * be useful when you need either PolygonSpriteBatch methods for drawing PolygonSprites or drawing from multiple Textures. If you use transform matrices, such as from scene2d Groups with transform
  * enabled, you should prefer the subclass {@link TextureArrayCpuPolygonSpriteBatch} instead. <p> If you're using this Batch to draw {@link Font}s with a non-STANDARD {@link Font.DistanceFieldType},
  * you should read the documentation for {@link TextureArrayShaders} and use its {@link TextureArrayShaders#initializeTextureArrayShaders()} method after creating this Batch, but before using any
  * {@link KnownFonts} methods. <p> This is an optimized version of the PolygonSpriteBatch that maintains an LFU texture-cache to combine draw calls with different textures effectively. <p> Use this
  * Batch if you frequently utilize more than a single texture between calling {@link #begin()} and {@link #end()}. An example would be if your Atlas is spread over multiple Textures or if you draw
  * with individual Textures. This extends PolygonBatch, which makes it suitable for Spine animations. Consider using {@link TextureArrayCpuPolygonSpriteBatch} instead, which may perform better in
  * GUIs. <p> Taken from <a href="https://github.com/rednblackgames/hyperlap2d-runtime-libgdx/tree/master/src/main/java/games/rednblack/editor/renderer/utils">Hyperlap2D's GitHub repo</a>. Originally
  * licensed under Apache 2.0, like TextraTypist and libGDX. The tint field, which modified the Batch color, has been removed because it was unused in TextraTypist.
  *
  * @see
  *   Batch
  */
class TextureArrayPolygonSpriteBatch(maxVertices: Int, maxTriangles: Int, defaultShader: Nullable[ShaderProgram])(using Sge) extends PolygonBatch {

  import TextureArrayPolygonSpriteBatch.*

  // 32767 is max vertex index.
  if (maxVertices > 32767)
    throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxVertices)

  TextureArrayPolygonSpriteBatch.ensureMaxTextureUnits()
  if (TextureArrayPolygonSpriteBatch.maxTextureUnits == 0) {
    throw new IllegalStateException("Texture Arrays are not supported on this device:" + TextureArrayPolygonSpriteBatch.shaderErrorLog.getOrElse(""))
  }

  private val usedTextures:    Array[Nullable[Texture]] = Array.fill(TextureArrayPolygonSpriteBatch.maxTextureUnits)(Nullable.empty)
  private val usedTexturesLFU: Array[Int]               = new Array[Int](TextureArrayPolygonSpriteBatch.maxTextureUnits)

  // This contains the numbers 0 ... maxTextureUnits - 1. We send these to the shader as a uniform.
  private val textureUnitIndicesBuffer: IntBuffer = BufferUtils.newIntBuffer(TextureArrayPolygonSpriteBatch.maxTextureUnits)
  for (i <- 0 until TextureArrayPolygonSpriteBatch.maxTextureUnits)
    textureUnitIndicesBuffer.put(i)
  textureUnitIndicesBuffer.flip()

  private val vertexDataType = if (Sge().graphics.gl30.isDefined) {
    Mesh.VertexDataType.VertexBufferObjectWithVAO
  } else {
    Mesh.VertexDataType.VertexBufferObject
  }

  private val mesh = Mesh(
    meshType = vertexDataType,
    isStatic = false,
    maxVertices = maxVertices,
    maxIndices = maxTriangles * 3,
    attributes = VertexAttributes(
      VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
      VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
      VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
      VertexAttribute(Usage.Generic, 1, TEXTURE_INDEX_ATTRIBUTE)
    )
  )

  protected val vertices:      Array[Float] = new Array[Float](maxVertices * VERTEX_SIZE)
  protected val triangles:     Array[Short] = new Array[Short](maxTriangles * 3)
  protected var vertexIndex:   Int          = 0
  protected var triangleIndex: Int          = 0
  protected var invTexWidth:   Float        = 0
  protected var invTexHeight:  Float        = 0
  protected var _drawing:      Boolean      = false

  private val _transformMatrix:  Matrix4 = Matrix4()
  private val _projectionMatrix: Matrix4 = Matrix4()
  private val combinedMatrix:    Matrix4 = Matrix4()

  private var blendingDisabled:   Boolean = false
  private var _blendSrcFunc:      Int     = GL20.GL_SRC_ALPHA
  private var _blendDstFunc:      Int     = GL20.GL_ONE_MINUS_SRC_ALPHA
  private var _blendSrcFuncAlpha: Int     = GL20.GL_SRC_ALPHA
  private var _blendDstFuncAlpha: Int     = GL20.GL_ONE_MINUS_SRC_ALPHA

  private val _shader:      ShaderProgram           = defaultShader.getOrElse(TextureArrayPolygonSpriteBatch.createDefaultShader())
  private var customShader: Nullable[ShaderProgram] = Nullable.empty
  private val ownsShader:   Boolean                 = defaultShader.isEmpty

  protected var colorPacked: Float = Color.WHITE_FLOAT_BITS

  private val _color: Color = Color(1, 1, 1, 1)

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  /** The maximum number of triangles rendered in one batch so far. * */
  var maxTrianglesInBatch: Int = 0

  /** The current number of textures in the LFU cache. Gets reset when calling {@link #begin()} * */
  private var currentTextureLFUSize: Int = 0

  /** The current number of texture swaps in the LFU cache. Gets reset when calling {@link #begin()} * */
  private var currentTextureLFUSwaps: Int = 0

  private var dirtyTextureArray: Boolean = true

  _projectionMatrix.setToOrtho2D(0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)

  /** Constructs a TextureArrayPolygonSpriteBatch with the default shader, 2000 vertices, and 4000 triangles.
    * @see
    *   #TextureArrayPolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this()(using Sge) =
    this(2000, 4000, Nullable.empty)

  /** Constructs a TextureArrayPolygonSpriteBatch with the default shader, size vertices, and size * 2 triangles.
    * @param size
    *   The max number of vertices and number of triangles in a single batch. Max of 32767.
    * @see
    *   #TextureArrayPolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this(size: Int)(using Sge) =
    this(size, size * 2, Nullable.empty)

  /** Constructs a TextureArrayPolygonSpriteBatch with the specified shader, size vertices and size * 2 triangles.
    * @param size
    *   The max number of vertices and number of triangles in a single batch. Max of 32767.
    * @see
    *   #TextureArrayPolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this(size: Int, defaultShader: Nullable[ShaderProgram])(using Sge) =
    this(size, size * 2, defaultShader)

  override def draw(region: PolygonRegion, x: Float, y: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val regionTriangles       = region.triangles
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.vertices
    val regionVerticesLength  = regionVertices.length

    val texture       = region.region.texture
    val localVertices = this.vertices
    if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > localVertices.length
    ) flush()

    val textureIndex = activateTexture(texture)

    var triIdx      = this.triangleIndex
    var vtxIdx      = this.vertexIndex
    val startVertex = vtxIdx / VERTEX_SIZE

    for (i <- 0 until regionTrianglesLength) {
      triangles(triIdx) = (regionTriangles(i) + startVertex).toShort
      triIdx += 1
    }
    this.triangleIndex = triIdx

    val color         = this.colorPacked
    val textureCoords = region.textureCoords

    for (i <- 0 until regionVerticesLength by 2) {
      localVertices(vtxIdx) = regionVertices(i) + x
      localVertices(vtxIdx + 1) = regionVertices(i + 1) + y
      localVertices(vtxIdx + 2) = color
      localVertices(vtxIdx + 3) = textureCoords(i)
      localVertices(vtxIdx + 4) = textureCoords(i + 1)
      localVertices(vtxIdx + 5) = textureIndex
      vtxIdx += VERTEX_SIZE
    }
    this.vertexIndex = vtxIdx
  }

  override def draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val regionTriangles       = region.triangles
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.vertices
    val regionVerticesLength  = regionVertices.length
    val textureRegion         = region.region

    val texture       = textureRegion.texture
    val localVertices = this.vertices
    if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > localVertices.length
    ) flush()

    val textureIndex = activateTexture(texture)

    var triIdx      = this.triangleIndex
    var vtxIdx      = this.vertexIndex
    val startVertex = vtxIdx / VERTEX_SIZE

    for (i <- 0 until regionTriangles.length) {
      triangles(triIdx) = (regionTriangles(i) + startVertex).toShort
      triIdx += 1
    }
    this.triangleIndex = triIdx

    val color         = this.colorPacked
    val textureCoords = region.textureCoords
    val sX            = width / textureRegion.regionWidth
    val sY            = height / textureRegion.regionHeight

    for (i <- 0 until regionVerticesLength by 2) {
      localVertices(vtxIdx) = regionVertices(i) * sX + x
      localVertices(vtxIdx + 1) = regionVertices(i + 1) * sY + y
      localVertices(vtxIdx + 2) = color
      localVertices(vtxIdx + 3) = textureCoords(i)
      localVertices(vtxIdx + 4) = textureCoords(i + 1)
      localVertices(vtxIdx + 5) = textureIndex
      vtxIdx += VERTEX_SIZE
    }
    this.vertexIndex = vtxIdx
  }

  override def draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val regionTriangles       = region.triangles
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.vertices
    val regionVerticesLength  = regionVertices.length
    val textureRegion         = region.region

    val texture       = textureRegion.texture
    val localVertices = this.vertices
    if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > localVertices.length
    ) flush()

    val textureIndex = activateTexture(texture)

    var triIdx      = this.triangleIndex
    var vtxIdx      = this.vertexIndex
    val startVertex = vtxIdx / VERTEX_SIZE

    for (i <- 0 until regionTrianglesLength) {
      triangles(triIdx) = (regionTriangles(i) + startVertex).toShort
      triIdx += 1
    }
    this.triangleIndex = triIdx

    val color         = this.colorPacked
    val textureCoords = region.textureCoords

    val worldOriginX = x + originX
    val worldOriginY = y + originY
    val sX           = width / textureRegion.regionWidth
    val sY           = height / textureRegion.regionHeight
    val cos          = MathUtils.cosDeg(rotation)
    val sin          = MathUtils.sinDeg(rotation)

    var fx: Float = 0
    var fy: Float = 0
    for (i <- 0 until regionVerticesLength by 2) {
      fx = (regionVertices(i) * sX - originX) * scaleX
      fy = (regionVertices(i + 1) * sY - originY) * scaleY
      localVertices(vtxIdx) = cos * fx - sin * fy + worldOriginX
      localVertices(vtxIdx + 1) = sin * fx + cos * fy + worldOriginY
      localVertices(vtxIdx + 2) = color
      localVertices(vtxIdx + 3) = textureCoords(i)
      localVertices(vtxIdx + 4) = textureCoords(i + 1)
      localVertices(vtxIdx + 5) = textureIndex
      vtxIdx += VERTEX_SIZE
    }
    this.vertexIndex = vtxIdx
  }

  override def draw(texture: Texture, polygonVertices: Array[Float], verticesOffset: Int, verticesCount: Int, polygonTriangles: Array[Short], trianglesOffset: Int, trianglesCount: Int): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    // Calculate how many vertices will be put in the batch
    val vCount = (verticesCount / 5) * 6

    if (triangleIndex + trianglesCount > triangles.length || vertexIndex + vCount > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    var triIdx      = this.triangleIndex
    val vtxIdx      = this.vertexIndex
    val startVertex = vtxIdx / VERTEX_SIZE

    for (i <- trianglesOffset until trianglesOffset + trianglesCount) {
      triangles(triIdx) = (polygonTriangles(i) + startVertex).toShort
      triIdx += 1
    }
    this.triangleIndex = triIdx

    var vIn      = vtxIdx
    var offsetIn = verticesOffset
    while (offsetIn < verticesCount + verticesOffset) {
      vertices(vIn) = polygonVertices(offsetIn) // x
      vertices(vIn + 1) = polygonVertices(offsetIn + 1) // y
      vertices(vIn + 2) = polygonVertices(offsetIn + 2) // color
      vertices(vIn + 3) = polygonVertices(offsetIn + 3) // u
      vertices(vIn + 4) = polygonVertices(offsetIn + 4) // v
      vertices(vIn + 5) = textureIndex // texture
      offsetIn += 5
      vIn += VERTEX_SIZE
    }

    this.vertexIndex += vCount
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
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

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

    // construct corner points, start from top left and go counterclockwise
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
    val idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x2
    vertices(idx + 7) = y2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = x3
    vertices(idx + 13) = y3
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = x4
    vertices(idx + 19) = y4
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

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
    val idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x
    vertices(idx + 7) = fy2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = fx2
    vertices(idx + 13) = fy2
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = fx2
    vertices(idx + 19) = y
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

    val u   = srcX * invTexWidth
    val v   = (srcY + srcHeight) * invTexHeight
    val u2  = (srcX + srcWidth) * invTexWidth
    val v2  = srcY * invTexHeight
    val fx2 = x + srcWidth
    val fy2 = y + srcHeight

    val color = this.colorPacked
    val idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x
    vertices(idx + 7) = fy2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = fx2
    vertices(idx + 13) = fy2
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = fx2
    vertices(idx + 19) = y
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

    val fx2 = x + width
    val fy2 = y + height

    val color = this.colorPacked
    val idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x
    vertices(idx + 7) = fy2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = fx2
    vertices(idx + 13) = fy2
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = fx2
    vertices(idx + 19) = y
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    draw(texture, x, y, texture.width.toFloat, texture.height.toFloat)

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

    val fx2 = x + width
    val fy2 = y + height
    val u:  Float = 0
    val v:  Float = 1
    val u2: Float = 1
    val v2: Float = 0

    val color = this.colorPacked
    val idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x
    vertices(idx + 7) = fy2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = fx2
    vertices(idx + 13) = fy2
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = fx2
    vertices(idx + 19) = y
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    // Calculate how many vertices and triangles will be put in the batch
    val triangleCount = (count / 20) * 6
    val verticesCount: Float = (count / 5).toFloat * 6
    if (this.triangleIndex + triangleCount > triangles.length || this.vertexIndex + verticesCount > vertices.length)
      flush()

    val textureIndex = activateTexture(texture)

    val vtxIdx = this.vertexIndex
    var triIdx = this.triangleIndex
    var vertex = (vtxIdx / VERTEX_SIZE).toShort
    val n      = triIdx + triangleCount
    while (triIdx < n) {
      triangles(triIdx) = vertex
      triangles(triIdx + 1) = (vertex + 1).toShort
      triangles(triIdx + 2) = (vertex + 2).toShort
      triangles(triIdx + 3) = (vertex + 2).toShort
      triangles(triIdx + 4) = (vertex + 3).toShort
      triangles(triIdx + 5) = vertex
      triIdx += 6
      vertex = (vertex + 4).toShort
    }
    this.triangleIndex = triIdx

    var vIn      = vtxIdx
    var offsetIn = offset
    while (offsetIn < count + offset) {
      vertices(vIn) = spriteVertices(offsetIn) // x
      vertices(vIn + 1) = spriteVertices(offsetIn + 1) // y
      vertices(vIn + 2) = spriteVertices(offsetIn + 2) // color
      vertices(vIn + 3) = spriteVertices(offsetIn + 3) // u
      vertices(vIn + 4) = spriteVertices(offsetIn + 4) // v
      vertices(vIn + 5) = textureIndex // texture index
      offsetIn += 5
      vIn += VERTEX_SIZE
    }
    this.vertexIndex = (vtxIdx + verticesCount).toInt
  }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    draw(region, x, y, region.regionWidth.toFloat, region.regionHeight.toFloat)

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

    val fx2 = x + width
    val fy2 = y + height
    val u   = region.u
    val v   = region.v2
    val u2  = region.u2
    val v2  = region.v

    val color = this.colorPacked
    val idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x
    vertices(idx + 7) = fy2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = fx2
    vertices(idx + 13) = fy2
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = fx2
    vertices(idx + 19) = y
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

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

    // construct corner points, start from top left and go counterclockwise
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
    val idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x2
    vertices(idx + 7) = y2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = x3
    vertices(idx + 13) = y3
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = x4
    vertices(idx + 19) = y4
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()
    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

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

    // construct corner points, start from top left and go counterclockwise
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

    var u1: Float = 0; var v1: Float = 0; var u2: Float = 0; var v2: Float = 0
    var u3: Float = 0; var v3: Float = 0; var u4: Float = 0; var v4: Float = 0
    if (clockwise) {
      u1 = region.u2
      v1 = region.v2
      u2 = region.u
      v2 = region.v2
      u3 = region.u
      v3 = region.v
      u4 = region.u2
      v4 = region.v
    } else {
      u1 = region.u
      v1 = region.v
      u2 = region.u2
      v2 = region.v
      u3 = region.u2
      v3 = region.v2
      u4 = region.u
      v4 = region.v2
    }

    val color = this.colorPacked
    val idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u1
    vertices(idx + 4) = v1
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x2
    vertices(idx + 7) = y2
    vertices(idx + 8) = color
    vertices(idx + 9) = u2
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = x3
    vertices(idx + 13) = y3
    vertices(idx + 14) = color
    vertices(idx + 15) = u3
    vertices(idx + 16) = v3
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = x4
    vertices(idx + 19) = y4
    vertices(idx + 20) = color
    vertices(idx + 21) = u4
    vertices(idx + 22) = v4
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
      flush()

    val textureIndex = activateTexture(texture)

    val triIdx      = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triIdx) = startVertex.toShort
    triangles(triIdx + 1) = (startVertex + 1).toShort
    triangles(triIdx + 2) = (startVertex + 2).toShort
    triangles(triIdx + 3) = (startVertex + 2).toShort
    triangles(triIdx + 4) = (startVertex + 3).toShort
    triangles(triIdx + 5) = startVertex.toShort
    this.triangleIndex = triIdx + 6

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
    val idx   = vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = x2
    vertices(idx + 7) = y2
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = x3
    vertices(idx + 13) = y3
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = x4
    vertices(idx + 19) = y4
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    vertexIndex = idx + SPRITE_SIZE
  }

  @publicInBinary override private[sge] def begin(): Unit = {
    if (_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.end must be called before begin.")
    renderCalls = 0

    Sge().graphics.gl.glDepthMask(false)
    customShader.getOrElse(_shader).bind()

    setupMatrices()

    _drawing = true
  }

  @publicInBinary override private[sge] def end(): Unit = {
    if (!_drawing) throw new IllegalStateException("TextureArrayPolygonSpriteBatch.begin must be called before end.")
    if (vertexIndex > 0) flush()
    _drawing = false

    val gl = Sge().graphics.gl
    gl.glDepthMask(true)
    if (blendingEnabled) gl.glDisable(EnableCap.Blend)

    currentTextureLFUSize = 0
    currentTextureLFUSwaps = 0

    Arrays.fill(usedTextures.asInstanceOf[Array[AnyRef]], null) // @nowarn -- Java interop: null-fill for Nullable array reset
    Arrays.fill(usedTexturesLFU, 0)
  }

  override def packedColor: Float = colorPacked

  override def packedColor_=(packedColor: Float): Unit = {
    Color.abgr8888ToColor(_color, packedColor)
    colorPacked = packedColor
  }

  override def color: Color = _color

  override def color_=(tint: Color): Unit = {
    _color.set(tint)
    colorPacked = tint.toFloatBits()
  }

  override def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    _color.set(r, g, b, a)
    colorPacked = _color.toFloatBits()
  }

  /** @return The number of texture swaps the LFU cache performed since calling {@link #begin()}. */
  def textureLFUSwaps: Int = currentTextureLFUSwaps

  /** @return The current number of textures in the LFU cache. Gets reset when calling {@link #begin()}. */
  def textureLFUSize: Int = currentTextureLFUSize

  /** @return The maximum number of textures that the LFU cache can hold. This limit is imposed by the driver. */
  def textureLFUCapacity: Int = TextureArrayPolygonSpriteBatch.getMaxTextureUnits()

  override def disableBlending(): Unit = {
    flush()
    blendingDisabled = true
  }

  override def enableBlending(): Unit = {
    flush()
    blendingDisabled = false
  }

  override def setBlendFunction(srcFunc: Int, dstFunc: Int): Unit =
    setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)

  override def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit =
    if (
      _blendSrcFunc == srcFuncColor && _blendDstFunc == dstFuncColor && _blendSrcFuncAlpha == srcFuncAlpha
      && _blendDstFuncAlpha == dstFuncAlpha
    ) {
      // no change
    } else {
      flush()
      _blendSrcFunc = srcFuncColor
      _blendDstFunc = dstFuncColor
      _blendSrcFuncAlpha = srcFuncAlpha
      _blendDstFuncAlpha = dstFuncAlpha
    }

  override def blendSrcFunc: Int = _blendSrcFunc

  override def blendDstFunc: Int = _blendDstFunc

  override def blendSrcFuncAlpha: Int = _blendSrcFuncAlpha

  override def blendDstFuncAlpha: Int = _blendDstFuncAlpha

  override def close(): Unit = {
    mesh.close()
    if (ownsShader) _shader.close()
  }

  override def projectionMatrix: Matrix4 = _projectionMatrix

  override def transformMatrix: Matrix4 = _transformMatrix

  override def projectionMatrix_=(projection: Matrix4): Unit = {
    if (_drawing) flush()
    _projectionMatrix.set(projection)
    if (_drawing) setupMatrices()
  }

  override def transformMatrix_=(transform: Matrix4): Unit = {
    if (_drawing) flush()
    _transformMatrix.set(transform)
    if (_drawing) setupMatrices()
  }

  override def flush(): Unit =
    if (vertexIndex == 0) {
      // nothing to flush
    } else {
      renderCalls += 1
      totalRenderCalls += 1
      val trianglesInBatch = triangleIndex
      if (trianglesInBatch > maxTrianglesInBatch) maxTrianglesInBatch = trianglesInBatch

      if (dirtyTextureArray) {
        // Bind the textures
        for (i <- 0 until currentTextureLFUSize)
          usedTextures(i).foreach(_.bind(i))

        dirtyTextureArray = false
      }

      // Set TEXTURE0 as active again before drawing.
      Sge().graphics.gl.glActiveTexture(GL20.GL_TEXTURE0)

      val mesh = this.mesh
      mesh.setVertices(vertices, 0, vertexIndex)
      mesh.setIndices(triangles, 0, triangleIndex)
      if (blendingDisabled) {
        Sge().graphics.gl.glDisable(EnableCap.Blend)
      } else {
        Sge().graphics.gl.glEnable(EnableCap.Blend)
        if (_blendSrcFunc != -1) Sge().graphics.gl.glBlendFuncSeparate(BlendFactor(_blendSrcFunc), BlendFactor(_blendDstFunc), BlendFactor(_blendSrcFuncAlpha), BlendFactor(_blendDstFuncAlpha))
      }

      mesh.render(customShader.getOrElse(_shader), PrimitiveMode.Triangles, 0, triangleIndex)

      vertexIndex = 0
      triangleIndex = 0
    }

  protected def setupMatrices(): Unit = {
    combinedMatrix.set(_projectionMatrix).mul(_transformMatrix)
    val activeShader = customShader.getOrElse(_shader)
    activeShader.setUniformMatrix("u_projTrans", combinedMatrix)
    Sge().graphics.gl.glUniform1iv(
      activeShader.fetchUniformLocation("u_textures", false).toInt,
      TextureArrayPolygonSpriteBatch.maxTextureUnits,
      textureUnitIndicesBuffer
    )
  }

  /** Assigns Texture units and manages the LFU cache.
    * @param texture
    *   The texture that shall be loaded into the cache, if it is not already loaded.
    * @return
    *   The texture slot that has been allocated to the selected texture
    */
  protected def activateTexture(texture: Texture): Float = {
    invTexWidth = 1.0f / texture.width.toInt
    invTexHeight = 1.0f / texture.height.toInt

    // This is our identifier for the textures
    val textureHandle = texture.textureObjectHandle.toInt

    // First try to see if the texture is already cached
    var i = 0
    while (i < currentTextureLFUSize) {
      // getTextureObjectHandle() just returns an int,
      // it's fine to call this method instead of caching the value.
      if (usedTextures(i).exists(_.textureObjectHandle.toInt == textureHandle)) {
        // Increase the access counter.
        usedTexturesLFU(i) += 1
        return i.toFloat
      }
      i += 1
    }

    // If a free texture unit is available we just use it
    // If not we have to flush and then throw out the least accessed one.
    if (currentTextureLFUSize < TextureArrayPolygonSpriteBatch.maxTextureUnits) {
      // Put the texture into the next free slot
      usedTextures(currentTextureLFUSize) = texture

      dirtyTextureArray = true

      // Increase the access counter.
      usedTexturesLFU(currentTextureLFUSize) += 1
      val slot = currentTextureLFUSize
      currentTextureLFUSize += 1
      slot.toFloat
    } else {
      // We have to flush if there is something in the pipeline already,
      // otherwise the texture index of previously rendered sprites gets invalidated
      if (vertexIndex > 0) {
        flush()
      }

      var slot    = 0
      var slotVal = usedTexturesLFU(0)

      var max     = 0
      var average = 0

      // We search for the best candidate for a swap (least accessed) and collect some data
      for (j <- 0 until TextureArrayPolygonSpriteBatch.maxTextureUnits) {
        val value = usedTexturesLFU(j)
        max = Math.max(value, max)
        average += value
        if (value <= slotVal) {
          slot = j
          slotVal = value
        }
      }

      // The LFU weights will be normalized to the range 0...100
      val normalizeRange = 100
      for (j <- 0 until TextureArrayPolygonSpriteBatch.maxTextureUnits)
        usedTexturesLFU(j) = usedTexturesLFU(j) * normalizeRange / max

      average = (average * normalizeRange) / (max * TextureArrayPolygonSpriteBatch.maxTextureUnits)

      // Give the new texture a fair (average) chance of staying.
      usedTexturesLFU(slot) = average
      usedTextures(slot) = texture

      dirtyTextureArray = true

      // For statistics
      currentTextureLFUSwaps += 1
      slot.toFloat
    }
  }

  override def shader_=(shader: Nullable[ShaderProgram]): Unit = {
    val newShader = shader.getOrElse(_shader)
    if (newShader eq customShader.getOrElse(_shader)) {
      // no change
    } else {
      if (_drawing) {
        flush()
      }
      customShader = shader
      if (_drawing) {
        customShader.getOrElse(_shader).bind()
        setupMatrices()
      }
    }
  }

  override def shader: ShaderProgram =
    customShader.getOrElse(_shader)

  override def blendingEnabled: Boolean = !blendingDisabled

  override def drawing: Boolean = _drawing

  protected def switchTexture(texture: Texture): Unit =
    throw SgeError.GraphicsError("Not implemented. Use TextureArrayPolygonSpriteBatch.activateTexture instead.")
}

object TextureArrayPolygonSpriteBatch {

  val TEXTURE_INDEX_ATTRIBUTE: String = "a_texture_index"

  val VERTEX_SIZE: Int = 2 + 1 + 2 + 1 // Position + Color + Texture Coordinates + Texture Index
  val SPRITE_SIZE: Int = 4 * VERTEX_SIZE // A Sprite has 4 Vertices

  /** The maximum number of available texture units for the fragment shader */
  private var maxTextureUnits: Int = -1

  private var shaderErrorLog: Nullable[String] = Nullable.empty

  /** Queries the number of supported textures in a texture array by trying to create the default shader.<br> The first call of this method is very expensive, after that it simply returns a cached
    * value.
    * @return
    *   the number of supported textures in a texture array or zero if this feature is unsupported on this device.
    */
  def getMaxTextureUnits()(using Sge): Int = {
    ensureMaxTextureUnits()
    maxTextureUnits
  }

  private def ensureMaxTextureUnits()(using Sge): Unit =
    if (maxTextureUnits == -1) {
      // Query the number of available texture units and decide on a safe number of texture units to use
      val texUnitsQueryBuffer = BufferUtils.newIntBuffer(32)
      Sge().graphics.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, texUnitsQueryBuffer)

      var maxTextureUnitsLocal = texUnitsQueryBuffer.get()

      // Some OpenGL drivers (I'm looking at you, Intel!) do not report the right values,
      // so we take caution and test it first, reducing the number of slots if needed.
      // Will try to find the maximum amount of texture units supported.
      var found = false
      while (maxTextureUnitsLocal > 0 && !found) {
        TextureArrayShaderCompiler.MAX_TEXTURE_UNIT = maxTextureUnitsLocal
        try {
          val tempProg = createDefaultShader()
          tempProg.close()
          found = true
        } catch {
          case e: Exception =>
            maxTextureUnitsLocal /= 2
            shaderErrorLog = e.getMessage
        }
      }

      TextureArrayShaderCompiler.MAX_TEXTURE_UNIT = maxTextureUnitsLocal
      maxTextureUnits = maxTextureUnitsLocal
    }

  def createDefaultShader()(using Sge): ShaderProgram = {
    val shader = TextureArrayShaderCompiler.compileShader(TextureArrayShaders.defaultArrayVertexShader(), TextureArrayShaders.defaultArrayFragmentShader())

    if (!shader.compiled) {
      throw new IllegalArgumentException("Error compiling shader: " + shader.log)
    }

    shader
  }
}
