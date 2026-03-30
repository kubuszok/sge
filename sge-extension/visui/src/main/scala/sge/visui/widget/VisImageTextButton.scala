/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.{ Button, Cell, Image, Label }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable, Scaling }
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** A button with a child [[Image]] and [[Label]].
  *
  * Due to scope of changes made this widget is not compatible with standard ImageTextButton.
  *
  * When listening for button press [[sge.scenes.scene2d.utils.ChangeListener]] should be always preferred.
  * @author
  *   Nathan Sweet, Kotcrab
  */
class VisImageTextButton(text: String, buttonStyle: VisImageTextButton.VisImageTextButtonStyle)(using Sge) extends Button(buttonStyle) with Focusable with BorderOwner {
  import VisImageTextButton._

  private val _image:                Image                   = new Image()
  private val _label:                Label                   = new Label(Nullable(text: CharSequence), new Label.LabelStyle(buttonStyle.font, buttonStyle.fontColor))
  private var _style:                VisImageTextButtonStyle = buttonStyle
  private var drawBorder:            Boolean                 = false
  private var _focusBorderEnabled:   Boolean                 = true
  private var generateDisabledImage: Boolean                 = false

  defaults().space(3)
  _image.scaling = Scaling.fit
  _label.setAlignment(Align.center)
  add(Nullable[sge.scenes.scene2d.Actor](_image))
  add(Nullable[sge.scenes.scene2d.Actor](_label))
  setStyle(buttonStyle)
  setSize(prefWidth, prefHeight)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisImageTextButton.this)
        false
      }
    }
  )

  def this(text: String, styleName: String)(using Sge) =
    this(
      text,
      new VisImageTextButton.VisImageTextButtonStyle(VisUI.getSkin.get[VisImageTextButton.VisImageTextButtonStyle](styleName))
    )

  def this(text: String, imageUp: Drawable)(using Sge) =
    this(
      text, {
        val s = new VisImageTextButton.VisImageTextButtonStyle(VisUI.getSkin.get[VisImageTextButton.VisImageTextButtonStyle])
        s.imageUp = Nullable(imageUp)
        s
      }
    )

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[VisImageTextButtonStyle])
      throw new IllegalArgumentException("style must be a VisImageTextButtonStyle.")
    super.setStyle(style)
    this._style = style.asInstanceOf[VisImageTextButtonStyle]
    if (_image != null) updateImage()
    if (_label != null) {
      val textButtonStyle = style.asInstanceOf[VisImageTextButtonStyle]
      val labelStyle      = _label.style
      labelStyle.font = textButtonStyle.font
      labelStyle.fontColor = textButtonStyle.fontColor
      _label.setStyle(labelStyle)
    }
  }

  private def updateImage(): Unit = {
    var drawable: Nullable[Drawable] = Nullable.empty
    if (disabled && _style.imageDisabled.isDefined)
      drawable = _style.imageDisabled
    else if (isPressed && _style.imageDown.isDefined)
      drawable = _style.imageDown
    else if (checked && _style.imageChecked.isDefined)
      drawable = if (_style.imageCheckedOver.isDefined && isOver) _style.imageCheckedOver else _style.imageChecked
    else if (isOver && _style.imageOver.isDefined)
      drawable = _style.imageOver
    else if (_style.imageUp.isDefined)
      drawable = _style.imageUp
    _image.drawable = drawable

    if (generateDisabledImage && _style.imageDisabled.isEmpty) {
      if (disabled) _image.color.set(Color.GRAY)
      else _image.color.set(Color.WHITE)
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    updateImage()
    super.draw(batch, parentAlpha)
    if (_focusBorderEnabled && drawBorder && _style.focusBorder.isDefined)
      _style.focusBorder.get.draw(batch, x, y, width, height)
  }

  def image:     Image                 = _image
  def imageCell: Nullable[Cell[Image]] = getCell(_image)
  def label:     Label                 = _label
  def labelCell: Nullable[Cell[Label]] = getCell(_label)

  def setText(text: CharSequence): Unit   = _label.setText(text)
  def getText:                     String = _label.text.toString

  override def toString: String = super.toString + ": " + _label.text.toString

  override def disabled_=(value: Boolean): Unit = {
    super.disabled_=(value)
    if (value) FocusManager.resetFocus(stage, this)
  }

  override def focusLost():                            Unit    = drawBorder = false
  override def focusGained():                          Unit    = drawBorder = true
  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  def isGenerateDisabledImage:                     Boolean = generateDisabledImage
  def setGenerateDisabledImage(generate: Boolean): Unit    = generateDisabledImage = generate
}

object VisImageTextButton {
  class VisImageTextButtonStyle() extends VisTextButton.VisTextButtonStyle() {

    /** Optional. */
    var imageUp:          Nullable[Drawable] = Nullable.empty
    var imageDown:        Nullable[Drawable] = Nullable.empty
    var imageOver:        Nullable[Drawable] = Nullable.empty
    var imageChecked:     Nullable[Drawable] = Nullable.empty
    var imageCheckedOver: Nullable[Drawable] = Nullable.empty
    var imageDisabled:    Nullable[Drawable] = Nullable.empty

    def this(up: Drawable, down: Drawable, checked: Drawable, font: BitmapFont) = {
      this()
      this.up = Nullable(up)
      this.down = Nullable(down)
      this.checked = Nullable(checked)
      this.font = font
    }

    def this(style: VisImageTextButtonStyle) = {
      this()
      this.up = style.up
      this.down = style.down
      this.over = style.over
      this.focused = style.focused
      this.disabled = style.disabled
      this.checked = style.checked
      this.checkedOver = style.checkedOver
      this.checkedFocused = style.checkedFocused
      this.font = style.font
      this.fontColor = style.fontColor
      this.downFontColor = style.downFontColor
      this.overFontColor = style.overFontColor
      this.focusedFontColor = style.focusedFontColor
      this.disabledFontColor = style.disabledFontColor
      this.checkedFontColor = style.checkedFontColor
      this.focusBorder = style.focusBorder
      this.imageUp = style.imageUp
      this.imageDown = style.imageDown
      this.imageOver = style.imageOver
      this.imageChecked = style.imageChecked
      this.imageCheckedOver = style.imageCheckedOver
      this.imageDisabled = style.imageDisabled
    }

    def this(style: VisTextButton.VisTextButtonStyle) = {
      this()
      this.up = style.up
      this.down = style.down
      this.over = style.over
      this.focused = style.focused
      this.disabled = style.disabled
      this.checked = style.checked
      this.checkedOver = style.checkedOver
      this.checkedFocused = style.checkedFocused
      this.font = style.font
      this.fontColor = style.fontColor
      this.downFontColor = style.downFontColor
      this.overFontColor = style.overFontColor
      this.focusedFontColor = style.focusedFontColor
      this.disabledFontColor = style.disabledFontColor
      this.checkedFontColor = style.checkedFontColor
      this.focusBorder = style.focusBorder
    }
  }
}
