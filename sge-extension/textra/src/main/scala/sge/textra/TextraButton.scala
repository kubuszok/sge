/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button → standalone class (scene2d base not inherited;
 *     button state exposed as mutable fields),
 *     Cell → Nullable.empty placeholder, Skin → removed
 *   Convention: getX()/setX() → public var or def pairs.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 187
 * Covenant-baseline-methods: TextraButton,_hasKeyboardFocus,_height,_isChecked,_isDisabled,_isOver,_isPressed,_style,_width,c,draw,focused,getFontColor,getHeight,getName,getPrefHeight,getPrefWidth,getStyle,getText,getTextraLabel,getTextraLabelCell,getWidth,hasKeyboardFocus,hasKeyboardFocus_,isChecked,isChecked_,isDisabled,isDisabled_,isOver,isOver_,isPressed,isPressed_,label,name,newLabel,setChecked,setSize,setStyle,setText,setTextraLabel,skipToTheEnd,this,toString,useIntegerPositions
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraButton.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.utils.{ Align, Nullable }

/** A button with a child {@link TextraLabel} to display text.
  * @author
  *   Nathan Sweet
  */
class TextraButton(text: Nullable[String], style: Styles.TextButtonStyle, replacementFont: Font) {

  private var label: TextraLabel = newLabel(
    Nullable.fold(text)("")(identity),
    replacementFont,
    Color.WHITE
  )
  label.setAlignment(Align.center)

  private var _style: Styles.TextButtonStyle = style

  // Name field (normally inherited from scene2d Actor)
  var name: Nullable[String] = Nullable.empty

  def getName: Nullable[String] = name

  // Preferred size (normally inherited from scene2d Widget/Table)
  private var _width:  Float = 0f
  private var _height: Float = 0f

  // Button state fields (normally inherited from scene2d Button)
  private var _isChecked:        Boolean = false
  private var _isPressed:        Boolean = false
  private var _isOver:           Boolean = false
  private var _isDisabled:       Boolean = false
  private var _hasKeyboardFocus: Boolean = false

  // Initialize size from preferred dimensions (as original does in constructor)
  _width = getPrefWidth
  _height = getPrefHeight

  def this(text: Nullable[String], style: Styles.TextButtonStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  def setStyle(style: Styles.TextButtonStyle): Unit = {
    this._style = style
    Nullable.foreach(style.font)(f => label.setFont(f))
    Nullable.foreach(style.fontColor)(c => label.setColor(c))
  }

  def setStyle(style: Styles.TextButtonStyle, makeGridGlyphs: Boolean): Unit = {
    this._style = style
    Nullable.foreach(style.font)(f => label.setFont(f))
    Nullable.foreach(style.fontColor)(c => label.setColor(c))
  }

  def setStyle(style: Styles.TextButtonStyle, font: Font): Unit = {
    this._style = style
    label.setFont(font)
    Nullable.foreach(style.fontColor)(c => label.setColor(c))
  }

  def getStyle: Styles.TextButtonStyle = _style

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
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
    label.draw(batch, parentAlpha)
  }

  def setTextraLabel(newLabel: TextraLabel): Unit = {
    require(newLabel != null, "label cannot be null.")
    if (!(this.label eq newLabel)) {
      this.label = newLabel
    }
  }

  def getTextraLabel: TextraLabel = label

  /** Returns the Cell containing the label (standalone: no Table backing, returns Nullable.empty). */
  def getTextraLabelCell: Nullable[AnyRef] = Nullable.empty

  /** Returns the preferred width based on the label. */
  def getPrefWidth: Float = label.getPrefWidth

  /** Returns the preferred height based on the label. */
  def getPrefHeight: Float = label.getPrefHeight

  def getWidth: Float = _width

  def getHeight: Float = _height

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
  }

  /** A no-op unless {@code label.getFont()} is a subclass that overrides {@link Font#handleIntegerPosition(float)}.
    * @param integer
    *   usually ignored
    * @return
    *   this for chaining
    */
  def useIntegerPositions(integer: Boolean): TextraButton = {
    label.getFont.integerPosition = integer
    this
  }

  def setText(text: Nullable[String]): Unit =
    label.setText(Nullable.fold(text)("")(identity))

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

  override def toString: String =
    if (name.isDefined) name.get
    else {
      val className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      val cn        = if (dotIndex != -1) className.substring(dotIndex + 1) else className
      (if (cn.indexOf('$') != -1) "TextraButton " else "") + cn + ": " + label.toString
    }
}
