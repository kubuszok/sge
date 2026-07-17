/*
 * SGE Gauntlet — scene2d: Label.setFontScale line-height metrics (ISS-757 B.2 regression). ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.Color
import sge.scenes.scene2d.ui.Label
import sge.utils.Align

import scala.collection.mutable.ListBuffer

/** Renders a 3-line Label at font scale 1 then scale 2 and verifies that doubling the font scale doubles the vertical size of the text block — both as reported metric (`prefHeight`) and as measured
  * rendered ink height. Post-ISS-757: `setFontScale` must scale line height, so the scale-2 block is ~2x the scale-1 block. A regression that ignores the scale (or scales glyphs but not line spacing)
  * fails the ratio checks.
  */
object LabelFontScaleProbe extends FeatureProbe {

  override def id: String = "scene2d/label-fontscale"

  override def area: String = "scene2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 2

  private val Text = "HH\nHH\nHH"

  // Scan window (world coords, y up) around the horizontally-centered "HH" text block. Narrow in x and
  // sampled every pixel so the thin 1px strokes of the small scale-1 glyphs are not stepped over.
  private val ScanXLo = 606
  private val ScanXHi = 674
  private val ScanYLo = 300
  private val ScanYHi = 420

  private val checks = ListBuffer.empty[Check]

  private var label: Option[Label]                       = None
  private var font:  Option[sge.graphics.g2d.BitmapFont] = None

  private var prefH1 = 0f
  private var prefH2 = 0f
  private var inkH1  = 0
  private var inkH2  = 0

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    prefH1 = 0f; prefH2 = 0f; inkH1 = 0; inkH2 = 0
    given Sge = ctx.sgeCtx
    val f     = GauntletFont.create()
    font = Some(f)
    val style = new Label.LabelStyle(f, lowlevel.Nullable(Color(Color.WHITE)))
    val l     = new Label(lowlevel.Nullable(Text: CharSequence), style)
    l.setAlignment(Align.center)
    l.setBounds(0f, 0f, ctx.width.toFloat, ctx.height.toFloat)
    label = Some(l)
    ctx.clear(0f, 0f, 0f, 1f)
  }

  /** Vertical ink extent (maxY - minY + 1) of white pixels in the scan window; 0 if none. */
  private def measureInkHeight(ctx: ProbeContext): Int = {
    var minY = Int.MaxValue
    var maxY = Int.MinValue
    var y    = ScanYLo
    while (y <= ScanYHi) {
      var x      = ScanXLo
      var rowInk = false
      while (x <= ScanXHi && !rowInk) {
        val (r, g, b, _) = ctx.pixelRgba(x, y)
        if (r > 180 && g > 180 && b > 180) rowInk = true
        x += 1
      }
      if (rowInk) {
        if (y < minY) minY = y
        if (y > maxY) maxY = y
      }
      y += 1
    }
    if (maxY >= minY) maxY - minY + 1 else 0
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    label.foreach { l =>
      val scale = if (frame == 1) 1f else 2f
      l.setFontScale(scale)
      l.validate()
      if (frame == 1) prefH1 = l.prefHeight else prefH2 = l.prefHeight
      ctx.clear(0f, 0f, 0f, 1f)
      val batch = ctx.batch
      batch.rendering {
        l.draw(batch, 1f)
      }
      val h = measureInkHeight(ctx)
      if (frame == 1) inkH1 = h else inkH2 = h
      ctx.log(s"frame $frame scale=$scale prefHeight=${l.prefHeight} inkHeight=$h")
    }

  private def near(actual: Float, expected: Float, tol: Float): Boolean =
    Math.abs(actual - expected) <= tol

  override def verify(ctx: ProbeContext): List[Check] = {
    // Metric: prefHeight doubles with scale 2.
    checks += Check.cond("pref-height-scale1-positive", prefH1 > 0f, "prefHeight(scale 1) > 0", prefH1.toString)
    checks += Check.cond(
      "pref-height-doubles",
      prefH1 > 0f && near(prefH2, 2f * prefH1, 0.5f + 0.1f * prefH1),
      s"prefHeight(scale 2) ~ 2x ${prefH1} = ${2f * prefH1}",
      prefH2.toString
    )
    // Rendered: ink block height doubles with scale 2.
    checks += Check.cond("ink-height-scale1-positive", inkH1 > 4, "> 4 ink rows at scale 1", inkH1.toString)
    checks += Check.cond("ink-height-scale2-positive", inkH2 > 8, "> 8 ink rows at scale 2", inkH2.toString)
    checks += Check.cond(
      "ink-height-doubles",
      inkH1 > 0 && near(inkH2.toFloat, 2f * inkH1, 0.35f * inkH1 + 2f),
      s"inkHeight(scale 2) ~ 2x $inkH1 = ${2 * inkH1}",
      inkH2.toString
    )
    label = None
    font.foreach { f =>
      f.region.texture.close()
      f.close()
    }
    font = None
    checks.toList
  }
}
