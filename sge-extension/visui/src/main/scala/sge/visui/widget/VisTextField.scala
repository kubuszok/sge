/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: mzechner, Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextField
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Nullable
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** Extends functionality of standard [[TextField]]. Style supports focus border and error border. Adds input validation support.
  *
  * Note: The original VisUI VisTextField was a complete reimplementation of TextField. This port extends SGE's existing TextField and adds the VisUI-specific features (focus border, error border,
  * input validity state).
  * @author
  *   mzechner, Nathan Sweet, Kotcrab
  * @see
  *   [[TextField]]
  */
class VisTextField(text: Nullable[String], visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends TextField(text, visStyle) with Focusable with BorderOwner {
  import VisTextField._

  private val _visStyle:               VisTextFieldStyle = visStyle
  private var drawBorder:              Boolean           = false
  private var _focusBorderEnabled:     Boolean           = true
  private var _inputValid:             Boolean           = true
  private var _readOnly:               Boolean           = false
  private var _ignoreEqualsTextChange: Boolean           = true
  private var _cursorPercentHeight:    Float             = 0.8f

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisTextField.this)
        false
      }
    }
  )

  def this()(using Sge) = this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    if (!disabled && !_inputValid && _visStyle.errorBorder.isDefined) {
      _visStyle.errorBorder.get.draw(batch, x, y, width, height)
    } else if (_focusBorderEnabled && drawBorder && _visStyle.focusBorder.isDefined) {
      _visStyle.focusBorder.get.draw(batch, x, y, width, height)
    }
  }

  def isInputValid:                  Boolean = _inputValid
  def setInputValid(valid: Boolean): Unit    = _inputValid = valid

  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  override def focusLost():   Unit = drawBorder = false
  override def focusGained(): Unit = drawBorder = true

  def isReadOnly:                     Boolean = _readOnly
  def setReadOnly(readOnly: Boolean): Unit    = _readOnly = readOnly

  def isIgnoreEqualsTextChange:                   Boolean = _ignoreEqualsTextChange
  def setIgnoreEqualsTextChange(ignore: Boolean): Unit    = _ignoreEqualsTextChange = ignore

  def cursorPercentHeight: Float = _cursorPercentHeight

  /** @param value
    *   cursor size, value from 0..1 range
    */
  def setCursorPercentHeight(value: Float): Unit = {
    if (value < 0 || value > 1) throw IllegalArgumentException("cursorPercentHeight must be >= 0 and <= 1")
    _cursorPercentHeight = value
  }

  /** Hook called right before ChangeEvent is fired. Subclasses (e.g. VisValidatableTextField) override this to trigger validation. */
  protected def beforeChangeEventFired(): Unit = ()

  override private[sge] def changeText(oldText: String, newText: String): Boolean =
    if (_ignoreEqualsTextChange && newText == oldText) false
    else {
      beforeChangeEventFired()
      super.changeText(oldText, newText)
    }
}

object VisTextField {
  class VisTextFieldStyle() extends TextField.TextFieldStyle() {
    var focusBorder: Nullable[Drawable] = Nullable.empty
    var errorBorder: Nullable[Drawable] = Nullable.empty

    def this(style: VisTextFieldStyle) = {
      this()
      this.font = style.font
      this.fontColor = style.fontColor
      this.focusedFontColor = style.focusedFontColor
      this.disabledFontColor = style.disabledFontColor
      this.background = style.background
      this.focusedBackground = style.focusedBackground
      this.disabledBackground = style.disabledBackground
      this.cursor = style.cursor
      this.selection = style.selection
      this.messageFont = style.messageFont
      this.messageFontColor = style.messageFontColor
      this.focusBorder = style.focusBorder
      this.errorBorder = style.errorBorder
    }
  }
}
