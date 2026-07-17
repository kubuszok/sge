/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-838 Native-row COVERAGE suite (wave 2026-07-17-F, territory Z).
 *
 * The Native freetype row had ZERO runtime coverage, so dropping the ISS-805
 * per-face font-buffer retention in FreetypeOpsNative (the `faceData`
 * ConcurrentHashMap that keeps each `newMemoryFace` byte array reachable for the
 * face's lifetime, FreetypeOpsNative.scala:83-107) would go UNDETECTED on this
 * platform. This suite ports the ISS-805 use-after-free reproduction shape to the
 * Native FFI so a future regression is caught.
 *
 * WHY the retention matters (same root cause as the JVM ISS-805 suite):
 *   sge_ft_new_memory_face passes the Scala byte array's pointer (`data.at(0)`)
 *   straight to FT_New_Memory_Face, which does NOT copy it — FreeType reads the
 *   font tables (glyf, cmap, ...) LAZILY from that memory for the whole lifetime
 *   of the FT_Face. If `data` becomes unreachable it is GC-collected and the
 *   pointer dangles; subsequent glyph loads read freed/reused memory and produce
 *   empty or garbage glyphs. FreetypeOpsNative keeps a strong reference in
 *   `faceData` (mirroring libGDX's Library.fontData LongMap) to prevent this.
 *
 * This is a GREEN pin (label: coverage), NOT a red: it PASSES at HEAD because the
 * retention is present. It was verified to go RED under a local uncommitted
 * mutation that removes the `faceData.put(face, data)` line — see the reproducer
 * report for the mutation evidence.
 *
 * Native row only: freetype has no JS axis, and the JVM row uses a separate
 * Panama FFI impl already covered by FreeTypeUseAfterFreeIss805RedSuite.
 */
package sge
package platform

import java.io.{ ByteArrayOutputStream, FileInputStream }
import java.nio.file.{ Files, Path, Paths }

import sge.graphics.g2d.freetype.FreeType

class FreetypeFaceDataRetentionIss838Suite extends munit.FunSuite {

  /** Bundled scalable OUTLINE TTF fixture (SIL OFL 1.1), the same font the JVM ISS-805 suite uses. It rasterizes crisp outline glyphs at any pixel size.
    */
  private val FontName = "sge-freetype-test-Inconsolata-LGC.ttf"

  /** `embedResources` is NOT enabled for the freetype module, so classpath / getResourceAsStream cannot resolve the fixture on the Native row. Read the file directly, locating it by walking up from
    * the process working directory (robust to the cwd being either the repo root or the module base).
    */
  private def locateFont(): Path = {
    val repoLayout   = "sge-extension/freetype/src/test/resources/" + FontName
    val moduleLayout = "src/test/resources/" + FontName
    var dir          = Paths.get(System.getProperty("user.dir")).toAbsolutePath
    var found: Path = null
    var hops = 0
    while ((found eq null) && (dir ne null) && hops < 12) {
      val c1 = dir.resolve(repoLayout)
      val c2 = dir.resolve(moduleLayout)
      if (Files.exists(c1)) found = c1
      else if (Files.exists(c2)) found = c2
      else {
        dir = dir.getParent
        hops += 1
      }
    }
    if (found eq null)
      throw new IllegalStateException(
        s"could not locate $FontName from user.dir=${System.getProperty("user.dir")} (looked for $repoLayout / $moduleLayout up to 12 parents)"
      )
    found
  }

  private def readAllBytes(path: Path): Array[Byte] = {
    val in  = new FileInputStream(path.toFile)
    val out = new ByteArrayOutputStream()
    try {
      val buf = new Array[Byte](64 * 1024)
      var n   = in.read(buf)
      while (n >= 0) {
        out.write(buf, 0, n)
        n = in.read(buf)
      }
      out.toByteArray()
    } finally in.close()
  }

  private lazy val fontBytes: Array[Byte] = readAllBytes(locateFont())

  /** Rasterizes an uppercase 'A' at 32px through the raw Native ops and reports whether the resulting bitmap is non-empty (rows>0 && width>0). A correctly retained outline font ALWAYS yields a
    * non-empty 'A'.
    */
  private def rasterizesA(ops: FreetypeOps, face: Long): Boolean =
    ops.setPixelSizes(face, 0, 32) &&
      ops.loadChar(face, 'A'.toInt, FreeType.FT_LOAD_DEFAULT) && {
        val slot = ops.getGlyphSlot(face)
        ops.renderGlyph(slot, FreeType.FT_RENDER_MODE_NORMAL)
        ops.getGlyphBitmapRows(slot) > 0 && ops.getGlyphBitmapWidth(slot) > 0
      }

  /** Allocates and fills a large amount of same-sized garbage so that, IF a face's font buffer were left unreachable (retention removed), the GC would reclaim it and this churn would overwrite the
    * freed region with 0xFF — corrupting the font tables FreeType still points at. With the retention present the buffers stay reachable and are untouched.
    */
  private def churn(chunkSize: Int): Unit = {
    var acc = 0
    var i   = 0
    while (i < 4096) {
      val garbage = new Array[Byte](chunkSize)
      java.util.Arrays.fill(garbage, 0xff.toByte)
      acc += garbage(chunkSize - 1).toInt // keep the fill from being elided
      i += 1
    }
    // Prevent DCE of the whole loop.
    if (acc == Int.MinValue) throw new IllegalStateException("unreachable")
  }

  test("ISS-838 coverage: many faces retain their font buffers and each rasterizes a non-empty 'A' (FreetypeOpsNative)") {
    val ops     = FreetypeOpsNative
    val library = ops.initFreeType()
    assert(library != 0L, "FT_Init_FreeType must succeed on the Native row")

    val count = 16
    val faces = new Array[Long](count)
    try {
      // Phase 1: create several faces from FRESH per-face copies of the font
      // bytes. Only FreetypeOpsNative.faceData keeps each copy reachable — the
      // test itself deliberately does NOT hold the byte arrays.
      var i = 0
      while (i < count) {
        val bytes = fontBytes.clone()
        val face  = ops.newMemoryFace(library, bytes, bytes.length, 0)
        assert(face != 0L, s"newMemoryFace must succeed for face $i")
        faces(i) = face
        i += 1
      }

      // Phase 2: churn the heap to force GC. Any UN-retained font buffer would be
      // collected and its memory overwritten here (mutation red); the retained
      // buffers survive (HEAD green).
      churn(fontBytes.length)

      // Phase 3: every face must still rasterize a non-empty 'A'.
      val empties = scala.collection.mutable.ArrayBuffer[Int]()
      i = 0
      while (i < count) {
        if (!rasterizesA(ops, faces(i))) empties += i
        i += 1
      }
      assert(
        empties.isEmpty,
        s"${empties.size}/$count faces rasterized an EMPTY 'A' (indices ${empties.mkString(",")}). " +
          "FreetypeOpsNative must keep each font buffer alive for the face's lifetime " +
          "(faceData.put in newMemoryFace; cf. libGDX FreeType.java fontData LongMap). See ISS-805 / ISS-838."
      )
    } finally {
      var i = 0
      while (i < count) {
        if (faces(i) != 0L) ops.doneFace(faces(i))
        i += 1
      }
      ops.doneFreeType(library)
    }
  }
}
