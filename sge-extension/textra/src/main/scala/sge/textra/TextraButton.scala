/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button → standalone class (scene2d base deferred),
 *     Cell → deferred, Skin → removed
 *   Convention: getX()/setX() → public var or def pairs.
 *   TODOs: Scene2d.ui Button integration deferred until scene2d wiring.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A button with a child TextraLabel to display text. */
class TextraButton(text: Nullable[String], style: Styles.TextButtonStyle, replacementFont: Font) {

  private var label: TextraLabel = newLabel(
    Nullable.fold(text)("")(identity),
    replacementFont,
    Color.WHITE
  )
  private var _style: Styles.TextButtonStyle = style

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

  def setStyle(style: Styles.TextButtonStyle, font: Font): Unit = {
    this._style = style
    label.setFont(font)
    Nullable.foreach(style.fontColor)(c => label.setColor(c))
  }

  def getStyle: Styles.TextButtonStyle = _style

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = _style.fontColor

  def setTextraLabel(label: TextraLabel): Unit = {
    require(label != null, "label cannot be null.")
    this.label = label
  }

  def getTextraLabel: TextraLabel = label

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

  override def toString: String = {
    val className = getClass.getName
    val dotIndex  = className.lastIndexOf('.')
    val cn        = if (dotIndex != -1) className.substring(dotIndex + 1) else className
    cn + ": " + label.toString
  }
}
