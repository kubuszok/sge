/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-581 (GlyphLayout.alignRuns tests the wrong Align bits;
 * setText(font, str) passes halign = 0, which is NOT Align.left).
 *
 * Every expected value below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/GlyphLayout.java (original-src/libgdx, commit
 * a729bf1f0de099ebcc60562d72f008157677b559). Java line numbers cited in the
 * test comments refer to that file.
 *
 * Align bit constants — identical in sge (sge/src/main/scala/sge/utils/
 * Align.scala lines 42-46) and libgdx (com/badlogic/gdx/utils/Align.java):
 *   center = 1 << 0 = 1
 *   top    = 1 << 1 = 2
 *   bottom = 1 << 2 = 4
 *   left   = 1 << 3 = 8
 *   right  = 1 << 4 = 16
 * Java alignRuns (lines 297-306) tests `(halign & Align.left) == 0` (line
 * 298, left = 8) and `boolean center = (halign & Align.center) != 0` (line
 * 299, center = 1), then `run.x += center ? 0.5f * (targetWidth - run.width)
 * : targetWidth - run.width` (line 303). The port instead tests
 * `(halign & 1) == 0` and `(halign & 2) != 0` — it reads the CENTER bit where
 * Java reads the LEFT bit and the TOP bit where Java reads the CENTER bit, so
 * Align.left right-aligns, Align.center is a no-op, and Align.topRight
 * centers. The 2-arg setText passes 0 (no bits set) instead of Java's
 * Align.left (line 95), so plain setText shifts every run to -run.width.
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

class GlyphLayoutAlignRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction (same fixture as GlyphLayoutRedSuite) -----
  //
  // BitmapFontData is built programmatically (no file, no GL): the no-arg
  // constructor skips load(), and glyphs are registered with registerGlyph
  // AFTER the BitmapFont is constructed so that BitmapFont.load(data) never
  // calls setGlyphRegion against the texture-less dummy TextureRegion.

  /** All glyphs used below: xadvance 10, width 9, xoffset 0, not fixed-width. */
  private def mkGlyph(ch: Char, xadvance: Int, width: Int): BitmapFont.Glyph = {
    val g = new BitmapFont.Glyph()
    g.id = ch.toInt
    g.xadvance = xadvance
    g.width = width
    g.height = 8
    g
  }

  /** Registers a glyph via the public BitmapFontData.setGlyph API (ISS-579 fixed: glyph pages start null and are allocated lazily, matching libGDX). */
  private def registerGlyph(data: BitmapFontData, g: BitmapFont.Glyph): Unit =
    data.setGlyph(g.id, g)

  /** Font metrics used in every trace below: capHeight=10, down=-12, scaleX=1, padLeft=padRight=0, spaceXadvance=10. Glyphs 'a','b': xadvance=10, width=9 => getGlyphWidth (Java line 449) = (9+0)*1-0 =
    * 9; first xAdvance (BitmapFontData.getGlyphs) = -xoffset*scaleX-padLeft = 0. For "ab": xAdvances = [0,10,9] (last entry set to glyph width 9 by setLastGlyphXAdvance, Java lines 441-445), so
    * calculateWidths (Java lines 275-294) gives run.width = 0+10+9 = 19.
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
    for (ch <- "ab") registerGlyph(data, mkGlyph(ch, 10, 9))
    font
  }

  /** Lays out "ab" (run.width 19) with the given halign and targetWidth=50, no wrap, no truncate, so alignRuns (Java lines 297-306) is the only thing that can move run.x away from 0. */
  private def layoutOf(font: BitmapFont, halign: Align): GlyphLayout = {
    val layout = new GlyphLayout()
    layout.setText(font, "ab", 0, 2, Color.WHITE, 50f, halign.toInt, false, Nullable.empty[String])
    layout
  }

  test("ISS-581: setText with Align.left and a targetWidth keeps run.x == 0") {
    // Java: halign = Align.left = 8; line 298 (8 & Align.left) == 0 is FALSE,
    // so alignRuns is a no-op and run.x stays 0.
    // The port tests (8 & 1) == 0 => true (it reads the center bit), then
    // center = (8 & 2) != 0 => false, so it RIGHT-aligns left-aligned text:
    // run.x += 50 - 19 = 31.
    val layout = layoutOf(makeFont(), Align.left)
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(layout.runs(0).width, 19f, 0.0001f, "fixture sanity: run.width = 0+10+9 (Java lines 275-294)")
    assertEqualsFloat(
      layout.runs(0).x,
      0f,
      0.0001f,
      "Align.left must leave run.x untouched (Java line 298: (halign & Align.left) == 0 is false for left = 8)"
    )
  }

  test("ISS-581: setText with Align.center centers the run: run.x == (targetWidth - run.width) / 2") {
    // Java: halign = Align.center = 1; line 298 (1 & 8) == 0 => aligns; line
    // 299 center = (1 & Align.center) != 0 => true; line 303 run.x +=
    // 0.5f * (50 - 19) = 15.5.
    // The port tests (1 & 1) == 0 => false, so alignRuns is a no-op and
    // run.x stays 0.
    val layout = layoutOf(makeFont(), Align.center)
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(layout.runs(0).x, 15.5f, 0.0001f, "Align.center must center the run (Java lines 298-303, center = 1)")
  }

  test("ISS-581: setText with Align.topRight (and Align.right) right-aligns the run: run.x == targetWidth - run.width") {
    // Java: halign = Align.topRight = top|right = 2|16 = 18; line 298
    // (18 & 8) == 0 => aligns; line 299 center = (18 & 1) != 0 => false; line
    // 303 run.x += 50 - 19 = 31. The vertical bit is irrelevant to alignRuns.
    // The port tests (18 & 1) == 0 => true, then center = (18 & 2) != 0 =>
    // TRUE (the TOP bit is misread as center), so it CENTERS right-aligned
    // text: run.x = 15.5.
    // NB: plain Align.right = 16 has neither bit 1 nor bit 2 set, so on the
    // buggy code it coincidentally falls into the correct branch and produces
    // the right answer; it is asserted second to pin the Java semantics, but
    // only topRight exposes the bug.
    val layout = layoutOf(makeFont(), Align.topRight)
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(
      layout.runs(0).x,
      31f,
      0.0001f,
      "Align.topRight must right-align the run (Java lines 298-303, right = 16, top bit ignored)"
    )
    val layoutRight = layoutOf(makeFont(), Align.right)
    assertEqualsFloat(layoutRight.runs(0).x, 31f, 0.0001f, "Align.right must right-align the run (Java lines 298-303)")
  }

  test("ISS-581: 2-arg setText(font, str) behaves as Align.left: run.x == 0") {
    // Java line 95: setText(font, str) delegates with Align.left (= 8), no
    // alignment happens, run.x stays 0. The port (GlyphLayout.scala line 63)
    // passes halign = 0 with the false comment "Align.left = 0": 0 has no
    // bits set, so the port's alignRuns takes the not-center branch and, with
    // targetWidth = 0, shifts the run to run.x += 0 - 19 = -19.
    val layout = new GlyphLayout()
    layout.setText(makeFont(), "ab")
    assertEquals(layout.runs.size, 1)
    assertEqualsFloat(
      layout.runs(0).x,
      0f,
      0.0001f,
      "setText(font, str) must behave as Align.left (Java line 95), not shift runs to -run.width"
    )
  }
}
