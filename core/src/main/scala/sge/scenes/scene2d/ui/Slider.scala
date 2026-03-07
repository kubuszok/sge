/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Slider.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) with Sge().input.isKeyPressed; Interpolation.linear SAM trait; Skin constructors present
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getSliderStyle, getSnapToValues/setSnapToValues, isOver, isDragging
 *   TODO: Int key refs (Input.Keys) → opaque Key type when available
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.math.Interpolation
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.Nullable

/** A slider is a horizontal indicator that allows a user to set a value. The slider has a range (min, max) and a stepping between each value the slider represents. <p> {@link ChangeEvent} is fired
  * when the slider knob is moved. Canceling the event will move the knob to where it was previously. <p> For a horizontal progress bar, its preferred height is determined by the larger of the knob
  * and background, and the preferred width is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Slider(
  min:      Float,
  max:      Float,
  stepSize: Float,
  vertical: Boolean,
  style:    Slider.SliderStyle
)(using Sge)
    extends ProgressBar(min, max, stepSize, vertical, style) {
  import Slider._

  var button:                             Int                    = -1
  var draggingPointer:                    Int                    = -1
  var mouseOver:                          Boolean                = false
  private var visualInterpolationInverse: Interpolation          = Interpolation.linear
  private var snapValues:                 Nullable[Array[Float]] = Nullable.empty
  private var threshold:                  Float                  = 0

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin)(using Sge) =
    this(min, max, stepSize, vertical, skin.get("default-" + (if (vertical) "vertical" else "horizontal"), classOf[Slider.SliderStyle]))

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin, styleName: String)(using Sge) =
    this(min, max, stepSize, vertical, skin.get(styleName, classOf[Slider.SliderStyle]))

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, btn: Int): Boolean =
        scala.util.boundary {
          if (disabled) scala.util.boundary.break(false)
          if (Slider.this.button != -1 && Slider.this.button != btn) scala.util.boundary.break(false)
          if (draggingPointer != -1) scala.util.boundary.break(false)
          draggingPointer = pointer
          calculatePositionAndValue(x, y)
          true
        }

      override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, btn: Int): Unit =
        if (pointer == draggingPointer) {
          draggingPointer = -1
          // The position is invalid when focus is cancelled
          if (event.isTouchFocusCancel || !calculatePositionAndValue(x, y)) {
            // Fire an event on touchUp even if the value didn't change, so listeners can see when a drag ends via isDragging.
            val changeEvent = Actor.POOLS.obtain(classOf[ChangeListener.ChangeEvent])
            fire(changeEvent)
            Actor.POOLS.free(changeEvent)
          }
        }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        calculatePositionAndValue(x, y)

      override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
        if (pointer == -1) mouseOver = true

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
        if (pointer == -1) mouseOver = false
    }
  )

  /** Returns the slider's style. Modifying the returned style may not have an effect until {@link #setStyle(ProgressBarStyle)} is called.
    */
  def getSliderStyle: SliderStyle = super.getStyle.asInstanceOf[SliderStyle]

  def isOver: Boolean = mouseOver

  override protected def getBackgroundDrawable(): Nullable[Drawable] = {
    val style = super.getStyle.asInstanceOf[SliderStyle]
    if (disabled && style.disabledBackground.isDefined) style.disabledBackground
    else if (isDragging && style.backgroundDown.isDefined) style.backgroundDown
    else if (mouseOver && style.backgroundOver.isDefined) style.backgroundOver
    else style.background
  }

  override protected def getKnobDrawable(): Nullable[Drawable] = {
    val style = super.getStyle.asInstanceOf[SliderStyle]
    if (disabled && style.disabledKnob.isDefined) style.disabledKnob
    else if (isDragging && style.knobDown.isDefined) style.knobDown
    else if (mouseOver && style.knobOver.isDefined) style.knobOver
    else Nullable(style.knob)
  }

  override protected def getKnobBeforeDrawable(): Nullable[Drawable] = {
    val style = super.getStyle.asInstanceOf[SliderStyle]
    if (disabled && style.disabledKnobBefore.isDefined) style.disabledKnobBefore
    else if (isDragging && style.knobBeforeDown.isDefined) style.knobBeforeDown
    else if (mouseOver && style.knobBeforeOver.isDefined) style.knobBeforeOver
    else style.knobBefore
  }

  override protected def getKnobAfterDrawable(): Nullable[Drawable] = {
    val style = super.getStyle.asInstanceOf[SliderStyle]
    if (disabled && style.disabledKnobAfter.isDefined) style.disabledKnobAfter
    else if (isDragging && style.knobAfterDown.isDefined) style.knobAfterDown
    else if (mouseOver && style.knobAfterOver.isDefined) style.knobAfterOver
    else style.knobAfter
  }

  private[ui] def calculatePositionAndValue(x: Float, y: Float): Boolean = {
    val style = getSliderStyle
    val knob  = Nullable(style.knob)
    val bg    = getBackgroundDrawable()

    val oldPosition = position

    val minVal = getMinValue
    val maxVal = getMaxValue

    val value: Float = if (vertical) {
      val bgDrawable = bg.getOrElse(throw new IllegalStateException("Slider background drawable must not be null"))
      val h          = height - bgDrawable.getTopHeight - bgDrawable.getBottomHeight
      val knobHeight = knob.map(_.getMinHeight).getOrElse(0f)
      position = y - bgDrawable.getBottomHeight - knobHeight * 0.5f
      val v = minVal + (maxVal - minVal) * visualInterpolationInverse.apply(position / (h - knobHeight))
      position = Math.max(Math.min(0, bgDrawable.getBottomHeight), position)
      position = Math.min(h - knobHeight, position)
      v
    } else {
      val bgDrawable = bg.getOrElse(throw new IllegalStateException("Slider background drawable must not be null"))
      val w          = width - bgDrawable.getLeftWidth - bgDrawable.getRightWidth
      val knobWidth  = knob.map(_.getMinWidth).getOrElse(0f)
      position = x - bgDrawable.getLeftWidth - knobWidth * 0.5f
      val v = minVal + (maxVal - minVal) * visualInterpolationInverse.apply(position / (w - knobWidth))
      position = Math.max(Math.min(0, bgDrawable.getLeftWidth), position)
      position = Math.min(w - knobWidth, position)
      v
    }

    val oldValue     = value
    val snappedValue =
      if (!Sge().input.isKeyPressed(Input.Keys.SHIFT_LEFT) && !Sge().input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) snap(value)
      else value
    val valueSet = setValue(snappedValue)
    if (snappedValue == oldValue) position = oldPosition
    valueSet
  }

  /** Returns a snapped value from a value calculated from the mouse position. The default implementation uses {@link #setSnapToValues(float, float...)}.
    */
  protected def snap(value: Float): Float =
    snapValues.fold(value) { values =>
      if (values.length == 0) value
      else {
        var bestDiff:  Float = -1
        var bestValue: Float = 0
        var i = 0
        while (i < values.length) {
          val snapValue = values(i)
          val diff      = Math.abs(value - snapValue)
          if (diff <= threshold) {
            if (bestDiff == -1 || diff < bestDiff) {
              bestDiff = diff
              bestValue = snapValue
            }
          }
          i += 1
        }
        if (bestDiff == -1) value else bestValue
      }
    }

  /** Makes this slider snap to the specified values when the knob is within the threshold.
    * @param values
    *   May be null to disable snapping.
    */
  def setSnapToValues(threshold: Float, values: Nullable[Array[Float]]): Unit = {
    values.foreach { v =>
      if (v.length == 0) throw new IllegalArgumentException("values cannot be empty.")
    }
    this.snapValues = values
    this.threshold = threshold
  }

  /** Makes this progress bar snap to the specified values, if the knob is within the threshold.
    * @param values
    *   May be null to disable snapping.
    * @deprecated
    *   Use {@link #setSnapToValues(float, float...)}.
    */
  @deprecated("Use setSnapToValues(threshold, values)", "")
  def setSnapToValues(values: Nullable[Array[Float]], threshold: Float): Unit =
    setSnapToValues(threshold, values)

  def getSnapToValues: Nullable[Array[Float]] = snapValues

  def getSnapToValuesThreshold: Float = threshold

  /** Returns true if the slider is being dragged. */
  def isDragging: Boolean = draggingPointer != -1

  /** Sets the mouse button, which can trigger a change of the slider. Is -1, so every button, by default. */
  def setButton(button: Int): Unit =
    this.button = button

  /** Sets the inverse interpolation to use for display. This should perform the inverse of the {@link #setVisualInterpolation(Interpolation) visual interpolation}.
    */
  def setVisualInterpolationInverse(interpolation: Interpolation): Unit =
    this.visualInterpolationInverse = interpolation

  /** Sets the value using the specified visual percent.
    * @see
    *   #setVisualInterpolation(Interpolation)
    */
  def setVisualPercent(percent: Float): Unit =
    setValue(min + (max - min) * visualInterpolationInverse.apply(percent))
}

object Slider {

  /** The style for a slider, see {@link Slider}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class SliderStyle() extends ProgressBar.ProgressBarStyle() {
    var backgroundOver: Nullable[Drawable] = Nullable.empty
    var backgroundDown: Nullable[Drawable] = Nullable.empty
    var knobOver:       Nullable[Drawable] = Nullable.empty
    var knobDown:       Nullable[Drawable] = Nullable.empty
    var knobBeforeOver: Nullable[Drawable] = Nullable.empty
    var knobBeforeDown: Nullable[Drawable] = Nullable.empty
    var knobAfterOver:  Nullable[Drawable] = Nullable.empty
    var knobAfterDown:  Nullable[Drawable] = Nullable.empty

    def this(background: Nullable[Drawable], knob: Drawable) = {
      this()
      this.background = background
      this.knob = knob
    }

    def this(style: SliderStyle) = {
      this()
      background = style.background
      disabledBackground = style.disabledBackground
      knob = style.knob
      disabledKnob = style.disabledKnob
      knobBefore = style.knobBefore
      disabledKnobBefore = style.disabledKnobBefore
      knobAfter = style.knobAfter
      disabledKnobAfter = style.disabledKnobAfter

      backgroundOver = style.backgroundOver
      backgroundDown = style.backgroundDown

      knobOver = style.knobOver
      knobDown = style.knobDown

      knobBeforeOver = style.knobBeforeOver
      knobBeforeDown = style.knobBeforeDown

      knobAfterOver = style.knobAfterOver
      knobAfterDown = style.knobAfterDown
    }
  }
}
