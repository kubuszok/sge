/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/InstanceBufferObjectSubData.java
 * Original authors: mrdlink
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (using Sge) for GL calls
 *   Idiom: split packages
 *   Idiom: typed GL enums -- BufferTarget, BufferUsage
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes
import sge.graphics.VertexAttribute

import sge.utils.BufferUtils
import sge.utils.Nullable
import sge.utils.SgeError
import sge.Sge

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** Modification of the VertexBufferObjectSubData class. Sets the glVertexAttribDivisor for every VertexAttribute automatically.
  *
  * @author
  *   mrdlink
  */
class InstanceBufferObjectSubData(val isStatic: Boolean, initialNumInstances: Int, instanceAttributes: VertexAttributes)(using Sge) extends InstanceData {

  def this(isStatic: Boolean, initialNumInstances: Int, instanceAttributes: VertexAttribute*)(using Sge) = {
    this(isStatic, initialNumInstances, VertexAttributes(instanceAttributes*))
  }

  val attributes: VertexAttributes = if (instanceAttributes.nonEmpty) instanceAttributes else VertexAttributes()

  val byteBuffer:      ByteBuffer  = BufferUtils.newByteBuffer(this.attributes.vertexSize * initialNumInstances)
  private val _buffer: FloatBuffer = byteBuffer.asFloatBuffer()
  var bufferHandle:    Int         = 0
  val isDirect:        Boolean     = true
  val usage:           BufferUsage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw
  var isDirty:         Boolean     = false
  var isBound:         Boolean     = false

  // Initialize

  bufferHandle = createBufferObject()
  _buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()

  private def createBufferObject()(using Sge): Int = {
    val result = Sge().graphics.gl20.glGenBuffer()
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ArrayBuffer, result)
    Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.capacity(), null, usage)
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    result
  }

  /** Effectively returns numInstances.
    *
    * @return
    *   number of instances in this buffer
    */
  override def numInstances: Int = _buffer.limit() * 4 / attributes.vertexSize

  /** Effectively returns numMaxInstances.
    *
    * @return
    *   maximum number of instances in this buffer
    */
  override def numMaxInstances: Int = byteBuffer.capacity() / attributes.vertexSize

  /** @deprecated use getBuffer(Boolean) instead */
  @deprecated("use getBuffer(Boolean) instead", "1.0")
  override def buffer: FloatBuffer = {
    isDirty = true
    _buffer
  }

  override def getBuffer(forWriting: Boolean): FloatBuffer = {
    isDirty |= forWriting
    _buffer
  }

  private def bufferChanged()(using Sge): Unit =
    if (isBound) {
      Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), null, usage)
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }

  override def setInstanceData(data: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      BufferUtils.copy(data, byteBuffer, count, offset)
      _buffer.asInstanceOf[Buffer].position(0)
      _buffer.asInstanceOf[Buffer].limit(count)
    } else {
      _buffer.asInstanceOf[Buffer].clear()
      _buffer.put(data, offset, count)
      _buffer.asInstanceOf[Buffer].flip()
      byteBuffer.asInstanceOf[Buffer].position(0)
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() << 2)
    }

    bufferChanged()
  }

  override def setInstanceData(data: FloatBuffer, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      BufferUtils.copy(data, byteBuffer, count)
      _buffer.asInstanceOf[Buffer].position(0)
      _buffer.asInstanceOf[Buffer].limit(count)
    } else {
      _buffer.asInstanceOf[Buffer].clear()
      _buffer.put(data)
      _buffer.asInstanceOf[Buffer].flip()
      byteBuffer.asInstanceOf[Buffer].position(0)
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() << 2)
    }

    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      val pos = byteBuffer.position()
      byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
      BufferUtils.copy(data, sourceOffset, count, byteBuffer)
      byteBuffer.asInstanceOf[Buffer].position(pos)
    } else
      throw SgeError.GraphicsError("InstanceBufferObjectSubData is not direct")

    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: FloatBuffer, sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      val pos = byteBuffer.position()
      byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
      data.asInstanceOf[Buffer].position(sourceOffset * 4)
      BufferUtils.copy(data, byteBuffer, count)
      byteBuffer.asInstanceOf[Buffer].position(pos)
    } else
      throw SgeError.GraphicsError("InstanceBufferObjectSubData is not direct")

    bufferChanged()
  }

  /** Binds this InstanceBufferObject for rendering via glDrawArraysInstanced or glDrawElementsInstanced
    *
    * @param shader
    *   the shader
    */
  override def bind(shader: ShaderProgram): Unit =
    bind(shader, Nullable.empty)

  override def bind(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    val gl = Sge().graphics.gl20

    gl.glBindBuffer(BufferTarget.ArrayBuffer, bufferHandle)
    if (isDirty) {
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() * 4)
      gl.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

    val numAttributes = attributes.size
    for (i <- 0 until numAttributes) {
      val attribute = attributes.get(i)
      val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
      if (location != AttributeLocation.notFound) {
        val unitOffset = attribute.unit
        val loc        = AttributeLocation(location.toInt + unitOffset)
        shader.enableVertexAttribute(loc)

        shader.setVertexAttribute(
          loc,
          attribute.numComponents,
          attribute.`type`,
          attribute.normalized,
          attributes.vertexSize,
          attribute.offset
        )
        Sge().graphics.gl30.foreach(_.glVertexAttribDivisor(loc.toInt, 1))
      }
    }
    isBound = true
  }

  /** Unbinds this InstanceBufferObject.
    *
    * @param shader
    *   the shader
    */
  override def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty)

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    val gl            = Sge().graphics.gl20
    val numAttributes = attributes.size
    locations.fold {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = shader.getAttributeLocation(attribute.alias)
        if (location != AttributeLocation.notFound) {
          val unitOffset = attribute.unit
          shader.disableVertexAttribute(AttributeLocation(location.toInt + unitOffset))
        }
      }
    } { locs =>
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locs(i)
        if (location != AttributeLocation.notFound) {
          val unitOffset = attribute.unit
          shader.enableVertexAttribute(AttributeLocation(location.toInt + unitOffset))
        }
      }
    }
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    isBound = false
  }

  /** Invalidates the InstanceBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = createBufferObject()
    isDirty = true
  }

  /** Disposes of all resources this InstanceBufferObject uses. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
  }

  /** Returns the InstanceBufferObject handle — use `bufferHandle` field directly */
}
