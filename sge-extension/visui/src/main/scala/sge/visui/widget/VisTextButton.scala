/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 117
 * Covenant-baseline-methods: VisTextButton,VisTextButtonStyle,_focusBorderEnabled,_visStyle,draw,drawBorder,focusBorder,focusBorderEnabled,focusBorderEnabled_,focusGained,focusLost,this,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisTextButton.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextButton
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.Nullable
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** Extends functionality of standard [[TextButton]], supports focus border. Compatible with standard [[TextButton]].
  *
  * When listening for button press [[ChangeListener]] should be always preferred (instead of [[sge.scenes.scene2d.utils.ClickListener]]). [[sge.scenes.scene2d.utils.ClickListener]] does not support
  * disabling button and will still report button presses.
  * @author
  *   Kotcrab
  * @see
  *   [[TextButton]]
  */
class VisTextButton(text: String, buttonStyle: VisTextButton.VisTextButtonStyle)(using Sge) extends TextButton(Nullable(text), buttonStyle) with Focusable with BorderOwner {
  import VisTextButton._

  private val _visStyle:           VisTextButtonStyle = buttonStyle
  private var drawBorder:          Boolean            = false
  private var _focusBorderEnabled: Boolean            = true

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisTextButton.this)
        false
      }
    }
  )

  def this(text: String, styleName: String)(using Sge) =
    this(text, VisUI.getSkin.get[VisTextButton.VisTextButtonStyle](styleName))

  def this(text: String)(using Sge) =
    this(text, VisUI.getSkin.get[VisTextButton.VisTextButtonStyle])

  def this(text: String, listener: ChangeListener)(using Sge) = {
    this(text, VisUI.getSkin.get[VisTextButton.VisTextButtonStyle])
    addListener(listener)
  }

  def this(text: String, styleName: String, listener: ChangeListener)(using Sge) = {
    this(text, VisUI.getSkin.get[VisTextButton.VisTextButtonStyle](styleName))
    addListener(listener)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    if (_focusBorderEnabled && drawBorder && _visStyle.focusBorder.isDefined) {
      _visStyle.focusBorder.get.draw(batch, x, y, width, height)
    }
  }

  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  override def focusLost():   Unit = drawBorder = false
  override def focusGained(): Unit = drawBorder = true
}

object VisTextButton {
  class VisTextButtonStyle() extends TextButton.TextButtonStyle() {
    var focusBorder: Nullable[Drawable] = Nullable.empty

    def this(up: Drawable, down: Drawable, checked: Drawable, font: BitmapFont) = {
      this()
      this.up = Nullable(up)
      this.down = Nullable(down)
      this.checked = Nullable(checked)
      this.font = font
    }

    def this(style: VisTextButtonStyle) = {
      this()
      // Copy parent fields
      this.up = style.up
      this.down = style.down
      this.over = style.over
      this.focused = style.focused
      this.disabled = style.disabled
      this.checked = style.checked
      this.checkedOver = style.checkedOver
      this.checkedFocused = style.checkedFocused
      this.pressedOffsetX = style.pressedOffsetX
      this.pressedOffsetY = style.pressedOffsetY
      this.unpressedOffsetX = style.unpressedOffsetX
      this.unpressedOffsetY = style.unpressedOffsetY
      this.checkedOffsetX = style.checkedOffsetX
      this.checkedOffsetY = style.checkedOffsetY
      this.font = style.font
      this.fontColor = style.fontColor
      this.downFontColor = style.downFontColor
      this.overFontColor = style.overFontColor
      this.focusedFontColor = style.focusedFontColor
      this.disabledFontColor = style.disabledFontColor
      this.checkedFontColor = style.checkedFontColor
      this.checkedOverFontColor = style.checkedOverFontColor
      this.checkedFocusedFontColor = style.checkedFocusedFontColor
      this.checkedDownFontColor = style.checkedDownFontColor
      this.focusBorder = style.focusBorder
    }
  }
}
