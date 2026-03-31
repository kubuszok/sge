/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/ImageTypingButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ImageTextraButton → extends ImageTextraButton
 *   Convention: Overrides newLabel to create TypingLabel instead of TextraLabel.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A button with a child Image and TypingLabel. */
class ImageTypingButton(
  text:            Nullable[String],
  style:           Styles.ImageTextButtonStyle,
  replacementFont: Font
) extends ImageTextraButton(text, style, replacementFont) {

  def this(text: Nullable[String], style: Styles.ImageTextButtonStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  override protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TypingLabel(text, style)

  override protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TypingLabel(text, font, color)
}
