// Minimal java.util.zip.CheckedOutputStream stub for Scala.js.
// Referenced by PixmapIO for PNG writing — never called at runtime on browser.
package java.util.zip

class CheckedOutputStream(out: java.io.OutputStream, cksum: Checksum) extends java.io.OutputStream {
  def write(b: Int): Unit =
    throw new UnsupportedOperationException("CheckedOutputStream is not supported on Scala.js")
  def getChecksum(): Checksum = cksum
}
