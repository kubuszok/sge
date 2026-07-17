/*
 * SGE Gauntlet — viewport: FitViewport letterboxing (pillarbox bars + centered content). ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.utils.viewport.FitViewport

import scala.collection.mutable.ListBuffer

/** Sets up a 400x300 (4:3) [[FitViewport]] on the fixed 1280x720 (16:9) target. Scaling.fit scales the world by 2.4x to 960x720 and centers it, so 160px black pillarbox bars appear left and right
  * while the content fills the full height. Asserts the computed gutter geometry (state) and the rendered result (pixels): bars are black at the expected x, content is drawn at the center and reaches
  * top and bottom, and a 10px-scaled border does not bleed into the gutters.
  */
object ViewportLetterboxProbe extends FeatureProbe {

  override def id: String = "g2d/viewport-letterbox"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 1

  private val WorldW = 400
  private val WorldH = 300

  // Expected fit geometry for a 400x300 world in a 1280x720 target.
  private val ExpVpW    = 960
  private val ExpVpH    = 720
  private val ExpGutter = 160

  private val checks = ListBuffer.empty[Check]

  private var viewport: Option[FitViewport] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    given Sge = ctx.sgeCtx
    val vp    = new FitViewport(WorldUnits(WorldW.toFloat), WorldUnits(WorldH.toFloat))
    vp.update(Pixels(ctx.width), Pixels(ctx.height), true) // centerCamera so world [0..W]x[0..H] fills the viewport
    viewport = Some(vp)
    ctx.clear(0f, 0f, 0f, 1f) // black bars
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    viewport.foreach { vp =>
      val shapes = ctx.shapes
      val gl     = ctx.sgeCtx.graphics.gl20
      // Set the GL viewport directly in FBO pixels to the centered sub-rectangle computed by the
      // FitViewport. We bypass Viewport.apply()/HdpiUtils here: on a HiDPI display HdpiUtils scales
      // logical coords up to the backbuffer, but the gauntlet renders into a fixed 1280x720 FBO
      // (backbuffer-independent), so the raw computed bounds are already the correct FBO pixels.
      gl.glViewport(vp.screenX, vp.screenY, vp.screenWidth, vp.screenHeight)
      shapes.setProjectionMatrix(vp.camera.combined)
      shapes.drawing(ShapeType.Filled) {
        shapes.setColor(0.2f, 0.4f, 1f, 1f) // blue world content fills the whole world rect
        shapes.rectangle(0f, 0f, WorldW.toFloat, WorldH.toFloat)
      }
      // Restore the full-target viewport and the harness projection for the screenshot and later probes.
      gl.glViewport(Pixels(0), Pixels(0), Pixels(ctx.width), Pixels(ctx.height))
      shapes.setProjectionMatrix(ctx.camera.combined)
    }

  private def near(actual: Int, expected: Int, tol: Int = 12): Boolean =
    Math.abs(actual - expected) <= tol

  private def isBlue(x: Int, y: Int)(using ctx: ProbeContext): Boolean = {
    val (r, g, b, _) = ctx.pixelRgba(x, y)
    near(r, 51) && near(g, 102) && near(b, 255)
  }

  private def isBlack(x: Int, y: Int)(using ctx: ProbeContext): Boolean = {
    val (r, g, b, _) = ctx.pixelRgba(x, y)
    near(r, 0) && near(g, 0) && near(b, 0)
  }

  override def verify(ctx: ProbeContext): List[Check] = {
    given ProbeContext = ctx
    viewport match {
      case Some(vp) =>
        // Computed fit geometry (state asserts).
        checks += Check.eq("viewport-width", ExpVpW, vp.screenWidth.toInt)
        checks += Check.eq("viewport-height", ExpVpH, vp.screenHeight.toInt)
        checks += Check.eq("left-gutter-width", ExpGutter, vp.leftGutterWidth.toInt)
        checks += Check.eq("right-gutter-width", ExpGutter, vp.rightGutterWidth.toInt)
        checks += Check.eq("bottom-gutter-height", 0, vp.bottomGutterHeight.toInt)
      case None =>
        checks += Check.cond("viewport-created", passed = false, "FitViewport created", "none")
    }

    // Rendered result (pixel asserts).
    checks += Check.cond("content-centered-blue", isBlue(640, 360), "blue at target center (640,360)", ctx.pixelRgba(640, 360).toString)
    checks += Check.cond("left-bar-black", isBlack(80, 360), "black left bar at (80,360)", ctx.pixelRgba(80, 360).toString)
    checks += Check.cond("right-bar-black", isBlack(1200, 360), "black right bar at (1200,360)", ctx.pixelRgba(1200, 360).toString)
    // Content edge lands at the gutter boundary (~x=160): inside is blue, just outside is black.
    checks += Check.cond("content-inside-left-edge", isBlue(175, 360), "blue just inside content (175,360)", ctx.pixelRgba(175, 360).toString)
    checks += Check.cond("gutter-outside-content", isBlack(150, 360), "black just outside content (150,360)", ctx.pixelRgba(150, 360).toString)
    // Pillarbox (not letterbox): content fills the full height.
    checks += Check.cond("content-reaches-bottom", isBlue(640, 10), "blue near bottom (640,10)", ctx.pixelRgba(640, 10).toString)
    checks += Check.cond("content-reaches-top", isBlue(640, 710), "blue near top (640,710)", ctx.pixelRgba(640, 710).toString)

    viewport = None
    checks.toList
  }
}
