/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/DataBuffer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `toArray()` -> `getBytes`; missing `getBuffer()` and `size()` methods
 *   Convention: different inheritance hierarchy; Java `DataBuffer` extends LibGDX `DataOutput`, Scala extends Java standard `DataOutput`; `OptimizedByteArrayOutputStream` inlined instead of reusing StreamUtils
 *   Idiom: split packages
 *   Issues: missing `getBuffer()` and `size()` from Java API; `OptimizedByteArrayOutputStream` duplicated from `StreamUtils`
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
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
