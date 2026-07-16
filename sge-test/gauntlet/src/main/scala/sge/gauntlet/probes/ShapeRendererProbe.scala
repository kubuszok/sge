/*
 * SGE Gauntlet — g2d: ShapeRenderer filled primitives with pixel asserts.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.glutils.ShapeRenderer.ShapeType

import scala.collection.mutable.ListBuffer

/** Draws a filled rectangle and a filled circle with the ShapeRenderer and asserts pixels inside, on, and outside the primitives. */
object ShapeRendererProbe extends FeatureProbe {

  override def id: String = "g2d/shaperenderer"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    ctx.clear(0f, 0f, 0f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val shapes = ctx.shapes
    shapes.drawing(ShapeType.Filled) {
      shapes.setColor(1f, 1f, 0f, 1f) // yellow rectangle
      shapes.rectangle(50f, 50f, 100f, 100f)
      shapes.setColor(0f, 1f, 1f, 1f) // cyan circle
      shapes.circle(400f, 300f, 50f)
    }
  }

  private def near(actual: Int, expected: Int, tolerance: Int = 12): Boolean =
    Math.abs(actual - expected) <= tolerance

  override def verify(ctx: ProbeContext): List[Check] = {
    val (rr, rg, rb, _) = ctx.pixelRgba(100, 100) // inside the rectangle
    checks += Check.cond("rectangle-pixel", near(rr, 255) && near(rg, 255) && near(rb, 0), "RGB(255,255,0) +-12 at (100,100)", s"RGB($rr,$rg,$rb)")
    val (cr, cg, cb, _) = ctx.pixelRgba(400, 300) // circle center
    checks += Check.cond("circle-pixel", near(cr, 0) && near(cg, 255) && near(cb, 255), "RGB(0,255,255) +-12 at (400,300)", s"RGB($cr,$cg,$cb)")
    val (or_, og, ob, _) = ctx.pixelRgba(700, 600) // outside both primitives
    checks += Check.cond("outside-pixel", near(or_, 0) && near(og, 0) && near(ob, 0), "RGB(0,0,0) +-12 at (700,600)", s"RGB($or_,$og,$ob)")
    val (er, eg, eb, _) = ctx.pixelRgba(460, 300) // just outside the circle's right edge
    checks += Check.cond("circle-edge-outside", near(er, 0) && near(eg, 0) && near(eb, 0), "RGB(0,0,0) +-12 at (460,300)", s"RGB($er,$eg,$eb)")
    checks.toList
  }
}
