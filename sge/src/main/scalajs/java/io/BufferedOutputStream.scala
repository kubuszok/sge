// Minimal java.io.BufferedOutputStream stub for Scala.js.
// Referenced by PixmapIO for writing — never called at runtime on browser.
package java.io

class BufferedOutputStream(out: OutputStream, size: Int) extends OutputStream {
  def this(out: OutputStream) = this(out, 8192)
  def write(b:  Int): Unit = out.write(b)
}
