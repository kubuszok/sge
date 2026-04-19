/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/freetype/FreeType.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError; Disposable -> AutoCloseable; SharedLibraryLoader -> FreetypePlatform
 *   Convention: JNI native methods -> FreetypeOps trait; inner static classes -> top-level classes in companion;
 *     Pointer base class -> Long handle field; getters -> Scala property accessors
 *   Idiom: boundary/break (0 return), Nullable (0 null), split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 513
 * Covenant-baseline-methods: Bitmap,BitmapGlyphBitmap,FT_ENCODING_ADOBE_CUSTOM,FT_ENCODING_ADOBE_EXPERT,FT_ENCODING_ADOBE_LATIN_1,FT_ENCODING_ADOBE_STANDARD,FT_ENCODING_APPLE_ROMAN,FT_ENCODING_BIG5,FT_ENCODING_GB2312,FT_ENCODING_JOHAB,FT_ENCODING_MS_SYMBOL,FT_ENCODING_NONE,FT_ENCODING_OLD_LATIN_2,FT_ENCODING_SJIS,FT_ENCODING_UNICODE,FT_ENCODING_WANSUNG,FT_FACE_FLAG_CID_KEYED,FT_FACE_FLAG_EXTERNAL_STREAM,FT_FACE_FLAG_FAST_GLYPHS,FT_FACE_FLAG_FIXED_SIZES,FT_FACE_FLAG_FIXED_WIDTH,FT_FACE_FLAG_GLYPH_NAMES,FT_FACE_FLAG_HINTER,FT_FACE_FLAG_HORIZONTAL,FT_FACE_FLAG_KERNING,FT_FACE_FLAG_MULTIPLE_MASTERS,FT_FACE_FLAG_SCALABLE,FT_FACE_FLAG_SFNT,FT_FACE_FLAG_TRICKY,FT_FACE_FLAG_VERTICAL,FT_KERNING_DEFAULT,FT_KERNING_UNFITTED,FT_KERNING_UNSCALED,FT_LOAD_CROP_BITMAP,FT_LOAD_DEFAULT,FT_LOAD_FORCE_AUTOHINT,FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH,FT_LOAD_IGNORE_TRANSFORM,FT_LOAD_LINEAR_DESIGN,FT_LOAD_MONOCHROME,FT_LOAD_NO_AUTOHINT,FT_LOAD_NO_BITMAP,FT_LOAD_NO_HINTING,FT_LOAD_NO_RECURSE,FT_LOAD_NO_SCALE,FT_LOAD_PEDANTIC,FT_LOAD_RENDER,FT_LOAD_TARGET_LCD,FT_LOAD_TARGET_LCD_V,FT_LOAD_TARGET_LIGHT,FT_LOAD_TARGET_MONO,FT_LOAD_TARGET_NORMAL,FT_LOAD_VERTICAL_LAYOUT,FT_PIXEL_MODE_GRAY,FT_PIXEL_MODE_GRAY2,FT_PIXEL_MODE_GRAY4,FT_PIXEL_MODE_LCD,FT_PIXEL_MODE_LCD_V,FT_PIXEL_MODE_MONO,FT_PIXEL_MODE_NONE,FT_RENDER_MODE_LCD,FT_RENDER_MODE_LCD_V,FT_RENDER_MODE_LIGHT,FT_RENDER_MODE_MAX,FT_RENDER_MODE_MONO,FT_RENDER_MODE_NORMAL,FT_STROKER_LINECAP_BUTT,FT_STROKER_LINECAP_ROUND,FT_STROKER_LINECAP_SQUARE,FT_STROKER_LINEJOIN_BEVEL,FT_STROKER_LINEJOIN_MITER,FT_STROKER_LINEJOIN_MITER_FIXED,FT_STROKER_LINEJOIN_MITER_VARIABLE,FT_STROKER_LINEJOIN_ROUND,FT_STYLE_FLAG_BOLD,FT_STYLE_FLAG_ITALIC,Face,FreeType,Glyph,GlyphMetrics,GlyphSlot,Library,Size,SizeMetrics,Stroker,address,advanceX,advanceY,ascender,bitmapLeft,bitmapTop,close,createStroker,descender,encode,faceFlags,format,getAdvanceX,getAdvanceY,getAscender,getBitmap,getBitmapLeft,getBitmapTop,getBuffer,getCharIndex,getDescender,getFormat,getGlyph,getHeight,getHoriAdvance,getHoriBearingX,getHoriBearingY,getKerning,getLastErrorCode,getLeft,getLinearHoriAdvance,getMaxAdvance,getMetrics,getNumGray,getPitch,getPixelMode,getPixmap,getRows,getSize,getTop,getWidth,getXScale,getXppem,getYppem,getYscale,hasKerning,height,horiAdvance,horiBearingX,horiBearingY,info,initFreeType,linearHoriAdvance,loadChar,loadGlyph,maxAdvance,maxAdvanceHeight,maxAdvanceWidth,newMemoryFace,numGlyphs,numGray,ops,pitch,pixelMode,renderGlyph,rendered,rows,selectSize,set,setCharSize,setPixelSizes,strokeBorder,styleFlags,toBitmap,toInt,underlinePosition,underlineThickness,width,xPpem,xScale,yPpem,yScale
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/freetype/FreeType.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g2d
package freetype

import sge.Pixels
import sge.graphics.{ Color, Pixmap }
import sge.graphics.Pixmap.{ Blending, Format }
import sge.platform.FreetypePlatform
import sge.utils.SgeError

/** FreeType font rasterization wrapper.
  *
  * Provides typed Scala wrappers around opaque FreeType handles (Library, Face, GlyphSlot, Glyph, Stroker, etc). All native operations are delegated to `FreetypePlatform.ops`.
  */
object FreeType {

  private def ops = FreetypePlatform.ops

  // ─── Pixel modes ────────────────────────────────────────────────────────

  val FT_PIXEL_MODE_NONE:  Int = 0
  val FT_PIXEL_MODE_MONO:  Int = 1
  val FT_PIXEL_MODE_GRAY:  Int = 2
  val FT_PIXEL_MODE_GRAY2: Int = 3
  val FT_PIXEL_MODE_GRAY4: Int = 4
  val FT_PIXEL_MODE_LCD:   Int = 5
  val FT_PIXEL_MODE_LCD_V: Int = 6

  // ─── Encodings ──────────────────────────────────────────────────────────

  private def encode(a: Char, b: Char, c: Char, d: Char): Int =
    (a << 24) | (b << 16) | (c << 8) | d

  val FT_ENCODING_NONE:           Int = 0
  val FT_ENCODING_MS_SYMBOL:      Int = encode('s', 'y', 'm', 'b')
  val FT_ENCODING_UNICODE:        Int = encode('u', 'n', 'i', 'c')
  val FT_ENCODING_SJIS:           Int = encode('s', 'j', 'i', 's')
  val FT_ENCODING_GB2312:         Int = encode('g', 'b', ' ', ' ')
  val FT_ENCODING_BIG5:           Int = encode('b', 'i', 'g', '5')
  val FT_ENCODING_WANSUNG:        Int = encode('w', 'a', 'n', 's')
  val FT_ENCODING_JOHAB:          Int = encode('j', 'o', 'h', 'a')
  val FT_ENCODING_ADOBE_STANDARD: Int = encode('A', 'D', 'O', 'B')
  val FT_ENCODING_ADOBE_EXPERT:   Int = encode('A', 'D', 'B', 'E')
  val FT_ENCODING_ADOBE_CUSTOM:   Int = encode('A', 'D', 'B', 'C')
  val FT_ENCODING_ADOBE_LATIN_1:  Int = encode('l', 'a', 't', '1')
  val FT_ENCODING_OLD_LATIN_2:    Int = encode('l', 'a', 't', '2')
  val FT_ENCODING_APPLE_ROMAN:    Int = encode('a', 'r', 'm', 'n')

  // ─── Face flags ─────────────────────────────────────────────────────────

  val FT_FACE_FLAG_SCALABLE:         Int = 1 << 0
  val FT_FACE_FLAG_FIXED_SIZES:      Int = 1 << 1
  val FT_FACE_FLAG_FIXED_WIDTH:      Int = 1 << 2
  val FT_FACE_FLAG_SFNT:             Int = 1 << 3
  val FT_FACE_FLAG_HORIZONTAL:       Int = 1 << 4
  val FT_FACE_FLAG_VERTICAL:         Int = 1 << 5
  val FT_FACE_FLAG_KERNING:          Int = 1 << 6
  val FT_FACE_FLAG_FAST_GLYPHS:      Int = 1 << 7
  val FT_FACE_FLAG_MULTIPLE_MASTERS: Int = 1 << 8
  val FT_FACE_FLAG_GLYPH_NAMES:      Int = 1 << 9
  val FT_FACE_FLAG_EXTERNAL_STREAM:  Int = 1 << 10
  val FT_FACE_FLAG_HINTER:           Int = 1 << 11
  val FT_FACE_FLAG_CID_KEYED:        Int = 1 << 12
  val FT_FACE_FLAG_TRICKY:           Int = 1 << 13

  // ─── Style flags ────────────────────────────────────────────────────────

  val FT_STYLE_FLAG_ITALIC: Int = 1 << 0
  val FT_STYLE_FLAG_BOLD:   Int = 1 << 1

  // ─── Load flags ─────────────────────────────────────────────────────────

  val FT_LOAD_DEFAULT:                     Int = 0x0
  val FT_LOAD_NO_SCALE:                    Int = 0x1
  val FT_LOAD_NO_HINTING:                  Int = 0x2
  val FT_LOAD_RENDER:                      Int = 0x4
  val FT_LOAD_NO_BITMAP:                   Int = 0x8
  val FT_LOAD_VERTICAL_LAYOUT:             Int = 0x10
  val FT_LOAD_FORCE_AUTOHINT:              Int = 0x20
  val FT_LOAD_CROP_BITMAP:                 Int = 0x40
  val FT_LOAD_PEDANTIC:                    Int = 0x80
  val FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH: Int = 0x200
  val FT_LOAD_NO_RECURSE:                  Int = 0x400
  val FT_LOAD_IGNORE_TRANSFORM:            Int = 0x800
  val FT_LOAD_MONOCHROME:                  Int = 0x1000
  val FT_LOAD_LINEAR_DESIGN:               Int = 0x2000
  val FT_LOAD_NO_AUTOHINT:                 Int = 0x8000

  // ─── Load targets ──────────────────────────────────────────────────────

  val FT_LOAD_TARGET_NORMAL: Int = 0x0
  val FT_LOAD_TARGET_LIGHT:  Int = 0x10000
  val FT_LOAD_TARGET_MONO:   Int = 0x20000
  val FT_LOAD_TARGET_LCD:    Int = 0x30000
  val FT_LOAD_TARGET_LCD_V:  Int = 0x40000

  // ─── Render modes ──────────────────────────────────────────────────────

  val FT_RENDER_MODE_NORMAL: Int = 0
  val FT_RENDER_MODE_LIGHT:  Int = 1
  val FT_RENDER_MODE_MONO:   Int = 2
  val FT_RENDER_MODE_LCD:    Int = 3
  val FT_RENDER_MODE_LCD_V:  Int = 4
  val FT_RENDER_MODE_MAX:    Int = 5

  // ─── Kerning modes ─────────────────────────────────────────────────────

  val FT_KERNING_DEFAULT:  Int = 0
  val FT_KERNING_UNFITTED: Int = 1
  val FT_KERNING_UNSCALED: Int = 2

  // ─── Stroker constants ──────────────────────────────────────────────────

  val FT_STROKER_LINECAP_BUTT:   Int = 0
  val FT_STROKER_LINECAP_ROUND:  Int = 1
  val FT_STROKER_LINECAP_SQUARE: Int = 2

  val FT_STROKER_LINEJOIN_ROUND:          Int = 0
  val FT_STROKER_LINEJOIN_BEVEL:          Int = 1
  val FT_STROKER_LINEJOIN_MITER_VARIABLE: Int = 2
  val FT_STROKER_LINEJOIN_MITER:          Int = FT_STROKER_LINEJOIN_MITER_VARIABLE
  val FT_STROKER_LINEJOIN_MITER_FIXED:    Int = 3

  // ─── Utility ────────────────────────────────────────────────────────────

  /** Converts a 26.6 fixed-point value to an integer, rounding up. */
  def toInt(value: Int): Int =
    ((value + 63) & -64) >> 6

  /** Returns the last error code FreeType reported. */
  def getLastErrorCode: Int = ops.getLastErrorCode()

  /** Initializes FreeType and returns a new Library instance. */
  def initFreeType(): Library = {
    val address = ops.initFreeType()
    if (address == 0)
      throw SgeError.GraphicsError(s"Couldn't initialize FreeType library, FreeType error code: $getLastErrorCode")
    new Library(address)
  }

  // ─── Library ────────────────────────────────────────────────────────────

  class Library(private[freetype] var address: Long) extends AutoCloseable {

    def newMemoryFace(data: Array[Byte], dataSize: Int, faceIndex: Int): Face = {
      val face = ops.newMemoryFace(address, data, dataSize, faceIndex)
      if (face == 0)
        throw SgeError.GraphicsError(s"Couldn't load font, FreeType error code: $getLastErrorCode")
      new Face(face, this)
    }

    def newMemoryFace(data: Array[Byte], faceIndex: Int): Face =
      newMemoryFace(data, data.length, faceIndex)

    def createStroker(): Stroker = {
      val stroker = ops.strokerNew(address)
      if (stroker == 0)
        throw SgeError.GraphicsError(s"Couldn't create FreeType stroker, FreeType error code: $getLastErrorCode")
      new Stroker(stroker)
    }

    override def close(): Unit =
      ops.doneFreeType(address)
  }

  // ─── Face ───────────────────────────────────────────────────────────────

  class Face(private[freetype] var address: Long, val library: Library) extends AutoCloseable {

    def faceFlags:          Int = ops.getFaceFlags(address)
    def styleFlags:         Int = ops.getStyleFlags(address)
    def numGlyphs:          Int = ops.getNumGlyphs(address)
    def ascender:           Int = ops.getAscender(address)
    def descender:          Int = ops.getDescender(address)
    def height:             Int = ops.getHeight(address)
    def maxAdvanceWidth:    Int = ops.getMaxAdvanceWidth(address)
    def maxAdvanceHeight:   Int = ops.getMaxAdvanceHeight(address)
    def underlinePosition:  Int = ops.getUnderlinePosition(address)
    def underlineThickness: Int = ops.getUnderlineThickness(address)

    def selectSize(strikeIndex: Int): Boolean =
      ops.selectSize(address, strikeIndex)

    def setCharSize(charWidth: Int, charHeight: Int, horzResolution: Int, vertResolution: Int): Boolean =
      ops.setCharSize(address, charWidth, charHeight, horzResolution, vertResolution)

    def setPixelSizes(pixelWidth: Int, pixelHeight: Int): Boolean =
      ops.setPixelSizes(address, pixelWidth, pixelHeight)

    def loadGlyph(glyphIndex: Int, loadFlags: Int): Boolean =
      ops.loadGlyph(address, glyphIndex, loadFlags)

    def loadChar(charCode: Int, loadFlags: Int): Boolean =
      ops.loadChar(address, charCode, loadFlags)

    def getGlyph: GlyphSlot =
      new GlyphSlot(ops.getGlyphSlot(address))

    def getSize: Size =
      new Size(address)

    def hasKerning: Boolean =
      ops.hasKerning(address)

    def getKerning(leftGlyph: Int, rightGlyph: Int, kernMode: Int): Int =
      ops.getKerning(address, leftGlyph, rightGlyph, kernMode)

    def getCharIndex(charCode: Int): Int =
      ops.getCharIndex(address, charCode)

    override def close(): Unit =
      ops.doneFace(address)
  }

  // ─── Size ───────────────────────────────────────────────────────────────

  class Size(private val faceAddress: Long) {
    def getMetrics: SizeMetrics = {
      val out = new Array[Int](8)
      ops.getSizeMetrics(faceAddress, out)
      new SizeMetrics(out)
    }
  }

  // ─── SizeMetrics ────────────────────────────────────────────────────────

  /** Cached size metrics. Fields correspond to FT_Size_Metrics: xPpem, yPpem, xScale, yScale, ascender, descender, height, maxAdvance.
    */
  class SizeMetrics(private val data: Array[Int]) {
    def xPpem:      Int = data(0)
    def yPpem:      Int = data(1)
    def xScale:     Int = data(2)
    def yScale:     Int = data(3)
    def ascender:   Int = data(4)
    def descender:  Int = data(5)
    def height:     Int = data(6)
    def maxAdvance: Int = data(7)

    // Compatibility aliases matching LibGDX API names
    def getXppem:      Int = xPpem
    def getYppem:      Int = yPpem
    def getXScale:     Int = xScale
    def getYscale:     Int = yScale
    def getAscender:   Int = ascender
    def getDescender:  Int = descender
    def getHeight:     Int = height
    def getMaxAdvance: Int = maxAdvance
  }

  // ─── GlyphSlot ──────────────────────────────────────────────────────────

  class GlyphSlot(private[freetype] val address: Long) {

    def getMetrics: GlyphMetrics = {
      val out = new Array[Int](5)
      ops.getGlyphMetrics(address, out)
      new GlyphMetrics(out)
    }

    def linearHoriAdvance: Int = ops.getGlyphLinearHoriAdvance(address)
    def advanceX:          Int = ops.getGlyphAdvanceX(address)
    def advanceY:          Int = ops.getGlyphAdvanceY(address)
    def format:            Int = ops.getGlyphFormat(address)
    def bitmapLeft:        Int = ops.getGlyphBitmapLeft(address)
    def bitmapTop:         Int = ops.getGlyphBitmapTop(address)

    // Compatibility aliases
    def getLinearHoriAdvance: Int = linearHoriAdvance
    def getAdvanceX:          Int = advanceX
    def getAdvanceY:          Int = advanceY
    def getFormat:            Int = format
    def getBitmapLeft:        Int = bitmapLeft
    def getBitmapTop:         Int = bitmapTop

    def getBitmap: Bitmap =
      new Bitmap(address)

    def renderGlyph(renderMode: Int): Boolean =
      ops.renderGlyph(address, renderMode)

    def getGlyph: Glyph = {
      val glyph = ops.getGlyphAsStroke(address)
      if (glyph == 0)
        throw SgeError.GraphicsError(s"Couldn't get glyph, FreeType error code: $getLastErrorCode")
      new Glyph(glyph)
    }
  }

  // ─── Glyph ──────────────────────────────────────────────────────────────

  class Glyph(private[freetype] var address: Long) extends AutoCloseable {
    private var rendered: Boolean = false

    def strokeBorder(stroker: Stroker, inside: Boolean): Unit =
      address = ops.strokeBorder(address, stroker.address, inside)

    def toBitmap(renderMode: Int): Unit = {
      val bitmap = ops.glyphToBitmap(address, renderMode)
      if (bitmap == 0)
        throw SgeError.GraphicsError(s"Couldn't render glyph, FreeType error code: $getLastErrorCode")
      address = bitmap
      rendered = true
    }

    def getBitmap: Bitmap = {
      if (!rendered) throw SgeError.GraphicsError("Glyph is not yet rendered")
      new BitmapGlyphBitmap(address)
    }

    def getLeft: Int = {
      if (!rendered) throw SgeError.GraphicsError("Glyph is not yet rendered")
      val out = new Array[Int](7)
      ops.getBitmapGlyphBitmap(address, out)
      out(5)
    }

    def getTop: Int = {
      if (!rendered) throw SgeError.GraphicsError("Glyph is not yet rendered")
      val out = new Array[Int](7)
      ops.getBitmapGlyphBitmap(address, out)
      out(6)
    }

    override def close(): Unit =
      ops.doneGlyph(address)
  }

  // ─── Bitmap ─────────────────────────────────────────────────────────────

  /** Bitmap from a glyph slot (data is read through the slot handle). */
  class Bitmap(private val slotAddress: Long) {
    def rows:      Int = ops.getGlyphBitmapRows(slotAddress)
    def width:     Int = ops.getGlyphBitmapWidth(slotAddress)
    def pitch:     Int = ops.getGlyphBitmapPitch(slotAddress)
    def numGray:   Int = ops.getGlyphBitmapNumGray(slotAddress)
    def pixelMode: Int = ops.getGlyphBitmapPixelMode(slotAddress)

    // Compatibility aliases
    def getRows:      Int = rows
    def getWidth:     Int = width
    def getPitch:     Int = pitch
    def getNumGray:   Int = numGray
    def getPixelMode: Int = pixelMode

    /** Returns the bitmap buffer as a byte array. */
    def getBuffer: Array[Byte] = {
      val r = rows
      val p = Math.abs(pitch)
      if (r == 0 || p == 0) {
        new Array[Byte](1) // Dummy buffer for empty bitmaps (matches LibGDX Issue #768)
      } else {
        val size   = r * p
        val buffer = new Array[Byte](size)
        ops.getGlyphBitmapBuffer(slotAddress, buffer, size)
        buffer
      }
    }

    /** Creates a Pixmap from the bitmap data with the given format, color, and gamma correction. */
    def getPixmap(format: Format, color: Color, gamma: Float)(using Sge): Pixmap = {
      val w        = width
      val r        = rows
      val src      = getBuffer
      val pMode    = pixelMode
      val rowBytes = Math.abs(pitch)
      val pixmap: Pixmap =
        if (color == Color.WHITE && pMode == FT_PIXEL_MODE_GRAY && rowBytes == w && gamma == 1) {
          val pm     = Pixmap(w, r, Format.Alpha)
          val pixels = pm.pixels
          pixels.put(src, 0, Math.min(src.length, pixels.capacity()))
          pixels.rewind()
          pm
        } else {
          val pm     = Pixmap(w, r, Format.RGBA8888)
          val rgba   = Color.rgba8888(color)
          val srcRow = new Array[Byte](rowBytes)
          val dstRow = new Array[Int](w)
          val dst    = pm.pixels.asIntBuffer()
          if (pMode == FT_PIXEL_MODE_MONO) {
            // Use the specified color for each set bit.
            var y = 0
            while (y < r) {
              System.arraycopy(src, y * rowBytes, srcRow, 0, rowBytes)
              var i = 0
              var x = 0
              while (x < w) {
                val b  = srcRow(i)
                var ii = 0
                val n  = Math.min(8, w - x)
                while (ii < n) {
                  if ((b & (1 << (7 - ii))) != 0)
                    dstRow(x + ii) = rgba
                  else
                    dstRow(x + ii) = 0
                  ii += 1
                }
                i += 1
                x += 8
              }
              dst.put(dstRow)
              y += 1
            }
          } else {
            // Use the specified color for RGB, blend the FreeType bitmap with alpha.
            val rgb = rgba & 0xffffff00
            val a   = rgba & 0xff
            var y   = 0
            while (y < r) {
              System.arraycopy(src, y * rowBytes, srcRow, 0, rowBytes)
              var x = 0
              while (x < w) {
                val alpha = srcRow(x) & 0xff
                if (alpha == 0)
                  dstRow(x) = rgb
                else if (alpha == 255)
                  dstRow(x) = rgb | a
                else
                  dstRow(x) = rgb | (a * Math.pow(alpha / 255f, gamma).toFloat).toInt // Inverse gamma.
                x += 1
              }
              dst.put(dstRow)
              y += 1
            }
          }
          pm
        }

      if (format != pixmap.format) {
        val converted = Pixmap(pixmap.width.toInt, pixmap.height.toInt, format)
        converted.setBlending(Blending.None)
        converted.drawPixmap(pixmap, Pixels(0), Pixels(0))
        converted.setBlending(Blending.SourceOver)
        pixmap.close()
        converted
      } else {
        pixmap
      }
    }
  }

  /** Bitmap from a rendered Glyph (BitmapGlyph). Data is read from the glyph handle. */
  private class BitmapGlyphBitmap(private val glyphAddress: Long) extends Bitmap(glyphAddress) {
    // Cache the bitmap info once
    private val info: Array[Int] = {
      val out = new Array[Int](7)
      ops.getBitmapGlyphBitmap(glyphAddress, out)
      out
    }

    override def rows:      Int = info(0)
    override def width:     Int = info(1)
    override def pitch:     Int = info(2)
    override def numGray:   Int = info(3)
    override def pixelMode: Int = info(4)

    override def getBuffer: Array[Byte] = {
      val r = rows
      val p = Math.abs(pitch)
      if (r == 0 || p == 0) {
        new Array[Byte](1)
      } else {
        val size   = r * p
        val buffer = new Array[Byte](size)
        ops.getBitmapGlyphBuffer(glyphAddress, buffer, size)
        buffer
      }
    }
  }

  // ─── GlyphMetrics ──────────────────────────────────────────────────────

  /** Cached glyph metrics. Fields: width, height, horiBearingX, horiBearingY, horiAdvance. */
  class GlyphMetrics(private val data: Array[Int]) {
    def width:        Int = data(0)
    def height:       Int = data(1)
    def horiBearingX: Int = data(2)
    def horiBearingY: Int = data(3)
    def horiAdvance:  Int = data(4)

    // Compatibility aliases
    def getWidth:        Int = width
    def getHeight:       Int = height
    def getHoriBearingX: Int = horiBearingX
    def getHoriBearingY: Int = horiBearingY
    def getHoriAdvance:  Int = horiAdvance
  }

  // ─── Stroker ────────────────────────────────────────────────────────────

  class Stroker(private[freetype] var address: Long) extends AutoCloseable {
    def set(radius: Int, lineCap: Int, lineJoin: Int, miterLimit: Int): Unit =
      ops.strokerSet(address, radius, lineCap, lineJoin, miterLimit)

    override def close(): Unit =
      ops.strokerDone(address)
  }
}
