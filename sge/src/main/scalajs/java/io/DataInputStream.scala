// java.io.DataInputStream implementation for Scala.js.
// Needed by the Scala.js test bridge (org.scalajs.testing.bridge.JSRPC) and also
// referenced by ETC1, KTX, and tile map loaders in SGE.
package java.io

class DataInputStream(in: InputStream) extends InputStream with DataInput {
  def read(): Int = in.read()

  override def read(b: Array[Byte], off: Int, len: Int): Int = in.read(b, off, len)

  def readFully(b: Array[Byte]): Unit = readFully(b, 0, b.length)

  def readFully(b: Array[Byte], off: Int, len: Int): Unit = {
    var n = 0
    while (n < len) {
      val count = in.read(b, off + n, len - n)
      if (count < 0) throw new EOFException()
      n += count
    }
  }

  def skipBytes(n: Int): Int = {
    var total = 0
    var cur   = 0
    while (total < n && { cur = in.read().toInt; cur } >= 0)
      total += 1
    total
  }

  def readBoolean(): Boolean = {
    val ch = in.read()
    if (ch < 0) throw new EOFException()
    ch != 0
  }

  def readByte(): Byte = {
    val ch = in.read()
    if (ch < 0) throw new EOFException()
    ch.toByte
  }

  def readUnsignedByte(): Int = {
    val ch = in.read()
    if (ch < 0) throw new EOFException()
    ch
  }

  def readShort(): Short = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0) throw new EOFException()
    ((ch1 << 8) + ch2).toShort
  }

  def readUnsignedShort(): Int = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0) throw new EOFException()
    (ch1 << 8) + ch2
  }

  def readChar(): Char = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0) throw new EOFException()
    ((ch1 << 8) + ch2).toChar
  }

  def readInt(): Int = {
    val ch1 = in.read()
    val ch2 = in.read()
    val ch3 = in.read()
    val ch4 = in.read()
    if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException()
    (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4
  }

  def readLong(): Long = {
    val buf = new Array[Byte](8)
    readFully(buf, 0, 8)
    ((buf(0).toLong & 0xff) << 56) |
      ((buf(1).toLong & 0xff) << 48) |
      ((buf(2).toLong & 0xff) << 40) |
      ((buf(3).toLong & 0xff) << 32) |
      ((buf(4).toLong & 0xff) << 24) |
      ((buf(5).toLong & 0xff) << 16) |
      ((buf(6).toLong & 0xff) << 8) |
      (buf(7).toLong & 0xff)
  }

  def readFloat(): Float = java.lang.Float.intBitsToFloat(readInt())

  def readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())

  def readLine(): String = throw new UnsupportedOperationException("DataInputStream.readLine not supported on Scala.js")

  def readUTF(): String = DataInputStream.readUTF(this)
}

object DataInputStream {
  def readUTF(in: DataInput): String = {
    val utflen  = in.readUnsignedShort()
    val bytearr = new Array[Byte](utflen)
    in.readFully(bytearr, 0, utflen)

    val chararr   = new Array[Char](utflen)
    var c         = 0
    var count     = 0
    var charCount = 0

    while (count < utflen) {
      c = bytearr(count).toInt & 0xff
      if (c > 127) {
        // multi-byte
        if ((c >> 4) == 12 || (c >> 4) == 13) {
          // 2-byte
          count += 1
          if (count >= utflen) throw new java.io.UTFDataFormatException("malformed input: partial character at end")
          val c2 = bytearr(count).toInt
          if ((c2 & 0xc0) != 0x80) throw new java.io.UTFDataFormatException("malformed input around byte " + count)
          chararr(charCount) = (((c & 0x1f) << 6) | (c2 & 0x3f)).toChar
          charCount += 1
        } else if ((c >> 4) == 14) {
          // 3-byte
          count += 1
          if (count + 1 >= utflen) throw new java.io.UTFDataFormatException("malformed input: partial character at end")
          val c2 = bytearr(count).toInt
          count += 1
          val c3 = bytearr(count).toInt
          if (((c2 & 0xc0) != 0x80) || ((c3 & 0xc0) != 0x80)) throw new java.io.UTFDataFormatException("malformed input around byte " + (count - 1))
          chararr(charCount) = (((c & 0x0f) << 12) | ((c2 & 0x3f) << 6) | (c3 & 0x3f)).toChar
          charCount += 1
        } else {
          throw new java.io.UTFDataFormatException("malformed input around byte " + count)
        }
      } else {
        // 1-byte ASCII
        chararr(charCount) = c.toChar
        charCount += 1
      }
      count += 1
    }
    new String(chararr, 0, charCount)
  }
}
