/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-495 (Gdx2dDraw scaled-blit filter dispatch inverted).
 *
 * The original C dispatch (original-src/libgdx/gdx/jni/gdx2d/gdx2d.c, blit,
 * lines 937-944) routes:
 *
 *   scale == GDX2D_SCALE_NEAREST  -> blit_linear   (gdx2d.c lines 888-935;
 *                                    despite its name this is the fixed-point
 *                                    NEAREST-neighbour implementation)
 *   scale == GDX2D_SCALE_BILINEAR -> blit_bilinear (gdx2d.c lines 807-886)
 *
 * The port (sge/src/main/scala/sge/graphics/g2d/Gdx2dDraw.scala line 677)
 * routes GDX2D_SCALE_LINEAR (the bilinear request) to blitNearest and the
 * NEAREST request to blitBilinear — Pixmap.setFilter semantics are exactly
 * reversed for scaled drawPixmap.
 *
 * Every expected value below is derived by hand-tracing the C original:
 *
 * Fixture: source pixmap 2x1 RGBA8888, left pixel pure black 0x000000FF,
 * right pixel pure white 0xFFFFFFFF; scaled drawPixmap to a 4x1 RGBA8888
 * destination, blending disabled.
 *
 * NEAREST (blit_linear, gdx2d.c 888-935):
 *   x_ratio = (src_width << 16) / dst_width + 1 = (2 << 16) / 4 + 1 = 32769
 *     (line 899)
 *   y_ratio = (1 << 16) / 1 + 1 = 65537 (line 900); sy = (0*65537)>>16 = 0
 *     (line 910)
 *   sx = (j * x_ratio) >> 16 (line 916):
 *     j=0: (0*32769)>>16 = 0     -> black
 *     j=1: (1*32769)>>16 = 0     -> black   (32769 < 65536)
 *     j=2: (2*32769)>>16 = 1     -> white   (65538 >> 16 = 1)
 *     j=3: (3*32769)>>16 = 1     -> white   (98307 >> 16 = 1)
 *   => dst = [0x000000FF, 0x000000FF, 0xFFFFFFFF, 0xFFFFFFFF] — every pixel
 *      EXACTLY the source black or the source white, never a blend.
 *
 * BILINEAR (blit_bilinear, gdx2d.c 807-886):
 *   x_ratio = ((float)src_width - 1) / dst_width = (2-1)/4 = 0.25f (line 818)
 *   y_ratio = (1-1)/1 = 0 (line 819) => y_diff = 0, vertical weights vanish
 *   sx = (int)(j * 0.25f) = 0 for j=0..3 (line 838)
 *   x_diff = 0.25f * j - 0 = 0.0, 0.25, 0.5, 0.75 (line 840)
 *   c1 = src(0,0) = black; c2 = src(1,0) = white because sx+1 = 1 < src_width
 *     = 2 (line 848); c3 = c4 contributions are zero since y_diff = 0.
 *   Each of r/g/b = (uint32_t)(0*(1-x_diff) + 255*x_diff) (lines 857-868):
 *     j=0: 0.0    -> 0x00
 *     j=1: 63.75  -> 0x3F  (truncation)
 *     j=2: 127.5  -> 0x7F
 *     j=3: 191.25 -> 0xBF
 *   alpha = 255 throughout (line 869-872: both c1 and c2 have alpha 0xff).
 *   All float steps (0.25 multiples, products with 255) are exactly
 *   representable in IEEE-754 single precision, so the values are pinned.
 *   => dst = [0x000000FF, 0x3F3F3FFF, 0x7F7F7FFF, 0xBFBFBFFF]
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original C semantics, not the port's.
 */
package sge
package graphics
package g2d

import sge.graphics.Pixmap
import sge.graphics.Pixmap.{ Blending, Filter, Format }

class Gdx2dBlitRedSuite extends munit.FunSuite {

  /** 2x1 RGBA8888: left pixel pure black, right pixel pure white. */
  private val black = 0x000000ff
  private val white = 0xffffffff

  private def makeSource(): Pixmap = {
    val src = new Pixmap(2, 1, Format.RGBA8888)
    src.setBlending(Blending.None)
    src.drawPixel(Pixels(0), Pixels(0), black)
    src.drawPixel(Pixels(1), Pixels(0), white)
    src
  }

  private def scaleTo4x1(src: Pixmap, filter: Filter): Pixmap = {
    val dst = new Pixmap(4, 1, Format.RGBA8888)
    dst.setBlending(Blending.None)
    dst.setFilter(filter)
    dst.drawPixmap(src, Pixels(0), Pixels(0), Pixels(2), Pixels(1), Pixels(0), Pixels(0), Pixels(4), Pixels(1))
    dst
  }

  private def hex(c: Int): String = f"0x$c%08X"

  test("ISS-495: NearestNeighbour scaled drawPixmap samples exact source pixels (C blit_linear, gdx2d.c 888-935)") {
    val src = makeSource()
    val dst = scaleTo4x1(src, Filter.NearestNeighbour)
    // Expected from the C fixed-point math (gdx2d.c lines 899, 916):
    // x_ratio = (2<<16)/4+1 = 32769; sx = (j*32769)>>16 = 0,0,1,1.
    val expected = Array(black, black, white, white)
    val actual   = Array.tabulate(4)(j => dst.getPixel(Pixels(j), Pixels(0)))
    src.close()
    dst.close()
    for (j <- 0 until 4) {
      assertEquals(
        actual(j),
        expected(j),
        s"dst pixel $j: expected ${hex(expected(j))} (nearest-neighbour, C blit_linear), got ${hex(actual(j))} — " +
          "intermediate values mean the bilinear filter ran instead"
      )
    }
  }

  test("ISS-495: NearestNeighbour scaled drawPixmap never produces blended colors") {
    val src = makeSource()
    val dst = scaleTo4x1(src, Filter.NearestNeighbour)
    val actual = Array.tabulate(4)(j => dst.getPixel(Pixels(j), Pixels(0)))
    src.close()
    dst.close()
    for (j <- 0 until 4) {
      assert(
        actual(j) == black || actual(j) == white,
        s"dst pixel $j: nearest-neighbour must output EXACTLY a source pixel (black ${hex(black)} or white ${hex(white)}), " +
          s"got blended ${hex(actual(j))}"
      )
    }
  }

  test("ISS-495: BiLinear scaled drawPixmap blends interior pixels (C blit_bilinear, gdx2d.c 807-886)") {
    val src = makeSource()
    val dst = scaleTo4x1(src, Filter.BiLinear)
    val actual = Array.tabulate(4)(j => dst.getPixel(Pixels(j), Pixels(0)))
    src.close()
    dst.close()
    // At least one interior pixel must be strictly between black and white:
    // C blit_bilinear weighs c1=black against c2=white with x_diff = 0.25*j
    // (gdx2d.c lines 818, 840, 848, 852-874).
    val blendedExists = (1 to 3).exists { j =>
      val r = (actual(j) >>> 24) & 0xff
      r > 0x00 && r < 0xff
    }
    assert(
      blendedExists,
      s"expected at least one interior pixel strictly between black and white, got ${actual.map(hex).mkString("[", ", ", "]")} — " +
        "only pure source colors mean the nearest-neighbour filter ran instead"
    )
  }

  test("ISS-495: BiLinear scaled drawPixmap produces the exact C blit_bilinear values") {
    val src = makeSource()
    val dst = scaleTo4x1(src, Filter.BiLinear)
    // Derived from gdx2d.c lines 818 (x_ratio=0.25), 840 (x_diff=0.25*j),
    // 848 (c2 = right source pixel), 857-874 (channel = trunc(255*x_diff),
    // alpha = 255). All float steps are exact in single precision.
    val expected = Array(0x000000ff, 0x3f3f3fff, 0x7f7f7fff, 0xbfbfbfff)
    val actual   = Array.tabulate(4)(j => dst.getPixel(Pixels(j), Pixels(0)))
    src.close()
    dst.close()
    for (j <- 0 until 4) {
      assertEquals(
        actual(j),
        expected(j),
        s"dst pixel $j: expected ${hex(expected(j))} (C blit_bilinear), got ${hex(actual(j))}"
      )
    }
  }
}
