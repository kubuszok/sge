/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Touchpad.java
 * Original authors: Josh Street
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.{ Circle, Vector2 }
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.{ Align, Nullable }

/** An on-screen joystick. The movement area of the joystick is circular, centered on the touchpad, and its size determined by the smaller touchpad dimension. <p> The preferred size of the touchpad is
  * determined by the background. <p> {@link ChangeEvent} is fired when the touchpad knob is moved. Cancelling the event will move the knob to where it was previously.
  * @author
  *   Josh Street
  */
class Touchpad(private var deadzoneRadius: Float, style: Touchpad.TouchpadStyle) extends Widget with Styleable[Touchpad.TouchpadStyle] {
  import Touchpad._

  private var _style:         TouchpadStyle = scala.compiletime.uninitialized
  var touched:                Boolean       = false
  var resetOnTouchUp:         Boolean       = true
  private val knobBounds:     Circle        = new Circle(0, 0, 0)
  private val touchBounds:    Circle        = new Circle(0, 0, 0)
  private val deadzoneBounds: Circle        = new Circle(0, 0, 0)
  private val knobPosition:   Vector2       = new Vector2()
  private val knobPercent:    Vector2       = new Vector2()

  if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0")

  knobPosition.set(getWidth / 2f, getHeight / 2f)

  setStyle(style)
  setSize(getPrefWidth, getPrefHeight)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
        if (touched) false
        else {
          touched = true
          calculatePositionAndValue(x, y, false)
          true
        }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        calculatePositionAndValue(x, y, false)

      override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit = {
        touched = false
        calculatePositionAndValue(x, y, resetOnTouchUp)
      }
    }
  )

  // /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  // def this(deadzoneRadius: Float, skin: Skin) = this(deadzoneRadius, skin.get(classOf[TouchpadStyle]))

  // /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  // def this(deadzoneRadius: Float, skin: Skin, styleName: String) = this(deadzoneRadius, skin.get(styleName, classOf[TouchpadStyle]))

  private[ui] def calculatePositionAndValue(x: Float, y: Float, isTouchUp: Boolean): Unit = {
    val oldPositionX = knobPosition.x
    val oldPositionY = knobPosition.y
    val oldPercentX  = knobPercent.x
    val oldPercentY  = knobPercent.y
    val centerX      = knobBounds.x
    val centerY      = knobBounds.y
    knobPosition.set(centerX, centerY)
    knobPercent.set(0f, 0f)
    if (!isTouchUp) {
      if (!deadzoneBounds.contains(x, y)) {
        knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius)
        val length = knobPercent.length
        if (length > 1) knobPercent.scale(1 / length)
        if (knobBounds.contains(x, y)) {
          knobPosition.set(x, y)
        } else {
          knobPosition.set(knobPercent).normalize().scale(knobBounds.radius).+(knobBounds.x, knobBounds.y)
        }
      }
    }
    if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
      val changeEvent = Actor.POOLS.obtain(classOf[ChangeListener.ChangeEvent])
      if (fire(changeEvent)) {
        knobPercent.set(oldPercentX, oldPercentY)
        knobPosition.set(oldPositionX, oldPositionY)
      }
      Actor.POOLS.free(changeEvent)
    }
  }

  override def setStyle(style: TouchpadStyle): Unit = {
    if (style == null) throw new IllegalArgumentException("style cannot be null")
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the touchpad's style. Modifying the returned style may not have an effect until {@link #setStyle(TouchpadStyle)} is called.
    */
  override def getStyle: TouchpadStyle = _style

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (touchable && this.getTouchable != Touchable.enabled) scala.util.boundary.break(Nullable.empty)
    if (!isVisible) scala.util.boundary.break(Nullable.empty)
    if (touchBounds.contains(x, y)) Nullable(this) else Nullable.empty
  }

  override def layout(): Unit = {
    // Recalc pad and deadzone bounds
    val halfWidth  = getWidth / 2
    val halfHeight = getHeight / 2
    val radius     = Math.min(halfWidth, halfHeight)
    touchBounds.set(halfWidth, halfHeight, radius)
    val knobRadius = _style.knob.fold(radius) { knob =>
      radius - Math.max(knob.getMinWidth, knob.getMinHeight) / 2
    }
    knobBounds.set(halfWidth, halfHeight, knobRadius)
    deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius)
    // Recalc pad values and knob position
    knobPosition.set(halfWidth, halfHeight)
    knobPercent.set(0, 0)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val c = getColor
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)

    var x = getX
    var y = getY
    val w = getWidth
    val h = getHeight

    _style.background.foreach { bg =>
      bg.draw(batch, x, y, w, h)
    }

    _style.knob.foreach { knob =>
      x += knobPosition.x - knob.getMinWidth / 2f
      y += knobPosition.y - knob.getMinHeight / 2f
      knob.draw(batch, x, y, knob.getMinWidth, knob.getMinHeight)
    }
  }

  override def getPrefWidth: Float = _style.background.fold(0f)(_.getMinWidth)

  override def getPrefHeight: Float = _style.background.fold(0f)(_.getMinHeight)

  def isTouched: Boolean = touched

  def getResetOnTouchUp: Boolean = resetOnTouchUp

  /** @param reset Whether to reset the knob to the center on touch up. */
  def setResetOnTouchUp(reset: Boolean): Unit =
    this.resetOnTouchUp = reset

  /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  def setDeadzone(deadzoneRadius: Float): Unit = {
    if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0")
    this.deadzoneRadius = deadzoneRadius
    invalidate()
  }

  /** Returns the x-position of the knob relative to the center of the widget. The positive direction is right. */
  def getKnobX: Float = knobPosition.x

  /** Returns the y-position of the knob relative to the center of the widget. The positive direction is up. */
  def getKnobY: Float = knobPosition.y

  /** Returns the x-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement area. The positive direction is right.
    */
  def getKnobPercentX: Float = knobPercent.x

  /** Returns the y-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement area. The positive direction is up.
    */
  def getKnobPercentY: Float = knobPercent.y
}

object Touchpad {

  /** The style for a {@link Touchpad}.
    * @author
    *   Josh Street
    */
  class TouchpadStyle() {

    /** Stretched in both directions. */
    var background: Nullable[Drawable] = Nullable.empty
    var knob:       Nullable[Drawable] = Nullable.empty

    def this(background: Nullable[Drawable], knob: Nullable[Drawable]) = {
      this()
      this.background = background
      this.knob = knob
    }

    def this(style: TouchpadStyle) = {
      this()
      background = style.background
      knob = style.knob
    }
  }
}
