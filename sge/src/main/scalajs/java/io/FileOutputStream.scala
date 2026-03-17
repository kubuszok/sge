// Minimal java.io.FileOutputStream stub for Scala.js.
// Referenced by PixmapIO and other file-writing paths — never called at runtime on browser.
package java.io

class FileOutputStream(file: File, append: Boolean) extends OutputStream {
  def this(file: File) = this(file, false)
  def this(name: String) = this(new File(name))
  def this(name: String, append: Boolean) = this(new File(name), append)

  def write(b: Int): Unit =
    throw new UnsupportedOperationException("FileOutputStream is not supported on Scala.js")
}
