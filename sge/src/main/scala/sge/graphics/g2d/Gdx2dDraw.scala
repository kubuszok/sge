/*
 * Pure Scala implementation of LibGDX gdx2d.c pixel drawing operations.
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: gdx/jni/gdx2d/gdx2d.c
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: C static functions → Scala private methods; macros → inline helpers
 *   Convention: Pure Scala port of gdx2d.c pixel manipulation, no native code
 *   Idiom: ByteBuffer byte-level access for cross-platform correctness
 *   Audited: 2026-03-10
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 887
 * Covenant-baseline-methods: Gdx2dDraw,a,b,blend,blitBilinear,blitNearest,blitSameSize,boundY1a,boundY2a,bpp,bytesPerPixel,circlePoints,clear,col,colFormat,dbpp,ddFx,ddFy,drawCircle,drawLine,drawPixmap,drawRect,dstA,dstB,dstG,dstR,dx,dy,edgeAssign,edgeSwap,edges,f,fillCircle,fillRect,fillTriangle,g,getPixel,getRawPixel,getRawPixelAt,hline,i,lens,lu4,lu5,lu6,newPixelBuffer,offset,p,pixels,px,py,r,sbpp,setPixel,setRawPixel,setRawPixelAt,size,slope0,slope1,srcA,srcB,srcG,srcR,stepx,stepy,stride,sy,tmpEdge,tmpLen,toFormat,toRGBA8888,vline,x,x0,x1,x2,xRatio,y,y0,y1,y2,yRatio
 * Covenant-source-reference: gdx/jni/gdx2d/gdx2d.c
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g2d

import java.nio.ByteBuffer

/** Pure Scala drawing primitives for Gdx2DPixmap, ported from gdx2d.c.
  *
  * All operations work directly on a ByteBuffer containing pixel data. Pixel bytes are stored in big-endian component order (e.g. RGBA8888 stores [R, G, B, A] per pixel), matching the C
  * implementation's memory layout on little-endian platforms.
  */
private[g2d] object Gdx2dDraw {
  import Gdx2DPixmap.*

  // ─── Lookup tables for RGB565/RGBA4444 to RGBA8888 ─────────────────────

  private val lu4: Array[Int] = Array.tabulate(16)(i => (i / 15.0f * 255).toInt)
  private val lu5: Array[Int] = Array.tabulate(32)(i => (i / 31.0f * 255).toInt)
  private val lu6: Array[Int] = Array.tabulate(64)(i => (i / 63.0f * 255).toInt)

  // ─── Bytes per pixel ───────────────────────────────────────────────────

  def bytesPerPixel(format: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA                                                         => 1
      case GDX2D_FORMAT_LUMINANCE_ALPHA | GDX2D_FORMAT_RGB565 | GDX2D_FORMAT_RGBA4444 => 2
      case GDX2D_FORMAT_RGB888                                                        => 3
      case GDX2D_FORMAT_RGBA8888                                                      => 4
      case _                                                                          => 4
    }

  // ─── Format conversion ─────────────────────────────────────────────────

  /** Converts an RGBA8888 color to the target pixel format's raw representation. */
  def toFormat(format: Int, color: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA =>
        color & 0xff
      case GDX2D_FORMAT_LUMINANCE_ALPHA =>
        val r = (color & 0xff000000) >>> 24
        val g = (color & 0xff0000) >>> 16
        val b = (color & 0xff00) >>> 8
        val a = color & 0xff
        val l = ((0.2126f * r + 0.7152f * g + 0.0722f * b).toInt & 0xff) << 8
        (l & 0xffffff00) | a
      case GDX2D_FORMAT_RGB888 =>
        color >>> 8
      case GDX2D_FORMAT_RGBA8888 =>
        color
      case GDX2D_FORMAT_RGB565 =>
        val r = (((color & 0xff000000) >>> 27) << 11) & 0xf800
        val g = (((color & 0xff0000) >>> 18) << 5) & 0x7e0
        val b = ((color & 0xff00) >>> 11) & 0x1f
        r | g | b
      case GDX2D_FORMAT_RGBA4444 =>
        val r = (((color & 0xff000000) >>> 28) << 12) & 0xf000
        val g = (((color & 0xff0000) >>> 20) << 8) & 0xf00
        val b = (((color & 0xff00) >>> 12) << 4) & 0xf0
        val a = ((color & 0xff) >>> 4) & 0xf
        r | g | b | a
      case _ => 0
    }

  /** Converts a raw pixel value in the given format to RGBA8888. */
  def toRGBA8888(format: Int, color: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA =>
        (color & 0xff) | 0xffffff00
      case GDX2D_FORMAT_LUMINANCE_ALPHA =>
        ((color & 0xff00) << 16) | ((color & 0xff00) << 8) | (color & 0xffff)
      case GDX2D_FORMAT_RGB888 =>
        (color << 8) | 0x000000ff
      case GDX2D_FORMAT_RGBA8888 =>
        color
      case GDX2D_FORMAT_RGB565 =>
        val r = lu5((color & 0xf800) >>> 11) << 24
        val g = lu6((color & 0x7e0) >>> 5) << 16
        val b = lu5(color & 0x1f) << 8
        r | g | b | 0xff
      case GDX2D_FORMAT_RGBA4444 =>
        val r = lu4((color & 0xf000) >>> 12) << 24
        val g = lu4((color & 0xf00) >>> 8) << 16
        val b = lu4((color & 0xf0) >>> 4) << 8
        val a = lu4(color & 0xf)
        r | g | b | a
      case _ => 0
    }

  // ─── Alpha blending (SRC_OVER) ────────────────────────────────────────

  /** SRC_OVER alpha blend of two RGBA8888 colors. */
  def blend(src: Int, dst: Int): Int = {
    val srcA = src & 0xff
    if (srcA == 0) return dst
    val srcR = (src >>> 24) & 0xff
    val srcG = (src >>> 16) & 0xff
    val srcB = (src >>> 8) & 0xff

    var dstA = dst & 0xff
    val dstR = (dst >>> 24) & 0xff
    val dstG = (dst >>> 16) & 0xff
    val dstB = (dst >>> 8) & 0xff

    dstA -= (dstA * srcA) / 255
    val a = dstA + srcA
    if (a == 0) return 0
    val r = (dstR * dstA + srcR * srcA) / a
    val g = (dstG * dstA + srcG * srcA) / a
    val b = (dstB * dstA + srcB * srcA) / a
    (r << 24) | (g << 16) | (b << 8) | a
  }

  // ─── Pixel read/write (byte-level, endian-safe) ────────────────────────

  /** Reads a raw pixel value from the buffer at (x, y). */
  private def getRawPixel(buf: ByteBuffer, width: Int, format: Int, bpp: Int, x: Int, y: Int): Int = {
    val offset = (y * width + x) * bpp
    format match {
      case GDX2D_FORMAT_ALPHA =>
        buf.get(offset) & 0xff
      case GDX2D_FORMAT_LUMINANCE_ALPHA | GDX2D_FORMAT_RGB565 | GDX2D_FORMAT_RGBA4444 =>
        ((buf.get(offset) & 0xff) << 8) | (buf.get(offset + 1) & 0xff)
      case GDX2D_FORMAT_RGB888 =>
        ((buf.get(offset) & 0xff) << 16) | ((buf.get(offset + 1) & 0xff) << 8) | (buf.get(offset + 2) & 0xff)
      case GDX2D_FORMAT_RGBA8888 =>
        ((buf.get(offset) & 0xff) << 24) | ((buf.get(offset + 1) & 0xff) << 16) |
          ((buf.get(offset + 2) & 0xff) << 8) | (buf.get(offset + 3) & 0xff)
      case _ => 0
    }
  }

  /** Writes a raw pixel value to the buffer at (x, y) with bounds checking. */
  private def setRawPixel(
    buf:    ByteBuffer,
    width:  Int,
    height: Int,
    format: Int,
    bpp:    Int,
    x:      Int,
    y:      Int,
    color:  Int
  ): Unit = {
    if (x < 0 || y < 0 || x >= width || y >= height) return
    val offset = (y * width + x) * bpp
    setRawPixelAt(buf, format, bpp, offset, color)
  }

  /** Writes a raw pixel value at a given byte offset (no bounds check). */
  private def setRawPixelAt(buf: ByteBuffer, format: Int, bpp: Int, offset: Int, color: Int): Unit =
    format match {
      case GDX2D_FORMAT_ALPHA =>
        buf.put(offset, (color & 0xff).toByte)
      case GDX2D_FORMAT_LUMINANCE_ALPHA | GDX2D_FORMAT_RGB565 | GDX2D_FORMAT_RGBA4444 =>
        buf.put(offset, ((color >>> 8) & 0xff).toByte)
        buf.put(offset + 1, (color & 0xff).toByte)
      case GDX2D_FORMAT_RGB888 =>
        buf.put(offset, ((color >>> 16) & 0xff).toByte)
        buf.put(offset + 1, ((color >>> 8) & 0xff).toByte)
        buf.put(offset + 2, (color & 0xff).toByte)
      case GDX2D_FORMAT_RGBA8888 =>
        buf.put(offset, ((color >>> 24) & 0xff).toByte)
        buf.put(offset + 1, ((color >>> 16) & 0xff).toByte)
        buf.put(offset + 2, ((color >>> 8) & 0xff).toByte)
        buf.put(offset + 3, (color & 0xff).toByte)
      case _ => ()
    }

  /** Reads a raw pixel at a given byte offset (no bounds check). */
  private def getRawPixelAt(buf: ByteBuffer, format: Int, bpp: Int, offset: Int): Int =
    format match {
      case GDX2D_FORMAT_ALPHA =>
        buf.get(offset) & 0xff
      case GDX2D_FORMAT_LUMINANCE_ALPHA | GDX2D_FORMAT_RGB565 | GDX2D_FORMAT_RGBA4444 =>
        ((buf.get(offset) & 0xff) << 8) | (buf.get(offset + 1) & 0xff)
      case GDX2D_FORMAT_RGB888 =>
        ((buf.get(offset) & 0xff) << 16) | ((buf.get(offset + 1) & 0xff) << 8) | (buf.get(offset + 2) & 0xff)
      case GDX2D_FORMAT_RGBA8888 =>
        ((buf.get(offset) & 0xff) << 24) | ((buf.get(offset + 1) & 0xff) << 16) |
          ((buf.get(offset + 2) & 0xff) << 8) | (buf.get(offset + 3) & 0xff)
      case _ => 0
    }

  // ─── Public pixel operations ───────────────────────────────────────────

  /** Gets a pixel as RGBA8888, returning 0 for out-of-bounds coordinates. */
  def getPixel(buf: ByteBuffer, width: Int, height: Int, format: Int, x: Int, y: Int): Int = {
    if (x < 0 || y < 0 || x >= width || y >= height) return 0
    val bpp = bytesPerPixel(format)
    toRGBA8888(format, getRawPixel(buf, width, format, bpp, x, y))
  }

  /** Sets a pixel from an RGBA8888 color, with optional blending. */
  def setPixel(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x:         Int,
    y:         Int,
    color:     Int
  ): Unit = {
    if (x < 0 || y < 0 || x >= width || y >= height) return
    val bpp    = bytesPerPixel(format)
    val offset = (y * width + x) * bpp
    val col    = if (blendMode != GDX2D_BLEND_NONE) {
      val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
      toFormat(format, blend(color, dst))
    } else {
      toFormat(format, color)
    }
    setRawPixelAt(buf, format, bpp, offset, col)
  }

  // ─── Clear ─────────────────────────────────────────────────────────────

  /** Clears the entire pixmap with the given RGBA8888 color. */
  def clear(buf: ByteBuffer, width: Int, height: Int, format: Int, color: Int): Unit = {
    val col    = toFormat(format, color)
    val bpp    = bytesPerPixel(format)
    val pixels = width * height
    var offset = 0
    var i      = 0
    while (i < pixels) {
      setRawPixelAt(buf, format, bpp, offset, col)
      offset += bpp
      i += 1
    }
  }

  // ─── Horizontal/vertical line helpers ──────────────────────────────────

  private def hline(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x1In:      Int,
    x2In:      Int,
    y:         Int,
    color:     Int
  ): Unit = {
    if (y < 0 || y >= height) return
    var x1 = x1In
    var x2 = x2In
    if (x1 > x2) { val tmp = x1; x1 = x2; x2 = tmp }
    if (x1 >= width) return
    if (x2 < 0) return
    if (x1 < 0) x1 = 0
    if (x2 >= width) x2 = width - 1

    val bpp       = bytesPerPixel(format)
    val colFormat = toFormat(format, color)
    var offset    = (x1 + y * width) * bpp
    var x         = x1
    while (x <= x2) {
      if (blendMode != GDX2D_BLEND_NONE) {
        val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
        setRawPixelAt(buf, format, bpp, offset, toFormat(format, blend(color, dst)))
      } else {
        setRawPixelAt(buf, format, bpp, offset, colFormat)
      }
      offset += bpp
      x += 1
    }
  }

  @SuppressWarnings(Array("unused"))
  private def vline(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    y1In:      Int,
    y2In:      Int,
    x:         Int,
    color:     Int
  ): Unit = {
    if (x < 0 || x >= width) return
    var y1 = y1In
    var y2 = y2In
    if (y1 > y2) { val tmp = y1; y1 = y2; y2 = tmp }
    if (y1 >= height) return
    if (y2 < 0) return
    if (y1 < 0) y1 = 0
    if (y2 >= height) y2 = height - 1

    val bpp       = bytesPerPixel(format)
    val stride    = bpp * width
    val colFormat = toFormat(format, color)
    var offset    = (x + y1 * width) * bpp
    var y         = y1
    while (y <= y2) {
      if (blendMode != GDX2D_BLEND_NONE) {
        val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
        setRawPixelAt(buf, format, bpp, offset, toFormat(format, blend(color, dst)))
      } else {
        setRawPixelAt(buf, format, bpp, offset, colFormat)
      }
      offset += stride
      y += 1
    }
  }

  // ─── Draw line (Bresenham) ─────────────────────────────────────────────

  def drawLine(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x0In:      Int,
    y0In:      Int,
    x1:        Int,
    y1:        Int,
    color:     Int
  ): Unit = {
    var x0        = x0In
    var y0        = y0In
    var dy        = y1 - y0
    var dx        = x1 - x0
    val bpp       = bytesPerPixel(format)
    val colFormat = toFormat(format, color)
    var stepx     = 0
    var stepy     = 0

    if (dy < 0) { dy = -dy; stepy = -1 }
    else { stepy = 1 }
    if (dx < 0) { dx = -dx; stepx = -1 }
    else { stepx = 1 }
    dy <<= 1
    dx <<= 1

    // Set first pixel
    if (x0 >= 0 && y0 >= 0 && x0 < width && y0 < height) {
      val offset = (x0 + y0 * width) * bpp
      if (blendMode != GDX2D_BLEND_NONE) {
        val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
        setRawPixelAt(buf, format, bpp, offset, toFormat(format, blend(color, dst)))
      } else {
        setRawPixelAt(buf, format, bpp, offset, colFormat)
      }
    }

    if (dx > dy) {
      var fraction = dy - (dx >> 1)
      while (x0 != x1) {
        if (fraction >= 0) {
          y0 += stepy
          fraction -= dx
        }
        x0 += stepx
        fraction += dy
        if (x0 >= 0 && y0 >= 0 && x0 < width && y0 < height) {
          val offset = (x0 + y0 * width) * bpp
          if (blendMode != GDX2D_BLEND_NONE) {
            val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
            setRawPixelAt(buf, format, bpp, offset, toFormat(format, blend(color, dst)))
          } else {
            setRawPixelAt(buf, format, bpp, offset, colFormat)
          }
        }
      }
    } else {
      var fraction = dx - (dy >> 1)
      while (y0 != y1) {
        if (fraction >= 0) {
          x0 += stepx
          fraction -= dy
        }
        y0 += stepy
        fraction += dx
        if (x0 >= 0 && y0 >= 0 && x0 < width && y0 < height) {
          val offset = (x0 + y0 * width) * bpp
          if (blendMode != GDX2D_BLEND_NONE) {
            val dst = toRGBA8888(format, getRawPixelAt(buf, format, bpp, offset))
            setRawPixelAt(buf, format, bpp, offset, toFormat(format, blend(color, dst)))
          } else {
            setRawPixelAt(buf, format, bpp, offset, colFormat)
          }
        }
      }
    }
  }

  // ─── Draw rect (outline) ──────────────────────────────────────────────

  def drawRect(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x:         Int,
    y:         Int,
    w:         Int,
    h:         Int,
    color:     Int
  ): Unit = {
    hline(buf, width, height, format, blendMode, x, x + w - 1, y, color)
    hline(buf, width, height, format, blendMode, x, x + w - 1, y + h - 1, color)
    vline(buf, width, height, format, blendMode, y, y + h - 1, x, color)
    vline(buf, width, height, format, blendMode, y, y + h - 1, x + w - 1, color)
  }

  // ─── Draw circle (outline, midpoint algorithm) ─────────────────────────

  def drawCircle(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    cx:        Int,
    cy:        Int,
    radius:    Int,
    color:     Int
  ): Unit = {
    val bpp       = bytesPerPixel(format)
    val colFormat = toFormat(format, color)
    var px        = 0
    var py        = radius
    var p         = (5 - radius * 4) / 4

    circlePoints(buf, width, height, format, bpp, colFormat, cx, cy, px, py)
    while (px < py) {
      px += 1
      if (p < 0) {
        p += 2 * px + 1
      } else {
        py -= 1
        p += 2 * (px - py) + 1
      }
      circlePoints(buf, width, height, format, bpp, colFormat, cx, cy, px, py)
    }
  }

  private def circlePoints(
    buf:    ByteBuffer,
    width:  Int,
    height: Int,
    format: Int,
    bpp:    Int,
    col:    Int,
    cx:     Int,
    cy:     Int,
    x:      Int,
    y:      Int
  ): Unit =
    if (x == 0) {
      setRawPixel(buf, width, height, format, bpp, cx, cy + y, col)
      setRawPixel(buf, width, height, format, bpp, cx, cy - y, col)
      setRawPixel(buf, width, height, format, bpp, cx + y, cy, col)
      setRawPixel(buf, width, height, format, bpp, cx - y, cy, col)
    } else if (x == y) {
      setRawPixel(buf, width, height, format, bpp, cx + x, cy + y, col)
      setRawPixel(buf, width, height, format, bpp, cx - x, cy + y, col)
      setRawPixel(buf, width, height, format, bpp, cx + x, cy - y, col)
      setRawPixel(buf, width, height, format, bpp, cx - x, cy - y, col)
    } else if (x < y) {
      setRawPixel(buf, width, height, format, bpp, cx + x, cy + y, col)
      setRawPixel(buf, width, height, format, bpp, cx - x, cy + y, col)
      setRawPixel(buf, width, height, format, bpp, cx + x, cy - y, col)
      setRawPixel(buf, width, height, format, bpp, cx - x, cy - y, col)
      setRawPixel(buf, width, height, format, bpp, cx + y, cy + x, col)
      setRawPixel(buf, width, height, format, bpp, cx - y, cy + x, col)
      setRawPixel(buf, width, height, format, bpp, cx + y, cy - x, col)
      setRawPixel(buf, width, height, format, bpp, cx - y, cy - x, col)
    }

  // ─── Fill rect ─────────────────────────────────────────────────────────

  def fillRect(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    xIn:       Int,
    yIn:       Int,
    w:         Int,
    h:         Int,
    color:     Int
  ): Unit = {
    var x  = xIn
    var y  = yIn
    var x2 = x + w - 1
    var y2 = y + h - 1

    if (x >= width) return
    if (y >= height) return
    if (x2 < 0) return
    if (y2 < 0) return

    if (x < 0) x = 0
    if (y < 0) y = 0
    if (x2 >= width) x2 = width - 1
    if (y2 >= height) y2 = height - 1

    while (y <= y2) {
      hline(buf, width, height, format, blendMode, x, x2, y, color)
      y += 1
    }
  }

  // ─── Fill circle ───────────────────────────────────────────────────────

  def fillCircle(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x0:        Int,
    y0:        Int,
    radius:    Int,
    color:     Int
  ): Unit = {
    val r    = radius.toInt
    var f    = 1 - r
    var ddFx = 1
    var ddFy = -2 * r
    var px   = 0
    var py   = r

    hline(buf, width, height, format, blendMode, x0, x0, y0 + r, color)
    hline(buf, width, height, format, blendMode, x0, x0, y0 - r, color)
    hline(buf, width, height, format, blendMode, x0 - r, x0 + r, y0, color)

    while (px < py) {
      if (f >= 0) {
        py -= 1
        ddFy += 2
        f += ddFy
      }
      px += 1
      ddFx += 2
      f += ddFx
      hline(buf, width, height, format, blendMode, x0 - px, x0 + px, y0 + py, color)
      hline(buf, width, height, format, blendMode, x0 - px, x0 + px, y0 - py, color)
      hline(buf, width, height, format, blendMode, x0 - py, x0 + py, y0 + px, color)
      hline(buf, width, height, format, blendMode, x0 - py, x0 + py, y0 - px, color)
    }
  }

  // ─── Fill triangle (scanline) ──────────────────────────────────────────

  def fillTriangle(
    buf:       ByteBuffer,
    width:     Int,
    height:    Int,
    format:    Int,
    blendMode: Int,
    x1:        Int,
    y1:        Int,
    x2:        Int,
    y2:        Int,
    x3:        Int,
    y3:        Int,
    color:     Int
  ): Unit = {
    // Colinear check
    if ((x2 - x1).toLong * (y3 - y1) == (x3 - x1).toLong * (y2 - y1)) return

    // Edge structure: (x1, y1) → (x2, y2) sorted so y1 <= y2
    val edges = Array.ofDim[Int](3, 4) // [edge][x1, y1, x2, y2]
    edgeAssign(edges(0), x1, y1, x2, y2)
    edgeAssign(edges(1), x1, y1, x3, y3)
    edgeAssign(edges(2), x2, y2, x3, y3)

    // Sort by descending y-length
    val lens = Array(
      edges(0)(3) - edges(0)(1),
      edges(1)(3) - edges(1)(1),
      edges(2)(3) - edges(2)(1)
    )
    if (lens(1) >= lens(0) && lens(1) >= lens(2)) {
      edgeSwap(edges, lens, 0, 1)
    } else if (lens(2) >= lens(0) && lens(2) >= lens(1)) {
      edgeSwap(edges, lens, 0, 2)
    }
    if (lens(2) > lens(1)) {
      edgeSwap(edges, lens, 1, 2)
    }

    // First half: edges 0 and 1
    val slope0 = (edges(0)(0) - edges(0)(2)).toFloat / (edges(0)(3) - edges(0)(1)).toFloat
    val slope1 = (edges(1)(0) - edges(1)(2)).toFloat / (edges(1)(3) - edges(1)(1)).toFloat

    val boundY1a = scala.math.max(edges(1)(1), 0)
    val boundY2a = scala.math.min(edges(1)(3), height - 1)
    var y        = boundY1a
    while (y <= boundY2a) {
      val cx1 = (edges(0)(2) + slope0 * (edges(0)(3) - y) + 0.5f).toInt
      val cx2 = (edges(1)(2) + slope1 * (edges(1)(3) - y) + 0.5f).toInt
      hline(buf, width, height, format, blendMode, cx1, cx2, y, color)
      y += 1
    }

    // Second half: edges 0 and 2
    if (edges(2)(3) - edges(2)(1) > 0) {
      val slope2 = (edges(2)(0) - edges(2)(2)).toFloat / (edges(2)(3) - edges(2)(1)).toFloat

      val boundY1b = scala.math.max(edges(2)(1), 0)
      val boundY2b = scala.math.min(edges(2)(3), height - 1)
      y = boundY1b
      while (y <= boundY2b) {
        val cx1 = (edges(0)(2) + slope0 * (edges(0)(3) - y) + 0.5f).toInt
        val cx2 = (edges(2)(2) + slope2 * (edges(2)(3) - y) + 0.5f).toInt
        hline(buf, width, height, format, blendMode, cx1, cx2, y, color)
        y += 1
      }
    }
  }

  private def edgeAssign(edge: Array[Int], ax: Int, ay: Int, bx: Int, by: Int): Unit =
    if (by > ay) { edge(0) = ax; edge(1) = ay; edge(2) = bx; edge(3) = by }
    else { edge(0) = bx; edge(1) = by; edge(2) = ax; edge(3) = ay }

  private def edgeSwap(edges: Array[Array[Int]], lens: Array[Int], a: Int, b: Int): Unit = {
    val tmpEdge = edges(a); edges(a) = edges(b); edges(b) = tmpEdge
    val tmpLen  = lens(a); lens(a) = lens(b); lens(b) = tmpLen
  }

  // ─── Draw pixmap (blit) ───────────────────────────────────────────────

  /** Blits src pixmap onto dst pixmap with optional scaling and blending. */
  def drawPixmap(
    srcBuf:    ByteBuffer,
    srcWidth:  Int,
    srcHeight: Int,
    srcFormat: Int,
    dstBuf:    ByteBuffer,
    dstWidth:  Int,
    dstHeight: Int,
    dstFormat: Int,
    dstBlend:  Int,
    dstScale:  Int,
    srcX:      Int,
    srcY:      Int,
    srcW:      Int,
    srcH:      Int,
    dstX:      Int,
    dstY:      Int,
    dstW:      Int,
    dstH:      Int
  ): Unit =
    if (srcW == dstW && srcH == dstH) {
      blitSameSize(srcBuf, srcWidth, srcHeight, srcFormat, dstBuf, dstWidth, dstHeight, dstFormat, dstBlend, srcX, srcY, dstX, dstY, srcW, srcH)
    } else if (dstScale == GDX2D_SCALE_LINEAR) {
      blitNearest(
        srcBuf,
        srcWidth,
        srcHeight,
        srcFormat,
        dstBuf,
        dstWidth,
        dstHeight,
        dstFormat,
        dstBlend,
        srcX,
        srcY,
        srcW,
        srcH,
        dstX,
        dstY,
        dstW,
        dstH
      )
    } else {
      blitBilinear(
        srcBuf,
        srcWidth,
        srcHeight,
        srcFormat,
        dstBuf,
        dstWidth,
        dstHeight,
        dstFormat,
        dstBlend,
        srcX,
        srcY,
        srcW,
        srcH,
        dstX,
        dstY,
        dstW,
        dstH
      )
    }

  private def blitSameSize(
    srcBuf:    ByteBuffer,
    srcWidth:  Int,
    srcHeight: Int,
    srcFormat: Int,
    dstBuf:    ByteBuffer,
    dstWidth:  Int,
    dstHeight: Int,
    dstFormat: Int,
    dstBlend:  Int,
    srcX:      Int,
    srcY:      Int,
    dstX:      Int,
    dstY:      Int,
    w:         Int,
    h:         Int
  ): Unit = {
    val sbpp = bytesPerPixel(srcFormat)
    val dbpp = bytesPerPixel(dstFormat)

    var sy = srcY
    var dy = dstY
    while (sy < srcY + h) {
      if (sy >= 0 && dy >= 0 && sy < srcHeight && dy < dstHeight) {
        var sx = srcX
        var dx = dstX
        while (sx < srcX + w) {
          if (sx >= 0 && dx >= 0 && sx < srcWidth && dx < dstWidth) {
            val srcOff = (sx + sy * srcWidth) * sbpp
            val dstOff = (dx + dy * dstWidth) * dbpp
            var srcCol = toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff))

            if (dstBlend != GDX2D_BLEND_NONE) {
              val dstCol = toRGBA8888(dstFormat, getRawPixelAt(dstBuf, dstFormat, dbpp, dstOff))
              srcCol = toFormat(dstFormat, blend(srcCol, dstCol))
            } else {
              srcCol = toFormat(dstFormat, srcCol)
            }
            setRawPixelAt(dstBuf, dstFormat, dbpp, dstOff, srcCol)
          }
          sx += 1
          dx += 1
        }
      }
      sy += 1
      dy += 1
    }
  }

  private def blitNearest(
    srcBuf:    ByteBuffer,
    srcWidth:  Int,
    srcHeight: Int,
    srcFormat: Int,
    dstBuf:    ByteBuffer,
    dstWidth:  Int,
    dstHeight: Int,
    dstFormat: Int,
    dstBlend:  Int,
    srcX:      Int,
    srcY:      Int,
    srcW:      Int,
    srcH:      Int,
    dstX:      Int,
    dstY:      Int,
    dstW:      Int,
    dstH:      Int
  ): Unit = {
    val sbpp   = bytesPerPixel(srcFormat)
    val dbpp   = bytesPerPixel(dstFormat)
    val xRatio = ((srcW.toLong << 16) / dstW + 1).toInt
    val yRatio = ((srcH.toLong << 16) / dstH + 1).toInt

    var i = 0
    while (i < dstH) {
      val sy = ((i * yRatio) >> 16) + srcY
      val dy = i + dstY
      if (sy >= 0 && dy >= 0 && sy < srcHeight && dy < dstHeight) {
        var j = 0
        while (j < dstW) {
          val sx = ((j * xRatio) >> 16) + srcX
          val dx = j + dstX
          if (sx >= 0 && dx >= 0 && sx < srcWidth && dx < dstWidth) {
            val srcOff = (sx + sy * srcWidth) * sbpp
            val dstOff = (dx + dy * dstWidth) * dbpp
            var srcCol = toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff))
            if (dstBlend != GDX2D_BLEND_NONE) {
              val dstCol = toRGBA8888(dstFormat, getRawPixelAt(dstBuf, dstFormat, dbpp, dstOff))
              srcCol = toFormat(dstFormat, blend(srcCol, dstCol))
            } else {
              srcCol = toFormat(dstFormat, srcCol)
            }
            setRawPixelAt(dstBuf, dstFormat, dbpp, dstOff, srcCol)
          }
          j += 1
        }
      }
      i += 1
    }
  }

  private def blitBilinear(
    srcBuf:    ByteBuffer,
    srcWidth:  Int,
    srcHeight: Int,
    srcFormat: Int,
    dstBuf:    ByteBuffer,
    dstWidth:  Int,
    dstHeight: Int,
    dstFormat: Int,
    dstBlend:  Int,
    srcX:      Int,
    srcY:      Int,
    srcW:      Int,
    srcH:      Int,
    dstX:      Int,
    dstY:      Int,
    dstW:      Int,
    dstH:      Int
  ): Unit = {
    val sbpp   = bytesPerPixel(srcFormat)
    val dbpp   = bytesPerPixel(dstFormat)
    val xRatio = (srcW - 1).toFloat / dstW
    val yRatio = (srcH - 1).toFloat / dstH

    var i = 0
    while (i < dstH) {
      val sy    = (i * yRatio).toInt + srcY
      val dy    = i + dstY
      val yDiff = (yRatio * i + srcY) - sy
      if (sy >= 0 && dy >= 0 && sy < srcHeight && dy < dstHeight) {
        var j = 0
        while (j < dstW) {
          val sx    = (j * xRatio).toInt + srcX
          val dx    = j + dstX
          val xDiff = (xRatio * j + srcX) - sx
          if (sx >= 0 && dx >= 0 && sx < srcWidth && dx < dstWidth) {
            val srcOff = (sx + sy * srcWidth) * sbpp
            val c1     = toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff))
            val c2     = if (sx + 1 < srcW) toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff + sbpp)) else c1
            val c3     = if (sy + 1 < srcH) toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff + srcWidth * sbpp)) else c1
            val c4     = if (sx + 1 < srcW && sy + 1 < srcH) toRGBA8888(srcFormat, getRawPixelAt(srcBuf, srcFormat, sbpp, srcOff + srcWidth * sbpp + sbpp)) else c1

            val ta = (1 - xDiff) * (1 - yDiff)
            val tb = xDiff * (1 - yDiff)
            val tc = (1 - xDiff) * yDiff
            val td = xDiff * yDiff

            val r = (((c1 >>> 24) & 0xff) * ta + ((c2 >>> 24) & 0xff) * tb + ((c3 >>> 24) & 0xff) * tc + ((c4 >>> 24) & 0xff) * td).toInt & 0xff
            val g = (((c1 >>> 16) & 0xff) * ta + ((c2 >>> 16) & 0xff) * tb + ((c3 >>> 16) & 0xff) * tc + ((c4 >>> 16) & 0xff) * td).toInt & 0xff
            val b = (((c1 >>> 8) & 0xff) * ta + ((c2 >>> 8) & 0xff) * tb + ((c3 >>> 8) & 0xff) * tc + ((c4 >>> 8) & 0xff) * td).toInt & 0xff
            val a = ((c1 & 0xff) * ta + (c2 & 0xff) * tb + (c3 & 0xff) * tc + (c4 & 0xff) * td).toInt & 0xff

            var srcCol = (r << 24) | (g << 16) | (b << 8) | a
            val dstOff = (dx + dy * dstWidth) * dbpp
            if (dstBlend != GDX2D_BLEND_NONE) {
              val dstCol = toRGBA8888(dstFormat, getRawPixelAt(dstBuf, dstFormat, dbpp, dstOff))
              srcCol = toFormat(dstFormat, blend(srcCol, dstCol))
            } else {
              srcCol = toFormat(dstFormat, srcCol)
            }
            setRawPixelAt(dstBuf, dstFormat, dbpp, dstOff, srcCol)
          }
          j += 1
        }
      }
      i += 1
    }
  }

  // ─── Pixmap allocation helper ──────────────────────────────────────────

  /** Creates a new direct ByteBuffer sized for the given dimensions and format. */
  def newPixelBuffer(width: Int, height: Int, format: Int): ByteBuffer = {
    val size = width * height * bytesPerPixel(format)
    ByteBuffer.allocateDirect(size)
  }
}
