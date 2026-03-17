// Minimal java.io.BufferedInputStream stub for Scala.js.
// The Scala.js linker needs this class because ETC1/KTX/PixmapIO reference it
// for compressed texture loading. These code paths are never reached at runtime
// on the browser.
package java.io

class BufferedInputStream(in: InputStream, size: Int) extends InputStream {
  def this(in: InputStream) = this(in, 8192)
  def read(): Int = in.read()
}
