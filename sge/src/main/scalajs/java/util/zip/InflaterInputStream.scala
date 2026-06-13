// java.util.zip.InflaterInputStream for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala wrapper that fully reads the underlying stream, inflates it with a
// real Inflater, and serves the decompressed bytes (ISS-533 / ISS-651). The
// previous throwing implementation broke PixmapIO.CIM reading and zlib tile-data decoding
// at runtime on JS.
//
// This buffers the whole compressed input on first read (the inputs SGE feeds it
// — .cim pixmaps and tiled tile-layer data — are already wholly in memory), then
// streams the inflated result through the InputStream contract.
package java.util.zip

import java.io.ByteArrayOutputStream

class InflaterInputStream(in: java.io.InputStream, inf: Inflater, size: Int) extends java.io.InputStream {
  def this(in: java.io.InputStream, inf: Inflater) = this(in, inf, 512)
  def this(in: java.io.InputStream) = this(in, new Inflater())

  private var decoded: Array[Byte] = null
  private var pos = 0

  private def ensureDecoded(): Unit =
    if (decoded == null) {
      val raw = new ByteArrayOutputStream()
      val buf = new Array[Byte](if (size > 0) size else 512)
      var n   = in.read(buf)
      while (n != -1) {
        raw.write(buf, 0, n)
        n = in.read(buf)
      }
      inf.setInput(raw.toByteArray)
      val out  = new ByteArrayOutputStream()
      val ibuf = new Array[Byte](4096)
      var r    = inf.inflate(ibuf)
      while (r > 0) {
        out.write(ibuf, 0, r)
        r = inf.inflate(ibuf)
      }
      decoded = out.toByteArray
    }

  override def read(): Int = {
    ensureDecoded()
    if (pos >= decoded.length) -1
    else { val b = decoded(pos) & 0xff; pos += 1; b }
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    ensureDecoded()
    if (len == 0) 0
    else if (pos >= decoded.length) -1
    else {
      val n = math.min(len, decoded.length - pos)
      System.arraycopy(decoded, pos, b, off, n)
      pos += n
      n
    }
  }

  override def available(): Int = {
    ensureDecoded()
    decoded.length - pos
  }

  override def close(): Unit = in.close()
}
