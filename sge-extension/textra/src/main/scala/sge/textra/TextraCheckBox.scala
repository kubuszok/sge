/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraCheckBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Button/TextraButton → standalone class,
 *     Image → Nullable[AnyRef] drawable (no scene2d Image in standalone mode),
 *     Cell → Nullable[AnyRef] (no Table backing), Skin → removed
 *   Convention: Scene2d checkbox image stored as Nullable[AnyRef] (Drawable at scene2d level).
 *   Idiom: Nullable[A] for nullable fields; checkbox drawable selection in draw().
 */
package sge
package textra

import sge.graphics.g2d.Batch
import sge.utils.{ Align, Nullable }

/** A checkbox is a button that contains an image indicating the checked or unchecked state and a {@link TextraLabel}.
  *
  * @author
  *   Nathan Sweet
  */
class TextraCheckBox(
  text:            Nullable[String],
  style:           Styles.CheckBoxStyle,
  replacementFont: Font
) extends TextraButton(text, style, replacementFont) {

  private var _checkStyle: Styles.CheckBoxStyle = style

  // The image drawable representing the checkbox check state (standalone equivalent of scene2d Image)
  private var _imageDrawable: Nullable[AnyRef] = newImage()

  // Image cell reference (standalone mode: no Table backing)
  private val _imageCell: Nullable[AnyRef] = Nullable.empty

  {
    val lbl = getTextraLabel
    lbl.setAlignment(Align.left)

    _imageDrawable = style.checkboxOff

    // Standalone layout: no Table backing, so we just track the drawable and label references.
    // In the original, clearChildren() + add(image) + add(label) + pack() builds the layout.
  }

  def this(text: Nullable[String], style: Styles.CheckBoxStyle) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  /** Creates the initial image drawable for the checkbox. The original creates a scene2d Image(null, Scaling.none); in standalone mode, this returns Nullable.empty as the initial drawable state.
    */
  protected def newImage(): Nullable[AnyRef] = Nullable.empty

  override def setStyle(style: Styles.TextButtonStyle): Unit = {
    require(style.isInstanceOf[Styles.CheckBoxStyle], "style must be a CheckBoxStyle.")
    this._checkStyle = style.asInstanceOf[Styles.CheckBoxStyle]
    super.setStyle(style)
  }

  override def setStyle(style: Styles.TextButtonStyle, makeGridGlyphs: Boolean): Unit = {
    require(style.isInstanceOf[Styles.CheckBoxStyle], "style must be a CheckBoxStyle.")
    this._checkStyle = style.asInstanceOf[Styles.CheckBoxStyle]
    super.setStyle(style, makeGridGlyphs)
  }

  override def setStyle(style: Styles.TextButtonStyle, font: Font): Unit = {
    require(style.isInstanceOf[Styles.CheckBoxStyle], "style must be a CheckBoxStyle.")
    this._checkStyle = style.asInstanceOf[Styles.CheckBoxStyle]
    super.setStyle(style, font)
  }

  /** Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is called.
    */
  override def getStyle: Styles.CheckBoxStyle = _checkStyle

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    var checkbox: Nullable[AnyRef] = Nullable.empty
    if (isDisabled) {
      if (isChecked && _checkStyle.checkboxOnDisabled.isDefined) {
        checkbox = _checkStyle.checkboxOnDisabled
      } else {
        checkbox = _checkStyle.checkboxOffDisabled
      }
    }
    if (checkbox.isEmpty) {
      val over = isOver && !isDisabled
      if (isChecked && _checkStyle.checkboxOn.isDefined) {
        checkbox = if (over && _checkStyle.checkboxOnOver.isDefined) _checkStyle.checkboxOnOver else _checkStyle.checkboxOn
      } else if (over && _checkStyle.checkboxOver.isDefined) {
        checkbox = _checkStyle.checkboxOver
      } else {
        checkbox = _checkStyle.checkboxOff
      }
    }
    _imageDrawable = checkbox
    super.draw(batch, parentAlpha)
  }

  /** Returns the current image drawable being rendered (standalone equivalent of getImage().getDrawable()). */
  def getImage: Nullable[AnyRef] = _imageDrawable

  /** Returns the image cell reference (standalone mode: no Table backing, returns Nullable.empty). */
  def getImageCell: Nullable[AnyRef] = _imageCell
}
