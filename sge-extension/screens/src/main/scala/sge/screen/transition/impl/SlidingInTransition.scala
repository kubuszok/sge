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

import sge.graphics.g2d.SpriteBatch
import sge.math.Interpolation
import sge.utils.Nullable

/** A transition where the new screen is sliding in. Can be reused.
  *
  * @author
  *   damios
  *
  * @see
  *   SlidingOutTransition
  */
class SlidingInTransition(
  batch:         SpriteBatch,
  dir:           SlidingDirection,
  duration:      Float,
  interpolation: Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends SlidingTransition(batch, dir, false, duration, interpolation) {

  def this(batch: SpriteBatch, dir: SlidingDirection, duration: Float)(using Sge) =
    this(batch, dir, duration, Nullable.empty)
}
