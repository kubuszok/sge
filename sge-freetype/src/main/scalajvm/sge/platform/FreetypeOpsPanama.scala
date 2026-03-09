// SGE FreeType — Panama-based implementation of FreetypeOps
//
// Calls Rust C ABI functions using the PanamaProvider abstraction.
// Works on both Desktop JVM (JdkPanama) and Android (PanamaPortProvider).
//
// Migration notes:
//   Origin: LibGDX gdx-freetype extension (JNI native methods)
//   Convention: Panama FFM delegation to Rust C ABI native lib
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

import java.lang.invoke.MethodHandle

private[sge] class FreetypeOpsPanama(val p: PanamaProvider) extends FreetypeOps {
  import p.*

  // ─── Native library + linker setup ─────────────────────────────────────

  private val linker: p.Linker = p.Linker.nativeLinker()

  private val lib: p.SymbolLookup = {
    val libName = System.mapLibraryName("sge_native_ops")
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    val found   = paths.iterator
      .map(dir => java.nio.file.Path.of(dir, libName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(
          s"Cannot find $libName in java.library.path: $libPath"
        )
      )
    p.SymbolLookup.libraryLookup(found, p.Arena.global())
  }

  private def lookup(name: String): p.MemorySegment =
    lib.findOrThrow(name)

  // ─── Method handle cache ───────────────────────────────────────────────

  // Library lifecycle
  private val hInitFreeType: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_init_freetype"),
    p.FunctionDescriptor.of(p.JAVA_LONG)
  )

  private val hDoneFreeType: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_done_freetype"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG)
  )

  // Face lifecycle
  private val hNewMemoryFace: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_new_memory_face"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.JAVA_INT)
  )

  private val hDoneFace: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_done_face"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG)
  )

  // Face configuration
  private val hSelectSize: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_select_size"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT)
  )

  private val hSetCharSize: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_set_char_size"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )

  private val hSetPixelSizes: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_set_pixel_sizes"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )

  // Glyph loading
  private val hLoadGlyph: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_load_glyph"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )

  private val hLoadChar: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_load_char"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )

  private val hRenderGlyph: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_render_glyph"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT)
  )

  // Kerning
  private val hHasKerning: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_has_kerning"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG)
  )

  private val hGetKerning: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_get_kerning"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )

  private val hGetCharIndex: MethodHandle = linker.downcallHandle(
    lookup("sge_ft_get_char_index"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT)
  )

  // Face metrics
  private val hGetFaceFlags:          MethodHandle = linker.downcallHandle(lookup("sge_ft_get_face_flags"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetStyleFlags:         MethodHandle = linker.downcallHandle(lookup("sge_ft_get_style_flags"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetNumGlyphs:          MethodHandle = linker.downcallHandle(lookup("sge_ft_get_num_glyphs"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetAscender:           MethodHandle = linker.downcallHandle(lookup("sge_ft_get_ascender"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetDescender:          MethodHandle = linker.downcallHandle(lookup("sge_ft_get_descender"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetHeight:             MethodHandle = linker.downcallHandle(lookup("sge_ft_get_height"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetMaxAdvanceWidth:    MethodHandle = linker.downcallHandle(lookup("sge_ft_get_max_advance_width"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetMaxAdvanceHeight:   MethodHandle = linker.downcallHandle(lookup("sge_ft_get_max_advance_height"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetUnderlinePosition:  MethodHandle = linker.downcallHandle(lookup("sge_ft_get_underline_position"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetUnderlineThickness: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_underline_thickness"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))

  // Glyph slot access
  private val hGetGlyphSlot:              MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_slot"), p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG))
  private val hGetGlyphMetrics:           MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_metrics"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS))
  private val hGetGlyphLinearHoriAdvance: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_linear_hori_advance"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphAdvanceX:          MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_advance_x"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphAdvanceY:          MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_advance_y"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphFormat:            MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_format"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))

  // Glyph slot bitmap
  private val hGetGlyphBitmapRows:      MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_rows"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapWidth:     MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_width"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapPitch:     MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_pitch"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapBuffer:    MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_buffer"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS, p.JAVA_INT))
  private val hGetGlyphBitmapNumGray:   MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_num_gray"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapPixelMode: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_pixel_mode"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapLeft:      MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_left"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))
  private val hGetGlyphBitmapTop:       MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph_bitmap_top"), p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG))

  // Size metrics
  private val hGetSizeMetrics: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_size_metrics"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS))

  // Stroker
  private val hStrokerNew:  MethodHandle = linker.downcallHandle(lookup("sge_ft_stroker_new"), p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG))
  private val hStrokerSet:  MethodHandle = linker.downcallHandle(lookup("sge_ft_stroker_set"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT))
  private val hStrokerDone: MethodHandle = linker.downcallHandle(lookup("sge_ft_stroker_done"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG))

  // Glyph outline operations
  private val hGetGlyphAsStroke:     MethodHandle = linker.downcallHandle(lookup("sge_ft_get_glyph"), p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG))
  private val hStrokeBorder:         MethodHandle = linker.downcallHandle(lookup("sge_ft_stroke_border"), p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT))
  private val hGlyphToBitmap:        MethodHandle = linker.downcallHandle(lookup("sge_ft_glyph_to_bitmap"), p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT))
  private val hGetBitmapGlyphBitmap: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_bitmap_glyph_bitmap"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS))
  private val hGetBitmapGlyphBuffer: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_bitmap_glyph_buffer"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS, p.JAVA_INT))
  private val hDoneGlyph:            MethodHandle = linker.downcallHandle(lookup("sge_ft_done_glyph"), p.FunctionDescriptor.ofVoid(p.JAVA_LONG))

  // Error
  private val hGetLastErrorCode: MethodHandle = linker.downcallHandle(lookup("sge_ft_get_last_error_code"), p.FunctionDescriptor.of(p.JAVA_INT))

  // ─── Implementation ────────────────────────────────────────────────────

  override def initFreeType(): Long =
    hInitFreeType.invoke().asInstanceOf[Long]

  override def doneFreeType(library: Long): Unit =
    hDoneFreeType.invoke(library)

  override def newMemoryFace(library: Long, data: Array[Byte], dataSize: Int, faceIndex: Int): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val dataSeg = arena.allocateElems(p.JAVA_BYTE, data.length.toLong)
      p.MemorySegment.copyFromBytes(data, 0, dataSeg, 0L, data.length)
      hNewMemoryFace.invoke(library, dataSeg, dataSize, faceIndex).asInstanceOf[Long]
    } finally arena.arenaClose()
  }

  override def doneFace(face: Long): Unit =
    hDoneFace.invoke(face)

  override def selectSize(face: Long, strikeIndex: Int): Boolean =
    (hSelectSize.invoke(face, strikeIndex): Any).asInstanceOf[Int] != 0

  override def setCharSize(face: Long, charWidth: Int, charHeight: Int, horzRes: Int, vertRes: Int): Boolean =
    (hSetCharSize.invoke(face, charWidth, charHeight, horzRes, vertRes): Any).asInstanceOf[Int] != 0

  override def setPixelSizes(face: Long, pixelWidth: Int, pixelHeight: Int): Boolean =
    (hSetPixelSizes.invoke(face, pixelWidth, pixelHeight): Any).asInstanceOf[Int] != 0

  override def loadGlyph(face: Long, glyphIndex: Int, loadFlags: Int): Boolean =
    (hLoadGlyph.invoke(face, glyphIndex, loadFlags): Any).asInstanceOf[Int] != 0

  override def loadChar(face: Long, charCode: Int, loadFlags: Int): Boolean =
    (hLoadChar.invoke(face, charCode, loadFlags): Any).asInstanceOf[Int] != 0

  override def renderGlyph(glyphSlot: Long, renderMode: Int): Boolean =
    (hRenderGlyph.invoke(glyphSlot, renderMode): Any).asInstanceOf[Int] != 0

  override def hasKerning(face: Long): Boolean =
    (hHasKerning.invoke(face): Any).asInstanceOf[Int] != 0

  override def getKerning(face: Long, leftGlyph: Int, rightGlyph: Int, kernMode: Int): Int =
    hGetKerning.invoke(face, leftGlyph, rightGlyph, kernMode).asInstanceOf[Int]

  override def getCharIndex(face: Long, charCode: Int): Int =
    hGetCharIndex.invoke(face, charCode).asInstanceOf[Int]

  override def getFaceFlags(face: Long): Int =
    hGetFaceFlags.invoke(face).asInstanceOf[Int]

  override def getStyleFlags(face: Long): Int =
    hGetStyleFlags.invoke(face).asInstanceOf[Int]

  override def getNumGlyphs(face: Long): Int =
    hGetNumGlyphs.invoke(face).asInstanceOf[Int]

  override def getAscender(face: Long): Int =
    hGetAscender.invoke(face).asInstanceOf[Int]

  override def getDescender(face: Long): Int =
    hGetDescender.invoke(face).asInstanceOf[Int]

  override def getHeight(face: Long): Int =
    hGetHeight.invoke(face).asInstanceOf[Int]

  override def getMaxAdvanceWidth(face: Long): Int =
    hGetMaxAdvanceWidth.invoke(face).asInstanceOf[Int]

  override def getMaxAdvanceHeight(face: Long): Int =
    hGetMaxAdvanceHeight.invoke(face).asInstanceOf[Int]

  override def getUnderlinePosition(face: Long): Int =
    hGetUnderlinePosition.invoke(face).asInstanceOf[Int]

  override def getUnderlineThickness(face: Long): Int =
    hGetUnderlineThickness.invoke(face).asInstanceOf[Int]

  override def getGlyphSlot(face: Long): Long =
    hGetGlyphSlot.invoke(face).asInstanceOf[Long]

  override def getGlyphMetrics(glyphSlot: Long, out: Array[Int]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val outSeg = arena.allocateElems(p.JAVA_INT, out.length.toLong)
      hGetGlyphMetrics.invoke(glyphSlot, outSeg)
      p.MemorySegment.copyToInts(outSeg, 0L, out, 0, out.length)
    } finally arena.arenaClose()
  }

  override def getGlyphLinearHoriAdvance(glyphSlot: Long): Int =
    hGetGlyphLinearHoriAdvance.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphAdvanceX(glyphSlot: Long): Int =
    hGetGlyphAdvanceX.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphAdvanceY(glyphSlot: Long): Int =
    hGetGlyphAdvanceY.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphFormat(glyphSlot: Long): Int =
    hGetGlyphFormat.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapRows(glyphSlot: Long): Int =
    hGetGlyphBitmapRows.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapWidth(glyphSlot: Long): Int =
    hGetGlyphBitmapWidth.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapPitch(glyphSlot: Long): Int =
    hGetGlyphBitmapPitch.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapBuffer(glyphSlot: Long, buffer: Array[Byte], bufferSize: Int): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val bufSeg = arena.allocateElems(p.JAVA_BYTE, bufferSize.toLong)
      hGetGlyphBitmapBuffer.invoke(glyphSlot, bufSeg, bufferSize)
      p.MemorySegment.copyToBytes(bufSeg, 0L, buffer, 0, bufferSize)
    } finally arena.arenaClose()
  }

  override def getGlyphBitmapNumGray(glyphSlot: Long): Int =
    hGetGlyphBitmapNumGray.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapPixelMode(glyphSlot: Long): Int =
    hGetGlyphBitmapPixelMode.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapLeft(glyphSlot: Long): Int =
    hGetGlyphBitmapLeft.invoke(glyphSlot).asInstanceOf[Int]

  override def getGlyphBitmapTop(glyphSlot: Long): Int =
    hGetGlyphBitmapTop.invoke(glyphSlot).asInstanceOf[Int]

  override def getSizeMetrics(face: Long, out: Array[Int]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val outSeg = arena.allocateElems(p.JAVA_INT, out.length.toLong)
      hGetSizeMetrics.invoke(face, outSeg)
      p.MemorySegment.copyToInts(outSeg, 0L, out, 0, out.length)
    } finally arena.arenaClose()
  }

  override def strokerNew(library: Long): Long =
    hStrokerNew.invoke(library).asInstanceOf[Long]

  override def strokerSet(stroker: Long, radius: Int, lineCap: Int, lineJoin: Int, miterLimit: Int): Unit =
    hStrokerSet.invoke(stroker, radius, lineCap, lineJoin, miterLimit)

  override def strokerDone(stroker: Long): Unit =
    hStrokerDone.invoke(stroker)

  override def getGlyphAsStroke(glyphSlot: Long): Long =
    hGetGlyphAsStroke.invoke(glyphSlot).asInstanceOf[Long]

  override def strokeBorder(glyph: Long, stroker: Long, inside: Boolean): Long =
    hStrokeBorder.invoke(glyph, stroker, if (inside) 1 else 0).asInstanceOf[Long]

  override def glyphToBitmap(glyph: Long, renderMode: Int): Long =
    hGlyphToBitmap.invoke(glyph, renderMode).asInstanceOf[Long]

  override def getBitmapGlyphBitmap(glyph: Long, out: Array[Int]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val outSeg = arena.allocateElems(p.JAVA_INT, out.length.toLong)
      hGetBitmapGlyphBitmap.invoke(glyph, outSeg)
      p.MemorySegment.copyToInts(outSeg, 0L, out, 0, out.length)
    } finally arena.arenaClose()
  }

  override def getBitmapGlyphBuffer(glyph: Long, buffer: Array[Byte], bufferSize: Int): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val bufSeg = arena.allocateElems(p.JAVA_BYTE, bufferSize.toLong)
      hGetBitmapGlyphBuffer.invoke(glyph, bufSeg, bufferSize)
      p.MemorySegment.copyToBytes(bufSeg, 0L, buffer, 0, bufferSize)
    } finally arena.arenaClose()
  }

  override def doneGlyph(glyph: Long): Unit =
    hDoneGlyph.invoke(glyph)

  override def getLastErrorCode(): Int =
    hGetLastErrorCode.invoke().asInstanceOf[Int]
}
