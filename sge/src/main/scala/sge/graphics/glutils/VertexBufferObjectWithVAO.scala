/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/VertexBufferObjectWithVAO.java
 * Original authors: mzechner, Dave Clayton <contact@redskyforge.com>, Nate Austin <nate.austin gmail>
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses DynamicArray[AttributeLocation] for cached locations; uses (using Sge)
 *   Idiom: split packages
 *   Idiom: typed GL enums -- BufferTarget, BufferUsage, AttributeLocation
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;;

import sge.utils.{ BufferUtils, SgeError }
import sge.utils.DynamicArray
import sge.utils.Nullable

/** <p> A {@link VertexData} implementation that uses vertex buffer objects and vertex array objects. (This is required for OpenGL 3.0+ core profiles. In particular, the default VAO has been
  * deprecated, as has the use of client memory for passing vertex attributes.) Use of VAOs should give a slight performance benefit since you don't have to bind the attributes on every draw anymore.
  * </p>
  *
  * <p> If the OpenGL ES context was lost you can call {@link #invalidate()} to recreate a new OpenGL vertex buffer object. </p>
  *
  * <p> VertexBufferObjectWithVAO objects must be disposed via the {@link #dispose()} method when no longer needed </p>
  *
  * Code adapted from {@link VertexBufferObject} .
  * @author
  *   mzechner, Dave Clayton <contact@redskyforge.com>, Nate Austin <nate.austin gmail>
  */
class VertexBufferObjectWithVAO(using Sge) extends VertexData {

  private var _attributes: VertexAttributes = scala.compiletime.uninitialized
  private var _buffer:     FloatBuffer      = scala.compiletime.uninitialized
  var byteBuffer:          ByteBuffer       = scala.compiletime.uninitialized
  var ownsBuffer:          Boolean          = scala.compiletime.uninitialized
  var bufferHandle:        Int              = scala.compiletime.uninitialized
  var isStatic:            Boolean          = scala.compiletime.uninitialized
  var usage:               BufferUsage      = scala.compiletime.uninitialized
  var isDirty         = false
  var isBound         = false
  var vaoHandle       = -1
  var cachedLocations = DynamicArray[AttributeLocation]()

  private def gl30: GL30 = Sge().graphics.gl30.getOrElse(
    throw SgeError.GraphicsError("VertexBufferObjectWithVAO requires GL30")
  )

  /** Constructs a new interleaved VertexBufferObjectWithVAO.
    *
    * @param isStatic
    *   whether the vertex data is static.
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link com.badlogic.gdx.graphics.VertexAttribute} s.
    */
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(using Sge) = {
    this()
    init(isStatic, numVertices, VertexAttributes(attributes*))
  }

  /** Constructs a new interleaved VertexBufferObjectWithVAO.
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
    init(isStatic, numVertices, attributes)
  }

  def this(isStatic: Boolean, unmanagedBuffer: ByteBuffer, attributes: VertexAttributes)(using Sge) = {
    this()
    this.isStatic = isStatic
    this._attributes = attributes

    byteBuffer = unmanagedBuffer
    ownsBuffer = false
    _buffer = byteBuffer.asFloatBuffer()
    _buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].flip()
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    usage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw
    createVAO()
  }

  private def init(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes): Unit = {
    this.isStatic = isStatic
    this._attributes = attributes

    byteBuffer = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * numVertices)
    _buffer = byteBuffer.asFloatBuffer()
    ownsBuffer = true
    _buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].flip()
    bufferHandle = Sge().graphics.gl20.glGenBuffer()
    usage = if (isStatic) BufferUsage.StaticDraw else BufferUsage.DynamicDraw
    createVAO()
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

  private def bufferChanged(): Unit =
    if (isBound) {
      Sge().graphics.gl20.glBindBuffer(BufferTarget.ArrayBuffer, bufferHandle)
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

  /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
    *
    * @param shader
    *   the shader
    */
  override def bind(shader: ShaderProgram): Unit =
    bind(shader, Nullable.empty)

  override def bind(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    gl30.glBindVertexArray(vaoHandle)

    bindAttributes(shader, locations)

    // if our data has changed upload it:
    bindData(Sge().graphics.gl20)

    isBound = true
  }

  private def bindAttributes(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    var stillValid    = cachedLocations.nonEmpty
    val numAttributes = attributes.size

    if (stillValid) {
      locations.fold {
        for (i <- 0 until numAttributes if stillValid) {
          val attribute = attributes.get(i)
          val location  = shader.getAttributeLocation(attribute.alias)
          stillValid = location == cachedLocations(i)
        }
      } { locs =>
        stillValid = locs.length == cachedLocations.size
        for (i <- 0 until numAttributes if stillValid)
          stillValid = locs(i) == cachedLocations(i)
      }
    }

    if (!stillValid) {
      Sge().graphics.gl.glBindBuffer(BufferTarget.ArrayBuffer, bufferHandle)
      unbindAttributes(shader)
      cachedLocations.clear()

      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locations.map(_(i)).getOrElse(shader.getAttributeLocation(attribute.alias))
        cachedLocations.add(location)

        if (location != AttributeLocation.notFound) {
          shader.enableVertexAttribute(location)
          shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, attribute.offset)
        }
      }
    }
  }

  private def unbindAttributes(shaderProgram: ShaderProgram): Unit =
    if (cachedLocations.nonEmpty) {
      val numAttributes = attributes.size
      for (i <- 0 until numAttributes) {
        val location = cachedLocations(i)
        if (location != AttributeLocation.notFound) {
          shaderProgram.disableVertexAttribute(location)
        }
      }
    }

  private def bindData(gl: GL20): Unit =
    if (isDirty) {
      gl.glBindBuffer(BufferTarget.ArrayBuffer, bufferHandle)
      byteBuffer.asInstanceOf[Buffer].limit(_buffer.limit() * 4)
      gl.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  /** Unbinds this VertexBufferObject.
    *
    * @param shader
    *   the shader
    */
  override def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty)

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[AttributeLocation]]): Unit = {
    gl30.glBindVertexArray(0)
    isBound = false
  }

  /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = gl30.glGenBuffer()
    createVAO()
    isDirty = true
  }

  /** Disposes of all resources this VertexBufferObject uses. */
  override def close(): Unit = {
    val gl = gl30
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
    if (ownsBuffer) {
      BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    }
    deleteVAO()
  }

  private def createVAO(): Unit = {
    val gl3       = gl30
    val tmpHandle = BufferUtils.newIntBuffer(1)
    tmpHandle.asInstanceOf[Buffer].clear()
    gl3.glGenVertexArrays(1, tmpHandle)
    vaoHandle = tmpHandle.get()
  }

  private def deleteVAO(): Unit =
    if (vaoHandle != -1) {
      val gl3       = gl30
      val tmpHandle = BufferUtils.newIntBuffer(1)
      tmpHandle.asInstanceOf[Buffer].clear()
      tmpHandle.put(vaoHandle)
      tmpHandle.asInstanceOf[Buffer].flip()
      gl3.glDeleteVertexArrays(1, tmpHandle)
      vaoHandle = -1
    }
}
