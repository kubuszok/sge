// java.util.zip.Inflater for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala RFC 1951 INFLATE decoder (with RFC 1950 zlib unwrapping when
// nowrap=false). It mirrors enough of the java.util.zip.Inflater contract that
// InflaterInputStream and the synchronous JS PNG decoder can round-trip the
// streams produced by Deflater and by real zlib/PNG encoders (ISS-533 /
// ISS-651). The previous throwing implementations broke PNG/zip reading at runtime on JS.
//
// This is a buffered (non-incremental at the bit level) decoder: setInput
// accumulates the whole compressed stream and the first inflate() call decodes
// it in one pass into an internal output buffer that subsequent inflate() calls
// drain. That matches how InflaterInputStream and the PNG decoder drive it
// (whole IDAT in memory) while keeping the public method contract intact.
package java.util.zip

class Inflater(nowrap: Boolean) {
  def this() = this(false)

  private var inputBuf  = new Array[Byte](0)
  private var output    = new Array[Byte](0)
  private var outputPos = 0
  private var decoded   = false
  private var finishedF = false
  private var totalInF  = 0

  def setInput(b: Array[Byte], off: Int, len: Int): Unit = {
    val merged = new Array[Byte](inputBuf.length + len)
    System.arraycopy(inputBuf, 0, merged, 0, inputBuf.length)
    System.arraycopy(b, off, merged, inputBuf.length, len)
    inputBuf = merged
  }

  def setInput(b: Array[Byte]): Unit = setInput(b, 0, b.length)

  def needsInput(): Boolean = !decoded && inputBuf.length == 0

  def needsDictionary(): Boolean = false

  def finished(): Boolean = finishedF && outputPos >= output.length

  def inflate(b: Array[Byte], off: Int, len: Int): Int = {
    if (!decoded) {
      output = Inflater.inflateAll(inputBuf, nowrap)
      totalInF = inputBuf.length
      decoded = true
      finishedF = true
    }
    val remaining = output.length - outputPos
    if (remaining <= 0) 0
    else {
      val n = math.min(remaining, len)
      System.arraycopy(output, outputPos, b, off, n)
      outputPos += n
      n
    }
  }

  def inflate(b: Array[Byte]): Int = inflate(b, 0, b.length)

  def end(): Unit = ()

  def reset(): Unit = {
    inputBuf = new Array[Byte](0)
    output = new Array[Byte](0)
    outputPos = 0
    decoded = false
    finishedF = false
    totalInF = 0
  }

  def getTotalIn:   Int = totalInF
  def getTotalOut:  Int = outputPos
  def getRemaining: Int = inputBuf.length
}

object Inflater {

  private val LengthBase  = Array(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
  private val LengthExtra = Array(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
  private val DistBase    = Array(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
  private val DistExtra   = Array(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
  // Order in which code-length-code lengths appear (RFC 1951 §3.2.7).
  private val CodeLengthOrder = Array(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

  /** Canonical Huffman decode table built from a list of code lengths. */
  final private class HuffTable(lengths: Array[Int]) {
    private val maxBits = { var m = 0; var i = 0; while (i < lengths.length) { if (lengths(i) > m) m = lengths(i); i += 1 }; m }
    // For each symbol, its canonical code (MSB-first) and bit length.
    private val codes = new Array[Int](lengths.length)
    locally {
      val blCount = new Array[Int](maxBits + 1)
      var i       = 0
      while (i < lengths.length) { if (lengths(i) != 0) blCount(lengths(i)) += 1; i += 1 }
      val nextCode = new Array[Int](maxBits + 1)
      var code     = 0
      var bits     = 1
      while (bits <= maxBits) {
        code = (code + blCount(bits - 1)) << 1
        nextCode(bits) = code
        bits += 1
      }
      i = 0
      while (i < lengths.length) {
        val len = lengths(i)
        if (len != 0) { codes(i) = nextCode(len); nextCode(len) += 1 }
        i += 1
      }
    }

    /** Decode the next symbol from the bit reader, MSB-first per RFC 1951. */
    def decode(br: BitReader): Int = {
      var code   = 0
      var len    = 0
      var result = -1
      while (result < 0) {
        code = (code << 1) | br.readBit()
        len += 1
        if (len > maxBits) throw new java.util.zip.DataFormatException("invalid Huffman code")
        var i     = 0
        var found = false
        while (i < lengths.length && !found) {
          if (lengths(i) == len && codes(i) == code) { result = i; found = true }
          i += 1
        }
      }
      result
    }
  }

  /** LSB-first bit reader over a byte array, as required by DEFLATE. */
  final private class BitReader(data: Array[Byte], start: Int) {
    private var bytePos = start
    private var bitBuf  = 0
    private var bitCnt  = 0

    def readBit(): Int = {
      if (bitCnt == 0) {
        if (bytePos >= data.length) throw new java.util.zip.DataFormatException("unexpected end of input")
        bitBuf = data(bytePos) & 0xff
        bytePos += 1
        bitCnt = 8
      }
      val bit = bitBuf & 1
      bitBuf >>>= 1
      bitCnt -= 1
      bit
    }

    def readBits(n: Int): Int = {
      var v = 0
      var i = 0
      while (i < n) { v |= (readBit() << i); i += 1 }
      v
    }

    def alignToByte(): Unit = { bitBuf = 0; bitCnt = 0 }

    def readAlignedBytes(n: Int): Array[Byte] = {
      val out = new Array[Byte](n)
      var i   = 0
      while (i < n) {
        if (bytePos >= data.length) throw new java.util.zip.DataFormatException("unexpected end of input")
        out(i) = data(bytePos); bytePos += 1; i += 1
      }
      out
    }

    def skipBytes(n: Int): Unit = bytePos += n
    def position:          Int  = bytePos
  }

  private def fixedLitLengths: Array[Int] = {
    val l = new Array[Int](288)
    var i = 0
    while (i <= 143) { l(i) = 8; i += 1 }
    while (i <= 255) { l(i) = 9; i += 1 }
    while (i <= 279) { l(i) = 7; i += 1 }
    while (i <= 287) { l(i) = 8; i += 1 }
    l
  }

  private def fixedDistLengths: Array[Int] = Array.fill(30)(5)

  /** A growable byte sink with direct indexed read access (the LZ77 window). */
  final private class ByteWindow {
    private var buf = new Array[Byte](1024)
    private var len = 0
    def size:                       Int  = len
    private def ensure(extra: Int): Unit =
      if (len + extra > buf.length) {
        var n = buf.length * 2
        while (n < len + extra) n *= 2
        val nb = new Array[Byte](n)
        System.arraycopy(buf, 0, nb, 0, len)
        buf = nb
      }
    def append(b:   Int):                           Unit        = { ensure(1); buf(len) = b.toByte; len += 1 }
    def append(src: Array[Byte], off: Int, n: Int): Unit        = { ensure(n); System.arraycopy(src, off, buf, len, n); len += n }
    def at(i:       Int):                           Byte        = buf(i)
    def toByteArray:                                Array[Byte] = java.util.Arrays.copyOf(buf, len)
  }

  def inflateAll(input: Array[Byte], nowrap: Boolean): Array[Byte] =
    if (input.length == 0) new Array[Byte](0)
    else inflateNonEmpty(input, nowrap)

  private def inflateNonEmpty(input: Array[Byte], nowrap: Boolean): Array[Byte] = {
    val start = if (nowrap) 0 else 2 // skip 2-byte zlib header (Adler-32 trailer is ignored on decode)
    val br    = new BitReader(input, start)
    val out   = new ByteWindow

    var finalBlock = false
    while (!finalBlock) {
      finalBlock = br.readBit() == 1
      val btype = br.readBits(2)
      btype match {
        case 0 => // stored
          br.alignToByte()
          val len = (input(br.position) & 0xff) | ((input(br.position + 1) & 0xff) << 8)
          br.skipBytes(4) // LEN(2) + NLEN(2)
          val bytes = br.readAlignedBytes(len)
          out.append(bytes, 0, len)
        case 1 => // fixed Huffman
          inflateBlock(br, new HuffTable(fixedLitLengths), new HuffTable(fixedDistLengths), out)
        case 2 => // dynamic Huffman
          val hlit      = br.readBits(5) + 257
          val hdist     = br.readBits(5) + 1
          val hclen     = br.readBits(4) + 4
          val clLengths = new Array[Int](19)
          var i         = 0
          while (i < hclen) { clLengths(CodeLengthOrder(i)) = br.readBits(3); i += 1 }
          val clTable    = new HuffTable(clLengths)
          val allLengths = new Array[Int](hlit + hdist)
          i = 0
          while (i < hlit + hdist) {
            val sym = clTable.decode(br)
            sym match {
              case s if s < 16 => allLengths(i) = s; i += 1
              case 16          =>
                val repeat = br.readBits(2) + 3
                val prev   = allLengths(i - 1)
                var r      = 0; while (r < repeat) { allLengths(i) = prev; i += 1; r += 1 }
              case 17 =>
                val repeat = br.readBits(3) + 3
                var r      = 0; while (r < repeat) { allLengths(i) = 0; i += 1; r += 1 }
              case 18 =>
                val repeat = br.readBits(7) + 11
                var r      = 0; while (r < repeat) { allLengths(i) = 0; i += 1; r += 1 }
              case _ => throw new java.util.zip.DataFormatException("invalid code length symbol")
            }
          }
          val litLengths  = allLengths.slice(0, hlit)
          val distLengths = allLengths.slice(hlit, hlit + hdist)
          inflateBlock(br, new HuffTable(litLengths), new HuffTable(distLengths), out)
        case _ =>
          throw new java.util.zip.DataFormatException("invalid block type")
      }
    }
    out.toByteArray
  }

  private def inflateBlock(br: BitReader, litTable: HuffTable, distTable: HuffTable, out: ByteWindow): Unit =
    scala.util.boundary {
      while (true) {
        val sym = litTable.decode(br)
        if (sym == 256) scala.util.boundary.break() // end-of-block
        else if (sym < 256) out.append(sym)
        else {
          val lc = sym - 257
          if (lc >= LengthBase.length) throw new java.util.zip.DataFormatException("invalid length code")
          val length = LengthBase(lc) + br.readBits(LengthExtra(lc))
          val dc     = distTable.decode(br)
          if (dc >= DistBase.length) throw new java.util.zip.DataFormatException("invalid distance code")
          val dist     = DistBase(dc) + br.readBits(DistExtra(dc))
          val srcStart = out.size - dist
          if (srcStart < 0) throw new java.util.zip.DataFormatException("invalid back-reference distance")
          // Byte-by-byte copy so overlapping back-references (dist < length) replicate correctly.
          var k = 0
          while (k < length) {
            out.append(out.at(srcStart + k) & 0xff)
            k += 1
          }
        }
      }
    }
}
