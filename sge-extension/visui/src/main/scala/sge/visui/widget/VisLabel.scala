/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: VisLabel,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisLabel.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.graphics.Color
import sge.scenes.scene2d.ui.Label
import sge.utils.{ Align, Nullable }

/** Compatible with [[Label]]. Does not provide additional features.
  * @author
  *   Kotcrab
  * @see
  *   [[Label]]
  */
class VisLabel(text: Nullable[CharSequence], labelStyle: Label.LabelStyle)(using Sge) extends Label(text, labelStyle) {

  def this()(using Sge) = this(Nullable("": CharSequence), VisUI.getSkin.get[Label.LabelStyle])

  def this(text: CharSequence, textColor: Color)(using Sge) = {
    this(Nullable(text), VisUI.getSkin.get[Label.LabelStyle])
    color.set(textColor)
  }

  def this(text: CharSequence, alignment: Align)(using Sge) = {
    this(Nullable(text), VisUI.getSkin.get[Label.LabelStyle])
    setAlignment(alignment)
  }

  def this(text: CharSequence)(using Sge) = this(Nullable(text), VisUI.getSkin.get[Label.LabelStyle])

  def this(text: CharSequence, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[Label.LabelStyle](styleName))

  def this(text: CharSequence, fontName: String, color: Color)(using Sge) =
    this(Nullable(text), new Label.LabelStyle(VisUI.getSkin.getFont(fontName), Nullable(color)))

  def this(text: CharSequence, fontName: String, colorName: String)(using Sge) =
    this(Nullable(text), new Label.LabelStyle(VisUI.getSkin.getFont(fontName), Nullable(VisUI.getSkin.getColor(colorName))))
}
