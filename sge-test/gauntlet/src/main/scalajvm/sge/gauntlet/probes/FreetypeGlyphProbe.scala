/*
 * SGE Gauntlet — text: FreeType glyph generation from a real TTF. ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.g2d.BitmapFont
import sge.graphics.g2d.freetype.FreeTypeFontGenerator

import scala.collection.mutable.ListBuffer

/** Generates a small BitmapFont from a real TrueType font via [[FreeTypeFontGenerator]] (real freetype natives, provided on the JVM row) and asserts the generated metrics and a rendered glyph.
  *
  * The engine ships no bundled TTF fixture, and authoring a valid TrueType binary in-repo is infeasible, so the probe locates an OUTLINE TTF from the host's well-known system font directories (macOS
  * / Linux / Windows). It tries candidates in order and keeps the first that actually rasterizes crisp outline glyphs — this skips embedded-bitmap fonts (e.g. macOS Geneva), whose fixed-size bitmaps
  * do not render at an arbitrary requested pixel size. If no usable font is found the `outline-ttf-usable` check fails honestly — the probe never fabricates a pass. requiresGpu because glyph packing
  * creates GL textures; in headless CI it reports `skipped_gpu`.
  */
object FreetypeGlyphProbe extends FeatureProbe {

  override def id: String = "text/freetype-glyph"

  override def area: String = "text"

  override def requiresGpu: Boolean = true

  override def frames: Int = 1

  /** Well-known system OUTLINE TTF locations across the desktop platforms the gauntlet runs on. */
  private val Candidates: List[String] = List(
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/Supplemental/Times New Roman.ttf",
    "/System/Library/Fonts/NewYork.ttf",
    "/Library/Fonts/Arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    "/usr/share/fonts/TTF/DejaVuSans.ttf",
    "/usr/share/fonts/dejavu/DejaVuSans.ttf",
    "C:\\Windows\\Fonts\\arial.ttf",
    "C:\\Windows\\Fonts\\segoeui.ttf"
  )

  private val FontSize = 40
  private val Anchor   = (640, 400)

  private val checks = ListBuffer.empty[Check]

  private var font:   Option[BitmapFont] = None
  private var chosen: String             = "<none>"

  /** True if `f` rasterized real outline glyphs (non-empty cap height and a non-empty 'A'). */
  private def usable(f: BitmapFont): Boolean =
    f.data.capHeight > 0f && f.data.getGlyph('A').exists(g => g.width > 0 && g.height > 0)

  /** Tries each existing candidate; keeps the first font that rasterizes crisp outline glyphs. */
  private def loadUsableFont(ctx: ProbeContext): Option[BitmapFont] = {
    given Sge = ctx.sgeCtx
    var result: Option[BitmapFont] = None
    val it = Candidates.iterator
    while (result.isEmpty && it.hasNext) {
      val path = it.next()
      if (new java.io.File(path).isFile) {
        try {
          val gen   = new FreeTypeFontGenerator(ctx.sgeCtx.files.absolute(path))
          val param = new FreeTypeFontGenerator.FreeTypeFontParameter()
          param.size = FontSize
          param.characters = "AaBbGgXxYy"
          val f = gen.generateFont(param)
          gen.close()
          if (usable(f)) {
            chosen = path
            result = Some(f)
          } else {
            ctx.log(s"skipping non-outline/empty font: $path")
            f.region.texture.close()
            f.close()
          }
        } catch {
          case scala.util.control.NonFatal(e) =>
            ctx.log(s"skipping unusable font $path: $e")
        }
      }
    }
    result
  }

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    font = None
    chosen = "<none>"
    ctx.clear(0f, 0f, 0f, 1f)
    font = loadUsableFont(ctx)
    checks += Check.cond("outline-ttf-usable", font.isDefined, "a usable outline .ttf on disk", chosen)
    if (font.isDefined) ctx.log(s"freetype source ttf: $chosen")
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    font.foreach { f =>
      val batch = ctx.batch
      batch.rendering {
        val _ = f.draw(batch, "A", Anchor._1.toFloat, Anchor._2.toFloat)
      }
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    font.foreach { f =>
      checks += Check.cond("line-height-positive", f.data.lineHeight > 0f, "generated lineHeight > 0", f.data.lineHeight.toString)
      checks += Check.cond("cap-height-positive", f.data.capHeight > 0f, "generated capHeight > 0", f.data.capHeight.toString)
      val glyphA = f.data.getGlyph('A')
      checks += Check.cond(
        "glyph-A-generated",
        glyphA.exists(g => g.width > 0 && g.height > 0),
        "glyph 'A' with width>0 & height>0",
        glyphA.fold("<missing>")(g => s"${g.width}x${g.height}")
      )
      // Rendered ink: scan a window around the anchor (wide enough to tolerate the plain-draw
      // horizontal alignment) for white glyph pixels.
      var ink = 0
      var y   = 350
      while (y <= 440) {
        var x = 560
        while (x <= 720) {
          val (r, g, b, _) = ctx.pixelRgba(x, y)
          if (r > 150 && g > 150 && b > 150) ink += 1
          x += 2
        }
        y += 1
      }
      ctx.log(s"freetype rendered ink pixels: $ink")
      checks += Check.cond("glyph-renders-ink", ink > 20, "> 20 white glyph pixels near anchor", ink.toString)
      f.region.texture.close()
      f.close()
    }
    font = None
    checks.toList
  }
}
