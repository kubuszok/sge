/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Label.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   TODO: direct Color.a mutation — update when Color becomes immutable
 *   Fixes: Removed redundant Java-style getters/setters (getGlyphLayout removed — field accessible, getWrap→wrap property, getLabelAlign/getLineAlign→labelAlign/lineAlign properties, getFontScaleX/Y→fontScaleX/Y properties, getBitmapFontCache→bitmapFontCache; style via Styleable; getText→text (returns _text backing field))
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont, BitmapFontCache, GlyphLayout }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, DynamicArray, Nullable }

/** A text label, with optional word wrapping. <p> The preferred size of the label is determined by the actual text bounds, unless {@link #setWrap(boolean) word wrap} is enabled.
  * @author
  *   Nathan Sweet
  */
class Label(initialText: Nullable[CharSequence], initialStyle: Label.LabelStyle)(using Sge) extends Widget with Styleable[Label.LabelStyle] {
  import Label._

  private var _style:           Label.LabelStyle   = scala.compiletime.uninitialized
  val glyphLayout:              GlyphLayout        = GlyphLayout()
  private var _prefWidth:       Float              = 0
  private var _prefHeight:      Float              = 0
  private val _text:            DynamicArray[Char] = DynamicArray[Char]()
  private var intValue:         Int                = Int.MinValue
  private var _cache:           BitmapFontCache    = scala.compiletime.uninitialized
  private var _labelAlign:      Align              = Align.left
  private var _lineAlign:       Align              = Align.left
  private var _wrap:            Boolean            = false
  private var lastPrefHeight:   Float              = 0
  private var prefSizeInvalid:  Boolean            = true
  private var _fontScaleX:      Float              = 1
  private var _fontScaleY:      Float              = 1
  private var fontScaleChanged: Boolean            = false
  private var ellipsis:         Nullable[String]   = Nullable.empty

  initialText.foreach { t =>
    var i = 0
    while (i < t.length()) {
      _text.add(t.charAt(i))
      i += 1
    }
  }
  setStyle(initialStyle)
  if (initialText.isDefined && _text.nonEmpty) setSize(prefWidth, prefHeight)

  /** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name from the skin and the specified color.
    */
  def this(text: Nullable[CharSequence], skin: Skin)(using Sge) = this(text, skin.get[Label.LabelStyle])

  def this(text: Nullable[CharSequence], skin: Skin, styleName: String)(using Sge) = this(text, skin.get[Label.LabelStyle](styleName))

  def this(text: Nullable[CharSequence], skin: Skin, fontName: String, color: Color)(using Sge) = {
    this(text, new Label.LabelStyle(skin.getFont(fontName), Nullable(color)))
  }

  def this(text: Nullable[CharSequence], skin: Skin, fontName: String, colorName: String)(using Sge) = {
    this(text, new Label.LabelStyle(skin.getFont(fontName), Nullable(skin.getColor(colorName))))
  }

  override def setStyle(style: Label.LabelStyle): Unit = {
    this._style = style

    _cache = style.font.newFontCache()
    invalidateHierarchy()
  }

  /** Returns the label's style. Modifying the returned style may not have an effect until {@link #setStyle(LabelStyle)} is called.
    */
  override def style: Label.LabelStyle = _style

  /** Sets the text to the specified integer value. If the text is already equivalent to the specified value, a string is not allocated.
    * @return
    *   true if the text was changed.
    */
  def setText(value: Int): Boolean =
    if (this.intValue == value) false
    else {
      _text.clear()
      val s = value.toString
      var i = 0
      while (i < s.length) {
        _text.add(s.charAt(i))
        i += 1
      }
      intValue = value
      invalidateHierarchy()
      true
    }

  /** @param newText If null, "" will be used. */
  def setText(newText: Nullable[CharSequence]): Unit = scala.util.boundary {
    newText.fold {
      if (_text.isEmpty) scala.util.boundary.break(())
      _text.clear()
    } { nt =>
      if (textEquals(nt)) scala.util.boundary.break(())
      _text.clear()
      var i = 0
      while (i < nt.length()) {
        _text.add(nt.charAt(i))
        i += 1
      }
    }
    intValue = Int.MinValue
    invalidateHierarchy()
  }

  def textEquals(other: CharSequence): Boolean = {
    val length = _text.size
    val chars  = _text.toArray
    if (length != other.length()) false
    else {
      var i     = 0
      var equal = true
      while (i < length && equal) {
        if (chars(i) != other.charAt(i)) equal = false
        i += 1
      }
      equal
    }
  }

  def text: DynamicArray[Char] = _text

  override def invalidate(): Unit = {
    super.invalidate()
    prefSizeInvalid = true
  }

  private def scaleAndComputePrefSize(): Unit = {
    val font      = _cache.font
    val oldScaleX = font.scaleX
    val oldScaleY = font.scaleY
    if (fontScaleChanged) {
      font.data.scaleX = _fontScaleX
      font.data.scaleY = _fontScaleY
    }

    computePrefSize(Label.prefSizeLayout)

    if (fontScaleChanged) {
      font.data.scaleX = oldScaleX
      font.data.scaleY = oldScaleY
    }
  }

  protected def computePrefSize(layout: GlyphLayout): Unit = {
    prefSizeInvalid = false
    val textStr = new String(_text.toArray)
    if (_wrap && ellipsis.isEmpty) {
      var width = this.width
      _style.background.foreach { bg =>
        width = Math.max(width, bg.minWidth) - bg.leftWidth - bg.rightWidth
      }
      layout.setText(_cache.font, textStr, Color.WHITE, width, Align.left.asInstanceOf[Int], true)
    } else {
      layout.setText(_cache.font, textStr)
    }
    _prefWidth = layout.width
    _prefHeight = layout.height
  }

  override def layout(): Unit = {
    val font      = _cache.font
    val oldScaleX = font.scaleX
    val oldScaleY = font.scaleY
    if (fontScaleChanged) {
      font.data.scaleX = _fontScaleX
      font.data.scaleY = _fontScaleY
    }

    val doWrap = this._wrap && ellipsis.isEmpty
    if (doWrap) {
      val ph = prefHeight
      if (ph != lastPrefHeight) {
        lastPrefHeight = ph
        invalidateHierarchy()
      }
    }

    var width      = this.width
    var height     = this.height
    val background = _style.background
    var x          = 0f
    var y          = 0f
    background.foreach { bg =>
      x = bg.leftWidth
      y = bg.bottomHeight
      width -= bg.leftWidth + bg.rightWidth
      height -= bg.bottomHeight + bg.topHeight
    }

    val textStr = new String(_text.toArray)
    val layout  = this.glyphLayout
    var textWidth:  Float = 0
    var textHeight: Float = 0
    if (doWrap || _text.contains('\n')) {
      // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
      layout.setText(font, textStr, 0, textStr.length, Color.WHITE, width, _lineAlign.asInstanceOf[Int], doWrap, ellipsis)
      textWidth = layout.width
      textHeight = layout.height

      if (!_labelAlign.isLeft) {
        if (_labelAlign.isRight)
          x += width - textWidth
        else
          x += (width - textWidth) / 2
      }
    } else {
      textWidth = width
      textHeight = font.data.capHeight
    }

    if (_labelAlign.isTop) {
      y += (if (_cache.font.flipped) 0 else height - textHeight)
      y += _style.font.descent
    } else if (_labelAlign.isBottom) {
      y += (if (_cache.font.flipped) height - textHeight else 0)
      y -= _style.font.descent
    } else {
      y += (height - textHeight) / 2
    }
    if (!_cache.font.flipped) y += textHeight

    layout.setText(font, textStr, 0, textStr.length, Color.WHITE, textWidth, _lineAlign.asInstanceOf[Int], doWrap, ellipsis)
    _cache.setText(layout, x, y)

    if (fontScaleChanged) {
      font.data.scaleX = oldScaleX
      font.data.scaleY = oldScaleY
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    val c = tempColor.set(this.color)
    c.a *= parentAlpha
    _style.background.foreach { bg =>
      batch.setColor(c.r, c.g, c.b, c.a)
      bg.draw(batch, x, y, width, height)
    }
    _style.fontColor.foreach { fc =>
      c.mul(fc)
    }
    _cache.tint(c)
    _cache.setPosition(x, y)
    _cache.draw(batch)
  }

  override def prefWidth: Float =
    if (wrap) 0
    else {
      if (prefSizeInvalid) scaleAndComputePrefSize()
      var width = _prefWidth
      _style.background.foreach { bg =>
        width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
      }
      width
    }

  override def prefHeight: Float = {
    if (prefSizeInvalid) scaleAndComputePrefSize()
    var descentScaleCorrection = 1f
    if (fontScaleChanged) descentScaleCorrection = _fontScaleY / _style.font.scaleY
    var height = _prefHeight - _style.font.descent * descentScaleCorrection * 2
    _style.background.foreach { bg =>
      height = Math.max(height + bg.topHeight + bg.bottomHeight, bg.minHeight)
    }
    height
  }

  def wrap: Boolean = _wrap

  /** If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds. If true, the text will word wrap using the width of the label. The
    * preferred width of the label will be 0, it is expected that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false. <p> When wrap
    * is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label will need to layout twice: once to set the width of the label and a second time
    * to adjust to the label's new preferred height.
    */
  def wrap_=(wrap: Boolean): Unit = {
    this._wrap = wrap
    invalidateHierarchy()
  }

  /** @deprecated Use `wrap = value` instead */
  def setWrap(wrap: Boolean): Unit = this.wrap_=(wrap)

  def labelAlign: Align = _labelAlign

  def lineAlign: Align = _lineAlign

  /** @param labelAlign
    *   Aligns all the text within the label (default left center).
    * @param lineAlign
    *   Aligns each line of text horizontally (default left).
    * @see
    *   Align
    */
  def setAlignment(newLabelAlign: Align): Unit =
    setAlignment(newLabelAlign, newLabelAlign)

  def setAlignment(newLabelAlign: Align, newLineAlign: Align): Unit = {
    this._labelAlign = newLabelAlign

    if (newLineAlign.isLeft)
      this._lineAlign = Align.left
    else if (newLineAlign.isRight)
      this._lineAlign = Align.right
    else
      this._lineAlign = Align.center

    invalidate()
  }

  def fontScaleX: Float = _fontScaleX

  def fontScaleY: Float = _fontScaleY

  def setFontScale(scale: Float): Unit = setFontScale(scale, scale)

  def setFontScale(newFontScaleX: Float, newFontScaleY: Float): Unit = {
    fontScaleChanged = true
    this._fontScaleX = newFontScaleX
    this._fontScaleY = newFontScaleY
    invalidateHierarchy()
  }

  def setFontScaleX(scaleX: Float): Unit =
    setFontScale(scaleX, _fontScaleY)

  def setFontScaleY(scaleY: Float): Unit =
    setFontScale(_fontScaleX, scaleY)

  /** When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
    */
  def setEllipsis(ellipsis: Nullable[String]): Unit =
    this.ellipsis = ellipsis

  /** When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when ellipsis is true. Default is false.
    */
  def setEllipsis(ellipsis: Boolean): Unit =
    if (ellipsis)
      this.ellipsis = Nullable("...")
    else
      this.ellipsis = Nullable.empty

  /** Allows subclasses to access the cache in {@link #draw(Batch, float)}. */
  protected def bitmapFontCache: BitmapFontCache = _cache

  override def toString: String =
    name.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "Label " else "") + className + ": " + new String(_text.toArray)
    }
}

object Label {
  private val tempColor:      Color       = Color()
  private val prefSizeLayout: GlyphLayout = GlyphLayout()

  /** The style for a label, see {@link Label}.
    * @author
    *   Nathan Sweet
    */
  class LabelStyle() {
    var font:       BitmapFont         = scala.compiletime.uninitialized
    var fontColor:  Nullable[Color]    = Nullable.empty
    var background: Nullable[Drawable] = Nullable.empty

    def this(font: BitmapFont, fontColor: Nullable[Color]) = {
      this()
      this.font = font
      this.fontColor = fontColor
    }

    def this(style: LabelStyle) = {
      this()
      font = style.font
      fontColor = style.fontColor.map(c => Color(c))
      background = style.background
    }
  }
}
