// java.util.zip.DeflaterOutputStream for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala wrapper that streams bytes through a real Deflater (ISS-533 /
// ISS-651). The previous throwing implementation broke PNG writing at runtime on JS.
//
// Matches the java.util.zip.DeflaterOutputStream contract: write() feeds the
// Deflater, deflate()/finish() drain its compressed output to the wrapped
// stream, and close() finishes then closes. flush() is a no-op on the
// compressed side (the default Deflater has no SYNC_FLUSH support here), which
// matches the base-class behavior for a plain DeflaterOutputStream.
package java.util.zip

class DeflaterOutputStream(out: java.io.OutputStream, defl: Deflater, size: Int, syncFlush: Boolean) extends java.io.OutputStream {
  def this(out: java.io.OutputStream, defl: Deflater, size: Int) = this(out, defl, size, false)
  def this(out: java.io.OutputStream, defl: Deflater) = this(out, defl, 512)
  def this(out: java.io.OutputStream) = this(out, new Deflater())

  protected val buf: Array[Byte] = new Array[Byte](if (size > 0) size else 512)
  private var closed = false

  private val single = new Array[Byte](1)

  override def write(b: Int): Unit = {
    single(0) = b.toByte
    write(single, 0, 1)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    if (closed) throw new java.io.IOException("Stream closed")
    if ((off | len | (off + len) | (b.length - (off + len))) < 0)
      throw new IndexOutOfBoundsException()
    if (len != 0) {
      defl.setInput(b, off, len)
      // The default single-shot Deflater produces nothing until finish(); the
      // loop drains whatever is currently available (none until finish).
      drain()
    }
  }

  override def write(b: Array[Byte]): Unit = write(b, 0, b.length)

  private def drain(): Unit = {
    var n = defl.deflate(buf, 0, buf.length)
    while (n > 0) {
      out.write(buf, 0, n)
      n = defl.deflate(buf, 0, buf.length)
    }
  }

  def finish(): Unit =
    if (!defl.finished()) {
      defl.finish()
      drain()
    }

  override def flush(): Unit =
    out.flush()

  override def close(): Unit =
    if (!closed) {
      finish()
      out.close()
      closed = true
    }
}
