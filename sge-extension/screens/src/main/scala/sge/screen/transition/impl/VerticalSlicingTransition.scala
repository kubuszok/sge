/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen
package transition
package impl

import sge.graphics.g2d.{ SpriteBatch, TextureRegion }
import sge.graphics.glutils.HdpiUtils
import sge.math.{ Interpolation, MathUtils }
import sge.utils.{ Nullable, Seconds }

/** A transition where the new screen is sliding in in vertical slices. Can be reused.
  *
  * @author
  *   damios
  */
class VerticalSlicingTransition(
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

    val sliceWidth = MathUtils.ceil(width.toFloat / sliceCount.toFloat)

    var i = 0
    while (i < sliceCount) {
      val x = i * sliceWidth

      val offsetY =
        if (i % 2 == 0) (height * (progress - 1)).toInt
        else (height * (1 - progress)).toInt

      batch.draw(
        currScreen.texture,
        x.toFloat,
        offsetY.toFloat,
        sliceWidth.toFloat,
        height.toFloat,
        HdpiUtils.toBackBufferX(Pixels(x)).toInt,
        0,
        HdpiUtils.toBackBufferX(Pixels(sliceWidth)).toInt,
        HdpiUtils.toBackBufferY(Pixels(height)).toInt,
        false,
        true
      )
      i += 1
    }

    batch.end()
  }
}
