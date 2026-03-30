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
import sge.math.Interpolation
import sge.utils.{ Nullable, Seconds }

/** A transition that blends two screens together over a certain time interval. Can be reused.
  *
  * @author
  *   damios
  */
class BlendingTransition(
  batch:         SpriteBatch,
  duration:      Float,
  interpolation: Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends BatchTransition(batch, duration, interpolation) {

  def this(batch: SpriteBatch, duration: Float)(using Sge) =
    this(batch, duration, Nullable.empty)

  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
    batch.begin()

    // Blends the two screens
    val c = batch.color
    batch.draw(lastScreen, 0, 0, width.toFloat, height.toFloat)

    batch.setColor(c.r, c.g, c.b, progress)
    batch.draw(currScreen, 0, 0, width.toFloat, height.toFloat)
    batch.setColor(c.r, c.g, c.b, 1)

    batch.end()
  }
}
