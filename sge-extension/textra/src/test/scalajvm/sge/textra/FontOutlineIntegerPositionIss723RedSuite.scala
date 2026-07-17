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
 * Upstream rounds each vertex (handleIntegerPosition == Math.round when
 * integerPosition), so EVERY drawn position vertex is a whole number; in the
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

    val font = new Font()
    font.cellWidth = 12f
    font.cellHeight = 16f // xOutline = yOutline = outlineStrength * 16 / 32 = 0.5
    font.originalCellHeight = 16f
    font.mapping.put('A'.toInt, HeadlessTextraSge.texturedGlyph(tex, 12, 16, 10f))
    font.useIntegerPositions(true)

    val batch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(batch, 'A'.toLong | Font.BLACK_OUTLINE, 5f, 40f)

    // 8 outline copies (3x3 grid minus the centre) + 1 main glyph quad.
    assertEquals(batch.quads.size, 9, "BLACK_OUTLINE draws 8 outline copies plus the main glyph quad")

    val posIdx = List(0, 1, 5, 6, 10, 11, 15, 16)
    batch.quads.zipWithIndex.foreach { case (quad, q) =>
      posIdx.foreach { i =>
        val v = quad.verts(i)
        assertEquals(
          v,
          Math.round(v).toFloat,
          s"ISS-723(3): quad $q position vertex $i must be a whole number under integer positions (upstream wraps outline writes in handleIntegerPosition, Font.java:5659-5660); the port's setQuadVertices leaves the +-0.5 outline offsets unrounded"
        )
      }
    }
  }
}
