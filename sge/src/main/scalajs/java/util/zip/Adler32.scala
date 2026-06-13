// java.util.zip.Adler32 for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala Adler-32 (RFC 1950 §9), used as the trailing checksum of the zlib
// container emitted by Deflater and verified by Inflater (ISS-533 / ISS-651).
package java.util.zip

class Adler32 extends Checksum {

  private val Base = 65521 // largest prime smaller than 65536

  private var a = 1
  private var b = 0

  def update(byte: Int): Unit = {
    a = (a + (byte & 0xff)) % Base
    b = (b + a) % Base
  }

  def update(buf: Array[Byte], off: Int, len: Int): Unit = {
    if (off < 0 || len < 0 || off > buf.length - len)
      throw new ArrayIndexOutOfBoundsException()
    var i   = off
    val end = off + len
    while (i < end) {
      a = (a + (buf(i) & 0xff)) % Base
      b = (b + a) % Base
      i += 1
    }
  }

  override def update(buf: Array[Byte]): Unit = update(buf, 0, buf.length)

  def getValue(): Long = ((b.toLong << 16) | a.toLong) & 0xffffffffL

  def reset(): Unit = {
    a = 1
    b = 0
  }
}
