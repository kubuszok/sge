/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/IndexBufferObjectSubData.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Idiom: typed GL enums -- BufferTarget, BufferUsage
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 182
 * Covenant-baseline-methods: IndexBufferObjectSubData,_buffer,bind,bufferHandle,byteBuffer,close,createBufferObject,getBuffer,gl,invalidate,isBound,isDirect,isDirty,numIndices,numMaxIndices,pos,result,setIndices,this,unbind,updateIndices,usage
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/IndexBufferObjectSubData.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 433466a3e68f1d847c72b566d0027400ae461e6c
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ShortBuffer

import sge.Sge
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
class IndexBufferObjectSubData(isStatic: Boolean, maxIndices: Int)(using Sge) extends IndexData with AutoCloseable {
  private var _buffer:      ShortBuffer = uninitialized
  private var byteBuffer:   ByteBuffer  = uninitialized
  private var bufferHandle: Int         = uninitialized
  @nowarn("msg=not read") // set in constructor, will be read when buffer operations implemented
  private var isDirect: Boolean     = uninitialized
  private var isDirty:  Boolean     = true
  private var isBound:  Boolean     = false
  private var usage:    BufferUsage = uninitialized

  byteBuffer = BufferUtils.newByteBuffer(maxIndices * 2)
  isDirect = true

  usage = if isStatic then BufferUsage.StaticDraw else BufferUsage.DynamicDraw
  _buffer = byteBuffer.asShortBuffer()
  _buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()
  bufferHandle = createBufferObject()

  /** Creates a new IndexBufferObject to be used with vertex arrays.
    *
    * @param maxIndices
    *   the maximum number of indices this buffer can hold
    */
  def this(maxIndices: Int)(using Sge) =
    this(true, maxIndices)

  private def createBufferObject(): Int = {
    val result = Sge().graphics.gl20.glGenBuffer()
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, result)
    Sge().graphics.gl20.glBufferData(BufferTarget.ElementArrayBuffer, byteBuffer.capacity(), null, usage)
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, 0)
    result
  }

  /** @return the number of indices currently stored in this buffer */
  def numIndices: Int =
    _buffer.limit()

  /** @return the maximum number of indices this IndexBufferObject can store. */
  def numMaxIndices: Int =
    _buffer.capacity()

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
    _buffer.asInstanceOf[Buffer].clear()
    _buffer.put(indices, offset, count)
    _buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(count << 1)

    if isBound then {
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ElementArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
  }

  def setIndices(indices: ShortBuffer): Unit = {
    val pos = indices.position()
    isDirty = true
    _buffer.asInstanceOf[Buffer].clear()
    _buffer.put(indices)
    _buffer.asInstanceOf[Buffer].flip()
    indices.asInstanceOf[Buffer].position(pos)
    byteBuffer.asInstanceOf[Buffer].position(0)
    byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() << 1)

    if isBound then {
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ElementArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
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
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ElementArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }
  }

  override def getBuffer(forWriting: Boolean): ShortBuffer = {
    isDirty |= forWriting
    _buffer
  }

  /** Binds this IndexBufferObject for rendering with glDrawElements. */
  def bind(): Unit = {
    if bufferHandle == 0 then throw SgeError.GraphicsError("IndexBufferObject cannot be used after it has been disposed.")

    Sge().graphics.gl20.glBindBuffer(BufferTarget.ElementArrayBuffer, bufferHandle)
    if isDirty then {
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() * 2)
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ElementArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
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
    bufferHandle = createBufferObject()
    isDirty = true
  }

  /** Closes this IndexBufferObject and all its associated OpenGL resources. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ElementArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
  }
}
