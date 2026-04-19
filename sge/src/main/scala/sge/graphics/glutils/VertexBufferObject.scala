/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/VertexBufferObject.java
 * Original authors: mzechner, Dave Clayton <contact@redskyforge.com>
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
 * Covenant-baseline-loc: 229
 * Covenant-baseline-methods: VertexBufferObject,_attributes,_buffer,_usage,attributes,bind,buffer,bufferChanged,bufferHandle,byteBuffer,close,data,getBuffer,gl,init,invalidate,isBound,isDirty,l,numAttributes,numMaxVertices,numVertices,ownsBuffer,pos,setBuffer,setVertices,this,unbind,updateVertices,usage,usage_
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/VertexBufferObject.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package glutils

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import sge.utils.{ BufferUtils, Nullable, SgeError };

/** <p> A {@link VertexData} implementation based on OpenGL vertex buffer objects. <p> If the OpenGL ES context was lost you can call {@link #invalidate()} to recreate a new OpenGL vertex buffer
  * object. <p> The data is bound via glVertexAttribPointer() according to the attribute aliases specified via {@link VertexAttributes} in the constructor. <p> VertexBufferObjects must be disposed via
  * the {@link #dispose()} method when no longer needed
  *
  * @author
  *   mzechner, Dave Clayton <contact@redskyforge.com>
  */
class VertexBufferObject(using Sge) extends VertexData {
  private var _attributes:  VertexAttributes = scala.compiletime.uninitialized
  private var _buffer:      FloatBuffer      = scala.compiletime.uninitialized
  private var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
  private var ownsBuffer:   Boolean          = scala.compiletime.uninitialized
  private var bufferHandle: Int              = scala.compiletime.uninitialized
  private var _usage:       BufferUsage      = scala.compiletime.uninitialized
  var isDirty = false
  var isBound = false

  /** Constructs a new interleaved VertexBufferObject.
    *
    * @param isStatic
    *   whether the vertex data is static.
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link VertexAttribute} s.
    */
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(using Sge) = {
    this()
    this.init(isStatic, numVertices, VertexAttributes(attributes*))
  }

  /** Constructs a new interleaved VertexBufferObject.
    *
    * @param isStatic
    *   whether the vertex data is static.
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link VertexAttributes} .
    */
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes)(using Sge) = {
    this()
    this.init(isStatic, numVertices, attributes)
  }

  protected def this(usage: BufferUsage, data: ByteBuffer, ownsBuffer: Boolean, attributes: VertexAttributes)(using Sge) = {
    this()
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    setBuffer(data, ownsBuffer, attributes)
    this.usage = usage
  }

  private def init(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes): Unit = {
    bufferHandle = Sge().graphics.gl20.glGenBuffer()

    val data = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * numVertices)
    data.asInstanceOf[Buffer].limit(0)
    setBuffer(data, true, attributes)
    usage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw
  }

  override def attributes: VertexAttributes = _attributes

  override def numVertices: Int = _buffer.limit() * 4 / _attributes.vertexSize

  override def numMaxVertices: Int = byteBuffer.capacity() / _attributes.vertexSize

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  @deprecated("use getBuffer(boolean) instead", "")
  override def buffer: FloatBuffer = {
    isDirty = true
    _buffer
  }

  override def getBuffer(forWriting: Boolean): FloatBuffer = {
    isDirty = isDirty || forWriting
    _buffer
  }

  /** Low level method to reset the buffer and attributes to the specified values. Use with care!
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

  private def bufferChanged(): Unit =
    if (isBound) {
      Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  override def setVertices(vertices: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(vertices, byteBuffer, count, offset)
    _buffer.asInstanceOf[Buffer].position(0)
    _buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def updateVertices(targetOffset: Int, vertices: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    _buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  /** @return
    *   The GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)} , e.g. GL_STATIC_DRAW or GL_DYNAMIC_DRAW
    */
  protected def usage: BufferUsage = _usage

  /** Set the GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)} , can only be called when the VBO is not bound.
    */
  protected def usage_=(value: BufferUsage): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change usage while VBO is bound")
    _usage = value
  }

  /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
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

    val numAttributes = _attributes.size
    for (i <- 0 until numAttributes) {
      val attribute = _attributes.get(i)
      val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
      if (location != AttributeLocation.notFound) {
        shader.enableVertexAttribute(location)
        shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, _attributes.vertexSize, attribute.offset)
      }
    }
    isBound = true
  }

  /** Unbinds this VertexBufferObject.
    *
    * @param shader
    *   the shader
    */
  override def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty)

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    val gl            = Sge().graphics.gl20
    val numAttributes = _attributes.size
    locations.fold {
      for (i <- 0 until numAttributes)
        shader.disableVertexAttribute(_attributes.get(i).alias)
    } { locs =>
      for (i <- 0 until numAttributes) {
        val location = locs(i)
        if (location != AttributeLocation.notFound) shader.disableVertexAttribute(location)
      }
    }
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    isBound = false
  }

  /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    isDirty = true
  }

  /** Disposes of all resources this VertexBufferObject uses. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
    if (ownsBuffer) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
  }
}
