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

import sge.graphics.g2d.{ SpriteBatch, TextureRegion }
import sge.math.Interpolation
import sge.utils.{ Nullable, Seconds }
import sge.utils.viewport.{ ScreenViewport, Viewport }

/** The base class for all transitions using a SpriteBatch. Can be reused.
  *
  * @author
  *   damios
  */
abstract class BatchTransition(
  protected val batch: SpriteBatch,
  duration:            Float,
  interpolation:       Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends TimedTransition(duration, interpolation) {

  protected val viewport: Viewport = ScreenViewport()
  protected var width:    Int      = 0
  protected var height:   Int      = 0

  final override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit = {
    viewport.apply()
    batch.projectionMatrix = viewport.camera.combined
    super.render(delta, lastScreen, currScreen)
  }

  /** The viewport was already applied and the batch's projection matrix is set.
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
  override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit

  override def resize(width: Pixels, height: Pixels): Unit = {
    this.width = width.toInt
    this.height = height.toInt
    viewport.update(width, height, true)
  }

  override def close(): Unit = {
    // there isn't anything to dispose
  }
}
