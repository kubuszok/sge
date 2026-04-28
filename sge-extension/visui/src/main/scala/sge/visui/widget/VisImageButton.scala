/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 176
 * Covenant-baseline-methods: VisImageButton,VisImageButtonStyle,_focusBorderEnabled,_image,_style,disabled_,draw,drawBorder,drawable,focusBorder,focusBorderEnabled,focusBorderEnabled_,focusGained,focusLost,generateDisabledImage,image,imageCell,imageChecked,imageCheckedOver,imageDisabled,imageDown,imageOver,imageUp,isGenerateDisabledImage,setGenerateDisabledImage,setStyle,this,touchDown,updateImage
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisImageButton.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.{ Button, Cell, Image }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Nullable, Scaling }
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** Due to scope of changes made this widget is not compatible with standard ImageButton.
  *
  * When listening for button press [[sge.scenes.scene2d.utils.ChangeListener]] should be always preferred (instead of [[sge.scenes.scene2d.utils.ClickListener]]).
  * @author
  *   Kotcrab
  */
class VisImageButton(buttonStyle: VisImageButton.VisImageButtonStyle)(using Sge) extends Button(buttonStyle) with Focusable with BorderOwner {
  import VisImageButton._

  private val _image:                Image               = new Image()
  private var _style:                VisImageButtonStyle = buttonStyle
  private var drawBorder:            Boolean             = false
  private var _focusBorderEnabled:   Boolean             = true
  private var generateDisabledImage: Boolean             = false

  _image.scaling = Scaling.fit
  add(Nullable[sge.scenes.scene2d.Actor](_image))
  setSize(prefWidth, prefHeight)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisImageButton.this)
        false
      }
    }
  )

  updateImage()

  def this(imageUp: Drawable)(using Sge) =
    this(
      {
        val s = new VisImageButton.VisImageButtonStyle(VisUI.getSkin.get[VisImageButton.VisImageButtonStyle])
        s.imageUp = Nullable(imageUp)
        s
      }
    )

  def this(imageUp: Drawable, imageDown: Nullable[Drawable], imageChecked: Nullable[Drawable])(using Sge) =
    this(
      {
        val s = new VisImageButton.VisImageButtonStyle(VisUI.getSkin.get[VisImageButton.VisImageButtonStyle])
        s.imageUp = Nullable(imageUp)
        s.imageDown = imageDown
        s.imageChecked = imageChecked
        s
      }
    )

  def this(styleName: String)(using Sge) = this(new VisImageButton.VisImageButtonStyle(VisUI.getSkin.get[VisImageButton.VisImageButtonStyle](styleName)))

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[VisImageButtonStyle])
      throw new IllegalArgumentException("style must be a VisImageButtonStyle.")
    super.setStyle(style)
    this._style = style.asInstanceOf[VisImageButtonStyle]
    updateImage()
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

  override def disabled_=(value: Boolean): Unit = {
    super.disabled_=(value)
    if (value) FocusManager.resetFocus(stage, this)
  }

  override def focusLost():                            Unit    = drawBorder = false
  override def focusGained():                          Unit    = drawBorder = true
  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  def isGenerateDisabledImage: Boolean = generateDisabledImage

  /** @param generate
    *   when set to true and button state is set to disabled then button image will be tinted with gray color to better symbolize that button is disabled. This works best for white images.
    */
  def setGenerateDisabledImage(generate: Boolean): Unit = generateDisabledImage = generate
}

object VisImageButton {
  class VisImageButtonStyle() extends Button.ButtonStyle() {

    /** Optional. */
    var imageUp:          Nullable[Drawable] = Nullable.empty
    var imageDown:        Nullable[Drawable] = Nullable.empty
    var imageOver:        Nullable[Drawable] = Nullable.empty
    var imageChecked:     Nullable[Drawable] = Nullable.empty
    var imageCheckedOver: Nullable[Drawable] = Nullable.empty
    var imageDisabled:    Nullable[Drawable] = Nullable.empty
    var focusBorder:      Nullable[Drawable] = Nullable.empty

    def this(up: Drawable, down: Drawable, checked: Drawable, imageUp: Drawable, imageDown: Drawable, imageChecked: Drawable) = {
      this()
      this.up = Nullable(up)
      this.down = Nullable(down)
      this.checked = Nullable(checked)
      this.imageUp = Nullable(imageUp)
      this.imageDown = Nullable(imageDown)
      this.imageChecked = Nullable(imageChecked)
    }

    def this(style: VisImageButtonStyle) = {
      this()
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
      this.imageUp = style.imageUp
      this.imageDown = style.imageDown
      this.imageOver = style.imageOver
      this.imageChecked = style.imageChecked
      this.imageCheckedOver = style.imageCheckedOver
      this.imageDisabled = style.imageDisabled
      this.focusBorder = style.focusBorder
    }
  }
}
