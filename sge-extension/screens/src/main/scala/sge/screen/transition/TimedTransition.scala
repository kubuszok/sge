/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: TimedTransition,duration,interpolation,isDone,progress,render,show,timePassed
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition

import sge.graphics.g2d.TextureRegion
import sge.math.Interpolation
import sge.utils.{ Nullable, Seconds }

/** A screen transition that lasts for a certain duration. The transition is reset on show() and can thus be reused.
  *
  * @author
  *   damios
  */
abstract class TimedTransition(
  protected val duration:      Float,
  protected val interpolation: Nullable[Interpolation] = Nullable.empty
) extends ScreenTransition {

  require(duration > 0, "duration must be > 0")

  protected var timePassed: Float = 0f

  override def show(): Unit =
    this.timePassed = 0f

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit = {
    this.timePassed = this.timePassed + delta.toFloat

    var progress = this.timePassed / duration
    interpolation.foreach { interp =>
      progress = interp.apply(progress)
    }

    render(delta, lastScreen, currScreen, if (progress > 1f) 1f else progress)
  }

  /** The render method to use in the timed transition.
    *
    * @param delta
    *   the time delta in seconds
    * @param lastScreen
    *   the old screen as a texture region
    * @param currScreen
    *   the screen the manager is transitioning to as a texture region
    * @param progress
    *   the progress of the transition; from 0 (excl.) to 1 (incl.)
    */
  def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit

  override def isDone: Boolean = this.timePassed >= this.duration
}
