/*
 * SGE Demos — Split-screen viewport comparison gallery.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package viewports

import scala.compiletime.uninitialized

import _root_.sge.{Pixels, Sge}
import _root_.sge.graphics.Color
import _root_.sge.graphics.EnableCap
import _root_.sge.graphics.OrthographicCamera
import _root_.sge.graphics.glutils.HdpiUtils
import _root_.sge.graphics.glutils.ShapeRenderer
import _root_.sge.graphics.glutils.ShapeRenderer.ShapeType
import _root_.sge.math.MathUtils
import _root_.sge.utils.ScreenUtils
import _root_.sge.utils.Scaling
import _root_.sge.utils.viewport.ExtendViewport
import _root_.sge.utils.viewport.FillViewport
import _root_.sge.utils.viewport.FitViewport
import _root_.sge.utils.viewport.ScalingViewport
import _root_.sge.utils.viewport.ScreenViewport
import _root_.sge.utils.viewport.StretchViewport
import _root_.sge.utils.viewport.Viewport
import sge.demos.shared.DemoScene
import sge.demos.shared.DemoUtils

/** Renders the same test pattern through 6 different viewport types in a 3x2 grid. */
object ViewportGalleryGame extends DemoScene {

  override def name: String = "Viewport Gallery"

  // Logical world for each sub-viewport
  private val WorldW = 200f
  private val WorldH = 150f

  // Grid layout: 3 columns, 2 rows
  private val Cols = 3
  private val Rows = 2

  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewports: Array[Viewport]   = uninitialized
  private var elapsed: Float               = 0f

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewports = Array(
      ScreenViewport(),
      FitViewport(WorldW, WorldH),
      FillViewport(WorldW, WorldH),
      StretchViewport(WorldW, WorldH),
      ExtendViewport(WorldW, WorldH),
      ScalingViewport(Scaling.fill, WorldW, WorldH, OrthographicCamera())
    )
    elapsed = 0f
  }

  override def render(dt: Float)(using Sge): Unit = {
    elapsed += dt

    val gl  = Sge().graphics.gl
    val sw  = Sge().graphics.getWidth().toInt
    val sh  = Sge().graphics.getHeight().toInt
    val cellW = sw / Cols
    val cellH = sh / Rows

    // Clear the full screen
    gl.glDisable(EnableCap.ScissorTest)
    ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f)

    // Enable scissor test so each cell clips independently
    gl.glEnable(EnableCap.ScissorTest)

    var idx = 0
    while (idx < viewports.length) {
      val col = idx % Cols
      val row = idx / Cols
      // OpenGL origin is bottom-left; row 0 = top → flip vertically
      val ox = col * cellW
      val oy = sh - (row + 1) * cellH

      // Scissor to this cell
      HdpiUtils.glScissor(Pixels(ox), Pixels(oy), Pixels(cellW), Pixels(cellH))

      // Configure and apply the sub-viewport.
      // update() computes viewport bounds relative to (0,0), so we add the cell
      // offset afterwards and re-apply the GL viewport.
      val vp = viewports(idx)
      vp.update(Pixels(cellW), Pixels(cellH), false)
      vp.screenX = Pixels(vp.screenX.toInt + ox)
      vp.screenY = Pixels(vp.screenY.toInt + oy)
      vp.camera.position.set(0f, 0f, 0f)
      vp.camera.update()
      HdpiUtils.glViewport(vp.screenX, vp.screenY, vp.screenWidth, vp.screenHeight)

      shapeRenderer.setProjectionMatrix(vp.camera.combined)

      // --- Draw test scene ---
      drawTestScene(vp.worldWidth, vp.worldHeight)

      // --- Label indicator: small colored rectangle in top-left corner ---
      shapeRenderer.begin(ShapeType.Filled)
      shapeRenderer.setColor(labelColor(idx))
      val lw = vp.worldWidth * 0.35f
      val lh = 8f
      shapeRenderer.rectangle(-vp.worldWidth / 2f + 4f, vp.worldHeight / 2f - lh - 4f, lw, lh)
      shapeRenderer.end()

      idx += 1
    }

    gl.glDisable(EnableCap.ScissorTest)
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    // Viewports are updated each frame in render(), so nothing extra needed here.
  }

  override def dispose()(using Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Test pattern ---

  private def drawTestScene(ww: Float, wh: Float)(using Sge): Unit = {
    val hw = ww / 2f
    val hh = wh / 2f

    // Subtle hue animation
    val hue = (elapsed * 30f) % 360f
    val bgColor = DemoUtils.hsvToColor(hue, 0.15f, 0.25f)

    // Background fill
    shapeRenderer.begin(ShapeType.Filled)
    shapeRenderer.setColor(bgColor)
    shapeRenderer.rectangle(-hw, -hh, ww, wh)
    shapeRenderer.end()

    // Grid lines
    shapeRenderer.begin(ShapeType.Line)
    shapeRenderer.setColor(Color(0.4f, 0.4f, 0.4f, 1f))
    val gridStep = 25f
    var gx = -hw
    while (gx <= hw) {
      shapeRenderer.line(gx, -hh, gx, hh)
      gx += gridStep
    }
    var gy = -hh
    while (gy <= hh) {
      shapeRenderer.line(-hw, gy, hw, gy)
      gy += gridStep
    }
    shapeRenderer.end()

    // Border rectangle
    shapeRenderer.begin(ShapeType.Line)
    shapeRenderer.setColor(Color.WHITE)
    shapeRenderer.line(-hw, -hh, hw, -hh)
    shapeRenderer.line(hw, -hh, hw, hh)
    shapeRenderer.line(hw, hh, -hw, hh)
    shapeRenderer.line(-hw, hh, -hw, -hh)
    shapeRenderer.end()

    // Center circle (animated radius)
    val baseRadius = scala.math.min(ww, wh) * 0.15f
    val pulse = 1f + 0.1f * MathUtils.sin(elapsed * 3f)
    shapeRenderer.begin(ShapeType.Filled)
    val circleColor = DemoUtils.hsvToColor((hue + 180f) % 360f, 0.7f, 0.9f)
    shapeRenderer.setColor(circleColor)
    shapeRenderer.circle(0f, 0f, baseRadius * pulse)
    shapeRenderer.end()

    // Corner markers (small filled squares)
    val ms = 8f // marker size
    shapeRenderer.begin(ShapeType.Filled)
    shapeRenderer.setColor(Color.RED)
    shapeRenderer.rectangle(-hw, hh - ms, ms, ms)        // top-left
    shapeRenderer.setColor(Color.GREEN)
    shapeRenderer.rectangle(hw - ms, hh - ms, ms, ms)    // top-right
    shapeRenderer.setColor(Color.BLUE)
    shapeRenderer.rectangle(-hw, -hh, ms, ms)             // bottom-left
    shapeRenderer.setColor(Color.YELLOW)
    shapeRenderer.rectangle(hw - ms, -hh, ms, ms)         // bottom-right
    shapeRenderer.end()
  }

  /** Return a distinct label color for each viewport slot. */
  private def labelColor(index: Int): Color = {
    index match {
      case 0 => Color.CYAN
      case 1 => Color.GREEN
      case 2 => Color.RED
      case 3 => Color.YELLOW
      case 4 => Color.BLUE
      case _ => Color.WHITE
    }
  }
}
