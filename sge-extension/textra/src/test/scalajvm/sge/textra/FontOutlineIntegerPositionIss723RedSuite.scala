/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-723 clause 3 (textra wave 2026-07-17-E), against
 * TextraTypist upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   BLACK_OUTLINE branch, Font.java:5659-5660: every outline copy's position
 *   vertices are written through font.handleIntegerPosition(...):
 *     vertices[0] = font.handleIntegerPosition(x + cos*p0x - sin*p0y + xa); ...
 *   HALO/NEON branch, Font.java:5680-5681: identical wrapping.
 *
 * The port funnels those writes through setQuadVertices (Font.scala:2401-2422),
 * which does NOT apply handleIntegerPosition — so with
 * font.useIntegerPositions(true) the outline copies land on fractional
 * positions (the +-0.5-pixel xa/ya offsets survive unrounded) while upstream
 * snaps every outline vertex to whole units.
 *
 * Pin: an integer-position font with cellHeight 16 (xOutline = yOutline =
 * outlineStrength * cellHeight / 32 = 0.5) drawing 'A' | BLACK_OUTLINE at an
 * integer position produces 8 outline quads offset by xa, ya in {-0.5, 0, 0.5}.
 * Upstream's handleIntegerPosition NO-OPs at 3fe5c930 (Font.java:5254-5256:
 * `return p;//integerPosition ? MathUtils.round(p) : p;` — the round is commented
 * out), so upstream does NOT snap these vertices. SGE deliberately honors the
 * useIntegerPositions contract and rounds instead — a ratified deviation (ISS-836)
 * — so under integer positions EVERY drawn vertex should be a whole number; in the
 * port the offset outline quads keep .5 fractions. The main-glyph quad is
 * written with handleIntegerPosition in both (Font.scala:2807-2812), so
 * asserting integrality over all drawn quads is red only through the outline
 * copies.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist integer-position semantics, not the port's.
 */
package sge
package textra

class FontOutlineIntegerPositionIss723RedSuite extends munit.FunSuite {

  test(
    "ISS-723 clause 3: with useIntegerPositions(true), BLACK_OUTLINE copies snap to whole positions (upstream Font.java:5659-5660 handleIntegerPosition); the port's setQuadVertices never rounds"
  ) {
    given Sge = HeadlessTextraSge.make()
    val tex   = HeadlessTextraSge.newTexture()

    val font = HeadlessTextraSge.emptyFont() // user ruling 2026-10-17: the empty font this test was written against
    font.cellWidth = 12f
    font.cellHeight = 16f // xOutline = yOutline = outlineStrength * 16 / 32 = 0.5
    font.originalCellHeight = 16f
    font.mapping.put('A'.toInt, HeadlessTextraSge.texturedGlyph(tex, 12, 16, 10f))
    font.useIntegerPositions(true)

    val batch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(batch, 'A'.toLong | Font.BLACK_OUTLINE, 5f, 40f)

    // 8 outline copies (3x3 grid minus the centre) + 1 main glyph quad.
    assertEquals(batch.quads.size, 9, "BLACK_OUTLINE draws 8 outline copies plus the main glyph quad")

    // user ruling 2026-10-17: java's handleIntegerPosition returns its input (Font.java:5254-5256, the round
    // commented out), so the outline copies keep their +-0.5 offsets from the main glyph, unrounded
    val main    = batch.quads.last.verts
    val offsets = batch.quads.init.map { quad =>
      val dx = List(0, 5, 10, 15).map(i => quad.verts(i) - main(i)).distinct
      val dy = List(1, 6, 11, 16).map(i => quad.verts(i) - main(i)).distinct
      assertEquals(dx.size, 1, s"an outline copy is the main quad translated as a whole on x: $dx")
      assertEquals(dy.size, 1, s"an outline copy is the main quad translated as a whole on y: $dy")
      (dx.head, dy.head)
    }
    val half = Set(-0.5f, 0f, 0.5f)
    assert(offsets.forall((dx, dy) => half(dx) && half(dy)), s"ISS-723(3): outline offsets stay at java's unrounded +-0.5: $offsets")
    assertEquals(offsets.distinct.size, 8, s"the eight outline copies sit at the eight distinct +-0.5 offsets: $offsets")
    assert(!offsets.contains((0f, 0f)), s"no outline copy sits on the main glyph: $offsets")
  }
}
