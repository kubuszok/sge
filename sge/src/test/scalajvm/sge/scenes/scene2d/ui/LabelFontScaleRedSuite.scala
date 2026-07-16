/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-757 (Label font-scale machinery assigns
 * font.data.scaleX/scaleY directly instead of calling
 * BitmapFontData.setScale, so lineHeight/capHeight/down/descent never
 * rescale; Skin.scaledSize has the same bug).
 *
 * Every expected value below is derived by hand-tracing the original
 * com/badlogic/gdx/scenes/scene2d/ui/Label.java (original-src/libgdx, commit
 * a729bf1f0de099ebcc60562d72f008157677b559). Java line numbers cited in the
 * test comments refer to that file:
 *   - scaleAndComputePrefSize: line 145 `if (fontScaleChanged)
 *     font.getData().setScale(fontScaleX, fontScaleY);`, line 149 restores
 *     via setScale(oldScaleX, oldScaleY)
 *   - layout(): lines 171 and 225 — the same setScale pair
 * BitmapFont.java setScale (mirrored at BitmapFont.scala lines 710-728)
 * multiplies lineHeight, spaceXadvance, xHeight, capHeight, ascent, descent,
 * down and the paddings by the scale ratio — that is what makes line metrics
 * follow the glyph scale. The port (Label.scala lines 155-158, 162-165,
 * 188-191, 249-252) instead assigns font.data.scaleX/scaleY DIRECTLY, so
 * only per-glyph x-advances scale while every vertical metric stays at the
 * unscaled value (Skin.scala lines 227-231 repeats the bug for scaledSize,
 * vs Skin.java line 581 `font.getData().setScale(scaledSize /
 * font.getCapHeight())`).
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package scenes
package scene2d
package ui

import lowlevel.Nullable
import lowlevel.util.DynamicArray
import sge.graphics.g2d.{ BitmapFont, BitmapFontData, GlyphLayout, TextureRegion }

class LabelFontScaleRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction (same fixture as the g2d red suites) ------
  //
  // BitmapFontData is built programmatically (no file, no GL): the no-arg
  // constructor skips load(), and glyphs are registered AFTER the BitmapFont
  // is constructed so that BitmapFont.load(data) never calls setGlyphRegion
  // against the texture-less dummy TextureRegion.

  /** Regular glyph: xoffset 0, yoffset 0, page 0, not fixed-width. */
  private def mkGlyph(ch: Char, xadvance: Int, width: Int): BitmapFont.Glyph = {
    val g = new BitmapFont.Glyph()
    g.id = ch.toInt
    g.xadvance = xadvance
    g.width = width
    g.height = 8
    g
  }

  /** Font metrics used in every trace below: lineHeight=15, capHeight=10, down=-12, descent=0, scaleX=scaleY=1, spaceXadvance=10. Glyph 'a': xadvance=10, width=9. For the two-line text "a\na"
    * GlyphLayout gives height = capHeight + |down| (one newline: y += down, then height = capHeight + Math.abs(y)) = 10 + 12 = 22 unscaled; after setScale(2, 2) capHeight = 20 and down = -24, so
    * height = 44.
    */
  private def makeFont(): BitmapFont = {
    val data = new BitmapFontData()
    data.lineHeight = 15f
    data.capHeight = 10f
    data.down = -12f
    data.spaceXadvance = 10f
    val regions = DynamicArray[TextureRegion]()
    regions.add(new TextureRegion())
    val font = new BitmapFont(data, Nullable(regions), true)
    // Register glyphs after the BitmapFont constructor ran (see note above).
    data.setGlyph('a'.toInt, mkGlyph('a', 10, 9))
    font
  }

  private def makeLabel(text: String, font: BitmapFont): Label =
    new Label(Nullable[CharSequence](text), new Label.LabelStyle(font, Nullable.empty))

  /** Label subclass observing the font metrics that are in effect WHILE the pref size is computed — i.e. between the scale-apply (Label.java line 145) and the scale-restore (line 149). */
  final private class MetricsProbeLabel(text: String, font: BitmapFont)(using Sge) extends Label(Nullable[CharSequence](text), new Label.LabelStyle(font, Nullable.empty)) {
    var observedLineHeight: Float = Float.NaN
    var observedCapHeight:  Float = Float.NaN

    override protected def computePrefSize(layout: GlyphLayout): Unit = {
      observedLineHeight = style.font.data.lineHeight
      observedCapHeight = style.font.data.capHeight
      super.computePrefSize(layout)
    }
  }

  test("ISS-757: setFontScale(2) rescales lineHeight/capHeight via BitmapFontData.setScale during pref-size computation") {
    // Java: scaleAndComputePrefSize (Label.java lines 141-150) calls
    // font.getData().setScale(fontScaleX, fontScaleY) (line 145), which
    // multiplies lineHeight 15 -> 30 and capHeight 10 -> 20 (BitmapFont.java
    // setScale). The port assigns font.data.scaleX/scaleY = 2 directly
    // (Label.scala lines 155-158), so during computePrefSize the observed
    // lineHeight stays 15 and capHeight stays 10.
    val font  = makeFont()
    val label = new MetricsProbeLabel("a\na", font)
    label.setFontScale(2f)
    val _ = label.prefHeight // forces scaleAndComputePrefSize with the new scale
    assertEqualsFloat(
      label.observedLineHeight,
      30f,
      0.0001f,
      "setFontScale(2) must rescale font.data.lineHeight 15 -> 30 during pref-size computation (Label.java line 145 -> BitmapFontData.setScale)"
    )
    assertEqualsFloat(
      label.observedCapHeight,
      20f,
      0.0001f,
      "setFontScale(2) must rescale font.data.capHeight 10 -> 20 during pref-size computation (Label.java line 145 -> BitmapFontData.setScale)"
    )
  }

  test("ISS-757: two-line Label prefHeight doubles with setFontScale(2)") {
    // Unscaled: prefHeight("a\na") = capHeight + |down| = 22 (descent = 0, no
    // background). With fontScale 2 the original computes the layout against
    // capHeight = 20 / down = -24 (setScale, Label.java line 145) ->
    // prefHeight = 44. The port leaves both metrics unscaled -> stays 22.
    val font  = makeFont()
    val label = makeLabel("a\na", font)
    assertEqualsFloat(label.prefHeight, 22f, 0.0001f, "fixture sanity: unscaled prefHeight = capHeight + |down| = 22")
    label.setFontScale(2f)
    assertEqualsFloat(
      label.prefHeight,
      44f,
      0.0001f,
      "setFontScale(2) must double the vertical metrics feeding prefHeight (Label.java lines 145/149, BitmapFont.java setScale)"
    )
  }
}
