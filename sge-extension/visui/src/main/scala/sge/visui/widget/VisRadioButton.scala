/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

/** Similar to [[VisCheckBox]] however uses round (instead of square) button drawable. Note that if you want to achieve 'select only one option' behaviour you need to use ButtonGroup.
  *
  * When listening for button press [[sge.scenes.scene2d.utils.ChangeListener]] should be always preferred (instead of [[sge.scenes.scene2d.utils.ClickListener]]).
  * @author
  *   Kotcrab
  * @see
  *   [[VisCheckBox]]
  */
class VisRadioButton(text: String, checkStyle: VisCheckBox.VisCheckBoxStyle)(using Sge) extends VisCheckBox(text, checkStyle) {
  def this(text: String)(using Sge) = this(text, VisUI.getSkin.get[VisCheckBox.VisCheckBoxStyle]("radio"))
}
