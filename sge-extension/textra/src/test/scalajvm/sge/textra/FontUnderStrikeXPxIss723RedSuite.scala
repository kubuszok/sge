/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-723 clause 2 (textra wave 2026-07-17-E), against
 * TextraTypist upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   Underline box-drawing branch, Font.java:5785:
 *     p0x = (changedW * (font.underX + 1f)) - font.cellWidth * 0.5f
 *           - changedW * 0.1f - cos * centerX - xPx;
 *   Strikethrough box-drawing branch, Font.java:5867: same shape with
 *     strikeX/strikeY, also ending in `- xPx`.
 *
 * The port drops the trailing `- xPx` in both (Font.scala:2859 `up0x = ...
 * - cos * ucx` and :2968 `sp0x = ... - cos * scx`), so the box-drawing
 * underline/strikethrough segments sit one pixel-width too far right AND stop
 * responding to the backbuffer resolution (xPx = 2 / (backBufferWidth *
 * projectionMatrix.values(0)), Font.java:5510 / Font.scala:2641).
 *
 * Pin: draw the same underlined (resp. struck-through) glyph under two
 * backbuffer widths (960 and 480, identity projection). Every non-xPx term of
 * p0x is backbuffer-independent, so upstream the underline/strike block quads
 * translate horizontally by exactly -(xPx@480 - xPx@960) = -(2/480 - 2/960)
 * while their y geometry is unchanged; the port yields byte-identical
 * geometry at both widths (observed delta 0). The expected delta is computed
 * with the same float expressions the code uses; the comparison allows a tiny
 * absolute tolerance because the delta is recovered by subtracting two
 * nearby world-space coordinates.
 *
 * The box-drawing path is selected by mapping 0x2500 to a GlyphRegion with
 * offsetX == NaN (Font.scala:2858/2967) and giving solidBlock a textured
 * region for drawBlockSequence to render; the underline/strike quads are the
 * ones drawn with the solid-block texture.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist geometry, not the port's.
 */
package sge
package textra

class FontUnderStrikeXPxIss723RedSuite extends munit.FunSuite {

  private val eps = 1e-4f

  /** Draws `glyphBits`-decorated 'A' at the given backbuffer width and returns the solid-block quads (the underline/strike segments). */
  private def blockQuads(bbWidth: Int, glyphBits: Long): Vector[Array[Float]] = {
    given Sge = HeadlessTextraSge.make(bbWidth = bbWidth, bbHeight = 540)

    val glyphTex = HeadlessTextraSge.newTexture()
    val blockTex = HeadlessTextraSge.newTexture() // distinct so the block quads are identifiable

    val font = new Font()
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    font.mapping.put('A'.toInt, HeadlessTextraSge.texturedGlyph(glyphTex, 12, 16, 10f))
    // Box-drawing branch: 0x2500 present with offsetX == NaN (Font.scala:2858/2967).
    font.mapping.put(0x2500, new Font.GlyphRegion(Float.NaN, 0f, 12f))
    font.mapping.put(font.solidBlock.toInt, HeadlessTextraSge.texturedGlyph(blockTex, 4, 4, 4f))

    val batch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(batch, 'A'.toLong | glyphBits, 5f, 40f)

    val quads = batch.quads.filter(_.texture eq blockTex).map(_.verts).toVector
    assert(quads.nonEmpty, "the box-drawing branch must draw at least one solid-block quad")
    quads
  }

  /** The expected horizontal translation between bb 480 and bb 960: upstream p0x carries -xPx (identity projection: xPx = 2/backBufferWidth). */
  private val expectedDx: Float = -(2f / 480f - 2f / 960f)

  private def checkAxis(glyphBits: Long, what: String): Unit = {
    val at960 = blockQuads(960, glyphBits)
    val at480 = blockQuads(480, glyphBits)
    assertEquals(at480.size, at960.size, s"the $what draws the same number of block quads at both widths")

    val xIdx = List(0, 5, 10, 15)
    val yIdx = List(1, 6, 11, 16)

    at960.indices.foreach { q =>
      xIdx.foreach { i =>
        assertEqualsFloat(
          at480(q)(i) - at960(q)(i),
          expectedDx,
          eps,
          s"ISS-723(2): $what block-quad $q x-vertex $i must shift by -(2/480 - 2/960) == $expectedDx when the backbuffer narrows (upstream p0x ends in -xPx, Font.java:5785/5867); the port dropped -xPx so the geometry is backbuffer-independent (delta 0)"
        )
      }
      yIdx.foreach { i =>
        assertEqualsFloat(
          at480(q)(i) - at960(q)(i),
          0f,
          eps,
          s"sanity: $what block-quad $q y-vertex $i is backbuffer-width-independent"
        )
      }
    }
  }

  test(
    "ISS-723 clause 2: box-drawing UNDERLINE p0x carries -xPx (upstream Font.java:5785); the port's up0x (Font.scala:2859) omits it"
  ) {
    checkAxis(Font.UNDERLINE, "underline")
  }

  test(
    "ISS-723 clause 2: box-drawing STRIKETHROUGH p0x carries -xPx (upstream Font.java:5867); the port's sp0x (Font.scala:2968) omits it"
  ) {
    checkAxis(Font.STRIKETHROUGH, "strikethrough")
  }
}
