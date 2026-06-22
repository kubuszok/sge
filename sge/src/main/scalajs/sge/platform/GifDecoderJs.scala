// SGE - Scala Game Engine
// Copyright 2024-2026 Mateusz Kubuszok
// Licensed under the Apache License, Version 2.0
//
// Synchronous pure-Scala GIF decoder for the Scala.js platform.
//
// Companion to PngDecoderJs: restores non-PNG synchronous image decoding on the
// Scala.js baseline (assets are embedded at build time and served
// synchronously, so there is no async browser-Canvas decode path). Output is
// always RGBA8888, matching the gdx2d decode contract (GDX2D_FORMAT_RGBA8888).
//
// Supported subset: GIF87a and GIF89a, the FIRST image frame only (static
// image; animation / multi-frame is out of scope), LZW decompression, a global
// and/or local color table, and the transparent color index from a Graphic
// Control Extension (mapped to alpha 0). Interlaced frames are de-interlaced.
// Anything malformed returns None so the caller falls through.

package sge
package platform

private[platform] object GifDecoderJs {

  /** Decodes a GIF byte range to an RGBA8888 result (first frame), or None if `data` is not a GIF this decoder understands.
    */
  def decode(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    val hasSignature =
      len >= 6 &&
        data(offset) == 'G'.toByte && data(offset + 1) == 'I'.toByte && data(offset + 2) == 'F'.toByte &&
        data(offset + 3) == '8'.toByte &&
        (data(offset + 4) == '7'.toByte || data(offset + 4) == '9'.toByte) &&
        data(offset + 5) == 'a'.toByte
    if (!hasSignature) None
    else
      try decodeChecked(data, offset, len)
      catch {
        case _: IndexOutOfBoundsException => None
        case _: GifError                  => None
      }
  }

  final private class GifError(msg: String) extends RuntimeException(msg)

  // Cursor over the source byte range; little-endian for multi-byte fields.
  final private class Reader(data: Array[Byte], val base: Int, val end: Int) {
    var pos:  Int = base
    def u8(): Int = {
      if (pos >= end) throw new GifError("EOF")
      val v = data(pos) & 0xff
      pos += 1
      v
    }
    def u16(): Int = {
      val lo = u8()
      val hi = u8()
      lo | (hi << 8)
    }
    def skip(n: Int): Unit = {
      if (pos + n > end) throw new GifError("EOF")
      pos += n
    }
    def readBytes(n: Int): Array[Byte] = {
      if (pos + n > end) throw new GifError("EOF")
      val a = new Array[Byte](n)
      System.arraycopy(data, pos, a, 0, n)
      pos += n
      a
    }
    def remaining: Int = end - pos
  }

  private def decodeChecked(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    val r = new Reader(data, offset, offset + len)
    r.skip(6) // signature "GIF87a"/"GIF89a"

    // Logical Screen Descriptor.
    r.u16() // logical screen width (unused; the frame descriptor defines the image)
    r.u16() // logical screen height
    val packed = r.u8()
    r.u8() // background color index
    r.u8() // pixel aspect ratio
    val gctFlag = (packed & 0x80) != 0
    val gctSize = 2 << (packed & 0x07)

    // Global Color Table (RGB triplets).
    val globalCt: Array[Byte] = if (gctFlag) r.readBytes(gctSize * 3) else Array.emptyByteArray

    var transparentIndex = -1

    // Walk blocks until the first Image Descriptor (0x2C); honor any preceding
    // Graphic Control Extension for the transparent color index.
    var result: Option[Gdx2dOps.DecodeResult] = None
    scala.util.boundary {
      while (true) {
        if (r.remaining < 1) scala.util.boundary.break()
        val sep = r.u8()
        sep match {
          case 0x21 => // Extension Introducer
            val label = r.u8()
            if (label == 0xf9) {
              // Graphic Control Extension: 1-byte size, packed, delay(2), tIndex, terminator.
              r.u8() // block size (always 4)
              val gcPacked = r.u8()
              r.u16() // delay time
              val tIndex = r.u8()
              r.u8() // block terminator
              if ((gcPacked & 0x01) != 0) transparentIndex = tIndex
            } else {
              // Other extension (comment, application, plain text): skip sub-blocks.
              skipSubBlocks(r)
            }
          case 0x2c => // Image Descriptor — decode this (first) frame and stop.
            result = Some(decodeFrame(r, globalCt, transparentIndex))
            scala.util.boundary.break()
          case 0x3b => // Trailer
            scala.util.boundary.break()
          case _ =>
            scala.util.boundary.break() // unknown block; bail cleanly
        }
      }
    }
    result
  }

  /** Skip a chain of GIF sub-blocks (each: 1-byte length, then that many bytes; terminated by a 0-length block). */
  private def skipSubBlocks(r: Reader): Unit =
    scala.util.boundary {
      while (true) {
        val n = r.u8()
        if (n == 0) scala.util.boundary.break()
        r.skip(n)
      }
    }

  private def decodeFrame(
    r:                Reader,
    globalCt:         Array[Byte],
    transparentIndex: Int
  ): Gdx2dOps.DecodeResult = {
    r.u16() // image left position
    r.u16() // image top position
    val w         = r.u16()
    val h         = r.u16()
    val packed    = r.u8()
    val lctFlag   = (packed & 0x80) != 0
    val interlace = (packed & 0x40) != 0
    val lctSize   = 2 << (packed & 0x07)

    val colorTable: Array[Byte] = if (lctFlag) r.readBytes(lctSize * 3) else globalCt
    if (colorTable.length == 0) throw new GifError("no color table")
    if (w <= 0 || h <= 0) throw new GifError("empty frame")

    // LZW minimum code size, then sub-blocks of compressed data.
    val minCodeSize = r.u8()
    val lzwData     = readSubBlocksConcat(r)

    val indices = lzwDecode(lzwData, minCodeSize, w * h)

    // Map indices through the color table, applying interlacing and transparency.
    val rgba       = new Array[Byte](w * h * 4)
    val colorCount = colorTable.length / 3

    var i = 0
    while (i < w * h) {
      val srcRow = i / w
      val col    = i % w
      val dstRow = if (interlace) deinterlaceRow(srcRow, h) else srcRow
      val di     = (dstRow * w + col) * 4
      val idx    = indices(i) & 0xff
      if (idx == transparentIndex) {
        rgba(di) = 0; rgba(di + 1) = 0; rgba(di + 2) = 0; rgba(di + 3) = 0
      } else if (idx < colorCount) {
        rgba(di) = colorTable(idx * 3)
        rgba(di + 1) = colorTable(idx * 3 + 1)
        rgba(di + 2) = colorTable(idx * 3 + 2)
        rgba(di + 3) = 0xff.toByte
      } else {
        // Index past the color table: treat as opaque black.
        rgba(di) = 0; rgba(di + 1) = 0; rgba(di + 2) = 0; rgba(di + 3) = 0xff.toByte
      }
      i += 1
    }

    Gdx2dOps.DecodeResult(w, h, 4, java.nio.ByteBuffer.wrap(rgba))
  }

  /** GIF interlacing: 4 passes. Translate a sequential (stored) row index to its image row. */
  private def deinterlaceRow(storedRow: Int, height: Int): Int = {
    // Rows produced per pass: start offset / step.
    //   pass1: start 0, step 8
    //   pass2: start 4, step 8
    //   pass3: start 2, step 4
    //   pass4: start 1, step 2
    val pass1Count = (height - 0 + 7) / 8
    val pass2Count = if (height > 4) (height - 4 + 7) / 8 else 0
    val pass3Count = if (height > 2) (height - 2 + 3) / 4 else 0
    if (storedRow < pass1Count) storedRow * 8
    else if (storedRow < pass1Count + pass2Count) 4 + (storedRow - pass1Count) * 8
    else if (storedRow < pass1Count + pass2Count + pass3Count) 2 + (storedRow - pass1Count - pass2Count) * 4
    else 1 + (storedRow - pass1Count - pass2Count - pass3Count) * 2
  }

  /** Concatenate all sub-blocks (the LZW data stream) into one array. */
  private def readSubBlocksConcat(r: Reader): Array[Byte] = {
    val out = new java.io.ByteArrayOutputStream()
    scala.util.boundary {
      while (true) {
        val n = r.u8()
        if (n == 0) scala.util.boundary.break()
        out.write(r.readBytes(n), 0, n)
      }
    }
    out.toByteArray
  }

  /** Variable-width LZW decode (GIF flavor), producing up to `expected` index bytes.
    *
    * Standard GIF LZW: codes start at minCodeSize+1 bits and grow as the dictionary fills; a clear code resets the dictionary, an end-of-information code stops the stream. Dictionary entries are
    * (prefix-code, suffix-byte) pairs expanded via a stack.
    */
  private def lzwDecode(input: Array[Byte], minCodeSize: Int, expected: Int): Array[Byte] = {
    val clearCode = 1 << minCodeSize
    val eoiCode   = clearCode + 1
    val maxCodes  = 4096

    val prefix = new Array[Int](maxCodes) // prefix code for each dictionary entry (-1 for roots)
    val suffix = new Array[Int](maxCodes) // suffix byte for each dictionary entry
    val stack  = new Array[Int](maxCodes + 1)

    val out    = new Array[Byte](expected)
    var outPos = 0

    var codeSize = minCodeSize + 1
    var next     = eoiCode + 1

    // Bit reader (LSB-first within each byte, codes span byte boundaries).
    var bitBuf = 0
    var bitCnt = 0
    var inPos  = 0

    var oldCode = -1

    scala.util.boundary {
      while (outPos < expected) {
        // Read one code of the current width.
        var enoughBits = true
        while (bitCnt < codeSize && enoughBits)
          if (inPos >= input.length) enoughBits = false
          else {
            bitBuf |= (input(inPos) & 0xff) << bitCnt
            bitCnt += 8
            inPos += 1
          }
        if (!enoughBits && bitCnt < codeSize) scala.util.boundary.break() // out of data
        val code = bitBuf & ((1 << codeSize) - 1)
        bitBuf >>>= codeSize
        bitCnt -= codeSize

        if (code == eoiCode) scala.util.boundary.break()
        else if (code == clearCode) {
          codeSize = minCodeSize + 1
          next = eoiCode + 1
          oldCode = -1
        } else {
          // Determine the code to expand and the new first symbol.
          val expandCode =
            if (oldCode == -1) code // first code after clear: must be a root
            else if (code < next) code
            else oldCode // KwKwK: code not yet in dictionary; expand oldCode then append firstSym

          // Walk the prefix chain, pushing suffix bytes onto the stack.
          var sp = 0
          var c  = expandCode
          while (c >= clearCode) { // roots are < clearCode (single bytes)
            stack(sp) = suffix(c)
            sp += 1
            c = prefix(c)
          }
          stack(sp) = c // the root byte
          sp += 1
          val first = c // first byte of expandCode's (and of the emitted) string

          // Output the expansion of expandCode (the stack holds it reversed).
          var k = sp - 1
          while (k >= 0) {
            if (outPos < out.length) { out(outPos) = stack(k).toByte; outPos += 1 }
            k -= 1
          }
          // KwKwK: the true string was oldString + first, so append `first`.
          val isKwKwK = oldCode != -1 && code >= next
          if (isKwKwK) {
            if (outPos < out.length) { out(outPos) = first.toByte; outPos += 1 }
          }

          // Add a new dictionary entry: oldCode + first (first byte of the
          // string just emitted). Skipped for the first code after a clear,
          // which has no prefix yet.
          if (oldCode != -1 && next < maxCodes) {
            prefix(next) = oldCode
            suffix(next) = first
            next += 1
            if (next == (1 << codeSize) && codeSize < 12) codeSize += 1
          }
          oldCode = code
        }
      }
    }

    out
  }
}
