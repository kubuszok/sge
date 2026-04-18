/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Skin → removed (standalone, no scene2d dependency)
 *   Convention: TypingButton extends TextraButton, overrides newLabel to produce TypingLabel.
 *   Idiom: Nullable[A] for nullable fields.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A button with a child {@link TypingLabel} to display text.
  */
class TypingButton(text: Nullable[String], style: Styles.TextButtonStyle, replacementFont: Font) extends TextraButton(text, style, replacementFont) {

  def this(text: Nullable[String], style: Styles.TextButtonStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  override protected def newLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  override protected def newLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)
}
