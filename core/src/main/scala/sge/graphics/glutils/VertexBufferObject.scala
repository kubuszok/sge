package sge
package graphics
package glutils

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import sge.utils.{ BufferUtils, SgeError };

/** <p> A {@link VertexData} implementation based on OpenGL vertex buffer objects. <p> If the OpenGL ES context was lost you can call {@link #invalidate()} to recreate a new OpenGL vertex buffer
  * object. <p> The data is bound via glVertexAttribPointer() according to the attribute aliases specified via {@link VertexAttributes} in the constructor. <p> VertexBufferObjects must be disposed via
  * the {@link #dispose()} method when no longer needed
  *
  * @author
  *   mzechner, Dave Clayton <contact@redskyforge.com>
  */
class VertexBufferObject(using sde: Sge) extends VertexData {
  private var attributes:   VertexAttributes = scala.compiletime.uninitialized
  private var buffer:       FloatBuffer      = scala.compiletime.uninitialized
  private var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
  private var ownsBuffer:   Boolean          = scala.compiletime.uninitialized
  private var bufferHandle: Int              = scala.compiletime.uninitialized
  private var usage:        Int              = scala.compiletime.uninitialized
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
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttribute*)(using sde: Sge) = {
    this()
    this.init(isStatic, numVertices, new VertexAttributes(attributes*))
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
  def this(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes)(using sde: Sge) = {
    this()
    this.init(isStatic, numVertices, attributes)
  }

  protected def this(usage: Int, data: ByteBuffer, ownsBuffer: Boolean, attributes: VertexAttributes)(using sde: Sge) = {
    this()
    bufferHandle = sde.graphics.gl20.glGenBuffer()
    setBuffer(data, ownsBuffer, attributes)
    setUsage(usage)
  }

  private def init(isStatic: Boolean, numVertices: Int, attributes: VertexAttributes): Unit = {
    bufferHandle = sde.graphics.gl20.glGenBuffer()

    val data = BufferUtils.newUnsafeByteBuffer(attributes.vertexSize * numVertices)
    data.asInstanceOf[Buffer].limit(0)
    setBuffer(data, true, attributes)
    setUsage(if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW)
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

  /** Low level method to reset the buffer and attributes to the specified values. Use with care!
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

  private def bufferChanged(): Unit =
    if (isBound) {
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

  /** @return
    *   The GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)} , e.g. GL_STATIC_DRAW or GL_DYNAMIC_DRAW
    */
  protected def getUsage(): Int = usage

  /** Set the GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)} , can only be called when the VBO is not bound.
    */
  protected def setUsage(value: Int): Unit = {
    if (isBound) throw SgeError.GraphicsError("Cannot change usage while VBO is bound")
    usage = value
  }

  /** Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
    * @param shader
    *   the shader
    */
  override def bind(shader: ShaderProgram): Unit =
    bind(shader, null)

  override def bind(shader: ShaderProgram, locations: Array[Int]): Unit = {
    val gl = sde.graphics.gl20

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
          shader.enableVertexAttribute(location)
          shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, attribute.offset)
        }
      }
    } else {
      for (i <- 0 until numAttributes) {
        val attribute = attributes.get(i)
        val location  = locations(i)
        if (location >= 0) {
          shader.enableVertexAttribute(location)
          shader.setVertexAttribute(location, attribute.numComponents, attribute.`type`, attribute.normalized, attributes.vertexSize, attribute.offset)
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
    unbind(shader, null)

  override def unbind(shader: ShaderProgram, locations: Array[Int]): Unit = {
    val gl            = sde.graphics.gl20
    val numAttributes = attributes.size
    if (locations == null) {
      for (i <- 0 until numAttributes)
        shader.disableVertexAttribute(attributes.get(i).alias)
    } else {
      for (i <- 0 until numAttributes) {
        val location = locations(i)
        if (location >= 0) shader.disableVertexAttribute(location)
      }
    }
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    isBound = false
  }

  /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  override def invalidate(): Unit = {
    bufferHandle = sde.graphics.gl20.glGenBuffer()
    isDirty = true
  }

  /** Disposes of all resources this VertexBufferObject uses. */
  override def close(): Unit = {
    val gl = sde.graphics.gl20
    gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
    gl.glDeleteBuffer(bufferHandle)
    bufferHandle = 0
    if (ownsBuffer) BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
  }
}
