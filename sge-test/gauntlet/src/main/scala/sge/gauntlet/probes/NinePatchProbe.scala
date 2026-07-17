/*
 * SGE Gauntlet — g2d: NinePatch stretch (borders fixed, center stretches). ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ Pixmap, Texture }
import sge.graphics.Texture.TextureFilter
import sge.graphics.g2d.NinePatch

import scala.collection.mutable.ListBuffer

/** Cuts a 24x24 texture (red border ring, green 8x8 center) into a 3x3 NinePatch with 8px splits and draws it at 240x240 — 10x its natural size. Asserts the defining NinePatch invariant: the four
  * corner/edge borders keep their natural 8px size (they do NOT scale), while the center patch stretches to fill the interior. Pixel asserts at border, just-inside-border, and center coordinates
  * prove both halves of the invariant (a scaled border would paint red 80px in from the edge; the probe checks that pixel is green).
  */
object NinePatchProbe extends FeatureProbe {

  override def id: String = "g2d/ninepatch"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 1

  private val Red   = (0xdd, 0x22, 0x22)
  private val Green = (0x22, 0xcc, 0x22)

  // Draw rectangle (world coords, y up) — 10x the 24px natural size.
  private val X0 = 200
  private val Y0 = 200
  private val W  = 240
  private val H  = 240

  private val checks = ListBuffer.empty[Check]

  private var texture: Option[Texture]   = None
  private var patch:   Option[NinePatch] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    given Sge  = ctx.sgeCtx
    val pixmap = Pixmap(24, 24, Pixmap.Format.RGBA8888)
    pixmap.setColor(0xdd2222ff) // red border ring
    pixmap.fill()
    pixmap.setColor(0x22cc22ff) // green 8x8 center
    pixmap.fillRectangle(8, 8, 8, 8)
    val tex = Texture(pixmap)
    pixmap.close()
    tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest) // crisp region boundaries
    texture = Some(tex)
    patch = Some(new NinePatch(tex, 8, 8, 8, 8))
    ctx.clear(0f, 0f, 0f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    patch.foreach { p =>
      val batch = ctx.batch
      batch.rendering {
        p.draw(batch, X0.toFloat, Y0.toFloat, W.toFloat, H.toFloat)
      }
    }

  private def near(actual: Int, expected: Int, tol: Int = 24): Boolean =
    Math.abs(actual - expected) <= tol

  private def isColor(x: Int, y: Int, rgb: (Int, Int, Int))(using ProbeContext): Boolean = {
    val ctx          = summon[ProbeContext]
    val (r, g, b, _) = ctx.pixelRgba(x, y)
    near(r, rgb._1) && near(g, rgb._2) && near(b, rgb._3)
  }

  private def sample(name: String, x: Int, y: Int, rgb: (Int, Int, Int), label: String)(using ctx: ProbeContext): Unit = {
    val (r, g, b, _) = ctx.pixelRgba(x, y)
    checks += Check.cond(name, isColor(x, y, rgb), s"$label at ($x,$y)", s"RGB($r,$g,$b)")
  }

  override def verify(ctx: ProbeContext): List[Check] = {
    given ProbeContext = ctx
    val cy             = Y0 + H / 2 // 320
    val cx             = X0 + W / 2 // 320

    // Borders keep their natural 8px width — sampled 4px in from each edge.
    sample("left-border-red", X0 + 4, cy, Red, "red left border")
    sample("right-border-red", X0 + W - 4, cy, Red, "red right border")
    sample("bottom-border-red", cx, Y0 + 4, Red, "red bottom border")
    sample("top-border-red", cx, Y0 + H - 4, Red, "red top border")
    sample("corner-red", X0 + 4, Y0 + 4, Red, "red bottom-left corner")

    // Center patch stretched across the interior.
    sample("center-green", cx, cy, Green, "green stretched center")

    // Border did NOT scale: 40px in from the left edge is already the stretched
    // center (a 10x-scaled 8px border would still be red out to 80px).
    sample("border-not-scaled", X0 + 40, cy, Green, "green (border not stretched)")
    // Just past the 8px border (12px in) is center, not border.
    sample("center-starts-after-border", X0 + 12, cy, Green, "green just past 8px border")

    // Untouched background.
    sample("background-black", 20, 20, (0, 0, 0), "black background")

    patch = None
    texture.foreach(_.close())
    texture = None
    checks.toList
  }
}
