// SGE FreeType — Platform-agnostic FreeType operations trait
//
// Platform implementations:
//   JVM:    FreetypeOpsPanama  (delegates to Rust via Panama FFM)
//   JS:     FreetypeOpsJs      (stub — users pre-generate fonts for JS)
//   Native: FreetypeOpsNative  (delegates to Rust via C ABI)
//
// Migration notes:
//   Origin: LibGDX gdx-freetype extension (JNI native methods in FreeType.java)
//   Convention: Long handles for opaque FreeType pointers; bulk data via Array[Byte]/Array[Int]
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

/** Low-level FreeType operations for font rasterization.
  *
  * All FreeType objects (Library, Face, GlyphSlot, Glyph, Stroker) are represented as opaque `Long` handles. Bitmap and metrics
  * data is copied into Scala arrays to avoid cross-platform pointer issues.
  */
private[sge] trait FreetypeOps {

  // ─── Library lifecycle ──────────────────────────────────────────────────

  /** Initializes a new FreeType library instance. Returns 0 on failure. */
  def initFreeType(): Long

  /** Destroys a FreeType library instance. */
  def doneFreeType(library: Long): Unit

  // ─── Face lifecycle ─────────────────────────────────────────────────────

  /** Creates a new face from font data in memory. Returns 0 on failure. */
  def newMemoryFace(library: Long, data: Array[Byte], dataSize: Int, faceIndex: Int): Long

  /** Destroys a face. */
  def doneFace(face: Long): Unit

  // ─── Face configuration ─────────────────────────────────────────────────

  /** Selects a bitmap strike by index. Returns true on success. */
  def selectSize(face: Long, strikeIndex: Int): Boolean

  /** Sets the character size in 26.6 fractional points. Returns true on success. */
  def setCharSize(face: Long, charWidth: Int, charHeight: Int, horzRes: Int, vertRes: Int): Boolean

  /** Sets the pixel size. Returns true on success. */
  def setPixelSizes(face: Long, pixelWidth: Int, pixelHeight: Int): Boolean

  // ─── Glyph loading ──────────────────────────────────────────────────────

  /** Loads a glyph by index. Returns true on success. */
  def loadGlyph(face: Long, glyphIndex: Int, loadFlags: Int): Boolean

  /** Loads a glyph by character code. Returns true on success. */
  def loadChar(face: Long, charCode: Int, loadFlags: Int): Boolean

  /** Renders the glyph in the current glyph slot. Returns true on success. */
  def renderGlyph(glyphSlot: Long, renderMode: Int): Boolean

  // ─── Kerning ────────────────────────────────────────────────────────────

  /** Returns true if the face has kerning data. */
  def hasKerning(face: Long): Boolean

  /** Returns the kerning vector x-component for the given glyph pair. */
  def getKerning(face: Long, leftGlyph: Int, rightGlyph: Int, kernMode: Int): Int

  /** Returns the glyph index for a character code. 0 means undefined. */
  def getCharIndex(face: Long, charCode: Int): Int

  // ─── Face metrics ───────────────────────────────────────────────────────

  def getFaceFlags(face: Long): Int
  def getStyleFlags(face: Long): Int
  def getNumGlyphs(face: Long): Int
  def getAscender(face: Long): Int
  def getDescender(face: Long): Int
  def getHeight(face: Long): Int
  def getMaxAdvanceWidth(face: Long): Int
  def getMaxAdvanceHeight(face: Long): Int
  def getUnderlinePosition(face: Long): Int
  def getUnderlineThickness(face: Long): Int

  // ─── Glyph slot access ─────────────────────────────────────────────────

  /** Returns the glyph slot handle from a face. */
  def getGlyphSlot(face: Long): Long

  /** Fills `out` with [width, height, horiBearingX, horiBearingY, horiAdvance]. */
  def getGlyphMetrics(glyphSlot: Long, out: Array[Int]): Unit

  def getGlyphLinearHoriAdvance(glyphSlot: Long): Int
  def getGlyphAdvanceX(glyphSlot: Long): Int
  def getGlyphAdvanceY(glyphSlot: Long): Int
  def getGlyphFormat(glyphSlot: Long): Int

  // ─── Glyph slot bitmap ──────────────────────────────────────────────────

  def getGlyphBitmapRows(glyphSlot: Long): Int
  def getGlyphBitmapWidth(glyphSlot: Long): Int
  def getGlyphBitmapPitch(glyphSlot: Long): Int

  /** Copies the glyph slot bitmap buffer into `buffer`. */
  def getGlyphBitmapBuffer(glyphSlot: Long, buffer: Array[Byte], bufferSize: Int): Unit

  def getGlyphBitmapNumGray(glyphSlot: Long): Int
  def getGlyphBitmapPixelMode(glyphSlot: Long): Int
  def getGlyphBitmapLeft(glyphSlot: Long): Int
  def getGlyphBitmapTop(glyphSlot: Long): Int

  // ─── Size metrics ───────────────────────────────────────────────────────

  /** Fills `out` with [xPpem, yPpem, xScale, yScale, ascender, descender, height, maxAdvance]. */
  def getSizeMetrics(face: Long, out: Array[Int]): Unit

  // ─── Stroker ────────────────────────────────────────────────────────────

  /** Creates a new stroker. Returns 0 on failure. */
  def strokerNew(library: Long): Long

  /** Configures the stroker. */
  def strokerSet(stroker: Long, radius: Int, lineCap: Int, lineJoin: Int, miterLimit: Int): Unit

  /** Destroys a stroker. */
  def strokerDone(stroker: Long): Unit

  // ─── Glyph outline operations ───────────────────────────────────────────

  /** Gets the glyph from the current slot as an FT_Glyph. Returns 0 on failure. */
  def getGlyphAsStroke(glyphSlot: Long): Long

  /** Applies a border stroke to a glyph. Returns a new glyph handle. */
  def strokeBorder(glyph: Long, stroker: Long, inside: Boolean): Long

  /** Converts a glyph to a bitmap glyph. Returns 0 on failure. */
  def glyphToBitmap(glyph: Long, renderMode: Int): Long

  /** Fills `out` with [rows, width, pitch, numGray, pixelMode, left, top] from a bitmap glyph. */
  def getBitmapGlyphBitmap(glyph: Long, out: Array[Int]): Unit

  /** Copies the bitmap glyph buffer into `buffer`. */
  def getBitmapGlyphBuffer(glyph: Long, buffer: Array[Byte], bufferSize: Int): Unit

  /** Destroys a glyph. */
  def doneGlyph(glyph: Long): Unit

  // ─── Error ──────────────────────────────────────────────────────────────

  /** Returns the last FreeType error code. */
  def getLastErrorCode(): Int
}
