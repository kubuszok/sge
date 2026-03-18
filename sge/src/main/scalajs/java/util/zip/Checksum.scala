// Minimal java.util.zip.Checksum stub for Scala.js.
// Interface required by CRC32 stub.
package java.util.zip

trait Checksum {
  def update(b: Int):                             Unit
  def update(b: Array[Byte], off: Int, len: Int): Unit
  def update(b: Array[Byte]):                     Unit
  def getValue():                                 Long
  def reset():                                    Unit
}
