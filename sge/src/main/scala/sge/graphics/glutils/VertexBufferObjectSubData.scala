/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/VertexBufferObjectSubData.java
 * Original authors: mzechner
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
 * Covenant-baseline-loc: 192
 * Covenant-baseline-methods: VertexBufferObjectSubData,_buffer,attributes,bind,buffer,bufferChanged,bufferHandle,byteBuffer,close,createBufferObject,getBuffer,gl,invalidate,isBound,isDirect,isDirty,isStatic,numAttributes,numMaxVertices,numVertices,result,setVertices,this,unbind,updateVertices,usage
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/VertexBufferObjectSubData.java
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
  *   mzechner
  */
class VertexBufferObjectSubData(
  val isStatic:       Boolean,
  initialNumVertices: Int,
  val attributes:     VertexAttributes
)(using Sge)
    extends VertexData {

  val byteBuffer:      ByteBuffer  = BufferUtils.newByteBuffer(attributes.vertexSize * initialNumVertices)
  val isDirect:        Boolean     = true
  val usage:           BufferUsage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw
  private val _buffer: FloatBuffer = byteBuffer.asFloatBuffer()
  var bufferHandle:    Int         = createBufferObject()
  var isDirty = false
  var isBound = false

  _buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()

  /** Constructs a new interleaved VertexBufferObject.
    *
    * @param isStatic
    *   whether the vertex data is static.
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link VertexAttributes} .
    */
  def this(isStatic: Boolean, initialNumVertices: Int, attributes: VertexAttribute*)(using Sge) =
    this(isStatic, initialNumVertices, VertexAttributes(attributes*))

  private def createBufferObject(): Int = {
    val result = Sge().graphics.gl20.glGenBuffer()
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ArrayBuffer, result)
    Sge().graphics.gl20.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.capacity(), null, usage) // @nowarn — GL API: null allocates buffer without initializing data
    Sge().graphics.gl20.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    result
  }

  override def numVertices: Int = _buffer.limit() * 4 / attributes.vertexSize

  override def numMaxVertices: Int = byteBuffer.capacity() / attributes.vertexSize

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

  private def bufferChanged(): Unit =
    if (isBound) {
      Sge().graphics.gl20.glBufferSubData(BufferTarget.ArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }

  override def setVertices(vertices: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      BufferUtils.copy(vertices, byteBuffer, count, offset)
      _buffer.asInstanceOf[Buffer].position(0)
      _buffer.asInstanceOf[Buffer].limit(count)
    } else {
      _buffer.asInstanceOf[Buffer].clear()
      _buffer.put(vertices, offset, count)
      _buffer.asInstanceOf[Buffer].flip()
      byteBuffer.asInstanceOf[Buffer].position(0)
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() << 2)
    }

    bufferChanged()
  }

  override def updateVertices(targetOffset: Int, vertices: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    if (isDirect) {
      val pos = byteBuffer.position()
      byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
      BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
      byteBuffer.asInstanceOf[Buffer].position(pos)
    } else {
      throw SgeError.GraphicsError("Buffer must be allocated direct.") // Should never happen
    }

    bufferChanged()
  }

  /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
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
      gl.glBufferSubData(BufferTarget.ArrayBuffer, 0, byteBuffer.limit(), byteBuffer)
      isDirty = false
    }

    val numAttributes = attributes.size
    for (i <- 0 until numAttributes) {
      val attribute = attributes.get(i)
      val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
      if (location != AttributeLocation.notFound) {
        shader.enableVertexAttribute(location)
        shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, attribute.offset)
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
    val numAttributes = attributes.size
    locations.fold {
      for (i <- 0 until numAttributes)
        shader.disableVertexAttribute(attributes.get(i).alias)
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
  def invalidate(): Unit = {
    bufferHandle = createBufferObject()
    isDirty = true
  }

  /** Disposes of all resources this VertexBufferObject uses. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
  }

  /** Returns the VBO handle — use `bufferHandle` field directly */
}
