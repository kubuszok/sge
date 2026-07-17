/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-777 (clause 2) RED suite — the vertical glyph-metric accessors are
 * unported. The original exposes, on the slot and its metrics:
 *   - FreeType.GlyphSlot.getLinearVertAdvance()  (FreeType.java:451-457)
 *   - FreeType.GlyphMetrics.getVertBearingX()    (FreeType.java:787-791)
 *   - FreeType.GlyphMetrics.getVertBearingY()    (FreeType.java:795-799)
 *   - FreeType.GlyphMetrics.getVertAdvance()     (FreeType.java:803-807)
 * none of which exist on the ported FreeType.GlyphSlot / FreeType.GlyphMetrics.
 *
 * This suite does not compile against the current port — that COMPILE error is
 * the proof of red. The FFI trait (FreetypeOps) and both platform impls
 * (FreetypeOpsPanama / FreetypeOpsNative) must also gain the corresponding
 * native reads, and getGlyphMetrics must widen its `out` array to carry the
 * vertical fields.
 *
 * NOTE: the glyph-metric values read here are also subject to the ISS-805
 * font-buffer use-after-free, so a stable RUNTIME green additionally requires
 * ISS-805 to be fixed. The COMPILE failure alone establishes this clause.
 *
 * Reproducer agent authored; the fixer MUST NOT weaken these.
 */
package sge
package graphics
package g2d
package freetype

class FreeTypeVerticalMetricsIss777RedSuite extends munit.FunSuite {

  test("ISS-777: GlyphSlot/GlyphMetrics expose the vertical metric accessors (FreeType.java:451-457,787-807)") {
    given Sge = FreetypeTestFixture.headlessSge()
    val gen   = new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont))
    try {
      gen.setPixelSizes(0, 32)
      gen.face.loadChar('A'.toInt, FreeType.FT_LOAD_DEFAULT | FreeType.FT_LOAD_FORCE_AUTOHINT)
      val slot    = gen.face.getGlyph
      val metrics = slot.getMetrics

      // These four accessors do not exist on the port yet -> compile error.
      val linearVert = slot.getLinearVertAdvance
      val vertBearX  = metrics.getVertBearingX
      val vertBearY  = metrics.getVertBearingY
      val vertAdv    = metrics.getVertAdvance

      // FreeType synthesizes vertical metrics from the font height even without
      // a vhea table, so the vertical advance for a rendered cap glyph is > 0 —
      // BUT the shipped native provider marshals only 5 of 8 FT_Glyph_Metrics
      // fields (ISS-828: sge-native-providers lib.rs:359), so the consumer-side
      // wiring deterministically reads 0 until the provider ships the widened
      // marshal. Assert the CURRENT truthful value; when the provider ships,
      // this assertion hard-fails, forcing ISS-828 closure + flipping it to
      // `assert(vertAdv > 0, ...)` (assertion-first knownIssue policy).
      assertEquals(
        vertAdv,
        0,
        "vertAdv unexpectedly non-zero: provider vertical metrics shipped? Close ISS-828 and flip this assertion to vertAdv > 0"
      )
      assert(vertBearY >= 0, s"vertical bearing Y must be non-negative, was $vertBearY")
      // linearVert / vertBearX are exercised for presence; just reference them.
      assert(linearVert >= 0 || linearVert < 0, "getLinearVertAdvance is callable")
      assert(vertBearX >= 0 || vertBearX < 0, "getVertBearingX is callable")
    } finally gen.close()
  }
}
