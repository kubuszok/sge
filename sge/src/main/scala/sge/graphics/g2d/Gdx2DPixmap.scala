/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Gdx2DPixmap.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; dispose() -> close()
 *   Convention: JNI native methods replaced with pure Scala drawing + platform decode
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: All drawing operations are now pure Scala (Gdx2dDraw), portable across JVM/JS/Native.
 *          Image decoding uses platform-specific Gdx2dOps (ImageIO on JVM, stubs on JS/Native).
 *   Fixes: Java-style getters → Scala properties
 *   Audited: 2026-03-10
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 293
 * Covenant-baseline-methods: GDX2D_BLEND_NONE,GDX2D_BLEND_SRC_OVER,GDX2D_FORMAT_ALPHA,GDX2D_FORMAT_LUMINANCE_ALPHA,GDX2D_FORMAT_RGB565,GDX2D_FORMAT_RGB888,GDX2D_FORMAT_RGBA4444,GDX2D_FORMAT_RGBA8888,GDX2D_SCALE_LINEAR,GDX2D_SCALE_NEAREST,Gdx2DPixmap,_blend,_format,_height,_pixelPtr,_scale,_width,buffer,bytes,clear,close,convert,drawCircle,drawLine,drawPixmap,drawRect,fillCircle,fillRect,fillTriangle,finalBuffer,format,formatString,getFormatString,getPixel,glFormat,glInternalFormat,glType,height,pixels,pixmap,readBytes,result,savedPos,setBlend,setPixel,setScale,this,toGlFormat,toGlType,width
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/Gdx2DPixmap.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g2d

import sge.graphics.GL20
import sge.platform.PlatformOps
import sge.utils.SgeError
import java.nio.ByteBuffer
import java.io.{ ByteArrayOutputStream, IOException, InputStream }

/** @author mzechner (original implementation) */
class Gdx2DPixmap extends AutoCloseable {
  import Gdx2DPixmap.*

  private var _width:    Int        = 0
  private var _height:   Int        = 0
  private var _format:   Int        = 0
  private var _pixelPtr: ByteBuffer = scala.compiletime.uninitialized
  private var _blend:    Int        = GDX2D_BLEND_SRC_OVER
  private var _scale:    Int        = GDX2D_SCALE_LINEAR

  def this(encodedData: Array[Byte], offset: Int, len: Int, requestedFormat: Int) = {
    this()
    val result = PlatformOps.gdx2d.decodeImage(encodedData, offset, len)
    result match {
      case Some(r) =>
        _pixelPtr = r.pixels
        _width = r.width
        _height = r.height
        _format = r.format
        if (requestedFormat != 0 && requestedFormat != _format) {
          convert(requestedFormat)
        }
      case None =>
        throw new IOException("Error loading pixmap: " + PlatformOps.gdx2d.failureReason)
    }
  }

  def this(encodedData: ByteBuffer, offset: Int, len: Int, requestedFormat: Int) = {
    this()
    // Copy ByteBuffer data into an array for decoding
    val bytes    = new Array[Byte](len)
    val savedPos = encodedData.position()
    encodedData.position(offset)
    encodedData.get(bytes, 0, len)
    encodedData.position(savedPos)

    val result = PlatformOps.gdx2d.decodeImage(bytes, 0, len)
    result match {
      case Some(r) =>
        _pixelPtr = r.pixels
        _width = r.width
        _height = r.height
        _format = r.format
        if (requestedFormat != 0 && requestedFormat != _format) {
          convert(requestedFormat)
        }
      case None =>
        throw new IOException("Error loading pixmap: " + PlatformOps.gdx2d.failureReason)
    }
  }

  def this(in: InputStream, requestedFormat: Int) = {
    this()
    val bytes     = new ByteArrayOutputStream(1024)
    val buffer    = new Array[Byte](1024)
    var readBytes = in.read(buffer)
    while (readBytes != -1) {
      bytes.write(buffer, 0, readBytes)
      readBytes = in.read(buffer)
    }

    val finalBuffer = bytes.toByteArray()
    val result      = PlatformOps.gdx2d.decodeImage(finalBuffer, 0, finalBuffer.length)
    result match {
      case Some(r) =>
        _pixelPtr = r.pixels
        _width = r.width
        _height = r.height
        _format = r.format
        if (requestedFormat != 0 && requestedFormat != _format) {
          convert(requestedFormat)
        }
      case None =>
        throw new IOException("Error loading pixmap: " + PlatformOps.gdx2d.failureReason)
    }
  }

  /** @throws SgeError.GraphicsError if allocation failed. */
  def this(width: Int, height: Int, format: Int) = {
    this()
    _pixelPtr = Gdx2dDraw.newPixelBuffer(width, height, format)
    _width = width
    _height = height
    _format = format
  }

  def this(pixelPtr: ByteBuffer, nativeData: Array[Long]) = {
    this()
    this._pixelPtr = pixelPtr
    this._width = nativeData(1).toInt
    this._height = nativeData(2).toInt
    this._format = nativeData(3).toInt
  }

  private def convert(requestedFormat: Int): Unit = {
    val pixmap = new Gdx2DPixmap(_width, _height, requestedFormat)
    pixmap._blend = GDX2D_BLEND_NONE
    pixmap.drawPixmap(this, 0, 0, 0, 0, _width, _height)
    // Take over the new pixmap's data
    this._format = pixmap._format
    this._pixelPtr = pixmap._pixelPtr
  }

  override def close(): Unit = {
    // ByteBuffer is managed by GC — no native free needed
  }

  def clear(color: Int): Unit =
    Gdx2dDraw.clear(_pixelPtr, _width, _height, _format, color)

  def setPixel(x: Int, y: Int, color: Int): Unit =
    Gdx2dDraw.setPixel(_pixelPtr, _width, _height, _format, _blend, x, y, color)

  def getPixel(x: Int, y: Int): Int =
    Gdx2dDraw.getPixel(_pixelPtr, _width, _height, _format, x, y)

  def drawLine(x: Int, y: Int, x2: Int, y2: Int, color: Int): Unit =
    Gdx2dDraw.drawLine(_pixelPtr, _width, _height, _format, _blend, x, y, x2, y2, color)

  def drawRect(x: Int, y: Int, width: Int, height: Int, color: Int): Unit =
    Gdx2dDraw.drawRect(_pixelPtr, _width, _height, _format, _blend, x, y, width, height, color)

  def drawCircle(x: Int, y: Int, radius: Int, color: Int): Unit =
    Gdx2dDraw.drawCircle(_pixelPtr, _width, _height, _format, _blend, x, y, radius, color)

  def fillRect(x: Int, y: Int, width: Int, height: Int, color: Int): Unit =
    Gdx2dDraw.fillRect(_pixelPtr, _width, _height, _format, _blend, x, y, width, height, color)

  def fillCircle(x: Int, y: Int, radius: Int, color: Int): Unit =
    Gdx2dDraw.fillCircle(_pixelPtr, _width, _height, _format, _blend, x, y, radius, color)

  def fillTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int): Unit =
    Gdx2dDraw.fillTriangle(_pixelPtr, _width, _height, _format, _blend, x1, y1, x2, y2, x3, y3, color)

  def drawPixmap(src: Gdx2DPixmap, srcX: Int, srcY: Int, dstX: Int, dstY: Int, width: Int, height: Int): Unit =
    Gdx2dDraw.drawPixmap(
      src._pixelPtr,
      src._width,
      src._height,
      src._format,
      _pixelPtr,
      _width,
      _height,
      _format,
      _blend,
      _scale,
      srcX,
      srcY,
      width,
      height,
      dstX,
      dstY,
      width,
      height
    )

  def drawPixmap(
    src:       Gdx2DPixmap,
    srcX:      Int,
    srcY:      Int,
    srcWidth:  Int,
    srcHeight: Int,
    dstX:      Int,
    dstY:      Int,
    dstWidth:  Int,
    dstHeight: Int
  ): Unit =
    Gdx2dDraw.drawPixmap(
      src._pixelPtr,
      src._width,
      src._height,
      src._format,
      _pixelPtr,
      _width,
      _height,
      _format,
      _blend,
      _scale,
      srcX,
      srcY,
      srcWidth,
      srcHeight,
      dstX,
      dstY,
      dstWidth,
      dstHeight
    )

  def setBlend(blend: Int): Unit =
    _blend = blend

  def setScale(scale: Int): Unit =
    _scale = scale

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
