// SGE FreeType — Scala Native implementation of FreetypeOps
//
// Binds to the sge_native_ops Rust library using @link/@extern annotations.
// Array data is passed to C functions via .at(offset) to obtain Ptr[Byte]/Ptr[Int].
//
// Migration notes:
//   Origin: LibGDX gdx-freetype extension (JNI native methods)
//   Convention: Scala Native @extern bindings to Rust C ABI
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

import scala.scalanative.unsafe.*

@link("sge_native_ops")
@extern
private object FreetypeC {
  def sge_ft_init_freetype(): Long                                                                        = extern
  def sge_ft_done_freetype(library: Long): Unit                                                           = extern
  def sge_ft_new_memory_face(library: Long, data: Ptr[Byte], dataSize: CInt, faceIndex: CInt): Long      = extern
  def sge_ft_done_face(face: Long): Unit                                                                  = extern
  def sge_ft_select_size(face: Long, strikeIndex: CInt): CInt                                             = extern
  def sge_ft_set_char_size(face: Long, charWidth: CInt, charHeight: CInt, horzRes: CInt, vertRes: CInt): CInt = extern
  def sge_ft_set_pixel_sizes(face: Long, pixelWidth: CInt, pixelHeight: CInt): CInt                       = extern
  def sge_ft_load_glyph(face: Long, glyphIndex: CInt, loadFlags: CInt): CInt                              = extern
  def sge_ft_load_char(face: Long, charCode: CInt, loadFlags: CInt): CInt                                 = extern
  def sge_ft_render_glyph(glyphSlot: Long, renderMode: CInt): CInt                                        = extern
  def sge_ft_has_kerning(face: Long): CInt                                                                = extern
  def sge_ft_get_kerning(face: Long, leftGlyph: CInt, rightGlyph: CInt, kernMode: CInt): CInt             = extern
  def sge_ft_get_char_index(face: Long, charCode: CInt): CInt                                             = extern
  def sge_ft_get_face_flags(face: Long): CInt                                                             = extern
  def sge_ft_get_style_flags(face: Long): CInt                                                            = extern
  def sge_ft_get_num_glyphs(face: Long): CInt                                                             = extern
  def sge_ft_get_ascender(face: Long): CInt                                                               = extern
  def sge_ft_get_descender(face: Long): CInt                                                              = extern
  def sge_ft_get_height(face: Long): CInt                                                                 = extern
  def sge_ft_get_max_advance_width(face: Long): CInt                                                      = extern
  def sge_ft_get_max_advance_height(face: Long): CInt                                                     = extern
  def sge_ft_get_underline_position(face: Long): CInt                                                     = extern
  def sge_ft_get_underline_thickness(face: Long): CInt                                                    = extern
  def sge_ft_get_glyph_slot(face: Long): Long                                                            = extern
  def sge_ft_get_glyph_metrics(glyphSlot: Long, out: Ptr[CInt]): Unit                                    = extern
  def sge_ft_get_glyph_linear_hori_advance(glyphSlot: Long): CInt                                        = extern
  def sge_ft_get_glyph_advance_x(glyphSlot: Long): CInt                                                  = extern
  def sge_ft_get_glyph_advance_y(glyphSlot: Long): CInt                                                  = extern
  def sge_ft_get_glyph_format(glyphSlot: Long): CInt                                                     = extern
  def sge_ft_get_glyph_bitmap_rows(glyphSlot: Long): CInt                                                = extern
  def sge_ft_get_glyph_bitmap_width(glyphSlot: Long): CInt                                               = extern
  def sge_ft_get_glyph_bitmap_pitch(glyphSlot: Long): CInt                                               = extern
  def sge_ft_get_glyph_bitmap_buffer(glyphSlot: Long, buffer: Ptr[Byte], bufferSize: CInt): Unit         = extern
  def sge_ft_get_glyph_bitmap_num_gray(glyphSlot: Long): CInt                                            = extern
  def sge_ft_get_glyph_bitmap_pixel_mode(glyphSlot: Long): CInt                                          = extern
  def sge_ft_get_glyph_bitmap_left(glyphSlot: Long): CInt                                                = extern
  def sge_ft_get_glyph_bitmap_top(glyphSlot: Long): CInt                                                 = extern
  def sge_ft_get_size_metrics(face: Long, out: Ptr[CInt]): Unit                                          = extern
  def sge_ft_stroker_new(library: Long): Long                                                            = extern
  def sge_ft_stroker_set(stroker: Long, radius: CInt, lineCap: CInt, lineJoin: CInt, miterLimit: CInt): Unit = extern
  def sge_ft_stroker_done(stroker: Long): Unit                                                            = extern
  def sge_ft_get_glyph(glyphSlot: Long): Long                                                           = extern
  def sge_ft_stroke_border(glyph: Long, stroker: Long, inside: CInt): Long                             = extern
  def sge_ft_glyph_to_bitmap(glyph: Long, renderMode: CInt): Long                                      = extern
  def sge_ft_get_bitmap_glyph_bitmap(glyph: Long, out: Ptr[CInt]): Unit                                 = extern
  def sge_ft_get_bitmap_glyph_buffer(glyph: Long, buffer: Ptr[Byte], bufferSize: CInt): Unit            = extern
  def sge_ft_done_glyph(glyph: Long): Unit                                                               = extern
  def sge_ft_get_last_error_code(): CInt                                                                   = extern
}

private[sge] object FreetypeOpsNative extends FreetypeOps {

  override def initFreeType(): Long =
    FreetypeC.sge_ft_init_freetype().toLong

  override def doneFreeType(library: Long): Unit =
    FreetypeC.sge_ft_done_freetype(library)

  override def newMemoryFace(library: Long, data: Array[Byte], dataSize: Int, faceIndex: Int): Long =
    FreetypeC.sge_ft_new_memory_face(library, data.at(0), dataSize, faceIndex).toLong

  override def doneFace(face: Long): Unit =
    FreetypeC.sge_ft_done_face(face)

  override def selectSize(face: Long, strikeIndex: Int): Boolean =
    FreetypeC.sge_ft_select_size(face, strikeIndex) != 0

  override def setCharSize(face: Long, charWidth: Int, charHeight: Int, horzRes: Int, vertRes: Int): Boolean =
    FreetypeC.sge_ft_set_char_size(face, charWidth, charHeight, horzRes, vertRes) != 0

  override def setPixelSizes(face: Long, pixelWidth: Int, pixelHeight: Int): Boolean =
    FreetypeC.sge_ft_set_pixel_sizes(face, pixelWidth, pixelHeight) != 0

  override def loadGlyph(face: Long, glyphIndex: Int, loadFlags: Int): Boolean =
    FreetypeC.sge_ft_load_glyph(face, glyphIndex, loadFlags) != 0

  override def loadChar(face: Long, charCode: Int, loadFlags: Int): Boolean =
    FreetypeC.sge_ft_load_char(face, charCode, loadFlags) != 0

  override def renderGlyph(glyphSlot: Long, renderMode: Int): Boolean =
    FreetypeC.sge_ft_render_glyph(glyphSlot, renderMode) != 0

  override def hasKerning(face: Long): Boolean =
    FreetypeC.sge_ft_has_kerning(face) != 0

  override def getKerning(face: Long, leftGlyph: Int, rightGlyph: Int, kernMode: Int): Int =
    FreetypeC.sge_ft_get_kerning(face, leftGlyph, rightGlyph, kernMode)

  override def getCharIndex(face: Long, charCode: Int): Int =
    FreetypeC.sge_ft_get_char_index(face, charCode)

  override def getFaceFlags(face: Long): Int =
    FreetypeC.sge_ft_get_face_flags(face)

  override def getStyleFlags(face: Long): Int =
    FreetypeC.sge_ft_get_style_flags(face)

  override def getNumGlyphs(face: Long): Int =
    FreetypeC.sge_ft_get_num_glyphs(face)

  override def getAscender(face: Long): Int =
    FreetypeC.sge_ft_get_ascender(face)

  override def getDescender(face: Long): Int =
    FreetypeC.sge_ft_get_descender(face)

  override def getHeight(face: Long): Int =
    FreetypeC.sge_ft_get_height(face)

  override def getMaxAdvanceWidth(face: Long): Int =
    FreetypeC.sge_ft_get_max_advance_width(face)

  override def getMaxAdvanceHeight(face: Long): Int =
    FreetypeC.sge_ft_get_max_advance_height(face)

  override def getUnderlinePosition(face: Long): Int =
    FreetypeC.sge_ft_get_underline_position(face)

  override def getUnderlineThickness(face: Long): Int =
    FreetypeC.sge_ft_get_underline_thickness(face)

  override def getGlyphSlot(face: Long): Long =
    FreetypeC.sge_ft_get_glyph_slot(face).toLong

  override def getGlyphMetrics(glyphSlot: Long, out: Array[Int]): Unit =
    FreetypeC.sge_ft_get_glyph_metrics(glyphSlot, out.at(0))

  override def getGlyphLinearHoriAdvance(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_linear_hori_advance(glyphSlot)

  override def getGlyphAdvanceX(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_advance_x(glyphSlot)

  override def getGlyphAdvanceY(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_advance_y(glyphSlot)

  override def getGlyphFormat(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_format(glyphSlot)

  override def getGlyphBitmapRows(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_rows(glyphSlot)

  override def getGlyphBitmapWidth(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_width(glyphSlot)

  override def getGlyphBitmapPitch(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_pitch(glyphSlot)

  override def getGlyphBitmapBuffer(glyphSlot: Long, buffer: Array[Byte], bufferSize: Int): Unit =
    FreetypeC.sge_ft_get_glyph_bitmap_buffer(glyphSlot, buffer.at(0), bufferSize)

  override def getGlyphBitmapNumGray(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_num_gray(glyphSlot)

  override def getGlyphBitmapPixelMode(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_pixel_mode(glyphSlot)

  override def getGlyphBitmapLeft(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_left(glyphSlot)

  override def getGlyphBitmapTop(glyphSlot: Long): Int =
    FreetypeC.sge_ft_get_glyph_bitmap_top(glyphSlot)

  override def getSizeMetrics(face: Long, out: Array[Int]): Unit =
    FreetypeC.sge_ft_get_size_metrics(face, out.at(0))

  override def strokerNew(library: Long): Long =
    FreetypeC.sge_ft_stroker_new(library).toLong

  override def strokerSet(stroker: Long, radius: Int, lineCap: Int, lineJoin: Int, miterLimit: Int): Unit =
    FreetypeC.sge_ft_stroker_set(stroker, radius, lineCap, lineJoin, miterLimit)

  override def strokerDone(stroker: Long): Unit =
    FreetypeC.sge_ft_stroker_done(stroker)

  override def getGlyphAsStroke(glyphSlot: Long): Long =
    FreetypeC.sge_ft_get_glyph(glyphSlot).toLong

  override def strokeBorder(glyph: Long, stroker: Long, inside: Boolean): Long =
    FreetypeC.sge_ft_stroke_border(glyph, stroker, if (inside) 1 else 0).toLong

  override def glyphToBitmap(glyph: Long, renderMode: Int): Long =
    FreetypeC.sge_ft_glyph_to_bitmap(glyph, renderMode).toLong

  override def getBitmapGlyphBitmap(glyph: Long, out: Array[Int]): Unit =
    FreetypeC.sge_ft_get_bitmap_glyph_bitmap(glyph, out.at(0))

  override def getBitmapGlyphBuffer(glyph: Long, buffer: Array[Byte], bufferSize: Int): Unit =
    FreetypeC.sge_ft_get_bitmap_glyph_buffer(glyph, buffer.at(0), bufferSize)

  override def doneGlyph(glyph: Long): Unit =
    FreetypeC.sge_ft_done_glyph(glyph)

  override def getLastErrorCode(): Int =
    FreetypeC.sge_ft_get_last_error_code()
}
