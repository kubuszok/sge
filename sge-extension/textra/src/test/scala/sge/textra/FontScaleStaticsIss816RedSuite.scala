/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * COMPILE-RED API-presence suite for ISS-816 (textra wave 2026-07-17-E),
 * against TextraTypist upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   public static float extractScale(long glyph)             — Font.java:8185, returns 1f always
 *   public static int   extractIntScale(long glyph)          — Font.java:8201, returns 4 always
 *   public static long  applyScale(long glyph, float scale)  — Font.java:8217, returns glyph unchanged
 *
 * All three are deprecated-but-public upstream statics (scale moved into
 * Layout.sizing/advances; these are kept as fixed-value compatibility shims).
 * The SGE port has none of them (surfaced by wave-D `enforce compare`), so this
 * suite references `Font.extractScale` / `Font.extractIntScale` /
 * `Font.applyScale` and therefore does NOT compile until they are ported —
 * the only expected errors are 'value extractScale/extractIntScale/applyScale
 * is not a member of object sge.textra.Font'. Runtime red is impossible for a
 * missing method, so per the wave brief (and the wave-D 766a8b67 precedent)
 * this compile-red suite is committed SEPARATELY, as the LAST commit, because
 * it breaks sge-textra test compilation (and its sibling suites) until the
 * fix lands.
 *
 * The value assertions pin the exact upstream shim semantics so the fixer
 * cannot satisfy the suite with differently-behaving methods.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist API surface and shim values, not the port's.
 */
package sge
package textra

class FontScaleStaticsIss816RedSuite extends munit.FunSuite {

  test("ISS-816: Font.extractScale(glyph) exists and always returns 1f (upstream Font.java:8185-8187)") {
    assertEquals(Font.extractScale('A'.toLong), 1f)
    assertEquals(Font.extractScale(0L), 1f)
    assertEquals(Font.extractScale(-1L), 1f)
  }

  test("ISS-816: Font.extractIntScale(glyph) exists and always returns 4 (upstream Font.java:8201-8203)") {
    assertEquals(Font.extractIntScale('A'.toLong), 4)
    assertEquals(Font.extractIntScale(0L), 4)
    assertEquals(Font.extractIntScale(-1L), 4)
  }

  test("ISS-816: Font.applyScale(glyph, scale) exists and returns glyph unchanged (upstream Font.java:8217-8219)") {
    val glyph = 'A'.toLong | Font.BOLD | Font.JOSTLE
    assertEquals(Font.applyScale(glyph, 2.5f), glyph)
    assertEquals(Font.applyScale(0L, 0.001f), 0L)
    assertEquals(Font.applyScale(-1L, 123456.789f), -1L)
  }
}
