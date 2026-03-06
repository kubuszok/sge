/*
 * SGE Demo — input visualization.
 *
 * Tests: Input polling (mouse/touch position, buttons, keyboard),
 *        ShapeRenderer drawing at input coordinates.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

import sge.graphics.OrthographicCamera
import sge.graphics.glutils.ShapeRenderer
import sge.utils.ScreenUtils

/** Visualizes input state: mouse/touch position, pressed keys, button state.
  *
  * Draws a cursor circle at each active pointer and a trail of recent positions.
  */
class InputScene extends DemoScene {

  override val name: String = "Input"

  private var renderer: ShapeRenderer      = scala.compiletime.uninitialized
  private var camera:   OrthographicCamera = scala.compiletime.uninitialized

  // Trail of recent mouse positions (ring buffer)
  private val trailSize = 100
  private val trailX    = new Array[Float](trailSize)
  private val trailY    = new Array[Float](trailSize)
  private var trailIdx  = 0

  override def init()(using Sge): Unit = {
    renderer = new ShapeRenderer()
    camera = new OrthographicCamera()
    val w = Sge().graphics.getWidth().toFloat
    val h = Sge().graphics.getHeight().toFloat
    camera.setToOrtho(false, w, h)
    camera.update()
  }

  override def render(elapsed: Float)(using Sge): Unit = {
    ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f)

    val w = Sge().graphics.getWidth().toFloat
    val h = Sge().graphics.getHeight().toFloat
    val input = Sge().input

    // Record mouse trail (flip Y: screen coords are top-down, GL is bottom-up)
    val mx = input.getX().toFloat
    val my = h - input.getY().toFloat
    trailX(trailIdx % trailSize) = mx
    trailY(trailIdx % trailSize) = my
    trailIdx += 1

    renderer.setProjectionMatrix(camera.combined)

    // Draw trail as fading line segments
    renderer.begin(ShapeRenderer.ShapeType.Line)
    val count = scala.math.min(trailIdx, trailSize)
    var i = 1
    while (i < count) {
      val prevI = (trailIdx - count + i - 1) % trailSize
      val currI = (trailIdx - count + i) % trailSize
      val alpha = i.toFloat / count
      renderer.setColor(0.3f, 0.8f, 1f, alpha)
      renderer.line(trailX(prevI), trailY(prevI), trailX(currI), trailY(currI))
      i += 1
    }
    renderer.end()

    // Draw pointer circles
    renderer.begin(ShapeRenderer.ShapeType.Filled)
    var p = 0
    while (p < input.getMaxPointers() && p < 5) {
      if (input.isTouched(p)) {
        val px = input.getX(p).toFloat
        val py = h - input.getY(p).toFloat
        val (r, g, b) = DemoUtils.hsvToRgb(p.toFloat / 5f, 1f, 1f)
        renderer.setColor(r, g, b, 0.8f)
        renderer.circle(px, py, 20f)
      }
      p += 1
    }

    // Draw cursor at current mouse position
    renderer.setColor(1f, 1f, 1f, 1f)
    renderer.circle(mx, my, 8f)
    renderer.end()
  }

  override def dispose()(using Sge): Unit =
    if (renderer != null) renderer.close()
}
