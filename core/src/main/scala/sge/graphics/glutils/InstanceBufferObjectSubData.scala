/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/InstanceBufferObjectSubData.java
 * Original authors: mrdlink
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (implicit sge: Sge) style (older form of context parameter)
 *   Idiom: split packages
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   TODO: typed GL enums -- BufferTarget -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes
import sge.graphics.VertexAttribute
import sge.graphics.GL20
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
class InstanceBufferObjectSubData(val isStatic: Boolean, numInstances: Int, instanceAttributes: VertexAttributes)(implicit sge: Sge) extends InstanceData {

  def this(isStatic: Boolean, numInstances: Int, instanceAttributes: VertexAttribute*)(implicit sge: Sge) =
    this(isStatic, numInstances, VertexAttributes(instanceAttributes*))

  val attributes: VertexAttributes = if (instanceAttributes.nonEmpty) instanceAttributes else VertexAttributes()

  val byteBuffer:   ByteBuffer  = BufferUtils.newByteBuffer(this.attributes.vertexSize * numInstances)
  val buffer:       FloatBuffer = byteBuffer.asFloatBuffer()
  var bufferHandle: Int         = 0
  val isDirect:     Boolean     = true
  val usage:        Int         = if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
  var isDirty:      Boolean     = false
  var isBound:      Boolean     = false

  // Initialize

  bufferHandle = createBufferObject()
  buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()

  private def createBufferObject()(implicit sge: Sge): Int = {
    val result = sge.graphics.gl20.glGenBuffer()
    sge.graphics.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, result)
    sge.graphics.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.capacity(), null, usage)
    sge.graphics.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    result
  }

  override def getAttributes(): VertexAttributes = attributes

  /** Effectively returns getNumInstances().
    *
    * @return
    *   number of instances in this buffer
    */
  override def getNumInstances(): Int = buffer.limit() * 4 / attributes.vertexSize

  /** Effectively returns getNumMaxInstances().
    *
    * @return
    *   maximum number of instances in this buffer
    */
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

  private def bufferChanged()(implicit sge: Sge): Unit =
    if (isBound) {
      sge.graphics.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), null, usage)
      sge.graphics.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }

  override def setInstanceData(data: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      BufferUtils.copy(data, byteBuffer, count, offset)
      buffer.asInstanceOf[Buffer].position(0)
      buffer.asInstanceOf[Buffer].limit(count)
    } else {
      buffer.asInstanceOf[Buffer].clear()
      buffer.put(data, offset, count)
      buffer.asInstanceOf[Buffer].flip()
      byteBuffer.asInstanceOf[Buffer].position(0)
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() << 2)
    }

    bufferChanged()
  }

  override def setInstanceData(data: FloatBuffer, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      BufferUtils.copy(data, byteBuffer, count)
      buffer.asInstanceOf[Buffer].position(0)
      buffer.asInstanceOf[Buffer].limit(count)
    } else {
      buffer.asInstanceOf[Buffer].clear()
      buffer.put(data)
      buffer.asInstanceOf[Buffer].flip()
      byteBuffer.asInstanceOf[Buffer].position(0)
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() << 2)
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

  override def bind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    val gl = sge.graphics.gl20

    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
    if (isDirty) {
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 4)
      gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

    val numAttributes = attributes.size
    for (i <- 0 until numAttributes) {
      val attribute = attributes.get(i)
      val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
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
    isBound = true
  }

  /** Unbinds this InstanceBufferObject.
    *
    * @param shader
    *   the shader
    */
  override def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty)

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    val gl            = sge.graphics.gl20
    val numAttributes = attributes.size
    locations.fold {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = shader.getAttributeLocation(attribute.alias)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.disableVertexAttribute(location + unitOffset)
        }
      }
    } { locs =>
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locs(i)
        if (location >= 0) {
          val unitOffset = attribute.unit
          shader.enableVertexAttribute(location + unitOffset)
        }
      }
    }
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    isBound = false
  }

  /** Invalidates the InstanceBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = createBufferObject()
    isDirty = true
  }

  /** Disposes of all resources this InstanceBufferObject uses. */
  override def close(): Unit = {
    val gl = sge.graphics.gl20
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
  }

  /** Returns the InstanceBufferObject handle
    *
    * @return
    *   the InstanceBufferObject handle
    */
  def getBufferHandle(): Int = bufferHandle
}
