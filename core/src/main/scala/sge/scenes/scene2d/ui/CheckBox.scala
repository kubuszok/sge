/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/CheckBox.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable, Scaling }

/** A checkbox is a button that contains an image indicating the checked or unchecked state and a label.
  * @author
  *   Nathan Sweet
  */
class CheckBox(text: Nullable[String], style: CheckBox.CheckBoxStyle) extends TextButton(text, style) {
  import CheckBox._

  private var _style:    CheckBoxStyle = scala.compiletime.uninitialized
  private val image:     Image         = newImage()
  private var imageCell: Cell[Image]   = scala.compiletime.uninitialized

  {
    val label = getLabel
    label.setAlignment(Align.left)

    image.setDrawable(style.checkboxOff)

    clearChildren()
    imageCell = add(Nullable(image))
    add(Nullable[Actor](label))
    setSize(getPrefWidth, getPrefHeight)
  }

  // def this(text: Nullable[String], skin: Skin) = this(text, skin.get(classOf[CheckBoxStyle]))

  // def this(text: Nullable[String], skin: Skin, styleName: String) = this(text, skin.get(styleName, classOf[CheckBoxStyle]))

  protected def newImage(): Image =
    new Image(Nullable.empty, Scaling.none)

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[CheckBoxStyle]) throw new IllegalArgumentException("style must be a CheckBoxStyle.")
    this._style = style.asInstanceOf[CheckBoxStyle]
    super.setStyle(style)
  }

  /** Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is called.
    */
  override def getStyle: CheckBoxStyle = _style

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    image.setDrawable(getImageDrawable)
    super.draw(batch, parentAlpha)
  }

  protected def getImageDrawable: Nullable[Drawable] = scala.util.boundary {
    if (isDisabled) {
      if (isChecked && _style.checkboxOnDisabled.isDefined) scala.util.boundary.break(_style.checkboxOnDisabled)
      scala.util.boundary.break(_style.checkboxOffDisabled)
    }
    val over = isOver && !isDisabled
    if (isChecked && _style.checkboxOn.isDefined) {
      scala.util.boundary.break(
        if (over && _style.checkboxOnOver.isDefined) _style.checkboxOnOver
        else _style.checkboxOn
      )
    }
    if (over && _style.checkboxOver.isDefined) scala.util.boundary.break(_style.checkboxOver)
    _style.checkboxOff
  }

  def getImage: Image = image

  def getImageCell: Cell[Image] = imageCell
}

object CheckBox {

  /** The style for a select box, see {@link CheckBox}.
    * @author
    *   Nathan Sweet
    */
  class CheckBoxStyle() extends TextButton.TextButtonStyle() {
    var checkboxOn:          Nullable[Drawable] = Nullable.empty
    var checkboxOff:         Nullable[Drawable] = Nullable.empty
    var checkboxOnOver:      Nullable[Drawable] = Nullable.empty
    var checkboxOver:        Nullable[Drawable] = Nullable.empty
    var checkboxOnDisabled:  Nullable[Drawable] = Nullable.empty
    var checkboxOffDisabled: Nullable[Drawable] = Nullable.empty

    def this(checkboxOff: Drawable, checkboxOn: Drawable, font: BitmapFont, fontColor: Nullable[Color]) = {
      this()
      this.checkboxOff = Nullable(checkboxOff)
      this.checkboxOn = Nullable(checkboxOn)
      this.font = font
      this.fontColor = fontColor
    }

    def this(style: CheckBoxStyle) = {
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
      fontColor = style.fontColor.map(c => new Color(c))
      downFontColor = style.downFontColor.map(c => new Color(c))
      overFontColor = style.overFontColor.map(c => new Color(c))
      focusedFontColor = style.focusedFontColor.map(c => new Color(c))
      disabledFontColor = style.disabledFontColor.map(c => new Color(c))
      checkedFontColor = style.checkedFontColor.map(c => new Color(c))
      checkedDownFontColor = style.checkedDownFontColor.map(c => new Color(c))
      checkedOverFontColor = style.checkedOverFontColor.map(c => new Color(c))
      checkedFocusedFontColor = style.checkedFocusedFontColor.map(c => new Color(c))

      checkboxOff = style.checkboxOff
      checkboxOn = style.checkboxOn
      checkboxOnOver = style.checkboxOnOver
      checkboxOver = style.checkboxOver
      checkboxOnDisabled = style.checkboxOnDisabled
      checkboxOffDisabled = style.checkboxOffDisabled
    }
  }
}
