/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 40
 * Covenant-baseline-methods: BlankTimedTransition,close,render,resize,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

import sge.graphics.g2d.TextureRegion
import sge.math.Interpolation
import sge.utils.{ Nullable, Seconds }

/** A blank screen transition going on for a given duration. Can be reused.
  *
  * @author
  *   damios
  */
class BlankTimedTransition(
  duration:      Float,
  interpolation: Nullable[Interpolation] = Nullable.empty
) extends TimedTransition(duration, interpolation) {

  def this(duration: Float) = this(duration, Nullable.empty)

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    // do nothing
  }

  override def resize(width: Pixels, height: Pixels): Unit = {
    // not needed
  }

  override def close(): Unit = {
    // not needed
  }
}
