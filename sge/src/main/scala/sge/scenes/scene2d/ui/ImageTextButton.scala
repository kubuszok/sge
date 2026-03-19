/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ImageTextButton.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, DynamicArray, Nullable, Scaling }

/** A button with a child {@link Image} and {@link Label}.
  * @see
  *   ImageButton
  * @see
  *   TextButton
  * @see
  *   Button
  * @author
  *   Nathan Sweet
  */
class ImageTextButton(initialText: Nullable[String], initialStyle: ImageTextButton.ImageTextButtonStyle)(using Sge) extends Button() {
  import ImageTextButton._

  private var _style: ImageTextButtonStyle = scala.compiletime.uninitialized
  private val _image: Image                = newImage()
  private var _label: Label                = scala.compiletime.uninitialized

  this._style = initialStyle

  defaults().space(3)

  _label = newLabel(initialText, new Label.LabelStyle(initialStyle.font, initialStyle.fontColor))
  _label.setAlignment(Align.center)

  add(Nullable(_image))
  add(Nullable[Actor](_label))

  setStyle(initialStyle)

  setSize(prefWidth, prefHeight)

  def this(text: Nullable[String], skin: Skin)(using Sge) = {
    this(text, skin.get[ImageTextButton.ImageTextButtonStyle])
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) = {
    this(text, skin.get[ImageTextButton.ImageTextButtonStyle](styleName))
    setSkin(Nullable(skin))
  }

  protected def newImage(): Image =
    Image(Nullable.empty, Scaling.fit)

  protected def newLabel(text: Nullable[String], style: Label.LabelStyle): Label =
    Label(text.map(s => s: CharSequence), style)

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[ImageTextButtonStyle])
      throw new IllegalArgumentException("style must be a ImageTextButtonStyle.")
    this._style = style.asInstanceOf[ImageTextButtonStyle]
    super.setStyle(style)

    Nullable(_image).foreach(_ => updateImage())

    Nullable(_label).foreach { l =>
      val textButtonStyle = style.asInstanceOf[ImageTextButtonStyle]
      val labelStyle      = l.style
      labelStyle.font = textButtonStyle.font
      labelStyle.fontColor = fontColor
      l.setStyle(labelStyle)
    }
  }

  override def style: ImageTextButtonStyle = _style

  /** Returns the appropriate image drawable from the style based on the current button state. */
  protected def imageDrawable: Nullable[Drawable] = scala.util.boundary {
    if (disabled && _style.imageDisabled.isDefined) scala.util.boundary.break(_style.imageDisabled)
    if (isPressed) {
      if (isChecked && _style.imageCheckedDown.isDefined) scala.util.boundary.break(_style.imageCheckedDown)
      if (_style.imageDown.isDefined) scala.util.boundary.break(_style.imageDown)
    }
    if (isOver) {
      if (isChecked) {
        if (_style.imageCheckedOver.isDefined) scala.util.boundary.break(_style.imageCheckedOver)
      } else {
        if (_style.imageOver.isDefined) scala.util.boundary.break(_style.imageOver)
      }
    }
    if (isChecked) {
      if (_style.imageChecked.isDefined) scala.util.boundary.break(_style.imageChecked)
      if (isOver && _style.imageOver.isDefined) scala.util.boundary.break(_style.imageOver)
    }
    _style.imageUp
  }

  /** Sets the image drawable based on the current button state. The default implementation sets the image drawable using {@link #getImageDrawable()}.
    */
  protected def updateImage(): Unit =
    _image.drawable = imageDrawable

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def fontColor: Nullable[Color] = scala.util.boundary {
    if (disabled && _style.disabledFontColor.isDefined) scala.util.boundary.break(_style.disabledFontColor)
    if (isPressed) {
      if (isChecked && _style.checkedDownFontColor.isDefined) scala.util.boundary.break(_style.checkedDownFontColor)
      if (_style.downFontColor.isDefined) scala.util.boundary.break(_style.downFontColor)
    }
    if (isOver) {
      if (isChecked) {
        if (_style.checkedOverFontColor.isDefined) scala.util.boundary.break(_style.checkedOverFontColor)
      } else {
        if (_style.overFontColor.isDefined) scala.util.boundary.break(_style.overFontColor)
      }
    }
    val focused = hasKeyboardFocus
    if (isChecked) {
      if (focused && _style.checkedFocusedFontColor.isDefined) scala.util.boundary.break(_style.checkedFocusedFontColor)
      if (_style.checkedFontColor.isDefined) scala.util.boundary.break(_style.checkedFontColor)
      if (isOver && _style.overFontColor.isDefined) scala.util.boundary.break(_style.overFontColor)
    }
    if (focused && _style.focusedFontColor.isDefined) scala.util.boundary.break(_style.focusedFontColor)
    _style.fontColor
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    updateImage()
    _label.style.fontColor = fontColor
    super.draw(batch, parentAlpha)
  }

  def image: Image = _image

  def imageCell: Nullable[Cell[Image]] = getCell(_image)

  def setLabel(label: Label): Unit = {
    labelCell.foreach(_.setActor(Nullable(label)))
    this._label = label
  }

  def label: Label = _label

  def labelCell: Nullable[Cell[Label]] = getCell(_label)

  def setText(text: Nullable[CharSequence]): Unit =
    _label.setText(text)

  def text: DynamicArray[Char] = _label.text

  override def toString: String =
    name.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "ImageTextButton " else "") + className + ": " + _image.drawable + " " + new String(
        _label.text.toArray
      )
    }
}

object ImageTextButton {

  /** The style for an image text button, see {@link ImageTextButton}.
    * @author
    *   Nathan Sweet
    */
  class ImageTextButtonStyle() extends TextButton.TextButtonStyle() {
    var imageUp:          Nullable[Drawable] = Nullable.empty
    var imageDown:        Nullable[Drawable] = Nullable.empty
    var imageOver:        Nullable[Drawable] = Nullable.empty
    var imageDisabled:    Nullable[Drawable] = Nullable.empty
    var imageChecked:     Nullable[Drawable] = Nullable.empty
    var imageCheckedDown: Nullable[Drawable] = Nullable.empty
    var imageCheckedOver: Nullable[Drawable] = Nullable.empty

    def this(up: Nullable[Drawable], down: Nullable[Drawable], checked: Nullable[Drawable], font: BitmapFont) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
      this.font = font
    }

    def this(style: ImageTextButtonStyle) = {
      this()
      up = style.up
      down = style.down
      over = style.over
      focused = style.focused
      disabled = style.disabled
      checked = style.checked
      checkedOver = style.checkedOver
      checkedDown = style.checkedDown
      checkedFocused = style.checkedFocused
      pressedOffsetX = style.pressedOffsetX
      pressedOffsetY = style.pressedOffsetY
      unpressedOffsetX = style.unpressedOffsetX
      unpressedOffsetY = style.unpressedOffsetY
      checkedOffsetX = style.checkedOffsetX
      checkedOffsetY = style.checkedOffsetY

      font = style.font
      fontColor = style.fontColor.map(c => Color(c))
      downFontColor = style.downFontColor.map(c => Color(c))
      overFontColor = style.overFontColor.map(c => Color(c))
      focusedFontColor = style.focusedFontColor.map(c => Color(c))
      disabledFontColor = style.disabledFontColor.map(c => Color(c))
      checkedFontColor = style.checkedFontColor.map(c => Color(c))
      checkedDownFontColor = style.checkedDownFontColor.map(c => Color(c))
      checkedOverFontColor = style.checkedOverFontColor.map(c => Color(c))
      checkedFocusedFontColor = style.checkedFocusedFontColor.map(c => Color(c))

      imageUp = style.imageUp
      imageDown = style.imageDown
      imageOver = style.imageOver
      imageDisabled = style.imageDisabled
      imageChecked = style.imageChecked
      imageCheckedDown = style.imageCheckedDown
      imageCheckedOver = style.imageCheckedOver
    }

    def this(style: TextButton.TextButtonStyle) = {
      this()
      up = style.up
      down = style.down
      over = style.over
      focused = style.focused
      disabled = style.disabled
      checked = style.checked
      checkedOver = style.checkedOver
      checkedDown = style.checkedDown
      checkedFocused = style.checkedFocused
      pressedOffsetX = style.pressedOffsetX
      pressedOffsetY = style.pressedOffsetY
      unpressedOffsetX = style.unpressedOffsetX
      unpressedOffsetY = style.unpressedOffsetY
      checkedOffsetX = style.checkedOffsetX
      checkedOffsetY = style.checkedOffsetY

      font = style.font
      fontColor = style.fontColor.map(c => Color(c))
      downFontColor = style.downFontColor.map(c => Color(c))
      overFontColor = style.overFontColor.map(c => Color(c))
      focusedFontColor = style.focusedFontColor.map(c => Color(c))
      disabledFontColor = style.disabledFontColor.map(c => Color(c))
      checkedFontColor = style.checkedFontColor.map(c => Color(c))
      checkedDownFontColor = style.checkedDownFontColor.map(c => Color(c))
      checkedOverFontColor = style.checkedOverFontColor.map(c => Color(c))
      checkedFocusedFontColor = style.checkedFocusedFontColor.map(c => Color(c))
    }
  }
}
