/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-727 RED suite — the public single-glyph API
 * `generateGlyphAndBitmap(int c, int size, boolean flip)` (original
 * FreeTypeFontGenerator.java:219-269) is entirely absent from the port, even
 * though its result type `GlyphAndBitmap` was ported
 * (FreeTypeFontGenerator.scala:779) and the surviving doc comment still refers
 * to the missing method.
 *
 * This suite does not compile against the current port (the method does not
 * exist) — that COMPILE error is the proof of red. Once the method is ported,
 * the assertions pin the original contract:
 *   - returns Nullable.empty when the char is not in the font
 *     (`face.getCharIndex(c) == 0`), FreeTypeFontGenerator.java:227-229;
 *   - glyph.id == c, glyph.srcX == 0, glyph.srcY == 0 (:261-263);
 *   - the flip yoffset math (:259):
 *       flip ? -bitmapTop + baseline : -(height - bitmapTop) - baseline
 *     Adding the two forms cancels bitmapTop and baseline, leaving
 *       yoffset(flip=true) + yoffset(flip=false) == -glyph.height
 *     which is a clean invariant independent of the (buffer-derived) top and
 *     baseline values.
 *
 * NOTE: the rasterization values these assertions read are also subject to the
 * ISS-805 font-buffer use-after-free, so a stable RUNTIME green additionally
 * requires ISS-805 to be fixed. The COMPILE failure alone establishes ISS-727.
 *
 * Reproducer agent authored; the fixer MUST NOT weaken these.
 */
package sge
package graphics
package g2d
package freetype

import lowlevel.Nullable
import sge.graphics.g2d.freetype.FreeTypeFontGenerator.GlyphAndBitmap

class FreeTypeGenerateGlyphAndBitmapIss727RedSuite extends munit.FunSuite {

  test("ISS-727: generateGlyphAndBitmap returns Nullable.empty for a char not in the font") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      val result: Nullable[GlyphAndBitmap] = gen.generateGlyphAndBitmap(0x1f600, 32, false)
      assert(
        result.isEmpty,
        "grinning-face emoji is not in the font -> generateGlyphAndBitmap must be Nullable.empty (orig :227-229)"
      )
    } finally gen.close()
  }

  test("ISS-727: generateGlyphAndBitmap yields glyph with id==c, srcX==0, srcY==0 for a present char") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      val result: Nullable[GlyphAndBitmap] = gen.generateGlyphAndBitmap('A'.toInt, 32, false)
      result.fold(fail("'A' is in the font -> generateGlyphAndBitmap must not be empty")) { gb =>
        assertEquals(gb.glyph.id, 'A'.toInt)
        assertEquals(gb.glyph.srcX, 0)
        assertEquals(gb.glyph.srcY, 0)
      }
    } finally gen.close()
  }

  test("ISS-727: the flip yoffset math obeys yoffset(flip) + yoffset(noflip) == -height") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      val flip:   GlyphAndBitmap = gen.generateGlyphAndBitmap('A'.toInt, 32, true).getOrElse(fail("flip result empty"))
      val noflip: GlyphAndBitmap = gen.generateGlyphAndBitmap('A'.toInt, 32, false).getOrElse(fail("noflip result empty"))
      assertEquals(
        flip.glyph.yoffset + noflip.glyph.yoffset,
        -noflip.glyph.height,
        "flip yoffset formula (orig FreeTypeFontGenerator.java:259)"
      )
    } finally gen.close()
  }
}
