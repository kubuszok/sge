/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-725 (freetype clause) characterization suite for the freetype package
 * core, pinned against the ORIGINAL libGDX FreeType.java /
 * FreeTypeFontGenerator.java. No test theater: every assertion checks a value
 * or contract that the original source dictates.
 *
 * These cover the parts of the package that are independent of the ISS-805
 * font-buffer use-after-free: the pure 26.6 fixed-point conversion, the
 * FreeType constant table, and face-level metadata that FreeType copies into
 * the FT_Face at load time (so it survives even the current buffer-lifetime
 * bug). Glyph-rasterization behaviour is characterized by the ISS-805 red
 * suite (it cannot be a stable green until that bug is fixed).
 */
package sge
package graphics
package g2d
package freetype

class FreeTypeCharacterizationIss725Suite extends munit.FunSuite {

  // ── Pure 26.6 fixed-point conversion (FreeType.java:942-943) ──────────────

  test("FreeType.toInt rounds a 26.6 fixed-point value up to whole pixels") {
    // ((value + 63) & -64) >> 6
    assertEquals(FreeType.toInt(0), 0)
    assertEquals(FreeType.toInt(1), 1) // rounds up
    assertEquals(FreeType.toInt(63), 1)
    assertEquals(FreeType.toInt(64), 1) // exactly 1px
    assertEquals(FreeType.toInt(65), 2) // rounds up
    assertEquals(FreeType.toInt(128), 2)
    assertEquals(FreeType.toInt(2240), 35)
  }

  // ── Constant table (FreeType.java:862-909) ────────────────────────────────

  test("FreeType face-flag / load / render / kerning constants match libGDX") {
    assertEquals(FreeType.FT_FACE_FLAG_SCALABLE, 1)
    assertEquals(FreeType.FT_FACE_FLAG_FIXED_SIZES, 2)
    assertEquals(FreeType.FT_FACE_FLAG_HORIZONTAL, 16)
    assertEquals(FreeType.FT_FACE_FLAG_KERNING, 64)
    assertEquals(FreeType.FT_LOAD_DEFAULT, 0x0)
    assertEquals(FreeType.FT_LOAD_FORCE_AUTOHINT, 0x20)
    assertEquals(FreeType.FT_LOAD_TARGET_LIGHT, 0x10000)
    assertEquals(FreeType.FT_LOAD_TARGET_MONO, 0x20000)
    assertEquals(FreeType.FT_RENDER_MODE_NORMAL, 0)
    assertEquals(FreeType.FT_RENDER_MODE_MONO, 2)
    assertEquals(FreeType.FT_KERNING_DEFAULT, 0)
  }

  // ── Face-level metadata (FreeTypeFontGenerator ctor + FreeType.Face) ───────

  test("a scalable outline TTF exposes SCALABLE|HORIZONTAL face flags, glyphs, and a matching name") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      val flags = gen.face.faceFlags
      assert((flags & FreeType.FT_FACE_FLAG_SCALABLE) != 0, "Inconsolata-LGC is a scalable outline font")
      assert((flags & FreeType.FT_FACE_FLAG_HORIZONTAL) != 0, "horizontal layout metrics are present")
      assertEquals(flags & FreeType.FT_FACE_FLAG_FIXED_SIZES, 0, "an outline font has no embedded bitmap strikes")
      assertEquals(gen.bitmapped, false, "checkForBitmapFont must not flag an outline font as bitmapped")
      assert(gen.face.numGlyphs > 0, "the font contains glyphs")
      // FreeTypeFontGenerator.name / toString = file name without extension.
      assertEquals(gen.toString, "sge-freetype-test-Inconsolata-LGC")
    } finally gen.close()
  }

  // NOTE: hasGlyph / getCharIndex (cmap lookup) and all glyph rasterization
  // read the font tables lazily from the memory buffer, so they are corrupted
  // by the ISS-805 use-after-free and cannot be a stable green until it is
  // fixed. Their contract is pinned by FreeTypeUseAfterFreeIss805RedSuite.

  test("size metrics after setPixelSizes have a positive ascent and negative descent") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      gen.setPixelSizes(0, 32)
      val m = gen.face.getSize.getMetrics
      assert(m.ascender > 0, s"ascender must be positive, was ${m.ascender}")
      assert(m.descender < 0, s"descender must be negative, was ${m.descender}")
      assert(m.height > 0, s"line height must be positive, was ${m.height}")
    } finally gen.close()
  }
}
