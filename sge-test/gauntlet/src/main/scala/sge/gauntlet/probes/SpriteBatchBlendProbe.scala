/*
 * SGE Gauntlet — g2d: SpriteBatch colored quads, blend modes, flush count.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ GL20, Pixmap, Texture }

import scala.collection.mutable.ListBuffer

/** Draws colored quads with the default and additive blend modes and asserts exact pixels at known coordinates plus the flush count (a blend-function change must force a flush). */
object SpriteBatchBlendProbe extends FeatureProbe {

  override def id: String = "g2d/spritebatch-blend"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  private var white: Option[Texture] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    given Sge  = ctx.sgeCtx
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(0xffffffff)
    pixmap.fill()
    white = Some(Texture(pixmap))
    pixmap.close()
    ctx.clear(0f, 0f, 0f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    white.foreach { tex =>
      val batch = ctx.batch
      batch.rendering {
        // default blend (SRC_ALPHA, ONE_MINUS_SRC_ALPHA): opaque red quad
        batch.setColor(1f, 0f, 0f, 1f)
        batch.draw(tex, 100f, 100f, 50f, 50f)
        // half-alpha green over black -> (0, ~127, 0)
        batch.setColor(0f, 1f, 0f, 0.5f)
        batch.draw(tex, 200f, 100f, 50f, 50f)
        // additive blend: blue over the red quad -> magenta. The blend switch must flush.
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE)
        batch.setColor(0f, 0f, 1f, 1f)
        batch.draw(tex, 100f, 100f, 50f, 50f)
      }
      checks += Check.eq("flush-count", 2, batch.renderCalls)
      // restore the default blend function for subsequent probes
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
      batch.setColor(1f, 1f, 1f, 1f)
    }

  private def near(actual: Int, expected: Int, tolerance: Int = 12): Boolean =
    Math.abs(actual - expected) <= tolerance

  override def verify(ctx: ProbeContext): List[Check] = {
    val (ar, ag, ab, _) = ctx.pixelRgba(125, 125) // red + additive blue = magenta
    checks += Check.cond(
      "additive-blend-pixel",
      near(ar, 255) && near(ag, 0) && near(ab, 255),
      "RGB(255,0,255) +-12 at (125,125)",
      s"RGB($ar,$ag,$ab)"
    )
    val (br, bg, bb, _) = ctx.pixelRgba(225, 125) // half-alpha green over black
    checks += Check.cond(
      "alpha-blend-pixel",
      near(br, 0) && near(bg, 127) && near(bb, 0),
      "RGB(0,127,0) +-12 at (225,125)",
      s"RGB($br,$bg,$bb)"
    )
    val (cr, cg, cb, _) = ctx.pixelRgba(20, 20) // untouched background
    checks += Check.cond("background-pixel", near(cr, 0) && near(cg, 0) && near(cb, 0), "RGB(0,0,0) +-12 at (20,20)", s"RGB($cr,$cg,$cb)")
    white.foreach(_.close())
    white = None
    checks.toList
  }
}
