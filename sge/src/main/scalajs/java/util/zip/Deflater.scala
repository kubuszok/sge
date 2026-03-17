// Minimal java.util.zip.Deflater stub for Scala.js.
// Referenced by PixmapIO for CIM/PNG writing — never called at runtime on browser.
package java.util.zip

class Deflater(level: Int, nowrap: Boolean) {
  def this() = this(Deflater.DEFAULT_COMPRESSION, false)
  def this(level: Int) = this(level, false)

  def setInput(input: Array[Byte], off: Int, len: Int): Unit =
    throw new UnsupportedOperationException("Deflater is not supported on Scala.js")
  def setInput(input: Array[Byte]):                     Unit    = setInput(input, 0, input.length)
  def finish():                                         Unit    = ()
  def finished():                                       Boolean = true
  def deflate(output: Array[Byte], off: Int, len: Int): Int     =
    throw new UnsupportedOperationException("Deflater is not supported on Scala.js")
  def deflate(output: Array[Byte]): Int  = deflate(output, 0, output.length)
  def end():                        Unit = ()
  def reset():                      Unit = ()
}

object Deflater {
  val DEFAULT_COMPRESSION: Int = -1
  val BEST_COMPRESSION:    Int = 9
  val BEST_SPEED:          Int = 1
  val NO_COMPRESSION:      Int = 0
  val DEFLATED:            Int = 8
}
