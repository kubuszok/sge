/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/BitmapFont.java
 * Original authors: Nathan Sweet, Matthias Mann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; ownsTexture() -> hasOwnTexture()
 *   Convention: Nullable throughout; dispose() -> close(); using Sge context parameter
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: BitmapFontData.load(), setGlyphRegion(), getGlyphs(), getWrapIndex() fully implemented; added setScale, scale, getImagePath, isBreakChar
 *   Fixes: Java-style getters/setters → Scala property accessors (color, scaleX/Y, region, lineHeight, capHeight, descent, etc.)
 *   Fixes: Removed redundant getImagePaths()/getFontFile() (public fields); getFirstGlyph() → firstGlyph
 *   Issue: test: needs .fnt fixture file to test BitmapFontData.load end-to-end
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.io.{ BufferedReader, InputStreamReader }
import java.util.StringTokenizer
import java.util.regex.Pattern

import scala.util.boundary
import scala.util.boundary.break

import sge.files.FileHandle
import sge.graphics.{ Color, Texture }
import sge.graphics.g2d.TextureRegion
import sge.utils.{ DynamicArray, Nullable, SgeError }

import scala.language.implicitConversions

class BitmapFont(val data: BitmapFontData, regionsParam: Nullable[DynamicArray[TextureRegion]], val integer: Boolean)(using Sge) extends AutoCloseable {

  val regions:     DynamicArray[TextureRegion] = regionsParam.getOrElse(loadRegions())
  val cache:       BitmapFontCache             = newFontCache()
  val flipped:     Boolean                     = data.flipped
  var ownsTexture: Boolean                     = false

  // Secondary constructors that call the primary constructor
  def this(fontFile: FileHandle, region: Nullable[TextureRegion])(using Sge) = {
    this(
      BitmapFontData(fontFile, false),
      region.map { r =>
        val da = DynamicArray[TextureRegion](); da.add(r); da
      },
      true
    )
  }

  def this(fontFile: FileHandle, region: Nullable[TextureRegion], flip: Boolean)(using Sge) = {
    this(
      BitmapFontData(fontFile, flip),
      region.map { r =>
        val da = DynamicArray[TextureRegion](); da.add(r); da
      },
      true
    )
  }

  def this(fontFile: FileHandle)(using Sge) = {
    this(fontFile, Nullable.empty[TextureRegion])
  }

  def this(fontFile: FileHandle, flip: Boolean)(using Sge) = {
    this(BitmapFontData(fontFile, flip), Nullable.empty, true)
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean)(using Sge) = {
    this(
      BitmapFontData(fontFile, flip),
      Nullable { val da = DynamicArray[TextureRegion](); da.add(TextureRegion(Texture(imageFile, false))); da },
      true
    )
    ownsTexture = true
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean, integer: Boolean)(using Sge) = {
    this(
      BitmapFontData(fontFile, flip),
      Nullable { val da = DynamicArray[TextureRegion](); da.add(TextureRegion(Texture(imageFile, false))); da },
      integer
    )
    ownsTexture = true
  }

  private def loadRegions(): DynamicArray[TextureRegion] =
    data.imagePaths.fold {
      throw new IllegalArgumentException("If no regions are specified, the font data must have an images path.")
    } { paths =>
      val n          = paths.length
      val newRegions = DynamicArray[TextureRegion]()
      for (i <- 0 until n)
        newRegions.add(TextureRegion(Texture(paths(i))))
      ownsTexture = true
      newRegions
    }

  load(data)

  protected def load(data: BitmapFontData): Unit = {
    for (page <- data.glyphs if Nullable(page).isDefined)
      for (glyph <- page if Nullable(glyph).isDefined)
        data.setGlyphRegion(glyph, regions(glyph.page))
    data.missingGlyph.foreach(mg => data.setGlyphRegion(mg, regions(mg.page)))
  }

  def draw(batch: Batch, str: CharSequence, x: Float, y: Float): GlyphLayout = {
    cache.clear()
    val layout = cache.addText(str, x, y)
    cache.draw(batch)
    layout
  }

  def draw(batch: Batch, str: CharSequence, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout = {
    cache.clear()
    val layout = cache.addText(str, x, y, targetWidth, halign, wrap)
    cache.draw(batch)
    layout
  }

  def draw(batch: Batch, str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout = {
    cache.clear()
    val layout = cache.addText(str, x, y, start, end, targetWidth, halign, wrap)
    cache.draw(batch)
    layout
  }

  def draw(batch: Batch, str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean, truncate: Nullable[String]): GlyphLayout = {
    cache.clear()
    val layout = cache.addText(str, x, y, start, end, targetWidth, halign, wrap, truncate)
    cache.draw(batch)
    layout
  }

  def draw(batch: Batch, layout: GlyphLayout, x: Float, y: Float): Unit = {
    cache.clear()
    cache.addText(layout, x, y)
    cache.draw(batch)
  }

  def color:                 Color         = cache.color
  def color_=(color: Color): Unit          = cache.color.set(color)
  def scaleX:                Float         = data.scaleX
  def scaleY:                Float         = data.scaleY
  def region:                TextureRegion = regions.first
  def region(index:  Int):   TextureRegion = regions(index)
  def lineHeight:            Float         = data.lineHeight
  def spaceXadvance:         Float         = data.spaceXadvance
  def xHeight:               Float         = data.xHeight
  def capHeight:             Float         = data.capHeight
  def ascent:                Float         = data.ascent
  def descent:               Float         = data.descent

  def close(): Unit =
    if (ownsTexture) {
      var i = 0
      while (i < regions.size) {
        regions(i).texture.close()
        i += 1
      }
    }

  def setFixedWidthGlyphs(glyphs: CharSequence): Unit = {
    val fontData   = this.data
    var maxAdvance = 0
    for (index <- 0 until glyphs.length())
      fontData.getGlyph(glyphs.charAt(index)).foreach { g =>
        if (g.xadvance > maxAdvance) maxAdvance = g.xadvance
      }
    for (index <- 0 until glyphs.length())
      fontData.getGlyph(glyphs.charAt(index)).foreach { glyph =>
        glyph.xoffset += (maxAdvance - glyph.xadvance) / 2
        glyph.xadvance = maxAdvance
        glyph.kerning = Nullable.empty
        glyph.fixedWidth = true
      }
  }

  def integerPositions:                        Boolean = integer
  def integerPositions_=(useInteger: Boolean): Unit    = cache.integerPositions = useInteger

  def newFontCache(): BitmapFontCache = BitmapFontCache(this, integer)

  override def toString(): String = data.name.getOrElse(super.toString())
}

object BitmapFont {
  val LOG2_PAGE_SIZE = 9
  val PAGE_SIZE      = 1 << LOG2_PAGE_SIZE
  val PAGES          = 0x10000 / PAGE_SIZE

  def indexOf(text: CharSequence, ch: Char, start: Int): Int = boundary {
    val n = text.length()
    var i = start
    while (i < n) {
      if (text.charAt(i) == ch) break(i)
      i += 1
    }
    n
  }

  class Glyph {
    var id:         Int                          = 0
    var srcX:       Int                          = 0
    var srcY:       Int                          = 0
    var width:      Int                          = 0
    var height:     Int                          = 0
    var u:          Float                        = 0f
    var v:          Float                        = 0f
    var u2:         Float                        = 0f
    var v2:         Float                        = 0f
    var xoffset:    Int                          = 0
    var yoffset:    Int                          = 0
    var xadvance:   Int                          = 0
    var kerning:    Nullable[Array[Array[Byte]]] = Nullable.empty
    var fixedWidth: Boolean                      = false
    var page:       Int                          = 0

    def getKerning(ch: Char): Int =
      kerning.fold(0) { k =>
        val page = k(ch >>> LOG2_PAGE_SIZE)
        if (Nullable(page).isDefined) page(ch & PAGE_SIZE - 1)
        else 0
      }

    def setKerning(ch: Int, value: Int): Unit = {
      if (kerning.isEmpty) kerning = Nullable(Array.ofDim[Byte](PAGES, 0))
      kerning.foreach { k =>
        var page = k(ch >>> LOG2_PAGE_SIZE)
        if (Nullable(page).isEmpty) {
          k(ch >>> LOG2_PAGE_SIZE) = Array.ofDim[Byte](PAGE_SIZE)
          page = k(ch >>> LOG2_PAGE_SIZE)
        }
        page(ch & PAGE_SIZE - 1) = value.toByte
      }
    }

    override def toString(): String = Character.toString(id.toChar)
  }
}

class BitmapFontData(val fontFile: Nullable[FileHandle] = Nullable.empty, val flipped: Boolean = false) {
  var name:           Nullable[String]        = Nullable.empty
  var imagePaths:     Nullable[Array[String]] = Nullable.empty
  var padTop:         Float                   = 0f
  var padRight:       Float                   = 0f
  var padBottom:      Float                   = 0f
  var padLeft:        Float                   = 0f
  var lineHeight:     Float                   = 0f
  var capHeight:      Float                   = 1f
  var ascent:         Float                   = 0f
  var descent:        Float                   = 0f
  var down:           Float                   = 0f
  var blankLineScale: Float                   = 1f
  var scaleX:         Float                   = 1f
  var scaleY:         Float                   = 1f
  var markupEnabled:  Boolean                 = false
  var cursorX:        Float                   = 0f

  val glyphs:       Array[Array[BitmapFont.Glyph]] = Array.ofDim[BitmapFont.Glyph](BitmapFont.PAGES, 0)
  var missingGlyph: Nullable[BitmapFont.Glyph]     = Nullable.empty

  var spaceXadvance: Float = 0f
  var xHeight:       Float = 1f

  var breakChars: Nullable[Array[Char]] = Nullable.empty
  var xChars:     Array[Char]           = Array('x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z')
  var capChars:   Array[Char]           = Array('M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')

  // Load font data if fontFile is provided (matches Java constructor behavior)
  fontFile.foreach(f => load(f, flipped))

  def load(fontFile: FileHandle, flip: Boolean): Unit = {
    if (imagePaths.isDefined) throw new IllegalStateException("Already loaded.")

    name = Nullable(fontFile.nameWithoutExtension)

    val reader = new BufferedReader(new InputStreamReader(fontFile.read()), 512)
    try {
      val infoLine = Nullable(reader.readLine()).getOrElse { // info
        throw SgeError.InvalidInput("File is empty.")
      }

      val paddingStr = infoLine.substring(infoLine.indexOf("padding=") + 8)
      val padding    = paddingStr.substring(0, paddingStr.indexOf(' ')).split(",", 4)
      if (padding.length != 4) throw SgeError.InvalidInput("Invalid padding.")
      padTop = Integer.parseInt(padding(0)).toFloat
      padRight = Integer.parseInt(padding(1)).toFloat
      padBottom = Integer.parseInt(padding(2)).toFloat
      padLeft = Integer.parseInt(padding(3)).toFloat
      val padY = padTop + padBottom

      val commonLine = Nullable(reader.readLine()).getOrElse {
        throw SgeError.InvalidInput("Missing common header.")
      }
      val common = commonLine.split(" ", 9)

      if (common.length < 3) throw SgeError.InvalidInput("Invalid common header.")

      if (!common(1).startsWith("lineHeight=")) throw SgeError.InvalidInput("Missing: lineHeight")
      lineHeight = Integer.parseInt(common(1).substring(11)).toFloat

      if (!common(2).startsWith("base=")) throw SgeError.InvalidInput("Missing: base")
      val baseLine = Integer.parseInt(common(2).substring(5)).toFloat

      var pageCount = 1
      if (common.length >= 6 && Nullable(common(5)).isDefined && common(5).startsWith("pages=")) {
        try
          pageCount = Math.max(1, Integer.parseInt(common(5).substring(6)))
        catch {
          case _: NumberFormatException => // Use one page.
        }
      }

      imagePaths = Nullable(new Array[String](pageCount))

      // Read each page definition.
      for (p <- 0 until pageCount) {
        val pageLine = Nullable(reader.readLine()).getOrElse {
          throw SgeError.InvalidInput("Missing additional page definitions.")
        }

        var matcher = Pattern.compile(".*id=(\\d+)").matcher(pageLine)
        if (matcher.find()) {
          val id = matcher.group(1)
          try {
            val pageID = Integer.parseInt(id)
            if (pageID != p) throw SgeError.InvalidInput("Page IDs must be indices starting at 0: " + id)
          } catch {
            case ex: NumberFormatException =>
              throw SgeError.InvalidInput("Invalid page id: " + id, Some(ex))
          }
        }

        matcher = Pattern.compile(".*file=\"?([^\"]+)\"?").matcher(pageLine)
        if (!matcher.find()) throw SgeError.InvalidInput("Missing: file")
        val fileName = matcher.group(1)

        imagePaths.foreach(_(p) = fontFile.parent().child(fileName).path.replaceAll("\\\\", "/"))
      }
      descent = 0

      var lastLine: Nullable[String] = Nullable.empty
      boundary {
        while (true) {
          val line = Nullable(reader.readLine()).getOrElse(break())
          if (line.startsWith("kernings ") || line.startsWith("metrics ")) {
            lastLine = Nullable(line)
            break()
          } else if (line.startsWith("char ")) {
            val glyph = BitmapFont.Glyph()

            val tokens = new StringTokenizer(line, " =")
            tokens.nextToken()
            tokens.nextToken()
            val ch = Integer.parseInt(tokens.nextToken())
            if (ch <= 0) {
              missingGlyph = Nullable(glyph)
            } else if (ch <= Character.MAX_VALUE) {
              setGlyph(ch, glyph)
            } else {
              // skip — continue to next iteration
            }
            if (ch <= Character.MAX_VALUE) {
              glyph.id = ch
              tokens.nextToken()
              glyph.srcX = Integer.parseInt(tokens.nextToken())
              tokens.nextToken()
              glyph.srcY = Integer.parseInt(tokens.nextToken())
              tokens.nextToken()
              glyph.width = Integer.parseInt(tokens.nextToken())
              tokens.nextToken()
              glyph.height = Integer.parseInt(tokens.nextToken())
              tokens.nextToken()
              glyph.xoffset = Integer.parseInt(tokens.nextToken())
              tokens.nextToken()
              if (flip) {
                glyph.yoffset = Integer.parseInt(tokens.nextToken())
              } else {
                glyph.yoffset = -(glyph.height + Integer.parseInt(tokens.nextToken()))
              }
              tokens.nextToken()
              glyph.xadvance = Integer.parseInt(tokens.nextToken())

              // Check for page safely, it could be omitted or invalid.
              if (tokens.hasMoreTokens()) tokens.nextToken()
              if (tokens.hasMoreTokens()) {
                try
                  glyph.page = Integer.parseInt(tokens.nextToken())
                catch {
                  case _: NumberFormatException =>
                }
              }

              if (glyph.width > 0 && glyph.height > 0) descent = Math.min(baseLine + glyph.yoffset, descent)
            }
          }
        }
      }
      descent += padBottom

      // Parse kernings — lastLine carries the line that ended char parsing
      if (lastLine.exists(_.startsWith("kernings "))) {
        boundary {
          while (true) {
            val line = Nullable(reader.readLine()).getOrElse(break())
            if (!line.startsWith("kerning ")) {
              lastLine = Nullable(line)
              break()
            }
            val tokens = new StringTokenizer(line, " =")
            tokens.nextToken()
            tokens.nextToken()
            val first = Integer.parseInt(tokens.nextToken())
            tokens.nextToken()
            val second = Integer.parseInt(tokens.nextToken())
            if (first >= 0 && first <= Character.MAX_VALUE && second >= 0 && second <= Character.MAX_VALUE) {
              getGlyph(first.toChar).foreach { glyph =>
                tokens.nextToken()
                val amount = Integer.parseInt(tokens.nextToken())
                glyph.setKerning(second, amount)
              }
            }
          }
        }
      }

      var hasMetricsOverride = false
      var overrideAscent     = 0f
      var overrideDescent    = 0f
      var overrideDown       = 0f
      var overrideCapHeight  = 0f
      var overrideLineHeight = 0f
      var overrideSpaceXAdv  = 0f
      var overrideXHeight    = 0f

      // Metrics override
      lastLine.filter(_.startsWith("metrics ")).foreach { metricsLine =>
        hasMetricsOverride = true
        val tokens = new StringTokenizer(metricsLine, " =")
        tokens.nextToken()
        tokens.nextToken()
        overrideAscent = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideDescent = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideDown = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideCapHeight = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideLineHeight = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideSpaceXAdv = java.lang.Float.parseFloat(tokens.nextToken())
        tokens.nextToken()
        overrideXHeight = java.lang.Float.parseFloat(tokens.nextToken())
      }

      val spaceGlyph = getGlyph(' ').getOrElse {
        val sg = BitmapFont.Glyph()
        sg.id = ' '
        val xadvanceGlyph = getGlyph('l').getOrElse(firstGlyph)
        sg.xadvance = xadvanceGlyph.xadvance
        setGlyph(' ', sg)
        sg
      }
      if (spaceGlyph.width == 0) {
        spaceGlyph.width = (padLeft + spaceGlyph.xadvance + padRight).toInt
        spaceGlyph.xoffset = (-padLeft).toInt
      }
      spaceXadvance = spaceGlyph.xadvance.toFloat

      var xGlyph: Nullable[BitmapFont.Glyph] = Nullable.empty
      for (xChar <- xChars if xGlyph.isEmpty)
        xGlyph = getGlyph(xChar)
      val resolvedXGlyph = xGlyph.getOrElse(firstGlyph)
      xHeight = resolvedXGlyph.height - padY

      var capGlyph: Nullable[BitmapFont.Glyph] = Nullable.empty
      for (capChar <- capChars if capGlyph.isEmpty)
        capGlyph = getGlyph(capChar)
      capGlyph.fold {
        for (page <- this.glyphs if Nullable(page).isDefined)
          for (glyph <- page if Nullable(glyph).isDefined && glyph.height != 0 && glyph.width != 0)
            capHeight = Math.max(capHeight, glyph.height.toFloat)
      } { cg =>
        capHeight = cg.height.toFloat
      }
      capHeight -= padY

      ascent = baseLine - capHeight
      down = -lineHeight
      if (flip) {
        ascent = -ascent
        down = -down
      }

      if (hasMetricsOverride) {
        this.ascent = overrideAscent
        this.descent = overrideDescent
        this.down = overrideDown
        this.capHeight = overrideCapHeight
        this.lineHeight = overrideLineHeight
        this.spaceXadvance = overrideSpaceXAdv
        this.xHeight = overrideXHeight
      }

    } catch {
      case ex: Exception =>
        throw SgeError.InvalidInput("Error loading font file: " + fontFile, Some(ex))
    } finally
      try reader.close()
      catch {
        case e: Error     => throw e // Never swallow OOM, SOE, etc.
        case _: Exception => // Intentionally ignored — matches StreamUtils.closeQuietly behavior
      }
  }

  def setGlyphRegion(glyph: BitmapFont.Glyph, region: TextureRegion): Unit = {
    val texture      = region.texture
    val invTexWidth  = 1.0f / texture.width.toFloat
    val invTexHeight = 1.0f / texture.height.toFloat

    var offsetX      = 0f
    var offsetY      = 0f
    val u            = region.u
    val v            = region.v
    val regionWidth  = region.regionWidth.toFloat
    val regionHeight = region.regionHeight.toFloat
    region match {
      case atlasRegion: TextureAtlas.AtlasRegion =>
        offsetX = atlasRegion.offsetX
        offsetY = atlasRegion.originalHeight - atlasRegion.packedHeight - atlasRegion.offsetY
      case _ =>
    }

    var x  = glyph.srcX.toFloat
    var x2 = (glyph.srcX + glyph.width).toFloat
    var y  = glyph.srcY.toFloat
    var y2 = (glyph.srcY + glyph.height).toFloat

    // Shift glyph for left and top edge stripped whitespace. Clip glyph for right and bottom edge stripped whitespace.
    if (offsetX > 0) {
      x -= offsetX
      if (x < 0) {
        glyph.width += x.toInt
        glyph.xoffset -= x.toInt
        x = 0
      }
      x2 -= offsetX
      if (x2 > regionWidth) {
        glyph.width -= (x2 - regionWidth).toInt
        x2 = regionWidth
      }
    }
    if (offsetY > 0) {
      y -= offsetY
      if (y < 0) {
        glyph.height += y.toInt
        if (glyph.height < 0) glyph.height = 0
        y = 0
      }
      y2 -= offsetY
      if (y2 > regionHeight) {
        val amount = y2 - regionHeight
        glyph.height -= amount.toInt
        glyph.yoffset += amount.toInt
        y2 = regionHeight
      }
    }

    glyph.u = u + x * invTexWidth
    glyph.u2 = u + x2 * invTexWidth
    if (flipped) {
      glyph.v = v + y * invTexHeight
      glyph.v2 = v + y2 * invTexHeight
    } else {
      glyph.v2 = v + y * invTexHeight
      glyph.v = v + y2 * invTexHeight
    }
  }

  def getGlyph(ch: Char): Nullable[BitmapFont.Glyph] = {
    val page = glyphs(ch / BitmapFont.PAGE_SIZE)
    if (Nullable(page).isDefined) page(ch & BitmapFont.PAGE_SIZE - 1) else Nullable.empty
  }

  def setLineHeight(height: Float): Unit = {
    lineHeight = height * scaleY
    down = if (flipped) lineHeight else -lineHeight
  }

  def setGlyph(ch: Int, glyph: BitmapFont.Glyph): Unit = {
    var page = glyphs(ch / BitmapFont.PAGE_SIZE)
    if (Nullable(page).isEmpty) {
      glyphs(ch / BitmapFont.PAGE_SIZE) = Array.ofDim[BitmapFont.Glyph](BitmapFont.PAGE_SIZE)
      page = glyphs(ch / BitmapFont.PAGE_SIZE)
    }
    page(ch & BitmapFont.PAGE_SIZE - 1) = glyph
  }

  def firstGlyph: BitmapFont.Glyph = scala.util.boundary {
    for (page <- glyphs if Nullable(page).isDefined)
      for (glyph <- page if Nullable(glyph).isDefined && glyph.height != 0 && glyph.width != 0)
        scala.util.boundary.break(glyph)
    throw new RuntimeException("No valid glyph found")
  }

  def hasGlyph(ch: Char): Boolean =
    missingGlyph.isDefined || getGlyph(ch).isDefined

  def getGlyphs(run: GlyphRun, str: CharSequence, start: Int, end: Int, lastGlyph: Nullable[BitmapFont.Glyph]): Unit = boundary {
    val max = end - start
    if (max == 0) break()
    val markupEnabledLocal = this.markupEnabled
    val scaleXLocal        = this.scaleX
    val glyphsArray        = run.glyphs
    val xAdvances          = run.xAdvances

    glyphsArray.ensureCapacity(max)
    xAdvances.ensureCapacity(max + 1)

    var currentLastGlyph = lastGlyph
    var i                = start
    while (i < end) {
      val ch = str.charAt(i)
      i += 1
      if (ch != '\r') {
        var glyph = getGlyph(ch)
        if (glyph.isEmpty) {
          if (missingGlyph.isEmpty) {
            // skip
          } else {
            glyph = missingGlyph
          }
        }
        glyph.foreach { g =>
          glyphsArray += g
          xAdvances += (
            if (currentLastGlyph.isEmpty) {
              if (g.fixedWidth) 0f else -g.xoffset * scaleXLocal - padLeft
            } else {
              (currentLastGlyph.getOrElse(g).xadvance + currentLastGlyph.getOrElse(g).getKerning(ch)) * scaleXLocal
            }
          )
          currentLastGlyph = Nullable(g)
        }

        // "[[" is an escaped left square bracket, skip second character.
        if (markupEnabledLocal && ch == '[' && i < end && str.charAt(i) == '[') i += 1
      }
    }
    currentLastGlyph.foreach { lg =>
      val lastGlyphWidth =
        if (lg.fixedWidth) lg.xadvance * scaleXLocal
        else (lg.width + lg.xoffset) * scaleXLocal - padRight
      xAdvances += lastGlyphWidth
    }
  }

  def getWrapIndex(glyphs: DynamicArray[BitmapFont.Glyph], start: Int): Int = boundary {
    var i  = start - 1
    val ch = glyphs(i).id.toChar
    if (isWhitespace(ch)) break(i)
    if (isBreakChar(ch)) i -= 1
    while (i > 0) {
      val c = glyphs(i).id.toChar
      if (isWhitespace(c) || isBreakChar(c)) break(i + 1)
      i -= 1
    }
    0
  }

  def isBreakChar(c: Char): Boolean =
    breakChars.exists(_.contains(c))

  def isWhitespace(ch: Char): Boolean =
    ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'

  def getImagePath(index: Int): Nullable[String] =
    imagePaths.map(_(index))

  def setScale(scaleX: Float, scaleY: Float): Unit = {
    if (scaleX == 0) throw new IllegalArgumentException("scaleX cannot be 0.")
    if (scaleY == 0) throw new IllegalArgumentException("scaleY cannot be 0.")
    val x = scaleX / this.scaleX
    val y = scaleY / this.scaleY
    lineHeight *= y
    spaceXadvance *= x
    xHeight *= y
    capHeight *= y
    ascent *= y
    descent *= y
    down *= y
    padLeft *= x
    padRight *= x
    padTop *= y
    padBottom *= y
    this.scaleX = scaleX
    this.scaleY = scaleY
  }

  def setScale(scaleXY: Float): Unit =
    setScale(scaleXY, scaleXY)

  def scale(amount: Float): Unit =
    setScale(scaleX + amount, scaleY + amount)

  override def toString(): String = name.getOrElse(super.toString())
}
