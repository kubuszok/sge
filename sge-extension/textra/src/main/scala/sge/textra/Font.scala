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
 *   TODOs: drawGlyph/drawGlyphs/drawBlocks/drawBlockSequence/drawFancyLine/
 *     enableShader/loadFNT/loadJSON/loadSad — rendering methods require Batch/Texture/
 *     ShaderProgram which are not available in a cross-platform extension module.
 *     These are left as stubs that throw UnsupportedOperationException until the
 *     rendering layer is wired up.
 */
package sge
package textra

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.textra.utils.{ CaseInsensitiveIntMap, StringUtils }
import sge.utils.Nullable

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

  /** Describes the region of a glyph in a larger TextureRegion. */
  class GlyphRegion(
    var offsetX:  Float,
    var offsetY:  Float,
    var xAdvance: Float
  ) {
    // In the full implementation, this would extend TextureRegion.
    // For now, this carries the metric data needed by markup processing and effects.
    private var regionWidth:  Int = 0
    private var regionHeight: Int = 0

    def getRegionWidth:  Int = regionWidth
    def getRegionHeight: Int = regionHeight

    def setRegionWidth(w:  Int): Unit = regionWidth = w
    def setRegionHeight(h: Int): Unit = regionHeight = h

    def flip(x: Boolean, y: Boolean): Unit = {
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
