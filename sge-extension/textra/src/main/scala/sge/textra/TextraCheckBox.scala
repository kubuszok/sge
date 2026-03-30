/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraCheckBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button/TextraButton → standalone class, Image/Cell → deferred,
 *     Skin → removed, Scaling → deferred
 *   Convention: Scene2d checkbox behavior deferred until scene2d wiring.
 */
package sge
package textra

import sge.utils.Nullable

/** A checkbox is a button that contains an image indicating the checked or unchecked state and a TextraLabel. */
class TextraCheckBox(
    text: Nullable[String],
    style: Styles.CheckBoxStyle,
    replacementFont: Font
) extends TextraButton(text, style, replacementFont) {

  private var _checkStyle: Styles.CheckBoxStyle = style

  def this(text: Nullable[String], style: Styles.CheckBoxStyle) = {
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))
  }

  def setCheckStyle(style: Styles.CheckBoxStyle): Unit = {
    this._checkStyle = style
    setStyle(style)
  }

  def getCheckStyle: Styles.CheckBoxStyle = _checkStyle
}
