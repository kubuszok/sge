/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 158
 * Covenant-baseline-methods: VisCheckBox,VisCheckBoxStyle,_focusBorderEnabled,_imageStackCell,_style,backgroundImage,bgImage,checkBackground,checkBackgroundDown,checkBackgroundOver,draw,drawBorder,errorBorder,focusBorder,focusBorderEnabled,focusBorderEnabled_,focusGained,focusLost,getCheckboxBgImage,getCheckboxTickImage,getImageStack,imageStack,imageStackCell,isStateInvalid,lbl,setStateInvalid,setStyle,stateInvalid,this,tick,tickDisabled,tickImage,tickImg,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisCheckBox.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.{ Button, Cell, Image, Label, Stack, TextButton }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.BorderOwner

/** A checkbox is a button that contains an image indicating the checked or unchecked state and a label. This widget is different than scene2d.ui's CheckBox. Style supports more checkbox states, focus
  * and error border. Due to scope of changes made this widget is not compatible with standard CheckBox.
  *
  * When listening for checkbox press [[sge.scenes.scene2d.utils.ChangeListener]] should be always preferred (instead of [[sge.scenes.scene2d.utils.ClickListener]]).
  * @author
  *   Nathan Sweet, Kotcrab
  */
class VisCheckBox(text: String, checkStyle: VisCheckBox.VisCheckBoxStyle)(using Sge) extends TextButton(Nullable(text), checkStyle) with Focusable with BorderOwner {
  import VisCheckBox._

  private var _style:              VisCheckBoxStyle = checkStyle
  private val bgImage:             Image            = new Image(checkStyle.checkBackground)
  private val tickImage:           Image            = new Image(checkStyle.tick)
  private val imageStack:          Stack            = new Stack()
  private var _imageStackCell:     Cell[Stack]      = scala.compiletime.uninitialized
  private var drawBorder:          Boolean          = false
  private var stateInvalid:        Boolean          = false
  private var _focusBorderEnabled: Boolean          = true

  clearChildren()
  imageStack.add(bgImage)
  imageStack.add(tickImage)
  _imageStackCell = add(Nullable[sge.scenes.scene2d.Actor](imageStack)).asInstanceOf[Cell[Stack]]
  val lbl: Label = label
  add(Nullable[sge.scenes.scene2d.Actor](lbl)).padLeft(5)
  lbl.setAlignment(Align.left)
  setSize(prefWidth, prefHeight)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!disabled) FocusManager.switchFocus(stage, VisCheckBox.this)
        false
      }
    }
  )

  def this(text: String)(using Sge) = this(text, VisUI.getSkin.get[VisCheckBox.VisCheckBoxStyle])

  def this(text: String, checked: Boolean)(using Sge) = {
    this(text, VisUI.getSkin.get[VisCheckBox.VisCheckBoxStyle])
    setChecked(checked)
  }

  def this(text: String, styleName: String)(using Sge) = this(text, VisUI.getSkin.get[VisCheckBox.VisCheckBoxStyle](styleName))

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[VisCheckBoxStyle])
      throw new IllegalArgumentException("style must be a VisCheckBoxStyle.")
    super.setStyle(style)
    this._style = style.asInstanceOf[VisCheckBoxStyle]
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    bgImage.drawable = getCheckboxBgImage
    tickImage.drawable = getCheckboxTickImage
    super.draw(batch, parentAlpha)

    if (!disabled && stateInvalid && _style.errorBorder.isDefined) {
      _style.errorBorder.get.draw(batch, x + imageStack.x, y + imageStack.y, imageStack.width, imageStack.height)
    } else if (_focusBorderEnabled && drawBorder && _style.focusBorder.isDefined) {
      _style.focusBorder.get.draw(batch, x + imageStack.x, y + imageStack.y, imageStack.width, imageStack.height)
    }
  }

  protected def getCheckboxBgImage: Nullable[Drawable] =
    if (disabled) _style.checkBackground
    else if (isPressed) _style.checkBackgroundDown.orElse(_style.checkBackground)
    else if (isOver) _style.checkBackgroundOver.orElse(_style.checkBackground)
    else _style.checkBackground

  protected def getCheckboxTickImage: Nullable[Drawable] =
    if (checked) {
      if (disabled) _style.tickDisabled.orElse(_style.tick)
      else _style.tick
    } else Nullable.empty

  def backgroundImage: Image       = bgImage
  def tickImg:         Image       = tickImage
  def getImageStack:   Stack       = imageStack
  def imageStackCell:  Cell[Stack] = _imageStackCell

  /** @param invalid if true error border around this checkbox will be drawn. Does not affect any other properties. */
  def setStateInvalid(invalid: Boolean): Unit    = stateInvalid = invalid
  def isStateInvalid:                    Boolean = stateInvalid

  override def focusLost():   Unit = drawBorder = false
  override def focusGained(): Unit = drawBorder = true

  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled
}

object VisCheckBox {
  class VisCheckBoxStyle() extends TextButton.TextButtonStyle() {
    var focusBorder:         Nullable[Drawable] = Nullable.empty
    var errorBorder:         Nullable[Drawable] = Nullable.empty
    var checkBackground:     Nullable[Drawable] = Nullable.empty
    var checkBackgroundOver: Nullable[Drawable] = Nullable.empty
    var checkBackgroundDown: Nullable[Drawable] = Nullable.empty
    var tick:                Nullable[Drawable] = Nullable.empty
    var tickDisabled:        Nullable[Drawable] = Nullable.empty

    def this(checkBackground: Drawable, tick: Drawable, font: BitmapFont, fontColor: Color) = {
      this()
      this.checkBackground = Nullable(checkBackground)
      this.tick = Nullable(tick)
      this.font = font
      this.fontColor = Nullable(fontColor)
    }

    def this(style: VisCheckBoxStyle) = {
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
      this.errorBorder = style.errorBorder
      this.checkBackground = style.checkBackground
      this.checkBackgroundOver = style.checkBackgroundOver
      this.checkBackgroundDown = style.checkBackgroundDown
      this.tick = style.tick
      this.tickDisabled = style.tickDisabled
    }
  }
}
