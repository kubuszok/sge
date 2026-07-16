/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-584 (BitmapFontCache 3-arg setText/addText pass halign = 0
 * with a false "Align.left = 0" comment; Align.left is 1 << 3 = 8).
 *
 * Every expected value below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/BitmapFontCache.java (original-src/libgdx,
 * commit a729bf1f0de099ebcc60562d72f008157677b559). Java line numbers cited
 * in the test comments refer to that file:
 *   - setText(str, x, y): lines 458-461 — delegates with `Align.left` (= 8)
 *   - addText(str, x, y): lines 495-497 — delegates with `Align.left` (= 8)
 * The port (BitmapFontCache.scala lines 342 and 366) passes halign = 0
 * annotated "// Align.left = 0", but sge.utils.Align.left == 8 exactly like
 * libgdx (Align.scala line 45). GlyphLayout.alignRuns (GlyphLayout.scala
 * lines 279-288, faithful to GlyphLayout.java lines 297-306 since ISS-581)
 * treats a CLEAR left bit as center/right alignment:
 *   (0 & Align.left) == 0  -> aligns;  center = (0 & Align.center) != 0 -> false
 *   -> run.x += targetWidth - run.width = 0 - run.width
 * so every default draw (font.draw(batch, str, x, y) routes through these
 * overloads) is RIGHT-aligned to x: the whole run is shifted to -run.width.
 * The gauntlet FontDrawProbe measured run.x = -34 with a real font; with the
 * fixture below the shift is -29.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import lowlevel.Nullable
import lowlevel.util.DynamicArray

class BitmapFontCacheDefaultAlignRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction (same fixture as BitmapFontCacheRedSuite) --
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

  /** Font metrics used in every trace below: capHeight=10, down=-12, scaleX=scaleY=1, padLeft=padRight=0, spaceXadvance=10. Glyph 'a': xadvance=10, width=9, xoffset=0. Layout of "aaa": single run at
    * run.y=0, xAdvances [0,10,10,9] (first entry -xoffset*scaleX-padLeft=0 from BitmapFontData.getGlyphs; last entry set to glyph width 9 by GlyphRun.setLastGlyphXAdvance). calculateWidths
    * (GlyphLayout.java lines 275-294) gives run.width = 0+10+10+9 = 29.
    */
  private def makeFont(): BitmapFont = {
    val data = new BitmapFontData()
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

  /** Page-0 vertex data. Named argument disambiguates from the no-arg `vertices` overload. */
  private def pageVerts(cache: BitmapFontCache): Array[Float] =
    cache.vertices(page = 0)

  /** x coordinate of the first (bottom-left) vertex of glyph quad `k` on page 0: each glyph emits 20 floats (4 vertices x [x,y,color,u,v]). */
  private def glyphX(cache: BitmapFontCache, k: Int): Float =
    pageVerts(cache)(k * 20)

  test("ISS-584: setText(str, x, y) defaults to Align.left — run.x stays 0 and the first glyph is cached at x = 0") {
    // Java trace (BitmapFontCache.java lines 458-461): setText(str, x, y)
    // delegates with halign = Align.left = 8, so alignRuns
    // ((8 & Align.left) == 0 is FALSE, GlyphLayout.java line 298) is a no-op
    // and run.x stays 0; the first glyph quad is cached at
    // gx = x + run.x + xAdvances[0] = 0 (addToCache, Java lines 391-404).
    // The port (BitmapFontCache.scala line 342) passes halign = 0 with the
    // false comment "Align.left = 0": (0 & 8) == 0 -> not-left branch,
    // center = (0 & 1) != 0 -> false, so run.x += 0 - run.width = -29 —
    // plain font.draw(batch, str, x, y) renders RIGHT-aligned to x.
    val font   = makeFont()
    val cache  = new BitmapFontCache(font)
    val layout = cache.setText("aaa", 0f, 0f)
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(layout.runs(0).width, 29f, 0.0001f, "fixture sanity: run.width = 0+10+10+9 (GlyphLayout.java lines 275-294)")
    assertEqualsFloat(
      layout.runs(0).x,
      0f,
      0.0001f,
      "setText(str, x, y) must behave as Align.left = 8 (BitmapFontCache.java line 460), not right-shift the run to -run.width"
    )
    assertEqualsFloat(
      glyphX(cache, 0),
      0f,
      0.0001f,
      "first glyph quad must be cached at x + run.x + xAdvances[0] = 0 (BitmapFontCache.java lines 391-404)"
    )
  }

  test("ISS-584: addText(str, x, y) defaults to Align.left — run.x stays 0") {
    // Java trace (BitmapFontCache.java lines 495-497): addText(str, x, y)
    // delegates with halign = Align.left = 8. The port
    // (BitmapFontCache.scala line 366) passes halign = 0 with the same false
    // "Align.left = 0" comment, shifting the run to run.x = -29 as above.
    val font   = makeFont()
    val cache  = new BitmapFontCache(font)
    val layout = cache.addText("aaa", 0f, 0f)
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(
      layout.runs(0).x,
      0f,
      0.0001f,
      "addText(str, x, y) must behave as Align.left = 8 (BitmapFontCache.java line 496), not right-shift the run to -run.width"
    )
  }
}
