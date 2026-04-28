/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingSelectBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Skin -> sge.scenes.scene2d.ui.Skin
 *   Convention: TypingSelectBox extends TextraSelectBox, overrides newLabel to produce
 *     TypingLabel with bottom alignment enforced.
 *   Idiom: Nullable[A] for nullable fields.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: TypingSelectBox,label,newLabel,this
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingSelectBox.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.scenes.scene2d.ui.Skin
import sge.utils.Align

/** A select box (aka a drop-down list) allows a user to choose one of a number of values from a list. When inactive, the selected value is displayed. When activated, it shows the list of values that
  * may be selected. <p> ChangeEvent is fired when the select box selection changes. <p> The preferred size of the select box is determined by the maximum text bounds of the items and the size of the
  * {@link Styles.SelectBoxStyle#background}.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class TypingSelectBox(style: Styles.SelectBoxStyle) extends TextraSelectBox(style) {

  def this(skin: Skin) = this(skin.get(classOf[Styles.SelectBoxStyle]))

  def this(skin: Skin, styleName: String) = this(skin.get(styleName, classOf[Styles.SelectBoxStyle]))

  override protected def newLabel(markupText: String, font: Font, color: Color): TextraLabel = {
    val label = new TypingLabel(markupText, font, color)
    // Enforces bottom alignment, and also disables top alignment to prevent top or center from being used.
    label.align = (label.align | Align.bottom) & ~Align.top
    label
  }
}
