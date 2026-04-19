/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/CheckBox.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 144
 * Covenant-baseline-methods: CheckBox,CheckBoxStyle,_image,_imageCell,_style,checkboxOff,checkboxOffDisabled,checkboxOn,checkboxOnDisabled,checkboxOnOver,checkboxOver,draw,image,imageCell,imageDrawable,lbl,newImage,over,setStyle,style,this
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/CheckBox.java
 * Covenant-verified: 2026-04-19
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
class CheckBox(text: Nullable[String], initialStyle: CheckBox.CheckBoxStyle)(using Sge) extends TextButton(text, initialStyle) {
  import CheckBox._

  private var _style:     CheckBoxStyle = scala.compiletime.uninitialized
  private val _image:     Image         = newImage()
  private var _imageCell: Cell[Image]   = scala.compiletime.uninitialized

  {
    val lbl = label
    lbl.setAlignment(Align.left)

    _image.drawable = initialStyle.checkboxOff

    clearChildren()
    _imageCell = add(Nullable(_image))
    add(Nullable[Actor](lbl))
    setSize(prefWidth, prefHeight)
  }

  def this(text: Nullable[String], skin: Skin)(using Sge) = this(text, skin.get[CheckBox.CheckBoxStyle])

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) = this(text, skin.get[CheckBox.CheckBoxStyle](styleName))

  protected def newImage(): Image =
    Image(Nullable.empty, Scaling.none)

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[CheckBoxStyle]) throw new IllegalArgumentException("style must be a CheckBoxStyle.")
    this._style = style.asInstanceOf[CheckBoxStyle]
    super.setStyle(style)
  }

  /** Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is called.
    */
  override def style: CheckBoxStyle = _style

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    _image.drawable = imageDrawable
    super.draw(batch, parentAlpha)
  }

  protected def imageDrawable: Nullable[Drawable] = scala.util.boundary {
    if (disabled) {
      if (isChecked && _style.checkboxOnDisabled.isDefined) scala.util.boundary.break(_style.checkboxOnDisabled)
      scala.util.boundary.break(_style.checkboxOffDisabled)
    }
    val over = isOver && !disabled
    if (isChecked && _style.checkboxOn.isDefined) {
      scala.util.boundary.break(
        if (over && _style.checkboxOnOver.isDefined) _style.checkboxOnOver
        else _style.checkboxOn
      )
    }
    if (over && _style.checkboxOver.isDefined) scala.util.boundary.break(_style.checkboxOver)
    _style.checkboxOff
  }

  def image: Image = _image

  def imageCell: Cell[Image] = _imageCell
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
      fontColor = style.fontColor.map(c => Color(c))
      downFontColor = style.downFontColor.map(c => Color(c))
      overFontColor = style.overFontColor.map(c => Color(c))
      focusedFontColor = style.focusedFontColor.map(c => Color(c))
      disabledFontColor = style.disabledFontColor.map(c => Color(c))
      checkedFontColor = style.checkedFontColor.map(c => Color(c))
      checkedDownFontColor = style.checkedDownFontColor.map(c => Color(c))
      checkedOverFontColor = style.checkedOverFontColor.map(c => Color(c))
      checkedFocusedFontColor = style.checkedFocusedFontColor.map(c => Color(c))

      checkboxOff = style.checkboxOff
      checkboxOn = style.checkboxOn
      checkboxOnOver = style.checkboxOnOver
      checkboxOver = style.checkboxOver
      checkboxOnDisabled = style.checkboxOnDisabled
      checkboxOffDisabled = style.checkboxOffDisabled
    }
  }
}
