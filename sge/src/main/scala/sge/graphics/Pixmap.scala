/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Pixmap.java
 * Original authors: badlogicgames@gmail.com, mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Blending/Filter/Format enums complete; Disposable -> AutoCloseable
 *   Idiom: split packages
 *   Fixes: Complete rewrite — all drawing methods (setColor, fill, drawLine, drawRectangle, fillRectangle, drawCircle,
 *     fillCircle, fillTriangle, drawPixel, drawPixmap), pixel access (getPixel, getPixels, setPixels), and all constructors
 *     now delegate to Gdx2DPixmap. Previously stubs with no functionality.
 *   Issues: Gdx2DPixmap native methods (load, newPixmap) are stubs — actual pixmap creation requires platform-specific native implementation.
 *   Idiom: opaque Pixels for getWidth/Height, drawPixmap x/y, getPixel x/y, drawPixel x/y params
 *   Convention: typed GL enums — PixelFormat, DataType for glReadPixels
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.files.FileHandle
import sge.graphics.g2d.Gdx2DPixmap
import sge.utils.{ BufferUtils, SgeError }
import java.io.IOException
import java.nio.ByteBuffer
import scala.util.control.NonFatal

import Pixmap.*

/** A Pixmap represents an image in memory. It has a width and height expressed in pixels as well as a {@link Format} specifying the number and order of color components per pixel. Coordinates of
  * pixels are specified with respect to the top left corner of the image, with the x-axis pointing to the right and the y-axis pointing downwards. <p> By default all methods use blending. You can
  * call {@link Pixmap#setBlending(Blending)} to disabled blending for all subsequent draw operations. You can also specify a color to be used for drawing operations via {@link Pixmap#setColor(Color)}
  * or {@link Pixmap#setColor(float, float, float, float)} .
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class Pixmap private (private val gdx2dPixmap: Gdx2DPixmap) extends AutoCloseable {

  private var _blending: Blending = Blending.SourceOver
  private var _filter:   Filter   = Filter.BiLinear
  private var color:     Int      = 0
  private var disposed:  Boolean  = false

  /** Creates a new Pixmap instance with the given width, height and format.
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param format
    *   the {@link Format}
    */
  def this(width: Int, height: Int, format: Format) = {
    this(Gdx2DPixmap(width, height, Format.toGdx2DPixmapFormat(format)))
    color = 0
    fill()
  }

  /** Creates a new Pixmap instance from the given encoded image data. The image can be encoded as JPEG, PNG or BMP.
    *
    * @param encodedData
    *   the encoded image data
    * @param offset
    *   the offset
    * @param len
    *   the length
    */
  def this(encodedData: Array[Byte], offset: Int, len: Int) = {
    this(
      try Gdx2DPixmap(encodedData, offset, len, 0)
      catch { case e: IOException => throw SgeError.GraphicsError("Couldn't load pixmap from image data", Some(e)) }
    )
  }

  /** Creates a new Pixmap instance from the given encoded image data. The image can be encoded as JPEG, PNG or BMP.
    *
    * @param encodedData
    *   the encoded image data
    * @param offset
    *   the offset relative to the base address of encodedData
    * @param len
    *   the length
    */
  def this(encodedData: ByteBuffer, offset: Int, len: Int) = {
    this(
      {
        if (!encodedData.isDirect()) throw SgeError.GraphicsError("Couldn't load pixmap from non-direct ByteBuffer")
        try Gdx2DPixmap(encodedData, offset, len, 0)
        catch { case e: IOException => throw SgeError.GraphicsError("Couldn't load pixmap from image data", Some(e)) }
      }
    )
  }

  /** Creates a new Pixmap instance from the given encoded image data. The image can be encoded as JPEG, PNG or BMP.
    *
    * Offset is based on the position of the buffer. Length is based on the remaining bytes of the buffer.
    *
    * @param encodedData
    *   the encoded image data
    */
  def this(encodedData: ByteBuffer) = {
    this(encodedData, encodedData.position(), encodedData.remaining())
  }

  /** Creates a new Pixmap instance from the given file. The file must be a Png, Jpeg or Bitmap. Paletted formats are not supported.
    *
    * @param file
    *   the {@link FileHandle}
    */
  def this(file: FileHandle) = {
    this(
      try {
        val bytes = file.readBytes()
        Gdx2DPixmap(bytes, 0, bytes.length, 0)
      } catch { case NonFatal(e) => throw SgeError.GraphicsError("Couldn't load file: " + file, Some(e)) }
    )
  }

  /** Sets the type of {@link Blending} to be used for all operations. Default is {@link Blending#SourceOver} .
    * @param blending
    *   the blending type
    */
  def setBlending(blending: Blending): Unit = {
    this._blending = blending
    gdx2dPixmap.setBlend(if (blending == Blending.None) 0 else 1)
  }

  /** @return the currently set {@link Blending} */
  def blending: Blending = _blending

  /** Sets the type of interpolation {@link Filter} to be used in conjunction with {@link Pixmap#drawPixmap(Pixmap, int, int, int, int, int, int, int, int)} .
    * @param filter
    *   the filter.
    */
  def setFilter(filter: Filter): Unit = {
    this._filter = filter
    gdx2dPixmap.setScale(if (filter == Filter.NearestNeighbour) Gdx2DPixmap.GDX2D_SCALE_NEAREST else Gdx2DPixmap.GDX2D_SCALE_LINEAR)
  }

  /** @return the currently set {@link Filter} */
  def filter: Filter = _filter

  /** Sets the color for the following drawing operations
    * @param color
    *   the color, encoded as RGBA8888
    */
  def setColor(color: Int): Unit =
    this.color = color

  /** Sets the color for the following drawing operations.
    * @param color
    *   The color.
    */
  def setColor(color: Color): Unit =
    this.color = Color.rgba8888(color.r, color.g, color.b, color.a)

  /** Fills the complete bitmap with the currently set color. */
  def fill(): Unit =
    gdx2dPixmap.clear(color)

  /** Draws a line between the given coordinates using the currently set color.
    *
    * @param x
    *   The x-coodinate of the first point
    * @param y
    *   The y-coordinate of the first point
    * @param x2
    *   The x-coordinate of the second point
    * @param y2
    *   The y-coordinate of the second point
    */
  def drawLine(x: Int, y: Int, x2: Int, y2: Int): Unit =
    gdx2dPixmap.drawLine(x, y, x2, y2, color)

  /** Draws a rectangle outline starting at x, y extending by width to the right and by height downwards (y-axis points downwards) using the current color.
    *
    * @param x
    *   The x coordinate
    * @param y
    *   The y coordinate
    * @param width
    *   The width in pixels
    * @param height
    *   The height in pixels
    */
  def drawRectangle(x: Int, y: Int, width: Int, height: Int): Unit =
    gdx2dPixmap.drawRect(x, y, width, height, color)

  /** Draws an area form another Pixmap to this Pixmap.
    * @param pixmap
    *   The other Pixmap
    * @param x
    *   The target x-coordinate (top left corner)
    * @param y
    *   The target y-coordinate (top left corner)
    */
  def drawPixmap(pixmap: Pixmap, x: Pixels, y: Pixels): Unit =
    drawPixmap(pixmap, x, y, Pixels.zero, Pixels.zero, pixmap.width, pixmap.height)

  /** Draws an area form another Pixmap to this Pixmap.
    * @param pixmap
    *   The other Pixmap
    * @param x
    *   The target x-coordinate (top left corner)
    * @param y
    *   The target y-coordinate (top left corner)
    * @param srcx
    *   The source x-coordinate (top left corner)
    * @param srcy
    *   The source y-coordinate (top left corner);
    * @param srcWidth
    *   The width of the area form the other Pixmap in pixels
    * @param srcHeight
    *   The height of the area form the other Pixmap in pixels
    */
  def drawPixmap(pixmap: Pixmap, x: Pixels, y: Pixels, srcx: Pixels, srcy: Pixels, srcWidth: Pixels, srcHeight: Pixels): Unit =
    gdx2dPixmap.drawPixmap(pixmap.gdx2dPixmap, srcx.toInt, srcy.toInt, x.toInt, y.toInt, srcWidth.toInt, srcHeight.toInt)

  /** Draws an area from another Pixmap to this Pixmap. This will automatically scale and stretch the source image to the specified target rectangle. Use {@link Pixmap#setFilter(Filter)} to specify
    * the type of filtering to be used (nearest neighbour or bilinear).
    *
    * @param pixmap
    *   The other Pixmap
    * @param srcx
    *   The source x-coordinate (top left corner)
    * @param srcy
    *   The source y-coordinate (top left corner);
    * @param srcWidth
    *   The width of the area from the other Pixmap in pixels
    * @param srcHeight
    *   The height of the area from the other Pixmap in pixels
    * @param dstx
    *   The target x-coordinate (top left corner)
    * @param dsty
    *   The target y-coordinate (top left corner)
    * @param dstWidth
    *   The target width
    * @param dstHeight
    *   the target height
    */
  def drawPixmap(pixmap: Pixmap, srcx: Pixels, srcy: Pixels, srcWidth: Pixels, srcHeight: Pixels, dstx: Pixels, dsty: Pixels, dstWidth: Pixels, dstHeight: Pixels): Unit =
    gdx2dPixmap.drawPixmap(
      pixmap.gdx2dPixmap,
      srcx.toInt,
      srcy.toInt,
      srcWidth.toInt,
      srcHeight.toInt,
      dstx.toInt,
      dsty.toInt,
      dstWidth.toInt,
      dstHeight.toInt
    )

  /** Fills a rectangle starting at x, y extending by width to the right and by height downwards (y-axis points downwards) using the current color.
    *
    * @param x
    *   The x coordinate
    * @param y
    *   The y coordinate
    * @param width
    *   The width in pixels
    * @param height
    *   The height in pixels
    */
  def fillRectangle(x: Int, y: Int, width: Int, height: Int): Unit =
    gdx2dPixmap.fillRect(x, y, width, height, color)

  /** Draws a circle outline with the center at x,y and a radius using the current color and stroke width.
    *
    * @param x
    *   The x-coordinate of the center
    * @param y
    *   The y-coordinate of the center
    * @param radius
    *   The radius in pixels
    */
  def drawCircle(x: Int, y: Int, radius: Int): Unit =
    gdx2dPixmap.drawCircle(x, y, radius, color)

  /** Fills a circle with the center at x,y and a radius using the current color.
    *
    * @param x
    *   The x-coordinate of the center
    * @param y
    *   The y-coordinate of the center
    * @param radius
    *   The radius in pixels
    */
  def fillCircle(x: Int, y: Int, radius: Int): Unit =
    gdx2dPixmap.fillCircle(x, y, radius, color)

  /** Fills a triangle with vertices at x1,y1 and x2,y2 and x3,y3 using the current color.
    *
    * @param x1
    *   The x-coordinate of vertex 1
    * @param y1
    *   The y-coordinate of vertex 1
    * @param x2
    *   The x-coordinate of vertex 2
    * @param y2
    *   The y-coordinate of vertex 2
    * @param x3
    *   The x-coordinate of vertex 3
    * @param y3
    *   The y-coordinate of vertex 3
    */
  def fillTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int): Unit =
    gdx2dPixmap.fillTriangle(x1, y1, x2, y2, x3, y3, color)

  /** Returns the 32-bit RGBA8888 value of the pixel at x, y. For Alpha formats the RGB components will be one.
    * @param x
    *   The x-coordinate
    * @param y
    *   The y-coordinate
    * @return
    *   The pixel color in RGBA8888 format.
    */
  def getPixel(x: Pixels, y: Pixels): Int =
    gdx2dPixmap.getPixel(x.toInt, y.toInt)

  /** @return The width of the Pixmap in pixels. */
  def width: Pixels = Pixels(gdx2dPixmap.width)

  /** @return The height of the Pixmap in pixels. */
  def height: Pixels = Pixels(gdx2dPixmap.height)

  /** @return the {@link Format} of this Pixmap. */
  def format: Format = Format.fromGdx2DPixmapFormat(gdx2dPixmap.format)

  /** Returns the OpenGL ES format of this Pixmap. Used as the seventh parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   one of GL_ALPHA, GL_RGB, GL_RGBA, GL_LUMINANCE, or GL_LUMINANCE_ALPHA.
    */
  def gLFormat: Int = gdx2dPixmap.glFormat

  /** Returns the OpenGL ES type of this Pixmap. Used as the eighth parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   one of GL_UNSIGNED_BYTE, GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_4_4_4_4
    */
  def glType: Int = gdx2dPixmap.glType

  /** Returns the OpenGL ES internal format of this Pixmap. Used as the third parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   the internal format for OpenGL texture creation.
    */
  def gLInternalFormat: Int = gdx2dPixmap.glInternalFormat

  /** Returns direct ByteBuffer holding the pixel data. For the format Alpha each value is encoded as a byte. For the format LuminanceAlpha the luminance is the first byte and the alpha is the second
    * byte of the pixel. For the formats RGB888 and RGBA8888 the color components are stored in a single byte each in the order red, green, blue (alpha). For the formats RGB565 and RGBA4444 the pixel
    * colors are stored in shorts in machine dependent order.
    * @return
    *   the direct {@link ByteBuffer} holding the pixel data.
    */
  def pixels: ByteBuffer = {
    if (disposed) throw SgeError.GraphicsError("Pixmap already disposed")
    gdx2dPixmap.pixels
  }

  /** Sets pixels from a provided direct byte buffer.
    * @param pixels
    *   Pixels to copy from, should be a direct ByteBuffer and match Pixmap data size (see {@link #getPixels()} ).
    */
  def pixels_=(pixels: ByteBuffer): Unit = {
    if (!pixels.isDirect()) throw SgeError.GraphicsError("Couldn't setPixels from non-direct ByteBuffer")
    val dst = gdx2dPixmap.pixels
    BufferUtils.copy(pixels, dst, dst.limit())
  }

  /** Draws a pixel at the given location with the current color.
    *
    * @param x
    *   the x-coordinate
    * @param y
    *   the y-coordinate
    */
  def drawPixel(x: Pixels, y: Pixels): Unit =
    gdx2dPixmap.setPixel(x.toInt, y.toInt, color)

  /** Draws a pixel at the given location with the given color.
    *
    * @param x
    *   the x-coordinate
    * @param y
    *   the y-coordinate
    * @param color
    *   the color in RGBA8888 format.
    */
  def drawPixel(x: Pixels, y: Pixels, color: Int): Unit =
    gdx2dPixmap.setPixel(x.toInt, y.toInt, color)

  /** Releases all resources associated with this Pixmap. */
  override def close(): Unit =
    if (!disposed) {
      gdx2dPixmap.close()
      disposed = true
    }

  def isDisposed: Boolean = disposed
}
object Pixmap {

  /** Creates a Pixmap from a part of the current framebuffer.
    * @param x
    *   framebuffer region x
    * @param y
    *   framebuffer region y
    * @param w
    *   framebuffer region width
    * @param h
    *   framebuffer region height
    * @return
    *   the pixmap
    */
  def createFromFrameBuffer(x: Pixels, y: Pixels, w: Pixels, h: Pixels)(using Sge): Pixmap = {
    Sge().graphics.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1)
    val pixmap = Pixmap(w.toInt, h.toInt, Format.RGBA8888)
    val pixels = pixmap.pixels
    Sge().graphics.gl.glReadPixels(x, y, w, h, PixelFormat.RGBA, DataType.UnsignedByte, pixels)
    pixmap
  }

  /** Blending functions to be set with {@link Pixmap#setBlending} .
    * @author
    *   mzechner (original implementation)
    */
  enum Blending {
    case None, SourceOver
  }

  /** Filters to be used with {@link Pixmap#drawPixmap(Pixmap, int, int, int, int, int, int, int, int)} .
    *
    * @author
    *   mzechner (original implementation)
    */
  enum Filter {
    case NearestNeighbour, BiLinear
  }

  /** Different pixel formats.
    *
    * @author
    *   mzechner (original implementation)
    */
  enum Format {
    case Alpha, Intensity, LuminanceAlpha, RGB565, RGBA4444, RGB888, RGBA8888
  }
  object Format {
    def toGdx2DPixmapFormat(format: Format): Int = format match {
      case Alpha          => Gdx2DPixmap.GDX2D_FORMAT_ALPHA
      case Intensity      => Gdx2DPixmap.GDX2D_FORMAT_ALPHA
      case LuminanceAlpha => Gdx2DPixmap.GDX2D_FORMAT_LUMINANCE_ALPHA
      case RGB565         => Gdx2DPixmap.GDX2D_FORMAT_RGB565
      case RGBA4444       => Gdx2DPixmap.GDX2D_FORMAT_RGBA4444
      case RGB888         => Gdx2DPixmap.GDX2D_FORMAT_RGB888
      case RGBA8888       => Gdx2DPixmap.GDX2D_FORMAT_RGBA8888
    }

    def fromGdx2DPixmapFormat(format: Int): Format = format match {
      case Gdx2DPixmap.GDX2D_FORMAT_ALPHA           => Format.Alpha
      case Gdx2DPixmap.GDX2D_FORMAT_LUMINANCE_ALPHA => Format.LuminanceAlpha
      case Gdx2DPixmap.GDX2D_FORMAT_RGB565          => Format.RGB565
      case Gdx2DPixmap.GDX2D_FORMAT_RGBA4444        => Format.RGBA4444
      case Gdx2DPixmap.GDX2D_FORMAT_RGB888          => Format.RGB888
      case Gdx2DPixmap.GDX2D_FORMAT_RGBA8888        => Format.RGBA8888
      case _                                        => Format.RGBA8888
    }

    def toGlFormat(format: Format): Int =
      Gdx2DPixmap.toGlFormat(toGdx2DPixmapFormat(format))

    def toGlType(format: Format): Int =
      Gdx2DPixmap.toGlType(toGdx2DPixmapFormat(format))
  }

  /** Response listener for {@link #downloadFromUrl(String, DownloadPixmapResponseListener)} */
  trait DownloadPixmapResponseListener {

    /** Called on the render thread when image was downloaded successfully.
      * @param pixmap
      */
    def downloadComplete(pixmap: Pixmap): Unit

    /** Called when image download failed. This might get called on a background thread. */
    def downloadFailed(t: Throwable): Unit
  }
}
