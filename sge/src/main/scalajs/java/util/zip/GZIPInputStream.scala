// java.util.zip.GZIPInputStream for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala GZIP reader (RFC 1952 header parsing + raw DEFLATE via Inflater)
// (ISS-533 / ISS-651). ETC1.loadCompressedData, KTXTextureData and the tiled
// map loaders decode gzip-wrapped data on the browser baseline; the previous
// implementation silently delegated to a zlib Inflater and could not read real gzip.
package java.util.zip

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GZIPInputStream(in: java.io.InputStream, size: Int) extends InflaterInputStream(GZIPInputStream.stripGzip(in), new Inflater(true), size) {
  def this(in: java.io.InputStream) = this(in, 512)
}

object GZIPInputStream {
  private val FHCRC    = 1 << 1
  private val FEXTRA   = 1 << 2
  private val FNAME    = 1 << 3
  private val FCOMMENT = 1 << 4

  /** Reads the whole stream, validates and strips the RFC 1952 gzip header and the 8-byte trailer, and returns a stream over just the raw DEFLATE payload.
    */
  private def stripGzip(in: java.io.InputStream): java.io.InputStream = {
    val raw = new ByteArrayOutputStream()
    val buf = new Array[Byte](4096)
    var n   = in.read(buf)
    while (n != -1) { raw.write(buf, 0, n); n = in.read(buf) }
    val data = raw.toByteArray
    if (data.length < 18) throw new java.io.IOException("Not in GZIP format")
    if ((data(0) & 0xff) != 0x1f || (data(1) & 0xff) != 0x8b) throw new java.io.IOException("Not in GZIP format")
    if ((data(2) & 0xff) != 8) throw new java.io.IOException("Unsupported compression method")
    val flg = data(3) & 0xff
    var p   = 10 // fixed header: magic(2) + CM(1) + FLG(1) + MTIME(4) + XFL(1) + OS(1)
    if ((flg & FEXTRA) != 0) {
      val xlen = (data(p) & 0xff) | ((data(p + 1) & 0xff) << 8)
      p += 2 + xlen
    }
    if ((flg & FNAME) != 0) { while (p < data.length && data(p) != 0) p += 1; p += 1 }
    if ((flg & FCOMMENT) != 0) { while (p < data.length && data(p) != 0) p += 1; p += 1 }
    if ((flg & FHCRC) != 0) p += 2
    // Trailing CRC32(4) + ISIZE(4) are not needed to inflate the raw payload.
    val payloadLen = data.length - p - 8
    if (payloadLen < 0) throw new java.io.IOException("Corrupt GZIP trailer")
    new ByteArrayInputStream(data, p, payloadLen)
  }
}
