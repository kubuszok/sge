// Minimal java.util.zip.CRC32 stub for Scala.js.
// Referenced by PixmapIO for PNG writing — never called at runtime on browser.
package java.util.zip

class CRC32 extends Checksum {
  def update(b: Int): Unit =
    throw new UnsupportedOperationException("CRC32 is not supported on Scala.js")
  def update(b: Array[Byte], off: Int, len: Int): Unit =
    throw new UnsupportedOperationException("CRC32 is not supported on Scala.js")
  def update(b: Array[Byte]): Unit = update(b, 0, b.length)
  def getValue():             Long = 0L
  def reset():                Unit = ()
}
