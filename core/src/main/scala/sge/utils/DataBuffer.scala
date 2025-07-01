package sge
package utils

import java.io.{ DataOutput, OutputStream }

// Abstract class since it has unimplemented Java interface methods
abstract class DataBuffer extends OutputStream with DataOutput {

  private val buffer = new OptimizedByteArrayOutputStream(256)

  def getBytes: Array[Byte] = buffer.toByteArray()

  def clear(): Unit = buffer.reset()

  private class OptimizedByteArrayOutputStream(initialSize: Int) extends java.io.ByteArrayOutputStream(initialSize) {
    override def toByteArray(): Array[Byte] =
      if (count == buf.length) buf
      else super.toByteArray()

    def getBuffer(): Array[Byte] = buf
  }
}
