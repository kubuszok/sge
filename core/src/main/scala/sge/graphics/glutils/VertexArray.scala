/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/VertexArray.java
 * Original authors: mzechner, Dave Clayton <contact@redskyforge.com>
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer

import sge.utils.{ BufferUtils, Nullable }

/** <p> Convenience class for working with OpenGL vertex arrays. It interleaves all data in the order you specified in the constructor via {@link VertexAttribute} . </p>
  *
  * <p> This class is not compatible with OpenGL 3+ core profiles. For this {@link VertexBufferObject} s are needed. </p>
  *
  * @author
  *   mzechner, Dave Clayton <contact@redskyforge.com>
  */
class VertexArray(numVertices: Int, val attributes: VertexAttributes) extends VertexData {
  val byteBuffer: ByteBuffer  = BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices)
  val buffer:     FloatBuffer = byteBuffer.asFloatBuffer()
  var isBound = false

  // Initialize buffers
  buffer.asInstanceOf[Buffer].flip()
  byteBuffer.asInstanceOf[Buffer].flip()

  /** Constructs a new interleaved VertexArray
    *
    * @param numVertices
    *   the maximum number of vertices
    * @param attributes
    *   the {@link VertexAttribute} s
    */
  def this(numVertices: Int, attributes: VertexAttribute*) = {
    this(numVertices, new VertexAttributes(attributes*))
  }

  override def close(): Unit =
    BufferUtils.disposeUnsafeByteBuffer(byteBuffer)

  /** @deprecated use {@link #getBuffer(boolean)} instead */
  override def getBuffer(): FloatBuffer =
    buffer

  override def getBuffer(forWriting: Boolean): FloatBuffer =
    buffer

  override def getNumVertices(): Int =
    buffer.limit() * 4 / attributes.vertexSize

  def getNumMaxVertices(): Int =
    byteBuffer.capacity() / attributes.vertexSize

  override def setVertices(vertices: Array[Float], offset: Int, count: Int): Unit = {
    BufferUtils.copy(vertices, byteBuffer, count, offset)
    buffer.asInstanceOf[Buffer].position(0)
    buffer.asInstanceOf[Buffer].limit(count)
  }

  override def updateVertices(targetOffset: Int, vertices: Array[Float], sourceOffset: Int, count: Int): Unit = {
    val pos = byteBuffer.position()
    byteBuffer.asInstanceOf[Buffer].position(targetOffset * 4)
    BufferUtils.copy(vertices, sourceOffset, count, byteBuffer)
    byteBuffer.asInstanceOf[Buffer].position(pos)
  }

  override def bind(shader: ShaderProgram): Unit =
    bind(shader, Nullable.empty)

  override def bind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    val numAttributes = attributes.size
    byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 4)
    for (i <- 0 until numAttributes) {
      val attribute = attributes.get(i)
      val location  = locations.fold(shader.getAttributeLocation(attribute.alias))(_(i))
      if (location < 0) {
        // continue to next iteration
      } else {
        shader.enableVertexAttribute(location)

        if (attribute.`type` == GL20.GL_FLOAT) {
          buffer.asInstanceOf[Buffer].position(attribute.offset / 4)
          shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, buffer)
        } else {
          byteBuffer.asInstanceOf[Buffer].position(attribute.offset)
          shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, byteBuffer)
        }
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

  override def unbind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    val numAttributes = attributes.size
    locations.fold {
      for (i <- 0 until numAttributes)
        shader.disableVertexAttribute(attributes.get(i).alias)
    } { locs =>
      for (i <- 0 until numAttributes) {
        val location = locs(i)
        if (location >= 0) shader.disableVertexAttribute(location)
      }
    }
    isBound = false
  }

  override def getAttributes(): VertexAttributes =
    attributes

  override def invalidate(): Unit = {}
}
