/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/ImageTextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button → standalone class (scene2d base deferred),
 *     Image/Cell → deferred, Scaling → deferred
 *   Convention: Image text button behavior preserved in API;
 *     actual rendering deferred until scene2d wiring.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

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

  def this(text: Nullable[String], style: Styles.ImageTextButtonStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  def setStyle(style: Styles.ImageTextButtonStyle): Unit = {
    this._style = style
    Nullable.foreach(style.font)(f => label.setFont(new Font(f)))
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
  }

  def setStyle(style: Styles.ImageTextButtonStyle, font: Font): Unit = {
    this._style = style
    label.setFont(font)
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
  }

  def getStyle: Styles.ImageTextButtonStyle = _style

  /** Returns the appropriate image drawable from the style based on the current button state. */
  protected def getImageDrawable: Nullable[AnyRef] = _style.imageUp

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = _style.fontColor

  def setLabel(label: TextraLabel): Unit =
    this.label = label

  def getLabel: TextraLabel = label

  def setText(text: CharSequence): Unit =
    label.setText(text.toString)

  def getText: String = label.toString

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    label.skipToTheEnd()

  override def toString: String = {
    val name      = getClass.getName
    val dotIndex  = name.lastIndexOf('.')
    val className = if (dotIndex != -1) name.substring(dotIndex + 1) else name
    className + ": " + label.toString
  }
}
