// SGE FreeType — Scala.js stub implementation
//
// FreeType native code is not available on Scala.js. Users should
// pre-generate bitmap fonts from TTF/OTF files using the JVM or Native
// backends, then load the resulting .fnt + .png files in the browser.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: all methods throw UnsupportedOperationException
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

private[sge] object FreetypeOpsJs extends FreetypeOps {

  private def unsupported(): Nothing =
    throw new UnsupportedOperationException(
      "FreeType is not available on Scala.js. Pre-generate bitmap fonts on JVM/Native."
    )

  override def initFreeType(): Long                                                                   = unsupported()
  override def doneFreeType(library: Long): Unit                                                      = unsupported()
  override def newMemoryFace(library: Long, data: Array[Byte], dataSize: Int, faceIndex: Int): Long   = unsupported()
  override def doneFace(face: Long): Unit                                                             = unsupported()
  override def selectSize(face: Long, strikeIndex: Int): Boolean                                      = unsupported()
  override def setCharSize(face: Long, charWidth: Int, charHeight: Int, horzRes: Int, vertRes: Int): Boolean = unsupported()
  override def setPixelSizes(face: Long, pixelWidth: Int, pixelHeight: Int): Boolean                  = unsupported()
  override def loadGlyph(face: Long, glyphIndex: Int, loadFlags: Int): Boolean                        = unsupported()
  override def loadChar(face: Long, charCode: Int, loadFlags: Int): Boolean                           = unsupported()
  override def renderGlyph(glyphSlot: Long, renderMode: Int): Boolean                                 = unsupported()
  override def hasKerning(face: Long): Boolean                                                        = unsupported()
  override def getKerning(face: Long, leftGlyph: Int, rightGlyph: Int, kernMode: Int): Int            = unsupported()
  override def getCharIndex(face: Long, charCode: Int): Int                                           = unsupported()
  override def getFaceFlags(face: Long): Int                                                          = unsupported()
  override def getStyleFlags(face: Long): Int                                                         = unsupported()
  override def getNumGlyphs(face: Long): Int                                                          = unsupported()
  override def getAscender(face: Long): Int                                                           = unsupported()
  override def getDescender(face: Long): Int                                                          = unsupported()
  override def getHeight(face: Long): Int                                                             = unsupported()
  override def getMaxAdvanceWidth(face: Long): Int                                                    = unsupported()
  override def getMaxAdvanceHeight(face: Long): Int                                                   = unsupported()
  override def getUnderlinePosition(face: Long): Int                                                  = unsupported()
  override def getUnderlineThickness(face: Long): Int                                                 = unsupported()
  override def getGlyphSlot(face: Long): Long                                                         = unsupported()
  override def getGlyphMetrics(glyphSlot: Long, out: Array[Int]): Unit                                = unsupported()
  override def getGlyphLinearHoriAdvance(glyphSlot: Long): Int                                        = unsupported()
  override def getGlyphAdvanceX(glyphSlot: Long): Int                                                 = unsupported()
  override def getGlyphAdvanceY(glyphSlot: Long): Int                                                 = unsupported()
  override def getGlyphFormat(glyphSlot: Long): Int                                                   = unsupported()
  override def getGlyphBitmapRows(glyphSlot: Long): Int                                               = unsupported()
  override def getGlyphBitmapWidth(glyphSlot: Long): Int                                              = unsupported()
  override def getGlyphBitmapPitch(glyphSlot: Long): Int                                              = unsupported()
  override def getGlyphBitmapBuffer(glyphSlot: Long, buffer: Array[Byte], bufferSize: Int): Unit      = unsupported()
  override def getGlyphBitmapNumGray(glyphSlot: Long): Int                                            = unsupported()
  override def getGlyphBitmapPixelMode(glyphSlot: Long): Int                                          = unsupported()
  override def getGlyphBitmapLeft(glyphSlot: Long): Int                                               = unsupported()
  override def getGlyphBitmapTop(glyphSlot: Long): Int                                                = unsupported()
  override def getSizeMetrics(face: Long, out: Array[Int]): Unit                                      = unsupported()
  override def strokerNew(library: Long): Long                                                        = unsupported()
  override def strokerSet(stroker: Long, radius: Int, lineCap: Int, lineJoin: Int, miterLimit: Int): Unit = unsupported()
  override def strokerDone(stroker: Long): Unit                                                       = unsupported()
  override def getGlyphAsStroke(glyphSlot: Long): Long                                                = unsupported()
  override def strokeBorder(glyph: Long, stroker: Long, inside: Boolean): Long                        = unsupported()
  override def glyphToBitmap(glyph: Long, renderMode: Int): Long                                      = unsupported()
  override def getBitmapGlyphBitmap(glyph: Long, out: Array[Int]): Unit                               = unsupported()
  override def getBitmapGlyphBuffer(glyph: Long, buffer: Array[Byte], bufferSize: Int): Unit          = unsupported()
  override def doneGlyph(glyph: Long): Unit                                                           = unsupported()
  override def getLastErrorCode(): Int                                                                = unsupported()
}
