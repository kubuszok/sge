// java.util.zip.CRC32 for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip, so SGE supplies a faithful
// pure-Scala implementation. PixmapIO/anim8 PNG writing and the tiled map
// loaders rely on a genuinely working CRC32 on the browser baseline (ISS-533 /
// ISS-651): the previous throwing implementation broke savePNG at runtime on JS.
//
// Standard CRC-32 (ISO 3309 / ITU-T V.42, polynomial 0xEDB88320, reflected),
// the same algorithm java.util.zip.CRC32 and zlib's crc32() compute, so the
// produced PNG/zlib checksums are byte-identical to the JVM and Native outputs.
package java.util.zip

class CRC32 extends Checksum {

  private var crc: Int = 0 // running CRC, stored pre-final-XOR (i.e. ~value)

  def update(b: Int): Unit = {
    var c = ~crc
    c = (c >>> 8) ^ CRC32.table((c ^ b) & 0xff)
    crc = ~c
  }

  def update(b: Array[Byte], off: Int, len: Int): Unit = {
    if (off < 0 || len < 0 || off > b.length - len)
      throw new ArrayIndexOutOfBoundsException()
    var c   = ~crc
    var i   = off
    val end = off + len
    while (i < end) {
      c = (c >>> 8) ^ CRC32.table((c ^ b(i)) & 0xff)
      i += 1
    }
    crc = ~c
  }

  override def update(b: Array[Byte]): Unit = update(b, 0, b.length)

  def getValue(): Long = crc.toLong & 0xffffffffL

  def reset(): Unit = crc = 0
}

object CRC32 {
  // Precomputed CRC-32 lookup table (polynomial 0xEDB88320, reflected).
  private val table: Array[Int] = {
    val t = new Array[Int](256)
    var n = 0
    while (n < 256) {
      var c = n
      var k = 0
      while (k < 8) {
        c = if ((c & 1) != 0) 0xedb88320 ^ (c >>> 1) else c >>> 1
        k += 1
      }
      t(n) = c
      n += 1
    }
    t
  }
}
