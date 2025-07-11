package sge
package graphics
package glutils

import java.nio.FloatBuffer

/** A VertexData instance holds vertices for rendering with OpenGL. It is implemented as either a {@link VertexArray} or a {@link VertexBufferObject} . Only the later supports OpenGL ES 2.0.
  *
  * @author
  *   mzechner
  */
trait VertexData extends AutoCloseable {

  /** @return the number of vertices this VertexData stores */
  def getNumVertices(): Int

  /** @return the number of vertices this VertedData can store */
  def getNumMaxVertices(): Int

  /** @return the {@link VertexAttributes} as specified during construction. */
  def getAttributes(): VertexAttributes

  /** Sets the vertices of this VertexData, discarding the old vertex data. The count must equal the number of floats per vertex times the number of vertices to be copied to this VertexData. The order
    * of the vertex attributes must be the same as specified at construction time via {@link VertexAttributes} . <p> This can be called in between calls to bind and unbind. The vertex data will be
    * updated instantly.
    * @param vertices
    *   the vertex data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def setVertices(vertices: Array[Float], offset: Int, count: Int): Unit

  /** Update (a portion of) the vertices. Does not resize the backing buffer.
    * @param vertices
    *   the vertex data
    * @param sourceOffset
    *   the offset to start copying the data from
    * @param count
    *   the number of floats to copy
    */
  def updateVertices(targetOffset: Int, vertices: Array[Float], sourceOffset: Int, count: Int): Unit

  /** Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to bind. If you need immediate uploading use
    * {@link #setVertices(float[], int, int)} ; Any modifications made to the Buffer *after* the call to bind will not automatically be uploaded.
    * @return
    *   the underlying FloatBuffer holding the vertex data.
    * @deprecated
    *   use {@link #getBuffer(boolean)} instead.
    */
  @deprecated("use getBuffer(boolean) instead", "")
  def getBuffer(): FloatBuffer

  /** Returns the underlying FloatBuffer for reading or writing.
    * @param forWriting
    *   when true, the underlying buffer will be uploaded on the next call to bind. If you need immediate uploading use {@link #setVertices(float[], int, int)} .
    * @return
    *   the underlying FloatBuffer holding the vertex data.
    */
  def getBuffer(forWriting: Boolean): FloatBuffer

  /** Binds this VertexData for rendering via glDrawArrays or glDrawElements. */
  def bind(shader: ShaderProgram): Unit

  /** Binds this VertexData for rendering via glDrawArrays or glDrawElements.
    * @param locations
    *   array containing the attribute locations.
    */
  def bind(shader: ShaderProgram, locations: Array[Int]): Unit

  /** Unbinds this VertexData. */
  def unbind(shader: ShaderProgram): Unit

  /** Unbinds this VertexData.
    * @param locations
    *   array containing the attribute locations.
    */
  def unbind(shader: ShaderProgram, locations: Array[Int]): Unit

  /** Invalidates the VertexData if applicable. Use this in case of a context loss. */
  def invalidate(): Unit

  /** Disposes this VertexData and all its associated OpenGL resources. */
  def close(): Unit
}
