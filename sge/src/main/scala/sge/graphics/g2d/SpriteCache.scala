/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/SpriteCache.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close(); getCustomShader -> customShader; get/setProjectionMatrix -> projectionMatrix/projectionMatrix_=
 *   Convention: Nullable throughout; AutoCloseable; using Sge context parameter; createDefaultShader in companion
 *   Idiom: boundary/break, Nullable, split packages, Scala property-style accessors
 *   Fixes: Named context parameter (using sde: Sge) -> anonymous (using Sge) + Sge() accessor.
 *   Fixes: Java-style getters/setters converted to Scala property-style accessors.
 *   Improvement: typed GL enums -- BufferTarget, BufferUsage, PrimitiveMode, EnableCap -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.g2d.Sprite.{ SPRITE_SIZE, VERTEX_SIZE }

import sge.graphics.VertexAttributes.Usage
import sge.graphics.glutils.ShaderProgram
import sge.math.MathUtils
import sge.math.Matrix4
import sge.utils.SgeError
import sge.graphics.{ Color, Mesh, PrimitiveMode, Texture, VertexAttribute }
import sge.graphics.g2d.{ Sprite, TextureRegion }
import sge.utils.DynamicArray
import sge.utils.Nullable

import scala.annotation.publicInBinary
import scala.compiletime.uninitialized
import scala.language.implicitConversions

/** Draws 2D images, optimized for geometry that does not change. Sprites and/or textures are cached and given an ID, which can later be used for drawing. The size, color, and texture region for each
  * cached image cannot be modified. This information is stored in video memory and does not have to be sent to the GPU each time it is drawn.<br> <br> To cache {@link Sprite sprites} or
  * {@link Texture textures} , first call {@link SpriteCache#beginCache()} , then call the appropriate add method to define the images. To complete the cache, call {@link SpriteCache#endCache()} and
  * store the returned cache ID.<br> <br> To draw with SpriteCache, first call {@link #begin()} , then call {@link #draw(int)} with a cache ID. When SpriteCache drawing is complete, call
  * {@link #end()} .<br> <br> By default, SpriteCache draws using screen coordinates and uses an x-axis pointing to the right, an y-axis pointing upwards and the origin is the bottom left corner of
  * the screen. The default transformation and projection matrices can be changed. If the screen is {@link ApplicationListener#resize(int, int) resized} , the SpriteCache's matrices must be updated.
  * For example:<br> <code>cache.projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());</code><br> <br> Note that SpriteCache does not manage blending. You will need
  * to enable blending (<i>Gdx.gl.glEnable(GL10.GL_BLEND);</i>) and set the blend func as needed before or between calls to {@link #draw(int)} .<br> <br> SpriteCache is managed. If the OpenGL context
  * is lost and the restored, all OpenGL resources a SpriteCache uses internally are restored.<br> <br> SpriteCache is a reasonably heavyweight object. Typically only one instance should be used for
  * an entire application.<br> <br> SpriteCache works with OpenGL ES 1.x and 2.0. For 2.0, it uses its own custom shader to draw.<br> <br> SpriteCache must be disposed once it is no longer needed.
  * @author
  *   Nathan Sweet
  */
class SpriteCache(size: Int, shader: ShaderProgram, useIndices: Boolean)(using Sge) extends AutoCloseable {
  import SpriteCache._

  private var mesh:              Mesh                = uninitialized
  private var _drawing:          Boolean             = false
  private val _transformMatrix:  Matrix4             = Matrix4()
  private val _projectionMatrix: Matrix4             = Matrix4()
  private val caches:            DynamicArray[Cache] = DynamicArray[Cache]()

  private val combinedMatrix: Matrix4 = Matrix4()

  private var currentCache: Nullable[Cache]       = Nullable.empty
  private val textures:     DynamicArray[Texture] = DynamicArray[Texture]()
  private val counts:       DynamicArray[Int]     = DynamicArray[Int]()

  // Track indices manually since getNumIndices is not available
  private val maxIndices:  Int          = if (useIndices) size * 6 else 0
  private var vertices:    Array[Float] = uninitialized
  private var vertexIndex: Int          = 0 // Track current position in vertices array

  private val _color:       Color = Color(1, 1, 1, 1)
  private var _colorPacked: Float = Color.WHITE_FLOAT_BITS

  private var _customShader: Nullable[ShaderProgram] = Nullable.empty

  /** Number of render calls since the last {@link #begin()}. * */
  var renderCalls: Int = 0

  /** Number of rendering calls, ever. Will not be reset unless set manually. * */
  var totalRenderCalls: Int = 0

  // Constructor body
  if (useIndices && size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size)

  val maxVertices = size * (if (useIndices) 4 else 6)
  vertices = Array.ofDim[Float](maxVertices * VERTEX_SIZE)

  mesh = Mesh(
    meshType = Mesh.VertexDataType.VertexBufferObject,
    isStatic = true,
    maxVertices = maxVertices,
    maxIndices = maxIndices,
    attributes = VertexAttributes(
      VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
      VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
      VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
    )
  )

  if (useIndices) {
    val length  = size * 6
    val indices = Array.ofDim[Short](length)
    var j: Short = 0
    var i = 0
    while (i < length) {
      indices(i + 0) = j
      indices(i + 1) = (j + 1).toShort
      indices(i + 2) = (j + 2).toShort
      indices(i + 3) = (j + 2).toShort
      indices(i + 4) = (j + 3).toShort
      indices(i + 5) = j
      i += 6
      j = (j + 4).toShort
    }
    mesh.setIndices(indices)
  }

  _projectionMatrix.setToOrtho2D(0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)

  /** Creates a cache that uses indexed geometry and can contain up to 1000 images. */
  def this()(using Sge) =
    this(1000, SpriteCache.createDefaultShader(), false)

  /** Creates a cache with the specified size, using a default shader if OpenGL ES 2.0 is being used.
    * @param size
    *   The maximum number of images this cache can hold. The memory required to hold the images is allocated up front. Max of 8191 if indices are used.
    * @param useIndices
    *   If true, indexed geometry will be used.
    */
  def this(size: Int, useIndices: Boolean)(using Sge) =
    this(size, SpriteCache.createDefaultShader(), useIndices)

  /** Sets the color used to tint images when they are added to the SpriteCache. Default is {@link Color#WHITE}. */
  def color_=(tint: Color): Unit = {
    _color.set(tint)
    _colorPacked = tint.toFloatBits()
  }

  /** @see #color_=(Color) */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    _color.set(r, g, b, a)
    _colorPacked = _color.toFloatBits()
  }

  def color: Color = _color

  /** Sets the color of this sprite cache, expanding the alpha from 0-254 to 0-255.
    * @see
    *   Color#toFloatBits()
    */
  def packedColor_=(packedColor: Float): Unit = {
    Color.abgr8888ToColor(_color, packedColor)
    _colorPacked = packedColor
  }

  def packedColor: Float = _colorPacked

  /** Starts the definition of a new cache, allowing the add and {@link #endCache()} methods to be called. */
  def beginCache(): Unit = {
    if (_drawing) throw new IllegalStateException("end must be called before beginCache")
    if (currentCache.isDefined) throw new IllegalStateException("endCache must be called before begin.")
    val cache = Cache(caches.size, vertexIndex)
    currentCache = cache
    caches.add(cache)
  }

  /** Starts the redefinition of an existing cache, allowing the add and {@link #endCache()} methods to be called. If this is not the last cache created, it cannot have more entries added to it than
    * when it was first created. To do that, use {@link #clear()} and then {@link #begin()} .
    */
  def beginCache(cacheID: Int): Unit = {
    if (_drawing) throw new IllegalStateException("end must be called before beginCache")
    if (currentCache.isDefined) throw new IllegalStateException("endCache must be called before begin.")
    if (cacheID == caches.size - 1) {
      val oldCache = caches.removeIndex(cacheID)
      vertexIndex = oldCache.offset
      beginCache()
    } else {
      val cache = caches(cacheID)
      currentCache = cache
      vertexIndex = cache.offset
    }
  }

  /** Ends the definition of a cache, returning the cache ID to be used with {@link #draw(int)}. */
  def endCache(): Int = {
    if (currentCache.isEmpty) throw new IllegalStateException("beginCache must be called before endCache.")
    val cache      = currentCache.getOrElse(throw new IllegalStateException("beginCache must be called before endCache."))
    val cacheCount = vertexIndex - cache.offset
    if (cache.textures.isEmpty) {
      // New cache.
      cache.maxCount = cacheCount
      cache.textureCount = textures.size
      val newTextures = textures.toArray
      cache.textures = newTextures
      val newCounts = Array.ofDim[Int](cache.textureCount)
      cache.counts = newCounts
      for (i <- 0 until cache.textureCount)
        newCounts(i) = counts(i)

      // Upload vertices to mesh
      mesh.setVertices(vertices, 0, vertexIndex)
    } else {
      // Redefine existing cache.
      if (cacheCount > cache.maxCount) {
        throw SgeError.GraphicsError(
          "If a cache is not the last created, it cannot be redefined with more entries than when it was first created: "
            + cacheCount + " (" + cache.maxCount + " max)"
        )
      }

      cache.textureCount = textures.size

      var tex = cache.textures.getOrElse(Array.empty[Texture])
      if (tex.length < cache.textureCount) {
        tex = Array.ofDim[Texture](cache.textureCount)
        cache.textures = tex
      }
      for (i <- 0 until cache.textureCount)
        tex(i) = textures(i)

      var cnt = cache.counts.getOrElse(Array.empty[Int])
      if (cnt.length < cache.textureCount) {
        cnt = Array.ofDim[Int](cache.textureCount)
        cache.counts = cnt
      }
      for (i <- 0 until cache.textureCount)
        cnt(i) = counts(i)

      // Upload vertices to mesh
      mesh.setVertices(vertices, 0, vertexIndex)
    }

    currentCache = Nullable.empty
    textures.clear()
    counts.clear()

    cache.id
  }

  /** Invalidates all cache IDs and resets the SpriteCache so new caches can be added. */
  def clear(): Unit = {
    caches.clear()
    vertexIndex = 0
    textures.clear()
    counts.clear()
  }

  /** Adds the specified vertices to the cache. Each vertex should have 5 elements, one for each of the attributes: x, y, color, u, and v. If indexed geometry is used, each image should be specified
    * as 4 vertices, otherwise each image should be specified as 6 vertices.
    */
  def add(texture: Texture, vertices: Array[Float], offset: Int, length: Int): Unit = {
    if (currentCache.isEmpty) throw new IllegalStateException("beginCache must be called before add.")

    val verticesPerImage = if (maxIndices > 0) 4 else 6
    val count            = length / (verticesPerImage * VERTEX_SIZE) * 6
    val lastIndex        = textures.size - 1
    if (lastIndex < 0 || textures(lastIndex) != texture) {
      textures.add(texture)
      counts.add(count)
    } else
      counts(lastIndex) += count

    // Copy vertices to our local vertices array
    System.arraycopy(vertices, offset, this.vertices, vertexIndex, length)
    vertexIndex += length
  }

  /** Adds the specified texture to the cache. */
  def add(texture: Texture, x: Float, y: Float): Unit = {
    val fx2 = x + texture.width.toFloat
    val fy2 = y + texture.height.toFloat

    tempVertices(0) = x
    tempVertices(1) = y
    tempVertices(2) = _colorPacked
    tempVertices(3) = 0
    tempVertices(4) = 1

    tempVertices(5) = x
    tempVertices(6) = fy2
    tempVertices(7) = _colorPacked
    tempVertices(8) = 0
    tempVertices(9) = 0

    tempVertices(10) = fx2
    tempVertices(11) = fy2
    tempVertices(12) = _colorPacked
    tempVertices(13) = 1
    tempVertices(14) = 0

    if (maxIndices > 0) {
      tempVertices(15) = fx2
      tempVertices(16) = y
      tempVertices(17) = _colorPacked
      tempVertices(18) = 1
      tempVertices(19) = 1
      add(texture, tempVertices, 0, 20)
    } else {
      tempVertices(15) = fx2
      tempVertices(16) = fy2
      tempVertices(17) = _colorPacked
      tempVertices(18) = 1
      tempVertices(19) = 0

      tempVertices(20) = fx2
      tempVertices(21) = y
      tempVertices(22) = _colorPacked
      tempVertices(23) = 1
      tempVertices(24) = 1

      tempVertices(25) = x
      tempVertices(26) = y
      tempVertices(27) = _colorPacked
      tempVertices(28) = 0
      tempVertices(29) = 1
      add(texture, tempVertices, 0, 30)
    }
  }

  /** Adds the specified texture to the cache. */
  def add(texture: Texture, x: Float, y: Float, srcWidth: Int, srcHeight: Int, u: Float, v: Float, u2: Float, v2: Float, color: Float): Unit = {
    val fx2 = x + srcWidth;
    val fy2 = y + srcHeight;

    tempVertices(0) = x;
    tempVertices(1) = y;
    tempVertices(2) = color;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x;
    tempVertices(6) = fy2;
    tempVertices(7) = color;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = fx2;
    tempVertices(11) = fy2;
    tempVertices(12) = color;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = fx2;
      tempVertices(16) = y;
      tempVertices(17) = color;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = fx2;
      tempVertices(16) = fy2;
      tempVertices(17) = color;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = fx2;
      tempVertices(21) = y;
      tempVertices(22) = color;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x;
      tempVertices(26) = y;
      tempVertices(27) = color;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified texture to the cache. */
  def add(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit = {
    val invTexWidth  = 1.0f / texture.width.toFloat
    val invTexHeight = 1.0f / texture.height.toFloat
    val u            = srcX * invTexWidth;
    val v            = (srcY + srcHeight) * invTexHeight;
    val u2           = (srcX + srcWidth) * invTexWidth;
    val v2           = srcY * invTexHeight;
    val fx2          = x + srcWidth;
    val fy2          = y + srcHeight;

    tempVertices(0) = x;
    tempVertices(1) = y;
    tempVertices(2) = _colorPacked;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x;
    tempVertices(6) = fy2;
    tempVertices(7) = _colorPacked;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = fx2;
    tempVertices(11) = fy2;
    tempVertices(12) = _colorPacked;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = fx2;
      tempVertices(16) = y;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = fx2;
      tempVertices(16) = fy2;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = fx2;
      tempVertices(21) = y;
      tempVertices(22) = _colorPacked;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x;
      tempVertices(26) = y;
      tempVertices(27) = _colorPacked;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified texture to the cache. */
  def add(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = {

    val invTexWidth  = 1.0f / texture.width.toFloat
    val invTexHeight = 1.0f / texture.height.toFloat
    var u            = srcX * invTexWidth
    var v            = (srcY + srcHeight) * invTexHeight
    var u2           = (srcX + srcWidth) * invTexWidth
    var v2           = srcY * invTexHeight
    val fx2          = x + width
    val fy2          = y + height

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

    tempVertices(0) = x;
    tempVertices(1) = y;
    tempVertices(2) = _colorPacked;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x;
    tempVertices(6) = fy2;
    tempVertices(7) = _colorPacked;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = fx2;
    tempVertices(11) = fy2;
    tempVertices(12) = _colorPacked;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = fx2;
      tempVertices(16) = y;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = fx2;
      tempVertices(16) = fy2;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = fx2;
      tempVertices(21) = y;
      tempVertices(22) = _colorPacked;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x;
      tempVertices(26) = y;
      tempVertices(27) = _colorPacked;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified texture to the cache. */
  def add(
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

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx = fx * scaleX
      fy = fy * scaleY
      fx2 = fx2 * scaleX
      fy2 = fy2 * scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx;
    val p1y = fy;
    val p2x = fx;
    val p2y = fy2;
    val p3x = fx2;
    val p3y = fy2;
    val p4x = fx2;
    val p4y = fy;

    var x1: Float = 0;
    var y1: Float = 0;
    var x2: Float = 0;
    var y2: Float = 0;
    var x3: Float = 0;
    var y3: Float = 0;
    var x4: Float = 0;
    var y4: Float = 0;

    // rotate
    if (rotation != 0) {
      val cos = MathUtils.cosDeg(rotation);
      val sin = MathUtils.sinDeg(rotation);

      x1 = cos * p1x - sin * p1y;
      y1 = sin * p1x + cos * p1y;

      x2 = cos * p2x - sin * p2y;
      y2 = sin * p2x + cos * p2y;

      x3 = cos * p3x - sin * p3y;
      y3 = sin * p3x + cos * p3y;

      x4 = x1 + (x3 - x2);
      y4 = y3 - (y2 - y1);
    } else {
      x1 = p1x;
      y1 = p1y;

      x2 = p2x;
      y2 = p2y;

      x3 = p3x;
      y3 = p3y;

      x4 = p4x;
      y4 = p4y;
    }

    x1 += worldOriginX;
    y1 += worldOriginY;
    x2 += worldOriginX;
    y2 += worldOriginY;
    x3 += worldOriginX;
    y3 += worldOriginY;
    x4 += worldOriginX;
    y4 += worldOriginY;

    val invTexWidth  = 1.0f / texture.width.toFloat
    val invTexHeight = 1.0f / texture.height.toFloat
    var u            = srcX * invTexWidth
    var v            = (srcY + srcHeight) * invTexHeight
    var u2           = (srcX + srcWidth) * invTexWidth
    var v2           = srcY * invTexHeight

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

    tempVertices(0) = x1;
    tempVertices(1) = y1;
    tempVertices(2) = _colorPacked;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x2;
    tempVertices(6) = y2;
    tempVertices(7) = _colorPacked;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = x3;
    tempVertices(11) = y3;
    tempVertices(12) = _colorPacked;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = x4;
      tempVertices(16) = y4;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = x3;
      tempVertices(16) = y3;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = x4;
      tempVertices(21) = y4;
      tempVertices(22) = _colorPacked;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x1;
      tempVertices(26) = y1;
      tempVertices(27) = _colorPacked;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified region to the cache. */
  def add(region: TextureRegion, x: Float, y: Float): Unit =
    add(region, x, y, region.regionWidth.toFloat, region.regionHeight.toFloat);

  /** Adds the specified region to the cache. */
  def add(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit = {
    val fx2 = x + width;
    val fy2 = y + height;
    val u   = region.u;
    val v   = region.v2;
    val u2  = region.u2;
    val v2  = region.v;

    tempVertices(0) = x;
    tempVertices(1) = y;
    tempVertices(2) = _colorPacked;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x;
    tempVertices(6) = fy2;
    tempVertices(7) = _colorPacked;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = fx2;
    tempVertices(11) = fy2;
    tempVertices(12) = _colorPacked;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = fx2;
      tempVertices(16) = y;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(region.texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = fx2;
      tempVertices(16) = fy2;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = fx2;
      tempVertices(21) = y;
      tempVertices(22) = _colorPacked;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x;
      tempVertices(26) = y;
      tempVertices(27) = _colorPacked;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(region.texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified region to the cache. */
  def add(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx = fx * scaleX
      fy = fy * scaleY
      fx2 = fx2 * scaleX
      fy2 = fy2 * scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx;
    val p1y = fy;
    val p2x = fx;
    val p2y = fy2;
    val p3x = fx2;
    val p3y = fy2;
    val p4x = fx2;
    val p4y = fy;

    var x1: Float = 0;
    var y1: Float = 0;
    var x2: Float = 0;
    var y2: Float = 0;
    var x3: Float = 0;
    var y3: Float = 0;
    var x4: Float = 0;
    var y4: Float = 0;

    // rotate
    if (rotation != 0) {
      val cos = MathUtils.cosDeg(rotation);
      val sin = MathUtils.sinDeg(rotation);

      x1 = cos * p1x - sin * p1y;
      y1 = sin * p1x + cos * p1y;

      x2 = cos * p2x - sin * p2y;
      y2 = sin * p2x + cos * p2y;

      x3 = cos * p3x - sin * p3y;
      y3 = sin * p3x + cos * p3y;

      x4 = x1 + (x3 - x2);
      y4 = y3 - (y2 - y1);
    } else {
      x1 = p1x;
      y1 = p1y;

      x2 = p2x;
      y2 = p2y;

      x3 = p3x;
      y3 = p3y;

      x4 = p4x;
      y4 = p4y;
    }

    x1 += worldOriginX;
    y1 += worldOriginY;
    x2 += worldOriginX;
    y2 += worldOriginY;
    x3 += worldOriginX;
    y3 += worldOriginY;
    x4 += worldOriginX;
    y4 += worldOriginY;

    val u  = region.u;
    val v  = region.v2;
    val u2 = region.u2;
    val v2 = region.v;

    tempVertices(0) = x1;
    tempVertices(1) = y1;
    tempVertices(2) = _colorPacked;
    tempVertices(3) = u;
    tempVertices(4) = v;

    tempVertices(5) = x2;
    tempVertices(6) = y2;
    tempVertices(7) = _colorPacked;
    tempVertices(8) = u;
    tempVertices(9) = v2;

    tempVertices(10) = x3;
    tempVertices(11) = y3;
    tempVertices(12) = _colorPacked;
    tempVertices(13) = u2;
    tempVertices(14) = v2;

    if (maxIndices > 0) {
      tempVertices(15) = x4;
      tempVertices(16) = y4;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v;
      add(region.texture, tempVertices, 0, 20);
    } else {
      tempVertices(15) = x3;
      tempVertices(16) = y3;
      tempVertices(17) = _colorPacked;
      tempVertices(18) = u2;
      tempVertices(19) = v2;

      tempVertices(20) = x4;
      tempVertices(21) = y4;
      tempVertices(22) = _colorPacked;
      tempVertices(23) = u2;
      tempVertices(24) = v;

      tempVertices(25) = x1;
      tempVertices(26) = y1;
      tempVertices(27) = _colorPacked;
      tempVertices(28) = u;
      tempVertices(29) = v;
      add(region.texture, tempVertices, 0, 30);
    }
  }

  /** Adds the specified sprite to the cache. */
  def add(sprite: Sprite): Unit =
    if (maxIndices > 0) {
      add(sprite.texture, sprite.vertices, 0, SPRITE_SIZE);
    } else {
      val spriteVertices = sprite.vertices;
      System.arraycopy(spriteVertices, 0, tempVertices, 0, 3 * VERTEX_SIZE); // temp0,1,2=sprite0,1,2
      System.arraycopy(spriteVertices, 2 * VERTEX_SIZE, tempVertices, 3 * VERTEX_SIZE, VERTEX_SIZE); // temp3=sprite2
      System.arraycopy(spriteVertices, 3 * VERTEX_SIZE, tempVertices, 4 * VERTEX_SIZE, VERTEX_SIZE); // temp4=sprite3
      System.arraycopy(spriteVertices, 0, tempVertices, 5 * VERTEX_SIZE, VERTEX_SIZE); // temp5=sprite0
      add(sprite.texture, tempVertices, 0, 30);
    }

  /** Prepares the OpenGL state for SpriteCache rendering. */
  @publicInBinary private[sge] def begin(): Unit = {
    if (_drawing) throw new IllegalStateException("end must be called before begin.");
    if (currentCache.isEmpty) throw new IllegalStateException("endCache must be called before begin");
    renderCalls = 0;
    combinedMatrix.set(_projectionMatrix).mul(_transformMatrix);

    Sge().graphics.gl.glDepthMask(false);

    _customShader.fold {
      shader.bind();
      shader.setUniformMatrix("u_projectionViewMatrix", combinedMatrix);
      shader.setUniformi("u_texture", 0);
    } { cs =>
      cs.bind();
      cs.setUniformMatrix("u_proj", _projectionMatrix);
      cs.setUniformMatrix("u_trans", _transformMatrix);
      cs.setUniformMatrix("u_projTrans", combinedMatrix);
      cs.setUniformi("u_texture", 0);
    }
    _drawing = true;
  }

  /** Completes rendering for this SpriteCache. */
  @publicInBinary private[sge] def end(): Unit = {
    if (!_drawing) throw new IllegalStateException("begin must be called before end.");
    _drawing = false;

    Sge().graphics.gl.glDepthMask(true);
  }

  /** Executes `body` between [[begin]] and [[end]], ensuring [[end]] is called even if `body` throws. */
  inline def rendering[A](inline body: => A): A = {
    begin()
    try body
    finally end()
  }

  /** Draws all the images defined for the specified cache ID. */
  def draw(cacheID: Int): Unit = {
    if (!_drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.");

    val cache            = caches(cacheID)
    val verticesPerImage = if (maxIndices > 0) 4 else 6
    var offset           = cache.offset / (verticesPerImage * VERTEX_SIZE) * 6
    val textures         = cache.textures.getOrElse(throw new IllegalStateException("Cache has no textures"))
    val counts           = cache.counts.getOrElse(throw new IllegalStateException("Cache has no counts"))
    val textureCount     = cache.textureCount
    val activeShader     = _customShader.getOrElse(shader)
    for (i <- 0 until textureCount) {
      val count = counts(i)
      textures(i).bind()
      mesh.render(activeShader, PrimitiveMode.Triangles, offset, count)
      offset += count
    }
    renderCalls += textureCount
    totalRenderCalls += textureCount
  }

  /** Draws a subset of images defined for the specified cache ID.
    * @param offset
    *   The first image to render.
    * @param length
    *   The number of images from the first image (inclusive) to render.
    */
  def draw(cacheID: Int, offset: Int, length: Int): Unit = {
    if (!_drawing) throw new IllegalStateException("SpriteCache.begin must be called before draw.")

    val cache            = caches(cacheID)
    val verticesPerImage = if (maxIndices > 0) 4 else 6
    var offsetVar        = cache.offset / (verticesPerImage * VERTEX_SIZE) * 6 + offset * 6
    var lengthVar        = length * 6
    val textures         = cache.textures.getOrElse(throw new IllegalStateException("Cache has no textures"))
    val counts           = cache.counts.getOrElse(throw new IllegalStateException("Cache has no counts"))
    val textureCount     = cache.textureCount
    val activeShader     = _customShader.getOrElse(shader)
    var i                = 0
    while (i < textureCount) {
      textures(i).bind()
      var count = counts(i)
      if (count > lengthVar) {
        i = textureCount
        count = lengthVar
      } else
        lengthVar -= count
      mesh.render(activeShader, PrimitiveMode.Triangles, offsetVar, count)
      offsetVar += count
      i += 1
    }
    renderCalls += cache.textureCount
    totalRenderCalls += textureCount
  }

  /** Releases all resources held by this SpriteCache. */
  def close(): Unit = {
    mesh.close();
    shader.close();
  }

  def projectionMatrix: Matrix4 =
    _projectionMatrix;

  def projectionMatrix_=(projection: Matrix4): Unit = {
    if (_drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
    _projectionMatrix.set(projection);
  }

  def transformMatrix: Matrix4 =
    _transformMatrix;

  def transformMatrix_=(transform: Matrix4): Unit = {
    if (_drawing) throw new IllegalStateException("Can't set the matrix within begin/end.");
    _transformMatrix.set(transform);
  }

  /** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture coordinates attribute is called called "a_texCoords", the color attribute is
    * called "a_color". The projection matrix is uploaded via a mat4 uniform called "u_proj", the transform matrix is uploaded via a uniform called "u_trans", the combined transform and projection
    * matrx is is uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a uniform called "u_texture".
    *
    * Call this method with a null argument to use the default shader.
    *
    * @param shader
    *   the {@link ShaderProgram} or null to use the default shader.
    */
  def customShader_=(shader: Nullable[ShaderProgram]): Unit =
    _customShader = shader

  /** Returns the custom shader, or Nullable.empty if the default shader is being used. */
  def customShader: Nullable[ShaderProgram] =
    _customShader

  def drawing: Boolean =
    _drawing;
}

object SpriteCache {
  private val tempVertices: Array[Float] = Array.ofDim[Float](VERTEX_SIZE * 6)

  class Cache(val id: Int, val offset: Int) {
    var maxCount:     Int                      = 0
    var textureCount: Int                      = 0
    var textures:     Nullable[Array[Texture]] = Nullable.empty
    var counts:       Nullable[Array[Int]]     = Nullable.empty
  }

  def createDefaultShader()(using Sge): ShaderProgram = {
    val vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "uniform mat4 u_projectionViewMatrix;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projectionViewMatrix * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"
    val fragmentShader = "#ifdef GL_ES\n" +
      "precision mediump float;\n" +
      "#endif\n" +
      "varying vec4 v_color;\n" +
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
