/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraTooltip.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Tooltip<TextraLabel> → standalone class (scene2d base deferred),
 *     Container → deferred, TooltipManager → deferred, Skin → removed
 *   Convention: Tooltip behavior preserved in API;
 *     actual scene2d integration deferred.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A tooltip that shows a TextraLabel. */
class TextraTooltip(
  text:            Nullable[String],
  style:           Styles.TextTooltipStyle,
  replacementFont: Font
) {

  private val _label: TextraLabel = newLabel(
    Nullable.fold(text)("")(identity),
    Nullable.fold(style.label)(new Styles.LabelStyle())(identity),
    replacementFont
  )

  def this(text: Nullable[String], style: Styles.TextTooltipStyle) =
    this(text, style, Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity)))

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, style: Styles.LabelStyle, font: Font): TextraLabel =
    new TextraLabel(text, style, font)

  protected def newLabel(text: String, font: Font): TextraLabel =
    new TextraLabel(text, font)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  def getActor: TextraLabel = _label

  def setStyle(style: Styles.TextTooltipStyle): Unit = {
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.font)(f => _label.setFont(f, false))
      Nullable.foreach(ls.fontColor)(c => _label.setColor(c))
    }
    _label.getFont.regenerateLayout(_label.layout)
    _label.setSize(_label.layout.getWidth, _label.layout.getHeight)
  }

  def setStyle(style: Styles.TextTooltipStyle, font: Font): Unit = {
    _label.setFont(font, false)
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.fontColor)(c => _label.setColor(c))
    }
    font.regenerateLayout(_label.layout)
    _label.setSize(_label.layout.getWidth, _label.layout.getHeight)
  }

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    _label.skipToTheEnd()
}
