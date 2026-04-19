/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: PushTransition,dir,render,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

import sge.graphics.g2d.{ SpriteBatch, TextureRegion }
import sge.math.Interpolation
import sge.utils.{ Nullable, Seconds }

/** A transition where the new screen is sliding in, while the last screen is sliding out. Thus, the new screen is pushing the last screen out, so to speak. Can be reused.
  *
  * @author
  *   damios
  */
class PushTransition(
  batch:           SpriteBatch,
  private val dir: SlidingDirection,
  duration:        Float,
  interpolation:   Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends BatchTransition(batch, duration, interpolation) {

  def this(batch: SpriteBatch, dir: SlidingDirection, duration: Float)(using Sge) =
    this(batch, dir, duration, Nullable.empty)

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    batch.begin()

    batch.draw(currScreen, width * dir.xPosFactor * (progress - 1), height * dir.yPosFactor * (progress - 1), width.toFloat, height.toFloat)
    batch.draw(lastScreen, width * dir.xPosFactor * progress, height * dir.yPosFactor * progress, width.toFloat, height.toFloat)

    batch.end()
  }
}
