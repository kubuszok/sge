// SGE - Scala Game Engine
// Copyright 2024-2026 Mateusz Kubuszok
// Licensed under the Apache License, Version 2.0
//
// Synchronous pure-Scala BMP decoder for the Scala.js platform.
//
// Companion to PngDecoderJs: restores non-PNG synchronous image decoding on the
// Scala.js baseline (assets are embedded at build time and served
// synchronously, so there is no async browser-Canvas decode path). Output is
// always RGBA8888, matching the gdx2d decode contract (GDX2D_FORMAT_RGBA8888).
//
// Supported subset: Windows BMP (BM) with a BITMAPINFOHEADER (40-byte) or larger
// header, uncompressed BI_RGB, 24-bit (BGR) and 32-bit (BGRA / BGRX). Both
// bottom-up (positive height) and top-down (negative height) row orders are
// handled. Anything else — RLE compression, BI_BITFIELDS, 1/4/8-bit palettized
// images, OS/2 BITMAPCOREHEADER — returns None so the caller falls through.

package sge
package platform

private[platform] object BmpDecoderJs {

  /** Decodes a BMP byte range to an RGBA8888 result, or None if `data` is not a
    * BMP this decoder understands (so the caller can fall back to its own error
    * path).
    */
  def decode(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    // Magic bytes: 'B','M'.
    val hasSignature = len >= 2 && data(offset) == 'B'.toByte && data(offset + 1) == 'M'.toByte
    if (!hasSignature) None
    else
      try decodeChecked(data, offset, len)
      catch {
        case _: IndexOutOfBoundsException => None
        case _: BmpError                  => None
      }
  }

  final private class BmpError(msg: String) extends RuntimeException(msg)

  // Little-endian readers over the source array.
  private def u16(d: Array[Byte], p: Int): Int = (d(p) & 0xff) | ((d(p + 1) & 0xff) << 8)
  private def u32(d: Array[Byte], p: Int): Int =
    (d(p) & 0xff) | ((d(p + 1) & 0xff) << 8) | ((d(p + 2) & 0xff) << 16) | ((d(p + 3) & 0xff) << 24)

  private def decodeChecked(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    if (len < 54) None // 14-byte file header + 40-byte BITMAPINFOHEADER minimum
    else {
      // BITMAPFILEHEADER (14 bytes): magic(2), size(4), reserved(4), pixel-data offset(10..13).
      val pixelOffset = u32(data, offset + 10)

      // DIB header.
      val headerSize = u32(data, offset + 14)
      if (headerSize < 40) None // OS/2 BITMAPCOREHEADER (12) unsupported
      else {
        val width        = u32(data, offset + 18)
        val rawHeight    = u32(data, offset + 22)
        val topDown      = rawHeight < 0
        val height       = scala.math.abs(rawHeight)
        val planes       = u16(data, offset + 26)
        val bitCount     = u16(data, offset + 28)
        val compression  = u32(data, offset + 30)

        // BI_RGB == 0. (BI_BITFIELDS == 3 unsupported; RLE 1/2 unsupported.)
        if (planes != 1) None
        else if (compression != 0) None
        else if (bitCount != 24 && bitCount != 32) None
        else if (width <= 0 || height <= 0) None
        else {
          val bytesPerPixel = bitCount / 8
          // Rows are padded to a 4-byte boundary.
          val rowSize  = ((width * bytesPerPixel + 3) / 4) * 4
          val dataBase = offset + pixelOffset
          val needed   = rowSize * height
          if (pixelOffset < 54 || dataBase + needed > offset + len) None
          else {
            val rgba = new Array[Byte](width * height * 4)
            var row  = 0
            while (row < height) {
              // Bottom-up: file row 0 is the bottom image row. Top-down: file row 0 is the top.
              val srcRow = if (topDown) row else height - 1 - row
              val srcOff = dataBase + srcRow * rowSize
              val dstOff = row * width * 4
              var x      = 0
              while (x < width) {
                val sp = srcOff + x * bytesPerPixel
                // BMP stores BGR(A); convert to RGBA.
                val b = data(sp) & 0xff
                val g = data(sp + 1) & 0xff
                val r = data(sp + 2) & 0xff
                // BI_RGB has no defined alpha channel: the 4th byte of a 32-bit
                // BI_RGB pixel is officially "reserved" and is commonly written
                // as 0 (e.g. by ImageIO). Treat 32-bit BI_RGB as fully opaque to
                // avoid a writer's zeroed reserved byte making the whole image
                // transparent. (Alpha-carrying BMPs use BI_BITFIELDS/BI_ALPHA,
                // which this decoder declines above.)
                val a = 0xff
                val dp = dstOff + x * 4
                rgba(dp) = r.toByte
                rgba(dp + 1) = g.toByte
                rgba(dp + 2) = b.toByte
                rgba(dp + 3) = a.toByte
                x += 1
              }
              row += 1
            }
            Some(Gdx2dOps.DecodeResult(width, height, 4, java.nio.ByteBuffer.wrap(rgba)))
          }
        }
      }
    }
  }
}
