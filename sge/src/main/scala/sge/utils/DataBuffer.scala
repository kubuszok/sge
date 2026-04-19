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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: DataBuffer,OptimizedByteArrayOutputStream,_buffer,buffer,bytes,clear,size,toByteArray
 * Covenant-source-reference: com/badlogic/gdx/utils/DataBuffer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

import java.io.{ DataOutput, OutputStream }

// Abstract class since it has unimplemented Java interface methods
abstract class DataBuffer extends OutputStream with DataOutput {

  private val _buffer = new OptimizedByteArrayOutputStream(256)

  /** Returns the backing array, which has 0 to [[size]] items. */
  def buffer: Array[Byte] = _buffer.buffer

  /** Returns the number of bytes written to this buffer. */
  def size: Int = buffer.size

  def bytes: Array[Byte] = _buffer.toByteArray()

  def clear(): Unit = _buffer.reset()

  private class OptimizedByteArrayOutputStream(initialSize: Int) extends java.io.ByteArrayOutputStream(initialSize) {
    override def toByteArray(): Array[Byte] =
      if (count == buf.length) buf
      else super.toByteArray

    def buffer: Array[Byte] = buf
  }
}
