/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 52
 * Covenant-baseline-methods: SlidingTransition,dir,render,slideLastScreen
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition

import sge.graphics.g2d.{ SpriteBatch, TextureRegion }
import sge.math.Interpolation
import sge.screen.transition.impl.SlidingDirection
import sge.utils.{ Nullable, Seconds }

/** The base class for sliding screen transitions. Can be reused.
  *
  * @author
  *   damios
  *
  * @see
  *   SlidingInTransition
  * @see
  *   SlidingOutTransition
  */
class SlidingTransition(
  batch:           SpriteBatch,
  private val dir: SlidingDirection,
  /** true if the last screen should slide out; false if the new screen should slide in. */
  private val slideLastScreen: Boolean,
  duration:                    Float,
  interpolation:               Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends BatchTransition(batch, duration, interpolation) {

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    batch.begin()

    if (slideLastScreen) {
      // slide out
      batch.draw(currScreen, 0, 0, width.toFloat, height.toFloat)
      batch.draw(lastScreen, width * dir.xPosFactor * progress, height * dir.yPosFactor * progress, width.toFloat, height.toFloat)
    } else {
      // slide in
      batch.draw(lastScreen, 0, 0, width.toFloat, height.toFloat)
      batch.draw(currScreen, width * dir.xPosFactor * (progress - 1), height * dir.yPosFactor * (progress - 1), width.toFloat, height.toFloat)
    }

    batch.end()
  }
}
