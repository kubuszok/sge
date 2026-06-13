// java.util.zip.CheckedOutputStream for Scala.js.
//
// Scala.js's javalib does not provide java.util.zip; SGE supplies a faithful
// pure-Scala implementation that forwards bytes to the wrapped stream while
// maintaining the running checksum (ISS-533 / ISS-651). The previous throwing
// implementation broke anim8's ChunkBuffer PNG writer at runtime on JS.
package java.util.zip

class CheckedOutputStream(out: java.io.OutputStream, val checksum: Checksum) extends java.io.OutputStream {

  override def write(b: Int): Unit = {
    out.write(b)
    checksum.update(b)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    out.write(b, off, len)
    checksum.update(b, off, len)
  }

  def getChecksum(): Checksum = checksum
}
