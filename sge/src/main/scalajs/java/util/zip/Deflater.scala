// java.util.zip.Deflater for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip, so SGE supplies a faithful
// pure-Scala DEFLATE encoder. PixmapIO/anim8 PNG writing relies on a genuinely
// working Deflater on the browser baseline (ISS-533 / ISS-651): the previous
// throwing implementation broke savePNG at runtime on JS.
//
// The encoder emits a standards-compliant RFC 1951 DEFLATE stream using LZ77
// (hash-chain match finder) and fixed Huffman codes, optionally wrapped in the
// RFC 1950 zlib container (2-byte header + Adler-32 trailer) when nowrap=false.
// Any conformant inflater — including the gdx2d PNG decoder used by the round
// trip tests — reconstructs the original bytes exactly. The lower compression
// level controls (NO_COMPRESSION emits stored blocks) are honored so the public
// contract matches java.util.zip.Deflater; the exact byte layout is not required
// to match the JVM (only that it inflates to the identical bytes).
package java.util.zip

import java.io.ByteArrayOutputStream

class Deflater(level: Int, nowrap: Boolean) {
  def this() = this(Deflater.DEFAULT_COMPRESSION, false)
  def this(level: Int) = this(level, false)

  private val input        = new ByteArrayOutputStream()
  private var finishCalled = false
  private var output       = new Array[Byte](0)
  private var outputPos    = 0
  private var produced     = false
  private val adler        = new Adler32()

  def setInput(b: Array[Byte], off: Int, len: Int): Unit = {
    input.write(b, off, len)
    if (!nowrap) adler.update(b, off, len)
  }

  def setInput(b: Array[Byte]): Unit = setInput(b, 0, b.length)

  def setLevel(level: Int): Unit = () // accepted; encoder strategy is level-agnostic except NO_COMPRESSION

  def finish(): Unit = finishCalled = true

  def finished(): Boolean = finishCalled && produced && outputPos >= output.length

  /** Compress accumulated input into `b`. Mirrors java.util.zip.Deflater: returns the number of bytes written, 0 when no more output is currently available.
    */
  def deflate(b: Array[Byte], off: Int, len: Int): Int = {
    // This encoder defers all work to finish() (single-shot): nothing is
    // produced until finish() has been called.
    if (!produced && finishCalled) {
      output = Deflater.compress(input.toByteArray, level, nowrap, adler)
      produced = true
    }
    if (!produced) 0
    else {
      val remaining = output.length - outputPos
      if (remaining <= 0) 0
      else {
        val n = math.min(remaining, len)
        System.arraycopy(output, outputPos, b, off, n)
        outputPos += n
        n
      }
    }
  }

  def deflate(b: Array[Byte]): Int = deflate(b, 0, b.length)

  def end(): Unit = ()

  def reset(): Unit = {
    input.reset()
    finishCalled = false
    output = new Array[Byte](0)
    outputPos = 0
    produced = false
    adler.reset()
  }

  def getTotalIn:  Int = input.size()
  def getTotalOut: Int = outputPos
}

object Deflater {
  val DEFAULT_COMPRESSION: Int = -1
  val BEST_COMPRESSION:    Int = 9
  val BEST_SPEED:          Int = 1
  val NO_COMPRESSION:      Int = 0
  val DEFLATED:            Int = 8

  private val WindowSize = 32768
  private val MinMatch   = 3
  private val MaxMatch   = 258
  private val HashBits   = 15
  private val HashSize   = 1 << HashBits
  private val MaxChain   = 128

  // RFC 1951 fixed-Huffman length codes (257..285): base length and extra bits.
  private val LengthBase  = Array(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
  private val LengthExtra = Array(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
  // RFC 1951 distance codes (0..29): base distance and extra bits.
  private val DistBase  = Array(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
  private val DistExtra = Array(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)

  /** LSB-first bit writer, as required by DEFLATE. */
  final private class BitWriter(out: ByteArrayOutputStream) {
    private var bitBuffer = 0
    private var bitCount  = 0

    def writeBits(value: Int, count: Int): Unit = {
      bitBuffer |= (value & ((1 << count) - 1)) << bitCount
      bitCount += count
      while (bitCount >= 8) {
        out.write(bitBuffer & 0xff)
        bitBuffer >>>= 8
        bitCount -= 8
      }
    }

    /** Huffman codes are transmitted MSB-first; flip the bits then emit LSB-first. */
    def writeHuffman(code: Int, count: Int): Unit = {
      var reversed = 0
      var i        = 0
      var c        = code
      while (i < count) {
        reversed = (reversed << 1) | (c & 1)
        c >>>= 1
        i += 1
      }
      writeBits(reversed, count)
    }

    def flushByte(): Unit =
      if (bitCount > 0) {
        out.write(bitBuffer & 0xff)
        bitBuffer = 0
        bitCount = 0
      }
  }

  // Fixed literal/length Huffman code: lengths per RFC 1951 §3.2.6.
  private def fixedLitCode(sym: Int): (Int, Int) =
    if (sym <= 143) (0x30 + sym, 8)
    else if (sym <= 255) (0x190 + (sym - 144), 9)
    else if (sym <= 279) (0x000 + (sym - 256), 7)
    else (0xc0 + (sym - 280), 8)

  private def lengthCode(len: Int): Int = {
    var code = 28
    while (code > 0 && len < LengthBase(code)) code -= 1
    code
  }

  private def distCode(dist: Int): Int = {
    var code = 29
    while (code > 0 && dist < DistBase(code)) code -= 1
    code
  }

  private def compress(data: Array[Byte], level: Int, nowrap: Boolean, adler: Adler32): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    if (!nowrap) {
      // RFC 1950 zlib header: CMF=0x78 (CM=8, CINFO=7 -> 32K window), FLG chosen so
      // (CMF<<8 | FLG) is a multiple of 31. 0x789c is the canonical default.
      out.write(0x78)
      out.write(0x9c)
    }

    if (level == NO_COMPRESSION) writeStored(data, out)
    else writeFixedHuffman(data, out)

    if (!nowrap) {
      val a = adler.getValue()
      out.write(((a >>> 24) & 0xff).toInt)
      out.write(((a >>> 16) & 0xff).toInt)
      out.write(((a >>> 8) & 0xff).toInt)
      out.write((a & 0xff).toInt)
    }
    out.toByteArray
  }

  /** Stored (BTYPE=00) blocks — used for NO_COMPRESSION. Fully RFC 1951 conformant. */
  private def writeStored(data: Array[Byte], out: ByteArrayOutputStream): Unit = {
    val n   = data.length
    var pos = 0
    if (n == 0) {
      // single empty final stored block
      out.write(0x01) // BFINAL=1, BTYPE=00
      out.write(0x00); out.write(0x00) // LEN=0
      out.write(0xff); out.write(0xff) // ~LEN
    } else {
      while (pos < n) {
        val chunk = math.min(65535, n - pos)
        val last  = pos + chunk >= n
        out.write(if (last) 0x01 else 0x00) // BFINAL flag, BTYPE=00; remaining padding bits are zero
        out.write(chunk & 0xff)
        out.write((chunk >>> 8) & 0xff)
        val nlen = (~chunk) & 0xffff
        out.write(nlen & 0xff)
        out.write((nlen >>> 8) & 0xff)
        out.write(data, pos, chunk)
        pos += chunk
      }
    }
  }

  /** A single fixed-Huffman (BTYPE=01) block with LZ77 back-references. */
  private def writeFixedHuffman(data: Array[Byte], out: ByteArrayOutputStream): Unit = {
    val bw = new BitWriter(out)
    bw.writeBits(1, 1) // BFINAL = 1
    bw.writeBits(1, 2) // BTYPE  = 01 (fixed Huffman)

    val n = data.length
    // Hash-chain match finder (zlib style).
    val head = new Array[Int](HashSize)
    java.util.Arrays.fill(head, -1)
    val prev = new Array[Int](if (n > 0) n else 1)

    def hash(i: Int): Int = {
      val a = data(i) & 0xff
      val b = data(i + 1) & 0xff
      val c = data(i + 2) & 0xff
      ((a << 10) ^ (b << 5) ^ c) & (HashSize - 1)
    }

    def emitLiteral(sym: Int): Unit = {
      val (code, bits) = fixedLitCode(sym)
      bw.writeHuffman(code, bits)
    }

    var i = 0
    while (i < n) {
      var matchLen  = 0
      var matchDist = 0
      if (i + MinMatch <= n) {
        val h         = hash(i)
        var candidate = head(h)
        var chain     = MaxChain
        val maxLen    = math.min(MaxMatch, n - i)
        while (candidate >= 0 && chain > 0) {
          val dist = i - candidate
          if (dist > 0 && dist <= WindowSize) {
            var l = 0
            while (l < maxLen && data(candidate + l) == data(i + l)) l += 1
            if (l > matchLen) {
              matchLen = l
              matchDist = dist
              if (l >= maxLen) chain = 0
            }
          }
          if (chain > 0) {
            candidate = prev(candidate)
            chain -= 1
          }
        }
      }

      if (matchLen >= MinMatch) {
        val lc           = lengthCode(matchLen)
        val (code, bits) = fixedLitCode(257 + lc)
        bw.writeHuffman(code, bits)
        if (LengthExtra(lc) > 0) bw.writeBits(matchLen - LengthBase(lc), LengthExtra(lc))
        val dc = distCode(matchDist)
        bw.writeHuffman(dc, 5) // fixed distance codes are 5 bits, MSB-first
        if (DistExtra(dc) > 0) bw.writeBits(matchDist - DistBase(dc), DistExtra(dc))

        // Insert hash entries for every position covered by the match.
        var j = 0
        while (j < matchLen && i + j + MinMatch <= n) {
          val h = hash(i + j)
          prev(i + j) = head(h)
          head(h) = i + j
          j += 1
        }
        i += matchLen
      } else {
        emitLiteral(data(i) & 0xff)
        if (i + MinMatch <= n) {
          val h = hash(i)
          prev(i) = head(h)
          head(h) = i
        }
        i += 1
      }
    }

    // End-of-block symbol (256), then pad to a byte boundary.
    val (eob, eobBits) = fixedLitCode(256)
    bw.writeHuffman(eob, eobBits)
    bw.flushByte()
  }
}
