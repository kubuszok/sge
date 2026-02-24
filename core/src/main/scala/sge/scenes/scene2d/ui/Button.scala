/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Button.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
class Button() extends Table() with Disableable with Styleable[Button.ButtonStyle] {
  import Button._

  private var _style:                   ButtonStyle              = scala.compiletime.uninitialized
  private[ui] var isChecked:            Boolean                  = false
  private[ui] var _isDisabled:          Boolean                  = false
  private[ui] var buttonGroup:          Nullable[ButtonGroup[?]] = Nullable.empty
  private var clickListener:            ClickListener            = scala.compiletime.uninitialized
  private var programmaticChangeEvents: Boolean                  = true

  initialize()

  // def this(skin: Skin) = {
  //   this()
  //   initialize()
  //   setSkin(skin)
  //   setStyle(skin.get(classOf[ButtonStyle]))
  //   setSize(getPrefWidth, getPrefHeight)
  // }

  // def this(skin: Skin, styleName: String) = {
  //   this()
  //   initialize()
  //   setSkin(skin)
  //   setStyle(skin.get(styleName, classOf[ButtonStyle]))
  //   setSize(getPrefWidth, getPrefHeight)
  // }

  // def this(child: Actor, skin: Skin, styleName: String) = {
  //   this(child, skin.get(styleName, classOf[ButtonStyle]))
  //   setSkin(skin)
  // }

  def this(child: Actor, style: Button.ButtonStyle) = {
    this()
    add(Nullable[Actor](child))
    setStyle(style)
    setSize(getPrefWidth, getPrefHeight)
  }

  def this(style: Button.ButtonStyle) = {
    this()
    setStyle(style)
    setSize(getPrefWidth, getPrefHeight)
  }

  private def initialize(): Unit = {
    setTouchable(Touchable.enabled)
    clickListener = new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit =
        if (!isDisabled) setChecked(!isChecked, true)
    }
    addListener(clickListener)
  }

  def this(up: Nullable[Drawable]) = {
    this(new Button.ButtonStyle(up, Nullable.empty, Nullable.empty))
  }

  def this(up: Nullable[Drawable], down: Nullable[Drawable]) = {
    this(new Button.ButtonStyle(up, down, Nullable.empty))
  }

  def this(up: Nullable[Drawable], down: Nullable[Drawable], checked: Nullable[Drawable]) = {
    this(new Button.ButtonStyle(up, down, checked))
  }

  // def this(child: Actor, skin: Skin) = this(child, skin.get(classOf[ButtonStyle]))

  def setChecked(isChecked: Boolean): Unit =
    setChecked(isChecked, programmaticChangeEvents)

  private[ui] def setChecked(isChecked: Boolean, fireEvent: Boolean): Unit =
    if (this.isChecked != isChecked && buttonGroup.fold(true)(bg => bg.asInstanceOf[ButtonGroup[Button]].canCheck(this, isChecked))) {
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

  def isOver: Boolean = clickListener.isOver

  def getClickListener: ClickListener = clickListener

  override def isDisabled: Boolean = _isDisabled

  /** When true, the button will not toggle {@link #isChecked()} when clicked and will not fire a {@link ChangeEvent}. */
  override def setDisabled(isDisabled: Boolean): Unit =
    this._isDisabled = isDisabled

  /** If false, {@link #setChecked(boolean)} and {@link #toggle()} will not fire {@link ChangeEvent}. The event will only be fired only when the user clicks the button
    */
  def setProgrammaticChangeEvents(programmaticChangeEvents: Boolean): Unit =
    this.programmaticChangeEvents = programmaticChangeEvents

  def getProgrammaticChangeEvents: Boolean = programmaticChangeEvents

  override def setStyle(style: ButtonStyle): Unit = {
    if (style == null) throw new IllegalArgumentException("style cannot be null.")
    this._style = style

    setBackground(getBackgroundDrawable)
  }

  /** Returns the button's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is called.
    */
  override def getStyle: ButtonStyle = _style

  /** @return May be null. */
  def getButtonGroup: Nullable[ButtonGroup[?]] = buttonGroup

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

    // TODO: requestRendering - draw doesn't have `using Sge` context
    // getStage.foreach { stage =>
    //   if (stage.getActionsRequestRendering && isPressed != clickListener.isPressed)
    //     sge.graphics.requestRendering()
    // }
  }

  override def getPrefWidth: Float = {
    var width = super.getPrefWidth
    _style.up.foreach(d => width = Math.max(width, d.getMinWidth))
    _style.down.foreach(d => width = Math.max(width, d.getMinWidth))
    _style.checked.foreach(d => width = Math.max(width, d.getMinWidth))
    width
  }

  override def getPrefHeight: Float = {
    var height = super.getPrefHeight
    _style.up.foreach(d => height = Math.max(height, d.getMinHeight))
    _style.down.foreach(d => height = Math.max(height, d.getMinHeight))
    _style.checked.foreach(d => height = Math.max(height, d.getMinHeight))
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
