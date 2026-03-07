/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Gdx2DPixmap.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; dispose() -> close()
 *   Convention: JNI native methods replaced with stub implementations
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Native stubs now use Nullable.empty.orNull with @nowarn at JNI boundary.
 *   Issues: (1) Missing newPixmap(InputStream,Int) and newPixmap(Int,Int,Int) factory methods. (2) All JNI stubs need real platform-specific implementations.
 *   Fixes: Java-style getters (getWidth/getHeight/getFormat/getPixels/getGLInternalFormat/getGLFormat/getGLType/getFormatString) → Scala properties
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.GL20
import sge.utils.{ Nullable, SgeError }
import scala.annotation.nowarn
import java.nio.ByteBuffer
import java.io.{ ByteArrayOutputStream, IOException, InputStream }
import scala.compiletime.uninitialized

/** @author mzechner (original implementation) */
class Gdx2DPixmap extends AutoCloseable {
  import Gdx2DPixmap.*

  private var basePtr:    Long        = uninitialized
  private var _width:     Int         = uninitialized
  private var _height:    Int         = uninitialized
  private var _format:    Int         = uninitialized
  private var _pixelPtr:  ByteBuffer  = uninitialized
  private var nativeData: Array[Long] = Array.ofDim[Long](4)

  def this(encodedData: Array[Byte], offset: Int, len: Int, requestedFormat: Int) = {
    this()
    try {
      _pixelPtr = load(nativeData, encodedData, offset, len)
      if (Nullable(_pixelPtr).isEmpty) throw new IOException("Error loading pixmap: " + getFailureReason())

      basePtr = nativeData(0)
      _width = nativeData(1).toInt
      _height = nativeData(2).toInt
      _format = nativeData(3).toInt

      if (requestedFormat != 0 && requestedFormat != _format) {
        convert(requestedFormat)
      }
    } catch {
      case e: IOException => throw e
    }
  }

  def this(encodedData: ByteBuffer, offset: Int, len: Int, requestedFormat: Int) = {
    this()
    try {
      if (!encodedData.isDirect()) throw new IOException("Couldn't load pixmap from non-direct ByteBuffer")
      _pixelPtr = loadByteBuffer(nativeData, encodedData, offset, len)
      if (Nullable(_pixelPtr).isEmpty) throw new IOException("Error loading pixmap: " + getFailureReason())

      basePtr = nativeData(0)
      _width = nativeData(1).toInt
      _height = nativeData(2).toInt
      _format = nativeData(3).toInt

      if (requestedFormat != 0 && requestedFormat != _format) {
        convert(requestedFormat)
      }
    } catch {
      case e: IOException => throw e
    }
  }

  def this(in: InputStream, requestedFormat: Int) = {
    this()
    try {
      val bytes     = new ByteArrayOutputStream(1024)
      val buffer    = new Array[Byte](1024)
      var readBytes = 0

      readBytes = in.read(buffer)
      while (readBytes != -1) {
        bytes.write(buffer, 0, readBytes)
        readBytes = in.read(buffer)
      }

      val finalBuffer = bytes.toByteArray()
      _pixelPtr = load(nativeData, finalBuffer, 0, finalBuffer.length)
      if (Nullable(_pixelPtr).isEmpty) throw new IOException("Error loading pixmap: " + getFailureReason())

      basePtr = nativeData(0)
      _width = nativeData(1).toInt
      _height = nativeData(2).toInt
      _format = nativeData(3).toInt

      if (requestedFormat != 0 && requestedFormat != _format) {
        convert(requestedFormat)
      }
    } catch {
      case e: IOException => throw e
    }
  }

  /** @throws SgeError.GraphicsError if allocation failed. */
  def this(width: Int, height: Int, format: Int) = {
    this()
    _pixelPtr = newPixmap(nativeData, width, height, format)
    if (Nullable(_pixelPtr).isEmpty) throw SgeError.GraphicsError(s"Unable to allocate memory for pixmap: ${width}x$height, ${getFormatString(format)}")

    this.basePtr = nativeData(0)
    this._width = nativeData(1).toInt
    this._height = nativeData(2).toInt
    this._format = nativeData(3).toInt
  }

  def this(pixelPtr: ByteBuffer, nativeData: Array[Long]) = {
    this()
    this._pixelPtr = pixelPtr
    this.basePtr = nativeData(0)
    this._width = nativeData(1).toInt
    this._height = nativeData(2).toInt
    this._format = nativeData(3).toInt
  }

  private def convert(requestedFormat: Int): Unit = {
    val pixmap = Gdx2DPixmap(_width, _height, requestedFormat)
    pixmap.setBlend(GDX2D_BLEND_NONE)
    pixmap.drawPixmap(this, 0, 0, 0, 0, _width, _height)
    close()
    this.basePtr = pixmap.basePtr
    this._format = pixmap._format
    this._height = pixmap._height
    this.nativeData = pixmap.nativeData
    this._pixelPtr = pixmap._pixelPtr
    this._width = pixmap._width
  }

  override def close(): Unit =
    free(basePtr)

  def clear(color: Int): Unit =
    clear(basePtr, color)

  def setPixel(x: Int, y: Int, color: Int): Unit =
    setPixel(basePtr, x, y, color)

  def getPixel(x: Int, y: Int): Int =
    getPixel(basePtr, x, y)

  def drawLine(x: Int, y: Int, x2: Int, y2: Int, color: Int): Unit =
    drawLine(basePtr, x, y, x2, y2, color)

  def drawRect(x: Int, y: Int, width: Int, height: Int, color: Int): Unit =
    drawRect(basePtr, x, y, width, height, color)

  def drawCircle(x: Int, y: Int, radius: Int, color: Int): Unit =
    drawCircle(basePtr, x, y, radius, color)

  def fillRect(x: Int, y: Int, width: Int, height: Int, color: Int): Unit =
    fillRect(basePtr, x, y, width, height, color)

  def fillCircle(x: Int, y: Int, radius: Int, color: Int): Unit =
    fillCircle(basePtr, x, y, radius, color)

  def fillTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int): Unit =
    fillTriangle(basePtr, x1, y1, x2, y2, x3, y3, color)

  def drawPixmap(src: Gdx2DPixmap, srcX: Int, srcY: Int, dstX: Int, dstY: Int, width: Int, height: Int): Unit =
    drawPixmap(src.basePtr, basePtr, srcX, srcY, width, height, dstX, dstY, width, height)

  def drawPixmap(src: Gdx2DPixmap, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, dstX: Int, dstY: Int, dstWidth: Int, dstHeight: Int): Unit =
    drawPixmap(src.basePtr, basePtr, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight)

  def setBlend(blend: Int): Unit =
    setBlend(basePtr, blend)

  def setScale(scale: Int): Unit =
    setScale(basePtr, scale)

  def glInternalFormat: Int =
    toGlFormat(_format)

  def glFormat: Int =
    toGlFormat(_format)

  def glType: Int =
    toGlType(_format)

  def formatString: String = getFormatString(_format)

  def pixels: ByteBuffer = _pixelPtr

  def height: Int = _height

  def width: Int = _width

  def format: Int = _format

  // Native method stubs - these would be implemented as JNI calls.
  // Returns null: JNI native boundary — callers check with Nullable().
  @nowarn("msg=deprecated")
  private def load(nativeData: Array[Long], buffer: Array[Byte], offset: Int, len: Int): ByteBuffer =
    // Stub implementation — JNI native method
    Nullable.empty[ByteBuffer].orNull

  @nowarn("msg=deprecated")
  private def loadByteBuffer(nativeData: Array[Long], buffer: ByteBuffer, offset: Int, len: Int): ByteBuffer =
    // Stub implementation — JNI native method
    Nullable.empty[ByteBuffer].orNull

  @nowarn("msg=deprecated")
  private def newPixmap(nativeData: Array[Long], width: Int, height: Int, format: Int): ByteBuffer =
    // Stub implementation — JNI native method
    Nullable.empty[ByteBuffer].orNull

  private def free(basePtr: Long): Unit = {
    // Stub implementation
  }

  private def clear(basePtr: Long, color: Int): Unit = {
    // Stub implementation
  }

  private def setPixel(basePtr: Long, x: Int, y: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def getPixel(basePtr: Long, x: Int, y: Int): Int =
    // Stub implementation
    0

  private def drawLine(basePtr: Long, x: Int, y: Int, x2: Int, y2: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def drawRect(basePtr: Long, x: Int, y: Int, width: Int, height: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def drawCircle(basePtr: Long, x: Int, y: Int, radius: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def fillRect(basePtr: Long, x: Int, y: Int, width: Int, height: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def fillCircle(basePtr: Long, x: Int, y: Int, radius: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def fillTriangle(basePtr: Long, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int): Unit = {
    // Stub implementation
  }

  private def drawPixmap(srcPtr: Long, dstPtr: Long, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, dstX: Int, dstY: Int, dstWidth: Int, dstHeight: Int): Unit = {
    // Stub implementation
  }

  private def setBlend(basePtr: Long, blend: Int): Unit = {
    // Stub implementation
  }

  private def setScale(basePtr: Long, scale: Int): Unit = {
    // Stub implementation
  }

  private def getFailureReason(): String =
    // Stub implementation
    "Unknown error"

  private def getFormatString(format: Int): String =
    format match {
      case GDX2D_FORMAT_ALPHA           => "alpha"
      case GDX2D_FORMAT_LUMINANCE_ALPHA => "luminance alpha"
      case GDX2D_FORMAT_RGB888          => "rgb888"
      case GDX2D_FORMAT_RGBA8888        => "rgba8888"
      case GDX2D_FORMAT_RGB565          => "rgb565"
      case GDX2D_FORMAT_RGBA4444        => "rgba4444"
      case _                            => "unknown"
    }
}

object Gdx2DPixmap {
  val GDX2D_FORMAT_ALPHA           = 1
  val GDX2D_FORMAT_LUMINANCE_ALPHA = 2
  val GDX2D_FORMAT_RGB888          = 3
  val GDX2D_FORMAT_RGBA8888        = 4
  val GDX2D_FORMAT_RGB565          = 5
  val GDX2D_FORMAT_RGBA4444        = 6

  val GDX2D_SCALE_NEAREST = 0
  val GDX2D_SCALE_LINEAR  = 1

  val GDX2D_BLEND_NONE     = 0
  val GDX2D_BLEND_SRC_OVER = 1

  def toGlFormat(format: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA =>
        GL20.GL_ALPHA
      case GDX2D_FORMAT_LUMINANCE_ALPHA =>
        GL20.GL_LUMINANCE_ALPHA
      case GDX2D_FORMAT_RGB888 | GDX2D_FORMAT_RGB565 =>
        GL20.GL_RGB
      case GDX2D_FORMAT_RGBA8888 | GDX2D_FORMAT_RGBA4444 =>
        GL20.GL_RGBA
      case _ =>
        throw SgeError.GraphicsError("unknown format: " + format)
    }

  def toGlType(format: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA | GDX2D_FORMAT_LUMINANCE_ALPHA | GDX2D_FORMAT_RGB888 | GDX2D_FORMAT_RGBA8888 =>
        GL20.GL_UNSIGNED_BYTE
      case GDX2D_FORMAT_RGB565 =>
        GL20.GL_UNSIGNED_SHORT_5_6_5
      case GDX2D_FORMAT_RGBA4444 =>
        GL20.GL_UNSIGNED_SHORT_4_4_4_4
      case _ =>
        throw SgeError.GraphicsError("unknown format: " + format)
    }
}
