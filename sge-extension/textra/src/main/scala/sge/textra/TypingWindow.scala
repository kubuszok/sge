/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingWindow.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Skin → sge.scenes.scene2d.ui.Skin
 *   Convention: TypingWindow extends TextraWindow, overrides newLabel to produce TypingLabel
 *     (as TextraLabel return type to match parent signature).
 *   Idiom: Nullable[A] for nullable fields.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 88
 * Covenant-baseline-methods: TypingWindow,newLabel,this
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingWindow.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.graphics.Color
import sge.scenes.scene2d.ui.Skin
import sge.utils.Nullable

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. The title uses a {@link TypingLabel} and will by default draw gradually. <p> The
  * preferred size of a window is the preferred size of the title text and the children as laid out by the table. After adding children to the window, it can be convenient to call {@link #pack()} to
  * size the window to the size of the children.
  *
  * @author
  *   Nathan Sweet
  */
class TypingWindow(title: String, style: Styles.WindowStyle, replacementFont: Font, scaleTitleFont: Boolean) extends TextraWindow(title, style, replacementFont, scaleTitleFont) {

  def this(title: String, style: Styles.WindowStyle, replacementFont: Font) =
    this(title, style, replacementFont, false)

  def this(title: String, style: Styles.WindowStyle, scaleTitleFont: Boolean) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), scaleTitleFont)

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), false)

  def this(title: String, skin: Skin) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, scaleTitleFont: Boolean) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, styleName: String) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, styleName: String, scaleTitleFont: Boolean) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, replacementFont: Font) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, replacementFont: Font, scaleTitleFont: Boolean) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font, scaleTitleFont: Boolean) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  override protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TypingLabel(text, style)

  override protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TypingLabel(text, font, color)
}
