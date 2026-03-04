/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/IndexBufferObjectSubData.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Issues: package declaration uses flat package sge.graphics.glutils instead of split package convention
 *   TODO: uses flat package declaration -- convert to split (package sge / package graphics / package glutils)
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   TODO: typed GL enums -- BufferTarget -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.graphics.glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

import sge.Sge
import sge.graphics.GL20
import sge.utils.BufferUtils
import sge.utils.SgeError
import scala.annotation.nowarn
import scala.compiletime.uninitialized

/** <p> IndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs. </p>
  *
  * <p> You can also use this to store indices for vertex arrays. Do not call {@link #bind()} or {@link #unbind()} in this case but rather use {@link #getBuffer()} to use the buffer directly with
  * glDrawElements. You must also create the IndexBufferObject with the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers. </p>
  *
  * <p> VertexBufferObjects must be disposed via the {@link #close()} method when no longer needed </p>
  *
  * @author
  *   mzechner
  */
class IndexBufferObjectSubData(isStatic: Boolean, maxIndices: Int)(using sde: Sge) extends IndexData with AutoCloseable {
  private var buffer:       ShortBuffer = uninitialized
  private var byteBuffer:   ByteBuffer  = uninitialized
  private var bufferHandle: Int         = uninitialized
  @nowarn("msg=not read") // set in constructor, will be read when buffer operations implemented
  private var isDirect: Boolean = uninitialized
  private var isDirty:  Boolean = true
  private var isBound:  Boolean = false
  private var usage:    Int     = uninitialized

  byteBuffer = BufferUtils.newByteBuffer(maxIndices * 2)
  isDirect = true

  usage = if isStatic then GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
  buffer = byteBuffer.asShortBuffer()
  buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()
  bufferHandle = createBufferObject()

  /** Creates a new IndexBufferObject to be used with vertex arrays.
    *
    * @param maxIndices
    *   the maximum number of indices this buffer can hold
    */
  def this(maxIndices: Int)(using sde: Sge) =
    this(true, maxIndices)

  private def createBufferObject(): Int = {
    val result = sde.graphics.gl20.glGenBuffer()
    sde.graphics.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, result)
    sde.graphics.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.capacity(), null, usage)
    sde.graphics.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    result
  }

  /** @return the number of indices currently stored in this buffer */
  def getNumIndices(): Int =
    buffer.limit()

  /** @return the maximum number of indices this IndexBufferObject can store. */
  def getNumMaxIndices(): Int =
    buffer.capacity()

  /** <p> Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be copied to this IndexBufferObject. </p>
    *
    * <p> This can be called in between calls to {@link #bind()} and {@link #unbind()} . The index data will be updated instantly. </p>
    *
    * @param indices
    *   the vertex data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def setIndices(indices: Array[Short], offset: Int, count: Int): Unit = {
    isDirty = true
    buffer.asInstanceOf[Buffer].clear()
    buffer.put(indices, offset, count)
    buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(count << 1)

    if isBound then {
      sde.graphics.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
  }

  def setIndices(indices: ShortBuffer): Unit = {
    val pos = indices.position()
    isDirty = true
    buffer.asInstanceOf[Buffer].clear()
    buffer.put(indices)
    buffer.asInstanceOf[Buffer].flip()
    indices.asInstanceOf[Buffer].position(pos)
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() << 1)

    if isBound then {
      sde.graphics.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
  }

  override def updateIndices(targetOffset: Int, indices: Array[Short], offset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 2)
    BufferUtils.copy(indices, offset, byteBuffer, count)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    buffer.asInstanceOf[Buffer].position(0)

    if isBound then {
      sde.graphics.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
  }

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  @deprecated("use getBuffer(boolean) instead")
  override def getBuffer(): ShortBuffer = {
    isDirty = true
    buffer
  }

  override def getBuffer(forWriting: Boolean): ShortBuffer = {
    isDirty |= forWriting
    buffer
  }

  /** Binds this IndexBufferObject for rendering with glDrawElements. */
  def bind(): Unit = {
    if bufferHandle == 0 then throw SgeError.GraphicsError("IndexBufferObject cannot be used after it has been disposed.")

    sde.graphics.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle)
    if isDirty then {
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 2)
      sde.graphics.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
    isBound = true
  }

  /** Unbinds this IndexBufferObject. */
  def unbind(): Unit = {
    sde.graphics.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    isBound = false
  }

  /** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  def invalidate(): Unit = {
    bufferHandle = createBufferObject()
    isDirty = true
  }

  /** Closes this IndexBufferObject and all its associated OpenGL resources. */
  override def close(): Unit = {
    val gl = sde.graphics.gl20
    gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
  }
}
