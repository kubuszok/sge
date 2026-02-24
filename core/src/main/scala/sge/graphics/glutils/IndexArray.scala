/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/IndexArray.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

import sge.utils.BufferUtils
import scala.compiletime.uninitialized

class IndexArray(maxIndices: Int) extends IndexData with AutoCloseable {
  private var buffer:     ShortBuffer = uninitialized
  private var byteBuffer: ByteBuffer  = uninitialized

  // used to work around bug: https://android-review.googlesource.com/#/c/73175/
  private val empty: Boolean = maxIndices == 0

  private val actualMaxIndices = if maxIndices == 0 then 1 else maxIndices // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0

  byteBuffer = BufferUtils.newUnsafeByteBuffer(actualMaxIndices * 2)
  buffer = byteBuffer.asShortBuffer()
  buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()

  /** @return the number of indices currently stored in this buffer */
  def getNumIndices(): Int =
    if empty then 0 else buffer.limit()

  /** @return the maximum number of indices this IndexArray can store. */
  def getNumMaxIndices(): Int =
    if empty then 0 else buffer.capacity()

  /** <p> Sets the indices of this IndexArray, discarding the old indices. The count must equal the number of indices to be copied to this IndexArray. </p>
    *
    * <p> This can be called in between calls to {@link #bind()} and {@link #unbind()} . The index data will be updated instantly. </p>
    *
    * @param indices
    *   the vertex data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of shorts to copy
    */
  def setIndices(indices: Array[Short], offset: Int, count: Int): Unit = {
    buffer.asInstanceOf[Buffer].clear()
    buffer.put(indices, offset, count)
    buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(count << 1)
  }

  def setIndices(indices: ShortBuffer): Unit = {
    val pos = indices.position()
    buffer.asInstanceOf[Buffer].clear()
    buffer.asInstanceOf[Buffer].limit(indices.remaining())
    buffer.put(indices)
    buffer.asInstanceOf[Buffer].flip()
    indices.asInstanceOf[Buffer].position(pos)
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() << 1)
  }

  override def updateIndices(targetOffset: Int, indices: Array[Short], offset: Int, count: Int): Unit = {
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 2)
    BufferUtils.copy(indices, offset, byteBuffer, count)
    byteBuffer.asInstanceOf[Buffer].position(pos)
  }

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  @deprecated("use getBuffer(boolean) instead")
  override def getBuffer(): ShortBuffer =
    buffer

  override def getBuffer(forWriting: Boolean): ShortBuffer =
    buffer

  /** Binds this IndexArray for rendering with glDrawElements. */
  def bind(): Unit = ()

  /** Unbinds this IndexArray. */
  def unbind(): Unit = ()

  /** Invalidates the IndexArray so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  def invalidate(): Unit = ()

  /** Closes this IndexArray and all its associated OpenGL resources. */
  override def close(): Unit =
    BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
}
