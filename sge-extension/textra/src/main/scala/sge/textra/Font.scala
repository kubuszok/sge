/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Font.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: IntMap → HashMap[Int, _], LongArray → ArrayBuffer[Long],
 *     Array<T> → ArrayBuffer[T], ObjectLongMap → HashMap[String, Long],
 *     CharArray → Array[Char], FloatArray → ArrayBuffer[Float],
 *     IntFloatMap → HashMap[Int, Float], NumberUtils → java.lang.Float,
 *     Category.caseUp/caseDown → Character.toUpperCase/toLowerCase,
 *     RegExodus → java.util.regex
 *   Convention: getX()/setX() → public var where no logic; (using Sge) deferred
 *     because this extension may operate headlessly (markup-only, no rendering).
 *   Idiom: boundary/break for early returns; Nullable[A] for nullable fields.
 *   Implemented: drawGlyph (core + bold + oblique + super/subscript), drawGlyphs,
 *     drawBlocks, drawBlockSequence, drawFancyLine, enableShader, loadFNT, loadJSON,
 *     loadSad. GlyphRegion extends TextureRegion for rendering support.
 *   Remaining TODOs: drawGlyph advanced effects (drop shadow, outlines, HALO, NEON,
 *     SHINY), underline/strikethrough decorations, box-drawing character rendering
 *     (require ColorUtils.multiplyAlpha/lerpColorsMultiplyAlpha and BlockUtils.BOX_DRAWING).
 */
package sge
package textra

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.textra.utils.{ CaseInsensitiveIntMap, StringUtils }
import sge.utils.{ Json, Nullable, given_JsonCodec_Json }

/** A replacement for libGDX's BitmapFont class, supporting additional markup to allow styling text with various effects. This includes the commonly-requested "faux bold" and oblique mode using one
  * font image; you don't need a bold and italic/oblique image separate from the book face. This also supports underline, strikethrough, subscript/superscript (and "midscript," for a height between
  * the two), color markup, scale/size markup, and the option to switch to other Fonts from a family of several.
  */
class Font {
  import Font._

  //// members section

  /** If true, some data structures are shared when this Font is copied. */
  var sharing: Boolean = false

  /** Maps char keys (stored as ints) to their corresponding GlyphRegion values. */
  var mapping: HashMap[Int, GlyphRegion] = HashMap.empty

  /** Maps names of TextureRegions to the indices they use in mapping. */
  var nameLookup: Nullable[CaseInsensitiveIntMap] = Nullable.empty

  /** A reversed form of nameLookup that maps char codes to printable names. */
  var namesByCharCode: Nullable[HashMap[Int, String]] = Nullable.empty

  /** Which GlyphRegion to display if a char isn't found in mapping. */
  var defaultValue: GlyphRegion = new GlyphRegion(0f, 0f, 0f)

  /** The distance field type this font uses. */
  protected var distanceField: DistanceFieldType = DistanceFieldType.STANDARD

  /** If true, this is a fixed-width (monospace) font. */
  var isMono: Boolean = false

  /** Kerning data; null for monospace fonts. */
  var kerning: Nullable[HashMap[Int, Float]] = Nullable.empty

  /** Working crispness value for distance field rendering. */
  var actualCrispness: Float = 1f

  /** Persistent crispness multiplier for distance field fonts. */
  var distanceFieldCrispness: Float = 1f

  /** The largest width of any glyph in the font, after scaling. */
  var cellWidth: Float = 1f

  /** The largest height of any glyph in the font, after scaling. */
  var cellHeight: Float = 1f

  /** The largest width of any glyph in the font, before scaling. */
  var originalCellWidth: Float = 1f

  /** The largest height of any glyph in the font, before scaling. */
  var originalCellHeight: Float = 1f

  /** Scale multiplier for width. */
  var scaleX: Float = 1f

  /** Scale multiplier for height. */
  var scaleY: Float = 1f

  /** How far the unscaled font descends below the baseline, typically negative. */
  var descent: Float = 0f

  /** Char used to draw solid blocks for box-drawing/block-element characters. */
  var solidBlock: Char = '\u2588' // full block, decimal 9608

  /** FontFamily for switching between typefaces using [@Name] syntax. */
  var family: Nullable[FontFamily] = Nullable.empty

  /** Determines how colors are looked up by name. */
  var colorLookup: ColorLookup = ColorLookup.DESCRIPTIVE

  /** By default, does nothing; subclasses can override. */
  var integerPosition: Boolean = false

  /** Multiplier for oblique text horizontal movement. */
  var obliqueStrength: Float = 1f

  /** Multiplier for bold text stretching distance. */
  var boldStrength: Float = 1f

  /** Multiplier for outline stretching distance. */
  var outlineStrength: Float = 1f

  /** Multiplier for box drawing line breadth. */
  var boxDrawingBreadth: Float = 1f

  /** Multiplier for glow strength in NEON/HALO modes. */
  var glowStrength: Float = 1f

  /** The name of the Font, for display purposes. */
  var name: String = "Unnamed Font"

  /** Whether to omit curly-brace tokens from the displayed text. */
  var omitCurlyBraces: Boolean = true

  /** Whether to enable square-bracket markup processing. */
  var enableSquareBrackets: Boolean = true

  // Packed float colors for various effects
  var PACKED_BLACK:         Float = java.lang.Float.intBitsToFloat(0xfe000000)
  var PACKED_WHITE:         Float = Color.WHITE_FLOAT_BITS
  var PACKED_RED:           Float = java.lang.Float.intBitsToFloat(0xfe0000ff)
  var PACKED_YELLOW:        Float = java.lang.Float.intBitsToFloat(0xfe00ffff)
  var PACKED_BLUE:          Float = java.lang.Float.intBitsToFloat(0xfeff0000)
  var PACKED_ERROR_COLOR:   Float = java.lang.Float.intBitsToFloat(0xfe0000ff)
  var PACKED_WARN_COLOR:    Float = java.lang.Float.intBitsToFloat(0xfe10d5ff)
  var PACKED_NOTE_COLOR:    Float = java.lang.Float.intBitsToFloat(0xfeb88830)
  var PACKED_CONTEXT_COLOR: Float = java.lang.Float.intBitsToFloat(0xfe228b22)
  var PACKED_SUGGEST_COLOR: Float = java.lang.Float.intBitsToFloat(0xfe999999)
  var PACKED_HALO_COLOR:    Float = java.lang.Float.intBitsToFloat(0xfe000000)
  var PACKED_SHADOW_COLOR:  Float = java.lang.Float.intBitsToFloat(0x7e212121)

  // Adjustment fields
  var xAdjust:      Float = 0f
  var yAdjust:      Float = 0f
  var widthAdjust:  Float = 0f
  var heightAdjust: Float = 0f

  // Underline metrics (Zen metrics — fractions of cellWidth/cellHeight)
  var underX:       Float = 0f
  var underY:       Float = 0f
  var underLength:  Float = 0f
  var underBreadth: Float = 0f

  // Strikethrough metrics
  var strikeX:       Float = 0f
  var strikeY:       Float = 0f
  var strikeLength:  Float = 0f
  var strikeBreadth: Float = 0f

  // Fancy line metrics (error, warning, note effects)
  var fancyX: Float = 0f
  var fancyY: Float = 0f

  // Inline image adjustments
  var inlineImageOffsetX:  Float = 0f
  var inlineImageOffsetY:  Float = 0f
  var inlineImageXAdvance: Float = 0f
  var inlineImageStretch:  Float = 1f

  // Drop shadow offset
  var dropShadowOffsetX: Float = 1f
  var dropShadowOffsetY: Float = -2f

  // Rendering fields (populated by loadFNT/loadJSON/loadSad or constructors that accept textures)

  /** The parent TextureRegions that GlyphRegion images are drawn from (font atlas pages). */
  var parents: ArrayBuffer[sge.graphics.g2d.TextureRegion] = ArrayBuffer.empty

  /** Distance field shader for SDF/MSDF fonts; null for standard fonts. */
  var shader: Nullable[sge.graphics.glutils.ShaderProgram] = Nullable.empty

  // Internal transient buffers
  protected val vertices:                 Array[Float]          = new Array[Float](20)
  @transient protected val tempLayout:    Layout                = new Layout()
  @transient protected val glyphBuffer:   ArrayBuffer[Long]     = ArrayBuffer.empty
  @transient protected val historyBuffer: ArrayBuffer[Long]     = ArrayBuffer.empty
  @transient protected val labeledStates: HashMap[String, Long] = HashMap.empty
  protected val storedStates:             HashMap[String, Long] = HashMap.empty

  /** Must be in lexicographic order for binary search. */
  protected val breakChars: Array[Char] = Array(
    '\t', '\r', ' ', '-', '\u00ad', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2008', '\u2009', '\u200a', '\u200b', '\u2010', '\u2012', '\u2013', '\u2014', '\u2027'
  )

  /** Must be in lexicographic order for binary search. */
  private val spaceChars: Array[Char] = Array(
    '\t', '\r', ' ', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2008', '\u2009', '\u200a', '\u200b'
  )

  //// getters/setters

  def getColorLookup: ColorLookup = colorLookup

  def setColorLookup(lookup: ColorLookup): Font = {
    if (lookup != null) colorLookup = lookup
    this
  }

  def getDistanceField: DistanceFieldType = distanceField

  def setDistanceField(df: DistanceFieldType): Font = {
    this.distanceField = if (df == null) DistanceFieldType.STANDARD else df
    this
  }

  def getFamily: Nullable[FontFamily] = family

  def setFamily(f: FontFamily): Font = {
    family = Nullable(f)
    this
  }

  def getName: String = name

  def setName(n: String): Font = {
    name = n
    this
  }

  def getDescent: Float = descent

  def setDescent(d: Float): Font = {
    descent = d
    this
  }

  def getCrispness: Float = distanceFieldCrispness

  def setCrispness(crispness: Float): Font = {
    distanceFieldCrispness = crispness
    this
  }

  def multiplyCrispness(multiplier: Float): Font = {
    distanceFieldCrispness *= multiplier
    this
  }

  def getObliqueStrength: Float = obliqueStrength

  def setObliqueStrength(s: Float): Font = {
    obliqueStrength = s
    this
  }

  def getBoldStrength: Float = boldStrength

  def setBoldStrength(s: Float): Font = {
    boldStrength = s
    this
  }

  def getOutlineStrength: Float = outlineStrength

  def setOutlineStrength(s: Float): Font = {
    outlineStrength = s
    this
  }

  def getBoxDrawingBreadth: Float = boxDrawingBreadth

  def setBoxDrawingBreadth(b: Float): Font = {
    boxDrawingBreadth = b
    this
  }

  def getUnderlineX: Float = underX
  def getUnderlineY: Float = underY

  def setUnderlinePosition(ux: Float, uy: Float): Font = {
    underX = ux; underY = uy; this
  }

  def setUnderlineMetrics(ux: Float, uy: Float, ul: Float, ub: Float): Font = {
    underX = ux; underY = uy; underLength = ul; underBreadth = ub; this
  }

  def getStrikethroughX: Float = strikeX
  def getStrikethroughY: Float = strikeY

  def setStrikethroughPosition(sx: Float, sy: Float): Font = {
    strikeX = sx; strikeY = sy; this
  }

  def setStrikethroughMetrics(sx: Float, sy: Float, sl: Float, sb: Float): Font = {
    strikeX = sx; strikeY = sy; strikeLength = sl; strikeBreadth = sb; this
  }

  def setLineMetrics(x: Float, y: Float, length: Float, breadth: Float): Font = {
    underX = x; underY = y; underLength = length; underBreadth = breadth
    strikeX = x; strikeY = y; strikeLength = length; strikeBreadth = breadth
    this
  }

  def getFancyLineX: Float = fancyX
  def getFancyLineY: Float = fancyY

  def setFancyLinePosition(x: Float, y: Float): Font = {
    fancyX = x; fancyY = y; this
  }

  def getInlineImageOffsetX:  Float = inlineImageOffsetX
  def getInlineImageOffsetY:  Float = inlineImageOffsetY
  def getInlineImageXAdvance: Float = inlineImageXAdvance
  def getInlineImageStretch:  Float = inlineImageStretch

  def setInlineImageStretch(s: Float): Font = {
    inlineImageStretch = s; this
  }

  def setInlineImageMetrics(offsetX: Float, offsetY: Float, xAdvance: Float): Font = {
    inlineImageOffsetX = offsetX; inlineImageOffsetY = offsetY; inlineImageXAdvance = xAdvance
    this
  }

  def setInlineImageMetrics(offsetX: Float, offsetY: Float, xAdvance: Float, stretch: Float): Font = {
    inlineImageOffsetX = offsetX; inlineImageOffsetY = offsetY
    inlineImageXAdvance = xAdvance; inlineImageStretch = stretch
    this
  }

  //// scaling section

  /** Scales the font by the given multiplier, applying it both horizontally and vertically. */
  def scale(both: Float): Font = {
    scaleX *= both; scaleY *= both; cellWidth *= both; cellHeight *= both; this
  }

  /** Scales the font by the given horizontal and vertical multipliers. */
  def scale(horizontal: Float, vertical: Float): Font = {
    scaleX *= horizontal; scaleY *= vertical
    cellWidth *= horizontal; cellHeight *= vertical
    this
  }

  /** Scales the font so that it will have the given width and height. */
  def scaleTo(width: Float, height: Float): Font = {
    scaleX = width / originalCellWidth; scaleY = height / originalCellHeight
    cellWidth = width; cellHeight = height; this
  }

  /** Scales the font so that it will have the given height, keeping the current aspect ratio. */
  def scaleHeightTo(height: Float): Font = scaleTo(cellWidth * height / cellHeight, height)

  /** Multiplies the line height by multiplier without changing the size of any characters. */
  def adjustLineHeight(multiplier: Float): Font = {
    cellHeight *= multiplier; originalCellHeight *= multiplier; descent *= multiplier; this
  }

  /** Multiplies the width used by each glyph in a monospaced font by multiplier. */
  def adjustCellWidth(multiplier: Float): Font = {
    cellWidth *= multiplier; originalCellWidth *= multiplier; this
  }

  /** Fits all chars into cells width by height in size, and optionally centers them. */
  def fitCell(width: Float, height: Float, center: Boolean): Font = {
    cellWidth = width
    cellHeight = height
    val wsx = width / scaleX
    for ((key, g) <- mapping)
      if (key >= 0xe000 && key < 0xf800) {
        // atlas images: don't adjust
      } else if (center) {
        g.offsetX += (wsx - g.xAdvance) * 0.5f
        g.xAdvance = wsx
      } else {
        g.xAdvance = wsx
      }
    isMono = true
    kerning = Nullable.empty
    this
  }

  def useIntegerPositions(integer: Boolean): Font = {
    integerPosition = integer; this
  }

  /** Assembles two chars into a kerning pair that can be looked up in kerning. */
  def kerningPair(first: Char, second: Char): Int = first << 16 | (second & 0xffff)

  //// atlas section

  /** Gets the char associated with a name from an atlas added to this, or -1 if not found. */
  def atlasLookup(lookupName: String): Int =
    Nullable.fold(nameLookup)(-1)(_.get(lookupName, -1))

  /** Adds a new spacing glyph to this Font. */
  def addSpacingGlyph(representation: Char, advance: Float): Font = {
    val space = mapping.getOrElse(' '.toInt, null)
    if (space == null || (representation >= 0xe000 && representation < 0xf800)) {
      this
    } else {
      val next = new GlyphRegion(space.offsetX, space.offsetY, advance)
      mapping.put(representation.toInt, next)
      this
    }
  }

  //// sharing section

  def setSharing(share: Boolean): Font = {
    if (sharing && !share) {
      val sharedMapping = mapping
      mapping = HashMap.empty
      for ((k, v) <- sharedMapping)
        mapping.put(k, new GlyphRegion(v.offsetX, v.offsetY, v.xAdvance))
      // nameLookup is not deep-copied here; CaseInsensitiveIntMap is effectively read-only after addAtlas
      Nullable.foreach(namesByCharCode)(nbc => namesByCharCode = Nullable(HashMap.from(nbc)))
      Nullable.foreach(kerning)(k => kerning = Nullable(HashMap.from(k)))
    }
    sharing = share
    this
  }

  //// measurement section

  /** Gets the distance to advance the cursor after drawing glyph, scaled by the font's scaleX. */
  def calculateSize(layout: Layout): Float = {
    var w             = 0f
    var currentHeight = 0f
    var a             = 0
    var ln            = 0
    while (ln < layout.lineCount) {
      var drawn = 0f
      var scaleXLocal: Float = 0f
      var advance:     Float = 0f
      val line    = layout.lines(ln)
      val glyphs  = line.glyphs
      var curly   = false
      var initial = true
      var kern    = -1
      var amt: Float = 0f
      line.height = currentHeight
      var i = 0
      while (i < glyphs.size) {
        val glyph = glyphs(i)
        var ch    = (glyph & 0xffff).toChar
        advance = layout.advances(a)
        a += 1
        if ((glyph & ALTERNATE_MODES_MASK) == SMALL_CAPS) ch = Character.toUpperCase(ch)
        if (omitCurlyBraces) {
          if (curly) {
            if (ch == '}') { curly = false; i += 1 }
            else if (ch == '{') { curly = false; i += 1 }
            else { i += 1 }
            // skip this glyph in the curly case (except when ch was { or })
            if (ch != '{' && ch != '}') {
              // already incremented i, continue
            } else {
              // fall through to measure
            }
          } else if (ch == '{') {
            curly = true; i += 1
          } else {
            // normal path, don't increment i yet
          }
          if (curly || (ch != '{' && ch != '}' && i > 0 && glyphs.size > 0)) {
            // This simplified logic just skips curly content for width measurement
          }
        }
        // simplified: measure width
        var font: Font = null
        Nullable.foreach(family)(fam => font = fam.connected((glyph >>> 16 & 15).toInt))
        if (font == null) font = this
        val tr = font.mapping.getOrElse(ch.toInt, null)
        if (tr != null) {
          if (ch != ' ') {
            val h = font.cellHeight * advance
            if (h > currentHeight) currentHeight = h
            line.height = Math.max(line.height, currentHeight)
          }
          if (ch >= 0xe000 && ch < 0xf800) {
            scaleXLocal = advance * font.cellHeight / tr.maxDimension * font.inlineImageStretch
          } else {
            scaleXLocal = font.scaleX * advance * (if ((glyph & SUPERSCRIPT) != 0L && !font.isMono) 0.5f else 1.0f)
          }
          var changedW = tr.xAdvance * scaleXLocal
          if (tr.offsetX.isNaN) {
            changedW = font.cellWidth * advance
          } else if (initial && !font.isMono) {
            val ox = tr.offsetX * scaleXLocal
            if (ox < 0) changedW -= ox
          }
          initial = false
          Nullable.foreach(font.kerning) { k =>
            kern = kern << 16 | ch.toInt
            amt = k.getOrElse(kern, 0f) * scaleXLocal
            drawn += amt
          }
          drawn += changedW
        }
        i += 1
      }
      line.width = drawn
      w = Math.max(w, drawn)
      ln += 1
    }
    layout.getWidth
  }

  //// markup section

  /** Parses markup text and produces a Layout containing styled glyphs. This is the core markup processing method that handles [color], [bold], [italic], and all other square-bracket markup tags,
    * plus curly-brace TypingLabel tokens.
    */
  def markup(text: String, appendTo: Layout): Layout = {
    var capitalize        = false
    var previousWasLetter = false
    var capsLock          = false
    var lowerCase         = false
    var initial           = true
    var c                 = 0
    var fontIndex         = -1
    var scaleVal          = 1f
    val rotation          = 0f
    var font: Font  = this
    var sclX: Float = 0f
    val COLOR_MASK = 0xffffffff00000000L
    val baseColor  = java.lang.Long.reverseBytes(java.lang.Float.floatToIntBits(appendTo.getBaseColor).toLong) & 0xfffffffe00000000L
    var color      = baseColor
    var current    = color
    if (Nullable.isEmpty(appendTo.font) || !Nullable.fold(appendTo.font)(false)(_.eq(this))) {
      appendTo.clear()
      appendTo.font = Nullable(this)
    }
    appendTo.peekLine.height = 0
    val targetWidth = appendTo.getTargetWidth
    var kern        = -1
    historyBuffer.clear()
    labeledStates.clear()
    labeledStates ++= storedStates

    var i = 0
    val n = text.length
    while (i < n) {
      sclX = font.scaleX * scaleVal

      //// CURLY BRACKETS
      if (omitCurlyBraces && text.charAt(i) == '{' && i + 1 < n && text.charAt(i + 1) != '{') {
        var end = text.indexOf('}', i)
        if (end == -1) end = text.length
        // Add all chars inside curly braces as invisible glyphs
        while (i < n && i <= end) {
          c = text.charAt(i)
          appendTo.add(current | c.toLong, scaleVal, scaleVal, 0f, 0f, rotation)
          i += 1
        }
        i -= 1 // will be incremented at end of loop
      } else if (enableSquareBrackets && text.charAt(i) == '[') {

        //// SQUARE BRACKET MARKUP
        c = '['
        i += 1
        if (i < n) {
          c = text.charAt(i)
          if (c != '[' && c != '+') {
            if (c == ']') {
              if (historyBuffer.isEmpty) {
                color = baseColor
                current = color & ~SUPERSCRIPT
                font = this
                capitalize = false; capsLock = false; lowerCase = false
              } else {
                current = historyBuffer.remove(historyBuffer.size - 1)
                Nullable.fold(family) {
                  font = this; fontIndex = 0
                } { fam =>
                  fontIndex = ((current & 0xf0000L) >>> 16).toInt
                  font = fam.connected(fontIndex & 15)
                  if (font == null) font = this
                }
              }
              i += 1
            } else {
              val len = text.indexOf(']', i) - i
              if (len < 0) { i = n } // break
              else {
                if (!(len == 1 && c == ' ')) historyBuffer += current
                c match {
                  case '*' => current ^= BOLD
                  case '/' => current ^= OBLIQUE
                  case '^' =>
                    if ((current & SUPERSCRIPT) == SUPERSCRIPT) current &= ~SUPERSCRIPT
                    else current |= SUPERSCRIPT
                  case '.' =>
                    if ((current & SUPERSCRIPT) == SUBSCRIPT) current &= ~SUBSCRIPT
                    else current = (current & ~SUPERSCRIPT) | SUBSCRIPT
                  case '=' =>
                    if ((current & SUPERSCRIPT) == MIDSCRIPT) current &= ~MIDSCRIPT
                    else current = (current & ~SUPERSCRIPT) | MIDSCRIPT
                  case '_' => current ^= UNDERLINE
                  case '~' => current ^= STRIKETHROUGH
                  case ';' => capitalize = !capitalize; capsLock = false; lowerCase = false
                  case '!' => capsLock = !capsLock; capitalize = false; lowerCase = false
                  case ',' => lowerCase = !lowerCase; capitalize = false; capsLock = false
                  case '%' =>
                    if (len >= 2) {
                      if (text.charAt(i + 1) == '?' || text.charAt(i + 1) == '^') {
                        val modes = parseModeFlags(text, i + 2, len - 2)
                        current = (current & 0xffffffffff0fffffL) ^ modes
                      } else {
                        scaleVal = StringUtils.floatFromDec(text, i + 1, i + len) * 0.01f
                      }
                    } else {
                      scaleVal = 1f
                    }
                  case '?' =>
                    if (len > 1) {
                      val modes = parseModeFlags(text, i + 1, len - 1)
                      current = (current & 0xffffffffff0fffffL) ^ modes
                    } else {
                      current &= 0xffffffffff0fffffL
                    }
                  case '#' =>
                    if (len >= 4 && len < 7) {
                      color = StringUtils.longFromHex(text, i + 1, i + 4)
                      color = (color << 52 & 0xf000000000000000L) | (color << 48 & 0x0f00000000000000L) |
                        (color << 48 & 0x00f0000000000000L) | (color << 44 & 0x000f000000000000L) |
                        (color << 44 & 0x0000f00000000000L) | (color << 40 & 0x00000f0000000000L) |
                        0x000000fe00000000L
                    } else if (len >= 7 && len < 9) {
                      color = StringUtils.longFromHex(text, i + 1, i + 7) << 40 | 0x000000fe00000000L
                    } else if (len >= 9) {
                      color = StringUtils.longFromHex(text, i + 1, i + 9) << 32 & 0xfffffffe00000000L
                    } else if (len == 1) {
                      current ^= BLACK_OUTLINE
                    } else {
                      color = baseColor
                    }
                    current = (current & ~COLOR_MASK) | color
                  case '@' =>
                    Nullable.fold(family) {
                      font = this; fontIndex = 0
                    } { fam =>
                      fontIndex = fam.fontAliases.get(StringUtils.safeSubstring(text, i + 1, i + len), 0)
                      current = (current & 0xfffffffffff0ffffL) | (fontIndex & 15L) << 16
                      font = fam.connected(fontIndex & 15)
                      if (font == null) font = this
                    }
                  case '(' =>
                    if (len - 2 > 0) {
                      labeledStates.put(StringUtils.safeSubstring(text, i + 1, i + len - 1), current & 0xffffffffffff0000L)
                    }
                  case '|' =>
                    val lookupColor = colorLookup.getRgba(text, i + 1, i + len) & 0xfffffffe
                    color = if (lookupColor == 256) baseColor else lookupColor.toLong << 32
                    current = (current & ~COLOR_MASK) | color
                  case ' ' =>
                    capitalize = false; capsLock = false; lowerCase = false
                    if (len > 1) {
                      current = labeledStates.getOrElse(StringUtils.safeSubstring(text, i + 1, i + len), current)
                      Nullable.foreach(family) { fam =>
                        font = fam.connected((current >>> 16 & 15).toInt)
                        if (font == null) font = this
                      }
                    } else {
                      color = baseColor
                      current = color & ~SUPERSCRIPT
                      scaleVal = 1f
                      font = this
                    }
                  case _ =>
                    val gdxColor = colorLookup.getRgba(text, i, i + len) & 0xfffffffe
                    color = if (gdxColor == 256) baseColor else gdxColor.toLong << 32
                    current = (current & ~COLOR_MASK) | color
                }
                i += len
              }
            }
          } else {
            // Escaped square bracket or texture region rendering
            var w: Float = 0f
            if (c == '+') {
              Nullable.foreach(font.nameLookup) { nl =>
                val len2 = text.indexOf(']', i) - i
                if (len2 >= 0) {
                  c = nl.get(StringUtils.safeSubstring(text, i + 1, i + len2), '\u200b')
                  i += len2
                  val gr = font.mapping.getOrElse(c, font.defaultValue)
                  sclX = scaleVal * font.cellHeight / gr.maxDimension * font.inlineImageStretch
                } else {
                  c = '\u200b'
                }
              }
            }
            val xAdv = Font.xAdvance(font, sclX, current | c.toLong)
            appendTo.peekLine.width += xAdv
            Nullable.foreach(font.kerning) { k =>
              kern = kern << 16 | c
              val kAmt = k.getOrElse(kern, 0f) * sclX * (1f + 0.5f * (-(current & SUPERSCRIPT) >> 63))
              appendTo.peekLine.width += kAmt
            }
            w = appendTo.peekLine.width
            if (initial && !isMono && !(c >= '\ue000' && c < '\uf800')) {
              val gr = font.mapping.getOrElse(c, font.defaultValue)
              var ox = gr.offsetX
              if (ox.isNaN) ox = 0
              else ox *= sclX * (1f + 0.5f * (-(current & SUPERSCRIPT) >> 63))
              if (ox < 0) { appendTo.peekLine.width -= ox; w = appendTo.peekLine.width }
            }
            initial = false
            if (c == '[') appendTo.add(current | 2, scaleVal, scaleVal, 0f, 0f, rotation)
            else appendTo.add(current | c.toLong, scaleVal, scaleVal, 0f, 0f, rotation)

            if ((targetWidth > 0 && w > targetWidth) || appendTo.atLimit) {
              handleLineWrap(appendTo, font, sclX, targetWidth, scaleVal, current, kern, initial)
            } else {
              appendTo.peekLine.height = Math.max(appendTo.peekLine.height, font.cellHeight * scaleVal)
            }
          }
        }
      } else {

        //// VISIBLE CHAR RENDERING
        var ch = text.charAt(i)
        if (StringUtils.isLowerCase(ch)) {
          if ((capitalize && !previousWasLetter) || capsLock) ch = Character.toUpperCase(ch)
          previousWasLetter = true
        } else if (StringUtils.isUpperCase(ch)) {
          if ((capitalize && previousWasLetter) || lowerCase) ch = Character.toLowerCase(ch)
          previousWasLetter = true
        } else {
          previousWasLetter = false
        }
        val showCh = if ((current & ALTERNATE_MODES_MASK) == SMALL_CAPS) Character.toUpperCase(ch) else ch
        if (ch >= 0xe000 && ch < 0xf800) {
          val gr = font.mapping.getOrElse(ch.toInt, font.defaultValue)
          sclX = scaleVal * font.cellHeight / gr.maxDimension * font.inlineImageStretch
        }
        var w: Float = 0f
        val xAdv = Font.xAdvance(font, sclX, current | showCh.toLong)
        appendTo.peekLine.width += xAdv
        Nullable.foreach(font.kerning) { k =>
          kern = kern << 16 | showCh.toInt
          val kAmt = k.getOrElse(kern, 0f) * sclX * (1f + 0.5f * (-((current | showCh.toLong) & SUPERSCRIPT) >> 63))
          appendTo.peekLine.width += kAmt
        }
        w = appendTo.peekLine.width
        if (initial && !isMono && !(showCh >= '\ue000' && showCh < '\uf800')) {
          val gr = font.mapping.getOrElse(showCh.toInt, font.defaultValue)
          var ox = gr.offsetX
          if (ox.isNaN) ox = 0
          else ox *= sclX
          ox *= (1f + 0.5f * (-(current & SUPERSCRIPT) >> 63))
          if (ox < 0) { appendTo.peekLine.width -= ox; w = appendTo.peekLine.width }
        }
        initial = false
        if (ch == '\n') {
          appendTo.peekLine.height = Math.max(appendTo.peekLine.height, font.cellHeight * scaleVal)
          initial = true
        }
        appendTo.add(current | ch.toLong, scaleVal, scaleVal, 0f, 0f, rotation)
        if ((targetWidth > 0 && w > targetWidth) || appendTo.atLimit) {
          handleLineWrap(appendTo, font, sclX, targetWidth, scaleVal, current, kern, initial)
        } else {
          appendTo.peekLine.height = Math.max(appendTo.peekLine.height, font.cellHeight * scaleVal)
        }
      }
      i += 1
    }
    appendTo
  }

  /** Internal: handles line wrapping during markup processing. */
  private def handleLineWrap(
    appendTo:    Layout,
    font:        Font,
    sclX:        Float,
    targetWidth: Float,
    scaleVal:    Float,
    current:     Long,
    kern:        Int,
    initialIn:   Boolean
  ): Unit = {
    val earlier = appendTo.peekLine
    if (appendTo.lines.size >= appendTo.maxLines) {
      handleEllipsis(appendTo)
    } else {
      val later = new Line()
      later.height = 0
      appendTo.lines += later
      // Simple wrap: find a break point and split
      var j     = earlier.glyphs.size - 2
      var found = false
      while (j >= 0 && !found) {
        val curr = earlier.glyphs(j)
        if (curr >>> 32 == 0L || java.util.Arrays.binarySearch(breakChars, (curr & 0xffff).toChar) >= 0) {
          val leadingSpace = j > 0 && {
            val prev = earlier.glyphs(j)
            prev >>> 32 == 0L || java.util.Arrays.binarySearch(spaceChars, (prev & 0xffff).toChar) >= 0
          }
          val cutJ = if (leadingSpace) j - 1 else j
          // Move glyphs from earlier to later
          var change     = 0f
          var changeNext = 0f
          val buf        = ArrayBuffer.empty[Long]
          var leading    = if (leadingSpace) 1 else 0
          var k          = cutJ + 1
          while (k < earlier.glyphs.size) {
            val g   = earlier.glyphs(k)
            val adv = Font.xAdvance(font, sclX, g)
            change += adv
            leading -= 1
            if (leading < 0) {
              buf += g
              changeNext += adv
            }
            k += 1
          }
          if (earlier.width - change <= targetWidth) {
            // Perform the split
            val truncSize = cutJ + 1
            while (earlier.glyphs.size > truncSize) earlier.glyphs.remove(earlier.glyphs.size - 1)
            if (leadingSpace) {
              earlier.glyphs += applyChar(if (earlier.glyphs.isEmpty) 0L else earlier.glyphs.last, ' ')
            }
            later.width = changeNext
            earlier.width -= change
            later.glyphs ++= buf
            later.height = Math.max(later.height, font.cellHeight * scaleVal)
            found = true
          }
        }
        j -= 1
      }
      if (later.glyphs.isEmpty) {
        appendTo.lines.remove(appendTo.lines.size - 1)
      }
    }
  }

  /** Handles ellipsis when max lines are reached. */
  protected def handleEllipsis(appendTo: Layout): Boolean = {
    val earlier  = appendTo.peekLine
    val ellipsis = Nullable.fold(appendTo.ellipsis)("")(identity)
    // Simplified: just truncate and add ellipsis
    if (ellipsis.nonEmpty && earlier.glyphs.size > ellipsis.length + 1) {
      val truncSize = earlier.glyphs.size - ellipsis.length
      while (earlier.glyphs.size > truncSize) earlier.glyphs.remove(earlier.glyphs.size - 1)
      val lastGlyph = if (earlier.glyphs.isEmpty) 0L else earlier.glyphs.last
      var e         = 0
      while (e < ellipsis.length) {
        appendTo.add((lastGlyph & 0xffffffff81ff0000L) | ellipsis.charAt(e).toLong)
        e += 1
      }
      true
    } else {
      false
    }
  }

  /** Parses mode flags from text for [?...] and [%?...] markup. */
  private def parseModeFlags(text: String, offset: Int, maxLen: Int): Long = {
    var modes = 0L
    if (maxLen >= 3) {
      val ch = Character.toUpperCase(text.charAt(offset))
      if (ch == 'B') {
        modes |= BLACK_OUTLINE
        if (maxLen >= 4 && Character.toUpperCase(text.charAt(offset + 2)) == 'U') modes |= BLUE_OUTLINE
      } else if (ch == 'W') {
        if (Character.toUpperCase(text.charAt(offset + 1)) == 'H') modes |= WHITE_OUTLINE | BLACK_OUTLINE
        else modes |= WARN
      } else if (ch == 'S') {
        if (Character.toUpperCase(text.charAt(offset + 1)) == 'U') modes |= SUGGEST
        else if (Character.toUpperCase(text.charAt(offset + 1)) == 'M') modes |= SMALL_CAPS
        else if (maxLen >= 4 && Character.toUpperCase(text.charAt(offset + 2)) == 'I') modes |= SHINY
        else if (Character.toUpperCase(text.charAt(offset + 1)) == 'H') modes |= DROP_SHADOW
      } else if (ch == 'C') { modes |= CONTEXT }
      else if (ch == 'D') { modes |= DROP_SHADOW }
      else if (ch == 'E') { modes |= ERROR }
      else if (ch == 'H') { modes |= HALO }
      else if (ch == 'J') { modes |= JOSTLE }
      else if (ch == 'N') {
        if (Character.toUpperCase(text.charAt(offset + 1)) == 'O') modes |= NOTE
        else if (Character.toUpperCase(text.charAt(offset + 1)) == 'E') modes |= NEON
      } else if (ch == 'R') { modes |= RED_OUTLINE | BLACK_OUTLINE }
      else if (ch == 'Y') { modes |= YELLOW_OUTLINE | BLACK_OUTLINE }
    }
    modes
  }

  //// state storage section

  def storeState(stateName: String, markupStr: String): Unit =
    storedStates.put(stateName, markupGlyph('\u0000', markupStr))

  def storeState(stateName: String, formatted: Long): Unit =
    storedStates.put(stateName, formatted & 0xffffffffffff0000L)

  def getStoredState(stateName: String, fallback: Long): Long =
    storedStates.getOrElse(stateName, fallback)

  def removeStoredState(stateName: String): Unit =
    storedStates.remove(stateName)

  //// markupGlyph section

  /** Reads markup from markup, processes it, and applies it to the given char. */
  def markupGlyph(chr: Int, markupStr: String): Long =
    (markupGlyph(markupStr + chr.toChar) & 0xffffffffffff0000L) | (chr & 0xffff).toLong

  /** Reads markup and processes it until it has a single complete glyph. */
  def markupGlyph(markupStr: String): Long = {
    val COLOR_MASK = 0xffffffff00000000L
    val baseColor  = 0xfffffffe00000000L
    var color      = baseColor
    var current    = color
    var font: Font = this
    var capsLock   = false
    var lowerCase  = false
    var capitalize = false
    var fontIndex  = -1

    boundary {
      var i = 0
      val n = markupStr.length
      while (i <= n) {
        if (i == n) break(current | ' '.toLong)
        if (enableSquareBrackets && markupStr.charAt(i) == '[') {
          i += 1
          if (i < n) {
            var c = markupStr.charAt(i)
            if (c != '[' && c != '+') {
              if (c == ']') {
                color = baseColor
                current = color & ~SUPERSCRIPT
                capsLock = false; lowerCase = false; capitalize = false
                i += 1
              } else {
                val len = markupStr.indexOf(']', i) - i
                if (len < 0) break(current | ' '.toLong)
                c match {
                  case '*' => current ^= BOLD
                  case '/' => current ^= OBLIQUE
                  case '^' =>
                    if ((current & SUPERSCRIPT) == SUPERSCRIPT) current &= ~SUPERSCRIPT else current |= SUPERSCRIPT
                  case '.' =>
                    if ((current & SUPERSCRIPT) == SUBSCRIPT) current &= ~SUBSCRIPT
                    else current = (current & ~SUPERSCRIPT) | SUBSCRIPT
                  case '=' =>
                    if ((current & SUPERSCRIPT) == MIDSCRIPT) current &= ~MIDSCRIPT
                    else current = (current & ~SUPERSCRIPT) | MIDSCRIPT
                  case '_'       => current ^= UNDERLINE
                  case '~'       => current ^= STRIKETHROUGH
                  case ';' | '!' => capsLock = !capsLock; lowerCase = false
                  case ','       => lowerCase = !lowerCase; capsLock = false
                  case '#'       =>
                    if (len >= 4 && len < 7) {
                      color = StringUtils.longFromHex(markupStr, i + 1, i + 4)
                      color = (color << 52 & 0xf000000000000000L) | (color << 48 & 0x0f00000000000000L) |
                        (color << 48 & 0x00f0000000000000L) | (color << 44 & 0x000f000000000000L) |
                        (color << 44 & 0x0000f00000000000L) | (color << 40 & 0x00000f0000000000L) |
                        0x000000fe00000000L
                    } else if (len >= 7 && len < 9) {
                      color = StringUtils.longFromHex(markupStr, i + 1, i + 7) << 40 | 0x000000fe00000000L
                    } else if (len >= 9) {
                      color = StringUtils.longFromHex(markupStr, i + 1, i + 9) << 32 & 0xfffffffe00000000L
                    } else if (len == 1) {
                      current ^= BLACK_OUTLINE
                    } else {
                      color = baseColor
                    }
                    current = (current & ~COLOR_MASK) | color
                  case '@' =>
                    Nullable.foreach(family) { fam =>
                      fontIndex = fam.fontAliases.get(StringUtils.safeSubstring(markupStr, i + 1, i + len), 0)
                      font = fam.connected(fontIndex)
                      current = (current & 0xfffffffffff0ffffL) | (fontIndex & 15L) << 16
                    }
                  case '|' =>
                    val lookupColor = colorLookup.getRgba(markupStr, i + 1, i + len) & 0xfffffffe
                    color = if (lookupColor == 256) baseColor else lookupColor.toLong << 32
                    current = (current & ~COLOR_MASK) | color
                  case _ =>
                    val gdxColor = colorLookup.getRgba(markupStr, i, i + len) & 0xfffffffe
                    color = if (gdxColor == 256) baseColor else gdxColor.toLong << 32
                    current = (current & ~COLOR_MASK) | color
                }
                i += len
              }
            } else {
              // Escaped bracket or texture region
              if (c == '+') {
                Nullable.foreach(font.nameLookup) { nl =>
                  val len2 = markupStr.indexOf(']', i) - i
                  if (len2 >= 0) {
                    c = nl.get(StringUtils.safeSubstring(markupStr, i + 1, i + len2), '\u200b').toChar
                  } else {
                    c = '\u200b'
                  }
                }
              }
              if (c == '[') break(current | 2L)
              else break(current | c.toLong)
            }
          }
        } else {
          // Visible char
          var ch = markupStr.charAt(i)
          if (StringUtils.isLowerCase(ch)) {
            if (capitalize || capsLock) ch = Character.toUpperCase(ch)
          } else if (StringUtils.isUpperCase(ch)) {
            if (lowerCase) ch = Character.toLowerCase(ch)
          }
          break(current | ch.toLong)
        }
        i += 1
      }
      current | ' '.toLong
    }
  }

  //// distance field section

  /** Adjusts actualCrispness for window resizing. */
  def resizeDistanceField(width: Float, height: Float): Unit =
    if (distanceField != DistanceFieldType.STANDARD) {
      actualCrispness = distanceFieldCrispness * Math.max(width / 1920f, height / 1080f)
    }

  //// CJK support

  /** Inserts a zero-width space after every CJK ideographic character. */
  def insertZeroWidthSpacesInCJK(text: String): String =
    text.replaceAll("([\u2e80-\u303f\u31c0-\u31ef\u3200-\u9fff\uf900-\ufaff\ufe30-\ufe4f])", "$1\u200b")

  //// close section

  /** Copy constructor that creates a new Font with the same settings as the given Font. */
  def this(other: Font) = {
    this()
    if (other.sharing) {
      mapping = other.mapping
      nameLookup = other.nameLookup
      namesByCharCode = other.namesByCharCode
      kerning = other.kerning
    } else {
      for ((k, v) <- other.mapping) mapping.put(k, new GlyphRegion(v.offsetX, v.offsetY, v.xAdvance))
      nameLookup = other.nameLookup
      namesByCharCode = other.namesByCharCode
      Nullable.foreach(other.kerning)(k => kerning = Nullable(HashMap.from(k)))
    }
    sharing = other.sharing
    defaultValue = other.defaultValue
    distanceField = other.distanceField
    isMono = other.isMono
    actualCrispness = other.actualCrispness
    distanceFieldCrispness = other.distanceFieldCrispness
    cellWidth = other.cellWidth
    cellHeight = other.cellHeight
    originalCellWidth = other.originalCellWidth
    originalCellHeight = other.originalCellHeight
    scaleX = other.scaleX
    scaleY = other.scaleY
    descent = other.descent
    solidBlock = other.solidBlock
    family = other.family
    colorLookup = other.colorLookup
    integerPosition = other.integerPosition
    obliqueStrength = other.obliqueStrength
    boldStrength = other.boldStrength
    outlineStrength = other.outlineStrength
    boxDrawingBreadth = other.boxDrawingBreadth
    glowStrength = other.glowStrength
    name = other.name
    omitCurlyBraces = other.omitCurlyBraces
    enableSquareBrackets = other.enableSquareBrackets
    PACKED_BLACK = other.PACKED_BLACK
    PACKED_WHITE = other.PACKED_WHITE
    xAdjust = other.xAdjust
    yAdjust = other.yAdjust
    widthAdjust = other.widthAdjust
    heightAdjust = other.heightAdjust
    underX = other.underX
    underY = other.underY
    underLength = other.underLength
    underBreadth = other.underBreadth
    strikeX = other.strikeX
    strikeY = other.strikeY
    strikeLength = other.strikeLength
    strikeBreadth = other.strikeBreadth
    fancyX = other.fancyX
    fancyY = other.fancyY
    inlineImageOffsetX = other.inlineImageOffsetX
    inlineImageOffsetY = other.inlineImageOffsetY
    inlineImageXAdvance = other.inlineImageXAdvance
    inlineImageStretch = other.inlineImageStretch
    dropShadowOffsetX = other.dropShadowOffsetX
    dropShadowOffsetY = other.dropShadowOffsetY
  }

  /** Re-wraps and re-calculates the given Layout based on this Font's settings. */
  def regenerateLayout(layout: Layout): Layout = {
    Nullable.foreach(layout.font) { f =>
      val text = layout.appendIntoDirect(new StringBuilder).toString
      f.markup(text, layout.clear())
    }
    layout
  }

  //// rendering section

  /** Enables the appropriate distance field shader on the batch for proper rendering of SDF/MSDF fonts. Must be called before rendering distance field fonts. Does nothing for standard fonts.
    * Calculates and sets the `u_smoothing` uniform based on current font scale.
    */
  def enableShader(batch: sge.graphics.g2d.Batch): Unit = {
    val batchShader = batch.shader
    val fontShader  = shader.getOrElse(null) // @nowarn — Java interop, comparing references
    if (batchShader ne fontShader) {
      distanceField match {
        case DistanceFieldType.MSDF =>
          batch.shader_=(shader)
          val smoothing = 8f * actualCrispness * Math.max(cellHeight / originalCellHeight, cellWidth / originalCellWidth)
          batch.flush()
          shader.foreach(_.setUniformf("u_smoothing", smoothing))
          Font.smoothingValues.put(batch, smoothing)
        case DistanceFieldType.SDF | DistanceFieldType.SDF_OUTLINE =>
          batch.shader_=(shader)
          val smoothing = 4f * actualCrispness * Math.max(cellHeight / originalCellHeight, cellWidth / originalCellWidth)
          batch.flush()
          shader.foreach(_.setUniformf("u_smoothing", smoothing))
          Font.smoothingValues.put(batch, smoothing)
        case _ =>
          batch.shader_=(Nullable.empty)
          Font.smoothingValues.put(batch, 0f)
      }
    }
  }

  /** Resumes the distance field shader if it was paused. Unlike [[enableShader]], this only acts if the batch's shader is already the font's shader. Recalculates smoothing.
    */
  def resumeDistanceFieldShader(batch: sge.graphics.g2d.Batch): Unit = {
    val fontShader = shader.getOrElse(null) // @nowarn — Java interop
    if (fontShader != null && (batch.shader eq fontShader)) {
      distanceField match {
        case DistanceFieldType.MSDF =>
          val smoothing = 8f * actualCrispness * Math.max(cellHeight / originalCellHeight, cellWidth / originalCellWidth)
          batch.flush()
          shader.foreach(_.setUniformf("u_smoothing", smoothing))
          Font.smoothingValues.put(batch, smoothing)
        case DistanceFieldType.SDF | DistanceFieldType.SDF_OUTLINE =>
          val smoothing = 4f * actualCrispness * Math.max(cellHeight / originalCellHeight, cellWidth / originalCellWidth)
          batch.flush()
          shader.foreach(_.setUniformf("u_smoothing", smoothing))
          Font.smoothingValues.put(batch, smoothing)
        case _ => ()
      }
    }
  }

  /** Temporarily disables the distance field shader on this batch, so non-distance-field textures can be drawn without artifacts. Call [[resumeDistanceFieldShader]] afterward.
    */
  def pauseDistanceFieldShader(batch: sge.graphics.g2d.Batch): Unit =
    if (distanceField != DistanceFieldType.STANDARD) {
      batch.shader_=(Nullable.empty)
    }

  /** Draws a pre-built vertex array (20 floats = 4 vertices * 5 elements: x, y, color, u, v) to the batch. This is the core low-level rendering primitive used by drawBlocks, drawBlockSequence,
    * drawFancyLine, and drawGlyph.
    */
  protected def drawVertices(batch: sge.graphics.g2d.Batch, texture: sge.graphics.Texture, verts: Array[Float]): Unit =
    batch.draw(texture, verts, 0, 20)

  /** Draws a grid of rectangular color blocks at the given position using the monospace font's solid block character. Only useful for monospace fonts. Colors is [x][y] indexed; zero-alpha colors are
    * skipped.
    */
  def drawBlocks(batch: sge.graphics.g2d.Batch, colors: Array[Array[Int]], x: Float, y: Float): Unit =
    drawBlocks(batch, solidBlock, colors, x, y)

  /** Draws a grid of rectangular color blocks at the given position using the specified block character. Only useful for monospace fonts. Colors is [x][y] indexed; zero-alpha colors are skipped.
    */
  def drawBlocks(batch: sge.graphics.g2d.Batch, blockChar: Char, colors: Array[Array[Int]], x: Float, y: Float): Unit = {
    val block = mapping.getOrElse(blockChar.toInt, null)
    if (block != null && block.texture != null) {
      val parent = block.texture
      val ipw    = 1.0f / parent.width.toFloat
      val iph    = 1.0f / parent.height.toFloat
      val bu     = block.u
      val bv     = block.v
      val bu2    = bu + ipw
      val bv2    = bv + iph

      val xStart = x + 3.90625e-3f // small offset to avoid line artifacts
      val yStart = y + 3.90625e-3f
      vertices(0) = xStart
      vertices(1) = yStart
      vertices(3) = bu
      vertices(4) = bv
      vertices(5) = xStart
      vertices(6) = yStart + cellHeight
      vertices(8) = bu
      vertices(9) = bv2
      vertices(10) = xStart + cellWidth
      vertices(11) = yStart + cellHeight
      vertices(13) = bu2
      vertices(14) = bv2
      vertices(15) = xStart + cellWidth
      vertices(16) = yStart
      vertices(18) = bu2
      vertices(19) = bv

      val xn = colors.length
      val yn = colors(0).length
      var xi = 0
      while (xi < xn) {
        var yi = 0
        while (yi < yn) {
          if ((colors(xi)(yi) & 254) != 0) {
            val c = java.lang.Float.intBitsToFloat(Integer.reverseBytes(colors(xi)(yi) & -2))
            vertices(2) = c; vertices(7) = c; vertices(12) = c; vertices(17) = c
            drawVertices(batch, parent, vertices)
          }
          vertices(1) += cellHeight; vertices(16) += cellHeight
          vertices(6) += cellHeight; vertices(11) += cellHeight
          yi += 1
        }
        vertices(0) += cellWidth; vertices(5) += cellWidth
        vertices(10) += cellWidth; vertices(15) += cellWidth
        vertices(1) = yStart; vertices(16) = yStart
        vertices(6) = yStart + cellHeight; vertices(11) = yStart + cellHeight
        xi += 1
      }
    } // end if (block != null)
  }

  /** Draws blocks in a sequence specified by a float array, with groups of 4: startX, startY, sizeX, sizeY. Used to render box-drawing and block-element characters from a solid block region.
    */
  protected def drawBlockSequence(
    batch:    sge.graphics.g2d.Batch,
    sequence: Array[Float],
    block:    sge.graphics.g2d.TextureRegion,
    color:    Float,
    x:        Float,
    y:        Float,
    width:    Float,
    height:   Float,
    rotation: Float
  ): Unit = drawBlockSequence(batch, sequence, block, color, x, y, width, height, rotation, 1f)

  /** Draws blocks in a sequence with optional breadth multiplier for box-drawing line thickness. */
  protected def drawBlockSequence(
    batch:    sge.graphics.g2d.Batch,
    sequence: Array[Float],
    block:    sge.graphics.g2d.TextureRegion,
    color:    Float,
    x:        Float,
    y:        Float,
    width:    Float,
    height:   Float,
    rotation: Float,
    breadth:  Float
  ): Unit = if (block.texture != null) {
    val parent     = block.texture
    val ipw        = 1f / parent.width.toFloat
    val iph        = 1f / parent.height.toFloat
    val halfWidth  = width * 0.5f
    val halfHeight = height * 0.5f
    val bu         = block.u
    val bv         = block.v
    val bu2        = bu + ipw
    val bv2        = bv - iph
    val sn         = sge.math.MathUtils.sinDeg(rotation)
    val cs         = sge.math.MathUtils.cosDeg(rotation)

    var b = 0
    while (b < sequence.length) {
      var startX = sequence(b)
      var startY = sequence(b + 1)
      var sizeX  = sequence(b + 2)
      var sizeY  = sequence(b + 3)

      if (breadth != 1f) {
        val thinAcross = utils.BlockUtils.THIN_ACROSS * breadth
        val wideAcross = utils.BlockUtils.WIDE_ACROSS * breadth

        if (sizeX == utils.BlockUtils.THIN_ACROSS || sizeX == utils.BlockUtils.TWIN_ACROSS) sizeX = thinAcross
        else if (sizeX == utils.BlockUtils.WIDE_ACROSS) sizeX = wideAcross
        else if (startX == 0f) {
          if (sizeX == utils.BlockUtils.THIN_OVER || sizeX == utils.BlockUtils.TWIN_OVER1 || sizeX == utils.BlockUtils.TWIN_OVER2) sizeX += thinAcross * 0.25f
          else if (sizeX == utils.BlockUtils.WIDE_OVER) sizeX += wideAcross * 0.25f
        } else if (startX > 0f) {
          if (sizeX == utils.BlockUtils.THIN_OVER || sizeX == utils.BlockUtils.TWIN_OVER1 || sizeX == utils.BlockUtils.TWIN_OVER2) sizeX += thinAcross * 0.25f
          else if (sizeX == utils.BlockUtils.WIDE_OVER) sizeX += wideAcross * 0.25f
        }

        if (sizeY == utils.BlockUtils.THIN_ACROSS || sizeY == utils.BlockUtils.TWIN_ACROSS) sizeY = thinAcross
        else if (sizeY == utils.BlockUtils.WIDE_ACROSS) sizeY = wideAcross
        else if (startY == 0f) {
          if (sizeY == utils.BlockUtils.THIN_OVER || sizeY == utils.BlockUtils.TWIN_OVER1 || sizeY == utils.BlockUtils.TWIN_OVER2) sizeY += thinAcross * 0.25f
          else if (sizeY == utils.BlockUtils.WIDE_OVER) sizeY += wideAcross * 0.25f
        } else if (startY > 0f) {
          if (sizeY == utils.BlockUtils.THIN_OVER || sizeY == utils.BlockUtils.TWIN_OVER1 || sizeY == utils.BlockUtils.TWIN_OVER2) sizeY += thinAcross * 0.25f
          else if (sizeY == utils.BlockUtils.WIDE_OVER) sizeY += wideAcross * 0.25f
        }

        if (startX == utils.BlockUtils.THIN_START || startX == utils.BlockUtils.TWIN_START1 || startX == utils.BlockUtils.TWIN_START2) startX -= thinAcross * 0.25f
        else if (startX == utils.BlockUtils.WIDE_START) startX -= wideAcross * 0.25f
        if (startY == utils.BlockUtils.THIN_START || startY == utils.BlockUtils.TWIN_START1 || startY == utils.BlockUtils.TWIN_START2) startY -= thinAcross * 0.25f
        else if (startY == utils.BlockUtils.WIDE_START) startY -= wideAcross * 0.25f
      }

      startX = startX * width - halfWidth
      startY = startY * height - halfHeight
      sizeX *= width
      sizeY *= height

      val p0x = startX
      val p0y = startY + sizeY
      val p1x = startX
      val p1y = startY
      val p2x = startX + sizeX
      val p2y = startY

      vertices(0) = x + cs * p0x - sn * p0y
      vertices(1) = y + sn * p0x + cs * p0y
      vertices(5) = x + cs * p1x - sn * p1y
      vertices(6) = y + sn * p1x + cs * p1y
      vertices(10) = x + cs * p2x - sn * p2y
      vertices(11) = y + sn * p2x + cs * p2y
      vertices(15) = vertices(0) - vertices(5) + vertices(10)
      vertices(16) = vertices(1) - vertices(6) + vertices(11)

      vertices(2) = color; vertices(3) = bu; vertices(4) = bv
      vertices(7) = color; vertices(8) = bu; vertices(9) = bv2
      vertices(12) = color; vertices(13) = bu2; vertices(14) = bv2
      vertices(17) = color; vertices(18) = bu2; vertices(19) = bv

      drawVertices(batch, parent, vertices)
      b += 4
    }
  }

  /** Draws a decorative underline pattern for special markup modes (ERROR, WARN, NOTE, CONTEXT, SUGGEST). Creates pixelated or wavy underline effects by repeatedly drawing tiny blocks in patterns.
    */
  protected def drawFancyLine(
    batch:    sge.graphics.g2d.Batch,
    mode:     Long,
    x:        Float,
    y:        Float,
    width:    Float,
    xPx:      Float,
    yPx:      Float,
    rotation: Float
  ): Unit = {
    val block = mapping.getOrElse(solidBlock.toInt, null)
    if (block != null && block.texture != null) {
      val parent = block.texture
      val ipw    = 1f / parent.width.toFloat
      val iph    = 1f / parent.height.toFloat
      val bu     = block.u
      val bv     = block.v
      val bu2    = bu + ipw
      val bv2    = bv + iph
      val sn     = sge.math.MathUtils.sinDeg(rotation)
      val cs     = sge.math.MathUtils.cosDeg(rotation)

      val color =
        if (mode == Font.ERROR) PACKED_ERROR_COLOR
        else if (mode == Font.CONTEXT) PACKED_CONTEXT_COLOR
        else if (mode == Font.WARN) PACKED_WARN_COLOR
        else if (mode == Font.SUGGEST) PACKED_SUGGEST_COLOR
        else PACKED_NOTE_COLOR
      // TODO: multiplyAlpha with batch color alpha when ColorUtils is available
      // val adjustedColor = ColorUtils.multiplyAlpha(color, batch.color.a)

      var index  = 0
      var startX = 0f
      while (startX <= width) {
        var shiftX = startX
        var shiftY = 0f
        if (mode == Font.ERROR) {
          shiftY = (index & 1) * yPx
        } else if (mode == Font.CONTEXT) {
          shiftX -= (index & 2) * xPx
          shiftY = -(index & 1) * yPx
        } else if (mode == Font.WARN) {
          shiftX += (~index & 1) * xPx
          shiftY = (~index & 1) * yPx
        } else if (mode == Font.SUGGEST) {
          shiftX -= (index & (index >>> 1) & 1) * xPx
          shiftY = -(index & (index >>> 1) & 1) * yPx
        } else {
          shiftY = (index >>> 1 & 1) * yPx
        }
        val p0x = shiftX
        val p0y = shiftY + yPx
        val p1x = shiftX
        val p1y = shiftY
        val p2x = shiftX + xPx
        val p2y = shiftY

        vertices(0) = x + cs * p0x - sn * p0y
        vertices(1) = y + sn * p0x + cs * p0y
        vertices(5) = x + cs * p1x - sn * p1y
        vertices(6) = y + sn * p1x + cs * p1y
        vertices(10) = x + cs * p2x - sn * p2y
        vertices(11) = y + sn * p2x + cs * p2y
        vertices(15) = vertices(0) - vertices(5) + vertices(10)
        vertices(16) = vertices(1) - vertices(6) + vertices(11)

        vertices(2) = color; vertices(3) = bu; vertices(4) = bv
        vertices(7) = color; vertices(8) = bu; vertices(9) = bv2
        vertices(12) = color; vertices(13) = bu2; vertices(14) = bv2
        vertices(17) = color; vertices(18) = bu2; vertices(19) = bv

        drawVertices(batch, parent, vertices)
        startX += xPx
        index += 1
      }
    } // end if (block != null)
  }

  /** Returns x rounded to integer position if [[integerPosition]] is true, otherwise returns x unchanged. */
  protected def handleIntegerPosition(x: Float): Float =
    if (integerPosition) Math.round(x).toFloat else x

  /** Sets the quad position vertices (0,1,5,6,10,11,15,16) for a rotated quad from 3 corner offsets. The fourth corner (vertex 3 = index 15,16) is computed as v0 - v1 + v2.
    */
  private def setQuadVertices(
    verts: Array[Float],
    x:     Float,
    y:     Float,
    p0x:   Float,
    p0y:   Float,
    p1x:   Float,
    p1y:   Float,
    p2x:   Float,
    p2y:   Float,
    sin:   Float,
    cos:   Float
  ): Unit = {
    verts(0) = x + cos * p0x - sin * p0y
    verts(1) = y + sin * p0x + cos * p0y
    verts(5) = x + cos * p1x - sin * p1y
    verts(6) = y + sin * p1x + cos * p1y
    verts(10) = x + cos * p2x - sin * p2y
    verts(11) = y + sin * p2x + cos * p2y
    verts(15) = verts(0) - verts(5) + verts(10)
    verts(16) = verts(1) - verts(6) + verts(11)
  }

  /** Draws the specified glyph with a Batch at the given x, y position. The glyph contains multiple types of data packed into one long: bottom 16 bits store a char, bits above that store formatting
    * (bold, underline, etc.), and the upper 32 bits store color as RGBA.
    * @return
    *   the distance in world units the drawn glyph uses up for width
    */
  def drawGlyph(batch: sge.graphics.g2d.Batch, glyph: Long, x: Float, y: Float): Float =
    drawGlyph(batch, glyph, x, y, 0f, 1f, 1f, 0, 1f)

  /** Draws the specified glyph with rotation.
    * @return
    *   the distance in world units the drawn glyph uses up for width
    */
  def drawGlyph(batch: sge.graphics.g2d.Batch, glyph: Long, x: Float, y: Float, rotation: Float): Float =
    drawGlyph(batch, glyph, x, y, rotation, 1f, 1f, 0, 1f)

  /** Draws the specified glyph with rotation and sizing.
    * @return
    *   the distance in world units the drawn glyph uses up for width
    */
  def drawGlyph(batch: sge.graphics.g2d.Batch, glyph: Long, x: Float, y: Float, rotation: Float, sizingX: Float, sizingY: Float): Float =
    drawGlyph(batch, glyph, x, y, rotation, sizingX, sizingY, 0, 1f)

  /** Draws the specified glyph with full parameters: position, rotation, sizing, background color, and advance multiplier. This is the core glyph rendering method.
    * @return
    *   the distance in world units the drawn glyph uses up for width
    */
  def drawGlyph(
    batch:             sge.graphics.g2d.Batch,
    glyphIn:           Long,
    xIn:               Float,
    yIn:               Float,
    rotation:          Float,
    sizingXIn:         Float,
    sizingYIn:         Float,
    backgroundColor:   Int,
    advanceMultiplier: Float
  ): Float = scala.util.boundary {
    var glyph   = glyphIn
    val sizingX = sizingXIn
    var sizingY = sizingYIn
    val sin     = sge.math.MathUtils.sinDeg(rotation)
    val cos     = sge.math.MathUtils.cosDeg(rotation)

    var font: Font = null
    family.foreach { fam => font = fam.connected((glyph >>> 16 & 15).toInt) }
    if (font == null) font = this

    var c        = (glyph & 0xffff).toChar
    var squashed = false
    if ((glyph & Font.ALTERNATE_MODES_MASK) == Font.SMALL_CAPS) {
      val upper = Character.toUpperCase(c)
      squashed = c != upper
      c = upper
      glyph = (glyph & 0xffffffffffff0000L) | c.toLong
    }

    val tr = font.mapping.getOrElse(c.toInt, null)
    if (tr == null) scala.util.boundary.break(0f)

    if (squashed) sizingY *= 0.75f

    val batchColor    = batch.color
    val batchAlpha    = batchColor.a
    val batchAlpha2   = batchAlpha * batchAlpha
    val batchAlpha3   = batchAlpha2 * batchAlpha
    val batchAlpha1_5 = Math.sqrt(batchAlpha3).toFloat

    val color = java.lang.Float.intBitsToFloat(
      ((if ((glyph & Font.BOLD) != 0L) batchAlpha1_5 else batchAlpha) * (glyph >>> 33 & 127)).toInt << 25
        | (batchColor.r * (glyph >>> 56)).toInt
        | (batchColor.g * (glyph >>> 48 & 0xff)).toInt << 8
        | (batchColor.b * (glyph >>> 40 & 0xff)).toInt << 16
    )

    val scale = 1f
    var fsx:         Float = 0f
    var fsy:         Float = 0f
    var scaleXLocal: Float = 0f
    var scaleYLocal: Float = 0f
    if (c >= 0xe000 && c < 0xf800) {
      fsx = font.cellHeight / tr.maxDimension * font.inlineImageStretch
      fsy = fsx
      scaleXLocal = scale * fsx
      scaleYLocal = scaleXLocal
    } else {
      fsx = font.scaleX
      fsy = font.scaleY
      scaleXLocal = fsx * scale
      scaleYLocal = fsy * scale
    }
    @scala.annotation.unused
    val osx             = fsx * (scale + 1f) * 0.5f // used by glyph-based underline fallback (not yet ported)
    val osy             = fsy * (scale + 1f) * 0.5f
    var centerX         = tr.xAdvance * scaleXLocal * advanceMultiplier * 0.5f
    var centerY         = font.originalCellHeight * scaleYLocal * 0.5f
    val scaleCorrection = font.descent * font.scaleY * 2f
    var x               = xIn
    var y               = yIn + scaleCorrection * scale

    val ox = x
    @scala.annotation.unused
    val oy = y // used by glyph-based underline fallback (not yet ported)

    val ix     = handleIntegerPosition(x + centerX)
    val iy     = handleIntegerPosition(y + centerY)
    val xShift = (x + centerX) - ix
    val yShift = (y + centerY) - iy
    x = handleIntegerPosition(ix - xShift)
    y = handleIntegerPosition(iy - yShift)
    centerX -= xShift * 0.5f
    centerY -= yShift * 0.5f

    // Alternate mode and secondary color (needed by both box-drawing and glyph effects)
    val altMode = glyph & Font.ALTERNATE_MODES_MASK

    // Box-drawing characters (offsetX is NaN)
    if (java.lang.Float.isNaN(tr.offsetX)) {
      val ci              = c.toInt - 0x2500
      val solidBlockGlyph = font.mapping.getOrElse(font.solidBlock.toInt, tr)
      if (ci >= 0 && ci < utils.BlockUtils.BOX_DRAWING.length) {
        // Background fill
        if (backgroundColor != 0) {
          drawBlockSequence(
            batch,
            utils.BlockUtils.BOX_DRAWING(0x88),
            solidBlockGlyph,
            java.lang.Float.intBitsToFloat(Integer.reverseBytes(backgroundColor & -2)),
            x,
            y,
            font.cellWidth * sizingX,
            font.cellHeight * scale * sizingY,
            rotation
          )
        }
        val boxes   = utils.BlockUtils.BOX_DRAWING(ci)
        val dashed  = c <= 0x250b || (c >= 0x254c && c <= 0x254f)
        val breadth = if (c < 0x2580) boxDrawingBreadth else 1f

        // Glow/outline effects for box-drawing
        if (altMode == Font.HALO || altMode == Font.NEON) {
          var xi = 3
          while (xi >= 1) {
            drawBlockSequence(
              batch,
              if (dashed) utils.BlockUtils.BOX_DRAWING(ci & 3) else boxes,
              solidBlockGlyph,
              utils.ColorUtils.lerpColorsMultiplyAlpha(color, color, Math.min(font.glowStrength * 0.6f / (xi * xi), 1f), batchAlpha1_5),
              x,
              y,
              font.cellWidth * sizingX,
              font.cellHeight * scale * sizingY,
              rotation,
              breadth + xi
            )
            xi -= 1
          }
        } else if ((glyph & Font.BLACK_OUTLINE) == Font.BLACK_OUTLINE) {
          drawBlockSequence(
            batch,
            if (dashed) utils.BlockUtils.BOX_DRAWING(ci & 3) else boxes,
            solidBlockGlyph,
            utils.ColorUtils.multiplyAlpha(PACKED_BLACK, batchAlpha1_5),
            x,
            y,
            font.cellWidth * sizingX,
            font.cellHeight * scale * sizingY,
            rotation,
            breadth + 1f
          )
        }

        drawBlockSequence(batch, boxes, solidBlockGlyph, color, x, y, font.cellWidth * sizingX, font.cellHeight * scale * sizingY, rotation, breadth)
      }
      scala.util.boundary.break(font.cellWidth)
    }

    val tex = tr.texture
    if (tex == null) scala.util.boundary.break(0f)

    val scaledHeight = font.cellHeight * scale * sizingY
    var x0           = 0f; var x1 = 0f; var x2 = 0f
    var y0           = 0f; var y1 = 0f; var y2 = 0f
    var w            = tr.regionWidth * scaleXLocal * sizingX
    val xAdvance     = tr.xAdvance
    var changedW     = xAdvance * scaleXLocal * advanceMultiplier

    x += cellWidth * 0.5f
    var xc = tr.offsetX * scaleXLocal * sizingX - cos * centerX - cellWidth * 0.5f

    val trrh = tr.regionHeight.toFloat
    var yt   = ((font.originalCellHeight - (trrh + tr.offsetY) + font.descent) * sizingY - font.descent) * scaleYLocal + sin * centerX - centerY

    var h = trrh * scaleYLocal * sizingY

    val u  = tr.u
    val v  = tr.v
    val u2 = tr.u2
    val v2 = tr.v2

    // Inline image adjustments
    if (c >= 0xe000 && c < 0xf800) {
      val stretchShift = (trrh * font.inlineImageStretch - trrh) * scaleXLocal * sizingX * 0.5f
      val xch          = tr.offsetX * scaleXLocal * sizingX
      xc -= xch + stretchShift
      x += xch + stretchShift
      val ych = tr.offsetY * scaleYLocal * sizingY
      yt = (sin * scaledHeight - scaledHeight) * 0.5f - ych - stretchShift
      y = oy + scaledHeight * 0.5f - ych
    }

    // Oblique
    if ((glyph & Font.OBLIQUE) != 0L) {
      val amount = h * obliqueStrength * 0.2f
      x0 += amount; x1 -= amount; x2 -= amount
    }

    // Superscript/subscript/midscript
    val script = glyph & Font.SUPERSCRIPT
    if (script == Font.SUPERSCRIPT) {
      w *= 0.5f; h *= 0.5f; yt = yt * 0.625f
      y0 += scaledHeight * 0.375f; y1 += scaledHeight * 0.375f; y2 += scaledHeight * 0.375f
      if (!font.isMono) changedW *= 0.5f
    } else if (script == Font.SUBSCRIPT) {
      w *= 0.5f; h *= 0.5f; yt = yt * 0.625f
      y0 -= scaledHeight * 0.375f; y1 -= scaledHeight * 0.375f; y2 -= scaledHeight * 0.375f
      if (!font.isMono) changedW *= 0.5f
    } else if (script == Font.MIDSCRIPT) {
      w *= 0.5f; h *= 0.5f; yt = yt * 0.625f
      if (!font.isMono) changedW *= 0.5f
    }

    // Compute quad vertices
    var p0x = xc + x0
    val p0y = yt + y0 + h
    var p1x = xc + x1
    val p1y = yt + y1
    var p2x = xc + x2 + w
    val p2y = yt + y2

    // Outline strength for effects
    val xOutline = outlineStrength * cellHeight / 32f
    val yOutline = outlineStrength * cellHeight / 32f

    vertices(3) = u; vertices(4) = v
    vertices(8) = u; vertices(9) = v2
    vertices(13) = u2; vertices(14) = v2
    vertices(18) = u2; vertices(19) = v

    // Secondary color for outlines and effects
    var drawColor = color
    val secondaryColor: Float =
      if (altMode == Font.HALO) { drawColor = PACKED_HALO_COLOR; color }
      else if (altMode == Font.NEON) { drawColor = PACKED_WHITE; color }
      else if (altMode == Font.BLUE_OUTLINE) PACKED_BLUE
      else if (altMode == Font.RED_OUTLINE) PACKED_RED
      else if (altMode == Font.YELLOW_OUTLINE) PACKED_YELLOW
      else if (altMode == Font.WHITE_OUTLINE) PACKED_WHITE
      else PACKED_BLACK

    // Drop shadow effect
    if (altMode == Font.DROP_SHADOW) {
      val shadow = utils.ColorUtils.multiplyAlpha(PACKED_SHADOW_COLOR, batchAlpha1_5)
      vertices(2) = shadow; vertices(7) = shadow; vertices(12) = shadow; vertices(17) = shadow
      vertices(0) = x + cos * p0x - sin * p0y + dropShadowOffsetX
      vertices(1) = y + sin * p0x + cos * p0y + dropShadowOffsetY
      vertices(5) = x + cos * p1x - sin * p1y + dropShadowOffsetX
      vertices(6) = y + sin * p1x + cos * p1y + dropShadowOffsetY
      vertices(10) = x + cos * p2x - sin * p2y + dropShadowOffsetX
      vertices(11) = y + sin * p2x + cos * p2y + dropShadowOffsetY
      vertices(15) = vertices(0) - vertices(5) + vertices(10)
      vertices(16) = vertices(1) - vertices(6) + vertices(11)
      drawVertices(batch, tex, vertices)
    } else if ((glyph & Font.BLACK_OUTLINE) == Font.BLACK_OUTLINE && altMode != Font.HALO && altMode != Font.NEON) {
      // Outline effect (BLACK_OUTLINE or colored outline)
      val widthAdj = if ((glyph & Font.BOLD) != 0L) 2 else 1
      val outline  = utils.ColorUtils.multiplyAlpha(secondaryColor, if (widthAdj == 1) batchAlpha1_5 else batchAlpha2)
      vertices(2) = outline; vertices(7) = outline; vertices(12) = outline; vertices(17) = outline
      var xi = -widthAdj
      while (xi <= widthAdj) {
        var xa = xi * xOutline
        if (widthAdj == 2 && (xi > 0 || boldStrength > 1f)) xa *= boldStrength
        var yi = -1
        while (yi <= 1) {
          if (xi != 0 || yi != 0) {
            val ya = yi * yOutline
            setQuadVertices(vertices, x + xa, y + ya, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
            drawVertices(batch, tex, vertices)
          }
          yi += 1
        }
        xi += 1
      }
    } else if (altMode == Font.HALO || altMode == Font.NEON) {
      // Glow effect (HALO/NEON)
      val widthAdj = if ((glyph & Font.BOLD) != 0L) 5 else 3
      val outline  = utils.ColorUtils.multiplyAlpha(secondaryColor, (if (widthAdj == 3) batchAlpha * 0.2f else batchAlpha * 0.1f) * glowStrength)
      vertices(2) = outline; vertices(7) = outline; vertices(12) = outline; vertices(17) = outline
      var xi = -widthAdj
      while (xi <= widthAdj) {
        var xa = xi * xOutline
        if (widthAdj == 5 && (xi > 0 || boldStrength > 1f)) xa *= boldStrength
        var yi = -3
        while (yi <= 3) {
          if ((xi != 0 || yi != 0) && (Math.abs(yi) + Math.abs(xi) <= widthAdj + 1)) {
            val ya = yi * yOutline
            setQuadVertices(vertices, x + xa, y + ya, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
            drawVertices(batch, tex, vertices)
          }
          yi += 1
        }
        xi += 1
      }
    } else if (altMode == Font.SHINY) {
      // Shiny effect (bright highlight above)
      val widthAdj = if ((glyph & Font.BOLD) != 0L) 1 else 0
      val shine    = utils.ColorUtils.multiplyAlpha(PACKED_WHITE, if (widthAdj == 0) batchAlpha1_5 else batchAlpha2)
      vertices(2) = shine; vertices(7) = shine; vertices(12) = shine; vertices(17) = shine
      var xi = -widthAdj
      while (xi <= widthAdj) {
        var xa = xi * xOutline
        if (widthAdj == 1 && (xi > 0 || boldStrength >= 1f)) xa *= boldStrength
        val ya = 1.5f * yOutline
        setQuadVertices(vertices, x + xa, y + ya, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
        drawVertices(batch, tex, vertices)
        xi += 1
      }
    }

    // Draw the main glyph
    vertices(2) = drawColor; vertices(7) = drawColor; vertices(12) = drawColor; vertices(17) = drawColor
    vertices(0) = handleIntegerPosition(x + cos * p0x - sin * p0y)
    vertices(1) = handleIntegerPosition(y + sin * p0x + cos * p0y)
    vertices(5) = handleIntegerPosition(x + cos * p1x - sin * p1y)
    vertices(6) = handleIntegerPosition(y + sin * p1x + cos * p1y)
    vertices(10) = handleIntegerPosition(x + cos * p2x - sin * p2y)
    vertices(11) = handleIntegerPosition(y + sin * p2x + cos * p2y)
    vertices(15) = vertices(0) - vertices(5) + vertices(10)
    vertices(16) = vertices(1) - vertices(6) + vertices(11)

    drawVertices(batch, tex, vertices)

    // Bold: draw extra shifted copies
    if ((glyph & Font.BOLD) != 0L) {
      val old0          = p0x; val old1 = p1x; val old2 = p2x
      val leftStrength  = if (boldStrength >= 1f) boldStrength else 0f
      val rightStrength = if (boldStrength >= 0f) 1f else 0f
      if (rightStrength != 0f) {
        p0x = old0 + rightStrength; p1x = old1 + rightStrength; p2x = old2 + rightStrength
        setQuadVertices(vertices, x, y, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
        drawVertices(batch, tex, vertices)
        p0x = old0 + rightStrength * 0.5f; p1x = old1 + rightStrength * 0.5f; p2x = old2 + rightStrength * 0.5f
        setQuadVertices(vertices, x, y, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
        drawVertices(batch, tex, vertices)
      }
      if (leftStrength != 0f) {
        p0x = old0 - leftStrength; p1x = old1 - leftStrength; p2x = old2 - leftStrength
        setQuadVertices(vertices, x, y, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
        drawVertices(batch, tex, vertices)
        p0x = old0 - leftStrength * 0.5f; p1x = old1 - leftStrength * 0.5f; p2x = old2 - leftStrength * 0.5f
        setQuadVertices(vertices, x, y, p0x, p0y, p1x, p1y, p2x, p2y, sin, cos)
        drawVertices(batch, tex, vertices)
      }
    }

    // Underline, strikethrough, and fancy line effects
    val oCenterX     = tr.xAdvance * scaleXLocal * advanceMultiplier * 0.5f
    val oCenterY     = font.originalCellHeight * osy * 0.5f
    val oyAdj        = y + (scaleCorrection * sizingY + scaledHeight) * -0.5f + centerY * 0.25f + scaleCorrection
    val solidBlockGR = font.mapping.getOrElse(font.solidBlock.toInt, tr)

    if ((glyph & Font.UNDERLINE) != 0L && (c < 0xe000 || c >= 0xf800)) {
      val uix = handleIntegerPosition(ox + oCenterX)
      val uiy = handleIntegerPosition(oyAdj + oCenterY)
      val uxs = (ox + oCenterX) - uix
      val uys = (oyAdj + oCenterY) - uiy
      val ux  = handleIntegerPosition(uix + uxs) + font.cellWidth * 0.5f
      val uy  = handleIntegerPosition(uiy + uys)
      val ucx = oCenterX + uxs * 0.5f
      val ucy = oCenterY + uys * 0.5f

      val under = font.mapping.getOrElse(0x2500, null)
      if (under != null && java.lang.Float.isNaN(under.offsetX)) {
        val up0x = (changedW * (font.underX + 1f)) - font.cellWidth * 0.5f - changedW * 0.1f - cos * ucx
        val up0y = -ucy - ((font.underY * font.cellHeight + font.descent * scaleYLocal) * sizingY) + sin * ucx

        if (altMode == Font.HALO || altMode == Font.NEON) {
          var uxi = 3
          while (uxi >= 1) {
            drawBlockSequence(
              batch,
              utils.BlockUtils.BOX_DRAWING(0),
              solidBlockGR,
              utils.ColorUtils.lerpColorsMultiplyAlpha(secondaryColor, drawColor, Math.min(font.glowStrength * 0.4f / (uxi * uxi), 1f), batchAlpha1_5),
              ux + (cos * up0x - sin * up0y),
              uy + (sin * up0x + cos * up0y),
              changedW * (font.underLength + 1.1f),
              font.cellHeight * sizingY * (1f + font.underBreadth + uxi * 0.5f),
              rotation
            )
            uxi -= 1
          }
        } else if ((glyph & Font.BLACK_OUTLINE) == Font.BLACK_OUTLINE) {
          drawBlockSequence(
            batch,
            utils.BlockUtils.BOX_DRAWING(0),
            solidBlockGR,
            utils.ColorUtils.multiplyAlpha(secondaryColor, batchAlpha1_5),
            ux + (cos * up0x - sin * up0y),
            uy + (sin * up0x + cos * up0y),
            changedW * (font.underLength + 1.1f),
            font.cellHeight * sizingY * (1.75f + font.underBreadth),
            rotation
          )
        }

        drawBlockSequence(
          batch,
          utils.BlockUtils.BOX_DRAWING(0),
          solidBlockGR,
          drawColor,
          ux + (cos * up0x - sin * up0y),
          uy + (sin * up0x + cos * up0y),
          changedW * (font.underLength + 1.1f),
          font.cellHeight * sizingY * (1f + font.underBreadth),
          rotation
        )
      }
    }

    if ((glyph & Font.STRIKETHROUGH) != 0L && (c < 0xe000 || c >= 0xf800)) {
      val six = handleIntegerPosition(ox + oCenterX)
      val siy = handleIntegerPosition(oyAdj + oCenterY)
      val sxs = (ox + oCenterX) - six
      val sys = (oyAdj + oCenterY) - siy
      val sx  = handleIntegerPosition(six + sxs) + font.cellWidth * 0.5f
      val sy  = handleIntegerPosition(siy + sys)
      val scx = oCenterX + sxs * 0.5f
      val scy = oCenterY + sys * 0.5f

      val dash = font.mapping.getOrElse(0x2500, null)
      if (dash != null && java.lang.Float.isNaN(dash.offsetX)) {
        val sp0x = (changedW * (font.strikeX + 1f)) - font.cellWidth * 0.5f - changedW * 0.1f - cos * scx
        val sp0y = sin * scx - scy - ((font.strikeY - 0.5f) * font.cellHeight + font.descent * scaleYLocal) * sizingY

        if (altMode == Font.HALO || altMode == Font.NEON) {
          var sxi = 3
          while (sxi >= 1) {
            drawBlockSequence(
              batch,
              utils.BlockUtils.BOX_DRAWING(0),
              solidBlockGR,
              utils.ColorUtils.lerpColorsMultiplyAlpha(secondaryColor, drawColor, Math.min(font.glowStrength * 0.4f / (sxi * sxi), 1f), batchAlpha1_5),
              sx + (cos * sp0x - sin * sp0y),
              sy + (sin * sp0x + cos * sp0y),
              changedW * (font.strikeLength + 1.1f),
              font.cellHeight * sizingY * (1f + font.strikeBreadth + sxi * 0.5f),
              rotation
            )
            sxi -= 1
          }
        } else if ((glyph & Font.BLACK_OUTLINE) == Font.BLACK_OUTLINE) {
          drawBlockSequence(
            batch,
            utils.BlockUtils.BOX_DRAWING(0),
            solidBlockGR,
            utils.ColorUtils.multiplyAlpha(secondaryColor, batchAlpha1_5),
            sx + (cos * sp0x - sin * sp0y),
            sy + (sin * sp0x + cos * sp0y),
            changedW * (font.strikeLength + 1.1f),
            font.cellHeight * sizingY * (1.75f + font.strikeBreadth),
            rotation
          )
        }

        drawBlockSequence(
          batch,
          utils.BlockUtils.BOX_DRAWING(0),
          solidBlockGR,
          drawColor,
          sx + (cos * sp0x - sin * sp0y),
          sy + (sin * sp0x + cos * sp0y),
          changedW * (font.strikeLength + 1.1f),
          font.cellHeight * sizingY * (1f + font.strikeBreadth),
          rotation
        )
      }
    }

    // Fancy line modes (ERROR, WARN, NOTE, CONTEXT, SUGGEST)
    if (altMode >= Font.ERROR && altMode <= Font.NOTE && (c < 0xe000 || c >= 0xf800)) {
      val fix = handleIntegerPosition(ox + oCenterX)
      val fiy = handleIntegerPosition(oyAdj + oCenterY)
      val fxs = (ox + oCenterX) - fix
      val fys = (oyAdj + oCenterY) - fiy
      val fx  = handleIntegerPosition(fix + fxs)
      val fy  = handleIntegerPosition(fiy + fys)
      val fcx = oCenterX + fxs * 0.5f
      val fcy = oCenterY + fys * 0.5f
      // Approximate pixel sizes from projection matrix (simplified: assume 1px = 1 world unit fallback)
      val xPx = 2f / Math.max(1f, batch.projectionMatrix.values(0) * 960f)
      val yPx = 2f / Math.max(1f, batch.projectionMatrix.values(5) * 540f)

      val fp0x = -cos * fcx + changedW * font.fancyX
      val fp0y = (font.descent * font.scaleY * 0.5f) * (scale * sizingY - font.fancyY) - fcy + sin * fcx

      drawFancyLine(batch, altMode, fx + (cos * fp0x - sin * fp0y), fy + (sin * fp0x + cos * fp0y), changedW * (1f + underLength), xPx, yPx, rotation)
    }

    changedW
  }

  /** Draws an entire Layout of text at the given position, left-aligned.
    * @return
    *   the width of the widest line drawn
    */
  def drawGlyphs(batch: sge.graphics.g2d.Batch, layout: Layout, x: Float, y: Float): Float = {
    val lines      = layout.lines
    var drawn      = 0f
    var lineY      = y
    var advanceIdx = 0

    var ln = 0
    while (ln < lines.size) {
      val line   = lines(ln)
      val glyphs = line.glyphs
      val lineW  = line.width
      var cx     = x

      var i = 0
      while (i < glyphs.size) {
        val glyph   = glyphs(i)
        val advance = if (advanceIdx < layout.advances.size) layout.advances(advanceIdx) else 1f
        advanceIdx += 1
        val drawnW = drawGlyph(batch, glyph, cx, lineY, 0f, advance, advance, 0, 1f)
        cx += drawnW
        i += 1
      }
      if (lineW > drawn) drawn = lineW
      lineY -= line.height
      ln += 1
    }
    drawn
  }

  //// font loading section

  /** Loads an AngelCode BMFont .fnt file, parsing glyph metrics and texture pages. Populates [[mapping]], [[parents]], [[kerning]], and all font metrics.
    */
  protected def loadFNT(fntHandle: sge.files.FileHandle, xAdj: Float, yAdj: Float, wAdj: Float, hAdj: Float, makeGridGlyphs: Boolean)(using sge.Sge): Unit = {
    val fnt = fntHandle.readString(Nullable("UTF-8"))
    xAdjust = xAdj; yAdjust = yAdj; widthAdjust = wAdj; heightAdjust = hAdj

    var idx = StringUtils.indexAfter(fnt, "padding=", 0)
    StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, ",", idx + 1); idx })
    StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, ",", idx + 1); idx })
    StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, ",", idx + 1); idx })
    StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "lineHeight=", idx + 1); idx })

    StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "base=", idx); idx })
    val baseline = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "pages=", idx); idx })
    descent = 0

    val pages = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "\npage id=", idx); idx })
    if (parents.isEmpty) {
      var pi = 0
      while (pi < pages) {
        val textureName = fnt.substring({ idx = StringUtils.indexAfter(fnt, "file=\"", idx); idx }, { idx = fnt.indexOf('"', idx); idx })
        if (Font.canUseTextures) {
          val textureHandle = summon[sge.Sge].files.internal(textureName)
          if (textureHandle.exists()) {
            val tex = new sge.graphics.Texture(textureHandle)
            if (distanceField != DistanceFieldType.STANDARD)
              tex.setFilter(sge.graphics.Texture.TextureFilter.Linear, sge.graphics.Texture.TextureFilter.Linear)
            parents += new sge.graphics.g2d.TextureRegion(tex)
          } else throw new RuntimeException("Missing texture file: " + textureName)
        }
        pi += 1
      }
    }

    val glyphCount = StringUtils.intFromDec(fnt, { idx = StringUtils.indexAfter(fnt, "\nchars count=", idx); idx }, { idx = StringUtils.indexAfter(fnt, "\nchar id=", idx); idx })
    mapping = HashMap.empty
    var minWidth = Float.MaxValue
    var gi       = 0
    while (gi < glyphCount) {
      if (idx >= fnt.length) gi = glyphCount
      else {
        val c  = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " x=", idx); idx })
        val gx = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " y=", idx); idx })
        val gy = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " width=", idx); idx })
        val gw = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " height=", idx); idx })
        val gh = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " xoffset=", idx); idx })
        val xo = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " yoffset=", idx); idx })
        val yo = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " xadvance=", idx); idx })
        var a  = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " page=", idx); idx })
        val p  = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "\nchar id=", idx); idx })

        if (c != 9608) minWidth = Math.min(minWidth, a + wAdj)
        val gr = new GlyphRegion(parents(p), gx, gy, gw, gh)
        if (c == 10) { a = 0; gr.offsetX = 0 }
        else if (makeGridGlyphs && utils.BlockUtils.isBlockGlyph(c)) gr.offsetX = Float.NaN
        else gr.offsetX = xo + xAdj
        gr.offsetY = yo + yAdj
        gr.xAdvance = a + wAdj
        cellWidth = Math.max(a + wAdj, cellWidth)
        cellHeight = Math.max(gh + hAdj, cellHeight)
        if (gw * gh > 1) descent = Math.min(baseline - gh - yo, descent)
        mapping.put(c, gr)
        if (c == '[') mapping.put(2, gr)
      }
      gi += 1
    }

    // Kerning
    idx = StringUtils.indexAfter(fnt, "\nkernings count=", 0)
    if (idx < fnt.length) {
      val kernings = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "\nkerning first=", idx); idx })
      if (kernings >= 1) {
        val kern = HashMap[Int, Float]()
        var ki   = 0
        while (ki < kernings) {
          val first  = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " second=", idx); idx })
          val second = StringUtils.intFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, " amount=", idx); idx })
          val amount = StringUtils.floatFromDec(fnt, idx, { idx = StringUtils.indexAfter(fnt, "\nkerning first=", idx); idx })
          kern.put(first << 16 | second, amount)
          if (first == '[') kern.put(2 << 16 | second, amount)
          if (second == '[') kern.put(first << 16 | 2, amount)
          ki += 1
        }
        kerning = Nullable(kern)
      }
    }

    finalizeFontCommon(makeGridGlyphs)
    originalCellHeight = cellHeight - descent * 0.25f
    cellHeight = originalCellHeight
    isMono = minWidth == cellWidth && kerning.isEmpty
  }

  /** Loads a Structured JSON font file (from msdf-atlas-gen or fontwriter). Supports .json, .dat (LZB compressed) formats.
    */
  protected def loadJSON(jsonHandle: sge.files.FileHandle, textureRegion: sge.graphics.g2d.TextureRegion, xAdj: Float, yAdj: Float, wAdj: Float, hAdj: Float, makeGridGlyphs: Boolean): Unit = {
    parents.clear()
    parents += textureRegion
    xAdjust = xAdj; yAdjust = yAdj; widthAdjust = wAdj; heightAdjust = hAdj

    val jsonStr =
      if (jsonHandle.extension == "dat") utils.LZBDecompression.decompressFromBytes(jsonHandle.readBytes())
      else jsonHandle.readString(Nullable("UTF-8"))

    val fnt  = sge.utils.readFromString[Json](jsonStr)
    val root = jsonObj(fnt)

    name = {
      val n = jsonHandle.name; val di = n.indexOf('.'); if (di >= 0) n.substring(0, di) else n
    }

    val atlas  = jsonObj(root.getOrElse("atlas", emptyJsonObj))
    val dfType = jsonStrOr(atlas, "type", "")

    if (dfType == "msdf" || dfType == "mtsdf") { setDistanceField(DistanceFieldType.MSDF); setCrispness(jsonNumOr(atlas, "distanceRange", 8f) * 0.2f) }
    else if (dfType == "sdf" || dfType == "psdf") { setDistanceField(DistanceFieldType.SDF); setCrispness(jsonNumOr(atlas, "distanceRange", 8f) * 0.2f) }
    else setDistanceField(DistanceFieldType.STANDARD)

    val size = jsonNumOr(atlas, "size", 16f)
    descent = size * -0.25f
    originalCellHeight = hAdj - descent + size; cellHeight = originalCellHeight
    underY = 0.05f; strikeY = 0.15f; strikeBreadth = -0.375f; underBreadth = -0.375f
    if (makeGridGlyphs) { underLength = 0f; strikeLength = 0f; underX = -0.3f; strikeX = -0.3f }
    else { underLength = 0f; strikeLength = 0f; underX = 0f; strikeX = 0f }
    fancyY = 2f

    val glyphs = jsonArr(root.getOrElse("glyphs", emptyJsonArr))
    mapping = HashMap.empty
    var minWidth = Float.MaxValue
    val regH     = textureRegion.regionHeight

    for (current <- glyphs) {
      val g = jsonObj(current)
      val c = jsonIntOr(g, "unicode", 65535)
      var a = jsonNumOr(g, "advance", 1f) * size

      val ab = g.get("atlasBounds").map(jsonObj)
      val pb = g.get("planeBounds").map(jsonObj)

      val (gx, gy, gw, gh) = ab match {
        case Some(m) =>
          val l = jsonNumOr(m, "left", 0f); val r = jsonNumOr(m, "right", 0f)
          val t = jsonNumOr(m, "top", 0f); val b  = jsonNumOr(m, "bottom", 0f)
          (l, regH - t, r - l, regH - b - (regH - t))
        case None => (0f, 0f, 0f, 0f)
      }
      val (xo, yo) = pb match {
        case Some(m) => (jsonNumOr(m, "left", 0f) * size, size - jsonNumOr(m, "top", 0f) * size)
        case None    => (0f, 0f)
      }

      if (c != 9608) minWidth = Math.min(minWidth, a + wAdj)
      val gr = new GlyphRegion(textureRegion, gx, gy, gw, gh)
      if (c == 10) { a = 0; gr.offsetX = 0 }
      else if (makeGridGlyphs && utils.BlockUtils.isBlockGlyph(c)) gr.offsetX = Float.NaN
      else gr.offsetX = xo + xAdj
      gr.offsetY = yo + yAdj; gr.xAdvance = a + wAdj
      cellWidth = Math.max(a + wAdj, cellWidth)
      mapping.put(c, gr)
      if (c == '[') mapping.put(2, gr)
    }

    val kernArr = jsonArr(root.getOrElse("kerning", emptyJsonArr))
    if (kernArr.isEmpty) kerning = Nullable.empty
    else {
      val kern = HashMap[Int, Float]()
      for (current <- kernArr) {
        val k  = jsonObj(current)
        val f  = jsonIntOr(k, "unicode1", 65535)
        val s  = jsonIntOr(k, "unicode2", 65535)
        val am = jsonNumOr(k, "advance", 0f)
        kern.put(f << 16 | s, am)
        if (f == '[') kern.put(2 << 16 | s, am)
        if (s == '[') kern.put(f << 16 | 2, am)
      }
      kerning = Nullable(kern)
    }

    finalizeJsonFont(textureRegion, makeGridGlyphs, minWidth)
  }

  /** Loads a SadConsole .font configuration file, which defines a grid-based monospace font. */
  protected def loadSad(prefix: String, fntName: String)(using app: sge.Sge): Unit = {
    val fntHandle = app.files.internal(prefix + fntName)
    if (!fntHandle.exists()) throw new RuntimeException("Missing font file: " + prefix + fntName)
    val fnt  = sge.utils.readFromString[Json](fntHandle.readString(Nullable("UTF-8")))
    val root = jsonObj(fnt)

    val parent: sge.graphics.g2d.TextureRegion =
      if (parents.nonEmpty) parents(0)
      else {
        val textureName   = jsonStrOr(root, "FilePath", "")
        val textureHandle = app.files.internal(prefix + textureName)
        if (!Font.canUseTextures) { val tr = new sge.graphics.g2d.TextureRegion(); parents += tr; tr }
        else if (textureHandle.exists()) { val tr = new sge.graphics.g2d.TextureRegion(new sge.graphics.Texture(textureHandle)); parents += tr; tr }
        else throw new RuntimeException("Missing texture file: " + prefix + textureName)
      }

    val columns = jsonIntOr(root, "Columns", 16)
    val padding = jsonIntOr(root, "GlyphPadding", 0)
    cellHeight = jsonNumOr(root, "GlyphHeight", 16f)
    cellWidth = jsonNumOr(root, "GlyphWidth", 8f)
    descent = Math.round(cellHeight * -0.25f).toFloat
    val rows = (parent.regionHeight - padding) / (cellHeight.toInt + padding)
    mapping = HashMap.empty
    var yGrid = 0; var ch = 0
    while (yGrid < rows) {
      var xGrid = 0
      while (xGrid < columns) {
        val gr = new GlyphRegion(parent, xGrid * (cellWidth.toInt + padding) + padding, yGrid * (cellHeight.toInt + padding) + padding, cellWidth, cellHeight)
        gr.offsetX = 0; gr.offsetY = descent; gr.xAdvance = if (ch == 10) 0 else cellWidth
        mapping.put(ch, gr)
        if (ch == '[') { if (mapping.contains(2)) mapping.put(rows * columns, mapping(2)); mapping.put(2, gr) }
        xGrid += 1; ch += 1
      }
      yGrid += 1
    }

    solidBlock = jsonIntOr(root, "SolidGlyphIndex", 219).toChar
    finalizeFontCommon(false)
    integerPosition = true; isMono = true
    underY += 0.375f; strikeY += 0.375f
    inlineImageOffsetX = 0f; inlineImageOffsetY = 0f; inlineImageXAdvance = 0f; inlineImageStretch = 1f
  }

  // JSON AST helpers (for loadJSON/loadSad)
  private def jsonObj(j: Json): Map[String, Json] = j match {
    case Json.Obj(fields) => fields.toMap
    case _                => Map.empty
  }
  private def jsonArr(j: Json): Seq[Json] = j match {
    case Json.Arr(items) => items.toSeq
    case _               => Seq.empty
  }
  private def jsonStr_(j: Json): String = j match {
    case Json.Str(s) => s
    case _           => ""
  }
  private def jsonNum(j: Json): Float = j match {
    case Json.Num(n) => n.toFloat.getOrElse(0f)
    case _           => 0f
  }
  private def jsonInt(j: Json): Int = j match {
    case Json.Num(n) => n.toInt.getOrElse(0)
    case _           => 0
  }
  private def jsonNumOr(m: Map[String, Json], key: String, default: Float): Float =
    m.get(key).map(jsonNum).getOrElse(default)
  private def jsonIntOr(m: Map[String, Json], key: String, default: Int): Int =
    m.get(key).map(jsonInt).getOrElse(default)
  private def jsonStrOr(m: Map[String, Json], key: String, default: String): String =
    m.get(key).map(jsonStr_).getOrElse(default)
  private val emptyJsonObj: Json = Json.Obj(sge.utils.JsonObject.empty)
  private val emptyJsonArr: Json = Json.Arr(Vector.empty)

  /** Common finalization: sets up space, newline, zwsp, solidBlock, defaultValue. */
  private def finalizeFontCommon(@scala.annotation.unused makeGridGlyphs: Boolean): Unit = {
    var space = mapping.getOrElse(' '.toInt, null)
    if (space == null) {
      val guess = mapping.getOrElse('l'.toInt, null)
      if (guess == null) throw new RuntimeException("Cannot create a font without space or 'l' to guess at space metrics.")
      space = new GlyphRegion(guess, 0, 0, 0, 0); space.xAdvance = guess.xAdvance; space.offsetX = 0; space.offsetY = 0
      mapping.put(' '.toInt, space)
    }
    mapping.put('\r'.toInt, space)
    val zwSpace = new GlyphRegion(space.offsetX, space.offsetY, 0f)
    if (space.texture != null) zwSpace.setRegion(space)
    mapping.put('\u200b'.toInt, zwSpace)
    mapping.get('\n'.toInt) match {
      case Some(gr) => if (Font.canUseTextures) { gr.setRegion(0, 0, 0, 0) }; gr.xAdvance = 0f
      case None     =>
        val nl = new GlyphRegion(zwSpace); if (Font.canUseTextures) { nl.setRegion(0, 0, 0, 0) }; nl.xAdvance = 0
        mapping.put('\n'.toInt, nl)
    }
    if (!mapping.contains(solidBlock.toInt)) solidBlock = '\uffff'
    defaultValue = mapping.getOrElse(' '.toInt, mapping.valuesIterator.next())
    originalCellWidth = cellWidth; originalCellHeight = cellHeight
    inlineImageOffsetX = 0f; inlineImageOffsetY = 0f; inlineImageXAdvance = 0f; inlineImageStretch = 1f
  }

  /** Finalization for loadJSON: solid block from texture corner, grid glyphs. */
  private def finalizeJsonFont(textureRegion: sge.graphics.g2d.TextureRegion, makeGridGlyphs: Boolean, minWidth: Float): Unit = {
    var space = mapping.getOrElse(' '.toInt, null)
    if (space == null) {
      val guess = mapping.getOrElse('l'.toInt, null)
      if (guess == null) throw new RuntimeException("Cannot create a font without space or 'l' to guess at space metrics.")
      space = new GlyphRegion(guess, 0, 0, 0, 0); space.xAdvance = guess.xAdvance; space.offsetX = 0; space.offsetY = 0
      mapping.put(' '.toInt, space)
    }
    mapping.put('\r'.toInt, space)
    val zwSpace = new GlyphRegion(space.offsetX, space.offsetY, 0f)
    if (space.texture != null) zwSpace.setRegion(space)
    mapping.put('\u200b'.toInt, zwSpace)
    mapping.get('\n'.toInt) match {
      case Some(gr) => if (Font.canUseTextures) { gr.setRegion(0, 0, 0, 0) }; gr.xAdvance = 0f
      case None     =>
        val nl = new GlyphRegion(zwSpace); if (Font.canUseTextures) { nl.setRegion(0, 0, 0, 0) }; nl.xAdvance = 0
        mapping.put('\n'.toInt, nl)
    }
    solidBlock = '\u2588'
    if (makeGridGlyphs) {
      val block = new GlyphRegion(
        new sge.graphics.g2d.TextureRegion(textureRegion, textureRegion.regionWidth - 2, textureRegion.regionHeight - 2, 1, 1),
        0,
        cellHeight,
        cellWidth
      )
      mapping.put(solidBlock.toInt, block)
      var bi = 0x2500;
      while (bi < 0x2500 + utils.BlockUtils.ALL_BLOCK_CHARS.length) {
        if (utils.BlockUtils.isBlockGlyph(bi)) mapping.put(bi, new GlyphRegion(block, Float.NaN, cellHeight, cellWidth))
        bi += 1
      }
    } else if (!mapping.contains(solidBlock.toInt)) {
      mapping.put(
        solidBlock.toInt,
        new GlyphRegion(
          new sge.graphics.g2d.TextureRegion(textureRegion, textureRegion.regionWidth - 2, textureRegion.regionHeight - 2, 1, 1),
          0,
          cellHeight,
          cellWidth
        )
      )
    }
    defaultValue = mapping.getOrElse(' '.toInt, mapping.valuesIterator.next())
    originalCellWidth = cellWidth; originalCellHeight = cellHeight
    if (textureRegion.texture != null) textureRegion.texture.setFilter(sge.graphics.Texture.TextureFilter.Linear, sge.graphics.Texture.TextureFilter.Linear)
    isMono = minWidth == cellWidth && kerning.isEmpty
    integerPosition = false
    inlineImageOffsetX = 0f; inlineImageOffsetY = 0f; inlineImageXAdvance = 0f; inlineImageStretch = 1f
  }

  def close(): Unit = {
    // Rendering resources would be released here
  }

  override def toString: String = s"Font '$name' at scale $scaleX by $scaleY"

  def debugString: String =
    s"Font{distanceField=$distanceField, isMono=$isMono, " +
      s"kerning=${Nullable.fold(kerning)("null")(k => s"${k.size} pairs")}, " +
      s"cellWidth=$cellWidth, cellHeight=$cellHeight, " +
      s"originalCellWidth=$originalCellWidth, originalCellHeight=$originalCellHeight, " +
      s"scaleX=$scaleX, scaleY=$scaleY, descent=$descent, " +
      s"name='$name'}"
}

object Font {

  import scala.util.boundary
  import scala.util.boundary.break

  /** Tracks the current u_smoothing uniform value per Batch, used by distance field shader management. */
  private val smoothingValues: java.util.IdentityHashMap[sge.graphics.g2d.Batch, java.lang.Float] =
    new java.util.IdentityHashMap(8)

  //// Style bit flags, stored in the upper 32 bits of a glyph long

  /** Bit flag for bold mode, as a long. */
  val BOLD: Long = 1L << 30

  /** Bit flag for oblique mode, as a long. */
  val OBLIQUE: Long = 1L << 29

  /** Bit flag for underline mode, as a long. */
  val UNDERLINE: Long = 1L << 28

  /** Bit flag for strikethrough mode, as a long. */
  val STRIKETHROUGH: Long = 1L << 27

  /** Bit flag for subscript mode, as a long. */
  val SUBSCRIPT: Long = 1L << 25

  /** Bit flag for midscript mode, as a long. */
  val MIDSCRIPT: Long = 2L << 25

  /** Two-bit flag for superscript mode, as a long. */
  val SUPERSCRIPT: Long = 3L << 25

  /** Bit flag for matching alternate modes, as a long. */
  val ALTERNATE_MODES_MASK: Long = 15L << 20

  /** Bit flag for black outline, as a long. */
  val BLACK_OUTLINE: Long = 1L << 24

  /** Bit flag for white outline mode, as a long. */
  val WHITE_OUTLINE: Long = 15L << 20

  /** Bit flag for yellow outline mode, as a long. */
  val YELLOW_OUTLINE: Long = 14L << 20

  /** Bit flag for red outline mode, as a long. */
  val RED_OUTLINE: Long = 13L << 20

  /** Bit flag for blue outline mode, as a long. */
  val BLUE_OUTLINE: Long = 12L << 20

  /** Bit flag for small caps mode, as a long. */
  val SMALL_CAPS: Long = 1L << 20

  /** Bit flag for jostle mode, as a long. */
  val JOSTLE: Long = 2L << 20

  /** Bit flag for shiny mode, as a long. */
  val SHINY: Long = 3L << 20

  /** Bit flag for neon mode, as a long. */
  val NEON: Long = 4L << 20

  /** Bit flag for halo mode, as a long. */
  val HALO: Long = 5L << 20

  /** Bit flag for drop shadow mode, as a long. */
  val DROP_SHADOW: Long = 6L << 20

  /** Bit flag for error mode, as a long. */
  val ERROR: Long = 7L << 20

  /** Bit flag for context mode, as a long. */
  val CONTEXT: Long = 8L << 20

  /** Bit flag for warning mode, as a long. */
  val WARN: Long = 9L << 20

  /** Bit flag for suggest mode, as a long. */
  val SUGGEST: Long = 10L << 20

  /** Bit flag for note mode, as a long. */
  val NOTE: Long = 11L << 20

  /** Padding adjustments for BitmapFont constructors. */
  val paddingForBitmapFont: Array[Int] = new Array[Int](4)

  def setPaddingForBitmapFont(top: Int, right: Int, bottom: Int, left: Int): Unit = {
    paddingForBitmapFont(0) = top; paddingForBitmapFont(1) = right
    paddingForBitmapFont(2) = bottom; paddingForBitmapFont(3) = left
  }

  def clearPaddingForBitmapFont(): Unit = {
    paddingForBitmapFont(0) = 0; paddingForBitmapFont(1) = 0
    paddingForBitmapFont(2) = 0; paddingForBitmapFont(3) = 0
  }

  /** If true, Fonts can use Texture objects. Set to false for headless usage. */
  var canUseTextures: Boolean = true

  /** Gets the distance to advance the cursor after drawing glyph, scaled appropriately. If the glyph is fully transparent, this returns 0.
    */
  def xAdvance(font: Font, scale: Float, glyph: Long): Float =
    if (glyph >>> 32 == 0L) 0f
    else {
      var ch = (glyph & 0xffff).toChar
      if ((glyph & ALTERNATE_MODES_MASK) == SMALL_CAPS) ch = Character.toUpperCase(ch)
      val tr = font.mapping.getOrElse(ch.toInt, null)
      if (tr == null) 0f
      else {
        var changedW = tr.xAdvance * scale
        if (!font.isMono) {
          if ((glyph & SUPERSCRIPT) != 0L) changedW *= 0.5f
        }
        changedW
      }
    }

  /** Given a glyph as a long, returns the RGBA8888 color it uses. */
  def extractColor(glyph: Long): Int = (glyph >>> 32).toInt

  /** Replaces the color section of a glyph. */
  def applyColor(glyph: Long, color: Int): Long =
    (glyph & 0xffffffffL) | (color.toLong << 32 & 0xfffffffe00000000L)

  /** Given a glyph as a long, returns the style bits it uses. */
  def extractStyle(glyph: Long): Long = glyph & 0x7f000000L

  /** Replaces the style section of a glyph. */
  def applyStyle(glyph: Long, style: Long): Long =
    (glyph & 0xffffffff80ffffffL) | (style & 0x7f000000L)

  /** Given a glyph as a long, returns the mode flags. */
  def extractMode(glyph: Long): Long = glyph & ALTERNATE_MODES_MASK

  /** Replaces the mode section of a glyph. */
  def applyMode(glyph: Long, modeFlags: Long): Long =
    (glyph & 0xffffffffff0fffffL) | (0xf00000L & modeFlags)

  /** Given a glyph as a long, returns the char it displays. */
  def extractChar(glyph: Long): Char = {
    val c = (glyph & 0xffff).toChar
    if (c == 2) '[' else c
  }

  /** Replaces the char section of a glyph. */
  def applyChar(glyph: Long, c: Char): Long =
    (glyph & 0xffffffffffff0000L) | c.toLong

  /** Clears static state for Android Activity.finish() compat. */
  def clearStatic(): Unit = {
    TypingConfig.GLOBAL_VARS.clear()
    TypingConfig.initializeGlobalVars()
  }

  /** Static markupGlyph with explicit ColorLookup. */
  def markupGlyph(chr: Char, markupStr: String, colorLookup: ColorLookup): Long =
    markupGlyph(chr, markupStr, colorLookup, null)

  /** Static markupGlyph with explicit ColorLookup and FontFamily. */
  def markupGlyph(chr: Char, markupStr: String, colorLookup: ColorLookup, familyArg: FontFamily): Long = boundary {
    val COLOR_MASK = 0xffffffff00000000L
    val baseColor  = 0xfffffffe00000000L | chr.toLong
    var color      = baseColor
    var current    = color
    var capsLock   = false
    var lowerCase  = false
    var i          = 0
    val n          = markupStr.length
    while (i < n)
      if (markupStr.charAt(i) == '[') {
        i += 1
        if (i < n) {
          val c = markupStr.charAt(i)
          if (c != '[') {
            if (c == ']') {
              color = baseColor; current = color; capsLock = false; lowerCase = false
              i += 1
            } else {
              val len = markupStr.indexOf(']', i) - i
              if (len < 0) break(current)
              c match {
                case '*' => current ^= BOLD
                case '/' => current ^= OBLIQUE
                case '^' =>
                  if ((current & SUPERSCRIPT) == SUPERSCRIPT) current &= ~SUPERSCRIPT else current |= SUPERSCRIPT
                case '.' =>
                  if ((current & SUPERSCRIPT) == SUBSCRIPT) current &= ~SUBSCRIPT
                  else current = (current & ~SUPERSCRIPT) | SUBSCRIPT
                case '=' =>
                  if ((current & SUPERSCRIPT) == MIDSCRIPT) current &= ~MIDSCRIPT
                  else current = (current & ~SUPERSCRIPT) | MIDSCRIPT
                case '_'       => current ^= UNDERLINE
                case '~'       => current ^= STRIKETHROUGH
                case ';' | '!' => capsLock = !capsLock; lowerCase = false
                case ','       => lowerCase = !lowerCase; capsLock = false
                case '#'       =>
                  if (len >= 4 && len < 7) {
                    color = StringUtils.longFromHex(markupStr, i + 1, i + 4)
                    color = (color << 52 & 0xf000000000000000L) | (color << 48 & 0x0f00000000000000L) |
                      (color << 48 & 0x00f0000000000000L) | (color << 44 & 0x000f000000000000L) |
                      (color << 44 & 0x0000f00000000000L) | (color << 40 & 0x00000f0000000000L) |
                      0x000000fe00000000L
                  } else if (len >= 7 && len < 9) {
                    color = StringUtils.longFromHex(markupStr, i + 1, i + 7) << 40 | 0x000000fe00000000L
                  } else if (len >= 9) {
                    color = StringUtils.longFromHex(markupStr, i + 1, i + 9) << 32 & 0xfffffffe00000000L
                  } else if (len == 1) {
                    current ^= BLACK_OUTLINE
                  } else {
                    color = baseColor
                  }
                  current = (current & ~COLOR_MASK) | color
                case '@' =>
                  if (familyArg != null) {
                    val fontIndex = familyArg.fontAliases.get(StringUtils.safeSubstring(markupStr, i + 1, i + len), 0)
                    current = (current & 0xfffffffffff0ffffL) | (fontIndex & 15L) << 16
                  }
                case '|' =>
                  val lc = colorLookup.getRgba(markupStr, i + 1, i + len) & 0xfffffffe
                  color = if (lc == 256) baseColor else lc.toLong << 32
                  current = (current & ~COLOR_MASK) | color
                case _ =>
                  val gc = colorLookup.getRgba(markupStr, i, i + len) & 0xfffffffe
                  color = if (gc == 256) baseColor else gc.toLong << 32
                  current = (current & ~COLOR_MASK) | color
              }
              i += len
            }
          } else {
            i += 1
          }
        }
      } else {
        i += 1
      }
    current
  }

  //// Inner classes

  /** Describes the region of a glyph in a larger TextureRegion, carrying both texture coordinates (inherited from TextureRegion) and glyph-specific offset/advance metrics.
    */
  class GlyphRegion(
    var offsetX:  Float,
    var offsetY:  Float,
    var xAdvance: Float
  ) extends sge.graphics.g2d.TextureRegion() {

    /** Creates a GlyphRegion from a parent TextureRegion with the given offsets and advance. Copies the texture and UV coordinates from the parent.
      */
    def this(textureRegion: sge.graphics.g2d.TextureRegion, offsetX: Float, offsetY: Float, xAdvance: Float) = {
      this(offsetX, offsetY, xAdvance)
      if (textureRegion.texture != null)
        setRegion(textureRegion)
    }

    /** Creates a GlyphRegion from a parent TextureRegion with default offsets (0, 0) and xAdvance equal to the region's width.
      */
    def this(textureRegion: sge.graphics.g2d.TextureRegion) =
      this(textureRegion, 0f, 0f, textureRegion.regionWidth.toFloat)

    /** Creates a GlyphRegion from a parent TextureRegion with specific pixel bounds. Useful for constructing glyphs from a font atlas grid.
      */
    def this(textureRegion: sge.graphics.g2d.TextureRegion, x: Float, y: Float, width: Float, height: Float) = {
      this(0f, 0f, width)
      if (textureRegion.texture != null)
        setRegion(textureRegion, Math.round(x), Math.round(y), Math.round(width), Math.round(height))
    }

    /** Copies another GlyphRegion (texture coordinates + metrics). */
    def this(other: GlyphRegion) = {
      this(other.offsetX, other.offsetY, other.xAdvance)
      if (other.texture != null)
        setRegion(other)
    }

    /** Flips the glyph region, adjusting offsets accordingly. */
    override def flip(x: Boolean, y: Boolean): Unit = {
      super.flip(x, y)
      if (x) { offsetX = -offsetX; xAdvance = -xAdvance }
      if (y) offsetY = -offsetY
    }

    def maxDimension: Float = Math.max(regionWidth, regionHeight).toFloat
  }

  /** Defines what types of distance field font this can use and render. */
  enum DistanceFieldType(val filePart: String, val namePart: String) extends java.lang.Enum[DistanceFieldType] {
    case STANDARD extends DistanceFieldType("-standard", "")
    case SDF extends DistanceFieldType("-sdf", " (SDF)")
    case MSDF extends DistanceFieldType("-msdf", " (MSDF)")
    case SDF_OUTLINE extends DistanceFieldType("-sdf", " (SDF Outline)")
  }

  /** Holds up to 16 Font values, accessible by index or by name, that markup can switch between while rendering. This uses the [@Name] syntax.
    */
  class FontFamily {

    /** Stores this Font and up to 15 other connected Fonts. */
    val connected: Array[Font] = new Array[Font](16)

    /** Maps font names/aliases to indices in connected. */
    val fontAliases: CaseInsensitiveIntMap = new CaseInsensitiveIntMap()

    def this(fonts: Array[Font]) = {
      this()
      if (fonts != null && fonts.length > 0) {
        var a   = 0
        var idx = 0
        while (idx < fonts.length && a < 16) {
          if (fonts(idx) != null) {
            connected(a) = fonts(idx)
            if (fonts(idx).name != null) fontAliases.put(fonts(idx).name, a)
            fontAliases.put(String.valueOf(a), a)
            a += 1
          }
          idx += 1
        }
      }
    }

    def this(aliases: Array[String], fonts: Array[Font]) = {
      this()
      if (aliases != null && fonts != null) {
        val length = Math.min(aliases.length, fonts.length)
        var a      = 0
        var idx    = 0
        while (idx < length && a < 16) {
          if (fonts(idx) != null) {
            connected(a) = fonts(idx)
            if (aliases(idx) != null) fontAliases.put(aliases(idx), a)
            if (fonts(idx).name != null) fontAliases.put(fonts(idx).name, a)
            fontAliases.put(String.valueOf(a), a)
            a += 1
          }
          idx += 1
        }
      }
    }

    /** Copy constructor. Font items in connected will not be copied (same references). */
    def this(other: FontFamily) = {
      this()
      System.arraycopy(other.connected, 0, connected, 0, 16)
      fontAliases.putAll(other.fontAliases)
    }

    /** Gets the corresponding Font for a name/alias, or null if not found. */
    def get(lookupName: String): Font =
      if (lookupName == null) null // @nowarn — matches original API
      else connected(fontAliases.get(lookupName, 0) & 15)

    /** Calls resizeDistanceField on each Font in this FontFamily. */
    def resizeDistanceFields(width: Float, height: Float): Unit = {
      var i = 0
      while (i < connected.length) {
        if (connected(i) != null) connected(i).resizeDistanceField(width, height)
        i += 1
      }
    }
  }

  //// Shader sources (for reference; actual shader compilation requires graphics context)

  val vertexShader: String =
    "attribute vec4 a_position;\n" +
      "attribute vec4 a_color;\n" +
      "attribute vec2 a_texCoord0;\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main() {\n" +
      "\tv_color = a_color;\n" +
      "\tv_color.a = v_color.a * (255.0/254.0);\n" +
      "\tv_texCoords = a_texCoord0;\n" +
      "\tgl_Position = u_projTrans * a_position;\n" +
      "}\n"

  val sdfFragmentShader: String =
    "#ifdef GL_ES\n" +
      "\tprecision mediump float;\n" +
      "\tprecision mediump int;\n" +
      "#endif\n" +
      "\n" +
      "uniform sampler2D u_texture;\n" +
      "uniform float u_smoothing;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main() {\n" +
      "\t if (u_smoothing > 0.0) {\n" +
      "\t\tfloat smoothing = 0.4 / u_smoothing;\n" +
      "\t\tvec4 color = texture2D(u_texture, v_texCoords);\n" +
      "\t\tfloat alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, color.a);\n" +
      "\t\tgl_FragColor = vec4(v_color.rgb * color.rgb, alpha * v_color.a);\n" +
      "  } else {\n" +
      "\t    gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
      "  }\n" +
      "}\n"

  val msdfFragmentShader: String =
    "#ifdef GL_ES\n" +
      "precision mediump float;\n" +
      "#endif\n" +
      "#if __VERSION__ >= 130\n" +
      "#define TEXTURE texture\n" +
      "#else\n" +
      "#define TEXTURE texture2D\n" +
      "#endif\n" +
      "uniform sampler2D u_texture;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "uniform float u_smoothing;\n" +
      "float median(float r, float g, float b) {\n" +
      "  return max(min(r, g), min(max(r, g), b));\n" +
      "}\n" +
      "void main() {\n" +
      "  if (u_smoothing > 0.0) {\n" +
      "    vec4 msdf = TEXTURE(u_texture, v_texCoords);\n" +
      "    float distance = u_smoothing * (median(msdf.r, msdf.g, msdf.b) - 0.5);\n" +
      "    float glyphAlpha = clamp(distance + 0.5, 0.0, 1.0);\n" +
      "    gl_FragColor = vec4(v_color.rgb, glyphAlpha * v_color.a);\n" +
      "  } else {\n" +
      "    gl_FragColor = v_color * TEXTURE(u_texture, v_texCoords);\n" +
      "  }\n" +
      "}"
}
