// java.io.DataOutputStream implementation for Scala.js.
// Needed by the Scala.js test bridge and referenced by PixmapIO/ETC1 in SGE.
package java.io

class DataOutputStream(out: OutputStream) extends OutputStream with DataOutput {
  protected var written: Int = 0

  def write(b: Int): Unit = {
    out.write(b)
    written += 1
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    out.write(b, off, len)
    written += len
  }

  def writeBoolean(v: Boolean): Unit = write(if (v) 1 else 0)

  def writeByte(v: Int): Unit = write(v)

  def writeShort(v: Int): Unit = {
    write((v >>> 8) & 0xff)
    write(v & 0xff)
  }

  def writeChar(v: Int): Unit = {
    write((v >>> 8) & 0xff)
    write(v & 0xff)
  }

  def writeInt(v: Int): Unit = {
    write((v >>> 24) & 0xff)
    write((v >>> 16) & 0xff)
    write((v >>> 8) & 0xff)
    write(v & 0xff)
  }

  def writeLong(v: Long): Unit = {
    write(((v >>> 56) & 0xff).toInt)
    write(((v >>> 48) & 0xff).toInt)
    write(((v >>> 40) & 0xff).toInt)
    write(((v >>> 32) & 0xff).toInt)
    write(((v >>> 24) & 0xff).toInt)
    write(((v >>> 16) & 0xff).toInt)
    write(((v >>> 8) & 0xff).toInt)
    write((v & 0xff).toInt)
  }

  def writeFloat(v: Float): Unit = writeInt(java.lang.Float.floatToIntBits(v))

  def writeDouble(v: Double): Unit = writeLong(java.lang.Double.doubleToLongBits(v))

  def writeBytes(s: String): Unit = {
    var i = 0
    while (i < s.length) {
      write(s.charAt(i).toInt)
      i += 1
    }
  }

  def writeChars(s: String): Unit = {
    var i = 0
    while (i < s.length) {
      writeChar(s.charAt(i).toInt)
      i += 1
    }
  }

  def writeUTF(s: String): Unit = {
    val utflen = {
      var len = 0
      var i   = 0
      while (i < s.length) {
        val c = s.charAt(i).toInt
        if (c >= 0x0001 && c <= 0x007f) len += 1
        else if (c > 0x07ff) len += 3
        else len += 2
        i += 1
      }
      len
    }
    if (utflen > 65535) throw new UTFDataFormatException("encoded string too long: " + utflen + " bytes")
    writeShort(utflen)
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i).toInt
      if (c >= 0x0001 && c <= 0x007f) {
        write(c)
      } else if (c > 0x07ff) {
        write(0xe0 | ((c >> 12) & 0x0f))
        write(0x80 | ((c >> 6) & 0x3f))
        write(0x80 | (c & 0x3f))
      } else {
        write(0xc0 | ((c >> 6) & 0x1f))
        write(0x80 | (c & 0x3f))
      }
      i += 1
    }
  }

  override def flush(): Unit = out.flush()
}
