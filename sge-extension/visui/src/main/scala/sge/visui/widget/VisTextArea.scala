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

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextArea
import sge.utils.Nullable
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** A multiple-line text input field with VisUI focus border support.
  *
  * Note: The original VisUI VisTextArea extended VisTextField (a complete reimplementation of TextField). This port extends SGE's TextArea and adds the VisUI-specific features (focus border, error
  * border).
  * @author
  *   Kotcrab
  * @see
  *   [[TextArea]]
  */
class VisTextArea(text: Nullable[String], visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends TextArea(text, visStyle) with Focusable with BorderOwner {

  private var drawBorder:          Boolean = false
  private var _focusBorderEnabled: Boolean = true
  private var _inputValid:         Boolean = true

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisTextArea.this)
        false
      }
    }
  )

  def this()(using Sge) = this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))
  def this(text: String, style:     VisTextField.VisTextFieldStyle)(using Sge) = this(Nullable(text), style)

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    val visStyle = style.asInstanceOf[VisTextField.VisTextFieldStyle]
    if (!disabled && !_inputValid && visStyle.errorBorder.isDefined) {
      visStyle.errorBorder.get.draw(batch, x, y, width, height)
    } else if (_focusBorderEnabled && drawBorder && visStyle.focusBorder.isDefined) {
      visStyle.focusBorder.get.draw(batch, x, y, width, height)
    }
  }

  def isInputValid:                  Boolean = _inputValid
  def setInputValid(valid: Boolean): Unit    = _inputValid = valid

  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  override def focusLost():   Unit = drawBorder = false
  override def focusGained(): Unit = drawBorder = true
}
