/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGenerator.java
 * Original authors: mzechner, Nathan Sweet, Rob Rendell
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError; Disposable -> AutoCloseable; dispose() -> close();
 *     Array<T> -> DynamicArray[T]; Gdx.app.log -> scribe.info
 *   Convention: Nullable throughout; using Sge context parameter on constructor;
 *     inner static classes -> companion object members; getters -> property accessors
 *   Idiom: boundary/break (0 return), Nullable (0 null), split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d
package freetype

import scala.util.boundary
import scala.util.boundary.break

import sge.{ Pixels, Sge }
import sge.files.FileHandle
import sge.graphics.{ Color, Pixmap }
import sge.graphics.Pixmap.{ Blending, Format }
import sge.graphics.Texture.TextureFilter
import sge.graphics.g2d.{ BitmapFont, BitmapFontData, GlyphRun, PixmapPacker, TextureRegion }
import sge.graphics.g2d.freetype.FreeType.*
import sge.math.MathUtils
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** Generates {@link BitmapFont} and {@link BitmapFont.BitmapFontData} instances from TrueType, OTF, and other FreeType supported
  * fonts.
  *
  * Usage example:
  * {{{
  * val gen = FreeTypeFontGenerator(Sge().files.internal("myfont.ttf"))
  * val param = FreeTypeFontGenerator.FreeTypeFontParameter()
  * param.size = 16
  * val font = gen.generateFont(param)
  * gen.close() // Don't close if doing incremental glyph generation.
  * }}}
  *
  * The generator has to be closed once it is no longer used. The returned {@link BitmapFont} instances are managed by the user and
  * have to be closed as usual.
  *
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  * @author
  *   Rob Rendell
  */
class FreeTypeFontGenerator(fontFile: FileHandle, faceIndex: Int)(using Sge) extends AutoCloseable {

  def this(fontFile: FileHandle)(using Sge) = this(fontFile, 0)

  import FreeTypeFontGenerator.*

  val library: Library = FreeType.initFreeType()
  val face: Face = {
    val data = fontFile.readBytes()
    library.newMemoryFace(data, data.length, faceIndex)
  }
  val name: String = fontFile.nameWithoutExtension()

  var bitmapped: Boolean = false
  // pixelWidth/pixelHeight not stored — only used transiently in setPixelSizes

  // Check for bitmap font on init
  if (!checkForBitmapFont()) setPixelSizes(0, 15)

  private def getLoadingFlags(parameter: FreeTypeFontParameter): Int = {
    var loadingFlags = FreeType.FT_LOAD_DEFAULT
    parameter.hinting match {
      case Hinting.None       => loadingFlags |= FreeType.FT_LOAD_NO_HINTING
      case Hinting.Slight     => loadingFlags |= FreeType.FT_LOAD_TARGET_LIGHT
      case Hinting.Medium     => loadingFlags |= FreeType.FT_LOAD_TARGET_NORMAL
      case Hinting.Full       => loadingFlags |= FreeType.FT_LOAD_TARGET_MONO
      case Hinting.AutoSlight => loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_LIGHT
      case Hinting.AutoMedium => loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_NORMAL
      case Hinting.AutoFull   => loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_MONO
    }
    loadingFlags
  }

  private def loadChar(c: Int): Boolean =
    loadChar(c, FreeType.FT_LOAD_DEFAULT | FreeType.FT_LOAD_FORCE_AUTOHINT)

  private def loadChar(c: Int, flags: Int): Boolean =
    face.loadChar(c, flags)

  private def checkForBitmapFont(): Boolean = {
    val faceFlags = face.faceFlags
    if (((faceFlags & FreeType.FT_FACE_FLAG_FIXED_SIZES) == FreeType.FT_FACE_FLAG_FIXED_SIZES)
      && ((faceFlags & FreeType.FT_FACE_FLAG_HORIZONTAL) == FreeType.FT_FACE_FLAG_HORIZONTAL)) {
      if (loadChar(32)) {
        val slot = face.getGlyph
        if (slot.format == 1651078259) {
          bitmapped = true
        }
      }
    }
    bitmapped
  }

  def generateFont(parameter: FreeTypeFontParameter): BitmapFont =
    generateFont(parameter, FreeTypeBitmapFontData(parameter.flip))

  /** Generates a new {@link BitmapFont}. The size is expressed in pixels. Throws a SgeError if the font could not be generated.
    * Using big sizes might cause such an exception.
    * @param parameter
    *   configures how the font is generated
    */
  def generateFont(parameter: FreeTypeFontParameter, data: FreeTypeBitmapFontData): BitmapFont = {
    val updateTextureRegions = data.regions.isEmpty && parameter.packer.isDefined
    if (updateTextureRegions) data.regions = Nullable(DynamicArray[TextureRegion]())
    generateData(parameter, data)
    if (updateTextureRegions)
      parameter.packer.foreach { p =>
        data.regions.foreach(r => p.updateTextureRegions(r, parameter.minFilter, parameter.magFilter, parameter.genMipMaps))
      }
    data.regions.foreach { r =>
      if (r.size == 0) throw SgeError.GraphicsError("Unable to create a font with no texture regions.")
    }
    val font = BitmapFont(data, data.regions, true)
    font.ownsTexture = parameter.packer.isEmpty
    font
  }

  /** Uses ascender and descender of font to calculate real height that makes all glyphs to fit in given pixel size. */
  def scaleForPixelHeight(height: Int): Int = {
    setPixelSizes(0, height)
    val fontMetrics = face.getSize.getMetrics
    val ascent  = FreeType.toInt(fontMetrics.ascender)
    val descent = FreeType.toInt(fontMetrics.descender)
    height * height / (ascent - descent)
  }

  /** Uses max advance, ascender and descender of font to calculate real height that makes any n glyphs to fit in given pixel
    * width.
    */
  def scaleForPixelWidth(width: Int, numChars: Int): Int = {
    val fontMetrics    = face.getSize.getMetrics
    val advance        = FreeType.toInt(fontMetrics.maxAdvance)
    val ascent         = FreeType.toInt(fontMetrics.ascender)
    val descent        = FreeType.toInt(fontMetrics.descender)
    val unscaledHeight = ascent - descent
    val height         = unscaledHeight * width / (advance * numChars)
    setPixelSizes(0, height)
    height
  }

  def scaleToFitSquare(width: Int, height: Int, numChars: Int): Int =
    Math.min(scaleForPixelHeight(height), scaleForPixelWidth(width, numChars))

  def setPixelSizes(pixelWidth: Int, pixelHeight: Int): Unit = {
    if (!bitmapped && !face.setPixelSizes(pixelWidth, pixelHeight))
      throw SgeError.GraphicsError("Couldn't set size for font")
  }

  /** Generates a new {@link BitmapFont.BitmapFontData} instance. */
  def generateData(size: Int): FreeTypeBitmapFontData = {
    val parameter = FreeTypeFontParameter()
    parameter.size = size
    generateData(parameter)
  }

  def generateData(parameter: FreeTypeFontParameter): FreeTypeBitmapFontData =
    generateData(parameter, FreeTypeBitmapFontData(parameter.flip))

  /** Generates a new {@link BitmapFont.BitmapFontData} instance, expert usage only. */
  def generateData(parameter: FreeTypeFontParameter, data: FreeTypeBitmapFontData): FreeTypeBitmapFontData = {
    data.name = Nullable(name + "-" + parameter.size)
    val characters       = parameter.characters.toCharArray
    val charactersLength = characters.length
    val incremental      = parameter.incremental
    val flags            = getLoadingFlags(parameter)

    setPixelSizes(0, parameter.size)

    // set general font data
    val fontMetrics = face.getSize.getMetrics
    // Note: flipped is set via the FreeTypeBitmapFontData constructor parameter
    data.ascent = FreeType.toInt(fontMetrics.ascender).toFloat
    data.descent = FreeType.toInt(fontMetrics.descender).toFloat
    data.lineHeight = FreeType.toInt(fontMetrics.height).toFloat
    val baseLine = data.ascent

    // if bitmapped
    if (bitmapped && (data.lineHeight == 0)) {
      var c = 32
      while (c < (32 + face.numGlyphs)) {
        if (loadChar(c, flags)) {
          val lh = FreeType.toInt(face.getGlyph.getMetrics.height)
          if (lh > data.lineHeight) data.lineHeight = lh.toFloat
        }
        c += 1
      }
    }
    data.lineHeight += parameter.spaceY

    // determine space width
    if (loadChar(' ', flags) || loadChar('l', flags)) {
      data.spaceXadvance = FreeType.toInt(face.getGlyph.getMetrics.horiAdvance).toFloat
    } else {
      data.spaceXadvance = face.maxAdvanceWidth.toFloat // Possibly very wrong.
    }

    // determine x-height
    var xFound = false
    var xi     = 0
    while (xi < data.xChars.length && !xFound) {
      if (loadChar(data.xChars(xi), flags)) {
        data.xHeight = FreeType.toInt(face.getGlyph.getMetrics.height).toFloat
        xFound = true
      }
      xi += 1
    }
    if (data.xHeight == 0) throw SgeError.GraphicsError("No x-height character found in font")

    // determine cap height
    var capFound = false
    var ci       = 0
    while (ci < data.capChars.length && !capFound) {
      if (loadChar(data.capChars(ci), flags)) {
        data.capHeight = FreeType.toInt(face.getGlyph.getMetrics.height).toFloat + Math.abs(parameter.shadowOffsetY)
        capFound = true
      }
      ci += 1
    }
    if (!bitmapped && data.capHeight == 1) throw SgeError.GraphicsError("No cap character found in font")

    data.ascent -= data.capHeight
    data.down = -data.lineHeight
    if (parameter.flip) {
      data.ascent = -data.ascent
      data.down = -data.down
    }

    val ownsAtlas = parameter.packer.isEmpty
    val packer: PixmapPacker = parameter.packer.getOrElse {
      // Create a packer.
      val size: Int = if (incremental) {
        maxTextureSize
      } else {
        val maxGlyphHeight = Math.ceil(data.lineHeight).toInt
        var s = MathUtils.nextPowerOfTwo(Math.sqrt(maxGlyphHeight.toDouble * maxGlyphHeight * charactersLength).toInt)
        if (maxTextureSize > 0) s = Math.min(s, maxTextureSize)
        s
      }
      // SkylineStrategy is not yet ported, use GuillotineStrategy for both cases
      val packStrategy: PixmapPacker.PackStrategy = PixmapPacker.GuillotineStrategy()
      val p = PixmapPacker(size, size, Format.RGBA8888, 1, false, packStrategy)
      p.transparentColor = Color(parameter.color.r, parameter.color.g, parameter.color.b, 0f)
      if (parameter.borderWidth > 0) {
        p.transparentColor = Color(parameter.borderColor.r, parameter.borderColor.g, parameter.borderColor.b, 0f)
      }
      p
    }

    if (incremental) data.incrementalGlyphs = Nullable(DynamicArray[BitmapFont.Glyph](charactersLength + 32))

    var stroker: Nullable[Stroker] = Nullable.empty
    if (parameter.borderWidth > 0) {
      val s = library.createStroker()
      s.set(
        (parameter.borderWidth * 64f).toInt,
        if (parameter.borderStraight) FreeType.FT_STROKER_LINECAP_BUTT else FreeType.FT_STROKER_LINECAP_ROUND,
        if (parameter.borderStraight) FreeType.FT_STROKER_LINEJOIN_MITER_FIXED else FreeType.FT_STROKER_LINEJOIN_ROUND,
        0
      )
      stroker = Nullable(s)
    }

    // Create glyphs largest height first for best packing.
    val heights = new Array[Int](charactersLength)
    var hi      = 0
    while (hi < charactersLength) {
      val c      = characters(hi)
      val height = if (loadChar(c, flags)) FreeType.toInt(face.getGlyph.getMetrics.height) else 0
      heights(hi) = height

      if (c == '\u0000') {
        val missingGlyph = createGlyph('\u0000', data, parameter, stroker, baseLine, packer)
        missingGlyph.foreach { mg =>
          if (mg.width != 0 && mg.height != 0) {
            data.setGlyph('\u0000', mg)
            data.missingGlyph = Nullable(mg)
            if (incremental) data.incrementalGlyphs.foreach(_.add(mg))
          }
        }
      }
      hi += 1
    }

    var heightsCount = heights.length
    while (heightsCount > 0) {
      var best      = 0
      var maxHeight = heights(0)
      var i         = 1
      while (i < heightsCount) {
        val h = heights(i)
        if (h > maxHeight) {
          maxHeight = h
          best = i
        }
        i += 1
      }

      val c = characters(best)
      if (data.getGlyph(c).isEmpty) {
        val glyph = createGlyph(c, data, parameter, stroker, baseLine, packer)
        glyph.foreach { g =>
          data.setGlyph(c, g)
          if (incremental) data.incrementalGlyphs.foreach(_.add(g))
        }
      }

      heightsCount -= 1
      heights(best) = heights(heightsCount)
      val tmpChar = characters(best)
      characters(best) = characters(heightsCount)
      characters(heightsCount) = tmpChar
    }

    if (stroker.isDefined && !incremental) stroker.foreach(_.close())

    if (incremental) {
      data.generator = Nullable(this)
      data.parameter = Nullable(parameter)
      data.stroker = stroker
      data.incrementalPacker = Nullable(packer)
    }

    // Generate kerning.
    val kerning = parameter.kerning && face.hasKerning
    if (kerning) {
      var i = 0
      while (i < charactersLength) {
        val firstChar = characters(i)
        val first     = data.getGlyph(firstChar)
        if (first.isDefined) {
          val firstIndex = face.getCharIndex(firstChar)
          var ii         = i
          while (ii < charactersLength) {
            val secondChar = characters(ii)
            val second     = data.getGlyph(secondChar)
            if (second.isDefined) {
              val secondIndex = face.getCharIndex(secondChar)
              val k1 = face.getKerning(firstIndex, secondIndex, 0) // FT_KERNING_DEFAULT
              if (k1 != 0) first.foreach(_.setKerning(secondChar, FreeType.toInt(k1)))
              val k2 = face.getKerning(secondIndex, firstIndex, 0)
              if (k2 != 0) second.foreach(_.setKerning(firstChar, FreeType.toInt(k2)))
            }
            ii += 1
          }
        }
        i += 1
      }
    }

    // Generate texture regions.
    if (ownsAtlas) {
      data.regions = Nullable(DynamicArray[TextureRegion]())
      data.regions.foreach(r => packer.updateTextureRegions(r, parameter.minFilter, parameter.magFilter, parameter.genMipMaps))
    }

    // Set space glyph.
    var spaceGlyph = data.getGlyph(' ')
    if (spaceGlyph.isEmpty) {
      val sg = BitmapFont.Glyph()
      sg.xadvance = data.spaceXadvance.toInt + parameter.spaceX
      sg.id = ' '.toInt
      data.setGlyph(' ', sg)
      spaceGlyph = Nullable(sg)
    }
    spaceGlyph.foreach { sg =>
      if (sg.width == 0) sg.width = (sg.xadvance + data.padRight).toInt
    }

    data
  }

  /** Returns Nullable.empty if glyph was not found in the font. */
  protected def createGlyph(
    c:         Char,
    data:      FreeTypeBitmapFontData,
    parameter: FreeTypeFontParameter,
    stroker:   Nullable[Stroker],
    baseLine:  Float,
    packer:    PixmapPacker
  ): Nullable[BitmapFont.Glyph] = {

    val missing = face.getCharIndex(c) == 0 && c != 0
    if (missing) Nullable.empty
    else if (!loadChar(c, getLoadingFlags(parameter))) Nullable.empty
    else boundary {
      val slot      = face.getGlyph
      var mainGlyph = slot.getGlyph
      try {
        mainGlyph.toBitmap(if (parameter.mono) FreeType.FT_RENDER_MODE_MONO else FreeType.FT_RENDER_MODE_NORMAL)
      } catch {
        case _: SgeError =>
          mainGlyph.close()
          scribe.info(s"FreeTypeFontGenerator: Couldn't render char: $c")
          break(Nullable.empty)
      }
      val mainBitmap = mainGlyph.getBitmap
      var mainPixmap = mainBitmap.getPixmap(Format.RGBA8888, parameter.color, parameter.gamma)

      if (mainBitmap.width != 0 && mainBitmap.rows != 0) {
        var offsetX = 0
        var offsetY = 0
        if (parameter.borderWidth > 0) {
          stroker.foreach { str =>
            // execute stroker; this generates a glyph "extended" along the outline
            val top  = mainGlyph.getTop
            val left = mainGlyph.getLeft
            val borderGlyph = slot.getGlyph
            borderGlyph.strokeBorder(str, false)
            borderGlyph.toBitmap(if (parameter.mono) FreeType.FT_RENDER_MODE_MONO else FreeType.FT_RENDER_MODE_NORMAL)
            offsetX = left - borderGlyph.getLeft
            offsetY = -(top - borderGlyph.getTop)

            // Render border (pixmap is bigger than main).
            val borderBitmap = borderGlyph.getBitmap
            val borderPixmap = borderBitmap.getPixmap(Format.RGBA8888, parameter.borderColor, parameter.borderGamma)

            // Draw main glyph on top of border.
            var ri = 0
            while (ri < parameter.renderCount) {
              borderPixmap.drawPixmap(mainPixmap, Pixels(offsetX), Pixels(offsetY))
              ri += 1
            }

            mainPixmap.close()
            mainGlyph.close()
            mainPixmap = borderPixmap
            mainGlyph = borderGlyph
          }
        }

        if (parameter.shadowOffsetX != 0 || parameter.shadowOffsetY != 0) {
          val mainW        = mainPixmap.getWidth().toInt
          val mainH        = mainPixmap.getHeight().toInt
          val shadowOffX   = Math.max(parameter.shadowOffsetX, 0)
          val shadowOffY   = Math.max(parameter.shadowOffsetY, 0)
          val shadowW      = mainW + Math.abs(parameter.shadowOffsetX)
          val shadowH      = mainH + Math.abs(parameter.shadowOffsetY)
          val shadowPixmap = Pixmap(shadowW, shadowH, mainPixmap.getFormat())
          shadowPixmap.setColor(packer.transparentColor)
          shadowPixmap.fill()

          val shadowColor = parameter.shadowColor
          val a           = shadowColor.a
          if (a != 0) {
            val r          = (shadowColor.r * 255).toByte
            val g          = (shadowColor.g * 255).toByte
            val b          = (shadowColor.b * 255).toByte
            val mainPixels   = mainPixmap.getPixels()
            val shadowPixels = shadowPixmap.getPixels()
            var y = 0
            while (y < mainH) {
              val shadowRow = shadowW * (y + shadowOffY) + shadowOffX
              var x = 0
              while (x < mainW) {
                val mainPixel = (mainW * y + x) * 4
                val mainA     = mainPixels.get(mainPixel + 3)
                if (mainA != 0) {
                  val shadowPixel = (shadowRow + x) * 4
                  shadowPixels.put(shadowPixel, r)
                  shadowPixels.put(shadowPixel + 1, g)
                  shadowPixels.put(shadowPixel + 2, b)
                  shadowPixels.put(shadowPixel + 3, ((mainA & 0xff) * a).toByte)
                }
                x += 1
              }
              y += 1
            }
          }

          // Draw main glyph (with any border) on top of shadow.
          var ri = 0
          while (ri < parameter.renderCount) {
            shadowPixmap.drawPixmap(mainPixmap, Pixels(Math.max(-parameter.shadowOffsetX, 0)), Pixels(Math.max(-parameter.shadowOffsetY, 0)))
            ri += 1
          }
          mainPixmap.close()
          mainPixmap = shadowPixmap
        } else if (parameter.borderWidth == 0) {
          // No shadow and no border, draw glyph additional times.
          var ri = 0
          while (ri < parameter.renderCount - 1) {
            mainPixmap.drawPixmap(mainPixmap, Pixels(0), Pixels(0))
            ri += 1
          }
        }

        if (parameter.padTop > 0 || parameter.padLeft > 0 || parameter.padBottom > 0 || parameter.padRight > 0) {
          val padPixmap = Pixmap(
            mainPixmap.getWidth().toInt + parameter.padLeft + parameter.padRight,
            mainPixmap.getHeight().toInt + parameter.padTop + parameter.padBottom,
            mainPixmap.getFormat()
          )
          padPixmap.setBlending(Blending.None)
          padPixmap.drawPixmap(mainPixmap, Pixels(parameter.padLeft), Pixels(parameter.padTop))
          mainPixmap.close()
          mainPixmap = padPixmap
        }
      }

      val metrics = slot.getMetrics
      val glyph   = BitmapFont.Glyph()
      glyph.id = c.toInt
      glyph.width = mainPixmap.getWidth().toInt
      glyph.height = mainPixmap.getHeight().toInt
      glyph.xoffset = mainGlyph.getLeft
      if (parameter.flip)
        glyph.yoffset = -mainGlyph.getTop + baseLine.toInt
      else
        glyph.yoffset = -(glyph.height - mainGlyph.getTop) - baseLine.toInt
      glyph.xadvance = FreeType.toInt(metrics.horiAdvance) + parameter.borderWidth.toInt + parameter.spaceX

      if (bitmapped) {
        mainPixmap.setColor(Color.CLEAR)
        mainPixmap.fill()
        val buf           = mainBitmap.getBuffer
        val whiteIntBits  = Color.WHITE.toIntBits()
        val clearIntBits  = Color.CLEAR.toIntBits()
        var h             = 0
        while (h < glyph.height) {
          val idx = h * mainBitmap.pitch
          var w   = 0
          while (w < (glyph.width + glyph.xoffset)) {
            val bit = (buf(idx + (w / 8)) >>> (7 - (w % 8))) & 1
            mainPixmap.drawPixel(Pixels(w), Pixels(h), if (bit == 1) whiteIntBits else clearIntBits)
            w += 1
          }
          h += 1
        }
      }

      val rectOpt = packer.pack(mainPixmap)
      rectOpt.foreach { rect =>
        glyph.page = packer.pages.indexOf(rect.page)
        glyph.srcX = rect.x
        glyph.srcY = rect.y
      }

      // If a page was added, create a new texture region for the incrementally added glyph.
      if (parameter.incremental && data.regions.isDefined) {
        data.regions.foreach { r =>
          if (r.size <= glyph.page) {
            packer.updateTextureRegions(r, parameter.minFilter, parameter.magFilter, parameter.genMipMaps)
          }
        }
      }

      mainPixmap.close()
      mainGlyph.close()

      Nullable(glyph)
    }
  }

  /** Check if the font glyph exists for a single UTF-32 code point. */
  def hasGlyph(charCode: Int): Boolean =
    face.getCharIndex(charCode) != 0

  override def toString: String = name

  /** Cleans up all resources of the generator. Call this if you no longer use the generator. */
  override def close(): Unit = {
    face.close()
    library.close()
  }
}

object FreeTypeFontGenerator {

  val DEFAULT_CHARS: String =
    "\u0000ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$\u20ac-%+=#_&~*\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008a\u008b\u008c\u008d\u008e\u008f\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009a\u009b\u009c\u009d\u009e\u009f\u00a0\u00a1\u00a2\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00aa\u00ab\u00ac\u00ad\u00ae\u00af\u00b0\u00b1\u00b2\u00b3\u00b4\u00b5\u00b6\u00b7\u00b8\u00b9\u00ba\u00bb\u00bc\u00bd\u00be\u00bf\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u00c6\u00c7\u00c8\u00c9\u00ca\u00cb\u00cc\u00cd\u00ce\u00cf\u00d0\u00d1\u00d2\u00d3\u00d4\u00d5\u00d6\u00d7\u00d8\u00d9\u00da\u00db\u00dc\u00dd\u00de\u00df\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u00e7\u00e8\u00e9\u00ea\u00eb\u00ec\u00ed\u00ee\u00ef\u00f0\u00f1\u00f2\u00f3\u00f4\u00f5\u00f6\u00f7\u00f8\u00f9\u00fa\u00fb\u00fc\u00fd\u00fe\u00ff"

  /** A hint to scale the texture as needed, without capping it at any maximum size. */
  val NO_MAXIMUM: Int = -1

  /** The maximum texture size allowed by generateData, when storing in a texture atlas. Multiple texture pages will be created if
    * necessary. Default is 1024.
    */
  var maxTextureSize: Int = 1024

  /** Font smoothing algorithm. */
  enum Hinting extends java.lang.Enum[Hinting] {
    /** Disable hinting. Generated glyphs will look blurry. */
    case None

    /** Light hinting with fuzzy edges, but close to the original shape. */
    case Slight

    /** Average hinting. */
    case Medium

    /** Strong hinting with crisp edges at the expense of shape fidelity. */
    case Full

    /** Light hinting with fuzzy edges, but close to the original shape. Uses the FreeType auto-hinter. */
    case AutoSlight

    /** Average hinting. Uses the FreeType auto-hinter. */
    case AutoMedium

    /** Strong hinting with crisp edges at the expense of shape fidelity. Uses the FreeType auto-hinter. */
    case AutoFull
  }

  /** Parameter container class that helps configure how {@link FreeTypeBitmapFontData} and {@link BitmapFont} instances are
    * generated.
    */
  class FreeTypeFontParameter {
    /** The size in pixels. */
    var size: Int = 16

    /** If true, font smoothing is disabled. */
    var mono: Boolean = false

    /** Strength of hinting. */
    var hinting: Hinting = Hinting.AutoMedium

    /** Foreground color (required for non-black borders). */
    var color: Color = Color.WHITE

    /** Glyph gamma. Values > 1 reduce antialiasing. */
    var gamma: Float = 1.8f

    /** Number of times to render the glyph. Useful with a shadow or border, so it doesn't show through the glyph. */
    var renderCount: Int = 2

    /** Border width in pixels, 0 to disable. */
    var borderWidth: Float = 0

    /** Border color; only used if borderWidth > 0. */
    var borderColor: Color = Color.BLACK

    /** true for straight (mitered), false for rounded borders. */
    var borderStraight: Boolean = false

    /** Values < 1 increase the border size. */
    var borderGamma: Float = 1.8f

    /** Offset of text shadow on X axis in pixels, 0 to disable. */
    var shadowOffsetX: Int = 0

    /** Offset of text shadow on Y axis in pixels, 0 to disable. */
    var shadowOffsetY: Int = 0

    /** Shadow color; only used if shadowOffset > 0. */
    var shadowColor: Color = Color(0, 0, 0, 0.75f)

    /** Pixels to add to glyph spacing when text is rendered. Can be negative. */
    var spaceX: Int = 0
    var spaceY: Int = 0

    /** Pixels to add to the glyph in the texture. Cannot be negative. */
    var padTop:    Int = 0
    var padLeft:   Int = 0
    var padBottom: Int = 0
    var padRight:  Int = 0

    /** The characters the font should contain. If '\0' is not included then missingGlyph is not set. */
    var characters: String = DEFAULT_CHARS

    /** Whether the font should include kerning. */
    var kerning: Boolean = true

    /** The optional PixmapPacker to use for packing multiple fonts into a single texture. */
    var packer: Nullable[PixmapPacker] = Nullable.empty

    /** Whether to flip the font vertically. */
    var flip: Boolean = false

    /** Whether to generate mip maps for the resulting texture. */
    var genMipMaps: Boolean = false

    /** Minification filter. */
    var minFilter: TextureFilter = TextureFilter.Nearest

    /** Magnification filter. */
    var magFilter: TextureFilter = TextureFilter.Nearest

    /** When true, glyphs are rendered on the fly to the font's glyph page textures as they are needed. */
    var incremental: Boolean = false
  }

  /** {@link BitmapFont.BitmapFontData} used for fonts generated via the {@link FreeTypeFontGenerator}. The texture storing the
    * glyphs is held in memory, thus the imagePaths and fontFile will be empty/null.
    */
  class FreeTypeBitmapFontData(isFlipped: Boolean = false) extends BitmapFontData(flipped = isFlipped) {
    var regions: Nullable[DynamicArray[TextureRegion]] = Nullable.empty

    // Fields for incremental glyph generation.
    var generator:        Nullable[FreeTypeFontGenerator]  = Nullable.empty
    var parameter:        Nullable[FreeTypeFontParameter]  = Nullable.empty
    var stroker:          Nullable[Stroker]                 = Nullable.empty
    var incrementalPacker: Nullable[PixmapPacker]           = Nullable.empty
    var incrementalGlyphs: Nullable[DynamicArray[BitmapFont.Glyph]] = Nullable.empty
    private var dirty: Boolean = false

    override def getGlyph(ch: Char): Nullable[BitmapFont.Glyph] = {
      var glyph = super.getGlyph(ch)
      if (glyph.isEmpty && generator.isDefined) {
        generator.foreach { gen =>
          parameter.foreach { param =>
            gen.setPixelSizes(0, param.size)
            val baseline = ((if (flipped) -ascent else ascent) + capHeight) / scaleY
            incrementalPacker.foreach { pack =>
              val created = gen.createGlyph(ch, this, param, stroker, baseline, pack)
              created.foreach { g =>
                glyph = Nullable(g)
                regions.foreach(r => setGlyphRegion(g, r(g.page)))
                setGlyph(ch, g)
                incrementalGlyphs.foreach(_.add(g))
                dirty = true

                if (param.kerning) {
                  val glyphIndex = gen.face.getCharIndex(ch)
                  incrementalGlyphs.foreach { glyphs =>
                    var i = 0
                    while (i < glyphs.size) {
                      val other      = glyphs(i)
                      val otherIndex = gen.face.getCharIndex(other.id)
                      val k1         = gen.face.getKerning(glyphIndex, otherIndex, 0)
                      if (k1 != 0) g.setKerning(other.id, FreeType.toInt(k1))
                      val k2 = gen.face.getKerning(otherIndex, glyphIndex, 0)
                      if (k2 != 0) other.setKerning(ch, FreeType.toInt(k2))
                      i += 1
                    }
                  }
                }
              }
            }
          }
        }
        if (glyph.isEmpty) glyph = missingGlyph
      }
      glyph
    }

    override def getGlyphs(run: GlyphRun, str: CharSequence, start: Int, end: Int, lastGlyph: Nullable[BitmapFont.Glyph]): Unit = {
      incrementalPacker.foreach(_.packToTexture = true) // All glyphs added after this are packed directly to the texture.
      super.getGlyphs(run, str, start, end, lastGlyph)
      if (dirty) {
        dirty = false
        parameter.foreach { param =>
          regions.foreach { r =>
            incrementalPacker.foreach(_.updateTextureRegions(r, param.minFilter, param.magFilter, param.genMipMaps))
          }
        }
      }
    }

    def close(): Unit = {
      stroker.foreach(_.close())
      incrementalPacker.foreach(_.close())
    }
  }

  /** Container for a glyph and its bitmap, used by generateGlyphAndBitmap. */
  class GlyphAndBitmap {
    var glyph:  BitmapFont.Glyph            = BitmapFont.Glyph()
    var bitmap: Nullable[FreeType.Bitmap] = Nullable.empty
  }
}
