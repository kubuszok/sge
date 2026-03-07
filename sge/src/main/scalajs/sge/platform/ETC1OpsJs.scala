// SGE Native Ops — ETC1 pure Scala implementation for Scala.js
//
// Faithful port of etc1_utils.cpp (Google, Apache 2.0) to pure Scala.
// Produces identical output to the Rust/C++ reference implementation.
// No native code — suitable for browser environments.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: pure Scala ETC1 codec for JS — encode, decode, PKM headers
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

import scala.util.boundary
import scala.util.boundary.break

private[platform] object ETC1OpsJs extends ETC1Ops {

  // ─── Constants and lookup tables ─────────────────────────────────────

  // Intensity modifier sets for ETC1 compressed textures (8 rows x 4 columns)
  private val kModifierTable: Array[Int] = Array(
    /* 0 */ 2, 8, -2, -8,
    /* 1 */ 5, 17, -5, -17,
    /* 2 */ 9, 29, -9, -29,
    /* 3 */ 13, 42, -13, -42,
    /* 4 */ 18, 60, -18, -60,
    /* 5 */ 24, 80, -24, -80,
    /* 6 */ 33, 106, -33, -106,
    /* 7 */ 47, 183, -47, -183
  )

  private val kLookup: Array[Int] = Array(0, 1, 2, 3, -4, -3, -2, -1)

  // ─── Helper functions ────────────────────────────────────────────────

  private def clamp(x: Int): Int =
    if (x >= 0) { if (x < 255) x else 255 }
    else 0

  private def convert4To8(b: Int): Int = {
    val c = b & 0xf
    (c << 4) | c
  }

  private def convert5To8(b: Int): Int = {
    val c = b & 0x1f
    (c << 3) | (c >> 2)
  }

  private def convert6To8(b: Int): Int = {
    val c = b & 0x3f
    (c << 2) | (c >> 4)
  }

  private def divideBy255(d: Int): Int =
    (d + 128 + (d >> 8)) >> 8

  private def convert8To4(b: Int): Int = {
    val c = b & 0xff
    divideBy255(c * 15)
  }

  private def convert8To5(b: Int): Int = {
    val c = b & 0xff
    divideBy255(c * 31)
  }

  private def convertDiff(base: Int, diff: Int): Int =
    convert5To8((0x1f & base) + kLookup(0x7 & diff))

  private def square(x: Int): Int = x * x

  private def inRange4bitSigned(color: Int): Boolean =
    color >= -4 && color <= 3

  // ─── Block decode ────────────────────────────────────────────────────

  /** Decodes one half (subblock) of a 4x4 ETC1 block. Writes RGB888 pixels into pOut. */
  private def decodeSubblock(
    pOut:        Array[Byte],
    outOffset:   Int,
    r:           Int,
    g:           Int,
    b:           Int,
    table:       Array[Int],
    tableOffset: Int,
    low:         Int,
    second:      Boolean,
    flipped:     Boolean
  ): Unit = {
    var baseX = 0
    var baseY = 0
    if (second) {
      if (flipped) { baseY = 2 }
      else { baseX = 2 }
    }
    var i = 0
    while (i < 8) {
      val x: Int =
        if (flipped) { baseX + (i >> 1) }
        else { baseX + (i >> 2) }
      val y: Int =
        if (flipped) { baseY + (i & 1) }
        else { baseY + (i & 3) }
      val k      = y + (x * 4)
      val offset = ((low >>> k) & 1) | ((low >>> (k + 15)) & 2)
      val delta  = table(tableOffset + offset)
      val qBase  = outOffset + 3 * (x + 4 * y)
      pOut(qBase) = clamp(r + delta).toByte
      pOut(qBase + 1) = clamp(g + delta).toByte
      pOut(qBase + 2) = clamp(b + delta).toByte
      i += 1
    }
  }

  /** Decodes a single 8-byte ETC1 block into a 4x4 block of RGB888 pixels (48 bytes). */
  private def decodeBlock(
    pIn:       Array[Byte],
    inOffset:  Int,
    pOut:      Array[Byte],
    outOffset: Int
  ): Unit = {
    // Read high and low 32-bit words from big-endian byte order.
    // Use & 0xff to treat bytes as unsigned.
    val high: Int =
      ((pIn(inOffset) & 0xff) << 24) |
        ((pIn(inOffset + 1) & 0xff) << 16) |
        ((pIn(inOffset + 2) & 0xff) << 8) |
        (pIn(inOffset + 3) & 0xff)
    val low: Int =
      ((pIn(inOffset + 4) & 0xff) << 24) |
        ((pIn(inOffset + 5) & 0xff) << 16) |
        ((pIn(inOffset + 6) & 0xff) << 8) |
        (pIn(inOffset + 7) & 0xff)

    var r1 = 0; var r2 = 0; var g1 = 0; var g2 = 0; var b1 = 0; var b2 = 0
    if ((high & 2) != 0) {
      // differential
      val rBase = high >> 27
      val gBase = high >> 19
      val bBase = high >> 11
      r1 = convert5To8(rBase)
      r2 = convertDiff(rBase, high >> 24)
      g1 = convert5To8(gBase)
      g2 = convertDiff(gBase, high >> 16)
      b1 = convert5To8(bBase)
      b2 = convertDiff(bBase, high >> 8)
    } else {
      // not differential
      r1 = convert4To8(high >> 28)
      r2 = convert4To8(high >> 24)
      g1 = convert4To8(high >> 20)
      g2 = convert4To8(high >> 16)
      b1 = convert4To8(high >> 12)
      b2 = convert4To8(high >> 8)
    }
    val tableIndexA = 7 & (high >> 5)
    val tableIndexB = 7 & (high >> 2)
    val tableAOff   = tableIndexA * 4
    val tableBOff   = tableIndexB * 4
    val flipped     = (high & 1) != 0
    decodeSubblock(pOut, outOffset, r1, g1, b1, kModifierTable, tableAOff, low, second = false, flipped)
    decodeSubblock(pOut, outOffset, r2, g2, b2, kModifierTable, tableBOff, low, second = true, flipped)
  }

  // ─── Block encode ────────────────────────────────────────────────────

  /** Mutable struct matching C's etc_compressed. score is compared as unsigned u32. */
  final private class EtcCompressed {
    var high:  Int = 0
    var low:   Int = 0
    var score: Int = 0 // Treated as unsigned — use Integer.compareUnsigned for comparisons
  }

  /** If b has a lower (unsigned) score than a, copy b into a. */
  private def takeBest(a: EtcCompressed, b: EtcCompressed): Unit =
    if (Integer.compareUnsigned(a.score, b.score) > 0) {
      a.high = b.high
      a.low = b.low
      a.score = b.score
    }

  /** Computes the average color for a subblock (8 pixels). */
  private def averageColorsSubblock(
    pIn:          Array[Byte],
    inOffset:     Int,
    inMask:       Int,
    pColors:      Array[Byte],
    colorsOffset: Int,
    flipped:      Boolean,
    second:       Boolean
  ): Unit = {
    var r = 0
    var g = 0
    var b = 0

    if (flipped) {
      val by = if (second) 2 else 0
      var y  = 0
      while (y < 2) {
        val yy = by + y
        var x  = 0
        while (x < 4) {
          val i = x + 4 * yy
          if ((inMask & (1 << i)) != 0) {
            val p = inOffset + i * 3
            r += (pIn(p) & 0xff)
            g += (pIn(p + 1) & 0xff)
            b += (pIn(p + 2) & 0xff)
          }
          x += 1
        }
        y += 1
      }
    } else {
      val bx = if (second) 2 else 0
      var y  = 0
      while (y < 4) {
        var x = 0
        while (x < 2) {
          val xx = bx + x
          val i  = xx + 4 * y
          if ((inMask & (1 << i)) != 0) {
            val p = inOffset + i * 3
            r += (pIn(p) & 0xff)
            g += (pIn(p + 1) & 0xff)
            b += (pIn(p + 2) & 0xff)
          }
          x += 1
        }
        y += 1
      }
    }
    pColors(colorsOffset) = ((r + 4) >> 3).toByte
    pColors(colorsOffset + 1) = ((g + 4) >> 3).toByte
    pColors(colorsOffset + 2) = ((b + 4) >> 3).toByte
  }

  /** Chooses the best modifier index for a single pixel, accumulating into pLow. Returns the per-pixel score (unsigned). Uses Long internally for score arithmetic.
    */
  private def chooseModifier(
    pBaseColors:    Array[Byte],
    baseOffset:     Int,
    pIn:            Array[Byte],
    inOffset:       Int,
    pLow:           Array[Int],
    bitIndex:       Int,
    pModifierTable: Array[Int],
    modTableOffset: Int
  ): Int = {
    // ~0 as unsigned int32 = 0xFFFFFFFF
    var bestScore: Long = 0xffffffffL
    var bestIndex = 0
    val pixelR    = pIn(inOffset) & 0xff
    val pixelG    = pIn(inOffset + 1) & 0xff
    val pixelB    = pIn(inOffset + 2) & 0xff
    val r         = pBaseColors(baseOffset) & 0xff
    val g         = pBaseColors(baseOffset + 1) & 0xff
    val b         = pBaseColors(baseOffset + 2) & 0xff
    var i         = 0
    while (i < 4) {
      val modifier = pModifierTable(modTableOffset + i)
      val decodedG = clamp(g + modifier)
      var score: Long = (6 * square(decodedG - pixelG)).toLong & 0xffffffffL
      if (score < bestScore) {
        val decodedR = clamp(r + modifier)
        score += (3 * square(decodedR - pixelR)).toLong & 0xffffffffL
        if (score < bestScore) {
          val decodedB = clamp(b + modifier)
          score += square(decodedB - pixelB).toLong & 0xffffffffL
          if (score < bestScore) {
            bestScore = score
            bestIndex = i
          }
        }
      }
      i += 1
    }
    val lowMask = (((bestIndex >> 1) << 16) | (bestIndex & 1)) << bitIndex
    pLow(0) = pLow(0) | lowMask
    bestScore.toInt
  }

  /** Encodes a subblock, accumulating score and low bits in pCompressed. */
  private def encodeSubblockHelper(
    pIn:            Array[Byte],
    inOffset:       Int,
    inMask:         Int,
    compHigh:       Int,
    compLow:        Array[Int], // single-element array to allow mutation
    compScore:      Array[Long], // single-element array (unsigned score as Long)
    flipped:        Boolean,
    second:         Boolean,
    pBaseColors:    Array[Byte],
    baseOffset:     Int,
    pModifierTable: Array[Int],
    modTableOffset: Int
  ): Unit =
    if (flipped) {
      val by = if (second) 2 else 0
      var y  = 0
      while (y < 2) {
        val yy = by + y
        var x  = 0
        while (x < 4) {
          val i = x + 4 * yy
          if ((inMask & (1 << i)) != 0) {
            val s = chooseModifier(
              pBaseColors,
              baseOffset,
              pIn,
              inOffset + i * 3,
              compLow,
              yy + x * 4,
              pModifierTable,
              modTableOffset
            )
            compScore(0) = compScore(0) + (s.toLong & 0xffffffffL)
          }
          x += 1
        }
        y += 1
      }
    } else {
      val bx = if (second) 2 else 0
      var y  = 0
      while (y < 4) {
        var x = 0
        while (x < 2) {
          val xx = bx + x
          val i  = xx + 4 * y
          if ((inMask & (1 << i)) != 0) {
            val s = chooseModifier(
              pBaseColors,
              baseOffset,
              pIn,
              inOffset + i * 3,
              compLow,
              y + xx * 4,
              pModifierTable,
              modTableOffset
            )
            compScore(0) = compScore(0) + (s.toLong & 0xffffffffL)
          }
          x += 1
        }
        y += 1
      }
    }

  /** Encodes the base colors for both subblocks, setting the high bits. */
  private def encodeBaseColors(
    pBaseColors:  Array[Byte],
    baseOffset:   Int,
    pColors:      Array[Byte],
    colorsOffset: Int,
    pCompressed:  EtcCompressed
  ): Unit = {
    var r1           = 0; var g1 = 0; var b1 = 0
    var r2           = 0; var g2 = 0; var b2 = 0
    var differential = false

    {
      val r51 = convert8To5(pColors(colorsOffset) & 0xff)
      val g51 = convert8To5(pColors(colorsOffset + 1) & 0xff)
      val b51 = convert8To5(pColors(colorsOffset + 2) & 0xff)
      val r52 = convert8To5(pColors(colorsOffset + 3) & 0xff)
      val g52 = convert8To5(pColors(colorsOffset + 4) & 0xff)
      val b52 = convert8To5(pColors(colorsOffset + 5) & 0xff)

      r1 = convert5To8(r51)
      g1 = convert5To8(g51)
      b1 = convert5To8(b51)

      val dr = r52 - r51
      val dg = g52 - g51
      val db = b52 - b51

      differential = inRange4bitSigned(dr) && inRange4bitSigned(dg) && inRange4bitSigned(db)
      if (differential) {
        r2 = convert5To8(r51 + dr)
        g2 = convert5To8(g51 + dg)
        b2 = convert5To8(b51 + db)
        pCompressed.high = pCompressed.high |
          (r51 << 27) | ((7 & dr) << 24) |
          (g51 << 19) | ((7 & dg) << 16) |
          (b51 << 11) | ((7 & db) << 8) | 2
      }
    }

    if (!differential) {
      val r41 = convert8To4(pColors(colorsOffset) & 0xff)
      val g41 = convert8To4(pColors(colorsOffset + 1) & 0xff)
      val b41 = convert8To4(pColors(colorsOffset + 2) & 0xff)
      val r42 = convert8To4(pColors(colorsOffset + 3) & 0xff)
      val g42 = convert8To4(pColors(colorsOffset + 4) & 0xff)
      val b42 = convert8To4(pColors(colorsOffset + 5) & 0xff)
      r1 = convert4To8(r41)
      g1 = convert4To8(g41)
      b1 = convert4To8(b41)
      r2 = convert4To8(r42)
      g2 = convert4To8(g42)
      b2 = convert4To8(b42)
      pCompressed.high = pCompressed.high |
        (r41 << 28) | (r42 << 24) |
        (g41 << 20) | (g42 << 16) |
        (b41 << 12) | (b42 << 8)
    }
    pBaseColors(baseOffset) = r1.toByte
    pBaseColors(baseOffset + 1) = g1.toByte
    pBaseColors(baseOffset + 2) = b1.toByte
    pBaseColors(baseOffset + 3) = r2.toByte
    pBaseColors(baseOffset + 4) = g2.toByte
    pBaseColors(baseOffset + 5) = b2.toByte
  }

  /** Encodes a complete 4x4 block with one flip orientation, trying all 8x8 table combinations. */
  private def encodeBlockHelper(
    pIn:          Array[Byte],
    inOffset:     Int,
    inMask:       Int,
    pColors:      Array[Byte],
    colorsOffset: Int,
    pCompressed:  EtcCompressed,
    flipped:      Boolean
  ): Unit = {
    // Initialize with worst possible score (unsigned max)
    pCompressed.score = ~0 // 0xFFFFFFFF as signed int, represents unsigned max
    pCompressed.high = if (flipped) 1 else 0
    pCompressed.low = 0

    val pBaseColors = new Array[Byte](6)
    encodeBaseColors(pBaseColors, 0, pColors, colorsOffset, pCompressed)

    val originalHigh = pCompressed.high

    // Try all 8 modifier tables for the first subblock
    var i = 0
    while (i < 8) {
      val tempLow   = Array(0)
      val tempScore = Array(0L)
      val tempHigh  = originalHigh | (i << 5)
      encodeSubblockHelper(
        pIn,
        inOffset,
        inMask,
        tempHigh,
        tempLow,
        tempScore,
        flipped,
        second = false,
        pBaseColors,
        0,
        kModifierTable,
        i * 4
      )
      // Compare as unsigned: tempScore(0) vs pCompressed.score as unsigned
      if (Integer.compareUnsigned(tempScore(0).toInt, pCompressed.score) < 0) {
        pCompressed.high = tempHigh
        pCompressed.low = tempLow(0)
        pCompressed.score = tempScore(0).toInt
      }
      i += 1
    }

    // Save the best first-half result
    val firstHalfHigh  = pCompressed.high
    val firstHalfLow   = pCompressed.low
    val firstHalfScore = pCompressed.score

    // Try all 8 modifier tables for the second subblock
    i = 0
    while (i < 8) {
      val tempLow   = Array(firstHalfLow)
      val tempScore = Array(firstHalfScore.toLong & 0xffffffffL)
      val tempHigh  = firstHalfHigh | (i << 2)
      encodeSubblockHelper(
        pIn,
        inOffset,
        inMask,
        tempHigh,
        tempLow,
        tempScore,
        flipped,
        second = true,
        pBaseColors,
        3,
        kModifierTable,
        i * 4
      )
      if (i == 0) {
        pCompressed.high = tempHigh
        pCompressed.low = tempLow(0)
        pCompressed.score = tempScore(0).toInt
      } else {
        if (Integer.compareUnsigned(tempScore(0).toInt, pCompressed.score) < 0) {
          pCompressed.high = tempHigh
          pCompressed.low = tempLow(0)
          pCompressed.score = tempScore(0).toInt
        }
      }
      i += 1
    }
  }

  /** Writes a 32-bit int as 4 big-endian bytes. */
  private def writeBigEndian(pOut: Array[Byte], offset: Int, d: Int): Unit = {
    pOut(offset) = (d >> 24).toByte
    pOut(offset + 1) = (d >> 16).toByte
    pOut(offset + 2) = (d >> 8).toByte
    pOut(offset + 3) = d.toByte
  }

  /** Encodes a 4x4 block of RGB888 pixels (48 bytes) into 8 bytes of ETC1 compressed data. */
  private def encodeBlock(
    pIn:       Array[Byte],
    inOffset:  Int,
    inMask:    Int,
    pOut:      Array[Byte],
    outOffset: Int
  ): Unit = {
    val colors        = new Array[Byte](6)
    val flippedColors = new Array[Byte](6)
    averageColorsSubblock(pIn, inOffset, inMask, colors, 0, flipped = false, second = false)
    averageColorsSubblock(pIn, inOffset, inMask, colors, 3, flipped = false, second = true)
    averageColorsSubblock(pIn, inOffset, inMask, flippedColors, 0, flipped = true, second = false)
    averageColorsSubblock(pIn, inOffset, inMask, flippedColors, 3, flipped = true, second = true)

    val a = new EtcCompressed
    val b = new EtcCompressed
    encodeBlockHelper(pIn, inOffset, inMask, colors, 0, a, flipped = false)
    encodeBlockHelper(pIn, inOffset, inMask, flippedColors, 0, b, flipped = true)
    takeBest(a, b)
    writeBigEndian(pOut, outOffset, a.high)
    writeBigEndian(pOut, outOffset + 4, a.low)
  }

  // ─── Image encode/decode ─────────────────────────────────────────────

  private val kYMask: Array[Int] = Array(0x0, 0xf, 0xff, 0xfff, 0xffff)
  private val kXMask: Array[Int] = Array(0x0, 0x1111, 0x3333, 0x7777, 0xffff)

  override def getCompressedDataSize(width: Int, height: Int): Int =
    (((width + 3) & ~3) * ((height + 3) & ~3)) >> 1

  override def encodeImage(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    if (pixelSize < 2 || pixelSize > 3) {
      throw new IllegalArgumentException(s"pixelSize must be 2 or 3, got $pixelSize")
    }

    val stride         = width * pixelSize
    val encodedWidth   = (width + 3) & ~3
    val encodedHeight  = (height + 3) & ~3
    val compressedSize = getCompressedDataSize(width, height)
    val out            = new Array[Byte](compressedSize)

    val block  = new Array[Byte](DECODED_BLOCK_SIZE)
    var outPos = 0

    var y = 0
    while (y < encodedHeight) {
      var yEnd = height - y
      if (yEnd > 4) { yEnd = 4 }
      val ymask = kYMask(yEnd)
      var x     = 0
      while (x < encodedWidth) {
        var xEnd = width - x
        if (xEnd > 4) { xEnd = 4 }
        val mask = ymask & kXMask(xEnd)

        var cy = 0
        while (cy < yEnd) {
          val qBase = (cy * 4) * 3
          val pBase = offset + pixelSize * x + stride * (y + cy)
          if (pixelSize == 3) {
            System.arraycopy(imageData, pBase, block, qBase, xEnd * 3)
          } else {
            var cx = 0
            var q  = qBase
            var p  = pBase
            while (cx < xEnd) {
              val pixel = ((imageData(p + 1) & 0xff) << 8) | (imageData(p) & 0xff)
              block(q) = convert5To8(pixel >> 11).toByte
              block(q + 1) = convert6To8(pixel >> 5).toByte
              block(q + 2) = convert5To8(pixel).toByte
              q += 3
              p += pixelSize
              cx += 1
            }
          }
          cy += 1
        }
        encodeBlock(block, 0, mask, out, outPos)
        outPos += ENCODED_BLOCK_SIZE
        x += 4
      }
      y += 4
    }
    out
  }

  override def decodeImage(
    compressedData:   Array[Byte],
    compressedOffset: Int,
    decodedData:      Array[Byte],
    decodedOffset:    Int,
    width:            Int,
    height:           Int,
    pixelSize:        Int
  ): Unit = {
    if (pixelSize < 2 || pixelSize > 3) {
      throw new IllegalArgumentException(s"pixelSize must be 2 or 3, got $pixelSize")
    }

    val stride        = width * pixelSize
    val encodedWidth  = (width + 3) & ~3
    val encodedHeight = (height + 3) & ~3

    val block = new Array[Byte](DECODED_BLOCK_SIZE)
    var inPos = compressedOffset

    var y = 0
    while (y < encodedHeight) {
      var yEnd = height - y
      if (yEnd > 4) { yEnd = 4 }
      var x = 0
      while (x < encodedWidth) {
        var xEnd = width - x
        if (xEnd > 4) { xEnd = 4 }
        decodeBlock(compressedData, inPos, block, 0)
        inPos += ENCODED_BLOCK_SIZE

        var cy = 0
        while (cy < yEnd) {
          val qBase = (cy * 4) * 3
          val pBase = decodedOffset + pixelSize * x + stride * (y + cy)
          if (pixelSize == 3) {
            System.arraycopy(block, qBase, decodedData, pBase, xEnd * 3)
          } else {
            var cx = 0
            var q  = qBase
            var p  = pBase
            while (cx < xEnd) {
              val r     = block(q) & 0xff
              val g     = block(q + 1) & 0xff
              val b     = block(q + 2) & 0xff
              val pixel = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3)
              decodedData(p) = pixel.toByte
              decodedData(p + 1) = (pixel >> 8).toByte
              q += 3
              p += pixelSize
              cx += 1
            }
          }
          cy += 1
        }
        x += 4
      }
      y += 4
    }
  }

  // ─── PKM header ──────────────────────────────────────────────────────

  private val PKM_MAGIC: Array[Byte] =
    Array('P'.toByte, 'K'.toByte, 'M'.toByte, ' '.toByte, '1'.toByte, '0'.toByte)

  private val ETC1_PKM_FORMAT_OFFSET         = 6
  private val ETC1_PKM_ENCODED_WIDTH_OFFSET  = 8
  private val ETC1_PKM_ENCODED_HEIGHT_OFFSET = 10
  private val ETC1_PKM_WIDTH_OFFSET          = 12
  private val ETC1_PKM_HEIGHT_OFFSET         = 14

  private val ETC1_RGB_NO_MIPMAPS = 0

  private def writeBEUint16(pOut: Array[Byte], offset: Int, data: Int): Unit = {
    pOut(offset) = (data >> 8).toByte
    pOut(offset + 1) = data.toByte
  }

  private def readBEUint16(pIn: Array[Byte], offset: Int): Int =
    ((pIn(offset) & 0xff) << 8) | (pIn(offset + 1) & 0xff)

  override def formatHeader(header: Array[Byte], offset: Int, width: Int, height: Int): Unit = {
    System.arraycopy(PKM_MAGIC, 0, header, offset, PKM_MAGIC.length)
    val encodedWidth  = (width + 3) & ~3
    val encodedHeight = (height + 3) & ~3
    writeBEUint16(header, offset + ETC1_PKM_FORMAT_OFFSET, ETC1_RGB_NO_MIPMAPS)
    writeBEUint16(header, offset + ETC1_PKM_ENCODED_WIDTH_OFFSET, encodedWidth)
    writeBEUint16(header, offset + ETC1_PKM_ENCODED_HEIGHT_OFFSET, encodedHeight)
    writeBEUint16(header, offset + ETC1_PKM_WIDTH_OFFSET, width)
    writeBEUint16(header, offset + ETC1_PKM_HEIGHT_OFFSET, height)
  }

  override def isValidPKM(header: Array[Byte], offset: Int): Boolean = boundary {
    // Check magic bytes
    var i = 0
    while (i < PKM_MAGIC.length) {
      if (header(offset + i) != PKM_MAGIC(i)) break(false)
      i += 1
    }
    val format        = readBEUint16(header, offset + ETC1_PKM_FORMAT_OFFSET)
    val encodedWidth  = readBEUint16(header, offset + ETC1_PKM_ENCODED_WIDTH_OFFSET)
    val encodedHeight = readBEUint16(header, offset + ETC1_PKM_ENCODED_HEIGHT_OFFSET)
    val width         = readBEUint16(header, offset + ETC1_PKM_WIDTH_OFFSET)
    val height        = readBEUint16(header, offset + ETC1_PKM_HEIGHT_OFFSET)
    format == ETC1_RGB_NO_MIPMAPS &&
    encodedWidth >= width && encodedWidth - width < 4 &&
    encodedHeight >= height && encodedHeight - height < 4
  }

  override def getWidthPKM(header: Array[Byte], offset: Int): Int =
    readBEUint16(header, offset + ETC1_PKM_WIDTH_OFFSET)

  override def getHeightPKM(header: Array[Byte], offset: Int): Int =
    readBEUint16(header, offset + ETC1_PKM_HEIGHT_OFFSET)

  override def encodeImagePKM(
    imageData: Array[Byte],
    offset:    Int,
    width:     Int,
    height:    Int,
    pixelSize: Int
  ): Array[Byte] = {
    val compressedSize = getCompressedDataSize(width, height)
    val result         = new Array[Byte](PKM_HEADER_SIZE + compressedSize)
    formatHeader(result, 0, width, height)
    val compressed = encodeImage(imageData, offset, width, height, pixelSize)
    System.arraycopy(compressed, 0, result, PKM_HEADER_SIZE, compressedSize)
    result
  }
}
