/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/CpuSpriteBatch.java
 * Original authors: Valentin Milea
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.Affine2
import sge.math.MathUtils
import sge.math.Matrix4
import sge.utils.Nullable
import sge.utils.SgeError
import sge.Sge
import sge.graphics.glutils.ShaderProgram

/** CpuSpriteBatch behaves like SpriteBatch, except it doesn't flush automatically whenever the transformation matrix changes. Instead, the vertices get adjusted on subsequent draws to match the
  * running batch. This can improve performance through longer batches, for example when drawing Groups with transform enabled.
  *
  * @see
  *   SpriteBatch#renderCalls
  * @see
  *   com.badlogic.gdx.scenes.scene2d.Group#setTransform(boolean) Group.setTransform()
  * @author
  *   Valentin Milea
  */
class CpuSpriteBatch(size: Int, defaultShader: Nullable[ShaderProgram] = Nullable.empty)(using Sge) extends SpriteBatch(size, defaultShader) {

  import CpuSpriteBatch._

  private val virtualMatrix:          Matrix4 = Matrix4()
  private val adjustAffine:           Affine2 = Affine2()
  private val tmpAffine:              Affine2 = Affine2()
  private var adjustNeeded:           Boolean = false
  private var haveIdentityRealMatrix: Boolean = true

  /** Constructs a new CpuSpriteBatch with a size of 1000 and the default shader. */
  def this()(using Sge) =
    this(1000, Nullable.empty)

  /** Constructs a CpuSpriteBatch with the default shader. */
  def this(size: Int)(using Sge) =
    this(size, Nullable.empty)

  /** <p> Flushes the batch and realigns the real matrix on the GPU. Subsequent draws won't need adjustment and will be slightly faster as long as the transform matrix is not
    * {@link #setTransformMatrix(Matrix4) changed} . </p> <p> Note: The real transform matrix <em>must</em> be invertible. If a singular matrix is detected, GdxRuntimeException will be thrown. </p>
    * @see
    *   SpriteBatch#flush()
    */
  def flushAndSyncTransformMatrix(): Unit = {
    flush()

    if (adjustNeeded) {
      // vertices flushed, safe now to replace matrix
      haveIdentityRealMatrix = checkIdt(virtualMatrix)

      if (!haveIdentityRealMatrix && virtualMatrix.det() == 0)
        throw SgeError.GraphicsError("Transform matrix is singular, can't sync")

      adjustNeeded = false;
      super.setTransformMatrix(virtualMatrix)
    }
  }

  override def getTransformMatrix(): Matrix4 =
    if (adjustNeeded) virtualMatrix else super.getTransformMatrix()

  /** Sets the transform matrix to be used by this Batch. Even if this is called inside a {@link #begin()} /{@link #end()} block, the current batch is <em>not</em> flushed to the GPU. Instead, for
    * every subsequent draw() the vertices will be transformed on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by restoring the
    * original matrix, or by calling {@link #flushAndSyncTransformMatrix()} .
    */
  override def setTransformMatrix(transform: Matrix4): Unit = {
    val realMatrix = super.getTransformMatrix()

    if (checkEqual(realMatrix, transform)) {
      adjustNeeded = false;
    } else {
      if (isDrawing()) {
        virtualMatrix.setAsAffine(transform);
        adjustNeeded = true;

        // adjust = inverse(real) x virtual
        // real x adjust x vertex = virtual x vertex

        if (haveIdentityRealMatrix) {
          adjustAffine.set(transform);
        } else {
          tmpAffine.set(transform);
          adjustAffine.set(realMatrix).inv().mul(tmpAffine);
        }
      } else {
        realMatrix.setAsAffine(transform);
        haveIdentityRealMatrix = checkIdt(realMatrix);
      }
    }
  }

  /** Sets the transform matrix to be used by this Batch. Even if this is called inside a {@link #begin()} /{@link #end()} block, the current batch is <em>not</em> flushed to the GPU. Instead, for
    * every subsequent draw() the vertices will be transformed on the CPU to match the original batch matrix. This adjustment must be performed until the matrices are realigned by restoring the
    * original matrix, or by calling {@link #flushAndSyncTransformMatrix()} or {@link #end()} .
    */
  def setTransformMatrix(transform: Affine2): Unit = {
    val realMatrix = super.getTransformMatrix()

    if (checkEqual(realMatrix, transform)) {
      adjustNeeded = false;
    } else {
      virtualMatrix.setAsAffine(transform);

      if (isDrawing()) {
        adjustNeeded = true;

        // adjust = inverse(real) x virtual
        // real x adjust x vertex = virtual x vertex

        if (haveIdentityRealMatrix) {
          adjustAffine.set(transform);
        } else {
          adjustAffine.set(realMatrix).inv().mul(transform);
        }
      } else {
        realMatrix.setAsAffine(transform);
        haveIdentityRealMatrix = checkIdt(realMatrix);
      }
    }
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
  ): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    } else {
      drawAdjusted(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    } else {
      drawAdjusted(texture, x, y, 0, 0, width, height, 1, 1, 0, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    }

  override def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, srcX, srcY, srcWidth, srcHeight);
    } else {
      drawAdjusted(texture, x, y, 0, 0, srcWidth.toFloat, srcHeight.toFloat, 1, 1, 0, srcX, srcY, srcWidth, srcHeight, false, false);
    }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height, u, v, u2, v2);
    } else {
      drawAdjustedUV(texture, x, y, 0, 0, width, height, 1, 1, 0, u, v, u2, v2, false, false);
    }

  override def draw(texture: Texture, x: Float, y: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y);
    } else {
      drawAdjusted(texture, x, y, 0, 0, texture.getWidth.toFloat, texture.getHeight.toFloat, 1, 1, 0, 0, 1, 1, 0, false, false);
    }

  override def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit =
    if (!adjustNeeded) {
      super.draw(texture, x, y, width, height);
    } else {
      drawAdjusted(texture, x, y, 0, 0, width, height, 1, 1, 0, 0, 1, 1, 0, false, false);
    }

  override def draw(region: TextureRegion, x: Float, y: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y);
    } else {
      drawAdjusted(region, x, y, 0, 0, region.getRegionWidth().toFloat, region.getRegionHeight().toFloat, 1, 1, 0);
    }

  override def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, width, height);
    } else {
      drawAdjusted(region, x, y, 0, 0, width, height, 1, 1, 0);
    }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    } else {
      drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

  override def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit =
    if (!adjustNeeded) {
      super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise);
    } else {
      drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise);
    }

  override def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (count % Sprite.SPRITE_SIZE != 0) throw SgeError.GraphicsError("invalid vertex count");

    if (!adjustNeeded) {
      super.draw(texture, spriteVertices, offset, count);
    } else {
      drawAdjusted(texture, spriteVertices, offset, count);
    }
  }

  override def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit =
    if (!adjustNeeded) {
      super.draw(region, width, height, transform);
    } else {
      drawAdjusted(region, width, height, transform);
    }

  private def drawAdjusted(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    // v must be flipped
    drawAdjustedUV(
      region.getTexture(),
      x,
      y,
      originX,
      originY,
      width,
      height,
      scaleX,
      scaleY,
      rotation,
      region.getU(),
      region.getV2(),
      region.getU2(),
      region.getV(),
      false,
      false
    );

  private def drawAdjusted(
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
    val invTexWidth  = 1.0f / texture.getWidth
    val invTexHeight = 1.0f / texture.getHeight

    val u  = srcX * invTexWidth;
    val v  = (srcY + srcHeight) * invTexHeight;
    val u2 = (srcX + srcWidth) * invTexWidth;
    val v2 = srcY * invTexHeight;

    drawAdjustedUV(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, u, v, u2, v2, flipX, flipY);
  }

  private def drawAdjustedUV(
    texture:  Texture,
    x:        Float,
    y:        Float,
    originX:  Float,
    originY:  Float,
    width:    Float,
    height:   Float,
    scaleX:   Float,
    scaleY:   Float,
    rotation: Float,
    u:        Float,
    v:        Float,
    u2:       Float,
    v2:       Float,
    flipX:    Boolean,
    flipY:    Boolean
  ): Unit = {
    if (!drawing) throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");

    if (texture != lastTexture)
      switchTexture(texture);
    else if (idx == vertices.length) super.flush();

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    var fx           = -originX
    var fy           = -originY
    var fx2          = width - originX
    var fy2          = height - originY

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX;
      fy *= scaleY
      fx2 *= scaleX
      fy2 *= scaleY
    }

    // construct corner points, start from top left and go counter clockwise
    val p1x = fx;
    val p1y = fy
    val p2x = fx
    val p2y = fy2
    val p3x = fx2
    val p3y = fy2
    val p4x = fx2
    val p4y = fy

    var x1: Float = 0f
    var y1: Float = 0f
    var x2: Float = 0f
    var y2: Float = 0f
    var x3: Float = 0f
    var y3: Float = 0f
    var x4: Float = 0f
    var y4: Float = 0f

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

    var uAdjusted  = u;
    var vAdjusted  = v;
    var u2Adjusted = u2;
    var v2Adjusted = v2;

    if (flipX) {
      val tmp = uAdjusted;
      uAdjusted = u2Adjusted;
      u2Adjusted = tmp;
    }
    if (flipY) {
      val tmp = vAdjusted;
      vAdjusted = v2Adjusted;
      v2Adjusted = tmp;
    }

    val t = adjustAffine;

    vertices(idx + 0) = t.m00 * x1 + t.m01 * y1 + t.m02;
    vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12;
    vertices(idx + 2) = colorPacked;
    vertices(idx + 3) = uAdjusted;
    vertices(idx + 4) = vAdjusted;

    vertices(idx + 5) = t.m00 * x2 + t.m01 * y2 + t.m02;
    vertices(idx + 6) = t.m10 * x2 + t.m11 * y2 + t.m12;
    vertices(idx + 7) = colorPacked;
    vertices(idx + 8) = uAdjusted;
    vertices(idx + 9) = v2Adjusted;

    vertices(idx + 10) = t.m00 * x3 + t.m01 * y3 + t.m02;
    vertices(idx + 11) = t.m10 * x3 + t.m11 * y3 + t.m12;
    vertices(idx + 12) = colorPacked;
    vertices(idx + 13) = u2Adjusted;
    vertices(idx + 14) = v2Adjusted;

    vertices(idx + 15) = t.m00 * x4 + t.m01 * y4 + t.m02;
    vertices(idx + 16) = t.m10 * x4 + t.m11 * y4 + t.m12;
    vertices(idx + 17) = colorPacked;
    vertices(idx + 18) = u2Adjusted;
    vertices(idx + 19) = vAdjusted;

    idx += Sprite.SPRITE_SIZE;
  }

  private def drawAdjusted(
    region:    TextureRegion,
    x:         Float,
    y:         Float,
    originX:   Float,
    originY:   Float,
    width:     Float,
    height:    Float,
    scaleX:    Float,
    scaleY:    Float,
    rotation:  Float,
    clockwise: Boolean
  ): Unit = {
    if (!drawing) throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");

    if (region.getTexture() != lastTexture)
      switchTexture(region.getTexture());
    else if (idx == vertices.length) super.flush();

    // bottom left and top right corner points relative to origin
    val worldOriginX = x + originX;
    val worldOriginY = y + originY;
    var fx           = -originX;
    var fy           = -originY;
    var fx2          = width - originX;
    var fy2          = height - originY;

    // scale
    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX;
      fy *= scaleY;
      fx2 *= scaleX;
      fy2 *= scaleY;
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

    var x1: Float = 0f
    var y1: Float = 0f
    var x2: Float = 0f
    var y2: Float = 0f
    var x3: Float = 0f
    var y3: Float = 0f
    var x4: Float = 0f
    var y4: Float = 0f

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

    val (u1, v1, u2, v2, u3, v3, u4, v4) = if (clockwise) {
      (region.getU2(), region.getV2(), region.getU(), region.getV2(), region.getU(), region.getV(), region.getU2(), region.getV())
    } else {
      (region.getU(), region.getV(), region.getU2(), region.getV(), region.getU2(), region.getV2(), region.getU(), region.getV2())
    };

    val t = adjustAffine;

    vertices(idx + 0) = t.m00 * x1 + t.m01 * y1 + t.m02;
    vertices(idx + 1) = t.m10 * x1 + t.m11 * y1 + t.m12;
    vertices(idx + 2) = colorPacked;
    vertices(idx + 3) = u1;
    vertices(idx + 4) = v1;

    vertices(idx + 5) = t.m00 * x2 + t.m01 * y2 + t.m02;
    vertices(idx + 6) = t.m10 * x2 + t.m11 * y2 + t.m12;
    vertices(idx + 7) = colorPacked;
    vertices(idx + 8) = u2;
    vertices(idx + 9) = v2;

    vertices(idx + 10) = t.m00 * x3 + t.m01 * y3 + t.m02;
    vertices(idx + 11) = t.m10 * x3 + t.m11 * y3 + t.m12;
    vertices(idx + 12) = colorPacked;
    vertices(idx + 13) = u3;
    vertices(idx + 14) = v3;

    vertices(idx + 15) = t.m00 * x4 + t.m01 * y4 + t.m02;
    vertices(idx + 16) = t.m10 * x4 + t.m11 * y4 + t.m12;
    vertices(idx + 17) = colorPacked;
    vertices(idx + 18) = u4;
    vertices(idx + 19) = v4;

    idx += Sprite.SPRITE_SIZE;
  }

  private def drawAdjusted(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = {
    if (!drawing) throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");

    if (region.getTexture() != lastTexture)
      switchTexture(region.getTexture());
    else if (idx == vertices.length) super.flush();

    val t = transform;

    // construct corner points
    val x1 = t.m02;
    val y1 = t.m12;
    val x2 = t.m01 * height + t.m02;
    val y2 = t.m11 * height + t.m12;
    val x3 = t.m00 * width + t.m01 * height + t.m02;
    val y3 = t.m10 * width + t.m11 * height + t.m12;
    val x4 = t.m00 * width + t.m02;
    val y4 = t.m10 * width + t.m12;

    // v must be flipped
    val u  = region.getU();
    val v  = region.getV2();
    val u2 = region.getU2();
    val v2 = region.getV();

    val tAdjust = adjustAffine;

    vertices(idx + 0) = tAdjust.m00 * x1 + tAdjust.m01 * y1 + tAdjust.m02;
    vertices(idx + 1) = tAdjust.m10 * x1 + tAdjust.m11 * y1 + tAdjust.m12;
    vertices(idx + 2) = colorPacked;
    vertices(idx + 3) = u;
    vertices(idx + 4) = v;

    vertices(idx + 5) = tAdjust.m00 * x2 + tAdjust.m01 * y2 + tAdjust.m02;
    vertices(idx + 6) = tAdjust.m10 * x2 + tAdjust.m11 * y2 + tAdjust.m12;
    vertices(idx + 7) = colorPacked;
    vertices(idx + 8) = u;
    vertices(idx + 9) = v2;

    vertices(idx + 10) = tAdjust.m00 * x3 + tAdjust.m01 * y3 + tAdjust.m02;
    vertices(idx + 11) = tAdjust.m10 * x3 + tAdjust.m11 * y3 + tAdjust.m12;
    vertices(idx + 12) = colorPacked;
    vertices(idx + 13) = u2;
    vertices(idx + 14) = v2;

    vertices(idx + 15) = tAdjust.m00 * x4 + tAdjust.m01 * y4 + tAdjust.m02;
    vertices(idx + 16) = tAdjust.m10 * x4 + tAdjust.m11 * y4 + tAdjust.m12;
    vertices(idx + 17) = colorPacked;
    vertices(idx + 18) = u2;
    vertices(idx + 19) = v;

    idx += Sprite.SPRITE_SIZE;
  }

  private def drawAdjusted(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit = {
    if (!drawing) throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");

    if (texture != lastTexture) switchTexture(texture);

    val t = adjustAffine;

    var copyCount      = Math.min(vertices.length - idx, count);
    var remainingCount = count;
    var currentOffset  = offset;

    while (remainingCount > 0) {
      remainingCount -= copyCount;
      while (copyCount > 0) {
        val x = spriteVertices(currentOffset);
        val y = spriteVertices(currentOffset + 1);

        vertices(idx) = t.m00 * x + t.m01 * y + t.m02; // x
        vertices(idx + 1) = t.m10 * x + t.m11 * y + t.m12; // y
        vertices(idx + 2) = spriteVertices(currentOffset + 2); // color
        vertices(idx + 3) = spriteVertices(currentOffset + 3); // u
        vertices(idx + 4) = spriteVertices(currentOffset + 4); // v

        idx += Sprite.VERTEX_SIZE;
        currentOffset += Sprite.VERTEX_SIZE;
        copyCount -= Sprite.VERTEX_SIZE;
      }

      if (remainingCount > 0) {
        super.flush();
        copyCount = Math.min(vertices.length, remainingCount);
      }
    }
  }
}

object CpuSpriteBatch {

  private def checkEqual(a: Matrix4, b: Matrix4): Boolean =
    if (a == b) true
    else {
      // matrices are assumed to be 2D transformations
      val aValues = a.getValues();
      val bValues = b.getValues();
      (aValues(Matrix4.M00) == bValues(Matrix4.M00) && aValues(Matrix4.M10) == bValues(Matrix4.M10)
      && aValues(Matrix4.M01) == bValues(Matrix4.M01) && aValues(Matrix4.M11) == bValues(Matrix4.M11)
      && aValues(Matrix4.M03) == bValues(Matrix4.M03) && aValues(Matrix4.M13) == bValues(Matrix4.M13))
    }

  private def checkEqual(matrix: Matrix4, affine: Affine2): Boolean = {
    val values = matrix.getValues();

    // matrix is assumed to be 2D transformation
    (values(Matrix4.M00) == affine.m00 && values(Matrix4.M10) == affine.m10 && values(Matrix4.M01) == affine.m01
    && values(Matrix4.M11) == affine.m11 && values(Matrix4.M03) == affine.m02 && values(Matrix4.M13) == affine.m12)
  }

  private def checkIdt(matrix: Matrix4): Boolean = {
    val values = matrix.getValues();

    // matrix is assumed to be 2D transformation
    (values(Matrix4.M00) == 1 && values(Matrix4.M10) == 0 && values(Matrix4.M01) == 0 && values(Matrix4.M11) == 1
    && values(Matrix4.M03) == 0 && values(Matrix4.M13) == 0)
  }
}
