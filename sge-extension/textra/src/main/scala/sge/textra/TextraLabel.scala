/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraLabel.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class (no scene2d base), Batch → deferred,
 *     FloatArray/LongArray → ArrayBuffer[Float]/ArrayBuffer[Long],
 *     Skin → removed (FWSkin integration deferred), Align → Int constants,
 *     TransformDrawable → AnyRef placeholder
 *   Convention: getX()/setX() → public var where no logic.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *   TODOs: draw() requires Batch/rendering layer; deferred until scene2d wiring.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A scene2d.ui Widget that displays text using a Font rather than a libGDX BitmapFont. This supports being laid out in a Table. This permits square-bracket tag markup from Font. It does not support the
  * curly-brace token markup that its subclass TypingLabel does, nor does this handle input in the way TypingLabel can.
  */
class TextraLabel {

  var layout: Layout = new Layout()
  protected var font: Font = new Font()
  var align: Int = 8 // Align.left
  /** If true, allows text to wrap when it would go past the layout's targetWidth. */
  var wrap: Boolean = false
  var storedText: String = ""
  var style: Nullable[Styles.LabelStyle] = Nullable.empty
  protected var prefSizeInvalid: Boolean = true
  protected var defaultToken: String = ""

  // Widget-like fields (normally inherited from scene2d)
  private var _width:    Float = 0f
  private var _height:   Float = 0f
  private var _x:        Float = 0f
  private var _y:        Float = 0f
  private var _scaleX:   Float = 1f
  private var _scaleY:   Float = 1f
  private var _rotation: Float = 0f
  private val _originX:  Float = 0f
  private val _originY:  Float = 0f
  private val _color:    Color = new Color(Color.WHITE)

  def getX: Float = _x
  def getY: Float = _y
  def setX(x: Float): Unit = _x = x
  def setY(y: Float): Unit = _y = y
  def getScaleX: Float = _scaleX
  def getScaleY: Float = _scaleY
  def setScaleX(sx: Float): Unit = _scaleX = sx
  def setScaleY(sy: Float): Unit = _scaleY = sy
  def getRotation: Float = _rotation
  def setRotation(rot: Float): Unit = _rotation = rot
  def getOriginX: Float = _originX
  def getOriginY: Float = _originY
  def getColor: Color = _color
  def setColor(c: Color): Unit = if (c != null) _color.set(c)

  /** Creates a TextraLabel that uses the default font with white color. */
  def this(dummy: Unit) = {
    this()
    layout = new Layout()
    font = new Font()
    style = Nullable(new Styles.LabelStyle(font, Nullable.empty))
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken
  }

  /** Creates a TextraLabel with the given text and using the given style. */
  def this(text: String, style: Styles.LabelStyle) = {
    this()
    this.font = Nullable.fold(style.font)(new Font())(identity)
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text and style, using a replacement font. */
  def this(text: String, style: Styles.LabelStyle, replacementFont: Font) = {
    this()
    this.font = replacementFont
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text and font. */
  def this(text: String, font: Font) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text, font, and default color. */
  def this(text: String, font: Font, color: Color) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
  }

  /** Creates a TextraLabel with the given text, font, color, and justification. */
  def this(text: String, font: Font, color: Color, justify: Justify) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color)
    defaultToken = TypingConfig.getDefaultInitialText
    storedText = defaultToken + text
    font.markup(storedText, layout)
    layout.setJustification(justify)
  }

  def getWidth: Float = _width
  def getHeight: Float = _height

  def setWidth(width: Float): Unit = {
    _width = width
    layout.setTargetWidth(width)
    font.calculateSize(layout)
  }

  def setHeight(height: Float): Unit = {
    _height = height
    font.calculateSize(layout)
  }

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
    layout.setTargetWidth(width)
    font.calculateSize(layout)
  }

  def getPrefWidth: Float = {
    if (wrap) return 0f
    layout.getWidth
  }

  def getPrefHeight: Float = {
    layout.getHeight
  }

  /** A no-op unless font is a subclass that overrides Font.handleIntegerPosition(float). */
  def useIntegerPositions(integer: Boolean): TextraLabel = {
    font.integerPosition = integer
    this
  }

  def isWrap: Boolean = wrap

  def setWrap(wrap: Boolean): TextraLabel = {
    if (this.wrap != wrap) {
      this.wrap = wrap
    }
    this
  }

  def getAlignment: Int = align

  def setAlignment(alignment: Int): Unit = {
    align = alignment
  }

  def getFont: Font = font

  /** Sets the font and regenerates the layout. */
  def setFont(font: Font): Unit = {
    if (!this.font.eq(font)) {
      this.font = font
      regenerateLayout()
    }
  }

  /** Sets the font; only regenerates the layout if regenerate is true. */
  def setFont(font: Font, regenerate: Boolean): Unit = {
    if (!this.font.eq(font)) {
      this.font = font
      if (regenerate) regenerateLayout()
    }
  }

  /** Re-calculates line breaks when wrapping is enabled, and always re-calculates the size. */
  def regenerateLayout(): Unit = {
    font.regenerateLayout(layout)
    font.calculateSize(layout)
  }

  /** Changes the text in this TextraLabel to the given String, parsing any markup in it. */
  def setText(markupText: String): Unit = {
    storedText = defaultToken + markupText
    if (wrap) layout.setTargetWidth(_width)
    font.markup(storedText, layout.clear())
  }

  /** By default, does nothing; this is overridden in TypingLabel to skip its text progression ahead. */
  def skipToTheEnd(): TextraLabel = this

  def invalidate(): Unit = {
    prefSizeInvalid = true
  }

  def validate(): Unit = {
    prefSizeInvalid = false
  }

  /** Gets a glyph from this label's layout. */
  def getGlyph(index: Int): Long = {
    var idx = index
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) return glyphs(idx)
      else idx -= glyphs.size
      i += 1
    }
    0xffffffL
  }

  /** The maximum number of Lines this label can display. */
  def getMaxLines: Int = layout.maxLines

  /** Sets the maximum number of Lines this Layout can display; this is always at least 1. */
  def setMaxLines(maxLines: Int): Unit = {
    layout.setMaxLines(maxLines)
  }

  /** Gets the ellipsis, which may be null, or may be a String placed at the end of text if its max lines are exceeded. */
  def getEllipsis: Nullable[String] = layout.ellipsis

  /** Sets the ellipsis text. */
  def setEllipsis(ellipsis: Nullable[String]): Unit = {
    layout.setEllipsis(ellipsis)
  }

  /** Gets a String from the layout, made of only the char portions of the glyphs from start (inclusive) to end (exclusive). */
  def substring(start: Int, end: Int): String = {
    val s = Math.max(0, start)
    val e = Math.min(layout.countGlyphs, end)
    var index      = s
    val sb         = new StringBuilder(e - s)
    var glyphCount = 0
    var i          = 0
    val n          = layout.lineCount
    while (i < n && index >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (index < glyphs.size) {
        val fin = index - s - glyphCount + e
        while (index < fin && index < glyphs.size) {
          val c = (glyphs(index) & 0xffff).toChar
          if (c >= '\ue000' && c <= '\uf800') {
            Nullable.fold(font.namesByCharCode) {
              sb.append(c)
            } { nbc =>
              nbc.get(c.toInt) match {
                case Some(name) => sb.append(name)
                case None       => sb.append(c)
              }
            }
          } else {
            if (c == '\u0002') sb.append('[')
            else if (c != '\u200b') sb.append(c) // do not print zero-width space
          }
          glyphCount += 1
          index += 1
        }
        if (glyphCount == e - s) return sb.toString
        index = 0
      } else {
        index -= glyphs.size
      }
      i += 1
    }
    sb.toString
  }

  /** Gets the height of the Line containing the glyph at the given index. */
  def getLineHeight(index: Int): Float = {
    var idx = index
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) return layout.lines(i).height
      else idx -= glyphs.size
      i += 1
    }
    font.cellHeight
  }

  /** Contains one float per glyph; each is a rotation in degrees to apply to that glyph (around its center). */
  def getRotations: FloatArrayHelper = new FloatArrayHelper(layout.rotations)

  /** Contains two floats per glyph; even items are x offsets, odd items are y offsets. */
  def getOffsets: FloatArrayHelper = new FloatArrayHelper(layout.offsets)

  /** Contains two floats per glyph, as size multipliers; even items apply to x, odd items apply to y. */
  def getSizing: FloatArrayHelper = new FloatArrayHelper(layout.sizing)

  /** Contains one float per glyph; each is a multiplier that affects the x-advance of that glyph. */
  def getAdvances: FloatArrayHelper = new FloatArrayHelper(layout.advances)

  /** Returns the default token being used in this label. */
  def getDefaultToken: String = defaultToken

  /** Sets the default token being used in this label. */
  def setDefaultToken(defaultToken: String): Unit = {
    this.defaultToken = if (defaultToken == null) "" else defaultToken
  }

  override def toString: String = substring(0, Int.MaxValue)
}
