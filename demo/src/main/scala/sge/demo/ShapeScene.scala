/*
 * SGE Demo — shape rendering with ShapeRenderer.
 *
 * Tests: ShapeRenderer (shader compilation, vertex buffers, draw calls),
 *        OrthographicCamera, color blending, basic geometry.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

import sge.graphics.OrthographicCamera
import sge.graphics.glutils.ShapeRenderer
import sge.utils.ScreenUtils

/** Draws a variety of shapes that animate over time.
  *
  * Exercises ShapeRenderer.Line and ShapeRenderer.Filled modes, camera projection, and color interpolation.
  */
class ShapeScene extends DemoScene {

  override val name: String = "Shapes"

  private var renderer: ShapeRenderer      = scala.compiletime.uninitialized
  private var camera:   OrthographicCamera = scala.compiletime.uninitialized

  override def init()(using Sge): Unit = {
    renderer = new ShapeRenderer()
    camera = new OrthographicCamera()
    val w = Sge().graphics.getWidth().toFloat
    val h = Sge().graphics.getHeight().toFloat
    camera.setToOrtho(false, w, h)
    camera.update()
  }

  override def render(elapsed: Float)(using Sge): Unit = {
    ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f)

    val w = Sge().graphics.getWidth().toFloat
    val h = Sge().graphics.getHeight().toFloat

    renderer.setProjectionMatrix(camera.combined)

    // Filled shapes
    renderer.begin(ShapeRenderer.ShapeType.Filled)

    // Pulsing circle in the center
    val radius = 50f + 30f * scala.math.sin(elapsed * 2.0).toFloat
    renderer.setColor(0.2f, 0.6f, 1f, 1f)
    renderer.circle(w / 2f, h / 2f, radius)

    // Rotating rectangles around the center
    val count = 6
    var i     = 0
    while (i < count) {
      val angle     = elapsed + i * (scala.math.Pi.toFloat * 2f / count)
      val cx        = w / 2f + 150f * scala.math.cos(angle).toFloat
      val cy        = h / 2f + 150f * scala.math.sin(angle).toFloat
      val hue       = (i.toFloat / count + elapsed * 0.1f) % 1f
      val (r, g, b) = DemoUtils.hsvToRgb(hue, 0.9f, 1f)
      renderer.setColor(r, g, b, 1f)
      renderer.rectangle(cx - 20f, cy - 20f, 40f, 40f)
      i += 1
    }

    renderer.end()

    // Line shapes
    renderer.begin(ShapeRenderer.ShapeType.Line)
    renderer.setColor(1f, 1f, 1f, 0.5f)

    // Grid lines
    val step = 80f
    var x    = step
    while (x < w) {
      renderer.line(x, 0f, x, h)
      x += step
    }
    var y = step
    while (y < h) {
      renderer.line(0f, y, w, y)
      y += step
    }

    // Crosshair at center
    renderer.setColor(1f, 1f, 0f, 1f)
    renderer.line(w / 2f - 20f, h / 2f, w / 2f + 20f, h / 2f)
    renderer.line(w / 2f, h / 2f - 20f, w / 2f, h / 2f + 20f)

    renderer.end()
  }

  override def dispose()(using Sge): Unit =
    if (renderer != null) renderer.close()
}
