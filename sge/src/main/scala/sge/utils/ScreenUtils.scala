/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/ScreenUtils.java
 * Original authors: espitz
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Gdx.gl`/`Gdx.graphics` -> `Sge().graphics.gl`/`Sge().graphics`
 *   Convention: methods take `(using Sge)` context parameter
 *   Idiom: split packages
 *   Fixes: `getFrameBufferTexture` and `getFrameBufferPixmap` unblocked by `Pixmap.createFromFrameBuffer` port
 *   Idiom: opaque Pixels for pixel dimension params in getFrameBufferPixels and getFrameBufferTexture
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.nio.Buffer
import sge.graphics.{ ClearMask, Color, DataType, GL20, PixelFormat, Pixmap, Texture }
import sge.graphics.Pixmap.{ Blending, Format }
import sge.graphics.g2d.TextureRegion
import sge.math.MathUtils

/** Class with static helper methods related to currently bound OpenGL frame buffer, including access to the current OpenGL FrameBuffer. These methods can be used to get the entire screen content or a
  * portion thereof.
  *
  * @author
  *   espitz (original implementation)
  */
object ScreenUtils {

  /** Clears the color buffers with the specified Color.
    * @param color
    *   Color to clear the color buffers with.
    */
  def clear(color: Color)(using Sge): Unit =
    clear(color.r, color.g, color.b, color.a, false)

  /** Clears the color buffers with the specified color. */
  def clear(r: Float, g: Float, b: Float, a: Float)(using Sge): Unit =
    clear(r, g, b, a, false)

  /** Clears the color buffers and optionally the depth buffer.
    * @param color
    *   Color to clear the color buffers with.
    * @param clearDepth
    *   Clears the depth buffer if true.
    */
  def clear(color: Color, clearDepth: Boolean)(using Sge): Unit =
    clear(color.r, color.g, color.b, color.a, clearDepth)

  /** Clears the color buffers and optionally the depth buffer.
    * @param clearDepth
    *   Clears the depth buffer if true.
    */
  def clear(r: Float, g: Float, b: Float, a: Float, clearDepth: Boolean)(using Sge): Unit =
    clear(r, g, b, a, clearDepth, false)

  /** Clears the color buffers, optionally the depth buffer and whether to apply antialiasing (requires to set number of samples in the launcher class).
    *
    * @param clearDepth
    *   Clears the depth buffer if true.
    * @param applyAntialiasing
    *   applies multi-sampling for antialiasing if true.
    */
  def clear(r: Float, g: Float, b: Float, a: Float, clearDepth: Boolean, applyAntialiasing: Boolean)(using Sge): Unit = {
    Sge().graphics.gl.glClearColor(r, g, b, a)
    var mask: ClearMask = ClearMask.ColorBufferBit
    if (clearDepth) mask = mask | ClearMask.DepthBufferBit
    if (applyAntialiasing && Sge().graphics.getBufferFormat().coverageSampling) mask = mask | ClearMask(GL20.GL_COVERAGE_BUFFER_BIT_NV)
    Sge().graphics.gl.glClear(mask)
  }

  /** Returns the current framebuffer contents as a {@link TextureRegion} with a width and height equal to the current screen size. The base {@link Texture} always has {@link MathUtils#nextPowerOfTwo}
    * dimensions and RGBA8888 {@link Format}. It can be accessed via {@link TextureRegion#getTexture}. The texture is not managed and has to be reloaded manually on a context loss. The returned
    * TextureRegion is flipped along the Y axis by default.
    */
  def getFrameBufferTexture()(using Sge): TextureRegion = {
    val w = Sge().graphics.getBackBufferWidth()
    val h = Sge().graphics.getBackBufferHeight()
    getFrameBufferTexture(Pixels.zero, Pixels.zero, w, h)
  }

  /** Returns a portion of the current framebuffer contents specified by x, y, width and height as a {@link TextureRegion} with the same dimensions. The base {@link Texture} always has
    * {@link MathUtils#nextPowerOfTwo} dimensions and RGBA8888 {@link Format}. It can be accessed via {@link TextureRegion#getTexture}. This texture is not managed and has to be reloaded manually on a
    * context loss. If the width and height specified are larger than the framebuffer dimensions, the Texture will be padded accordingly. Pixels that fall outside of the current screen will have RGBA
    * values of 0.
    *
    * @param x
    *   the x position of the framebuffer contents to capture
    * @param y
    *   the y position of the framebuffer contents to capture
    * @param w
    *   the width of the framebuffer contents to capture
    * @param h
    *   the height of the framebuffer contents to capture
    */
  def getFrameBufferTexture(x: Pixels, y: Pixels, w: Pixels, h: Pixels)(using Sge): TextureRegion = {
    val potW = MathUtils.nextPowerOfTwo(w.toInt)
    val potH = MathUtils.nextPowerOfTwo(h.toInt)

    val pixmap    = Pixmap.createFromFrameBuffer(x, y, w, h)
    val potPixmap = Pixmap(potW, potH, Format.RGBA8888)
    potPixmap.setBlending(Blending.None)
    potPixmap.drawPixmap(pixmap, Pixels.zero, Pixels.zero)
    val texture       = Texture(potPixmap)
    val textureRegion = TextureRegion(texture, 0, h.toInt, w.toInt, -h.toInt)
    potPixmap.close()
    pixmap.close()

    textureRegion
  }

  /** @deprecated use {@link Pixmap#createFromFrameBuffer(int, int, int, int)} instead. */
  @deprecated("use Pixmap.createFromFrameBuffer instead", "")
  def getFrameBufferPixmap(x: Pixels, y: Pixels, w: Pixels, h: Pixels)(using Sge): Pixmap =
    Pixmap.createFromFrameBuffer(x, y, w, h)

  /** Returns the current framebuffer contents as a byte[] array with a length equal to screen width * height * 4. The byte[] will always contain RGBA8888 data. Because of differences in screen and
    * image origins the framebuffer contents should be flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this functionality wisely.
    *
    * @param flipY
    *   whether to flip pixels along Y axis
    */
  def getFrameBufferPixels(flipY: Boolean)(using Sge): Array[Byte] = {
    val w = Sge().graphics.getBackBufferWidth()
    val h = Sge().graphics.getBackBufferHeight()
    getFrameBufferPixels(Pixels.zero, Pixels.zero, w, h, flipY)
  }

  /** Returns a portion of the current framebuffer contents specified by x, y, width and height, as a byte[] array with a length equal to the specified width * height * 4. The byte[] will always
    * contain RGBA8888 data. If the width and height specified are larger than the framebuffer dimensions, the Texture will be padded accordingly. Pixels that fall outside of the current screen will
    * have RGBA values of 0. Because of differences in screen and image origins the framebuffer contents should be flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not
    * a cheap operation, so use this functionality wisely.
    *
    * @param flipY
    *   whether to flip pixels along Y axis
    */
  def getFrameBufferPixels(x: Pixels, y: Pixels, w: Pixels, h: Pixels, flipY: Boolean)(using Sge): Array[Byte] = {
    Sge().graphics.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
    val wi     = w.toInt
    val hi     = h.toInt
    val pixels = BufferUtils.newByteBuffer(wi * hi * 4)
    Sge().graphics.gl.glReadPixels(x, y, w, h, PixelFormat.RGBA, DataType.UnsignedByte, pixels)
    val numBytes = wi * hi * 4
    val lines    = new Array[Byte](numBytes)
    if (flipY) {
      val numBytesPerLine = wi * 4
      var i               = 0
      while (i < hi) {
        pixels.asInstanceOf[Buffer].position((hi - i - 1) * numBytesPerLine)
        pixels.get(lines, i * numBytesPerLine, numBytesPerLine)
        i += 1
      }
    } else {
      pixels.asInstanceOf[Buffer].clear()
      pixels.get(lines)
    }
    lines
  }
}
