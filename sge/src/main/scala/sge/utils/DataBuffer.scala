/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/DataBuffer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `toArray()` -> `getBytes`
 *   Convention: different inheritance hierarchy; Java `DataBuffer` extends LibGDX `DataOutput`, Scala extends Java standard `DataOutput`; `OptimizedByteArrayOutputStream` inlined instead of reusing StreamUtils
 *   Idiom: split packages
 *   Fixes: added `getBuffer()` and `size()` delegating to inner OptimizedByteArrayOutputStream
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ DataOutput, OutputStream }

// Abstract class since it has unimplemented Java interface methods
abstract class DataBuffer extends OutputStream with DataOutput {

  private val buffer = new OptimizedByteArrayOutputStream(256)

  /** Returns the backing array, which has 0 to [[size]] items. */
  def getBuffer(): Array[Byte] = buffer.getBuffer()

  /** Returns the number of bytes written to this buffer. */
  def size(): Int = buffer.size()

  def getBytes: Array[Byte] = buffer.toByteArray()

  def clear(): Unit = buffer.reset()

  private class OptimizedByteArrayOutputStream(initialSize: Int) extends java.io.ByteArrayOutputStream(initialSize) {
    override def toByteArray(): Array[Byte] =
      if (count == buf.length) buf
      else super.toByteArray()

    def getBuffer(): Array[Byte] = buf
  }
}
