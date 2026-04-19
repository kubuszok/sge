/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Layout.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 287
 * Covenant-baseline-methods: Layout,add,advances,appendInto,appendIntoDirect,appendSubstringInto,atLimit,baseColor,clear,count,countGlyphs,countGlyphsBeforeLine,e,ellipsis,font,getBaseColor,getEllipsis,getFont,getHeight,getJustification,getLine,getMaxLines,getTargetWidth,getWidth,glyphCount,h,i,i2,index,insertLine,justification,line,lineCount,lines,maxLines,n,offsets,peekLine,prev,pushLine,pushLineBare,reset,rotations,s,setBaseColor,setEllipsis,setFont,setJustification,setMaxLines,setTargetWidth,sizing,targetWidth,this,toString,truncateExtra
 * Covenant-source-reference: com/github/tommyettinger/textra/Layout.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.utils.Nullable

/** A replacement for libGDX's GlyphLayout; stores one or more (possibly empty) Lines of text. */
class Layout() {

  var font:          Nullable[Font]    = Nullable.empty
  val lines:         ArrayBuffer[Line] = ArrayBuffer(new Line())
  var maxLines:      Int               = Int.MaxValue
  var atLimit:       Boolean           = false
  var ellipsis:      Nullable[String]  = Nullable.empty
  var targetWidth:   Float             = 0f
  var baseColor:     Float             = Color.WHITE_FLOAT_BITS
  var justification: Justify           = Justify.NONE

  /** Contains two floats per glyph; even items are x offsets, odd items are y offsets. */
  val offsets: ArrayBuffer[Float] = ArrayBuffer.empty

  /** Contains two floats per glyph, as size multipliers. */
  val sizing: ArrayBuffer[Float] = ArrayBuffer.empty

  /** Contains one float per glyph; each is a rotation in degrees. */
  val rotations: ArrayBuffer[Float] = ArrayBuffer.empty

  /** Contains one float per glyph; each is a multiplier to the x-advance. */
  val advances: ArrayBuffer[Float] = ArrayBuffer.empty

  def this(font: Font) = {
    this()
    this.font = Nullable(font)
  }

  def this(other: Layout) = {
    this()
    this.font = other.font
    this.maxLines = other.maxLines
    this.atLimit = other.atLimit
    this.ellipsis = other.ellipsis
    this.targetWidth = other.targetWidth
    this.baseColor = other.baseColor
    this.lines.clear()
    var i = 0
    while (i < other.lineCount) {
      val ln = new Line()
      val o  = other.lines(i)
      ln.glyphs ++= o.glyphs
      lines += ln.size(o.width, o.height)
      i += 1
    }
    rotations ++= other.rotations
    offsets ++= other.offsets
    sizing ++= other.sizing
    advances ++= other.advances
    justification = other.justification
  }

  /** One of the ways to set the font on a Layout; this one returns this Layout for chaining. */
  def setFont(font: Font): Layout = {
    if (this.font.isEmpty || this.font.get != font) {
      this.font = Nullable(font)
      lines.clear()
      lines += new Line()
    }
    this
  }

  def getFont: Nullable[Font] = font

  /** Adds a long glyph as processed by Font to store color and style info with the char. */
  def add(glyph: Long): Layout = add(glyph, 1f, 1f, 0f, 0f, 0f)

  def add(glyph: Long, scale: Float, advance: Float, offsetX: Float, offsetY: Float, rotation: Float): Layout = boundary {
    if (!atLimit) {
      if ((glyph & 0xffffL) == 10L) {
        if (lines.size >= maxLines) {
          atLimit = true
          break(this)
        }
        val line = new Line()
        val prev = lines.last
        prev.glyphs += '\n'.toLong
        line.height = 0
        lines += line
      } else {
        lines.last.glyphs += glyph
      }
      sizing += scale += scale
      advances += advance
      offsets += offsetX += offsetY
      rotations += rotation
    }
    this
  }

  def clear(): Layout = {
    lines.clear()
    sizing.clear()
    advances.clear()
    offsets.clear()
    rotations.clear()
    lines += new Line()
    atLimit = false
    this
  }

  def getWidth: Float =
    if (justification != Justify.NONE && (lines.size > 1 && !justification.ignoreLastLine)) targetWidth
    else {
      var w = 0f
      var i = 0
      while (i < lines.size) { w = Math.max(w, lines(i).width); i += 1 }
      w
    }

  def getHeight: Float = {
    var h = 0f
    var i = 0
    while (i < lines.size) { h += lines(i).height; i += 1 }
    h
  }

  def lineCount: Int = lines.size

  def getLine(i: Int): Nullable[Line] =
    if (i >= lines.size) Nullable.empty else Nullable(lines(i))

  def peekLine: Line = lines.last

  def pushLine(): Nullable[Line] =
    if (lines.size >= maxLines) { atLimit = true; Nullable.empty }
    else {
      val line = new Line()
      if (advances.isEmpty) add('\n'.toLong)
      else add('\n'.toLong, sizing.last, advances.last, offsets(offsets.size - 2), offsets.last, rotations.last)
      line.height = 0
      lines += line
      Nullable(line)
    }

  def pushLineBare(): Nullable[Line] =
    if (lines.size >= maxLines) { atLimit = true; Nullable.empty }
    else {
      val line = new Line()
      line.height = 0
      lines += line
      Nullable(line)
    }

  def insertLine(index: Int): Nullable[Line] = boundary {
    if (lines.size >= maxLines) { atLimit = true; break(Nullable.empty) }
    if (index < 0 || index >= maxLines) break(Nullable.empty)
    val line = new Line()
    val prev = lines(index)
    prev.glyphs += '\n'.toLong
    line.height = 0
    lines.insert(index + 1, line)
    Nullable(line)
  }

  def getTargetWidth:            Float  = targetWidth
  def setTargetWidth(tw: Float): Layout = { targetWidth = tw; this }
  def getBaseColor:              Float  = baseColor
  def setBaseColor(bc:   Float): Layout = { baseColor = bc; this }
  def setBaseColor(bc: Color):   Layout = {
    baseColor = if (bc == null) Color.WHITE_FLOAT_BITS else bc.toFloatBits() // @nowarn — Java interop boundary
    this
  }
  def getMaxLines:                       Int              = maxLines
  def setMaxLines(ml: Int):              Layout           = { maxLines = Math.max(1, ml); this }
  def getEllipsis:                       Nullable[String] = ellipsis
  def setEllipsis(e:  Nullable[String]): Layout           = { ellipsis = e; this }
  def getJustification:                  Justify          = justification
  def setJustification(j: Justify):      Layout           = {
    justification = if (j == null) Justify.NONE else j // @nowarn — Java interop boundary
    this
  }

  def countGlyphs: Int = {
    var count = 0
    var i     = 0
    while (i < lines.size) { count += lines(i).glyphs.size; i += 1 }
    count
  }

  def countGlyphsBeforeLine(lineIndex: Int): Int = {
    var count = 0
    var i     = 0
    val n     = Math.min(lines.size, lineIndex)
    while (i < n) { count += lines(i).glyphs.size; i += 1 }
    count
  }

  def reset(): Unit = {
    clear()
    justification = Justify.NONE
    targetWidth = 0f
    baseColor = Color.WHITE_FLOAT_BITS
    maxLines = Int.MaxValue
    atLimit = false
    ellipsis = Nullable.empty
    font = Nullable.empty
  }

  def appendIntoDirect(sb: StringBuilder): StringBuilder = {
    var i = 0
    while (i < lines.size) {
      val line = lines(i)
      var j    = 0
      while (j < line.glyphs.size) {
        sb.append(line.glyphs(j).toChar)
        j += 1
      }
      i += 1
    }
    sb
  }

  def appendInto(sb: StringBuilder): StringBuilder =
    appendSubstringInto(sb, 0, Int.MaxValue)

  def appendSubstringInto(sb: StringBuilder, start: Int, end: Int): StringBuilder = boundary {
    val s          = Math.max(0, start)
    val e          = Math.min(Math.max(countGlyphs, s), end)
    var index      = s
    var glyphCount = 0
    var i          = 0
    while (i < lines.size && index >= 0) {
      val glyphs = lines(i).glyphs
      if (index < glyphs.size) {
        val fin = index - s - glyphCount + e
        while (index < fin && index < glyphs.size) {
          val c = glyphs(index).toChar
          if (c >= '\ue000' && c <= '\uf800') {
            Nullable.fold(font) {
              sb.append(c)
            } { f =>
              Nullable.fold(f.namesByCharCode) {
                sb.append(c)
              } { nbc =>
                nbc.get(c.toInt) match {
                  case Some(name) => sb.append(name)
                  case None       => sb.append(c)
                }
              }
            }
          } else {
            if (c == 2) sb.append('[') else sb.append(c)
          }
          glyphCount += 1
          index += 1
        }
        if (glyphCount == e - s) break(sb)
        index = 0
      } else {
        index -= glyphs.size
      }
      sb.append('\n')
      i += 1
    }
    sb
  }

  override def toString: String = appendInto(new StringBuilder).toString

  def truncateExtra(i: Int): Layout = {
    if (advances.size > i) advances.dropRightInPlace(advances.size - i)
    if (rotations.size > i) rotations.dropRightInPlace(rotations.size - i)
    val i2 = i << 1
    if (sizing.size > i2) sizing.dropRightInPlace(sizing.size - i2)
    if (offsets.size > i2) offsets.dropRightInPlace(offsets.size - i2)
    this
  }
}
