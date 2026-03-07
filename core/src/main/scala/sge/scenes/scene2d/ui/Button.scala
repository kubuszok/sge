/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Button.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: isChecked() getter -> getIsChecked (avoids collision with isChecked field)
 *   Convention: null -> Nullable
 *   Idiom: split packages
 *   Fixes: Removed redundant Java-style getters/setters (programmaticChangeEvents is public var; clickListener/buttonGroup accessed directly; isPressed/isOver/isDisabled/setDisabled/setChecked retained — have logic or implement trait)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.{ ChangeListener, ClickListener, Disableable, Drawable }
import sge.utils.Nullable

/** A button is a {@link Table} with a checked state and additional {@link ButtonStyle style} fields for pressed, unpressed, and checked. Each time a button is clicked, the checked state is toggled.
  * Being a table, a button can contain any other actors.<br> <br> The button's padding is set to the background drawable's padding when the background changes, overwriting any padding set manually.
  * Padding can still be set on the button's table cells. <p> {@link ChangeEvent} is fired when the button is clicked. Cancelling the event will restore the checked button state to what is was
  * previously. <p> The preferred size of the button is determined by the background and the button contents.
  * @author
  *   Nathan Sweet
  */
class Button()(using Sge) extends Table() with Disableable with Styleable[Button.ButtonStyle] {
  import Button._

  private var _style:            ButtonStyle              = scala.compiletime.uninitialized
  private[ui] var isChecked:     Boolean                  = false
  private[ui] var _isDisabled:   Boolean                  = false
  private[ui] var buttonGroup:   Nullable[ButtonGroup[?]] = Nullable.empty
  private[ui] var clickListener: ClickListener            = scala.compiletime.uninitialized
  var programmaticChangeEvents:  Boolean                  = true

  initialize()

  def this(style: Button.ButtonStyle)(using Sge) = {
    this()
    setStyle(style)
    setSize(getPrefWidth, getPrefHeight)
  }

  def this(skin: Skin)(using Sge) = {
    this()
    setSkin(Nullable(skin))
    setStyle(skin.get(classOf[Button.ButtonStyle]))
    setSize(getPrefWidth, getPrefHeight)
  }

  def this(skin: Skin, styleName: String)(using Sge) = {
    this()
    setSkin(Nullable(skin))
    setStyle(skin.get(styleName, classOf[Button.ButtonStyle]))
    setSize(getPrefWidth, getPrefHeight)
  }

  def this(child: Actor, style: Button.ButtonStyle)(using Sge) = {
    this()
    add(Nullable[Actor](child))
    setStyle(style)
    setSize(getPrefWidth, getPrefHeight)
  }

  def this(child: Actor, skin: Skin)(using Sge) = {
    this(child, skin.get(classOf[Button.ButtonStyle]))
    setSkin(Nullable(skin))
  }

  def this(child: Actor, skin: Skin, styleName: String)(using Sge) = {
    this(child, skin.get(styleName, classOf[Button.ButtonStyle]))
    setSkin(Nullable(skin))
  }

  private def initialize(): Unit = {
    touchable = Touchable.enabled
    clickListener = new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit =
        if (!isDisabled) setChecked(!isChecked, true)
    }
    addListener(clickListener)
  }

  def this(up: Nullable[Drawable])(using Sge) =
    this(new Button.ButtonStyle(up, Nullable.empty, Nullable.empty))

  def this(up: Nullable[Drawable], down: Nullable[Drawable])(using Sge) =
    this(new Button.ButtonStyle(up, down, Nullable.empty))

  def this(up: Nullable[Drawable], down: Nullable[Drawable], checked: Nullable[Drawable])(using Sge) =
    this(new Button.ButtonStyle(up, down, checked))

  def setChecked(isChecked: Boolean): Unit =
    setChecked(isChecked, programmaticChangeEvents)

  private[ui] def setChecked(isChecked: Boolean, fireEvent: Boolean): Unit =
    if (this.isChecked != isChecked && buttonGroup.forall(bg => bg.asInstanceOf[ButtonGroup[Button]].canCheck(this, isChecked))) {
      this.isChecked = isChecked

      if (fireEvent) {
        val changeEvent = Actor.POOLS.obtain(classOf[ChangeListener.ChangeEvent])
        if (fire(changeEvent)) this.isChecked = !isChecked
        Actor.POOLS.free(changeEvent)
      }
    }

  /** Toggles the checked state. This method changes the checked state, which fires a {@link ChangeEvent} (if programmatic change events are enabled), so can be used to simulate a button click.
    */
  def toggle(): Unit =
    setChecked(!isChecked)

  def getIsChecked: Boolean = isChecked

  def isPressed: Boolean = clickListener.isVisualPressed

  def isOver: Boolean = clickListener.over

  override def isDisabled: Boolean = _isDisabled

  /** When true, the button will not toggle {@link #isChecked()} when clicked and will not fire a {@link ChangeEvent}. */
  override def setDisabled(isDisabled: Boolean): Unit =
    this._isDisabled = isDisabled

  override def setStyle(style: ButtonStyle): Unit = {
    this._style = style

    setBackground(getBackgroundDrawable)
  }

  /** Returns the button's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is called.
    */
  override def getStyle: ButtonStyle = _style

  /** Returns appropriate background drawable from the style based on the current button state. */
  protected def getBackgroundDrawable: Nullable[Drawable] =
    if (isDisabled && _style.disabled.isDefined) _style.disabled
    else if (isPressed) {
      if (isChecked && _style.checkedDown.isDefined) _style.checkedDown
      else if (_style.down.isDefined) _style.down
      else if (isOver) {
        if (isChecked) {
          if (_style.checkedOver.isDefined) _style.checkedOver
          else _style.up
        } else {
          if (_style.over.isDefined) _style.over
          else _style.up
        }
      } else {
        val focused = hasKeyboardFocus
        if (isChecked) {
          if (focused && _style.checkedFocused.isDefined) _style.checkedFocused
          else if (_style.checked.isDefined) _style.checked
          else if (isOver && _style.over.isDefined) _style.over
          else _style.up
        } else {
          if (focused && _style.focused.isDefined) _style.focused
          else _style.up
        }
      }
    } else if (isOver) {
      if (isChecked) {
        if (_style.checkedOver.isDefined) _style.checkedOver
        else _style.up
      } else {
        if (_style.over.isDefined) _style.over
        else _style.up
      }
    } else {
      val focused = hasKeyboardFocus
      if (isChecked) {
        if (focused && _style.checkedFocused.isDefined) _style.checkedFocused
        else if (_style.checked.isDefined) _style.checked
        else if (isOver && _style.over.isDefined) _style.over
        else _style.up
      } else {
        if (focused && _style.focused.isDefined) _style.focused
        else _style.up
      }
    }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    setBackground(getBackgroundDrawable)

    var offsetX = 0f
    var offsetY = 0f
    if (isPressed && !isDisabled) {
      offsetX = _style.pressedOffsetX
      offsetY = _style.pressedOffsetY
    } else if (isChecked && !isDisabled) {
      offsetX = _style.checkedOffsetX
      offsetY = _style.checkedOffsetY
    } else {
      offsetX = _style.unpressedOffsetX
      offsetY = _style.unpressedOffsetY
    }
    val offset = offsetX != 0 || offsetY != 0

    val children = getChildren
    if (offset) {
      var i = 0
      while (i < children.size) {
        children(i).moveBy(offsetX, offsetY)
        i += 1
      }
    }
    super.draw(batch, parentAlpha)
    if (offset) {
      var i = 0
      while (i < children.size) {
        children(i).moveBy(-offsetX, -offsetY)
        i += 1
      }
    }

    stage.foreach { stage =>
      if (stage.getActionsRequestRendering && isPressed != clickListener.pressed)
        Sge().graphics.requestRendering()
    }
  }

  override def getPrefWidth: Float = {
    var width = super.getPrefWidth
    _style.up.foreach(d => width = Math.max(width, d.minWidth))
    _style.down.foreach(d => width = Math.max(width, d.minWidth))
    _style.checked.foreach(d => width = Math.max(width, d.minWidth))
    width
  }

  override def getPrefHeight: Float = {
    var height = super.getPrefHeight
    _style.up.foreach(d => height = Math.max(height, d.minHeight))
    _style.down.foreach(d => height = Math.max(height, d.minHeight))
    _style.checked.foreach(d => height = Math.max(height, d.minHeight))
    height
  }

  override def getMinWidth: Float = getPrefWidth

  override def getMinHeight: Float = getPrefHeight
}

object Button {

  /** The style for a button, see {@link Button}.
    * @author
    *   mzechner
    */
  class ButtonStyle() {
    var up:               Nullable[Drawable] = Nullable.empty
    var down:             Nullable[Drawable] = Nullable.empty
    var over:             Nullable[Drawable] = Nullable.empty
    var focused:          Nullable[Drawable] = Nullable.empty
    var disabled:         Nullable[Drawable] = Nullable.empty
    var checked:          Nullable[Drawable] = Nullable.empty
    var checkedOver:      Nullable[Drawable] = Nullable.empty
    var checkedDown:      Nullable[Drawable] = Nullable.empty
    var checkedFocused:   Nullable[Drawable] = Nullable.empty
    var pressedOffsetX:   Float              = 0
    var pressedOffsetY:   Float              = 0
    var unpressedOffsetX: Float              = 0
    var unpressedOffsetY: Float              = 0
    var checkedOffsetX:   Float              = 0
    var checkedOffsetY:   Float              = 0

    def this(up: Nullable[Drawable], down: Nullable[Drawable], checked: Nullable[Drawable]) = {
      this()
      this.up = up
      this.down = down
      this.checked = checked
    }

    def this(style: ButtonStyle) = {
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
