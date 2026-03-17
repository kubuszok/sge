// Minimal java.io.DataInput stub for Scala.js.
// Required by DataInputStream stub.
package java.io

trait DataInput {
  def readFully(b: Array[Byte]):                     Unit
  def readFully(b: Array[Byte], off: Int, len: Int): Unit
  def skipBytes(n: Int):                             Int
  def readBoolean():                                 Boolean
  def readByte():                                    Byte
  def readUnsignedByte():                            Int
  def readShort():                                   Short
  def readUnsignedShort():                           Int
  def readChar():                                    Char
  def readInt():                                     Int
  def readLong():                                    Long
  def readFloat():                                   Float
  def readDouble():                                  Double
  def readLine():                                    String
  def readUTF():                                     String
}
