/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/ImageTextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button → standalone class (scene2d base not inherited;
 *     button state exposed as mutable fields),
 *     Image/Cell → Nullable placeholder, Scaling → deferred
 *   Convention: Image text button state and rendering preserved in API.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 188
 * Covenant-baseline-methods: ImageTextraButton,_hasKeyboardFocus,_imageDrawable,_isChecked,_isDisabled,_isOver,_isPressed,_style,c,className,dotIndex,draw,focused,getFontColor,getImage,getImageDrawable,getImageDrawableCurrent,getLabel,getStyle,getText,hasKeyboardFocus,hasKeyboardFocus_,isChecked,isChecked_,isDisabled,isDisabled_,isOver,isOver_,isPressed,isPressed_,label,name,newLabel,setChecked,setLabel,setStyle,setText,skipToTheEnd,this,toString,updateImage
 * Covenant-source-reference: com/github/tommyettinger/textra/ImageTextraButton.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.utils.{ Align, Nullable }

/** A button with a child Image and TextraLabel. */
class ImageTextraButton(
  text:            Nullable[String],
  style:           Styles.ImageTextButtonStyle,
  replacementFont: Font
) {

  private var _style: Styles.ImageTextButtonStyle = style
  private var label:  TextraLabel                 = newLabel(
    Nullable.fold(text)("")(identity),
    replacementFont,
    Nullable.fold(style.fontColor)(Color.WHITE)(identity)
  )
  label.setAlignment(Align.center)

  /** The current image drawable being rendered. */
  private var _imageDrawable: Nullable[AnyRef] = Nullable.empty

  // Button state fields (normally inherited from scene2d Button)
  private var _isChecked:        Boolean = false
  private var _isPressed:        Boolean = false
  private var _isOver:           Boolean = false
  private var _isDisabled:       Boolean = false
  private var _hasKeyboardFocus: Boolean = false

  def this(text: Nullable[String], style: Styles.ImageTextButtonStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  def setStyle(style: Styles.ImageTextButtonStyle): Unit = {
    this._style = style

    updateImage()

    Nullable.foreach(style.font)(f => label.setFont(new Font(f)))
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
  }

  def setStyle(style: Styles.ImageTextButtonStyle, makeGridGlyphs: Boolean): Unit = {
    this._style = style

    updateImage()

    Nullable.foreach(style.font)(f => label.setFont(f))
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
  }

  def setStyle(style: Styles.ImageTextButtonStyle, font: Font): Unit = {
    this._style = style

    updateImage()

    label.setFont(font)
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
  }

  def getStyle: Styles.ImageTextButtonStyle = _style

  /** Returns the appropriate image drawable from the style based on the current button state. */
  protected def getImageDrawable: Nullable[AnyRef] = boundary {
    if (_isDisabled && _style.imageDisabled.isDefined) break(_style.imageDisabled)
    if (_isPressed) {
      if (_isChecked && _style.imageCheckedDown.isDefined) break(_style.imageCheckedDown)
      if (_style.imageDown.isDefined) break(_style.imageDown)
    }
    if (_isOver) {
      if (_isChecked) {
        if (_style.imageCheckedOver.isDefined) break(_style.imageCheckedOver)
      } else {
        if (_style.imageOver.isDefined) break(_style.imageOver)
      }
    }
    if (_isChecked) {
      if (_style.imageChecked.isDefined) break(_style.imageChecked)
      if (_isOver && _style.imageOver.isDefined) break(_style.imageOver)
    }
    _style.imageUp
  }

  /** Sets the image drawable based on the current button state. The default implementation sets the image drawable using {@link #getImageDrawable()}.
    */
  protected def updateImage(): Unit =
    _imageDrawable = getImageDrawable

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = boundary {
    if (_isDisabled && _style.disabledFontColor.isDefined) break(_style.disabledFontColor)
    if (_isPressed) {
      if (_isChecked && _style.checkedDownFontColor.isDefined) break(_style.checkedDownFontColor)
      if (_style.downFontColor.isDefined) break(_style.downFontColor)
    }
    if (_isOver) {
      if (_isChecked) {
        if (_style.checkedOverFontColor.isDefined) break(_style.checkedOverFontColor)
      } else {
        if (_style.overFontColor.isDefined) break(_style.overFontColor)
      }
    }
    val focused = _hasKeyboardFocus
    if (_isChecked) {
      if (focused && _style.checkedFocusedFontColor.isDefined) break(_style.checkedFocusedFontColor)
      if (_style.checkedFontColor.isDefined) break(_style.checkedFontColor)
      if (_isOver && _style.overFontColor.isDefined) break(_style.overFontColor)
    }
    if (focused && _style.focusedFontColor.isDefined) break(_style.focusedFontColor)
    _style.fontColor
  }

  def draw(batch: Batch, parentAlpha: Float): Unit = {
    updateImage()
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
    label.draw(batch, parentAlpha)
  }

  /** Returns the current image drawable. */
  def getImageDrawableCurrent: Nullable[AnyRef] = _imageDrawable

  /** Returns the image drawable in use (from style state). Equivalent to getImageDrawable but named to match the original API which returns an Image widget.
    */
  def getImage: Nullable[AnyRef] = _imageDrawable

  def setLabel(newLabel: TextraLabel): Unit =
    this.label = newLabel

  def getLabel: TextraLabel = label

  def setText(text: CharSequence): Unit =
    label.setText(text.toString)

  def getText: String = label.toString

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    label.skipToTheEnd()

  // Button state accessors
  def isChecked:                          Boolean = _isChecked
  def isChecked_=(value:        Boolean): Unit    = _isChecked = value
  def setChecked(value:         Boolean): Unit    = _isChecked = value
  def isPressed:                          Boolean = _isPressed
  def isPressed_=(value:        Boolean): Unit    = _isPressed = value
  def isOver:                             Boolean = _isOver
  def isOver_=(value:           Boolean): Unit    = _isOver = value
  def isDisabled:                         Boolean = _isDisabled
  def isDisabled_=(value:       Boolean): Unit    = _isDisabled = value
  def hasKeyboardFocus:                   Boolean = _hasKeyboardFocus
  def hasKeyboardFocus_=(value: Boolean): Unit    = _hasKeyboardFocus = value

  override def toString: String = {
    val name      = getClass.getName
    val dotIndex  = name.lastIndexOf('.')
    val className = if (dotIndex != -1) name.substring(dotIndex + 1) else name
    (if (className.indexOf('$') != -1) "ImageTextraButton " else "") + className + ": " + _imageDrawable + " " + label.toString
  }
}
