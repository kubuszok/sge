/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Label.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   TODO: direct Color.a mutation — update when Color becomes immutable
 *   TODO: Java-style getters/setters — getText/setText, getWrap/setWrap, getLabelAlign, getFontScaleX/Y, getGlyphLayout, getStyle/setStyle
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
class Label(text: Nullable[CharSequence], style: Label.LabelStyle)(using Sge) extends Widget with Styleable[Label.LabelStyle] {
  import Label._

  private var _style:           Label.LabelStyle   = scala.compiletime.uninitialized
  private val glyphLayout:      GlyphLayout        = GlyphLayout()
  private var prefWidth:        Float              = 0
  private var prefHeight:       Float              = 0
  private val _text:            DynamicArray[Char] = DynamicArray[Char]()
  private var intValue:         Int                = Int.MinValue
  private var cache:            BitmapFontCache    = scala.compiletime.uninitialized
  private var labelAlign:       Align              = Align.left
  private var lineAlign:        Align              = Align.left
  private var wrap:             Boolean            = false
  private var lastPrefHeight:   Float              = 0
  private var prefSizeInvalid:  Boolean            = true
  private var fontScaleX:       Float              = 1
  private var fontScaleY:       Float              = 1
  private var fontScaleChanged: Boolean            = false
  private var ellipsis:         Nullable[String]   = Nullable.empty

  text.foreach { t =>
    var i = 0
    while (i < t.length()) {
      _text.add(t.charAt(i))
      i += 1
    }
  }
  setStyle(style)
  if (text.isDefined && _text.nonEmpty) setSize(getPrefWidth, getPrefHeight)

  /** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name from the skin and the specified color.
    */
  def this(text: Nullable[CharSequence], skin: Skin)(using Sge) = this(text, skin.get(classOf[Label.LabelStyle]))

  def this(text: Nullable[CharSequence], skin: Skin, styleName: String)(using Sge) = this(text, skin.get(styleName, classOf[Label.LabelStyle]))

  def this(text: Nullable[CharSequence], skin: Skin, fontName: String, color: Color)(using Sge) =
    this(text, new Label.LabelStyle(skin.getFont(fontName), Nullable(color)))

  def this(text: Nullable[CharSequence], skin: Skin, fontName: String, colorName: String)(using Sge) =
    this(text, new Label.LabelStyle(skin.getFont(fontName), Nullable(skin.getColor(colorName))))

  override def setStyle(style: Label.LabelStyle): Unit = {
    this._style = style

    cache = style.font.newFontCache()
    invalidateHierarchy()
  }

  /** Returns the label's style. Modifying the returned style may not have an effect until {@link #setStyle(LabelStyle)} is called.
    */
  override def getStyle: Label.LabelStyle = _style

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

  def getText: DynamicArray[Char] = _text

  override def invalidate(): Unit = {
    super.invalidate()
    prefSizeInvalid = true
  }

  private def scaleAndComputePrefSize(): Unit = {
    val font      = cache.font
    val oldScaleX = font.scaleX
    val oldScaleY = font.scaleY
    if (fontScaleChanged) {
      font.data.scaleX = fontScaleX
      font.data.scaleY = fontScaleY
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
    if (wrap && ellipsis.isEmpty) {
      var width = getWidth
      _style.background.foreach { bg =>
        width = Math.max(width, bg.getMinWidth) - bg.getLeftWidth - bg.getRightWidth
      }
      layout.setText(cache.font, textStr, Color.WHITE, width, Align.left.asInstanceOf[Int], true)
    } else {
      layout.setText(cache.font, textStr)
    }
    prefWidth = layout.width
    prefHeight = layout.height
  }

  override def layout(): Unit = {
    val font      = cache.font
    val oldScaleX = font.scaleX
    val oldScaleY = font.scaleY
    if (fontScaleChanged) {
      font.data.scaleX = fontScaleX
      font.data.scaleY = fontScaleY
    }

    val doWrap = this.wrap && ellipsis.isEmpty
    if (doWrap) {
      val ph = getPrefHeight
      if (ph != lastPrefHeight) {
        lastPrefHeight = ph
        invalidateHierarchy()
      }
    }

    var width      = getWidth
    var height     = getHeight
    val background = _style.background
    var x          = 0f
    var y          = 0f
    background.foreach { bg =>
      x = bg.getLeftWidth
      y = bg.getBottomHeight
      width -= bg.getLeftWidth + bg.getRightWidth
      height -= bg.getBottomHeight + bg.getTopHeight
    }

    val textStr = new String(_text.toArray)
    val layout  = this.glyphLayout
    var textWidth:  Float = 0
    var textHeight: Float = 0
    if (doWrap || _text.contains('\n')) {
      // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
      layout.setText(font, textStr, 0, textStr.length, Color.WHITE, width, lineAlign.asInstanceOf[Int], doWrap, ellipsis)
      textWidth = layout.width
      textHeight = layout.height

      if (!labelAlign.isLeft) {
        if (labelAlign.isRight)
          x += width - textWidth
        else
          x += (width - textWidth) / 2
      }
    } else {
      textWidth = width
      textHeight = font.data.capHeight
    }

    if (labelAlign.isTop) {
      y += (if (cache.font.flipped) 0 else height - textHeight)
      y += _style.font.descent
    } else if (labelAlign.isBottom) {
      y += (if (cache.font.flipped) height - textHeight else 0)
      y -= _style.font.descent
    } else {
      y += (height - textHeight) / 2
    }
    if (!cache.font.flipped) y += textHeight

    layout.setText(font, textStr, 0, textStr.length, Color.WHITE, textWidth, lineAlign.asInstanceOf[Int], doWrap, ellipsis)
    cache.setText(layout, x, y)

    if (fontScaleChanged) {
      font.data.scaleX = oldScaleX
      font.data.scaleY = oldScaleY
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    val color = tempColor.set(getColor)
    color.a *= parentAlpha
    _style.background.foreach { bg =>
      batch.setColor(color.r, color.g, color.b, color.a)
      bg.draw(batch, getX, getY, getWidth, getHeight)
    }
    _style.fontColor.foreach { fc =>
      color.mul(fc)
    }
    cache.tint(color)
    cache.setPosition(getX, getY)
    cache.draw(batch)
  }

  override def getPrefWidth: Float =
    if (wrap) 0
    else {
      if (prefSizeInvalid) scaleAndComputePrefSize()
      var width = prefWidth
      _style.background.foreach { bg =>
        width = Math.max(width + bg.getLeftWidth + bg.getRightWidth, bg.getMinWidth)
      }
      width
    }

  override def getPrefHeight: Float = {
    if (prefSizeInvalid) scaleAndComputePrefSize()
    var descentScaleCorrection = 1f
    if (fontScaleChanged) descentScaleCorrection = fontScaleY / _style.font.scaleY
    var height = prefHeight - _style.font.descent * descentScaleCorrection * 2
    _style.background.foreach { bg =>
      height = Math.max(height + bg.getTopHeight + bg.getBottomHeight, bg.getMinHeight)
    }
    height
  }

  def getGlyphLayout: GlyphLayout = glyphLayout

  /** If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds. If true, the text will word wrap using the width of the label. The
    * preferred width of the label will be 0, it is expected that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false. <p> When wrap
    * is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label will need to layout twice: once to set the width of the label and a second time
    * to adjust to the label's new preferred height.
    */
  def setWrap(wrap: Boolean): Unit = {
    this.wrap = wrap
    invalidateHierarchy()
  }

  def getWrap: Boolean = wrap

  def getLabelAlign: Align = labelAlign

  def getLineAlign: Align = lineAlign

  /** @param labelAlign
    *   Aligns all the text within the label (default left center).
    * @param lineAlign
    *   Aligns each line of text horizontally (default left).
    * @see
    *   Align
    */
  def setAlignment(labelAlign: Align, lineAlign: Align = labelAlign): Unit = {
    this.labelAlign = labelAlign

    if (lineAlign.isLeft)
      this.lineAlign = Align.left
    else if (lineAlign.isRight)
      this.lineAlign = Align.right
    else
      this.lineAlign = Align.center

    invalidate()
  }

  def setFontScale(fontScaleX: Float, fontScaleY: Float = fontScaleX): Unit = {
    fontScaleChanged = true
    this.fontScaleX = fontScaleX
    this.fontScaleY = fontScaleY
    invalidateHierarchy()
  }

  def getFontScaleX: Float = fontScaleX

  def setFontScaleX(fontScaleX: Float): Unit =
    setFontScale(fontScaleX, fontScaleY)

  def getFontScaleY: Float = fontScaleY

  def setFontScaleY(fontScaleY: Float): Unit =
    setFontScale(fontScaleX, fontScaleY)

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
  protected def getBitmapFontCache: BitmapFontCache = cache

  override def toString: String =
    getName.getOrElse {
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
