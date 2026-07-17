/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-777 (clause 1) RED suite — the kerning flag update
 * `parameter.kerning &= face.hasKerning()` (original FreeTypeFontGenerator.java
 * :440) became a read-only local in the port:
 *
 *     val kerning = parameter.kerning && face.hasKerning   (:346)
 *
 * so `parameter.kerning` is NEVER cleared. The original MUTATES the parameter:
 * after generateData, `parameter.kerning` equals `face.hasKerning()`. Because
 * the incremental getGlyph path (FreeTypeBitmapFontData.getGlyph) later gates
 * its kerning work on `param.kerning`, the port issues redundant, always-zero
 * kerning calls for every incrementally generated glyph on a font that has no
 * kern table.
 *
 * The bundled Inconsolata-LGC fixture reports `hasKerning == false` (it kerns
 * via GPOS, which FT_HAS_KERNING does not report), so the original contract is
 * `parameter.kerning == false` after generateData; the port leaves it `true`.
 *
 * Reproducer agent authored; the fixer MUST NOT weaken this. Retrying dodges
 * the concurrent ISS-805 use-after-free flake (generateData occasionally
 * throws before reaching the kerning code); the kerning-mutation assertion is
 * a distinct, deterministic defect.
 */
package sge
package graphics
package g2d
package freetype

import sge.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

class FreeTypeKerningMutationIss777RedSuite extends munit.FunSuite {

  test(
    "ISS-777: generateData must clear parameter.kerning when the face has no kern table (parameter.kerning &= face.hasKerning())"
  ) {
    given Sge = FreetypeTestFixture.headlessSge()

    // Retry to obtain one generateData run that completes (ISS-805 can make it
    // throw early); the assertion below is orthogonal to that flake.
    var completed  = false
    var attempt    = 0
    var kerningEnd = false
    var hasKerning = false
    while (!completed && attempt < 12) {
      attempt += 1
      val gen = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
      try {
        hasKerning = gen.face.hasKerning
        val param = new FreeTypeFontParameter()
        param.size = 32
        param.characters = "AVoxy."
        val _ = gen.generateData(param)
        kerningEnd = param.kerning
        completed = true
      } catch { case _: Throwable => () }
      finally gen.close()
    }

    assert(
      completed,
      s"ISS-805 masked ISS-777: generateData threw on all $attempt attempts (use-after-free); cannot observe the kerning mutation"
    )
    assertEquals(
      hasKerning,
      false,
      "test fixture invariant: Inconsolata-LGC must report hasKerning == false (kerns via GPOS, not a legacy kern table)"
    )
    assertEquals(
      kerningEnd,
      hasKerning,
      "original FreeTypeFontGenerator.java:440 does `parameter.kerning &= face.hasKerning()`, so after generateData " +
        "parameter.kerning must equal face.hasKerning (== false here); the port leaves it true (read-only local at " +
        "FreeTypeFontGenerator.scala:346)"
    )
  }
}
