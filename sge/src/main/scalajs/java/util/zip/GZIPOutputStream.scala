// Minimal java.util.zip.GZIPOutputStream stub for Scala.js.
// Referenced by ETC1 for writing compressed texture data — never called at runtime on browser.
package java.util.zip

class GZIPOutputStream(out: java.io.OutputStream, size: Int) extends DeflaterOutputStream(out) {
  def this(out: java.io.OutputStream) = this(out, 512)
}
