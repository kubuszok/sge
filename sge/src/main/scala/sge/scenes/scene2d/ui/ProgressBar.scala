/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ProgressBar.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context; Skin constructors present (not commented out)
 *   Idiom: split packages
 *   Fixes: Removed redundant Java-style getters/setters (min/max/stepSize/vertical/disabled are public; getValue→value, getVisualValue→visualValue, getPercent→percent, getVisualPercent→visualPercent, isAnimating→animating, getKnobPosition removed, programmaticChangeEvents is public var; style via Styleable)
 *   Convention: opaque Seconds for animateDuration, animateTime
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.math.{ Interpolation, MathUtils }
import sge.scenes.scene2d.utils.{ ChangeListener, Disableable, Drawable }
import sge.utils.{ Nullable, Seconds }

/** A progress bar is a widget that visually displays the progress of some activity or a value within given range. The progress bar has a range (min, max) and a stepping between each value it
  * represents. The percentage of completeness typically starts out as an empty progress bar and gradually becomes filled in as the task or variable value progresses. <p> {@link ChangeEvent} is fired
  * when the progress bar knob is moved. Cancelling the event will move the knob to where it was previously. <p> For a horizontal progress bar, its preferred height is determined by the larger of the
  * knob and background, and the preferred width is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class ProgressBar(
  var min:      Float,
  var max:      Float,
  var stepSize: Float,
  val vertical: Boolean,
  initialStyle: ProgressBar.ProgressBarStyle
)(using Sge)
    extends Widget
    with Disableable
    with Styleable[ProgressBar.ProgressBarStyle] {
  import ProgressBar._

  private var _style:               ProgressBarStyle  = scala.compiletime.uninitialized
  private var _value:               Float             = scala.compiletime.uninitialized
  private var animateFromValue:     Float             = scala.compiletime.uninitialized
  var position:                     Float             = scala.compiletime.uninitialized
  private var animateDuration:      sge.utils.Seconds = sge.utils.Seconds.zero
  private var animateTime:          sge.utils.Seconds = sge.utils.Seconds.zero
  private var animateInterpolation: Interpolation     = Interpolation.linear
  private var visualInterpolation:  Interpolation     = Interpolation.linear
  private var _disabled:            Boolean           = false
  private var round:                Boolean           = true
  var programmaticChangeEvents:     Boolean           = true

  if (min > max) throw new IllegalArgumentException("max must be > min. min,max: " + min + ", " + max)
  if (stepSize <= 0) throw new IllegalArgumentException("stepSize must be > 0: " + stepSize)
  setStyle(initialStyle)
  this._value = min
  setSize(prefWidth, prefHeight)

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin)(using Sge) =
    this(
      min,
      max,
      stepSize,
      vertical,
      skin.get[ProgressBar.ProgressBarStyle]("default-" + (if (vertical) "vertical" else "horizontal"))
    )

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin, styleName: String)(using Sge) =
    this(min, max, stepSize, vertical, skin.get[ProgressBar.ProgressBarStyle](styleName))

  override def setStyle(style: ProgressBarStyle): Unit = {
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the progress bar's style. Modifying the returned style may not have an effect until {@link #setStyle(ProgressBarStyle)} is called.
    */
  override def style: ProgressBarStyle = _style

  override def act(delta: Seconds): Unit = {
    super.act(delta)
    if (animateTime > sge.utils.Seconds.zero) {
      animateTime = animateTime - delta
      this.stage.foreach { s =>
        if (s.actionsRequestRendering) Sge().graphics.requestRendering()
      }
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val style       = this._style
    val knob        = Nullable(style.knob)
    val currentKnob = knobDrawable
    val bg          = backgroundDrawable
    val knobBefore  = knobBeforeDrawable
    val knobAfter   = knobAfterDrawable

    val c          = this.color
    val bx         = this.x
    val by         = this.y
    var bw         = this.width
    var bh         = this.height
    val knobHeight = knob.map(_.minHeight).getOrElse(0f)
    val knobWidth  = knob.map(_.minWidth).getOrElse(0f)
    val percent    = visualPercent

    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)

    if (vertical) {
      var bgTopHeight    = 0f
      var bgBottomHeight = 0f
      bg.foreach { bgDrawable =>
        drawRound(batch, bgDrawable, bx + (bw - bgDrawable.minWidth) * 0.5f, by, bgDrawable.minWidth, bh)
        bgTopHeight = bgDrawable.topHeight
        bgBottomHeight = bgDrawable.bottomHeight
        bh -= bgTopHeight + bgBottomHeight
      }

      val total        = bh - knobHeight
      val beforeHeight = MathUtils.clamp(total * percent, 0, total)
      position = bgBottomHeight + beforeHeight

      val knobHeightHalf = knobHeight * 0.5f
      knobBefore.foreach { kb =>
        drawRound(batch, kb, bx + (bw - kb.minWidth) * 0.5f, by + bgBottomHeight, kb.minWidth, beforeHeight + knobHeightHalf)
      }
      knobAfter.foreach { ka =>
        drawRound(
          batch,
          ka,
          bx + (bw - ka.minWidth) * 0.5f,
          by + position + knobHeightHalf,
          ka.minWidth,
          total - (if (round) Math.ceil((beforeHeight - knobHeightHalf).toDouble).toFloat else beforeHeight - knobHeightHalf)
        )
      }
      currentKnob.foreach { ck =>
        val w = ck.minWidth
        val h = ck.minHeight
        drawRound(batch, ck, bx + (bw - w) * 0.5f, by + position + (knobHeight - h) * 0.5f, w, h)
      }
    } else {
      var bgLeftWidth  = 0f
      var bgRightWidth = 0f
      bg.foreach { bgDrawable =>
        drawRound(
          batch,
          bgDrawable,
          bx,
          Math.round(by + (bh - bgDrawable.minHeight) * 0.5f).toFloat,
          bw,
          Math.round(bgDrawable.minHeight).toFloat
        )
        bgLeftWidth = bgDrawable.leftWidth
        bgRightWidth = bgDrawable.rightWidth
        bw -= bgLeftWidth + bgRightWidth
      }

      val total       = bw - knobWidth
      val beforeWidth = MathUtils.clamp(total * percent, 0, total)
      position = bgLeftWidth + beforeWidth

      val knobWidthHalf = knobWidth * 0.5f
      knobBefore.foreach { kb =>
        drawRound(batch, kb, bx + bgLeftWidth, by + (bh - kb.minHeight) * 0.5f, beforeWidth + knobWidthHalf, kb.minHeight)
      }
      knobAfter.foreach { ka =>
        drawRound(
          batch,
          ka,
          bx + position + knobWidthHalf,
          by + (bh - ka.minHeight) * 0.5f,
          total - (if (round) Math.ceil((beforeWidth - knobWidthHalf).toDouble).toFloat else beforeWidth - knobWidthHalf),
          ka.minHeight
        )
      }
      currentKnob.foreach { ck =>
        val w = ck.minWidth
        val h = ck.minHeight
        drawRound(batch, ck, bx + position + (knobWidth - w) * 0.5f, by + (bh - h) * 0.5f, w, h)
      }
    }
  }

  private def drawRound(batch: Batch, drawable: Drawable, x: Float, y: Float, w: Float, h: Float): Unit = {
    var dx = x
    var dy = y
    var dw = w
    var dh = h
    if (round) {
      dx = Math.floor(dx.toDouble).toFloat
      dy = Math.floor(dy.toDouble).toFloat
      dw = Math.ceil(dw.toDouble).toFloat
      dh = Math.ceil(dh.toDouble).toFloat
    }
    drawable.draw(batch, dx, dy, dw, dh)
  }

  def value: Float = _value

  /** If {@link #setAnimateDuration(float) animating} the progress bar value, this returns the value current displayed. */
  def visualValue: Float =
    if (animateTime > sge.utils.Seconds.zero) animateInterpolation.apply(animateFromValue, _value, 1 - (animateTime / animateDuration))
    else _value

  /** Sets the visual value equal to the actual value. This can be used to set the value without animating. */
  def updateVisualValue(): Unit =
    animateTime = sge.utils.Seconds.zero

  def percent: Float =
    if (min == max) 0
    else (_value - min) / (max - min)

  def visualPercent: Float =
    if (min == max) 0
    else visualInterpolation.apply((visualValue - min) / (max - min))

  protected def backgroundDrawable: Nullable[Drawable] =
    if (disabled && _style.disabledBackground.isDefined) _style.disabledBackground
    else _style.background

  protected def knobDrawable: Nullable[Drawable] =
    if (disabled && _style.disabledKnob.isDefined) _style.disabledKnob
    else Nullable(_style.knob)

  protected def knobBeforeDrawable: Nullable[Drawable] =
    if (disabled && _style.disabledKnobBefore.isDefined) _style.disabledKnobBefore
    else _style.knobBefore

  protected def knobAfterDrawable: Nullable[Drawable] =
    if (disabled && _style.disabledKnobAfter.isDefined) _style.disabledKnobAfter
    else _style.knobAfter

  /** Sets the progress bar position, rounded to the nearest step size and clamped to the minimum and maximum values. {@link #clamp(float)} can be overridden to allow values outside of the progress
    * bar's min/max range.
    * @return
    *   false if the value was not changed because the progress bar already had the value or it was canceled by a listener.
    */
  def setValue(value: Float): Boolean = scala.util.boundary {
    val clampedValue = clamp(round(value))
    val oldValue     = this._value
    if (clampedValue == oldValue) scala.util.boundary.break(false)
    val oldVisualValue = visualValue
    this._value = clampedValue

    if (programmaticChangeEvents) {
      val changeEvent = Actor.POOLS.obtain[ChangeListener.ChangeEvent]
      val cancelled   = fire(changeEvent)
      Actor.POOLS.free(changeEvent)
      if (cancelled) {
        this._value = oldValue
        scala.util.boundary.break(false)
      }
    }

    if (animateDuration > sge.utils.Seconds.zero) {
      animateFromValue = oldVisualValue
      animateTime = animateDuration
    }
    true
  }

  /** Rounds the value using the progress bar's step size. This can be overridden to customize or disable rounding. */
  protected def round(value: Float): Float =
    Math.round(value / stepSize) * stepSize

  /** Clamps the value to the progress bar's min/max range. This can be overridden to allow a range different from the progress bar knob's range.
    */
  protected def clamp(value: Float): Float =
    MathUtils.clamp(value, min, max)

  /** Sets the range of this progress bar. The progress bar's current value is clamped to the range. */
  def setRange(min: Float, max: Float): Unit = {
    if (min > max) throw new IllegalArgumentException("min must be <= max: " + min + " <= " + max)
    this.min = min
    this.max = max
    if (_value < min)
      setValue(min)
    else if (_value > max)
      setValue(max)
  }

  def setStepSize(stepSize: Float): Unit = {
    if (stepSize <= 0) throw new IllegalArgumentException("steps must be > 0: " + stepSize)
    this.stepSize = stepSize
  }

  override def prefWidth: Float =
    if (vertical) {
      val knob = Nullable(_style.knob)
      val bg   = backgroundDrawable
      Math.max(knob.map(_.minWidth).getOrElse(0f), bg.map(_.minWidth).getOrElse(0f))
    } else 140

  override def prefHeight: Float =
    if (vertical) 140
    else {
      val knob = Nullable(_style.knob)
      val bg   = backgroundDrawable
      Math.max(knob.map(_.minHeight).getOrElse(0f), bg.map(_.minHeight).getOrElse(0f))
    }

  /** If > 0, changes to the progress bar value via {@link #setValue(float)} will happen over this duration in seconds. */
  def setAnimateDuration(duration: Float): Unit =
    this.animateDuration = sge.utils.Seconds(duration)

  /** Sets the interpolation to use for {@link #setAnimateDuration(float)}. */
  def setAnimateInterpolation(animateInterpolation: Interpolation): Unit =
    this.animateInterpolation = animateInterpolation

  /** Sets the interpolation to use for display. */
  def setVisualInterpolation(interpolation: Interpolation): Unit =
    this.visualInterpolation = interpolation

  /** If true (the default), inner Drawable positions and sizes are rounded to integers. */
  def setRound(round: Boolean): Unit =
    this.round = round

  override def disabled_=(value: Boolean): Unit =
    this._disabled = value

  def animating: Boolean = animateTime > sge.utils.Seconds.zero

  override def disabled: Boolean = _disabled
}

object ProgressBar {

  /** The style for a progress bar, see {@link ProgressBar}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class ProgressBarStyle() {

    /** The progress bar background, stretched only in one direction. */
    var background:         Nullable[Drawable] = Nullable.empty
    var disabledBackground: Nullable[Drawable] = Nullable.empty
    var knob:               Drawable           = scala.compiletime.uninitialized
    var disabledKnob:       Nullable[Drawable] = Nullable.empty
    var knobBefore:         Nullable[Drawable] = Nullable.empty
    var disabledKnobBefore: Nullable[Drawable] = Nullable.empty
    var knobAfter:          Nullable[Drawable] = Nullable.empty
    var disabledKnobAfter:  Nullable[Drawable] = Nullable.empty

    def this(background: Nullable[Drawable], knob: Drawable) = {
      this()
      this.background = background
      this.knob = knob
    }

    def this(style: ProgressBarStyle) = {
      this()
      background = style.background
      disabledBackground = style.disabledBackground
      knob = style.knob
      disabledKnob = style.disabledKnob
      knobBefore = style.knobBefore
      disabledKnobBefore = style.disabledKnobBefore
      knobAfter = style.knobAfter
      disabledKnobAfter = style.disabledKnobAfter
    }
  }
}
