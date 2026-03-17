// Minimal java.util.zip.DeflaterOutputStream stub for Scala.js.
// Referenced by PixmapIO for CIM writing — never called at runtime on browser.
package java.util.zip

class DeflaterOutputStream(out: java.io.OutputStream, defl: Deflater, size: Int, syncFlush: Boolean) extends java.io.OutputStream {
  def this(out: java.io.OutputStream, defl: Deflater, size: Int) = this(out, defl, size, false)
  def this(out: java.io.OutputStream, defl: Deflater) = this(out, defl, 512)
  def this(out: java.io.OutputStream) = this(out, new Deflater())

  def write(b: Int): Unit =
    throw new UnsupportedOperationException("DeflaterOutputStream is not supported on Scala.js")
  def finish(): Unit = ()
}
