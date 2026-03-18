/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/BitmapFontCache.java
 * Original authors: Nathan Sweet, davebaol, Alexander Dorokhov
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: boundary/break for early returns; Nullable for null safety
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters → Scala property accessors (color, x, y, integerPositions, pageCount, vertices, vertexCount, layouts)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.Color
import sge.utils.{ DynamicArray, Nullable, Pool }
import sge.utils.NumberUtils
import scala.util.boundary
import scala.util.boundary.break

class BitmapFontCache(val font: BitmapFont, private var integer: Boolean) {
  import BitmapFontCache.tempColor

  private val _layouts      = DynamicArray[GlyphLayout]()
  private val pooledLayouts = new Pool.Default[GlyphLayout](() => GlyphLayout()) with Pool.Flushable[GlyphLayout]
  private var glyphCount: Int   = 0
  private var _x:         Float = 0f
  private var _y:         Float = 0f
  private val _color = Color(1, 1, 1, 1)
  private var currentTint: Float = 0f

  // Vertex data per page
  private var pageVertices: Array[Array[Float]] = Array.empty
  // Number of vertex data entries per page
  private var idx: Array[Int] = Array.empty
  // For each page, an array with a value for each glyph from that page
  private var pageGlyphIndices: Array[DynamicArray[Int]] = Array.empty
  // Used internally to ensure a correct capacity for multi-page font vertex data
  private var tempGlyphCount: Array[Int] = Array.empty

  def this(font: BitmapFont) = {
    this(font, font.integer)
  }

  // Initialize fields in the primary constructor
  private val _pageCount = font.regions.size
  if (_pageCount == 0) throw new IllegalArgumentException("The specified font must contain at least one texture page.")

  pageVertices = Array.ofDim[Float](_pageCount, 0)
  idx = Array.ofDim[Int](_pageCount)
  if (_pageCount > 1) {
    // Contains the indices of the glyph in the cache as they are added.
    pageGlyphIndices = Array.ofDim[DynamicArray[Int]](_pageCount)
    for (i <- 0 until _pageCount)
      pageGlyphIndices(i) = DynamicArray[Int]()
  }
  tempGlyphCount = Array.ofDim[Int](_pageCount)

  def setPosition(x: Float, y: Float): Unit =
    translate(x - this._x, y - this._y)

  def translate(xAmount: Float, yAmount: Float): Unit = boundary {
    if (xAmount == 0 && yAmount == 0) break()
    var adjXAmount = xAmount
    var adjYAmount = yAmount
    if (integer) {
      adjXAmount = Math.round(xAmount).toFloat
      adjYAmount = Math.round(yAmount).toFloat
    }
    _x += adjXAmount
    _y += adjYAmount

    val pageVerticesLocal = this.pageVertices
    for (i <- pageVerticesLocal.indices) {
      val vertices = pageVerticesLocal(i)
      var ii       = 0
      while (ii < idx(i)) {
        vertices(ii) += adjXAmount
        vertices(ii + 1) += adjYAmount
        ii += 5
      }
    }
  }

  def tint(tint: Color): Unit = boundary {
    val newTint = tint.toFloatBits()
    if (currentTint == newTint) break()
    currentTint = newTint

    val pageVerticesLocal   = this.pageVertices
    val tempGlyphCountLocal = this.tempGlyphCount
    for (i <- tempGlyphCountLocal.indices)
      tempGlyphCountLocal(i) = 0

    var i = 0
    while (i < _layouts.size) {
      val layout = _layouts(i)

      val colors              = layout.colors
      var colorsIndex         = 0
      var nextColorGlyphIndex = 0
      var glyphIndex          = 0
      var lastColorFloatBits  = 0f
      var ii                  = 0
      while (ii < layout.runs.size) {
        val run    = layout.runs(ii)
        val glyphs = run.glyphs
        var iii    = 0
        while (iii < glyphs.size) {
          if (glyphIndex == nextColorGlyphIndex) {
            colorsIndex += 1
            Color.abgr8888ToColor(tempColor, colors(colorsIndex))
            lastColorFloatBits = tempColor.mul(tint).toFloatBits()
            colorsIndex += 1
            nextColorGlyphIndex = if (colorsIndex < colors.size) colors(colorsIndex) else -1
          }
          val page   = glyphs(iii).page
          val offset = tempGlyphCountLocal(page) * 20 + 2
          tempGlyphCountLocal(page) += 1
          val vertices = pageVerticesLocal(page)
          vertices(offset) = lastColorFloatBits
          vertices(offset + 5) = lastColorFloatBits
          vertices(offset + 10) = lastColorFloatBits
          vertices(offset + 15) = lastColorFloatBits
          glyphIndex += 1
          iii += 1
        }
        ii += 1
      }
      i += 1
    }
  }

  def setAlphas(alpha: Float): Unit = {
    val alphaBits = (254 * alpha).toInt << 24
    var prev      = 0f
    var newColor  = 0f
    for (j <- pageVertices.indices) {
      val vertices = pageVertices(j)
      var i        = 2
      while (i < idx(j)) {
        val c = vertices(i)
        if (c == prev && i != 2) {
          vertices(i) = newColor
        } else {
          prev = c
          val rgba = NumberUtils.floatToIntColor(c)
          newColor = NumberUtils.intToFloatColor((rgba & 0x00ffffff) | alphaBits)
          vertices(i) = newColor
        }
        i += 5
      }
    }
  }

  def setColors(color: Float): Unit =
    for (j <- pageVertices.indices) {
      val vertices = pageVertices(j)
      var i        = 2
      while (i < idx(j)) {
        vertices(i) = color
        i += 5
      }
    }

  def setColors(tint: Color): Unit =
    setColors(tint.toFloatBits())

  def setColors(r: Float, g: Float, b: Float, a: Float): Unit = {
    val intBits = ((255 * a).toInt << 24) | ((255 * b).toInt << 16) | ((255 * g).toInt << 8) | (255 * r).toInt
    setColors(NumberUtils.intToFloatColor(intBits))
  }

  def setColors(tint: Color, start: Int, end: Int): Unit =
    setColors(tint.toFloatBits(), start, end)

  def setColors(color: Float, start: Int, end: Int): Unit = scala.util.boundary {
    if (pageVertices.length == 1) { // One page.
      val vertices = pageVertices(0)
      var i        = start * 20 + 2
      val n        = Math.min(end * 20, idx(0))
      while (i < n) {
        vertices(i) = color
        i += 5
      }
      scala.util.boundary.break()
    }

    val pageCount = pageVertices.length
    for (i <- 0 until pageCount) {
      val vertices     = pageVertices(i)
      val glyphIndices = pageGlyphIndices(i)
      // Loop through the indices and determine whether the glyph is inside begin/end.
      var j = 0
      while (j < glyphIndices.size && glyphIndices(j) < end) {
        val glyphIndex = glyphIndices(j)

        // If inside start and end, change its colour.
        if (glyphIndex >= start) { // && glyphIndex < end
          val offset = j * 20 + 2
          vertices(offset) = color
          vertices(offset + 5) = color
          vertices(offset + 10) = color
          vertices(offset + 15) = color
        }
        j += 1
      }
    }
  }

  def color: Color = _color

  def color_=(color: Color): Unit =
    this._color.set(color)

  def setColor(r: Float, g: Float, b: Float, a: Float): Unit =
    _color.set(r, g, b, a)

  def draw(spriteBatch: Batch): Unit = {
    val regions = font.regions
    for (j <- pageVertices.indices)
      if (idx(j) > 0) { // ignore if this texture has no glyphs
        val vertices = pageVertices(j)
        spriteBatch.draw(regions(j).texture, vertices, 0, idx(j))
      }
  }

  def draw(spriteBatch: Batch, start: Int, end: Int): Unit = scala.util.boundary {
    if (pageVertices.length == 1) { // 1 page.
      spriteBatch.draw(font.region.texture, pageVertices(0), start * 20, (end - start) * 20)
      scala.util.boundary.break()
    }

    // Determine vertex offset and count to render for each page. Some pages might not need to be rendered at all.
    val regions = font.regions
    for (i <- pageVertices.indices) {
      var offset = -1
      var count  = 0

      // For each set of glyph indices, determine where to begin within the start/end bounds.
      val glyphIndices = pageGlyphIndices(i)
      var ii           = 0
      while (ii < glyphIndices.size && glyphIndices(ii) < end) {
        val glyphIndex = glyphIndices(ii)

        // Determine if this glyph is within bounds. Use the first match of that for the offset.
        if (offset == -1 && glyphIndex >= start) offset = ii

        // Determine the vertex count by counting glyphs within bounds.
        if (glyphIndex >= start) count += 1
        ii += 1
      }

      // Page doesn't need to be rendered.
      if (offset != -1 && count != 0) {
        // Render the page vertex data with the offset and count.
        spriteBatch.draw(regions(i).texture, pageVertices(i), offset * 20, count * 20)
      }
    }
  }

  def draw(spriteBatch: Batch, alphaModulation: Float): Unit = scala.util.boundary {
    if (alphaModulation == 1) {
      draw(spriteBatch)
      scala.util.boundary.break()
    }
    val color    = this.color
    val oldAlpha = color.a
    color.a *= alphaModulation
    setColors(color)
    draw(spriteBatch)
    color.a = oldAlpha
    setColors(color)
  }

  def clear(): Unit = {
    _x = 0
    _y = 0
    pooledLayouts.flush()
    _layouts.clear()
    for (i <- idx.indices) {
      if (pageGlyphIndices.nonEmpty) pageGlyphIndices(i).clear()
      idx(i) = 0
    }
  }

  private def requireGlyphs(layout: GlyphLayout): Unit =
    if (pageVertices.length == 1) {
      // Simple if we just have one page.
      requirePageGlyphs(0, layout.glyphCount)
    } else {
      val tempGlyphCountLocal = this.tempGlyphCount
      for (i <- tempGlyphCountLocal.indices)
        tempGlyphCountLocal(i) = 0
      // Determine # of glyphs in each page.
      var i = 0
      while (i < layout.runs.size) {
        val glyphs = layout.runs(i).glyphs
        var ii     = 0
        while (ii < glyphs.size) {
          tempGlyphCountLocal(glyphs(ii).page) += 1
          ii += 1
        }
        i += 1
      }
      // Require that many for each page.
      for (i <- tempGlyphCountLocal.indices)
        requirePageGlyphs(i, tempGlyphCountLocal(i))
    }

  private def requirePageGlyphs(page: Int, glyphCount: Int): Unit = {
    if (pageGlyphIndices.nonEmpty) {
      if (glyphCount > pageGlyphIndices(page).size)
        pageGlyphIndices(page).ensureCapacity(glyphCount - pageGlyphIndices(page).size)
    }

    val vertexCount = idx(page) + glyphCount * 20
    val vertices    = pageVertices(page)
    if (Nullable(vertices).isEmpty) {
      pageVertices(page) = Array.ofDim[Float](vertexCount)
    } else if (vertices.length < vertexCount) {
      val newVertices = Array.ofDim[Float](vertexCount)
      System.arraycopy(vertices, 0, newVertices, 0, idx(page))
      pageVertices(page) = newVertices
    }
  }

  def setText(str: CharSequence, x: Float, y: Float): GlyphLayout = {
    clear()
    addText(str, x, y, 0, str.length(), 0, 0, false) // Align.left = 0
  }

  def setText(str: CharSequence, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout = {
    clear()
    addText(str, x, y, 0, str.length(), targetWidth, halign, wrap)
  }

  def setText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout = {
    clear()
    addText(str, x, y, start, end, targetWidth, halign, wrap)
  }

  def setText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean, truncate: Nullable[String]): GlyphLayout = {
    clear()
    addText(str, x, y, start, end, targetWidth, halign, wrap, truncate)
  }

  def setText(layout: GlyphLayout, x: Float, y: Float): Unit = {
    clear()
    addText(layout, x, y)
  }

  def addText(str: CharSequence, x: Float, y: Float): GlyphLayout =
    addText(str, x, y, 0, str.length(), 0, 0, false, Nullable.empty) // Align.left = 0

  def addText(str: CharSequence, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout =
    addText(str, x, y, 0, str.length(), targetWidth, halign, wrap, Nullable.empty)

  def addText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout =
    addText(str, x, y, start, end, targetWidth, halign, wrap, Nullable.empty)

  def addText(str: CharSequence, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int, wrap: Boolean, truncate: Nullable[String]): GlyphLayout = {
    val layout = pooledLayouts.obtain()
    layout.setText(font, str, start, end, _color, targetWidth, halign, wrap, truncate)
    addText(layout, x, y)
    layout
  }

  def addText(layout: GlyphLayout, x: Float, y: Float): Unit =
    addToCache(layout, x, y + font.data.ascent)

  private def addToCache(layout: GlyphLayout, x: Float, y: Float): Unit = boundary {
    val runCount = layout.runs.size
    if (runCount == 0) break()

    // Check if the number of font pages has changed.
    if (pageVertices.length < font.regions.size) setPageCount(font.regions.size)

    _layouts.add(layout)
    requireGlyphs(layout)

    val colors              = layout.colors
    var colorsIndex         = 0
    var nextColorGlyphIndex = 0
    var glyphIndex          = 0
    var lastColorFloatBits  = 0f
    for (i <- 0 until runCount) {
      val run    = layout.runs(i)
      val glyphs = run.glyphs
      val gx     = x + run.x
      val gy     = y + run.y
      var ii     = 0
      while (ii < glyphs.size) {
        if (glyphIndex == nextColorGlyphIndex) {
          colorsIndex += 1
          lastColorFloatBits = NumberUtils.intToFloatColor(colors(colorsIndex))
          colorsIndex += 1
          nextColorGlyphIndex = if (colorsIndex < colors.size) colors(colorsIndex) else -1
        }
        // gx += xAdvances[ii]
        addGlyph(glyphs(ii), gx, gy, lastColorFloatBits)
        glyphIndex += 1
        ii += 1
      }
    }

    // currentTint = Color.WHITE_FLOAT_BITS // Cached glyphs have changed, reset the current tint.
  }

  private def addGlyph(glyph: BitmapFont.Glyph, x: Float, y: Float, color: Float): Unit = {
    val scaleX    = font.data.scaleX
    val scaleY    = font.data.scaleY
    val adjustedX = x + glyph.xoffset * scaleX
    val adjustedY = y + glyph.yoffset * scaleY
    val width     = glyph.width * scaleX
    val height    = glyph.height * scaleY
    val u         = glyph.u
    val u2        = glyph.u2
    val v         = glyph.v
    val v2        = glyph.v2

    var finalX      = adjustedX
    var finalY      = adjustedY
    var finalWidth  = width
    var finalHeight = height
    if (integer) {
      finalX = Math.round(adjustedX).toFloat
      finalY = Math.round(adjustedY).toFloat
      finalWidth = Math.round(width).toFloat
      finalHeight = Math.round(height).toFloat
    }
    val x2 = finalX + finalWidth
    val y2 = finalY + finalHeight

    val page       = glyph.page
    var currentIdx = this.idx(page)
    this.idx(page) += 20

    if (pageGlyphIndices.nonEmpty) pageGlyphIndices(page).add(glyphCount)
    glyphCount += 1

    val vertices = pageVertices(page)
    vertices(currentIdx) = finalX; currentIdx += 1
    vertices(currentIdx) = finalY; currentIdx += 1
    vertices(currentIdx) = color; currentIdx += 1
    vertices(currentIdx) = u; currentIdx += 1
    vertices(currentIdx) = v; currentIdx += 1

    vertices(currentIdx) = finalX; currentIdx += 1
    vertices(currentIdx) = y2; currentIdx += 1
    vertices(currentIdx) = color; currentIdx += 1
    vertices(currentIdx) = u; currentIdx += 1
    vertices(currentIdx) = v2; currentIdx += 1

    vertices(currentIdx) = x2; currentIdx += 1
    vertices(currentIdx) = y2; currentIdx += 1
    vertices(currentIdx) = color; currentIdx += 1
    vertices(currentIdx) = u2; currentIdx += 1
    vertices(currentIdx) = v2; currentIdx += 1

    vertices(currentIdx) = x2; currentIdx += 1
    vertices(currentIdx) = finalY; currentIdx += 1
    vertices(currentIdx) = color; currentIdx += 1
    vertices(currentIdx) = u2; currentIdx += 1
    vertices(currentIdx) = v
  }

  private def setPageCount(pageCount: Int): Unit = {
    val newPageVertices = Array.ofDim[Array[Float]](pageCount)
    System.arraycopy(pageVertices, 0, newPageVertices, 0, pageVertices.length)
    pageVertices = newPageVertices

    val newIdx = Array.ofDim[Int](pageCount)
    System.arraycopy(idx, 0, newIdx, 0, idx.length)
    idx = newIdx

    val newPageGlyphIndices    = Array.ofDim[DynamicArray[Int]](pageCount)
    var pageGlyphIndicesLength = 0
    if (pageGlyphIndices.nonEmpty) {
      pageGlyphIndicesLength = pageGlyphIndices.length
      System.arraycopy(pageGlyphIndices, 0, newPageGlyphIndices, 0, pageGlyphIndices.length)
    }
    for (i <- pageGlyphIndicesLength until pageCount)
      newPageGlyphIndices(i) = DynamicArray[Int]()
    pageGlyphIndices = newPageGlyphIndices

    tempGlyphCount = Array.ofDim[Int](pageCount)
  }

  def x:                                Float                     = _x
  def y:                                Float                     = _y
  def integerPositions:                 Boolean                   = integer
  def integerPositions_=(use: Boolean): Unit                      = this.integer = use
  def pageCount:                        Int                       = pageVertices.length
  def vertices:                         Array[Float]              = pageVertices(0)
  def vertices(page:          Int):     Array[Float]              = pageVertices(page)
  def vertexCount(page:       Int):     Int                       = idx(page)
  def layouts:                          DynamicArray[GlyphLayout] = _layouts
}

object BitmapFontCache {
  private val tempColor: Color = Color(1, 1, 1, 1)
}
