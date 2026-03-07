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
abstract class TemporalAction(var duration: Float = 0, var interpolation: Nullable[Interpolation] = Nullable.empty) extends Action with FinishableAction {

  /** The transition time so far. */
  var time: Float = 0

  /** When true, the action's progress will go from 100% to 0%. */
  var reverse:       Boolean = false
  private var began: Boolean = false

  /** Returns true after {@link #act(float)} has been called where time >= duration. */
  var complete: Boolean = false

  def act(delta: Float): Boolean =
    if (complete) true
    else {
      val savedPool = pool
      pool = Nullable.empty // Ensure this action can't be returned to the pool while executing.
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
        pool = savedPool
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
}
