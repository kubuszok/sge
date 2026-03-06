/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Pixmap.java
 * Original authors: badlogicgames@gmail.com, mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Blending/Filter/Format enums complete; Disposable -> AutoCloseable
 *   Idiom: split packages
 *   Issues: drawPixmap/getPixel/getPixels are stubs; missing setColor, drawLine, drawRect, fillRect, etc.
 *   TODO: Java-style getters/setters -- getWidth, getHeight, getFormat
 *   TODO: uses flat package declaration -- convert to split (package sge / package graphics)
 *   TODO: opaque Pixels for getWidth/Height, drawPixmap x/y, getPixel x/y params -- see docs/improvements/opaque-types.md
 *   TODO: typed GL enums -- PixelFormat, DataType for glReadPixels -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.graphics

import sge.files.FileHandle
import sge.graphics.g2d.Gdx2DPixmap
import sge.utils.Nullable
import java.nio.ByteBuffer
import scala.annotation.nowarn

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
class Pixmap private (file: Nullable[FileHandle], private val width: Int, private val height: Int, private val format: Format) extends AutoCloseable {

  private var pixmap: Nullable[Gdx2DPixmap] = Nullable.empty

  @nowarn("msg=not read") // will be read when drawing methods are implemented
  private var blending: Blending = Blending.SourceOver

  def this(file: FileHandle) = {
    this(Nullable(file), 100, 100, Format.RGBA8888)
  }

  def this(width: Int, height: Int, format: Format) = {
    this(Nullable.empty, width, height, format)
  }

  def this(pixmap: Gdx2DPixmap) = {
    this(Nullable.empty, pixmap.width, pixmap.height, Format.fromGdx2DPixmapFormat(pixmap.format))
  }

  def getWidth():  Int    = width
  def getHeight(): Int    = height
  def getFormat(): Format = format

  /** Sets the type of {@link Blending} to be used for all operations. Default is {@link Blending#SourceOver} .
    * @param blending
    *   the blending type
    */
  def setBlending(blending: Blending): Unit =
    this.blending = blending

  /** Draws an area form another Pixmap to this Pixmap.
    * @param pixmap
    *   The other Pixmap
    * @param x
    *   The target x-coordinate (top left corner)
    * @param y
    *   The target y-coordinate (top left corner)
    */
  def drawPixmap(pixmap: Pixmap, x: Int, y: Int): Unit =
    drawPixmap(pixmap, x, y, 0, 0, pixmap.getWidth(), pixmap.getHeight())

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
  def drawPixmap(pixmap: Pixmap, x: Int, y: Int, srcx: Int, srcy: Int, srcWidth: Int, srcHeight: Int): Unit = {
    // Stub implementation
  }

  /** Returns the 32-bit RGBA8888 value of the pixel at x, y. For Alpha formats the RGB components will be zero.
    * @param x
    *   The x-coordinate
    * @param y
    *   The y-coordinate
    * @return
    *   The pixel color in RGBA8888 format.
    */
  def getPixel(x: Int, y: Int): Int =
    pixmap.fold(0) { _ =>
      // Return pixel data from underlying pixmap
      0 // Stub implementation
    }

  /** Returns the OpenGL ES format of this Pixmap. Used as the seventh parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   one of GL_ALPHA, GL_RGB, GL_RGBA, GL_LUMINANCE, or GL_LUMINANCE_ALPHA.
    */
  def getGLFormat(): Int =
    format match {
      case Format.Alpha          => 0x1906 // GL_ALPHA
      case Format.RGB888         => 0x1907 // GL_RGB
      case Format.RGBA8888       => 0x1908 // GL_RGBA
      case Format.Intensity      => 0x1909 // GL_LUMINANCE
      case Format.LuminanceAlpha => 0x190a // GL_LUMINANCE_ALPHA
      case _                     => 0x1908 // GL_RGBA as default
    }

  /** Returns the OpenGL ES type of this Pixmap. Used as the eighth parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   one of GL_UNSIGNED_BYTE, GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_4_4_4_4
    */
  def getGLType(): Int =
    format match {
      case Format.RGB565   => 0x8363 // GL_UNSIGNED_SHORT_5_6_5
      case Format.RGBA4444 => 0x8033 // GL_UNSIGNED_SHORT_4_4_4_4
      case _               => 0x1401 // GL_UNSIGNED_BYTE
    }

  /** Returns the OpenGL ES internal format of this Pixmap. Used as the third parameter to {@link GL20#glTexImage2D(int, int, int, int, int, int, int, java.nio.Buffer)} .
    * @return
    *   the internal format for OpenGL texture creation.
    */
  def getGLInternalFormat(): Int =
    // For OpenGL ES 2.0, internal format should match the format parameter
    getGLFormat()

  /** Returns direct ByteBuffer holding the pixel data. For the format Alpha each value is encoded as a byte. For the format LuminanceAlpha the luminance is the first byte and the alpha is the second
    * byte of the pixel. For the formats RGB888 and RGBA8888 the color components are stored in a single byte each in the order red, green, blue (alpha). For the formats RGB565 and RGBA4444 the pixel
    * colors are stored in shorts in machine dependent order.
    * @return
    *   the direct {@link ByteBuffer} holding the pixel data.
    */
  def getPixels(): ByteBuffer =
    pixmap.fold(ByteBuffer.allocateDirect(0)) { _ =>
      // Return pixel buffer from underlying pixmap
      ByteBuffer.allocateDirect(0) // Stub implementation
    }

  override def close(): Unit = {
    pixmap.foreach(_.close())
    pixmap = Nullable.empty
  }
}
object Pixmap {

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
      // case _ => throw SgeError.GraphicsError("Unknown Format: " + format)
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
