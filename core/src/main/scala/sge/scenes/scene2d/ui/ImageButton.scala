/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ImageButton.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Nullable, Scaling }

/** A button with a child {@link Image} to display an image. This is useful when the button must be larger than the image and the image centered on the button. If the image is the size of the button,
  * a {@link Button} without any children can be used, where the {@link Button.ButtonStyle#up}, {@link Button.ButtonStyle#down}, and {@link Button.ButtonStyle#checked} nine patches define the image.
  * @author
  *   Nathan Sweet
  */
class ImageButton(style: ImageButton.ImageButtonStyle)(using Sge) extends Button() {
  import ImageButton._

  private var _style: ImageButtonStyle = scala.compiletime.uninitialized
  private val image:  Image            = newImage()

  add(Nullable(image))
  setStyle(style)
  setSize(getPrefWidth, getPrefHeight)

  def this(skin: Skin)(using Sge) = {
    this(skin.get(classOf[ImageButton.ImageButtonStyle]))
    setSkin(Nullable(skin))
  }

  def this(skin: Skin, styleName: String)(using Sge) = {
    this(skin.get(styleName, classOf[ImageButton.ImageButtonStyle]))
    setSkin(Nullable(skin))
  }

  def this(imageUp: Nullable[Drawable])(using Sge) =
    this(ImageButton.ImageButtonStyle(Nullable.empty, Nullable.empty, Nullable.empty, imageUp, Nullable.empty, Nullable.empty))

  def this(imageUp: Nullable[Drawable], imageDown: Nullable[Drawable])(using Sge) =
    this(ImageButton.ImageButtonStyle(Nullable.empty, Nullable.empty, Nullable.empty, imageUp, imageDown, Nullable.empty))

  def this(imageUp: Nullable[Drawable], imageDown: Nullable[Drawable], imageChecked: Nullable[Drawable])(using Sge) =
    this(ImageButton.ImageButtonStyle(Nullable.empty, Nullable.empty, Nullable.empty, imageUp, imageDown, imageChecked))

  protected def newImage(): Image =
    Image(Nullable.empty, Scaling.fit)

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[ImageButtonStyle]) throw new IllegalArgumentException("style must be an ImageButtonStyle.")
    this._style = style.asInstanceOf[ImageButtonStyle]
    super.setStyle(style)

    Nullable(image).foreach(_ => updateImage())
  }

  override def getStyle: ImageButtonStyle = _style

  /** Returns the appropriate image drawable from the style based on the current button state. */
  protected def getImageDrawable: Nullable[Drawable] = scala.util.boundary {
    if (isDisabled && _style.imageDisabled.isDefined) scala.util.boundary.break(_style.imageDisabled)
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
    image.drawable = getImageDrawable

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    updateImage()
    super.draw(batch, parentAlpha)
  }

  def getImage: Image = image

  def getImageCell: Nullable[Cell[Image]] = getCell(image)

  override def toString: String =
    name.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "ImageButton " else "") + className + ": " + image.drawable
    }
}

object ImageButton {

  /** The style for an image button, see {@link ImageButton}.
    * @author
    *   Nathan Sweet
    */
  class ImageButtonStyle() extends Button.ButtonStyle() {
    var imageUp:          Nullable[Drawable] = Nullable.empty
    var imageDown:        Nullable[Drawable] = Nullable.empty
    var imageOver:        Nullable[Drawable] = Nullable.empty
    var imageDisabled:    Nullable[Drawable] = Nullable.empty
    var imageChecked:     Nullable[Drawable] = Nullable.empty
    var imageCheckedDown: Nullable[Drawable] = Nullable.empty
    var imageCheckedOver: Nullable[Drawable] = Nullable.empty

    def this(
      up:           Nullable[Drawable],
      down:         Nullable[Drawable],
      checked:      Nullable[Drawable],
      imageUp:      Nullable[Drawable],
      imageDown:    Nullable[Drawable],
      imageChecked: Nullable[Drawable]
    ) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
      this.imageUp = imageUp
      this.imageDown = imageDown
      this.imageChecked = imageChecked
    }

    def this(style: ImageButtonStyle) = {
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

      imageUp = style.imageUp
      imageDown = style.imageDown
      imageOver = style.imageOver
      imageDisabled = style.imageDisabled
      imageChecked = style.imageChecked
      imageCheckedDown = style.imageCheckedDown
      imageCheckedOver = style.imageCheckedOver
    }

    def this(style: Button.ButtonStyle) = {
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
    }
  }
}
