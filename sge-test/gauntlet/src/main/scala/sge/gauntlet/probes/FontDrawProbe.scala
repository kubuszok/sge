/*
 * SGE Gauntlet — g2d: BitmapFont.draw left-alignment (ISS-584 re-verification).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.g2d.{ BitmapFont, GlyphLayout }

import scala.collection.mutable.ListBuffer

/** Draws text with the plain 4-arg `font.draw(batch, str, x, y)` and asserts left alignment two ways: the returned GlyphLayout's first run must start at x=0 (relative to the anchor), and all ink
  * pixels must land at or right of the anchor.
  *
  * knownIssue ISS-584: the 3-arg BitmapFontCache.setText/addText overloads pass `halign = 0` (with a false "Align.left = 0" comment; Align.left is 1&lt;&lt;3), and `GlyphLayout.alignRuns` treats a
  * cleared left bit as right-align — so plain draws render right-aligned at x.
  */
object FontDrawProbe extends FeatureProbe {

  override def id: String = "g2d/font-default-draw"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def knownIssue: Option[String] = Some("ISS-584")

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  private var font:   Option[BitmapFont]  = None
  private var layout: Option[GlyphLayout] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    layout = None
    given Sge = ctx.sgeCtx
    val f = GauntletFont.create()
    f.data.setScale(2f)
    font = Some(f)
    ctx.clear(0f, 0f, 0f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    font.foreach { f =>
      val batch = ctx.batch
      batch.rendering {
        layout = Some(f.draw(batch, "HHH", 100f, 400f))
      }
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    layout match {
      case Some(l) =>
        checks += Check.cond("layout-has-runs", l.runs.size > 0, "at least one glyph run", s"${l.runs.size} runs")
        if (l.runs.size > 0) {
          val run = l.runs(0)
          checks += Check.eq("left-aligned-run-x", 0f, run.x)
        }
      case None =>
        checks += Check.cond("layout-has-runs", passed = false, "layout returned by draw", "no layout")
    }
    // Ink placement: every ink pixel of "HHH" must be at or right of the x=100 anchor.
    // (Right-aligned rendering — the ISS-584 failure mode — puts all ink left of the anchor.)
    var inkRight = 0
    var inkLeft  = 0
    var y        = 378
    while (y <= 404) {
      var x = 40
      while (x <= 180) {
        val (r, g, b, _) = ctx.pixelRgba(x, y)
        if (r > 200 && g > 200 && b > 200) {
          if (x >= 100) inkRight += 1 else inkLeft += 1
        }
        x += 1
      }
      y += 1
    }
    checks += Check.cond("ink-right-of-anchor", inkRight > 20, "> 20 ink pixels at x >= 100", s"$inkRight ink pixels")
    checks += Check.eq("no-ink-left-of-anchor", 0, inkLeft)
    ctx.log(s"ink pixels: right=$inkRight left=$inkLeft")
    font.foreach { f =>
      f.region.texture.close()
      f.close()
    }
    font = None
    checks.toList
  }
}
