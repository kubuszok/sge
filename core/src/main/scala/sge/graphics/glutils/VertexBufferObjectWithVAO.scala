/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/VertexBufferObjectWithVAO.java
 * Original authors: mzechner, Dave Clayton <contact@redskyforge.com>, Nate Austin <nate.austin gmail>
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import sge.utils.BufferUtils;
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
class VertexBufferObjectWithVAO(using sde: Sge) extends VertexData {

  var attributes:   VertexAttributes = scala.compiletime.uninitialized
  var buffer:       FloatBuffer      = scala.compiletime.uninitialized
  var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
  var ownsBuffer:   Boolean          = scala.compiletime.uninitialized
  var bufferHandle: Int              = scala.compiletime.uninitialized
  var isStatic:     Boolean          = scala.compiletime.uninitialized
  var usage:        Int              = scala.compiletime.uninitialized
  var isDirty         = false
  var isBound         = false
  var vaoHandle       = -1
  var cachedLocations = DynamicArray[Int]()

  /** Constructs a new interleaved VertexBufferObjectWithVAO.
    *
    * @param isStatic
    *   whether the vertex data is static.
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link com.badlogic.gdx.graphics.VertexAttribute} s.
    */
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(using sde: Sge) = {
    this()
    init(isStatic, numVertices, new VertexAttributes(attributes*))
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
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes)(using sde: Sge) = {
    this()
    init(isStatic, numVertices, attributes)
  }

  def this(isStatic: Boolean, unmanagedBuffer: ByteBuffer, attributes: VertexAttributes)(using sde: Sge) = {
    this()
    this.isStatic = isStatic
    this.attributes = attributes

    byteBuffer = unmanagedBuffer
    ownsBuffer = false
    buffer = byteBuffer.asFloatBuffer()
    buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].flip()
    bufferHandle = sde.graphics.gl20.glGenBuffer()
    usage = if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
    createVAO()
  }

  private def init(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes): Unit = {
    this.isStatic = isStatic
    this.attributes = attributes

    byteBuffer = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * numVertices)
    buffer = byteBuffer.asFloatBuffer()
    ownsBuffer = true
    buffer.asInstanceOf[Buffer].flip()
    byteBuffer.asInstanceOf[Buffer].flip()
    bufferHandle = sde.graphics.gl20.glGenBuffer()
    usage = if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
    createVAO()
  }

  override def getAttributes(): VertexAttributes = attributes

  override def getNumVertices(): Int = buffer.limit() * 4 / attributes.vertexSize

  override def getNumMaxVertices(): Int = byteBuffer.capacity() / attributes.vertexSize

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  @deprecated("use getBuffer(boolean) instead", "")
  override def getBuffer(): FloatBuffer = {
    isDirty = true
    buffer
  }

  override def getBuffer(forWriting: Boolean): FloatBuffer = {
    isDirty = isDirty || forWriting
    buffer
  }

  private def bufferChanged(): Unit =
    if (isBound) {
      sde.graphics.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
      sde.graphics.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  override def setVertices(vertices: Array[Float], offset: Int, count: Int): Unit = {
    isDirty = true
    BufferUtils.copy(vertices, byteBuffer, count, offset)
    buffer.asInstanceOf[Buffer].position(0)
    buffer.asInstanceOf[Buffer].limit(count)
    bufferChanged()
  }

  override def updateVertices(targetOffset: Int, vertices: Array[Float], sourceOffset: Int, count: Int): Unit = {
    isDirty = true
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
    byteBuffer.asInstanceOf[Buffer].position(pos)
    buffer.asInstanceOf[Buffer].position(0)
    bufferChanged()
  }

  /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
    *
    * @param shader
    *   the shader
    */
  override def bind(shader: ShaderProgram): Unit =
    bind(shader, Nullable.empty)

  override def bind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    sde.graphics.gl30.foreach(_.glBindVertexArray(vaoHandle))

    bindAttributes(shader, locations)

    // if our data has changed upload it:
    bindData(sde.graphics.gl20)

    isBound = true
  }

  private def bindAttributes(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
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
      sde.graphics.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
      unbindAttributes(shader)
      cachedLocations.clear()

      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locations.fold(shader.getAttributeLocation(attribute.alias))(_(i))
        cachedLocations.add(location)

        if (location >= 0) {
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
        if (location >= 0) {
          shaderProgram.disableVertexAttribute(location)
        }
      }
    }

  private def bindData(gl: GL20): Unit =
    if (isDirty) {
      gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle)
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 4)
      gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
      isDirty = false
    }

  /** Unbinds this VertexBufferObject.
    *
    * @param shader
    *   the shader
    */
  override def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty)

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    sde.graphics.gl30.foreach(_.glBindVertexArray(0))
    isBound = false
  }

  /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    sde.graphics.gl30.foreach { gl =>
      bufferHandle = gl.glGenBuffer()
    }
    createVAO()
    isDirty = true
  }

  /** Disposes of all resources this VertexBufferObject uses. */
  override def close(): Unit = {
    sde.graphics.gl30.foreach { gl =>
      gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
      gl.glDeleteBuffer(bufferHandle)
    }
    bufferHandle = 0
    if (ownsBuffer) {
      BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
    }
    deleteVAO()
  }

  private def createVAO(): Unit =
    sde.graphics.gl30.foreach { gl =>
      val tmpHandle = BufferUtils.newIntBuffer(1)
      tmpHandle.asInstanceOf[Buffer].clear()
      gl.glGenVertexArrays(1, tmpHandle)
      vaoHandle = tmpHandle.get()
    }

  private def deleteVAO(): Unit =
    if (vaoHandle != -1) {
      sde.graphics.gl30.foreach { gl =>
        val tmpHandle = BufferUtils.newIntBuffer(1)
        tmpHandle.asInstanceOf[Buffer].clear()
        tmpHandle.put(vaoHandle)
        tmpHandle.asInstanceOf[Buffer].flip()
        gl.glDeleteVertexArrays(1, tmpHandle)
      }
      vaoHandle = -1
    }
}
