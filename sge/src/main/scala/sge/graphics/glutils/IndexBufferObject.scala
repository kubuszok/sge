/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/IndexBufferObject.java
 * Original authors: mzechner, Thorsten Schleinzer
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (using Sge) instead of Gdx.gl20 statics
 *   Idiom: split packages
 *   Idiom: typed GL enums -- BufferTarget, BufferUsage
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

import sge.utils.BufferUtils
import sge.utils.SgeError
import scala.annotation.nowarn
import scala.compiletime.uninitialized

/** <p> In IndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs. This class can be seamlessly used with OpenGL ES 1.x and 2.0. </p>
  *
  * <p> Uses indirect Buffers on Android 1.5/1.6 to fix GC invocation due to leaking PlatformAddress instances. </p>
  *
  * <p> You can also use this to store indices for vertex arrays. Do not call {@link #bind()} or {@link #unbind()} in this case but rather use {@link #getBuffer()} to use the buffer directly with
  * glDrawElements. You must also create the IndexBufferObject with the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers. </p>
  *
  * <p> VertexBufferObjects must be disposed via the {@link #close()} method when no longer needed </p>
  *
  * @author
  *   mzechner, Thorsten Schleinzer
  */
class IndexBufferObject(isStatic: Boolean, maxIndices: Int)(using Sge) extends IndexData with AutoCloseable {
  private var _buffer:      ShortBuffer = uninitialized
  private var byteBuffer:   ByteBuffer  = uninitialized
  private var ownsBuffer:   Boolean     = uninitialized
  private var bufferHandle: Int         = uninitialized
  @nowarn("msg=not read") // set in constructor, will be read when buffer operations implemented
  private var isDirect: Boolean     = uninitialized
  private var isDirty:  Boolean     = true
  private var isBound:  Boolean     = false
  private var usage:    BufferUsage = uninitialized

  // used to work around bug: https://android-review.googlesource.com/#/c/73175/
  private val empty: Boolean = maxIndices == 0

  private val actualMaxIndices = if maxIndices == 0 then 1 else maxIndices // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0

  byteBuffer = BufferUtils.newUnsafeByteBuffer(actualMaxIndices * 2)
  isDirect = true

  _buffer = byteBuffer.asShortBuffer()
  ownsBuffer = true
  _buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()
  bufferHandle = Sge().graphics.gl20.glGenBuffer()
  usage = if isStatic then BufferUsage.StaticDraw else BufferUsage.DynamicDraw

  /** Creates a new static IndexBufferObject to be used with vertex arrays.
    *
    * @param maxIndices
    *   the maximum number of indices this buffer can hold
    */
  def this(maxIndices: Int)(using Sge) =
    this(true, maxIndices)

  def this(isStatic: Boolean, data: ByteBuffer)(using Sge) = {
    this(isStatic, if data.limit() == 0 then 0 else 1) // Initialize with a dummy size

    // Override the initialization for ByteBuffer constructor
    byteBuffer = data
    isDirect = true

    _buffer = byteBuffer.asShortBuffer()
    ownsBuffer = false
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    usage = if isStatic then BufferUsage.StaticDraw else BufferUsage.DynamicDraw
  }

  /** @return the number of indices currently stored in this buffer */
  def numIndices: Int =
    if empty then 0 else _buffer.limit()

  /** @return the maximum number of indices this IndexBufferObject can store. */
  def numMaxIndices: Int =
    if empty then 0 else _buffer.capacity()

  /** <p> Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be copied to this IndexBufferObject. </p>
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
    isDirty = true
    _buffer.asInstanceOf[Buffer].clear()
    _buffer.put(indices, offset, count)
    _buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(count << 1)

    if isBound then {
      Sge().graphics.gl20.glBufferData(BufferTarget.ElementArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }
  }

  def setIndices(indices: ShortBuffer): Unit = {
    isDirty = true
    val pos = indices.position()
    _buffer.asInstanceOf[Buffer].clear()
    _buffer.put(indices)
    _buffer.asInstanceOf[Buffer].flip()
    indices.asInstanceOf[Buffer].position(pos)
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() << 1)

    if isBound then {
      Sge().graphics.gl20.glBufferData(BufferTarget.ElementArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }
  }

  override def updateIndices(targetOffset: Int, indices: Array[Short], offset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 2)
    BufferUtils.copy(indices, offset, byteBuffer, count)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    _buffer.asInstanceOf[Buffer].position(0)

    if isBound then {
      Sge().graphics.gl20.glBufferData(BufferTarget.ElementArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }
  }

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  @deprecated("use getBuffer(boolean) instead")
  override def buffer: ShortBuffer = {
    isDirty = true
    _buffer
  }

  override def getBuffer(forWriting: Boolean): ShortBuffer = {
    isDirty |= forWriting
    _buffer
  }

  /** Binds this IndexBufferObject for rendering with glDrawElements. */
  def bind(): Unit = {
    if bufferHandle == 0 then throw SgeError.GraphicsError("No buffer allocated!")

    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, bufferHandle)
    if isDirty then {
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() * 2)
      Sge().graphics.gl20.glBufferData(BufferTarget.ElementArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }
    isBound = true
  }

  /** Unbinds this IndexBufferObject. */
  def unbind(): Unit = {
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, 0)
    isBound = false
  }

  /** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  def invalidate(): Unit = {
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    isDirty = true
  }

  /** Closes this IndexBufferObject and all its associated OpenGL resources. */
  override def close(): Unit = {
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, 0)
    Sge().graphics.gl20.glDeleteBuffer(bufferHandle)
    bufferHandle = 0

    if ownsBuffer then BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
  }
}
