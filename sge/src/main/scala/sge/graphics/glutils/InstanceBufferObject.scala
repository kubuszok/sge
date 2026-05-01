/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/InstanceBufferObject.java
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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 234
 * Covenant-baseline-methods: InstanceBufferObject,_attributes,_buffer,_usage,attributes,bind,bufferChanged,bufferHandle,byteBuffer,close,data,getBuffer,gl,invalidate,isBound,isDirty,l,numAttributes,numInstances,numMaxInstances,ownsBuffer,pos,setBuffer,setInstanceData,this,unbind,updateInstanceData,usage,usage_
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/InstanceBufferObject.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: b6d788be694d082fd715b92c0d18530c827e209c
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes
import sge.graphics.VertexAttribute
import sge.utils.{ BufferUtils, Nullable, SgeError }
import sge.Sge

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** Modification of the VertexBufferObject class. Sets the glVertexAttribDivisor for every VertexAttribute automatically.
  *
  * @author
  *   mrdlink
  */
class InstanceBufferObject(isStatic: Boolean, numVertices: Int, instanceAttributes: VertexAttributes)(using Sge) extends InstanceData {

  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(using Sge) =
    this(isStatic, numVertices, VertexAttributes(attributes*))

  private var _attributes:  VertexAttributes = scala.compiletime.uninitialized
  private var _buffer:      FloatBuffer      = scala.compiletime.uninitialized
  private var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
  private var ownsBuffer:   Boolean          = false
  private var bufferHandle: Int              = 0
  private var _usage:       BufferUsage      = BufferUsage(0)
  var isDirty:              Boolean          = false
  var isBound:              Boolean          = false

  if (Sge().graphics.gl30.isEmpty)
    throw SgeError.GraphicsError("InstanceBufferObject requires a device running with GLES 3.0 compatibilty")

  bufferHandle = Sge().graphics.gl20.glGenBuffer()

  val data = BufferUtils.newUnsafeByteBuffer(instanceAttributes.vertexSize * numVertices)
  data.asInstanceOf[Buffer].limit(0)
  setBuffer(data, true, instanceAttributes)
  usage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw

  override def attributes: VertexAttributes = _attributes

  override def numInstances: Int = _buffer.limit() * 4 / _attributes.vertexSize

  override def numMaxInstances: Int = byteBuffer.capacity() / _attributes.vertexSize

  override def getBuffer(forWriting: Boolean): FloatBuffer = {
    isDirty |= forWriting
    _buffer
  }

  /** Low level method to reset the buffer and attributes to the specified values. Use with care!
    *
    * @param data
    * @param ownsBuffer
    * @param value
    */
  protected def setBuffer(data: Buffer, ownsBuffer: Boolean, value: VertexAttributes): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change attributes while VBO is bound")
    if (this.ownsBuffer && Nullable(byteBuffer).isDefined) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    _attributes = value
    data match {
      case bb: ByteBuffer => byteBuffer = bb
      case _ => throw SgeError.GraphicsError("Only ByteBuffer is currently supported")
    }
    this.ownsBuffer = ownsBuffer

    val l = byteBuffer.limit()
    byteBuffer.asInstanceOf[Buffer].limit(byteBuffer.capacity())
    _buffer = byteBuffer.asFloatBuffer()
    byteBuffer.asInstanceOf[Buffer].limit(l)
    _buffer.asInstanceOf[Buffer].limit(l / 4)
  }

  private def bufferChanged()(using Sge): Unit =
    if (isBound) {
      Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), null, usage)
      Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  override def setInstanceData(data: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(data, byteBuffer, count, offset)
    _buffer.asInstanceOf[Buffer].position(0)
    _buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def setInstanceData(data: FloatBuffer, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(data, byteBuffer, count)
    _buffer.asInstanceOf[Buffer].position(0)
    _buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    BufferUtils.copy(data, sourceOffset, count, byteBuffer)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    _buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  override def updateInstanceData(targetOffset: Int, data: FloatBuffer, sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    data.asInstanceOf[Buffer].position(sourceOffset * 4)
    BufferUtils.copy(data, byteBuffer, count)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    _buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  /** @return
    *   The GL enum used in the call to GL20.glBufferData(int, int, java.nio.Buffer, int), e.g. GL_STATIC_DRAW or GL_DYNAMIC_DRAW
    */
  protected def usage: BufferUsage = _usage

  /** Set the GL enum used in the call to GL20.glBufferData(int, int, java.nio.Buffer, int), can only be called when the VBO is not bound.
    */
  protected def usage_=(value: BufferUsage): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change usage while VBO is bound")
    _usage = value
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
    for (i <- 0 until numAttributes) {
      val attribute = attributes.get(i)
      val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
      if (location != AttributeLocation.notFound) {
        val unitOffset = attribute.unit
        shader.disableVertexAttribute(AttributeLocation(location.toInt + unitOffset))
      }
    }
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    isBound = false
  }

  /** Invalidates the InstanceBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    isDirty = true
  }

  /** Disposes of all resources this InstanceBufferObject uses. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
    if (ownsBuffer) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
  }
}
