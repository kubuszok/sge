/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 69
 * Covenant-baseline-methods: HorizontalSlicingTransition,i,render,sliceCount,sliceHeight,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

import sge.graphics.g2d.{ SpriteBatch, TextureRegion }
import sge.graphics.glutils.HdpiUtils
import sge.math.{ Interpolation, MathUtils }
import sge.utils.{ Nullable, Seconds }

/** A transition where the new screen is sliding in in horizontal slices. Can be reused.
  *
  * @author
  *   damios
  */
class HorizontalSlicingTransition(
  batch:                  SpriteBatch,
  private val sliceCount: Int,
  duration:               Float,
  interpolation:          Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends BatchTransition(batch, duration, interpolation) {

  require(sliceCount >= 2, "The slice count has to be at least 2")

  def this(batch: SpriteBatch, sliceCount: Int, duration: Float)(using Sge) =
    this(batch, sliceCount, duration, Nullable.empty)

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    batch.begin()

    batch.draw(lastScreen, 0, 0, width.toFloat, height.toFloat)

    val sliceHeight = MathUtils.ceil(height.toFloat / sliceCount.toFloat)

    var i = 0
    while (i < sliceCount) {
      val y = i * sliceHeight

      val offsetX =
        if (i % 2 == 0) (width * (progress - 1)).toInt
        else (width * (1 - progress)).toInt

      batch.draw(
        currScreen.texture,
        offsetX.toFloat,
        y.toFloat,
        width.toFloat,
        sliceHeight.toFloat,
        0,
        HdpiUtils.toBackBufferY(Pixels(y)).toInt,
        HdpiUtils.toBackBufferX(Pixels(width)).toInt,
        HdpiUtils.toBackBufferY(Pixels(sliceHeight)).toInt,
        false,
        true
      )
      i += 1
    }

    batch.end()
  }
}
