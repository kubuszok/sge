/*
 * SGE Demo — simplest possible scene: clear screen to a cycling color.
 *
 * Tests: GL context creation, glClearColor, glClear, frame timing.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

import sge.utils.ScreenUtils

/** Cycles the screen clear color through the rainbow using a hue rotation.
  *
  * This is the absolute minimum test — if this works, the GL context is alive and rendering frames.
  */
object ClearColorScene extends DemoScene {

  override val name: String = "Clear Color"

  override def init()(using Sge): Unit = ()

  override def render(elapsed: Float)(using Sge): Unit = {
    // Rotate hue over time (full cycle every 3 seconds)
    val hue       = (elapsed / 3f) % 1f
    val (r, g, b) = DemoUtils.hsvToRgb(hue, 0.8f, 0.9f)
    ScreenUtils.clear(r, g, b, 1f)
  }

  override def dispose()(using Sge): Unit = ()

}
