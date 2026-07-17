/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests — ISS-734 umbrella clauses c4 and c6 (core minors #2), wave
 * 2026-07-17-F, territory X. Reproducer-authored: MUST NOT be modified by the
 * fixer; they encode the ORIGINAL LibGDX semantics
 * (com/badlogic/gdx/graphics/g2d/BitmapFont.java, original-src/libgdx), not the
 * port's.
 *
 * --- c4: empty-but-present pageRegions array must fall back to loadRegions ---
 *
 * Original BitmapFont.java line 156:
 *   if (pageRegions == null || pageRegions.size == 0) { ... regions read from data.imagePaths ... }
 * i.e. a regions array that is present but EMPTY (size == 0) is treated the same
 * as a null array: the page images are loaded from the BitmapFontData's
 * imagePaths.
 *
 * The port (BitmapFont.scala line 50):
 *   val regions = regionsParam.getOrElse(loadRegions())
 * only falls back when the Nullable is EMPTY. A present-but-empty
 * DynamicArray[TextureRegion] is used verbatim, so loadRegions() (and the
 * size == 0 check) is skipped.
 *
 * --- c6: glyph metric add-then-truncate vs truncate-then-add ---
 *
 * Original BitmapFont.java setGlyphRegion lines 750-771 mutate the INT glyph
 * fields with FLOAT operands via Java compound assignment, e.g.
 *   glyph.width += x;   // int += float  ==  glyph.width = (int)(glyph.width + x)
 * JLS 15.26.2: `E1 op= E2` is `E1 = (T)(E1 op E2)`, so the addition happens in
 * float and is truncated to int ONCE, AFTER adding — "add-then-truncate".
 *
 * The port (BitmapFont.scala lines 597-618) truncates the float operand FIRST:
 *   glyph.width += x.toInt      // "truncate-then-add"
 * These differ by up to 1px whenever the offset is fractional (offsetX/offsetY
 * are Float fields). Exact-value proof below.
 */
package sge
package graphics
package g2d

import scala.util.control.NonFatal

import sge.noop.{ NoopGL20, NoopGraphics }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class BitmapFontRegionsAndGlyphMetricIss734RedSuite extends munit.FunSuite {

  // ── shared headless fixture ────────────────────────────────────────────────

  private object BufferGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glGenBuffer as _, * }
    def glGenBuffer(): Int = 1
  }

  private def makeSge(files: Files = null): Sge =
    if (files == null) SgeTestFixture.testSge(graphics = new NoopGraphics() { override def gl20: GL20 = BufferGL20 })
    else SgeTestFixture.testSge(graphics = new NoopGraphics() { override def gl20: GL20 = BufferGL20 }, files = files)

  /** Custom-type dummy texture: no Pixmap, no real GL upload, width/height as given. */
  final private class DummyTextureData(w: Int, h: Int) extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = w
    def height:                                   Int                         = h
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  // ── c4 fixtures ────────────────────────────────────────────────────────────

  /** Marker thrown from RecordingFiles to prove loadRegions() reached the page-image resolution. */
  final private class LoadRegionsInvoked extends RuntimeException("loadRegions reached page-image resolution")

  /** A Files whose internal()/getFileHandle() abort immediately, so loadRegions() aborts BEFORE any real PNG / GL work — but only if it is actually called. */
  final private class RecordingFiles extends Files {
    def internal(path:      String):                           files.FileHandle = throw new LoadRegionsInvoked
    def getFileHandle(path: String, fileType: files.FileType): files.FileHandle = throw new LoadRegionsInvoked
    def classpath(path:     String):                           files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                           files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                   String           = ""
    def isExternalStorageAvailable:                            Boolean          = false
    def localStoragePath:                                      String           = ""
    def isLocalStorageAvailable:                               Boolean          = false
  }

  test(
    "ISS-734 c4: a present-but-EMPTY pageRegions array must fall back to loadRegions() (BitmapFont.java:156 pageRegions.size == 0)"
  ) {
    given Sge = makeSge(files = new RecordingFiles)

    // BitmapFontData with imagePaths present but no explicit fontFile, so the
    // fallback path resolves pages via Texture(path) -> Sge().files.internal(...).
    val data = new BitmapFontData()
    data.imagePaths = Nullable(Array("page.png"))

    // A present (non-null) but EMPTY regions array. Original treats size == 0
    // identically to a missing array and reads pages from imagePaths.
    val emptyRegions = DynamicArray[TextureRegion]()

    val outcome =
      try {
        val _ = new BitmapFont(data, Nullable(emptyRegions), true)
        "constructed-with-empty-regions"
      } catch {
        case _: LoadRegionsInvoked => "loadRegions-invoked"
        case NonFatal(e) => "threw:" + e.getClass.getSimpleName
      }

    assertEquals(
      outcome,
      "loadRegions-invoked",
      "empty-but-present pageRegions must fall back to loadRegions (BitmapFont.java:156 checks size == 0); " +
        "the port's `regionsParam.getOrElse(loadRegions())` uses the empty array verbatim and never loads the pages"
    )
  }

  // ── c6 ─────────────────────────────────────────────────────────────────────

  test("ISS-734 c6: setGlyphRegion clips glyph.width with add-then-truncate (BitmapFont.java:750), not truncate-then-add") {
    given Sge = makeSge()

    // 64x64 texture so regionWidth = 64 (the x2 > regionWidth branch stays off).
    val texture = new Texture(new DummyTextureData(64, 64))
    val region  = new TextureAtlas.AtlasRegion(texture, 0, 0, 64, 64)
    // Fractional left-edge whitespace offset. offsetX is a Float field; the
    // original's arithmetic was written for fractional offsets. offsetY stays 0
    // (originalHeight == packedHeight, atlas offsetY == 0) so only the X branch runs.
    region.offsetX = 0.5f

    val glyph = new BitmapFont.Glyph()
    glyph.srcX = 0
    glyph.width = 5
    glyph.xoffset = 3
    glyph.height = 8
    glyph.srcY = 0
    glyph.page = 0

    val data = new BitmapFontData() // flipped = false
    data.setGlyphRegion(glyph, region)

    // Derivation (BitmapFont.java:740-758):
    //   x  = srcX(0)               ; offsetX = 0.5 > 0
    //   x -= 0.5  -> x = -0.5  (< 0):
    //     glyph.width += x   -> (int)(5 + (-0.5)) = (int)(4.5) = 4     [add-then-truncate]
    //     glyph.xoffset -= x -> (int)(3 - (-0.5)) = (int)(3.5) = 3
    //     x = 0
    //   x2 = srcX + width - 0.5 = 4.5 ; 4.5 > regionWidth(64)? no -> width unchanged
    //
    // The port computes glyph.width += (-0.5).toInt = 5 + 0 = 5 (truncate-then-add),
    // one pixel too wide.
    assertEquals(
      glyph.width,
      4,
      "glyph.width must be add-then-truncate: (int)(5 + (-0.5)) = 4 (BitmapFont.java:750). " +
        "The port's `glyph.width += x.toInt` yields 5 (truncate-then-add)."
    )
    assertEquals(glyph.xoffset, 3, "glyph.xoffset must be (int)(3 - (-0.5)) = 3 (BitmapFont.java:751)")
  }
}
