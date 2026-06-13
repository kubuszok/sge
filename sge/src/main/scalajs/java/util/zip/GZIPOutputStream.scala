// java.util.zip.GZIPOutputStream for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala GZIP writer (RFC 1952 header + raw DEFLATE + CRC32/ISIZE trailer)
// (ISS-533 / ISS-651). ETC1 writes gzip-wrapped texture data; the previous implementation
// emitted a zlib stream with no gzip framing, which no gzip reader could parse.
package java.util.zip

class GZIPOutputStream(out: java.io.OutputStream, size: Int) extends DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), size) {
  def this(out: java.io.OutputStream) = this(out, 512)

  private val crc            = new CRC32()
  private var headerWritten  = false
  private var trailerWritten = false
  private var isize          = 0

  private def writeHeader(): Unit = {
    // RFC 1952 fixed 10-byte header: magic, CM=8 (DEFLATE), FLG=0, MTIME=0, XFL=0, OS=255 (unknown).
    out.write(0x1f); out.write(0x8b); out.write(8); out.write(0)
    out.write(0); out.write(0); out.write(0); out.write(0)
    out.write(0); out.write(0xff)
    headerWritten = true
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    if (!headerWritten) writeHeader()
    super.write(b, off, len)
    crc.update(b, off, len)
    isize += len
  }

  override def finish(): Unit =
    if (!trailerWritten) {
      if (!headerWritten) writeHeader()
      super.finish()
      val c = crc.getValue()
      // CRC32 and ISIZE are little-endian (RFC 1952 §2.3.1).
      out.write((c & 0xff).toInt); out.write(((c >>> 8) & 0xff).toInt); out.write(((c >>> 16) & 0xff).toInt); out.write(((c >>> 24) & 0xff).toInt)
      out.write(isize & 0xff); out.write((isize >>> 8) & 0xff); out.write((isize >>> 16) & 0xff); out.write((isize >>> 24) & 0xff)
      trailerWritten = true
    }
}
