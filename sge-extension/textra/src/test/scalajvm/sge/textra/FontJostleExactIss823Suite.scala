/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Strengthened JOSTLE guard for ISS-823 (textra wave 2026-07-17-E), against
 * TextraTypist upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java:5593-5596):
 *
 *     if (jostled) {
 *         int code = NumberUtils.floatToIntBits(
 *             x * 1.8191725133961645f + y * 1.6710436067037893f + c * 1.5497004779019703f) & 0xFFFFFF;
 *         xc += code % 5 - 2f;
 *         yt += (code >>> 6) % 5 - 2f;
 *     }
 *
 * ISS-710's original regression suite (FontDrawGlyphWaveDRedSuite) asserts only
 * that jostled geometry differs from plain SOMEWHERE, which survives a mutation
 * that drops one offset axis or uses the wrong jostle constants (auditor M
 * finding 1). This suite REPLACES that weak check with EXACT per-axis deltas:
 * for a known (x, y, c) it independently recomputes the upstream `code` and the
 * two integer offsets `code % 5 - 2` (x) and `(code >>> 6) % 5 - 2` (y), then
 * asserts the rendered jostled quad is translated from the plain quad by
 * exactly those amounts on each axis and each of the four corners.
 *
 * A mutant that dropped the x offset would render jostled.x == plain.x, so the
 * observed x-delta (0) would mismatch the recomputed non-zero `code % 5 - 2` and
 * this suite would fail; likewise for a wrong constant (different `code`, hence
 * different offsets). This does NOT edit FontDrawGlyphWaveDRedSuite.
 *
 * xc feeds directly into all three corner x-offsets and yt into all three
 * corner y-offsets (Font.scala:2718-2723), and with rotation 0 (cos == 1,
 * sin == 0) the quad vertices are `x + p*x` / `y + p*y` (Font.scala:2807-2814),
 * so the jostle offset is a pure integer translation of the whole quad. With
 * this font config (scaleX = scaleY = 1, descent = 0, integerPosition = false)
 * the value fed to the `code` formula is exactly x = xIn + 11f, y = yIn + 8f,
 * c = 'A' = 65 (cellWidth 12 -> +6, xAdvance 10 -> centerX 5; originalCellHeight
 * 16 -> centerY 8), all exact integer floats — so the recomputed offsets match
 * the port bit-for-bit on the same JVM.
 *
 * Render-path assertion (no GL); written by the reproducer agent and MUST NOT
 * be modified by the fixer.
 */
package sge
package textra

class FontJostleExactIss823Suite extends munit.FunSuite {

  // Upstream jostle mixing constants (Font.java:5594) — as Float literals, matching the port.
  private val kx = 1.8191725133961645f
  private val ky = 1.6710436067037893f
  private val kc = 1.5497004779019703f

  private def configuredFont()(using Sge): (Font, sge.graphics.Texture) = {
    val tex  = HeadlessTextraSge.newTexture()
    val font = new Font()
    font.mapping.put('A'.toInt, HeadlessTextraSge.texturedGlyph(tex, 12, 16, 10f))
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    (font, tex)
  }

  /** The exact post-transform inputs the jostle `code` formula sees for a glyph drawn at (xIn, yIn) with this config. */
  private def postX(xIn: Float): Float = xIn + 11f // + centerX (xAdvance*0.5 = 5) + cellWidth*0.5 (6)
  private def postY(yIn: Float): Float = yIn + 8f // + centerY (originalCellHeight*0.5 = 8); descent 0

  private def offsets(xIn: Float, yIn: Float, c: Int): (Int, Int) = {
    val code = java.lang.Float.floatToIntBits(postX(xIn) * kx + postY(yIn) * ky + c * kc) & 0xffffff
    (code % 5 - 2, (code >>> 6) % 5 - 2)
  }

  test(
    "ISS-823: JOSTLE translates the glyph quad by exactly (code%5-2, (code>>>6)%5-2) on each axis (upstream Font.java:5594-5596)"
  ) {
    given Sge = HeadlessTextraSge.make()

    val c = 'A'.toInt // low 16 bits of the glyph == the char code fed to the formula

    // Find an input whose expected offsets are non-zero on BOTH axes, so the test
    // exercises (and would catch the loss of) each axis independently.
    val chosenY = 40f
    var chosenX = Float.NaN
    var dx      = 0
    var dy      = 0
    var xi      = 0
    while (xi < 128 && (dx == 0 || dy == 0)) {
      val xIn        = xi.toFloat
      val (cdx, cdy) = offsets(xIn, chosenY, c)
      if (cdx != 0 && cdy != 0) {
        chosenX = xIn
        dx = cdx
        dy = cdy
      }
      xi += 1
    }
    assert(!chosenX.isNaN, "found an input position whose jostle offsets are non-zero on both axes")

    val (font, _) = configuredFont()

    val plainBatch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(plainBatch, c.toLong, chosenX, chosenY)

    val jostleBatch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(jostleBatch, c.toLong | Font.JOSTLE, chosenX, chosenY)

    assertEquals(plainBatch.quads.size, 1, "a plain 'A' draws exactly one quad")
    assertEquals(jostleBatch.quads.size, 1, "a jostled 'A' draws exactly one quad")

    val plain  = plainBatch.quads.head.verts
    val jostle = jostleBatch.quads.head.verts

    // Vertex layout: x at indices 0,5,10,15 and y at 1,6,11,16 (four corners).
    val xIdx = List(0, 5, 10, 15)
    val yIdx = List(1, 6, 11, 16)

    xIdx.foreach { i =>
      assertEquals(
        jostle(i) - plain(i),
        dx.toFloat,
        s"ISS-823: x-corner $i must shift by exactly code%5-2 == $dx (upstream Font.java:5595); a dropped-x or wrong-constant jostle would not"
      )
    }
    yIdx.foreach { i =>
      assertEquals(
        jostle(i) - plain(i),
        dy.toFloat,
        s"ISS-823: y-corner $i must shift by exactly (code>>>6)%5-2 == $dy (upstream Font.java:5596); a dropped-y or wrong-constant jostle would not"
      )
    }
  }
}
