/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TemporalAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Renames: implements FinishableAction -> with FinishableAction
 *   Idiom: setPool(null) -> setPool(Nullable.empty); early return -> if/else;
 *          interpolation null-check -> interpolation.foreach; ternary -> if/else
 *   TODO: Java-style getters/setters -- getTime/setTime, getDuration/setDuration, getInterpolation/setInterpolation, isReverse/setReverse, isComplete
 *   TODO: opaque Seconds for duration, time, act(delta) params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable
import sge.math.Interpolation

/** Base class for actions that transition over time using the percent complete.
  * @author
  *   Nathan Sweet
  */
abstract class TemporalAction(private var duration: Float = 0, private var interpolation: Nullable[Interpolation] = Nullable.empty) extends Action with FinishableAction {

  private var time:     Float   = 0
  private var reverse:  Boolean = false
  private var began:    Boolean = false
  private var complete: Boolean = false

  def act(delta: Float): Boolean =
    if (complete) true
    else {
      val pool = getPool
      setPool(Nullable.empty) // Ensure this action can't be returned to the pool while executing.
      try {
        if (!began) {
          begin()
          began = true
        }
        time += delta
        complete = time >= duration
        var percent = if (complete) 1f else time / duration
        interpolation.foreach(i => percent = i.apply(percent))
        update(if (reverse) 1 - percent else percent)
        if (complete) end()
        complete
      } finally
        setPool(pool)
    }

  /** Called the first time {@link #act(float)} is called. This is a good place to query the {@link #actor actor's} starting state.
    */
  protected def begin(): Unit = {}

  /** Called the last time {@link #act(float)} is called. */
  protected def end(): Unit = {}

  /** Called each frame.
    * @param percent
    *   The percentage of completion for this action, growing from 0 to 1 over the duration. If {@link #setReverse(boolean) reversed}, this will shrink from 1 to 0.
    */
  protected def update(percent: Float): Unit

  /** Skips to the end of the transition. */
  def finish(): Unit =
    time = duration

  override def restart(): Unit = {
    time = 0
    began = false
    complete = false
  }

  override def reset(): Unit = {
    super.reset()
    reverse = false
    interpolation = Nullable.empty
  }

  /** Gets the transition time so far. */
  def getTime: Float = time

  /** Sets the transition time so far. */
  def setTime(time: Float): Unit =
    this.time = time

  def getDuration: Float = duration

  /** Sets the length of the transition in seconds. */
  def setDuration(duration: Float): Unit =
    this.duration = duration

  def getInterpolation: Nullable[Interpolation] = interpolation

  def setInterpolation(interpolation: Nullable[Interpolation]): Unit =
    this.interpolation = interpolation

  def isReverse: Boolean = reverse

  /** When true, the action's progress will go from 100% to 0%. */
  def setReverse(reverse: Boolean): Unit =
    this.reverse = reverse

  /** Returns true after {@link #act(float)} has been called where time >= duration. */
  def isComplete: Boolean = complete
}
