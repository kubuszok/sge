/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/TextButton.java
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
import sge.utils.{ Align, DynamicArray, Nullable }

/** A button with a child {@link Label} to display text.
  * @author
  *   Nathan Sweet
  */
class TextButton(text: Nullable[String], style: TextButton.TextButtonStyle) extends Button() {
  import TextButton._

  private var _style: TextButtonStyle = scala.compiletime.uninitialized
  private var label:  Label           = scala.compiletime.uninitialized

  setStyle(style)
  label = newLabel(text, new Label.LabelStyle(style.font, style.fontColor))
  label.setAlignment(Align.center)
  add(Nullable[Actor](label)).grow()
  setSize(getPrefWidth, getPrefHeight)

  // def this(text: Nullable[String], skin: Skin) = {
  //   this(text, skin.get(classOf[TextButtonStyle]))
  //   setSkin(skin)
  // }

  // def this(text: Nullable[String], skin: Skin, styleName: String) = {
  //   this(text, skin.get(styleName, classOf[TextButtonStyle]))
  //   setSkin(skin)
  // }

  protected def newLabel(text: Nullable[String], style: Label.LabelStyle): Label =
    new Label(text.map(s => s: CharSequence), style)

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[TextButtonStyle]) throw new IllegalArgumentException("style must be a TextButtonStyle.")
    this._style = style.asInstanceOf[TextButtonStyle]
    super.setStyle(style)

    Nullable(label).foreach { l =>
      val textButtonStyle = style.asInstanceOf[TextButtonStyle]
      val labelStyle      = l.getStyle
      labelStyle.font = textButtonStyle.font
      labelStyle.fontColor = textButtonStyle.fontColor
      l.setStyle(labelStyle)
    }
  }

  override def getStyle: TextButtonStyle = _style

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = scala.util.boundary {
    if (isDisabled && _style.disabledFontColor.isDefined) scala.util.boundary.break(_style.disabledFontColor)
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
    label.getStyle.fontColor = getFontColor
    super.draw(batch, parentAlpha)
  }

  def setLabel(label: Label): Unit = {
    getLabelCell.foreach(_.setActor(Nullable(label)))
    this.label = label
  }

  def getLabel: Label = label

  def getLabelCell: Nullable[Cell[Label]] = getCell(label)

  def setText(text: Nullable[String]): Unit =
    label.setText(text.map(s => s: CharSequence))

  def getText: DynamicArray[Char] = label.getText

  override def toString: String =
    getName.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "TextButton " else "") + className + ": " + new String(label.getText.toArray)
    }
}

object TextButton {

  /** The style for a text button, see {@link TextButton}.
    * @author
    *   Nathan Sweet
    */
  class TextButtonStyle() extends Button.ButtonStyle() {
    var font:                    BitmapFont      = scala.compiletime.uninitialized
    var fontColor:               Nullable[Color] = Nullable.empty
    var downFontColor:           Nullable[Color] = Nullable.empty
    var overFontColor:           Nullable[Color] = Nullable.empty
    var focusedFontColor:        Nullable[Color] = Nullable.empty
    var disabledFontColor:       Nullable[Color] = Nullable.empty
    var checkedFontColor:        Nullable[Color] = Nullable.empty
    var checkedDownFontColor:    Nullable[Color] = Nullable.empty
    var checkedOverFontColor:    Nullable[Color] = Nullable.empty
    var checkedFocusedFontColor: Nullable[Color] = Nullable.empty

    def this(up: Nullable[Drawable], down: Nullable[Drawable], checked: Nullable[Drawable], font: Nullable[BitmapFont]) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
      font.foreach(f => this.font = f)
    }

    def this(style: TextButtonStyle) = {
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
    }
  }
}
