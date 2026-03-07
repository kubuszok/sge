/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Touchpad.java
 * Original authors: Josh Street
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context
 *   Idiom: split packages
 *   Fixes: Removed redundant Java-style getters/setters (touched/resetOnTouchUp are public vars; knobX/Y/knobPercentX/Y exposed as defs; style via Styleable)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.math.{ Circle, Vector2 }
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.Nullable

/** An on-screen joystick. The movement area of the joystick is circular, centered on the touchpad, and its size determined by the smaller touchpad dimension. <p> The preferred size of the touchpad is
  * determined by the background. <p> {@link ChangeEvent} is fired when the touchpad knob is moved. Cancelling the event will move the knob to where it was previously.
  * @author
  *   Josh Street
  */
class Touchpad(private var deadzoneRadius: Float, style: Touchpad.TouchpadStyle)(using Sge) extends Widget() with Styleable[Touchpad.TouchpadStyle] {
  import Touchpad._

  private var _style:         TouchpadStyle = scala.compiletime.uninitialized
  var touched:                Boolean       = false
  var resetOnTouchUp:         Boolean       = true
  private val knobBounds:     Circle        = Circle(0, 0, 0)
  private val touchBounds:    Circle        = Circle(0, 0, 0)
  private val deadzoneBounds: Circle        = Circle(0, 0, 0)
  private val knobPosition:   Vector2       = Vector2()
  private val knobPercent:    Vector2       = Vector2()

  if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0")

  knobPosition.set(width / 2f, height / 2f)

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

  /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  def this(deadzoneRadius: Float, skin: Skin)(using Sge) =
    this(deadzoneRadius, skin.get[Touchpad.TouchpadStyle](classOf[Touchpad.TouchpadStyle]))

  /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  def this(deadzoneRadius: Float, skin: Skin, styleName: String)(using Sge) =
    this(deadzoneRadius, skin.get[Touchpad.TouchpadStyle](styleName, classOf[Touchpad.TouchpadStyle]))

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
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the touchpad's style. Modifying the returned style may not have an effect until {@link #setStyle(TouchpadStyle)} is called.
    */
  override def getStyle: TouchpadStyle = _style

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (touchable && this.touchable != Touchable.enabled) scala.util.boundary.break(Nullable.empty)
    if (!visible) scala.util.boundary.break(Nullable.empty)
    if (touchBounds.contains(x, y)) Nullable(this) else Nullable.empty
  }

  override def layout(): Unit = {
    // Recalc pad and deadzone bounds
    val halfWidth  = width / 2
    val halfHeight = height / 2
    val radius     = Math.min(halfWidth, halfHeight)
    touchBounds.set(halfWidth, halfHeight, radius)
    val knobRadius = _style.knob.fold(radius) { knob =>
      radius - Math.max(knob.minWidth, knob.minHeight) / 2
    }
    knobBounds.set(halfWidth, halfHeight, knobRadius)
    deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius)
    // Recalc pad values and knob position
    knobPosition.set(halfWidth, halfHeight)
    knobPercent.set(0, 0)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val c = this.color
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)

    var dx = this.x
    var dy = this.y
    val w  = this.width
    val h  = this.height

    _style.background.foreach { bg =>
      bg.draw(batch, dx, dy, w, h)
    }

    _style.knob.foreach { knob =>
      dx += knobPosition.x - knob.minWidth / 2f
      dy += knobPosition.y - knob.minHeight / 2f
      knob.draw(batch, dx, dy, knob.minWidth, knob.minHeight)
    }
  }

  override def getPrefWidth: Float = _style.background.map(_.minWidth).getOrElse(0f)

  override def getPrefHeight: Float = _style.background.map(_.minHeight).getOrElse(0f)

  /** @param deadzoneRadius The distance in pixels from the center of the touchpad required for the knob to be moved. */
  def setDeadzone(deadzoneRadius: Float): Unit = {
    if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0")
    this.deadzoneRadius = deadzoneRadius
    invalidate()
  }

  /** Returns the x-position of the knob relative to the center of the widget. The positive direction is right. */
  def knobX: Float = knobPosition.x

  /** Returns the y-position of the knob relative to the center of the widget. The positive direction is up. */
  def knobY: Float = knobPosition.y

  /** Returns the x-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement area. The positive direction is right.
    */
  def knobPercentX: Float = knobPercent.x

  /** Returns the y-position of the knob as a percentage from the center of the touchpad to the edge of the circular movement area. The positive direction is up.
    */
  def knobPercentY: Float = knobPercent.y
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
