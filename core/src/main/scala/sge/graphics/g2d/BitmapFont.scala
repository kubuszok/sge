/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/BitmapFont.java
 * Original authors: Nathan Sweet, Matthias Mann
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.files.FileHandle
import sge.graphics.{ Color, Texture }
import sge.graphics.g2d.TextureRegion
import sge.utils.{ Nullable, SgeError }
import sge.utils.Pool.Poolable
import scala.collection.mutable.ArrayBuffer
import java.io.{ BufferedReader, InputStreamReader }
import java.util.StringTokenizer

import scala.language.implicitConversions

class BitmapFont(val data: BitmapFontData, regionsParam: Nullable[ArrayBuffer[TextureRegion]], val integer: Boolean)(using sge: Sge) extends AutoCloseable {

  val regions:             ArrayBuffer[TextureRegion] = if (regionsParam.isDefined) regionsParam.orNull else loadRegions()
  private val cache:       BitmapFontCache            = newFontCache()
  private val flipped:     Boolean                    = data.flipped
  private var ownsTexture: Boolean                    = false

  // Secondary constructors that call the primary constructor
  def this(fontFile: FileHandle, region: Nullable[TextureRegion])(using sge: Sge) = {
    this(new BitmapFontData(fontFile, false), if (region.isDefined) Nullable(ArrayBuffer(region.orNull)) else Nullable.empty, true)
  }

  def this(fontFile: FileHandle, region: Nullable[TextureRegion], flip: Boolean)(using sge: Sge) = {
    this(new BitmapFontData(fontFile, flip), if (region.isDefined) Nullable(ArrayBuffer(region.orNull)) else Nullable.empty, true)
  }

  def this(fontFile: FileHandle)(using sge: Sge) = {
    this(fontFile, Nullable.empty[TextureRegion])
  }

  def this(fontFile: FileHandle, flip: Boolean)(using sge: Sge) = {
    this(new BitmapFontData(fontFile, flip), Nullable.empty, true)
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean)(using sge: Sge) = {
    this(
      new BitmapFontData(fontFile, flip),
      Nullable(ArrayBuffer(new TextureRegion(new Texture(imageFile, false)))),
      true
    )
    ownsTexture = true
  }

  def this(fontFile: FileHandle, imageFile: FileHandle, flip: Boolean, integer: Boolean)(using sge: Sge) = {
    this(
      new BitmapFontData(fontFile, flip),
      Nullable(ArrayBuffer(new TextureRegion(new Texture(imageFile, false)))),
      integer
    )
    ownsTexture = true
  }

  private def loadRegions()(using sge: Sge): ArrayBuffer[TextureRegion] = {
    if (data.imagePaths == null)
      throw new IllegalArgumentException("If no regions are specified, the font data must have an images path.")

    val n          = data.imagePaths.length
    val newRegions = ArrayBuffer[TextureRegion]()
    for (i <- 0 until n)
      newRegions += new TextureRegion(new Texture(data.imagePaths(i)))
    ownsTexture = true
    newRegions
  }

  load(data)

  protected def load(data: BitmapFontData): Unit = {
    for (page <- data.glyphs if page != null)
      for (glyph <- page if glyph != null)
        data.setGlyphRegion(glyph, regions(glyph.page))
    if (data.missingGlyph.isDefined) data.setGlyphRegion(data.missingGlyph.orNull, regions(data.missingGlyph.orNull.page))
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

  def getColor():                                            Color                      = cache.getColor()
  def setColor(color:  Color):                               Unit                       = cache.getColor().set(color)
  def setColor(r:      Float, g: Float, b: Float, a: Float): Unit                       = cache.getColor().set(r, g, b, a)
  def getScaleX():                                           Float                      = data.scaleX
  def getScaleY():                                           Float                      = data.scaleY
  def getRegion():                                           TextureRegion              = regions.head
  def getRegions():                                          ArrayBuffer[TextureRegion] = regions
  def getRegion(index: Int):                                 TextureRegion              = regions(index)
  def getLineHeight():                                       Float                      = data.lineHeight
  def getSpaceXadvance():                                    Float                      = data.spaceXadvance
  def getXHeight():                                          Float                      = data.xHeight
  def getCapHeight():                                        Float                      = data.capHeight
  def getAscent():                                           Float                      = data.ascent
  def getDescent():                                          Float                      = data.descent
  def isFlipped():                                           Boolean                    = flipped

  def close(): Unit =
    if (ownsTexture) {
      for (i <- regions.indices) {
        // regions(i).getTexture().close() // Close method not available
      }
    }

  def setFixedWidthGlyphs(glyphs: CharSequence): Unit = {
    val fontData   = this.data
    var maxAdvance = 0
    for (index <- 0 until glyphs.length()) {
      val g = fontData.getGlyph(glyphs.charAt(index))
      if (g.isDefined && g.orNull.xadvance > maxAdvance) maxAdvance = g.orNull.xadvance
    }
    for (index <- 0 until glyphs.length()) {
      val g = fontData.getGlyph(glyphs.charAt(index))
      if (g.isDefined) {
        val glyph = g.orNull
        glyph.xoffset += (maxAdvance - glyph.xadvance) / 2
        glyph.xadvance = maxAdvance
        glyph.kerning = null
        glyph.fixedWidth = true
      }
    }
  }

  def setUseIntegerPositions(useInteger: Boolean): Unit =
    cache.setUseIntegerPositions(useInteger)

  def usesIntegerPositions(): Boolean = integer

  def getCache(): BitmapFontCache = cache

  def getData(): BitmapFontData = data

  def hasOwnTexture(): Boolean = ownsTexture

  def setOwnsTexture(owns: Boolean): Unit =
    this.ownsTexture = owns

  def newFontCache(): BitmapFontCache = new BitmapFontCache(this, integer)

  override def toString(): String = data.name.getOrElse(super.toString())
}

object BitmapFont {
  val LOG2_PAGE_SIZE = 9
  val PAGE_SIZE      = 1 << LOG2_PAGE_SIZE
  val PAGES          = 0x10000 / PAGE_SIZE

  def indexOf(text: CharSequence, ch: Char, start: Int): Int = {
    val n = text.length()
    var i = start
    while (i < n) {
      if (text.charAt(i) == ch) return i
      i += 1
    }
    n
  }

  class Glyph {
    var id:         Int                = 0
    var srcX:       Int                = 0
    var srcY:       Int                = 0
    var width:      Int                = 0
    var height:     Int                = 0
    var u:          Float              = 0f
    var v:          Float              = 0f
    var u2:         Float              = 0f
    var v2:         Float              = 0f
    var xoffset:    Int                = 0
    var yoffset:    Int                = 0
    var xadvance:   Int                = 0
    var kerning:    Array[Array[Byte]] = null
    var fixedWidth: Boolean            = false
    var page:       Int                = 0

    def getKerning(ch: Char): Int = {
      if (kerning != null) {
        val page = kerning(ch >>> LOG2_PAGE_SIZE)
        if (page != null) return page(ch & PAGE_SIZE - 1)
      }
      0
    }

    def setKerning(ch: Int, value: Int): Unit = {
      if (kerning == null) kerning = Array.ofDim[Byte](PAGES, 0)
      var page = kerning(ch >>> LOG2_PAGE_SIZE)
      if (page == null) {
        kerning(ch >>> LOG2_PAGE_SIZE) = Array.ofDim[Byte](PAGE_SIZE)
        page = kerning(ch >>> LOG2_PAGE_SIZE)
      }
      page(ch & PAGE_SIZE - 1) = value.toByte
    }

    override def toString(): String = Character.toString(id.toChar)
  }
}

class BitmapFontData(val fontFile: Nullable[FileHandle] = Nullable.empty, val flipped: Boolean = false) {
  var name:           Nullable[String] = Nullable.empty
  var imagePaths:     Array[String]    = null
  var padTop:         Float            = 0f
  var padRight:       Float            = 0f
  var padBottom:      Float            = 0f
  var padLeft:        Float            = 0f
  var lineHeight:     Float            = 0f
  var capHeight:      Float            = 1f
  var ascent:         Float            = 0f
  var descent:        Float            = 0f
  var down:           Float            = 0f
  var blankLineScale: Float            = 1f
  var scaleX:         Float            = 1f
  var scaleY:         Float            = 1f
  var markupEnabled:  Boolean          = false
  var cursorX:        Float            = 0f

  val glyphs:       Array[Array[BitmapFont.Glyph]] = Array.ofDim[BitmapFont.Glyph](BitmapFont.PAGES, 0)
  var missingGlyph: Nullable[BitmapFont.Glyph]     = Nullable.empty

  var spaceXadvance: Float = 0f
  var xHeight:       Float = 1f

  var breakChars: Array[Char] = null
  var xChars:     Array[Char] = Array('x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z')
  var capChars:   Array[Char] = Array('M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')

  def load(fontFile: FileHandle, flip: Boolean): Unit = {
    // Implementation would go here - simplified for now
  }

  def setGlyphRegion(glyph: BitmapFont.Glyph, region: TextureRegion): Unit = {
    // Implementation would go here - simplified for now
  }

  def getGlyph(ch: Char): Nullable[BitmapFont.Glyph] = {
    val page = glyphs(ch / BitmapFont.PAGE_SIZE)
    if (page != null) page(ch & BitmapFont.PAGE_SIZE - 1) else Nullable.empty
  }

  def setLineHeight(height: Float): Unit = {
    lineHeight = height * scaleY
    down = if (flipped) lineHeight else -lineHeight
  }

  def setGlyph(ch: Int, glyph: BitmapFont.Glyph): Unit = {
    var page = glyphs(ch / BitmapFont.PAGE_SIZE)
    if (page == null) {
      glyphs(ch / BitmapFont.PAGE_SIZE) = Array.ofDim[BitmapFont.Glyph](BitmapFont.PAGE_SIZE)
      page = glyphs(ch / BitmapFont.PAGE_SIZE)
    }
    page(ch & BitmapFont.PAGE_SIZE - 1) = glyph
  }

  def getFirstGlyph(): BitmapFont.Glyph = scala.util.boundary {
    for (page <- glyphs if page != null)
      for (glyph <- page if glyph != null && glyph.height != 0 && glyph.width != 0)
        scala.util.boundary.break(glyph)
    throw new RuntimeException("No valid glyph found")
  }

  def hasGlyph(ch: Char): Boolean =
    missingGlyph.isDefined || getGlyph(ch).isDefined

  // Missing methods that need to be implemented
  def getGlyphs(run: GlyphRun, str: CharSequence, start: Int, end: Int, lastGlyph: BitmapFont.Glyph): Unit =
    // Simplified implementation
    for (i <- start until end) {
      val ch    = str.charAt(i)
      val glyph = getGlyph(ch).getOrElse(missingGlyph.orNull)
      if (glyph != null) {
        run.glyphs += glyph
        run.xAdvances += glyph.xadvance.toFloat
      }
    }

  def getWrapIndex(glyphs: ArrayBuffer[BitmapFont.Glyph], start: Int): Int = {
    // Simplified implementation - find next whitespace
    var i = start
    while (i < glyphs.size && !isWhitespace(glyphs(i).id.toChar))
      i += 1
    i
  }

  def isWhitespace(ch: Char): Boolean =
    ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'

  override def toString(): String = name.getOrElse(super.toString())
}
