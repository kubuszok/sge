/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonSpriteBatch.java
 * Original authors: mzechner, Stefan Bachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.Mesh.VertexDataType
import sge.graphics.g2d.Sprite.{ SPRITE_SIZE, VERTEX_SIZE }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.glutils.ShaderProgram
import sge.math.Affine2
import sge.math.MathUtils
import sge.math.Matrix4
import sge.graphics.{ Color, GL20, Mesh, Texture, VertexAttribute }

import scala.compiletime.uninitialized

/** A PolygonSpriteBatch is used to draw 2D polygons that reference a texture (region). The class will batch the drawing commands and optimize them for processing by the GPU. <p> To draw something
  * with a PolygonSpriteBatch one has to first call the {@link PolygonSpriteBatch#begin()} method which will setup appropriate render states. When you are done with drawing you have to call
  * {@link PolygonSpriteBatch#end()} which will actually draw the things you specified. <p> All drawing commands of the PolygonSpriteBatch operate in screen coordinates. The screen coordinate system
  * has an x-axis pointing to the right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also provide your own transformation and projection matrices if
  * you so wish. <p> A PolygonSpriteBatch is managed. In case the OpenGL context is lost all OpenGL resources a PolygonSpriteBatch uses internally get invalidated. A context is lost when a user
  * switches to another application or receives an incoming call on Android. A SpritPolygonSpriteBatcheBatch will be automatically reloaded after the OpenGL context is restored. <p> A
  * PolygonSpriteBatch is a pretty heavy object so you should only ever have one in your program. <p> A PolygonSpriteBatch works with OpenGL ES 1.x and 2.0. In the case of a 2.0 context it will use
  * its own custom shader to draw all provided sprites. You can set your own custom shader via {@link #setShader(ShaderProgram)} . <p> A PolygonSpriteBatch has to be disposed if it is no longer used.
  * @author
  *   mzechner
  * @author
  *   Stefan Bachmann
  * @author
  *   Nathan Sweet
  */
class PolygonSpriteBatch(maxVertices: Int, maxTriangles: Int, defaultShader: ShaderProgram)(using sge: Sge) extends PolygonBatch {

  private var mesh: Mesh = uninitialized

  private val vertices:      Array[Float] = Array.ofDim[Float](maxVertices * VERTEX_SIZE)
  private val triangles:     Array[Short] = Array.ofDim[Short](maxTriangles * 3)
  private var vertexIndex:   Int          = 0
  private var triangleIndex: Int          = 0
  private var lastTexture:   Texture      = null
  private var invTexWidth:   Float        = 0
  private var invTexHeight:  Float        = 0
  private var drawing:       Boolean      = false

  private val transformMatrix:  Matrix4 = new Matrix4()
  private val projectionMatrix: Matrix4 = new Matrix4()
  private val combinedMatrix:   Matrix4 = new Matrix4()

  private var blendingDisabled:  Boolean = false
  private var blendSrcFunc:      Int     = GL20.GL_SRC_ALPHA
  private var blendDstFunc:      Int     = GL20.GL_ONE_MINUS_SRC_ALPHA
  private var blendSrcFuncAlpha: Int     = GL20.GL_SRC_ALPHA
  private var blendDstFuncAlpha: Int     = GL20.GL_ONE_MINUS_SRC_ALPHA

  private val shader:       ShaderProgram = if (defaultShader == null) SpriteBatch.createDefaultShader() else defaultShader
  private var customShader: ShaderProgram = null
  private var ownsShader:   Boolean       = defaultShader == null

  private val color:       Color = new Color(1, 1, 1, 1)
  private var colorPacked: Float = Color.WHITE_FLOAT_BITS

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  /** The maximum number of triangles rendered in one batch so far. * */
  var maxTrianglesInBatch: Int = 0

  // Constructor body
  {
    // 32767 is max vertex index.
    if (maxVertices > 32767)
      throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxVertices)

    val vertexDataType: VertexDataType = if (sge.graphics.gl30.isDefined) {
      VertexDataType.VertexBufferObjectWithVAO
    } else {
      VertexDataType.VertexArray
    }

    mesh = Mesh(
      meshType = vertexDataType,
      isStatic = false,
      maxVertices = maxVertices,
      maxIndices = maxTriangles * 3,
      attributes = VertexAttributes(
        VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
        VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
        VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
      )
    )

    projectionMatrix.setToOrtho2D(0, 0, sge.graphics.getWidth().toFloat, sge.graphics.getHeight().toFloat)
  }

  /** Constructs a PolygonSpriteBatch with the default shader, 2000 vertices, and 4000 triangles.
    * @see
    *   #PolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this()(using sge: Sge) = {
    this(2000, 4000, null)
  }

  /** Constructs a PolygonSpriteBatch with the default shader, size vertices, and size * 2 triangles.
    * @param size
    *   The max number of vertices and number of triangles in a single batch. Max of 32767.
    * @see
    *   #PolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this(size: Int)(using sge: Sge) = {
    this(size, size * 2, null)
  }

  /** Constructs a PolygonSpriteBatch with the specified shader, size vertices and size * 2 triangles.
    * @param size
    *   The max number of vertices and number of triangles in a single batch. Max of 32767.
    * @see
    *   #PolygonSpriteBatch(int, int, ShaderProgram)
    */
  def this(size: Int, defaultShader: ShaderProgram)(using sge: Sge) = {
    this(size, size * 2, defaultShader)
  }

  override def begin(): Unit = {
    if (drawing) throw new IllegalStateException("PolygonSpriteBatch.end must be called before begin.")
    renderCalls = 0

    sge.graphics.gl.glDepthMask(false)
    if (customShader != null)
      customShader.bind()
    else
      shader.bind()
    setupMatrices()

    drawing = true
  }

  override def end(): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before end.")
    if (vertexIndex > 0) flush()
    lastTexture = null
    drawing = false

    val gl = sge.graphics.gl
    gl.glDepthMask(true)
    if (isBlendingEnabled()) gl.glDisable(GL20.GL_BLEND)
  }

  override def setColor(tint: Color): Unit = {
    color.set(tint)
    colorPacked = tint.toFloatBits()
  }

  override def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    color.set(r, g, b, a)
    colorPacked = color.toFloatBits()
  }

  override def setPackedColor(packedColor: Float): Unit = {
    Color.abgr8888ToColor(color, packedColor)
    colorPacked = packedColor
  }

  override def getColor(): Color = color

  override def getPackedColor(): Float = colorPacked

  override def draw(region: PolygonRegion, x: Float, y: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val vertices              = this.vertices
    val regionTriangles       = region.getTriangles()
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.getVertices()
    val regionVerticesLength  = regionVertices.length

    val texture = region.getRegion().texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length
    ) flush()

    var triangleIdx = this.triangleIndex
    var vertexIdx   = this.vertexIndex
    val startVertex = vertexIdx / VERTEX_SIZE

    for (i <- 0 until regionTrianglesLength) {
      triangles(triangleIdx) = (regionTriangles(i) + startVertex).toShort
      triangleIdx += 1
    }
    this.triangleIndex = triangleIdx
    val color         = this.colorPacked
    val textureCoords = region.getTextureCoords()

    for (i <- 0 until regionVerticesLength by 2) {
      vertices(vertexIdx) = regionVertices(i) + x
      vertices(vertexIdx + 1) = regionVertices(i + 1) + y
      vertices(vertexIdx + 2) = color
      vertices(vertexIdx + 3) = textureCoords(i)
      vertices(vertexIdx + 4) = textureCoords(i + 1)
      vertexIdx += 5
    }
    this.vertexIndex = vertexIdx
  }

  override def draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val vertices              = this.vertices
    val regionTriangles       = region.getTriangles()
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.getVertices()
    val regionVerticesLength  = regionVertices.length
    val textureRegion         = region.getRegion()

    val texture = textureRegion.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length
    ) flush()

    var triangleIdx = this.triangleIndex
    var vertexIdx   = this.vertexIndex
    val startVertex = vertexIdx / VERTEX_SIZE

    for (i <- 0 until regionTrianglesLength) {
      triangles(triangleIdx) = (regionTriangles(i) + startVertex).toShort
      triangleIdx += 1
    }
    this.triangleIndex = triangleIdx
    val color         = this.colorPacked
    val textureCoords = region.getTextureCoords()
    val sX            = width / textureRegion.regionWidth
    val sY            = height / textureRegion.regionHeight

    for (i <- 0 until regionVerticesLength by 2) {
      vertices(vertexIdx) = regionVertices(i) * sX + x
      vertices(vertexIdx + 1) = regionVertices(i + 1) * sY + y
      vertices(vertexIdx + 2) = color
      vertices(vertexIdx + 3) = textureCoords(i)
      vertices(vertexIdx + 4) = textureCoords(i + 1)
      vertexIdx += 5
    }
    this.vertexIndex = vertexIdx
  }

  override def draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles             = this.triangles
    val vertices              = this.vertices
    val regionTriangles       = region.getTriangles()
    val regionTrianglesLength = regionTriangles.length
    val regionVertices        = region.getVertices()
    val regionVerticesLength  = regionVertices.length
    val textureRegion         = region.getRegion()

    val texture = textureRegion.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (
      triangleIndex + regionTrianglesLength > triangles.length
      || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length
    ) flush()

    var triangleIdx = this.triangleIndex
    var vertexIdx   = this.vertexIndex
    val startVertex = vertexIdx / VERTEX_SIZE

    for (i <- 0 until regionTrianglesLength) {
      triangles(triangleIdx) = (regionTriangles(i) + startVertex).toShort
      triangleIdx += 1
    }
    this.triangleIndex = triangleIdx
    val color         = this.colorPacked
    val textureCoords = region.getTextureCoords()

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
      vertices(vertexIdx) = cos * fx - sin * fy + worldOriginX
      vertices(vertexIdx + 1) = sin * fx + cos * fy + worldOriginY
      vertices(vertexIdx + 2) = color
      vertices(vertexIdx + 3) = textureCoords(i)
      vertices(vertexIdx + 4) = textureCoords(i + 1)
      vertexIdx += 5
    }
    this.vertexIndex = vertexIdx
  }

  override def draw(texture: Texture, polygonVertices: Array[Float], verticesOffset: Int, verticesCount: Int, polygonTriangles: Array[Short], trianglesOffset: Int, trianglesCount: Int): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + trianglesCount > triangles.length || vertexIndex + verticesCount > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val vertexIdx   = this.vertexIndex
    val startVertex = vertexIdx / VERTEX_SIZE

    for (i <- trianglesOffset until trianglesOffset + trianglesCount) {
      triangles(triangleIdx) = (polygonTriangles(i) + startVertex).toShort
      triangleIdx += 1
    }
    this.triangleIndex = triangleIdx

    Array.copy(polygonVertices, verticesOffset, vertices, vertexIdx, verticesCount)
    this.vertexIndex += verticesCount
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
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx:  Float = -originX
    var fy:  Float = -originY
    var fx2: Float = width - originX
    var fy2: Float = height - originY

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
    var idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x2
    vertices(idx + 6) = y2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = x3
    vertices(idx + 11) = y3
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = x4
    vertices(idx + 16) = y4
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

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
    var idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    val u   = srcX * invTexWidth
    val v   = (srcY + srcHeight) * invTexHeight
    val u2  = (srcX + srcWidth) * invTexWidth
    val v2  = srcY * invTexHeight
    val fx2 = x + srcWidth
    val fy2 = y + srcHeight

    val color = this.colorPacked
    var idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    val fx2 = x + width
    val fy2 = y + height

    val color = this.colorPacked
    var idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    draw(texture, x, y, texture.getWidth.toFloat, texture.getHeight.toFloat)

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    val fx2 = x + width
    val fy2 = y + height
    val u   = 0f
    val v   = 1f
    val u2  = 1f
    val v2  = 0f

    val color = this.colorPacked
    var idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    var triangleCount = count / SPRITE_SIZE * 6
    var batch         = 0
    if (texture != lastTexture) {
      switchTexture(texture)
      batch = scala.math.min(scala.math.min(count, vertices.length - (vertices.length % SPRITE_SIZE)), triangles.length / 6 * SPRITE_SIZE)
      triangleCount = batch / SPRITE_SIZE * 6
    } else if (triangleIndex + triangleCount > triangles.length || vertexIndex + count > vertices.length) {
      flush()
      batch = scala.math.min(scala.math.min(count, vertices.length - (vertices.length % SPRITE_SIZE)), triangles.length / 6 * SPRITE_SIZE)
      triangleCount = batch / SPRITE_SIZE * 6
    } else
      batch = count

    var vertexIdx   = this.vertexIndex
    val vertex      = (vertexIdx / VERTEX_SIZE).toShort
    var triangleIdx = this.triangleIndex
    for (n <- triangleIdx until triangleIdx + triangleCount by 6) {
      triangles(triangleIdx) = vertex
      triangles(triangleIdx + 1) = (vertex + 1).toShort
      triangles(triangleIdx + 2) = (vertex + 2).toShort
      triangles(triangleIdx + 3) = (vertex + 2).toShort
      triangles(triangleIdx + 4) = (vertex + 3).toShort
      triangles(triangleIdx + 5) = vertex
    }

    var remainingCount = count
    var currentOffset  = offset
    var continue       = true
    while (continue) {
      Array.copy(spriteVertices, currentOffset, vertices, vertexIdx, batch)
      this.vertexIndex = vertexIdx + batch
      this.triangleIndex = triangleIdx
      remainingCount -= batch
      if (remainingCount == 0) {
        continue = false
      } else {
        currentOffset += batch
        flush()
        vertexIdx = 0
        if (batch > remainingCount) {
          batch = scala.math.min(remainingCount, triangles.length / 6 * SPRITE_SIZE)
          triangleIdx = batch / SPRITE_SIZE * 6
        }
      }
    }
  }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    draw(region, x, y, region.getRegionWidth().toFloat, region.getRegionHeight().toFloat)

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    val fx2 = x + width
    val fy2 = y + height
    val u   = region.u
    val v   = region.v2
    val u2  = region.u2
    val v2  = region.v

    val color = this.colorPacked
    var idx   = this.vertexIndex
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx:  Float = -originX
    var fy:  Float = -originY
    var fx2: Float = width - originX
    var fy2: Float = height - originY

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
    var idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x2
    vertices(idx + 6) = y2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = x3
    vertices(idx + 11) = y3
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = x4
    vertices(idx + 16) = y4
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    this.vertexIndex = idx + 20
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.")

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx:  Float = -originX
    var fy:  Float = -originY
    var fx2: Float = width - originX
    var fy2: Float = height - originY

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

    var u1: Float = 0
    var v1: Float = 0
    var u2: Float = 0
    var v2: Float = 0
    var u3: Float = 0
    var v3: Float = 0
    var u4: Float = 0
    var v4: Float = 0
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
    var idx   = this.vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u1
    vertices(idx + 4) = v1

    vertices(idx + 5) = x2
    vertices(idx + 6) = y2
    vertices(idx + 7) = color
    vertices(idx + 8) = u2
    vertices(idx + 9) = v2

    vertices(idx + 10) = x3
    vertices(idx + 11) = y3
    vertices(idx + 12) = color
    vertices(idx + 13) = u3
    vertices(idx + 14) = v3

    vertices(idx + 15) = x4
    vertices(idx + 16) = y4
    vertices(idx + 17) = color
    vertices(idx + 18) = u4
    vertices(idx + 19) = v4
    this.vertexIndex = idx + 20
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!drawing) throw new IllegalStateException("PolygonSpriteBatch.begin must be called before draw.");

    val triangles = this.triangles
    val vertices  = this.vertices

    val texture = region.texture
    if (texture != lastTexture)
      switchTexture(texture)
    else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
      flush()

    var triangleIdx = this.triangleIndex
    val startVertex = vertexIndex / VERTEX_SIZE
    triangles(triangleIdx) = startVertex.toShort
    triangles(triangleIdx + 1) = (startVertex + 1).toShort
    triangles(triangleIdx + 2) = (startVertex + 2).toShort
    triangles(triangleIdx + 3) = (startVertex + 2).toShort
    triangles(triangleIdx + 4) = (startVertex + 3).toShort
    triangles(triangleIdx + 5) = startVertex.toShort
    this.triangleIndex = triangleIdx + 6

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
    var idx   = vertexIndex
    vertices(idx) = x1
    vertices(idx + 1) = y1
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v

    vertices(idx + 5) = x2
    vertices(idx + 6) = y2
    vertices(idx + 7) = color
    vertices(idx + 8) = u
    vertices(idx + 9) = v2

    vertices(idx + 10) = x3
    vertices(idx + 11) = y3
    vertices(idx + 12) = color
    vertices(idx + 13) = u2
    vertices(idx + 14) = v2

    vertices(idx + 15) = x4
    vertices(idx + 16) = y4
    vertices(idx + 17) = color
    vertices(idx + 18) = u2
    vertices(idx + 19) = v
    vertexIndex = idx + 20
  }

  override def flush(): Unit = {
    if (vertexIndex == 0) return

    renderCalls += 1
    totalRenderCalls += 1
    val trianglesInBatch = triangleIndex
    if (trianglesInBatch > maxTrianglesInBatch) maxTrianglesInBatch = trianglesInBatch

    lastTexture.bind()
    val mesh = this.mesh
    mesh.setVertices(vertices, 0, vertexIndex)
    mesh.setIndices(triangles.slice(0, trianglesInBatch))
    if (blendingDisabled) {
      sge.graphics.gl.glDisable(GL20.GL_BLEND)
    } else {
      sge.graphics.gl.glEnable(GL20.GL_BLEND)
      if (blendSrcFunc != -1) sge.graphics.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
    }

    mesh.render(if (customShader != null) customShader else shader, GL20.GL_TRIANGLES, 0, trianglesInBatch)

    vertexIndex = 0
    triangleIndex = 0
  }

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

  override def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit = {
    if (
      blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
      && blendDstFuncAlpha == dstFuncAlpha
    ) return
    flush()
    blendSrcFunc = srcFuncColor
    blendDstFunc = dstFuncColor
    blendSrcFuncAlpha = srcFuncAlpha
    blendDstFuncAlpha = dstFuncAlpha
  }

  override def getBlendSrcFunc(): Int = blendSrcFunc

  override def getBlendDstFunc(): Int = blendDstFunc

  override def getBlendSrcFuncAlpha(): Int = blendSrcFuncAlpha

  override def getBlendDstFuncAlpha(): Int = blendDstFuncAlpha

  override def close(): Unit = {
    mesh.close()
    if (ownsShader && shader != null) shader.close()
  }

  override def getProjectionMatrix(): Matrix4 = projectionMatrix

  override def getTransformMatrix(): Matrix4 = transformMatrix

  override def setProjectionMatrix(projection: Matrix4): Unit = {
    if (drawing) flush()
    projectionMatrix.set(projection)
    if (drawing) setupMatrices()
  }

  override def setTransformMatrix(transform: Matrix4): Unit = {
    if (drawing) flush()
    transformMatrix.set(transform)
    if (drawing) setupMatrices()
  }

  protected def setupMatrices(): Unit = {
    combinedMatrix.set(projectionMatrix).mul(transformMatrix)
    if (customShader != null) {
      customShader.setUniformMatrix("u_projTrans", combinedMatrix)
      customShader.setUniformi("u_texture", 0)
    } else {
      shader.setUniformMatrix("u_projTrans", combinedMatrix)
      shader.setUniformi("u_texture", 0)
    }
  }

  protected def switchTexture(texture: Texture): Unit = {
    flush()
    lastTexture = texture
    invTexWidth = 1.0f / texture.getWidth
    invTexHeight = 1.0f / texture.getHeight
  }

  override def setShader(shader: ShaderProgram): Unit = {
    if (drawing) {
      flush()
    }
    customShader = shader
    if (drawing) {
      if (customShader != null)
        customShader.bind()
      else
        this.shader.bind()
      setupMatrices()
    }
  }

  override def getShader(): ShaderProgram =
    if (customShader == null) {
      shader
    } else {
      customShader
    }

  override def isBlendingEnabled(): Boolean = !blendingDisabled

  override def isDrawing(): Boolean = drawing
}
