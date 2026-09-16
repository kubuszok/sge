/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: TextraButton extends sge.scenes.scene2d.ui.Button
 *     (Button extends Table extends WidgetGroup extends Actor), so button
 *     state (isChecked/isPressed/isOver/disabled/hasKeyboardFocus/toggle/
 *     ButtonGroup membership) and the Table/Cell backing are inherited rather
 *     than hand-rolled. The textra Styles.TextButtonStyle is flattened (it is
 *     NOT a scene2d Button.ButtonStyle), so the inherited Button carries an
 *     empty Button.ButtonStyle for its own background/offset machinery while
 *     the textra style drives the label font/color. Upstream's inherited Java
 *     getters (getName/getPrefWidth/getPrefHeight/getWidth/getHeight/
 *     isDisabled) are exposed as thin accessors over the renamed scene2d
 *     properties so the rest of the textra package keeps compiling.
 *   Convention: getX()/setX() → public var or def pairs.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 216
 * Covenant-baseline-methods: TextraButton,_style,c,draw,focused,getFontColor,getHeight,getName,getPrefHeight,getPrefWidth,getStyle,getText,getTextraLabel,getTextraLabelCell,getWidth,isDisabled,isDisabled_,label,newLabel,setStyle,setText,setTextraLabel,skipToTheEnd,this,toString,useIntegerPositions
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraButton.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import lowlevel.Nullable
import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.{ Button, Cell, Skin }
import sge.utils.Align

/** A button with a child {@link TextraLabel} to display text.
  * @author
  *   Nathan Sweet
  */
class TextraButton(text: Nullable[String], style: Styles.TextButtonStyle, replacementFont: Font)(using Sge) extends Button() {

  private var label: TextraLabel = newLabel(
    Nullable.fold(text)("")(identity),
    replacementFont,
    Color.WHITE
  )
  label.setAlignment(Align.center)

  private var _style: Styles.TextButtonStyle = style

  // Upstream `super()` then `setStyle(style, replacementFont)` then
  // `add(label).expand().fill()` then `setSize(getPrefWidth(), getPrefHeight())`
  // (TextraButton.java:62-69). The label is added to the Table backing this
  // Button and so gets a real Cell (TextraButton.java:156 `getCell(label)`).
  setStyle(style, replacementFont)
  add(Nullable[Actor](label)).expand().fill()
  setSize(getPrefWidth, getPrefHeight)

  /** The button's name. Upstream reads `getName()` (inherited from Actor); SGE exposes the inherited `name` property under the Java getter the textra package is written against.
    */
  def getName: Nullable[String] = name

  def this(text: Nullable[String], skin: Skin)(using Sge) = {
    this(text, skin.get[Styles.TextButtonStyle])
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) = {
    this(text, skin.get[Styles.TextButtonStyle](styleName))
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], style: Styles.TextButtonStyle)(using Sge) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  def this(text: Nullable[String], skin: Skin, replacementFont: Font)(using Sge) = {
    this(text, skin.get[Styles.TextButtonStyle], replacementFont)
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String, replacementFont: Font)(using Sge) = {
    this(text, skin.get[Styles.TextButtonStyle](styleName), replacementFont)
    setSkin(Nullable(skin))
  }

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  def setStyle(style: Styles.TextButtonStyle): Unit =
    setStyle(style, false)

  def setStyle(style: Styles.TextButtonStyle, makeGridGlyphs: Boolean): Unit = {
    this._style = style
    // The inherited Button carries an empty scene2d ButtonStyle for its own
    // background/offset machinery; the textra Styles.TextButtonStyle (flattened,
    // not a scene2d Button.ButtonStyle) drives only the label font/color here.
    super.setStyle(new Button.ButtonStyle())

    Nullable(label).foreach { l =>
      Nullable.foreach(style.font)(f => l.setFont(f))
      Nullable.foreach(style.fontColor)(c => l.setColor(c))
    }
  }

  def setStyle(style: Styles.TextButtonStyle, font: Font): Unit = {
    this._style = style
    super.setStyle(new Button.ButtonStyle())

    Nullable(label).foreach { l =>
      l.setFont(font)
      Nullable.foreach(style.fontColor)(c => l.setColor(c))
    }
  }

  def getStyle: Styles.TextButtonStyle = _style

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = boundary {
    if (isDisabled && _style.disabledFontColor.isDefined) break(_style.disabledFontColor)
    if (isPressed) {
      if (isChecked && _style.checkedDownFontColor.isDefined) break(_style.checkedDownFontColor)
      if (_style.downFontColor.isDefined) break(_style.downFontColor)
    }
    if (isOver) {
      if (isChecked) {
        if (_style.checkedOverFontColor.isDefined) break(_style.checkedOverFontColor)
      } else {
        if (_style.overFontColor.isDefined) break(_style.overFontColor)
      }
    }
    val focused = hasKeyboardFocus
    if (isChecked) {
      if (focused && _style.checkedFocusedFontColor.isDefined) break(_style.checkedFocusedFontColor)
      if (_style.checkedFontColor.isDefined) break(_style.checkedFontColor)
      if (isOver && _style.overFontColor.isDefined) break(_style.overFontColor)
    }
    if (focused && _style.focusedFontColor.isDefined) break(_style.focusedFontColor)
    _style.fontColor
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
    super.draw(batch, parentAlpha)
  }

  def setTextraLabel(newLabel: TextraLabel): Unit = {
    require(newLabel != null, "label cannot be null.")
    if (!(this.label eq newLabel)) {
      getTextraLabelCell.setActor(Nullable[Actor](newLabel))
      this.label = newLabel
    }
  }

  def getTextraLabel: TextraLabel = label

  /** Returns the Cell containing the label (the label was added to the Table backing this Button). */
  def getTextraLabelCell: Cell[TextraLabel] = getCell(label).get

  /** Returns the preferred width based on the label. */
  def getPrefWidth: Float = prefWidth

  /** Returns the preferred height based on the label. */
  def getPrefHeight: Float = prefHeight

  def getWidth: Float = width

  def getHeight: Float = height

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

  /** Upstream reads `isDisabled()` (inherited from Button); SGE renamed it to the `disabled` property, exposed here under the Java name the textra package (and TextraButton's own state-driven font
    * logic) is written against. `isChecked`/`isPressed`/`isOver`/`hasKeyboardFocus`/`setChecked`/`toggle` come from the inherited Button.
    */
  def isDisabled:                   Boolean = disabled
  def isDisabled_=(value: Boolean): Unit    = disabled = value

  override def toString: String =
    if (name.isDefined) name.get
    else {
      val className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      val cn        = if (dotIndex != -1) className.substring(dotIndex + 1) else className
      (if (cn.indexOf('$') != -1) "TextraButton " else "") + cn + ": " + label.toString
    }
}
