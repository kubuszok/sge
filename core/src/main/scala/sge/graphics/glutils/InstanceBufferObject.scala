/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/InstanceBufferObject.java
 * Original authors: mrdlink
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes
import sge.graphics.VertexAttribute
import sge.graphics.GL20
import sge.graphics.GL30
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.Sge

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import scala.compiletime.uninitialized

/** Modification of the VertexBufferObject class. Sets the glVertexAttribDivisor for every VertexAttribute automatically.
  *
  * @author
  *   mrdlink
  */
class InstanceBufferObject(isStatic: Boolean, numVertices: Int, instanceAttributes: VertexAttributes)(implicit sge: Sge) extends InstanceData {

  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(implicit sge: Sge) = {
    this(isStatic, numVertices, new VertexAttributes(attributes*))
  }

  private var attributes:   VertexAttributes = scala.compiletime.uninitialized
  private var buffer:       FloatBuffer      = scala.compiletime.uninitialized
  private var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
  private var ownsBuffer:   Boolean          = false
  private var bufferHandle: Int              = 0
  private var usage:        Int              = 0
  var isDirty:              Boolean          = false
  var isBound:              Boolean          = false

  if (sge.graphics.gl30.isEmpty)
    throw SgeError.GraphicsError("InstanceBufferObject requires a device running with GLES 3.0 compatibilty")

  bufferHandle = sge.graphics.gl20.glGenBuffer()

  val data = BufferUtils.newUnsafeByteBuffer(instanceAttributes.vertexSize * numVertices)
  data.asInstanceOf[Buffer].limit(0)
  setBuffer(data, true, instanceAttributes)
  setUsage(if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW)

  override def getAttributes(): VertexAttributes = attributes

  override def getNumInstances(): Int = buffer.limit() * 4 / attributes.vertexSize

  override def getNumMaxInstances(): Int = byteBuffer.capacity() / attributes.vertexSize

  /** @deprecated use getBuffer(Boolean) instead */
  @deprecated("use getBuffer(Boolean) instead", "1.0")
  override def getBuffer(): FloatBuffer = {
    isDirty = true
    buffer
  }

  override def getBuffer(forWriting: Boolean): FloatBuffer = {
    isDirty |= forWriting
    buffer
  }

  /** Low level method to reset the buffer and attributes to the specified values. Use with care!
    *
    * @param data
    * @param ownsBuffer
    * @param value
    */
  protected def setBuffer(data: Buffer, ownsBuffer: Boolean, value: VertexAttributes): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change attributes while VBO is bound")
    if (this.ownsBuffer && byteBuffer != null) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    attributes = value
    data match {
      case bb: ByteBuffer => byteBuffer = bb
      case _ => throw SgeError.GraphicsError("Only ByteBuffer is currently supported")
    }
    this.ownsBuffer = ownsBuffer

    val l = byteBuffer.limit()
    byteBuffer.asInstanceOf[Buffer].limit(byteBuffer.capacity())
    buffer = byteBuffer.asFloatBuffer()
    byteBuffer.asInstanceOf[Buffer].limit(l)
    buffer.asInstanceOf[Buffer].limit(l / 4)
  }

  private def bufferChanged()(implicit sge: Sge): Unit =
    if (isBound) {
      sge.graphics.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), null, usage)
      sge.graphics.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  override def setInstanceData(data: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(data, byteBuffer, count, offset)
    buffer.asInstanceOf[Buffer].position(0)
    buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def setInstanceData(data: FloatBuffer, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(data, byteBuffer, count)
    buffer.asInstanceOf[Buffer].position(0)
    buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    BufferUtils.copy(data, sourceOffset, count, byteBuffer)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: FloatBuffer, sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    data.asInstanceOf[Buffer].position(sourceOffset * 4)
    BufferUtils.copy(data, byteBuffer, count)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  /** @return
    *   The GL enum used in the call to GL20.glBufferData(int, int, java.nio.Buffer, int), e.g. GL_STATIC_DRAW or GL_DYNAMIC_DRAW
    */
  protected def getUsage(): Int = usage

  /** Set the GL enum used in the call to GL20.glBufferData(int, int, java.nio.Buffer, int), can only be called when the VBO is not bound.
    */
  protected def setUsage(value: Int): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change usage while VBO is bound")
    usage = value
  }

  /** Binds this InstanceBufferObject for rendering via glDrawArraysInstanced or glDrawElementsInstanced
    *
    * @param shader
    *   the shader
    */
  override def bind(shader: ShaderProgram): Unit =
    bind(shader, null)

  override def bind(shader: ShaderProgram, locations: Array[Int]): Unit = {
    val gl = sge.graphics.gl20

    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
    if (isDirty) {
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 4)
      gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

    val numAttributes = attributes.size
    if (locations == null) {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = shader.getAttributeLocation(attribute.alias)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.enableVertexAttribute(location + unitOffset)

          shader.setVertexAttribute(
            location + unitOffset,
            attribute.numComponents,
            attribute.`type`,
            attribute.normalized,
            attributes.vertexSize,
            attribute.offset
          )
          sge.graphics.gl30.foreach(_.glVertexAttribDivisor(location + unitOffset, 1))
        }
      }
    } else {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locations(i)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.enableVertexAttribute(location + unitOffset)

          shader.setVertexAttribute(
            location + unitOffset,
            attribute.numComponents,
            attribute.`type`,
            attribute.normalized,
            attributes.vertexSize,
            attribute.offset
          )
          sge.graphics.gl30.foreach(_.glVertexAttribDivisor(location + unitOffset, 1))
        }
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
    unbind(shader, null)

  override def unbind(shader: ShaderProgram, locations: Array[Int]): Unit = {
    val gl            = sge.graphics.gl20
    val numAttributes = attributes.size
    if (locations == null) {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = shader.getAttributeLocation(attribute.alias)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.disableVertexAttribute(location + unitOffset)
        }
      }
    } else {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locations(i)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.disableVertexAttribute(location + unitOffset)
        }
      }
    }
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    isBound = false
  }

  /** Invalidates the InstanceBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = sge.graphics.gl20.glGenBuffer()
    isDirty = true
  }

  /** Disposes of all resources this InstanceBufferObject uses. */
  override def close(): Unit = {
    val gl = sge.graphics.gl20
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
    if (ownsBuffer) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
  }
}
