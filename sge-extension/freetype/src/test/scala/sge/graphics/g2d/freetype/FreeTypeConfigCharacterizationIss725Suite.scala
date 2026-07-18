/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-725 (freetype clause) CROSS-PLATFORM characterization suite for the
 * lib-INDEPENDENT configuration surface of the freetype package. It lives in
 * the shared `src/test/scala` root so it is compiled and run on BOTH the JVM
 * and Scala Native rows (freetype has no JS axis — ISS-553).
 *
 * Coverage rationale (the real gap, wave 2026-07-18-K / territory K3):
 *   FreeTypeCharacterizationIss725Suite already pins FreeType.toInt and a
 *   SUBSET of the constant table, plus native-lib-backed face metadata. It does
 *   NOT cover the pure-logic configuration surface that drives generation:
 *     - FreeTypeFontParameter field defaults (size, hinting, colors, gamma,
 *       renderCount, border/shadow/pad/space, characters, kerning, filters, ...)
 *     - the Hinting enum (ordinals + java.lang.Enum round-trip)
 *     - the REMAINING FreeType constants (pixel modes, encodings' four-char
 *       codes, stroker line caps/joins incl. the MITER alias, style flags, the
 *       full load / load-target / render-mode / kerning / face-flag families)
 *     - DEFAULT_CHARS / NO_MAXIMUM / maxTextureSize
 *     - GlyphAndBitmap, FreeTypeBitmapFontData and FreeTypeFontLoaderParameter
 *       default state.
 *
 * Every value pinned here is fixed by the ORIGINAL libGDX FreeType.java /
 * FreeTypeFontGenerator.java / FreetypeFontLoader.java — no test theater. None
 * of these paths touch the native FreeType library, so the suite is a
 * deterministic green on every platform with no availability assume() gate.
 */
package sge
package graphics
package g2d
package freetype

import sge.graphics.Color
import sge.graphics.Texture.TextureFilter
import sge.graphics.g2d.freetype.FreeTypeFontGenerator.{ FreeTypeBitmapFontData, FreeTypeFontParameter, GlyphAndBitmap, Hinting }
import lowlevel.Nullable

class FreeTypeConfigCharacterizationIss725Suite extends munit.FunSuite {

  // ── FreeTypeFontParameter defaults (FreeTypeFontGenerator.java:406-464) ────

  test("a fresh FreeTypeFontParameter carries the libGDX default configuration") {
    val p = FreeTypeFontParameter()
    assertEquals(p.size, 16, "default size in pixels")
    assertEquals(p.mono, false, "smoothing enabled by default")
    assertEquals(p.hinting, Hinting.AutoMedium, "default hinting is the auto-hinter, average strength")
    assertEquals(p.color, Color.WHITE, "default foreground is white")
    assertEquals(p.gamma, 1.8f, "default glyph gamma")
    assertEquals(p.renderCount, 2, "glyph is rendered twice by default")
    assertEquals(p.borderWidth, 0f, "border disabled by default")
    assertEquals(p.borderColor, Color.BLACK, "default border color is black")
    assertEquals(p.borderStraight, false, "borders are rounded by default")
    assertEquals(p.borderGamma, 1.8f, "default border gamma")
    assertEquals(p.shadowOffsetX, 0, "shadow disabled on X by default")
    assertEquals(p.shadowOffsetY, 0, "shadow disabled on Y by default")
    // shadowColor default is a 75%-opaque black.
    assertEquals(p.shadowColor.r, 0f)
    assertEquals(p.shadowColor.g, 0f)
    assertEquals(p.shadowColor.b, 0f)
    assertEquals(p.shadowColor.a, 0.75f, "default shadow alpha")
    assertEquals(p.spaceX, 0)
    assertEquals(p.spaceY, 0)
    assertEquals(p.padTop, 0)
    assertEquals(p.padLeft, 0)
    assertEquals(p.padBottom, 0)
    assertEquals(p.padRight, 0)
    assertEquals(p.characters, FreeTypeFontGenerator.DEFAULT_CHARS, "default character set is DEFAULT_CHARS")
    assertEquals(p.kerning, true, "kerning requested by default")
    assertEquals(p.packer.isEmpty, true, "no packer by default")
    assertEquals(p.flip, false, "not flipped by default")
    assertEquals(p.genMipMaps, false, "no mip maps by default")
    assertEquals(p.minFilter, TextureFilter.Nearest, "default minification filter")
    assertEquals(p.magFilter, TextureFilter.Nearest, "default magnification filter")
    assertEquals(p.incremental, false, "not incremental by default")
  }

  test("FreeTypeFontParameter fields are independent mutable state") {
    val p = FreeTypeFontParameter()
    // Mutation 1: size is a plain var, editing it must not disturb the rest.
    p.size = 42
    assertEquals(p.size, 42)
    assertEquals(FreeTypeFontParameter().size, 16, "the default is per-instance, not shared mutable state")
    // Mutation 2: kerning is independent of size.
    p.kerning = false
    assertEquals(p.kerning, false)
    assertEquals(p.size, 42, "toggling kerning does not touch size")
    // Mutation 3: incremental toggle is independent again.
    p.incremental = true
    assertEquals(p.incremental, true)
    assertEquals(p.kerning, false)
  }

  // ── Hinting enum (FreeTypeFontGenerator.java:98-124) ──────────────────────

  test("Hinting enumerates the seven libGDX smoothing modes in order") {
    val values = Hinting.values.toList
    assertEquals(
      values,
      List(Hinting.None, Hinting.Slight, Hinting.Medium, Hinting.Full, Hinting.AutoSlight, Hinting.AutoMedium, Hinting.AutoFull)
    )
    assertEquals(Hinting.None.ordinal, 0)
    assertEquals(Hinting.AutoMedium.ordinal, 5)
    assertEquals(Hinting.AutoFull.ordinal, 6)
    // extends java.lang.Enum -> valueOf round-trips by name.
    assertEquals(Hinting.valueOf("AutoMedium"), Hinting.AutoMedium)
    assertEquals(Hinting.AutoMedium.name, "AutoMedium")
  }

  // ── Remaining FreeType constant table (FreeType.java) ─────────────────────
  //    (the ISS-805/ISS-725 suite pins the SCALABLE/HORIZONTAL/KERNING/LOAD/
  //     RENDER_NORMAL/RENDER_MONO/KERNING_DEFAULT subset; these are the rest.)

  test("FreeType pixel-mode constants match libGDX") {
    assertEquals(FreeType.FT_PIXEL_MODE_NONE, 0)
    assertEquals(FreeType.FT_PIXEL_MODE_MONO, 1)
    assertEquals(FreeType.FT_PIXEL_MODE_GRAY, 2)
    assertEquals(FreeType.FT_PIXEL_MODE_GRAY2, 3)
    assertEquals(FreeType.FT_PIXEL_MODE_GRAY4, 4)
    assertEquals(FreeType.FT_PIXEL_MODE_LCD, 5)
    assertEquals(FreeType.FT_PIXEL_MODE_LCD_V, 6)
  }

  test("FreeType encoding constants are FreeType four-char codes") {
    def fourCC(a: Char, b: Char, c: Char, d: Char): Int =
      (a.toInt << 24) | (b.toInt << 16) | (c.toInt << 8) | d.toInt
    assertEquals(FreeType.FT_ENCODING_NONE, 0)
    assertEquals(FreeType.FT_ENCODING_UNICODE, fourCC('u', 'n', 'i', 'c'))
    assertEquals(FreeType.FT_ENCODING_MS_SYMBOL, fourCC('s', 'y', 'm', 'b'))
    assertEquals(FreeType.FT_ENCODING_SJIS, fourCC('s', 'j', 'i', 's'))
    assertEquals(FreeType.FT_ENCODING_GB2312, fourCC('g', 'b', ' ', ' '))
    assertEquals(FreeType.FT_ENCODING_BIG5, fourCC('b', 'i', 'g', '5'))
    assertEquals(FreeType.FT_ENCODING_ADOBE_STANDARD, fourCC('A', 'D', 'O', 'B'))
    assertEquals(FreeType.FT_ENCODING_APPLE_ROMAN, fourCC('a', 'r', 'm', 'n'))
  }

  test("FreeType stroker line-cap / line-join constants match libGDX (incl. the MITER alias)") {
    assertEquals(FreeType.FT_STROKER_LINECAP_BUTT, 0)
    assertEquals(FreeType.FT_STROKER_LINECAP_ROUND, 1)
    assertEquals(FreeType.FT_STROKER_LINECAP_SQUARE, 2)
    assertEquals(FreeType.FT_STROKER_LINEJOIN_ROUND, 0)
    assertEquals(FreeType.FT_STROKER_LINEJOIN_BEVEL, 1)
    assertEquals(FreeType.FT_STROKER_LINEJOIN_MITER_VARIABLE, 2)
    // FT_STROKER_LINEJOIN_MITER is an alias of MITER_VARIABLE, NOT of MITER_FIXED.
    assertEquals(FreeType.FT_STROKER_LINEJOIN_MITER, FreeType.FT_STROKER_LINEJOIN_MITER_VARIABLE)
    assertEquals(FreeType.FT_STROKER_LINEJOIN_MITER_FIXED, 3)
  }

  test("FreeType style-flag, load-target, render-mode and kerning constants match libGDX") {
    assertEquals(FreeType.FT_STYLE_FLAG_ITALIC, 1)
    assertEquals(FreeType.FT_STYLE_FLAG_BOLD, 2)
    // Load targets are packed into the high 16 bits of the load flags.
    assertEquals(FreeType.FT_LOAD_TARGET_NORMAL, 0x0)
    assertEquals(FreeType.FT_LOAD_TARGET_LIGHT, 0x10000)
    assertEquals(FreeType.FT_LOAD_TARGET_MONO, 0x20000)
    assertEquals(FreeType.FT_LOAD_TARGET_LCD, 0x30000)
    assertEquals(FreeType.FT_LOAD_TARGET_LCD_V, 0x40000)
    assertEquals(FreeType.FT_RENDER_MODE_LIGHT, 1)
    assertEquals(FreeType.FT_RENDER_MODE_LCD, 3)
    assertEquals(FreeType.FT_RENDER_MODE_LCD_V, 4)
    assertEquals(FreeType.FT_RENDER_MODE_MAX, 5)
    assertEquals(FreeType.FT_KERNING_UNFITTED, 1)
    assertEquals(FreeType.FT_KERNING_UNSCALED, 2)
  }

  test("FreeType face-flag bit positions match libGDX (the ones the 805 suite does not pin)") {
    assertEquals(FreeType.FT_FACE_FLAG_FIXED_WIDTH, 1 << 2)
    assertEquals(FreeType.FT_FACE_FLAG_SFNT, 1 << 3)
    assertEquals(FreeType.FT_FACE_FLAG_VERTICAL, 1 << 5)
    assertEquals(FreeType.FT_FACE_FLAG_GLYPH_NAMES, 1 << 9)
    assertEquals(FreeType.FT_FACE_FLAG_HINTER, 1 << 11)
    assertEquals(FreeType.FT_FACE_FLAG_TRICKY, 1 << 13)
  }

  test("FreeType load flags used by getLoadingFlags are the documented FreeType bits") {
    assertEquals(FreeType.FT_LOAD_NO_HINTING, 0x2)
    assertEquals(FreeType.FT_LOAD_RENDER, 0x4)
    assertEquals(FreeType.FT_LOAD_NO_BITMAP, 0x8)
    assertEquals(FreeType.FT_LOAD_NO_AUTOHINT, 0x8000)
  }

  // ── Static generator config (FreeTypeFontGenerator.java:69-96) ────────────

  test("DEFAULT_CHARS begins with the missing-glyph marker and contains the ASCII alphanumerics") {
    val chars = FreeTypeFontGenerator.DEFAULT_CHARS
    assertEquals(chars.charAt(0).toInt, 0, "DEFAULT_CHARS starts with NUL (\\u0000) so the missing glyph is generated")
    assert(chars.contains("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), "contains the uppercase Latin alphabet")
    assert(chars.contains("abcdefghijklmnopqrstuvwxyz"), "contains the lowercase Latin alphabet")
    assert(chars.contains("1234567890"), "contains the digits")
    assertEquals(chars.length, 224, "the full DEFAULT_CHARS length")
  }

  test("NO_MAXIMUM and the default maxTextureSize match libGDX") {
    assertEquals(FreeTypeFontGenerator.NO_MAXIMUM, -1)
    assertEquals(FreeTypeFontGenerator.maxTextureSize, 1024)
  }

  // ── Value containers ──────────────────────────────────────────────────────

  test("a fresh GlyphAndBitmap has an empty bitmap and a non-null glyph") {
    val gab = GlyphAndBitmap()
    assertEquals(gab.bitmap, Nullable.empty[FreeType.Bitmap], "no bitmap until one is rendered")
    assert(gab.glyph != null, "the glyph container is always present")
  }

  test("FreeTypeBitmapFontData starts empty and propagates the flip flag") {
    val data = FreeTypeBitmapFontData()
    assertEquals(data.flipped, false, "default data is not flipped")
    assertEquals(data.regions, Nullable.empty, "no texture regions until generation")
    assertEquals(data.generator.isEmpty, true, "no incremental generator until generateData")
    assertEquals(data.parameter.isEmpty, true, "no incremental parameter until generateData")
    assertEquals(data.stroker.isEmpty, true, "no stroker until a border is requested")
    assertEquals(data.incrementalPacker.isEmpty, true, "no incremental packer until incremental generation")
    assertEquals(data.incrementalGlyphs.isEmpty, true, "no incremental glyph list until incremental generation")

    val flippedData = FreeTypeBitmapFontData(isFlipped = true)
    assertEquals(flippedData.flipped, true, "the constructor flag flows to BitmapFontData.flipped")
  }

  // ── Loader parameter defaults (FreetypeFontLoader.java) ───────────────────

  test("FreeTypeFontLoaderParameter defaults to an empty file name and a default font parameter") {
    val p = FreetypeFontLoader.FreeTypeFontLoaderParameter()
    assertEquals(p.fontFileName, "", "no font file name until set")
    assert(p.fontParameters != null, "a default FreeTypeFontParameter is always present")
    assertEquals(p.fontParameters.size, 16, "the nested parameter carries the default size")
  }
}
