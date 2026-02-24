/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/InstanceData.java
 * Original authors: mrdlink
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes

import java.nio.FloatBuffer

/** A InstanceData instance holds instance data for rendering with OpenGL. It is implemented as either a InstanceBufferObject or a InstanceBufferObjectSubData. Both require Open GL 3.3+.
  *
  * @author
  *   mrdlink
  */
trait InstanceData extends AutoCloseable {

  /** @return the number of vertices this InstanceData stores */
  def getNumInstances(): Int

  /** @return the number of vertices this InstanceData can store */
  def getNumMaxInstances(): Int

  /** @return the VertexAttributes as specified during construction. */
  def getAttributes(): VertexAttributes

  /** Sets the vertices of this InstanceData, discarding the old vertex data. The count must equal the number of floats per vertex times the number of vertices to be copied to this VertexData. The
    * order of the vertex attributes must be the same as specified at construction time via VertexAttributes. <p> This can be called in between calls to bind and unbind. The vertex data will be
    * updated instantly.
    *
    * @param data
    *   the instance data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def setInstanceData(data: Array[Float], offset: Int, count: Int): Unit

  /** Update (a portion of) the vertices. Does not resize the backing buffer.
    *
    * @param data
    *   the instance data
    * @param sourceOffset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def updateInstanceData(targetOffset: Int, data: Array[Float], sourceOffset: Int, count: Int): Unit

  /** Sets the vertices of this InstanceData, discarding the old vertex data. The count must equal the number of floats per vertex times the number of vertices to be copied to this InstanceData. The
    * order of the vertex attributes must be the same as specified at construction time via VertexAttributes. <p> This can be called in between calls to bind and unbind. The vertex data will be
    * updated instantly.
    *
    * @param data
    *   the instance data
    * @param count
    *   the number of floats to copy
    */
  def setInstanceData(data: FloatBuffer, count: Int): Unit

  /** Update (a portion of) the vertices. Does not resize the backing buffer.
    *
    * @param data
    *   the vertex data
    * @param sourceOffset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def updateInstanceData(targetOffset: Int, data: FloatBuffer, sourceOffset: Int, count: Int): Unit

  /** Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to bind. If you need immediate uploading use setInstanceData(Array[Float],
    * Int, Int); Any modifications made to the Buffer *after* the call to bind will not automatically be uploaded.
    *
    * @return
    *   the underlying FloatBuffer holding the vertex data.
    * @deprecated
    *   use getBuffer(Boolean) instead
    */
  @deprecated("use getBuffer(Boolean) instead", "1.0")
  def getBuffer(): FloatBuffer

  /** Returns the underlying FloatBuffer for reading or writing.
    * @param forWriting
    *   when true, the underlying buffer will be uploaded on the next call to bind. If you need immediate uploading use setInstanceData(Array[Float], Int, Int).
    * @return
    *   the underlying FloatBuffer holding the vertex data.
    */
  def getBuffer(forWriting: Boolean): FloatBuffer

  /** Binds this InstanceData for rendering via glDrawArraysInstanced or glDrawElementsInstanced. */
  def bind(shader: ShaderProgram): Unit

  /** Binds this InstanceData for rendering via glDrawArraysInstanced or glDrawElementsInstanced.
    *
    * @param locations
    *   array containing the attribute locations.
    */
  def bind(shader: ShaderProgram, locations: Array[Int]): Unit

  /** Unbinds this InstanceData. */
  def unbind(shader: ShaderProgram): Unit

  /** Unbinds this InstanceData.
    *
    * @param locations
    *   array containing the attribute locations.
    */
  def unbind(shader: ShaderProgram, locations: Array[Int]): Unit

  /** Invalidates the InstanceData if applicable. Use this in case of a context loss. */
  def invalidate(): Unit

  /** Disposes this InstanceData and all its associated OpenGL resources. */
  def close(): Unit
}
