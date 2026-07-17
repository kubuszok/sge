/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-805 RED suite — reproducer for "FreeType rasterization intermittently
 * returns EMPTY glyphs / no texture regions". Reproducer agent authored; the
 * fixer MUST NOT weaken these assertions — they encode the ORIGINAL libGDX
 * contract, not the port's current (buggy) behavior.
 *
 * ROOT CAUSE (found during reproduction, cited for the implementer):
 *
 *   FreeType's `FT_New_Memory_Face` does NOT copy the font bytes — it stores
 *   the caller's buffer pointer and reads the font tables (glyf, cmap, ...)
 *   LAZILY, for the whole lifetime of the FT_Face. libGDX honours this: the
 *   Library keeps every font ByteBuffer alive in a `LongMap<ByteBuffer>
 *   fontData` (FreeType.java:63) via `fontData.put(face, buffer)`
 *   (FreeType.java:126) and only frees it in `Face.dispose` / `Library.dispose`
 *   (FreeType.java:72-74, 169-173).
 *
 *   The SGE port frees it immediately. FreetypeOpsPanama.newMemoryFace
 *   (sge-extension/freetype/src/main/scalajvm/sge/platform/FreetypeOpsPanama.scala:161-168)
 *   allocates the bytes in a CONFINED arena and closes that arena in `finally`
 *   as soon as `sge_ft_new_memory_face` returns:
 *
 *       val arena = p.Arena.ofConfined()
 *       try { ... hNewMemoryFace.invoke(library, dataSeg, ...) }
 *       finally arena.arenaClose()   // <-- frees the buffer FreeType still points at
 *
 *   Every later glyph/cmap read therefore dereferences freed (and, after any
 *   intervening native allocation, reused) memory. Whether a glyph rasterizes
 *   correctly depends on whether that region has been overwritten yet — which
 *   is exactly the observed nondeterminism (Arial/Times "sometimes" empty,
 *   `getCharIndex` returning the raw char code, all-zero glyph metrics).
 *
 *   The Native path (FreetypeOpsNative.newMemoryFace) must be checked for the
 *   same lifetime bug when this is fixed.
 *
 * The fix must keep the font-data segment alive for the face's lifetime
 * (e.g. an Arena/segment map keyed by the face handle, freed in doneFace),
 * mirroring libGDX's fontData map.
 *
 * This suite forces the failure deterministically by allocating several faces
 * up front (each freeing its font buffer) before rasterizing, so the freed
 * regions are reliably reused. Runs on the JVM row only (freetype has no JS
 * axis; the Native row uses a separate FFI impl).
 */
package sge
package graphics
package g2d
package freetype

import sge.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

class FreeTypeUseAfterFreeIss805RedSuite extends munit.FunSuite {

  /** Rasterizes a plain uppercase 'A' from a freshly created generator and reports whether the glyph bitmap is non-empty (width>0 && height>0). A correctly loaded outline font ALWAYS produces a
    * non-empty 'A' at 32px.
    */
  private def rasterizesA(gen: FreeTypeFontGenerator)(using Sge): Boolean =
    try {
      val param = new FreeTypeFontParameter()
      param.size = 32
      param.characters = "Ao" // tiny set: 'o' provides the x-height char, 'A' the cap char
      val data = gen.generateData(param)
      data.getGlyph('A').exists(g => g.width > 0 && g.height > 0)
    } catch { case _: Throwable => false }

  test(
    "ISS-805: every face loaded from a valid outline TTF must rasterize a non-empty 'A' (font data must survive newMemoryFace)"
  ) {
    given Sge = FreetypeTestFixture.headlessSge()

    // Phase 1: create several generators from the SAME good outline font and
    // hold them. Each ctor calls newMemoryFace, whose confined arena is closed
    // immediately — freeing that face's font buffer while FreeType keeps the
    // pointer. The repeated allocation churn overwrites the earliest freed
    // buffers.
    val count = 16
    val gens  = (0 until count).map(_ => new FreeTypeFontGenerator(FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont)))

    // Phase 2: every one of them must still rasterize its glyphs. On the port
    // most of the early faces come back EMPTY (use-after-free); libGDX (and the
    // fixed port) yield a non-empty 'A' for all of them.
    try {
      val empties = gens.zipWithIndex.filter { case (gen, _) => !rasterizesA(gen) }.map(_._2)
      assert(
        empties.isEmpty,
        s"${empties.size} of $count faces rasterized an EMPTY 'A' (indices ${empties.mkString(",")}). " +
          "FreeType.Library must keep each font buffer alive for the face's lifetime " +
          "(FreetypeOpsPanama.newMemoryFace frees it in a finally-closed confined arena; " +
          "cf. libGDX FreeType.java fontData LongMap). See ISS-805."
      )
    } finally gens.foreach(_.close())
  }
}
