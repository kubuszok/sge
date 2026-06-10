/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-492 (BitmapFontCache.addToCache port bugs).
 *
 * Every expected value below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/BitmapFontCache.java (original-src/libgdx,
 * commit a729bf1f0de099ebcc60562d72f008157677b559). Java line numbers cited
 * in the test comments refer to that file:
 *   - addToCache: lines 378-407
 *   - per-glyph x-advance accumulation `gx += xAdvances[ii];`: line 401
 *   - tint reset `currentTint = Color.WHITE_FLOAT_BITS;`: line 406
 *   - tint() early-out on identical tint: lines 116-119
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import lowlevel.Nullable
import lowlevel.util.DynamicArray
import sge.utils.Align

class BitmapFontCacheRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction -------------------------------------------
  //
  // Same fixture as GlyphLayoutRedSuite: BitmapFontData is built
  // programmatically (no file, no GL): the no-arg constructor skips load(),
  // and glyphs are registered AFTER the BitmapFont is constructed so that
  // BitmapFont.load(data) never calls setGlyphRegion against the texture-less
  // dummy TextureRegion. BitmapFontCache only requires regions.size >= 1,
  // which the dummy provides, and neither addToCache nor tint touches GL.

  /** Regular glyph: xoffset 0, yoffset 0, page 0, not fixed-width. */
  private def mkGlyph(ch: Char, xadvance: Int, width: Int): BitmapFont.Glyph = {
    val g = new BitmapFont.Glyph()
    g.id = ch.toInt
    g.xadvance = xadvance
    g.width = width
    g.height = 8
    g
  }

  /** Registers a glyph by writing the page array directly. BitmapFontData.setGlyph cannot be used here: data.glyphs pages are zero-length (not null) arrays, so its `Nullable(page).isEmpty` check
    * never allocates a page and indexing throws ArrayIndexOutOfBoundsException — an unrelated pre-existing bug this suite must not depend on.
    */
  private def registerGlyph(data: BitmapFontData, g: BitmapFont.Glyph): Unit = {
    val page = g.id / BitmapFont.PAGE_SIZE
    if (data.glyphs(page).length == 0) data.glyphs(page) = new Array[BitmapFont.Glyph](BitmapFont.PAGE_SIZE)
    data.glyphs(page)(g.id & (BitmapFont.PAGE_SIZE - 1)) = g
  }

  /** Font metrics used in every trace below: capHeight=10, down=-12, ascent=0 (never set; addText's `y + font.data.ascent` is a no-op), scaleX=scaleY=1, padLeft=padRight=0, spaceXadvance=10. Glyph
    * 'a': xadvance=10, width=9, xoffset=0. Layout of "aaa": single run at run.x=0, run.y=0, xAdvances [0,10,10,9] (first entry -xoffset*scaleX-padLeft=0 from BitmapFontData.getGlyphs; last entry set
    * to glyph width 9 by GlyphRun.setLastGlyphXAdvance, BitmapFont.java lines 441-445 — addToCache consumes only entries 0..2).
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
    registerGlyph(data, mkGlyph('a', 10, 9))
    font
  }

  /** Page-0 vertex data. Named argument disambiguates from the no-arg `vertices` overload, which would otherwise make `cache.vertices(i)` ambiguous. */
  private def pageVerts(cache: BitmapFontCache): Array[Float] =
    cache.vertices(page = 0)

  /** x coordinate of the first (bottom-left) vertex of glyph quad `k` on page 0: each glyph emits 20 floats (4 vertices x [x,y,color,u,v], BitmapFontCache.java lines 409-447). */
  private def glyphX(cache: BitmapFontCache, k: Int): Float =
    pageVerts(cache)(k * 20)

  // --- ISS-492 symptom 1: gx never advances ----------------------------------

  test("ISS-492: setText(\"aaa\",0,0) caches per-glyph x positions advancing by xadvance (0, 10, 20)") {
    // Java trace (addToCache, lines 391-404), x=0, run.x=0, xAdvances [0,10,10,9]:
    //   ii=0: gx += xAdvances[0] = 0  -> gx = 0;  addGlyph at x=0  (line 401-402)
    //   ii=1: gx += xAdvances[1] = 10 -> gx = 10; addGlyph at x=10
    //   ii=2: gx += xAdvances[2] = 10 -> gx = 20; addGlyph at x=20
    // addGlyph: finalX = gx + xoffset*scaleX = gx (xoffset=0), written to
    // vertices[k*20] (Java lines 409-447 / BitmapFontCache.scala addGlyph).
    // The port comments the accumulation out (`// gx += xAdvances[ii]`,
    // BitmapFontCache.scala line 412) and `gx` is a val, so every glyph of the
    // run is cached at the same x position — all three quads overlap and the
    // deltas below come out 0 instead of 10.
    // Align.left is passed explicitly (Java BitmapFontCache.setText also uses
    // Align.left == 8). Positions are asserted RELATIVE to glyph 0: the run.x
    // base offset is applied to gx once for the whole run (Java line 395), so
    // deltas isolate the per-glyph advance from an unrelated port discrepancy
    // in GlyphLayout.alignRuns (GlyphLayout.scala line 279 tests `halign & 1`
    // while sge.utils.Align.left == 8 like libgdx, shifting run.x even for
    // left-aligned text) — this test must not trip on that, fixed or not.
    val font  = makeFont()
    val cache = new BitmapFontCache(font)
    cache.setText("aaa", 0f, 0f, 0f, Align.left.toInt, false)
    assertEquals(cache.vertexCount(0), 60, "3 glyphs x 20 floats must be cached")
    val x0 = glyphX(cache, 0) // = x + run.x + xAdvances[0] (Java lines 395, 401)
    assertEqualsFloat(glyphX(cache, 1), x0 + 10f, 0.0001f, "glyph 1 must advance by xAdvances[1]=10 from glyph 0 (Java line 401)")
    assertEqualsFloat(glyphX(cache, 2), x0 + 20f, 0.0001f, "glyph 2 must advance by xAdvances[1]+xAdvances[2]=20 from glyph 0 (Java line 401)")
    assert(glyphX(cache, 0) < glyphX(cache, 1) && glyphX(cache, 1) < glyphX(cache, 2), "per-glyph x positions must be strictly increasing, not overlapping")
  }

  // --- ISS-492 symptom 2: currentTint not reset after caching new glyphs ------

  test("ISS-492: addToCache resets currentTint, so tint() with the previously-used tint still recolors new glyphs") {
    // Java semantics: addToCache ends with
    //   `currentTint = Color.WHITE_FLOAT_BITS; // Cached glyphs have changed, reset the current tint.`
    // (line 406), so after a setText the cache is "untinted" again and
    // tint(RED) — even though RED was already the last tint used — passes the
    // `if (currentTint == newTint) return;` guard (lines 118-119) and
    // recolors the fresh glyphs. The port drops the reset (commented out at
    // BitmapFontCache.scala line 419), so currentTint stays RED across
    // setText and the second tint(RED) silently no-ops, leaving the new
    // glyphs white.
    val font  = makeFont()
    val cache = new BitmapFontCache(font)
    val white = Color.WHITE.toFloatBits()
    val red   = Color.RED.toFloatBits() // layout color WHITE mul RED = RED (tint(), Java lines 137-139)

    cache.setText("aa", 0f, 0f)
    assertEquals(pageVerts(cache)(2), white, "precondition: fresh glyphs are cached with the layout color WHITE (Java lines 397-400)")
    cache.tint(Color.RED)
    assertEquals(pageVerts(cache)(2), red, "precondition: the first tint(RED) recolors the cache (Java lines 116-145)")

    // Re-cache: Java line 406 resets currentTint to WHITE_FLOAT_BITS here.
    cache.setText("aa", 0f, 0f)
    assertEquals(pageVerts(cache)(2), white, "precondition: re-cached glyphs start white again (Java lines 397-400)")

    cache.tint(Color.RED)
    assertEquals(pageVerts(cache)(2), red, "tint(RED) after re-caching must recolor the new glyphs: addToCache resets currentTint to WHITE_FLOAT_BITS (Java line 406)")
  }
}
