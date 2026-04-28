/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextureArrayCpuPolygonSpriteBatch.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError;
 *     getTransformMatrix -> transformMatrix; setTransformMatrix -> transformMatrix_=;
 *     isDrawing -> drawing; texture.getWidth -> texture.width.toFloat;
 *     texture.getHeight -> texture.height.toFloat;
 *     region.getTexture -> region.texture; region.getU -> region.u;
 *     region.getRegionWidth -> region.regionWidth; region.getRegionHeight -> region.regionHeight;
 *     region.getTriangles -> region.triangles; region.getVertices -> region.vertices;
 *     region.getTextureCoords -> region.textureCoords; region.getRegion -> region.region
 *   Convention: extends TextureArrayPolygonSpriteBatch; (using Sge) propagation; Nullable
 *   Idiom: split packages; boundary/break
 *
 * Originally from Hyperlap2D's GitHub repo.
 * Originally licensed under Apache 2.0, like TextraTypist and libGDX.
 *
 * @author mzechner (Original SpriteBatch)
 * @author Nathan Sweet (Original SpriteBatch)
 * @author Valentin Milea (Transformation Matrix Extension)
 * @author VaTTeRGeR (TextureArray Extension)
 * @author fgnm (PolygonBatch Extension)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1446
 * Covenant-baseline-methods: TextureArrayCpuPolygonSpriteBatch,adjustAffine,adjustNeeded,checkEqual,checkIdt,color,draw,drawRegionRotated,flushAndSyncTransformMatrix,fx,fx2,fy,fy2,haveIdentityRealMatrix,idx,p1x,p1y,p2x,p2y,p3x,p3y,p4x,p4y,realMatrix,setTransformMatrix,startVertex,t,texture,textureIndex,this,tmpAffine,transformMatrix,transformMatrix_,triIdx,triangles,u,u2,v,v2,vertices,virtualMatrix,worldOriginX,worldOriginY,x1,x2,x3,x4,y1,y2,y3,y4
 * Covenant-source-reference: com/github/tommyettinger/textra/TextureArrayCpuPolygonSpriteBatch.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Texture
import sge.graphics.g2d.{ PolygonRegion, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, MathUtils, Matrix4 }
import sge.utils.{ Nullable, SgeError }

/** TextureArrayCpuPolygonSpriteBatch behaves like a SpriteBatch with the polygon drawing features of a PolygonSpriteBatch, the transformation matrix optimizations of a CpuSpriteBatch, and
  * optimizations for Batches that switch between Textures frequently. This can be useful when you need any of: PolygonSpriteBatch methods for drawing PolygonSprites, scene2d Groups with transform
  * enabled, and/or drawing from multiple Textures. <p> If you're using this Batch to draw {@link Font}s with a non-STANDARD {@link Font.DistanceFieldType}, you should read the documentation for
  * {@link TextureArrayShaders} and use its {@link TextureArrayShaders#initializeTextureArrayShaders()} method after creating this Batch, but before using any {@link KnownFonts} methods. <p> This is
  * an optimized version of the PolygonSpriteBatch that maintains an LFU texture-cache to combine draw calls with different textures effectively. It also uses CpuSpriteBatch's optimizations that avoid
  * flushing when the transform matrix changes. <p> Use this Batch if you frequently utilize more than a single texture between calling {@link #begin()} and {@link #end()}. An example would be if your
  * Atlas is spread over multiple Textures or if you draw with individual Textures. This can be a good "default" Batch implementation if you expect to use multiple Textures often, or use scene2d
  * Groups often. In TextraTypist, typically each Font has its own large Texture, and if you use emoji or icons, those typically use a different large Texture. Switching between them has a performance
  * cost, which is essentially eliminated by this Batch. There is more logic in this Batch, and each vertex needs slightly more data, which counterbalances the performance gains from more efficient
  * Texture swaps. If you only use one Texture and don't use Groups where transform is enabled, this Batch is expected to perform somewhat worse than a SpriteBatch. Using many Textures or using Group
  * transformation makes this perform relatively better than SpriteBatch in those cases. This is also a PolygonSpriteBatch, which makes it suitable for Spine animations. <p> Taken from <a
  * href="https://github.com/rednblackgames/hyperlap2d-runtime-libgdx/tree/master/src/main/java/games/rednblack/editor/renderer/utils">Hyperlap2D's GitHub repo</a>. Originally licensed under Apache
  * 2.0, like TextraTypist and libGDX.
  *
  * @see
  *   TextureArrayPolygonSpriteBatch
  */
class TextureArrayCpuPolygonSpriteBatch(size: Int, defaultShader: Nullable[ShaderProgram])(using Sge) extends TextureArrayPolygonSpriteBatch(size, defaultShader) {

  import TextureArrayPolygonSpriteBatch.{ SPRITE_SIZE, VERTEX_SIZE }

  private val virtualMatrix:          Matrix4 = Matrix4()
  private val adjustAffine:           Affine2 = Affine2()
  private var adjustNeeded:           Boolean = false
  private var haveIdentityRealMatrix: Boolean = true

  private val tmpAffine: Affine2 = Affine2()

  /** Constructs a CpuSpriteBatch with a size of 2000 and the default shader.
    *
    * @see
    *   TextureArrayPolygonSpriteBatch
    */
  def this()(using Sge) =
    this(2000, Nullable.empty)

  /** Constructs a CpuSpriteBatch with the default shader.
    *
    * @see
    *   TextureArrayPolygonSpriteBatch
    */
  def this(size: Int)(using Sge) =
    this(size, Nullable.empty)

  /** <p> Flushes the batch and realigns the real matrix on the GPU. Subsequent draws won't need adjustment and will be slightly faster as long as the transform matrix is not
    * {@link #transformMatrix_= changed}. </p> <p> Note: The real transform matrix <em>must</em> be invertible. If a singular matrix is detected, SgeError will be thrown. </p>
    */
  def flushAndSyncTransformMatrix(): Unit = {
    flush()

    if (adjustNeeded) {
      // vertices flushed, safe now to replace matrix
      haveIdentityRealMatrix = checkIdt(virtualMatrix)

      if (!haveIdentityRealMatrix && virtualMatrix.det() == 0)
        throw SgeError.GraphicsError("Transform matrix is singular, can't sync")

      adjustNeeded = false
      super.transformMatrix_=(virtualMatrix)
    }
  }

  override def transformMatrix: Matrix4 =
    if (adjustNeeded) virtualMatrix else super.transformMatrix

  /** Sets the transform matrix to be used by this Batch. Even if this is called inside a {@link #begin()}/{@link #end()} block, the current batch is <em>not</em> flushed to the GPU. Instead, for
    * every subsequent draw() the vertices will be transformed on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by restoring the
    * original matrix, or by calling {@link #flushAndSyncTransformMatrix()}.
    */
  override def transformMatrix_=(transform: Matrix4): Unit = {
    val realMatrix = super.transformMatrix

    if (checkEqual(realMatrix, transform)) {
      adjustNeeded = false
    } else {
      if (drawing) {
        virtualMatrix.setAsAffine(transform)
        adjustNeeded = true

        // adjust = inverse(real) x virtual
        // real x adjust x vertex = virtual x vertex

        if (haveIdentityRealMatrix) {
          adjustAffine.set(transform)
        } else {
          tmpAffine.set(transform)
          adjustAffine.set(realMatrix).inv().mul(tmpAffine)
        }
      } else {
        realMatrix.setAsAffine(transform)
        haveIdentityRealMatrix = checkIdt(realMatrix)
      }
    }
  }

  /** Sets the transform matrix to be used by this Batch. Even if this is called inside a {@link #begin()}/{@link #end()} block, the current batch is <em>not</em> flushed to the GPU. Instead, for
    * every subsequent draw() the vertices will be transformed on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by restoring the
    * original matrix, or by calling {@link #flushAndSyncTransformMatrix()} or {@link #end()}.
    */
  def setTransformMatrix(transform: Affine2): Unit = {
    val realMatrix = super.transformMatrix

    if (checkEqual(realMatrix, transform)) {
      adjustNeeded = false
    } else {
      virtualMatrix.setAsAffine(transform)

      if (drawing) {
        adjustNeeded = true

        // adjust = inverse(real) x virtual
        // real x adjust x vertex = virtual x vertex

        if (haveIdentityRealMatrix) {
          adjustAffine.set(transform)
        } else {
          adjustAffine.set(realMatrix).inv().mul(transform)
        }
      } else {
        realMatrix.setAsAffine(transform)
        haveIdentityRealMatrix = checkIdt(realMatrix)
      }
    }
  }

  override def draw(region: PolygonRegion, x: Float, y: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

      val triangles             = this.triangles
      val regionTriangles       = region.triangles
      val regionTrianglesLength = regionTriangles.length
      val regionVertices        = region.vertices
      val regionVerticesLength  = regionVertices.length

      val texture       = region.region.texture
      val localVertices = this.vertices
      if (triangleIndex + regionTrianglesLength > triangles.length || vertexIndex + regionVerticesLength > localVertices.length)
        flush()

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

      val t = adjustAffine
      for (i <- 0 until regionVerticesLength by 2) {
        val x1 = regionVertices(i) + x
        val y1 = regionVertices(i + 1) + y
        localVertices(vtxIdx) = t.m00 * x1 + t.m01 * y1 + t.m02
        localVertices(vtxIdx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        localVertices(vtxIdx + 2) = color
        localVertices(vtxIdx + 3) = textureCoords(i)
        localVertices(vtxIdx + 4) = textureCoords(i + 1)
        localVertices(vtxIdx + 5) = textureIndex
        vtxIdx += VERTEX_SIZE
      }
      this.vertexIndex = vtxIdx
    }

  override def draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
        || vertexIndex + regionVerticesLength + regionVerticesLength * VERTEX_SIZE / 2 > localVertices.length
      )
        flush()

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
      val t = adjustAffine
      for (i <- 0 until regionVerticesLength by 2) {
        fx = (regionVertices(i) * sX - originX) * scaleX
        fy = (regionVertices(i + 1) * sY - originY) * scaleY
        val x1 = cos * fx - sin * fy + worldOriginX
        val y1 = sin * fx + cos * fy + worldOriginY
        localVertices(vtxIdx) = t.m00 * x1 + t.m01 * y1 + t.m02
        localVertices(vtxIdx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        localVertices(vtxIdx + 2) = color
        localVertices(vtxIdx + 3) = textureCoords(i)
        localVertices(vtxIdx + 4) = textureCoords(i + 1)
        localVertices(vtxIdx + 5) = textureIndex
        vtxIdx += VERTEX_SIZE
      }
      this.vertexIndex = vtxIdx
    }

  override def draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, width, height)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      )
        flush()

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

      val t = adjustAffine
      for (i <- 0 until regionVerticesLength by 2) {
        val x1 = regionVertices(i) * sX + x
        val y1 = regionVertices(i + 1) * sY + y
        localVertices(vtxIdx) = t.m00 * x1 + t.m01 * y1 + t.m02
        localVertices(vtxIdx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
        localVertices(vtxIdx + 2) = color
        localVertices(vtxIdx + 3) = textureCoords(i)
        localVertices(vtxIdx + 4) = textureCoords(i + 1)
        localVertices(vtxIdx + 5) = textureIndex
        vtxIdx += VERTEX_SIZE
      }
      this.vertexIndex = vtxIdx
    }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y)
    } else {
      val width  = texture.width.toFloat
      val height = texture.height.toFloat
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
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
    if (!adjustNeeded) {
      super.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x1 + t.m01 * y1 + t.m02
      vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x2 + t.m01 * y2 + t.m02
      vertices(idx + 7) = t.m10 * x2 + t.m11 * y2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * x3 + t.m01 * y3 + t.m02
      vertices(idx + 13) = t.m10 * x3 + t.m11 * y3 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * x4 + t.m01 * y4 + t.m02
      vertices(idx + 19) = t.m10 * x4 + t.m11 * y4 + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }
  }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height, u, v, u2, v2)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, srcX, srcY, srcWidth, srcHeight)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  /** Draws the polygon using the given vertices and triangles. Each vertex must be made up of 5 elements in this order: x, y, color, u, v.
    */
  override def draw(texture: Texture, polygonVertices: Array[Float], verticesOffset: Int, verticesCount: Int, polygonTriangles: Array[Short], trianglesOffset: Int, trianglesCount: Int): Unit =
    if (!adjustNeeded) {
      super.draw(texture, polygonVertices, verticesOffset, verticesCount, polygonTriangles, trianglesOffset, trianglesCount)
    } else {
      if (!_drawing) throw new IllegalStateException("TextureArrayCpuPolygonSpriteBatch.begin must be called before draw.")

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

      val t        = adjustAffine
      var vIn      = vtxIdx
      var offsetIn = verticesOffset
      while (offsetIn < verticesCount + verticesOffset) {
        val xp = polygonVertices(offsetIn)
        val yp = polygonVertices(offsetIn + 1)

        vertices(vIn) = t.m00 * xp + t.m01 * yp + t.m02 // x
        vertices(vIn + 1) = t.m10 * xp + t.m11 * yp + t.m12 // y
        vertices(vIn + 2) = polygonVertices(offsetIn + 2) // color
        vertices(vIn + 3) = polygonVertices(offsetIn + 3) // u
        vertices(vIn + 4) = polygonVertices(offsetIn + 4) // v
        vertices(vIn + 5) = textureIndex // texture
        offsetIn += 5
        vIn += VERTEX_SIZE
      }
      this.vertexIndex += vCount
    }

  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit =
    if (!adjustNeeded) {
      super.draw(texture, spriteVertices, offset, count)
    } else {
      if (!_drawing) throw new IllegalStateException("TextureArrayCpuPolygonSpriteBatch.begin must be called before draw.")

      val triangles = this.triangles
      val vertices  = this.vertices

      // Calculate how many vertices and triangles will be put in the batch
      val triangleCount = (count / 20) * 6
      val verticesCount = (count / 5) * 6
      if (this.triangleIndex + triangleCount > triangles.length || this.vertexIndex + verticesCount > vertices.length)
        flush()

      val textureIndex = activateTexture(texture)

      var triIdx = this.triangleIndex

      var startVertex = vertexIndex / VERTEX_SIZE
      val n           = triIdx + triangleCount
      while (triIdx < n) {
        triangles(triIdx) = startVertex.toShort
        triangles(triIdx + 1) = (startVertex + 1).toShort
        triangles(triIdx + 2) = (startVertex + 2).toShort
        triangles(triIdx + 3) = (startVertex + 2).toShort
        triangles(triIdx + 4) = (startVertex + 3).toShort
        triangles(triIdx + 5) = startVertex.toShort
        triIdx += 6
        startVertex += 4
      }
      this.triangleIndex = triIdx

      val t        = adjustAffine
      var idx      = vertexIndex
      var offsetIn = offset
      while (offsetIn < count + offset) {
        val xp = spriteVertices(offsetIn)
        val yp = spriteVertices(offsetIn + 1)

        vertices(idx) = t.m00 * xp + t.m01 * yp + t.m02 // x
        vertices(idx + 1) = t.m10 * xp + t.m11 * yp + t.m12 // y
        vertices(idx + 2) = spriteVertices(offsetIn + 2) // color
        vertices(idx + 3) = spriteVertices(offsetIn + 3) // u
        vertices(idx + 4) = spriteVertices(offsetIn + 4) // v
        vertices(idx + 5) = textureIndex // texture index
        idx += VERTEX_SIZE
        offsetIn += 5
      }
      this.vertexIndex = idx
    }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit =
    if (!adjustNeeded) {
      super.draw(region, width, height, transform)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x1 + t.m01 * y1 + t.m02
      vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x2 + t.m01 * y2 + t.m02
      vertices(idx + 7) = t.m10 * x2 + t.m11 * y2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * x3 + t.m01 * y3 + t.m02
      vertices(idx + 13) = t.m10 * x3 + t.m11 * y3 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * x4 + t.m01 * y4 + t.m02
      vertices(idx + 19) = t.m10 * x4 + t.m11 * y4 + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      vertexIndex = idx + SPRITE_SIZE
    }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y)
    } else {
      val width  = region.regionWidth.toFloat
      val height = region.regionHeight.toFloat
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    } else {
      drawRegionRotated(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

  private def drawRegionRotated(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
    val t     = adjustAffine
    vertices(idx) = t.m00 * x1 + t.m01 * y1 + t.m02
    vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
    vertices(idx + 2) = color
    vertices(idx + 3) = u
    vertices(idx + 4) = v
    vertices(idx + 5) = textureIndex

    vertices(idx + 6) = t.m00 * x2 + t.m01 * y2 + t.m02
    vertices(idx + 7) = t.m10 * x2 + t.m11 * y2 + t.m12
    vertices(idx + 8) = color
    vertices(idx + 9) = u
    vertices(idx + 10) = v2
    vertices(idx + 11) = textureIndex

    vertices(idx + 12) = t.m00 * x3 + t.m01 * y3 + t.m02
    vertices(idx + 13) = t.m10 * x3 + t.m11 * y3 + t.m12
    vertices(idx + 14) = color
    vertices(idx + 15) = u2
    vertices(idx + 16) = v2
    vertices(idx + 17) = textureIndex

    vertices(idx + 18) = t.m00 * x4 + t.m01 * y4 + t.m02
    vertices(idx + 19) = t.m10 * x4 + t.m11 * y4 + t.m12
    vertices(idx + 20) = color
    vertices(idx + 21) = u2
    vertices(idx + 22) = v
    vertices(idx + 23) = textureIndex
    this.vertexIndex = idx + SPRITE_SIZE
  }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = {
    if (!adjustNeeded) {
      super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x1 + t.m01 * y1 + t.m02
      vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u1
      vertices(idx + 4) = v1
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x2 + t.m01 * y2 + t.m02
      vertices(idx + 7) = t.m10 * x2 + t.m11 * y2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u2
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * x3 + t.m01 * y3 + t.m02
      vertices(idx + 13) = t.m10 * x3 + t.m11 * y3 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u3
      vertices(idx + 16) = v3
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * x4 + t.m01 * y4 + t.m02
      vertices(idx + 19) = t.m10 * x4 + t.m11 * y4 + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u4
      vertices(idx + 22) = v4
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }
  }

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, width, height)
    } else {
      if (!_drawing) throw new IllegalStateException("CpuPolygonSpriteBatch.begin must be called before draw.")

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
      val t     = adjustAffine
      vertices(idx) = t.m00 * x + t.m01 * y + t.m02
      vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12
      vertices(idx + 2) = color
      vertices(idx + 3) = u
      vertices(idx + 4) = v
      vertices(idx + 5) = textureIndex

      vertices(idx + 6) = t.m00 * x + t.m01 * fy2 + t.m02
      vertices(idx + 7) = t.m10 * x + t.m11 * fy2 + t.m12
      vertices(idx + 8) = color
      vertices(idx + 9) = u
      vertices(idx + 10) = v2
      vertices(idx + 11) = textureIndex

      vertices(idx + 12) = t.m00 * fx2 + t.m01 * fy2 + t.m02
      vertices(idx + 13) = t.m10 * fx2 + t.m11 * fy2 + t.m12
      vertices(idx + 14) = color
      vertices(idx + 15) = u2
      vertices(idx + 16) = v2
      vertices(idx + 17) = textureIndex

      vertices(idx + 18) = t.m00 * fx2 + t.m01 * y + t.m02
      vertices(idx + 19) = t.m10 * fx2 + t.m11 * y + t.m12
      vertices(idx + 20) = color
      vertices(idx + 21) = u2
      vertices(idx + 22) = v
      vertices(idx + 23) = textureIndex
      this.vertexIndex = idx + SPRITE_SIZE
    }

  private def checkEqual(a: Matrix4, b: Matrix4): Boolean =
    if (a eq b) true
    else {
      // matrices are assumed to be 2D transformations
      a.values(Matrix4.M00) == b.values(Matrix4.M00) && a.values(Matrix4.M10) == b.values(Matrix4.M10) &&
      a.values(Matrix4.M01) == b.values(Matrix4.M01) && a.values(Matrix4.M11) == b.values(Matrix4.M11) &&
      a.values(Matrix4.M03) == b.values(Matrix4.M03) && a.values(Matrix4.M13) == b.values(Matrix4.M13)
    }

  private def checkEqual(matrix: Matrix4, affine: Affine2): Boolean = {
    val v = matrix.values

    // matrix is assumed to be 2D transformation
    v(Matrix4.M00) == affine.m00 && v(Matrix4.M10) == affine.m10 && v(Matrix4.M01) == affine.m01 &&
    v(Matrix4.M11) == affine.m11 && v(Matrix4.M03) == affine.m02 && v(Matrix4.M13) == affine.m12
  }

  private def checkIdt(matrix: Matrix4): Boolean = {
    val v = matrix.values

    // matrix is assumed to be 2D transformation
    v(Matrix4.M00) == 1 && v(Matrix4.M10) == 0 && v(Matrix4.M01) == 0 && v(Matrix4.M11) == 1 &&
    v(Matrix4.M03) == 0 && v(Matrix4.M13) == 0
  }
}
