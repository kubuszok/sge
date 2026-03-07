/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Mesh.java
 * Original authors: mzechner, Dave Clayton <contact@redskyforge.com>, Xoppa, Jaroslaw Wisniewski <j.wisniewski@appsisle.com>
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Disposable -> AutoCloseable; managed mesh lifecycle
 *   Idiom: split packages
 *   Convention: calculateRadius uses Nullable[Matrix4] instead of null; copy uses Nullable[Array[Int]]
 *   Idiom: typed GL enums -- PrimitiveMode for glDrawArrays/glDrawElements
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import scala.collection.mutable

import sge.graphics.VertexAttributes.Usage
import sge.graphics.glutils.IndexArray
import sge.graphics.glutils.IndexBufferObject
import sge.graphics.glutils.IndexBufferObjectSubData
import sge.graphics.glutils.IndexData
import sge.graphics.glutils.InstanceBufferObject
import sge.graphics.glutils.InstanceData
import sge.graphics.glutils.ShaderProgram
import sge.graphics.glutils.VertexArray
import sge.graphics.glutils.VertexBufferObject
import sge.graphics.glutils.VertexBufferObjectSubData
import sge.graphics.glutils.VertexBufferObjectWithVAO
import sge.graphics.glutils.VertexData
import sge.math.Matrix3
import sge.math.Matrix4
import sge.math.Vector2
import sge.math.Vector3
import sge.math.collision.BoundingBox
import sge.utils.DynamicArray
import sge.utils.SgeError
import sge.utils.Nullable
import scala.util.boundary

/** <p> A Mesh holds vertices composed of attributes specified by a {@link VertexAttributes} instance. The vertices are held either in VRAM in form of vertex buffer objects or in RAM in form of vertex
  * arrays. The former variant is more performant and is preferred over vertex arrays if hardware supports it. </p>
  *
  * <p> Meshes are automatically managed. If the OpenGL context is lost all vertex buffer objects get invalidated and must be reloaded when the context is recreated. This only happens on Android when
  * a user switches to another application or receives an incoming call. A managed Mesh will be reloaded automagically so you don't have to do this manually. </p>
  *
  * <p> A Mesh consists of vertices and optionally indices which specify which vertices define a triangle. Each vertex is composed of attributes such as position, normal, color or texture coordinate.
  * Note that not all of this attributes must be given, except for position which is non-optional. Each attribute has an alias which is used when rendering a Mesh in OpenGL ES 2.0. The alias is used
  * to bind a specific vertex attribute to a shader attribute. The shader source and the alias of the attribute must match exactly for this to work. </p>
  *
  * @author
  *   mzechner, Dave Clayton <contact@redskyforge.com>, Xoppa
  */
class Mesh protected (val vertices: VertexData, val indices: IndexData, val isVertexArray: Boolean)(using Sge) extends AutoCloseable {

  var autoBind:        Boolean                = true
  var instances:       Nullable[InstanceData] = Nullable.empty
  var isInstancedFlag: Boolean                = false
  private val tmpV = Vector3()

  // Add to managed meshes
  Mesh.addManagedMesh(Sge().application, this)

  /** Creates a new Mesh with the given attributes.
    *
    * @param isStatic
    *   whether this mesh is static or not. Allows for internal optimizations.
    * @param maxVertices
    *   the maximum number of vertices this mesh can hold
    * @param maxIndices
    *   the maximum number of indices this mesh can hold
    * @param attributes
    *   the {@link VertexAttributes} . Each vertex attribute defines one property of a vertex such as position, normal or texture coordinate
    */
  def this(isStatic: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes)(using Sge) = {
    this(
      vertices = Mesh.makeVertexBuffer(isStatic, maxVertices, attributes),
      indices = IndexBufferObject(isStatic, maxIndices),
      isVertexArray = false
    )
  }

  /** Creates a new Mesh with the given attributes.
    *
    * @param isStatic
    *   whether this mesh is static or not. Allows for internal optimizations.
    * @param maxVertices
    *   the maximum number of vertices this mesh can hold
    * @param maxIndices
    *   the maximum number of indices this mesh can hold
    * @param attributes
    *   the {@link VertexAttribute} s. Each vertex attribute defines one property of a vertex such as position, normal or texture coordinate
    */
  def this(isStatic: Boolean, maxVertices: Int, maxIndices: Int)(attributes: VertexAttribute*)(using Sge) = {
    this(isStatic = isStatic, maxVertices = maxVertices, maxIndices = maxIndices, attributes = VertexAttributes(attributes*))
  }

  /** Creates a new Mesh with the given attributes. Adds extra optimizations for dynamic (frequently modified) meshes.
    *
    * @param staticVertices
    *   whether vertices of this mesh are static or not. Allows for internal optimizations.
    * @param staticIndices
    *   whether indices of this mesh are static or not. Allows for internal optimizations.
    * @param maxVertices
    *   the maximum number of vertices this mesh can hold
    * @param maxIndices
    *   the maximum number of indices this mesh can hold
    * @param attributes
    *   the {@link VertexAttributes} . Each vertex attribute defines one property of a vertex such as position, normal or texture coordinate
    *
    * @author
    *   Jaroslaw Wisniewski <j.wisniewski@appsisle.com> *
    */
  def this(staticVertices: Boolean, staticIndices: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes)(using Sge) = {
    this(
      vertices = Mesh.makeVertexBuffer(staticVertices, maxVertices, attributes),
      indices = IndexBufferObject(staticIndices, maxIndices),
      isVertexArray = false
    )
  }

  /** Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
    *
    * @param meshType
    *   the {@link VertexDataType} to be used, VBO or VA.
    * @param isStatic
    *   whether this mesh is static or not. Allows for internal optimizations.
    * @param maxVertices
    *   the maximum number of vertices this mesh can hold
    * @param maxIndices
    *   the maximum number of indices this mesh can hold
    * @param attributes
    *   the {@link VertexAttributes} .
    */
  def this(meshType: Mesh.VertexDataType, isStatic: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttributes)(using Sge) = {
    this(
      Mesh.createVertexData(meshType, isStatic, maxVertices, attributes),
      Mesh.createIndexData(meshType, isStatic, maxIndices),
      meshType == Mesh.VertexDataType.VertexArray
    )
  }

  /** Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
    *
    * @param type
    *   the {@link VertexDataType} to be used, VBO or VA.
    * @param isStatic
    *   whether this mesh is static or not. Allows for internal optimizations.
    * @param maxVertices
    *   the maximum number of vertices this mesh can hold
    * @param maxIndices
    *   the maximum number of indices this mesh can hold
    * @param attributes
    *   the {@link VertexAttribute} s. Each vertex attribute defines one property of a vertex such as position, normal or texture coordinate
    */
  def this(meshType: Mesh.VertexDataType, isStatic: Boolean, maxVertices: Int, maxIndices: Int, attributes: VertexAttribute*)(using Sge) = {
    this(
      meshType = meshType,
      isStatic = isStatic,
      maxVertices = maxVertices,
      maxIndices = maxIndices,
      attributes = VertexAttributes(attributes*)
    )
  }

  def enableInstancedRendering(isStatic: Boolean, maxInstances: Int, attributes: VertexAttribute*): Mesh = {
    if (!isInstancedFlag) {
      isInstancedFlag = true
      instances = Nullable(InstanceBufferObject(isStatic, maxInstances, attributes*))
    } else {
      throw new SgeError.GraphicsError(
        "Trying to enable InstancedRendering on same Mesh instance twice." +
          " Use disableInstancedRendering to clean up old InstanceData first",
        None
      )
    }
    this
  }

  def disableInstancedRendering(): Mesh = {
    if (isInstancedFlag) {
      isInstancedFlag = false
      instances.foreach(_.close())
      instances = Nullable.empty
    }
    this
  }

  /** Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param instanceData
    *   the instance data.
    * @param offset
    *   the offset into the vertices array
    * @param count
    *   the number of floats to use
    * @return
    *   the mesh for invocation chaining.
    */
  def setInstanceData(instanceData: Array[Float], offset: Int, count: Int): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before setting instance data!", None)) {
      _.setInstanceData(instanceData, offset, count)
    }
    this
  }

  /** Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param instanceData
    *   the instance data.
    * @return
    *   the mesh for invocation chaining.
    */
  def setInstanceData(instanceData: Array[Float]): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before setting instance data!", None)) {
      _.setInstanceData(instanceData, 0, instanceData.length)
    }
    this
  }

  /** Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param instanceData
    *   the instance data.
    * @param count
    *   the number of floats to use
    * @return
    *   the mesh for invocation chaining.
    */
  def setInstanceData(instanceData: FloatBuffer, count: Int): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before setting instance data!", None)) {
      _.setInstanceData(instanceData, count)
    }
    this
  }

  /** Sets the instance data of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param instanceData
    *   the instance data.
    * @return
    *   the mesh for invocation chaining.
    */
  def setInstanceData(instanceData: FloatBuffer): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before setting instance data!", None)) {
      _.setInstanceData(instanceData, instanceData.limit())
    }
    this
  }

  /** Update (a portion of) the instance data. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the instance data to update the mesh part with
    */
  def updateInstanceData(targetOffset: Int, source: Array[Float]): Mesh =
    updateInstanceData(targetOffset, source, 0, source.length)

  /** Update (a portion of) the instance data. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the instance data to update the mesh part with
    * @param sourceOffset
    *   the offset in number of floats within the source array
    * @param count
    *   the number of floats to update
    */
  def updateInstanceData(targetOffset: Int, source: Array[Float], sourceOffset: Int, count: Int): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before updating instance data!", None)) {
      _.updateInstanceData(targetOffset, source, sourceOffset, count)
    }
    this
  }

  /** Update (a portion of) the instance data. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the instance data to update the mesh part with
    */
  def updateInstanceData(targetOffset: Int, source: FloatBuffer): Mesh =
    updateInstanceData(targetOffset, source, 0, source.limit())

  /** Update (a portion of) the instance data. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the instance data to update the mesh part with
    * @param sourceOffset
    *   the offset in number of floats within the source array
    * @param count
    *   the number of floats to update
    */
  def updateInstanceData(targetOffset: Int, source: FloatBuffer, sourceOffset: Int, count: Int): Mesh = {
    instances.fold(throw new SgeError.GraphicsError("An InstanceBufferObject must be set before updating instance data!", None)) {
      _.updateInstanceData(targetOffset, source, sourceOffset, count)
    }
    this
  }

  /** Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param vertices
    *   the vertices.
    * @return
    *   the mesh for invocation chaining.
    */
  def setVertices(vertices: Array[Float]): Mesh = {
    this.vertices.setVertices(vertices, 0, vertices.length)
    this
  }

  /** @return Indicates whether this mesh uses instancing. */
  def isInstanced(): Boolean =
    this.isInstancedFlag

  /** Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
    *
    * @param vertices
    *   the vertices.
    * @param offset
    *   the offset into the vertices array
    * @param count
    *   the number of floats to use
    * @return
    *   the mesh for invocation chaining.
    */
  def setVertices(vertices: Array[Float], offset: Int, count: Int): Mesh = {
    this.vertices.setVertices(vertices, offset, count)
    this
  }

  /** Update (a portion of) the vertices. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the vertex data to update the mesh part with
    */
  def updateVertices(targetOffset: Int, source: Array[Float]): Mesh =
    updateVertices(targetOffset, source, 0, source.length)

  /** Update (a portion of) the vertices. Does not resize the backing buffer.
    * @param targetOffset
    *   the offset in number of floats of the mesh part.
    * @param source
    *   the vertex data to update the mesh part with
    * @param sourceOffset
    *   the offset in number of floats within the source array
    * @param count
    *   the number of floats to update
    */
  def updateVertices(targetOffset: Int, source: Array[Float], sourceOffset: Int, count: Int): Mesh = {
    this.vertices.updateVertices(targetOffset, source, sourceOffset, count)
    this
  }

  /** Copies the vertices from the Mesh to the float array. The float array must be large enough to hold all the Mesh's vertices.
    * @param vertices
    *   the array to copy the vertices to
    */
  def getVertices(vertices: Array[Float]): Array[Float] =
    getVertices(0, -1, vertices)

  /** Copies the the remaining vertices from the Mesh to the float array. The float array must be large enough to hold the remaining vertices.
    * @param srcOffset
    *   the offset (in number of floats) of the vertices in the mesh to copy
    * @param vertices
    *   the array to copy the vertices to
    */
  def getVertices(srcOffset: Int, vertices: Array[Float]): Array[Float] =
    getVertices(srcOffset, -1, vertices)

  /** Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold count vertices.
    * @param srcOffset
    *   the offset (in number of floats) of the vertices in the mesh to copy
    * @param count
    *   the amount of floats to copy
    * @param vertices
    *   the array to copy the vertices to
    */
  def getVertices(srcOffset: Int, count: Int, vertices: Array[Float]): Array[Float] =
    getVertices(srcOffset, count, vertices, 0)

  /** Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold destOffset+count vertices.
    * @param srcOffset
    *   the offset (in number of floats) of the vertices in the mesh to copy
    * @param count
    *   the amount of floats to copy
    * @param vertices
    *   the array to copy the vertices to
    * @param destOffset
    *   the offset (in floats) in the vertices array to start copying
    */
  def getVertices(srcOffset: Int, count: Int, vertices: Array[Float], destOffset: Int): Array[Float] = {
    // TODO: Perhaps this method should be vertexSize aware??
    val max         = getNumVertices() * getVertexSize() / 4
    var actualCount = count
    if (count == -1) {
      actualCount = max - srcOffset
      if (actualCount > vertices.length - destOffset) actualCount = vertices.length - destOffset
    }
    if (srcOffset < 0 || actualCount <= 0 || (srcOffset + actualCount) > max || destOffset < 0 || destOffset >= vertices.length)
      throw new IndexOutOfBoundsException()
    if ((vertices.length - destOffset) < actualCount) throw new IllegalArgumentException("not enough room in vertices array, has " + vertices.length + " floats, needs " + actualCount)
    val verticesBuffer = getVerticesBuffer(false)
    val pos            = verticesBuffer.position()
    verticesBuffer.asInstanceOf[Buffer].position(srcOffset)
    verticesBuffer.get(vertices, destOffset, actualCount)
    verticesBuffer.asInstanceOf[Buffer].position(pos)
    vertices
  }

  /** Sets the indices of this Mesh
    *
    * @param indices
    *   the indices
    * @return
    *   the mesh for invocation chaining.
    */
  def setIndices(indices: Array[Short]): Mesh = {
    this.indices.setIndices(indices, 0, indices.length)
    this
  }

  /** Sets the indices of this Mesh.
    *
    * @param indices
    *   the indices
    * @param offset
    *   the offset into the indices array
    * @param count
    *   the number of indices to copy
    * @return
    *   the mesh for invocation chaining.
    */
  def setIndices(indices: Array[Short], offset: Int, count: Int): Mesh = {
    this.indices.setIndices(indices, offset, count)
    this
  }

  /** Copies the indices from the Mesh to the short array. The short array must be large enough to hold all the Mesh's indices.
    * @param indices
    *   the array to copy the indices to
    */
  def getIndices(indices: Array[Short]): Unit =
    getIndices(indices, 0)

  /** Copies the indices from the Mesh to the short array. The short array must be large enough to hold destOffset + all the Mesh's indices.
    * @param indices
    *   the array to copy the indices to
    * @param destOffset
    *   the offset in the indices array to start copying
    */
  def getIndices(indices: Array[Short], destOffset: Int): Unit =
    getIndices(0, indices, destOffset)

  /** Copies the remaining indices from the Mesh to the short array. The short array must be large enough to hold destOffset + all the remaining indices.
    * @param srcOffset
    *   the zero-based offset of the first index to fetch
    * @param indices
    *   the array to copy the indices to
    * @param destOffset
    *   the offset in the indices array to start copying
    */
  def getIndices(srcOffset: Int, indices: Array[Short], destOffset: Int): Unit =
    getIndices(srcOffset, -1, indices, destOffset)

  /** Copies the indices from the Mesh to the short array. The short array must be large enough to hold destOffset + count indices.
    * @param srcOffset
    *   the zero-based offset of the first index to fetch
    * @param count
    *   the total amount of indices to copy
    * @param indices
    *   the array to copy the indices to
    * @param destOffset
    *   the offset in the indices array to start copying
    */
  def getIndices(srcOffset: Int, count: Int, indices: Array[Short], destOffset: Int): Unit = {
    val max         = getNumIndices()
    var actualCount = count
    if (count < 0) actualCount = max - srcOffset
    if (srcOffset < 0 || srcOffset >= max || srcOffset + actualCount > max)
      throw new IllegalArgumentException("Invalid range specified, offset: " + srcOffset + ", count: " + actualCount + ", max: " + max)
    if ((indices.length - destOffset) < actualCount) throw new IllegalArgumentException("not enough room in indices array, has " + indices.length + " shorts, needs " + actualCount)
    val indicesBuffer = getIndicesBuffer(false)
    val pos           = indicesBuffer.position()
    indicesBuffer.asInstanceOf[Buffer].position(srcOffset)
    indicesBuffer.get(indices, destOffset, actualCount)
    indicesBuffer.asInstanceOf[Buffer].position(pos)
  }

  /** @return the number of defined indices */
  def getNumIndices(): Int =
    indices.getNumIndices()

  /** @return the number of defined vertices */
  def getNumVertices(): Int =
    vertices.getNumVertices()

  /** @return the maximum number of vertices this mesh can hold */
  def getMaxVertices(): Int =
    vertices.getNumMaxVertices()

  /** @return the maximum number of indices this mesh can hold */
  def getMaxIndices(): Int =
    indices.getNumMaxIndices()

  /** @return the size of a single vertex in bytes */
  def getVertexSize(): Int =
    vertices.getAttributes().vertexSize

  def getIndexData(): IndexData =
    indices

  /** Sets whether to bind the underlying {@link VertexArray} or {@link VertexBufferObject} automatically on a call to one of the render methods. Usually you want to use autobind. Manual binding is an
    * expert functionality. There is a driver bug on the MSM720xa chips that will fuck up memory if you manipulate the vertices and indices of a Mesh multiple times while it is bound. Keep this in
    * mind.
    *
    * @param autoBind
    *   whether to autobind meshes.
    */
  def setAutoBind(autoBind: Boolean): Unit =
    this.autoBind = autoBind

  /** Binds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} if indices where given. Use this with OpenGL ES 2.0 and when auto-bind is disabled.
    *
    * @param shader
    *   the shader (does not bind the shader)
    */
  def bind(shader: ShaderProgram): Unit =
    bind(shader, Nullable.empty, Nullable.empty)

  /** Binds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} if indices where given. Use this with OpenGL ES 2.0 and when auto-bind is disabled.
    *
    * @param shader
    *   the shader (does not bind the shader)
    * @param locations
    *   array containing the vertex attribute locations.
    * @param instanceLocations
    *   array containing the instance attribute locations.
    */
  def bind(shader: ShaderProgram, locations: Nullable[Array[Int]], instanceLocations: Nullable[Array[Int]]): Unit = {
    vertices.bind(shader, locations)
    instances.foreach { inst =>
      if (inst.getNumInstances() > 0) inst.bind(shader, instanceLocations)
    }
    if (indices.getNumIndices() > 0) indices.bind()
  }

  /** Unbinds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} is indices were given. Use this with OpenGL ES 1.x and when auto-bind is disabled.
    *
    * @param shader
    *   the shader (does not unbind the shader)
    */
  def unbind(shader: ShaderProgram): Unit =
    unbind(shader, Nullable.empty, Nullable.empty)

  /** Unbinds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} is indices were given. Use this with OpenGL ES 1.x and when auto-bind is disabled.
    *
    * @param shader
    *   the shader (does not unbind the shader)
    * @param locations
    *   array containing the vertex attribute locations.
    * @param instanceLocations
    *   array containing the instance attribute locations.
    */
  def unbind(shader: ShaderProgram, locations: Nullable[Array[Int]], instanceLocations: Nullable[Array[Int]]): Unit = {
    vertices.unbind(shader, locations)
    instances.foreach { inst =>
      if (inst.getNumInstances() > 0) inst.unbind(shader, instanceLocations)
    }
    if (indices.getNumIndices() > 0) indices.unbind()
  }

  /** <p> Renders the mesh using the given primitive type. If indices are set for this mesh then getNumIndices() / #vertices per primitive primitives are rendered. If no indices are set then
    * getNumVertices() / #vertices per primitive are rendered. </p>
    *
    * <p> This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to the respective shader attributes. The binding is based on the
    * alias defined for each VertexAttribute. </p>
    *
    * <p> This method must only be called after the {@link ShaderProgram#bind()} method has been called! </p>
    *
    * <p> This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used. </p>
    *
    * @param primitiveType
    *   the primitive type
    */
  def render(shader: ShaderProgram, primitiveType: PrimitiveMode): Unit =
    render(shader, primitiveType, 0, if (indices.getNumMaxIndices() > 0) getNumIndices() else getNumVertices(), autoBind)

  /** <p> Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index buffer depending on whether indices are defined. count specifies the
    * number of vertices or indices to use thus count / #vertices per primitive primitives are rendered. </p>
    *
    * <p> This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to the respective shader attributes. The binding is based on the
    * alias defined for each VertexAttribute. </p>
    *
    * <p> This method must only be called after the {@link ShaderProgram#bind()} method has been called! </p>
    *
    * <p> This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used. </p>
    *
    * @param shader
    *   the shader to be used
    * @param primitiveType
    *   the primitive type
    * @param offset
    *   the offset into the vertex or index buffer
    * @param count
    *   number of vertices or indices to use
    */
  def render(shader: ShaderProgram, primitiveType: PrimitiveMode, offset: Int, count: Int): Unit =
    render(shader, primitiveType, offset, count, autoBind)

  /** <p> Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index buffer depending on whether indices are defined. count specifies the
    * number of vertices or indices to use thus count / #vertices per primitive primitives are rendered. </p>
    *
    * <p> This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to the respective shader attributes. The binding is based on the
    * alias defined for each VertexAttribute. </p>
    *
    * <p> This method must only be called after the {@link ShaderProgram#bind()} method has been called! </p>
    *
    * <p> This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used. </p>
    *
    * @param shader
    *   the shader to be used
    * @param primitiveType
    *   the primitive type
    * @param offset
    *   the offset into the vertex or index buffer
    * @param count
    *   number of vertices or indices to use
    * @param autoBind
    *   overrides the autoBind member of this Mesh
    */
  def render(shader: ShaderProgram, primitiveType: PrimitiveMode, offset: Int, count: Int, autoBind: Boolean): Unit =
    if (count != 0) {
      if (autoBind) bind(shader)

      if (isVertexArray) {
        if (indices.getNumIndices() > 0) {
          val buffer      = indices.getBuffer(false)
          val oldPosition = buffer.position()
          buffer.asInstanceOf[Buffer].position(offset)
          summon[Sge].graphics.gl20.glDrawElements(primitiveType, count, DataType.UnsignedShort, buffer)
          buffer.asInstanceOf[Buffer].position(oldPosition)
        } else {
          summon[Sge].graphics.gl20.glDrawArrays(primitiveType, offset, count)
        }
      } else {
        val numInstances = if (isInstancedFlag) instances.map(_.getNumInstances()).getOrElse(0) else 0

        if (indices.getNumIndices() > 0) {
          if (count + offset > indices.getNumMaxIndices()) {
            throw new SgeError.GraphicsError(
              "Mesh attempting to access memory outside of the index buffer (count: " + count +
                ", offset: " + offset + ", max: " + indices.getNumMaxIndices() + ")",
              None
            )
          }

          if (isInstancedFlag && numInstances > 0) {
            summon[Sge].graphics.gl30.foreach(_.glDrawElementsInstanced(primitiveType, count, DataType.UnsignedShort, offset * 2, numInstances))
          } else {
            summon[Sge].graphics.gl20.glDrawElements(primitiveType, count, DataType.UnsignedShort, offset * 2)
          }
        } else {
          if (isInstancedFlag && numInstances > 0) {
            summon[Sge].graphics.gl30.foreach(_.glDrawArraysInstanced(primitiveType, offset, count, numInstances))
          } else {
            summon[Sge].graphics.gl20.glDrawArrays(primitiveType, offset, count)
          }
        }
      }

      if (autoBind) unbind(shader)
    }

  /** Frees all resources associated with this Mesh */
  override def close(): Unit = {
    if (Mesh.meshes.contains(summon[Sge].application)) {
      Mesh.meshes(summon[Sge].application).removeValue(this)
    }
    vertices.close()
    instances.foreach(_.close())
    indices.close()
  }

  /** Returns the first {@link VertexAttribute} having the given {@link Usage} .
    *
    * @param usage
    *   the Usage.
    * @return
    *   the VertexAttribute or null if no attribute with that usage was found.
    */
  def getVertexAttribute(usage: Int): Nullable[VertexAttribute] = boundary {
    val attributes = vertices.getAttributes()
    val len        = attributes.size
    for (i <- 0 until len)
      if (attributes.get(i).usage == usage) boundary.break(Nullable(attributes.get(i)))
    Nullable.empty
  }

  /** @return the vertex attributes of this Mesh */
  def getVertexAttributes(): VertexAttributes =
    vertices.getAttributes()

  /** @return the instanced attributes of this Mesh if any */
  def getInstancedAttributes(): Nullable[VertexAttributes] =
    instances.map(_.getAttributes())

  /** @return
    *   the backing FloatBuffer holding the vertices. Does not have to be a direct buffer on Android!
    * @deprecated
    *   use {@link #getVerticesBuffer(boolean)} instead
    */
  @deprecated("use getVerticesBuffer(Boolean) instead", "")
  def getVerticesBuffer(): FloatBuffer =
    vertices.getBuffer(true)

  def getVerticesBuffer(forWriting: Boolean): FloatBuffer =
    vertices.getBuffer(forWriting)

  /** Calculates the {@link BoundingBox} of the vertices contained in this mesh. In case no vertices are defined yet a {@link GdxRuntimeException} is thrown. This method creates a new BoundingBox
    * instance.
    *
    * @return
    *   the bounding box.
    */
  def calculateBoundingBox(): BoundingBox = {
    val bbox = BoundingBox()
    calculateBoundingBox(bbox)
    bbox
  }

  /** Calculates the {@link BoundingBox} of the vertices contained in this mesh. In case no vertices are defined yet a {@link GdxRuntimeException} is thrown.
    *
    * @param bbox
    *   the bounding box to store the result in.
    */
  def calculateBoundingBox(bbox: BoundingBox): Unit = {
    val numVertices = getNumVertices()
    if (numVertices == 0) throw new SgeError.GraphicsError("No vertices defined")

    val verts = vertices.getBuffer(false)
    bbox.inf()
    val posAttrib  = getVertexAttribute(Usage.Position).getOrElse(throw new SgeError.GraphicsError("No position attribute"))
    val offset     = posAttrib.offset / 4
    val vertexSize = vertices.getAttributes().vertexSize / 4
    var idx        = offset

    posAttrib.numComponents match {
      case 1 =>
        for (_ <- 0 until numVertices) {
          bbox.ext(verts.get(idx), 0, 0)
          idx += vertexSize
        }
      case 2 =>
        for (_ <- 0 until numVertices) {
          bbox.ext(verts.get(idx), verts.get(idx + 1), 0)
          idx += vertexSize
        }
      case 3 =>
        for (_ <- 0 until numVertices) {
          bbox.ext(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
          idx += vertexSize
        }
    }
  }

  /** @return
    *   the backing shortbuffer holding the indices. Does not have to be a direct buffer on Android!
    * @deprecated
    *   use {@link #getIndicesBuffer(boolean)} instead
    */
  @deprecated("use getIndicesBuffer(Boolean) instead", "")
  def getIndicesBuffer(): ShortBuffer =
    indices.getBuffer(true)

  def getIndicesBuffer(forWriting: Boolean): ShortBuffer =
    indices.getBuffer(forWriting)

  /** Calculate the {@link BoundingBox} of the specified part.
    * @param out
    *   the bounding box to store the result in.
    * @param offset
    *   the start index of the part.
    * @param count
    *   the amount of indices the part contains.
    * @return
    *   the value specified by out.
    */
  def calculateBoundingBox(out: BoundingBox, offset: Int, count: Int): BoundingBox =
    extendBoundingBox(out.inf(), offset, count)

  /** Calculate the {@link BoundingBox} of the specified part.
    * @param out
    *   the bounding box to store the result in.
    * @param offset
    *   the start index of the part.
    * @param count
    *   the amount of indices the part contains.
    * @param transform
    *   the transform to apply to each vertex.
    * @return
    *   the value specified by out.
    */
  def calculateBoundingBox(out: BoundingBox, offset: Int, count: Int, transform: Matrix4): BoundingBox =
    extendBoundingBox(out.inf(), offset, count, Nullable(transform))

  /** Extends the specified {@link BoundingBox} with the specified part.
    * @param out
    *   the bounding box to store the result in.
    * @param offset
    *   the start index of the part.
    * @param count
    *   the amount of indices the part contains.
    * @return
    *   the value specified by out.
    */
  def extendBoundingBox(out: BoundingBox, offset: Int, count: Int): BoundingBox =
    extendBoundingBox(out, offset, count, Nullable.empty)

  /** Extends the specified {@link BoundingBox} with the specified part.
    * @param out
    *   the bounding box to store the result in.
    * @param offset
    *   the start of the part.
    * @param count
    *   the size of the part.
    * @param transform
    *   optional transform to apply to each vertex.
    * @return
    *   the value specified by out.
    */
  def extendBoundingBox(out: BoundingBox, offset: Int, count: Int, transform: Nullable[Matrix4]): BoundingBox = {
    val numIndicesVal  = getNumIndices()
    val numVerticesVal = getNumVertices()
    val max            = if (numIndicesVal == 0) numVerticesVal else numIndicesVal
    if (offset < 0 || count < 1 || offset + count > max)
      throw new SgeError.GraphicsError(
        "Invalid part specified ( offset=" + offset + ", count=" + count + ", max=" + max + " )"
      )

    val verts      = vertices.getBuffer(false)
    val index      = indices.getBuffer(false)
    val posAttrib  = getVertexAttribute(Usage.Position).getOrElse(throw new SgeError.GraphicsError("No position attribute"))
    val posoff     = posAttrib.offset / 4
    val vertexSize = vertices.getAttributes().vertexSize / 4
    val end        = offset + count

    posAttrib.numComponents match {
      case 1 =>
        if (numIndicesVal > 0) {
          for (i <- offset until end) {
            val idx = (index.get(i) & 0xffff) * vertexSize + posoff
            tmpV.set(verts.get(idx), 0, 0)
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        } else {
          for (i <- offset until end) {
            val idx = i * vertexSize + posoff
            tmpV.set(verts.get(idx), 0, 0)
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        }
      case 2 =>
        if (numIndicesVal > 0) {
          for (i <- offset until end) {
            val idx = (index.get(i) & 0xffff) * vertexSize + posoff
            tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        } else {
          for (i <- offset until end) {
            val idx = i * vertexSize + posoff
            tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        }
      case 3 =>
        if (numIndicesVal > 0) {
          for (i <- offset until end) {
            val idx = (index.get(i) & 0xffff) * vertexSize + posoff
            tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        } else {
          for (i <- offset until end) {
            val idx = i * vertexSize + posoff
            tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
            transform.foreach(t => tmpV.mul(t))
            out.ext(tmpV)
          }
        }
      case _ => // do nothing for unsupported component counts
    }
    out
  }

  /** Calculates the squared radius of the bounding sphere around the specified center for the specified part.
    * @param centerX
    *   The X coordinate of the center of the bounding sphere
    * @param centerY
    *   The Y coordinate of the center of the bounding sphere
    * @param centerZ
    *   The Z coordinate of the center of the bounding sphere
    * @param offset
    *   the start index of the part.
    * @param count
    *   the amount of indices the part contains.
    * @param transform
    *   optional transformation matrix
    * @return
    *   the squared radius of the bounding sphere.
    */
  def calculateRadiusSquared(centerX: Float, centerY: Float, centerZ: Float, offset: Int, count: Int, transform: Nullable[Matrix4]): Float = {
    val numIndicesVal = getNumIndices()
    if (offset < 0 || count < 1 || offset + count > numIndicesVal) {
      throw SgeError.GraphicsError("Not enough indices")
    }

    val verts      = getVerticesBuffer(false)
    val index      = getIndicesBuffer(false)
    val posAttrib  = getVertexAttribute(Usage.Position).getOrElse(throw SgeError.GraphicsError("No position attribute"))
    val posoff     = posAttrib.offset / 4
    val vertexSize = vertices.getAttributes().vertexSize / 4
    val end        = offset + count

    var result = 0f

    posAttrib.numComponents match {
      case 1 =>
        for (i <- offset until end) {
          val idx = (index.get(i) & 0xffff) * vertexSize + posoff
          tmpV.set(verts.get(idx), 0, 0)
          transform.foreach(t => tmpV.mul(t))
          val r = tmpV.sub(centerX, centerY, centerZ).lengthSq
          if (r > result) result = r
        }
      case 2 =>
        for (i <- offset until end) {
          val idx = (index.get(i) & 0xffff) * vertexSize + posoff
          tmpV.set(verts.get(idx), verts.get(idx + 1), 0)
          transform.foreach(t => tmpV.mul(t))
          val r = tmpV.sub(centerX, centerY, centerZ).lengthSq
          if (r > result) result = r
        }
      case 3 =>
        for (i <- offset until end) {
          val idx = (index.get(i) & 0xffff) * vertexSize + posoff
          tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2))
          transform.foreach(t => tmpV.mul(t))
          val r = tmpV.sub(centerX, centerY, centerZ).lengthSq
          if (r > result) result = r
        }
      case _ => // do nothing for unsupported component counts
    }
    result
  }

  /** Calculates the radius of the bounding sphere around the specified center for the specified part. */
  def calculateRadius(centerX: Float, centerY: Float, centerZ: Float, offset: Int, count: Int, transform: Nullable[Matrix4]): Float =
    Math.sqrt(calculateRadiusSquared(centerX, centerY, centerZ, offset, count, transform).toDouble).toFloat

  /** Calculates the radius of the bounding sphere around the specified center for the specified part. */
  def calculateRadius(center: Vector3, offset: Int, count: Int, transform: Nullable[Matrix4]): Float =
    calculateRadius(center.x, center.y, center.z, offset, count, transform)

  /** Calculates the radius of the bounding sphere around the specified center for the specified part. */
  def calculateRadius(centerX: Float, centerY: Float, centerZ: Float, offset: Int, count: Int): Float =
    calculateRadius(centerX, centerY, centerZ, offset, count, Nullable.empty)

  /** Calculates the radius of the bounding sphere around the specified center for the specified part. */
  def calculateRadius(center: Vector3, offset: Int, count: Int): Float =
    calculateRadius(center.x, center.y, center.z, offset, count, Nullable.empty)

  /** Calculates the radius of the bounding sphere around the specified center. */
  def calculateRadius(centerX: Float, centerY: Float, centerZ: Float): Float =
    calculateRadius(centerX, centerY, centerZ, 0, getNumIndices(), Nullable.empty)

  /** Calculates the radius of the bounding sphere around the specified center. */
  def calculateRadius(center: Vector3): Float =
    calculateRadius(center.x, center.y, center.z, 0, getNumIndices(), Nullable.empty)

  /** Method to scale the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with care. It will also create a temporary float[] which will be garbage
    * collected.
    *
    * @param scaleX
    *   scale on x
    * @param scaleY
    *   scale on y
    * @param scaleZ
    *   scale on z
    */
  def scale(scaleX: Float, scaleY: Float, scaleZ: Float): Unit = {
    val posAttr        = getVertexAttribute(Usage.Position).getOrElse(throw SgeError.GraphicsError("No position attribute"))
    val offset         = posAttr.offset / 4
    val numComponents  = posAttr.numComponents
    val numVerticesVal = getNumVertices()
    val vertexSize     = getVertexSize() / 4

    val verts = new Array[Float](numVerticesVal * vertexSize)
    getVertices(verts)

    var idx = offset
    numComponents match {
      case 1 =>
        for (_ <- 0 until numVerticesVal) {
          verts(idx) *= scaleX
          idx += vertexSize
        }
      case 2 =>
        for (_ <- 0 until numVerticesVal) {
          verts(idx) *= scaleX
          verts(idx + 1) *= scaleY
          idx += vertexSize
        }
      case 3 =>
        for (_ <- 0 until numVerticesVal) {
          verts(idx) *= scaleX
          verts(idx + 1) *= scaleY
          verts(idx + 2) *= scaleZ
          idx += vertexSize
        }
      case _ => // do nothing
    }

    setVertices(verts)
  }

  /** Method to transform the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with care. It will also create a temporary float[] which will be garbage
    * collected.
    *
    * @param matrix
    *   the transformation matrix
    */
  def transform(matrix: Matrix4): Unit =
    transform(matrix, 0, getNumVertices())

  /** Transforms positions in the mesh for a portion of vertices. */
  def transform(matrix: Matrix4, start: Int, count: Int): Unit = {
    val posAttr       = getVertexAttribute(Usage.Position).getOrElse(throw SgeError.GraphicsError("No position attribute"))
    val posOffset     = posAttr.offset / 4
    val stride        = getVertexSize() / 4
    val numComponents = posAttr.numComponents

    val verts = new Array[Float](count * stride)
    getVertices(start * stride, count * stride, verts)
    Mesh.transform(matrix, verts, stride, posOffset, numComponents, 0, count)
    updateVertices(start * stride, verts)
  }

  /** Method to transform the texture coordinates in the mesh. This is a potentially slow operation, use with care. It will also create a temporary float[] which will be garbage collected.
    *
    * @param matrix
    *   the transformation matrix
    */
  def transformUV(matrix: Matrix3): Unit =
    transformUV(matrix, 0, getNumVertices())

  /** Transforms UV coordinates for a portion of vertices. */
  protected def transformUV(matrix: Matrix3, start: Int, count: Int): Unit = {
    val posAttr        = getVertexAttribute(Usage.TextureCoordinates).getOrElse(throw SgeError.GraphicsError("No texture coordinates attribute"))
    val offset         = posAttr.offset / 4
    val vertexSize     = getVertexSize() / 4
    val numVerticesVal = getNumVertices()

    val verts = new Array[Float](numVerticesVal * vertexSize)
    getVertices(0, verts.length, verts)
    Mesh.transformUV(matrix, verts, vertexSize, offset, start, count)
    setVertices(verts, 0, verts.length)
  }

  /** Copies this mesh optionally removing duplicate vertices and/or reducing the amount of attributes.
    * @param isStatic
    *   whether the new mesh is static or not. Allows for internal optimizations.
    * @param removeDuplicates
    *   whether to remove duplicate vertices if possible. Only the vertices specified by usage are checked.
    * @param usage
    *   which attributes (if available) to copy
    * @return
    *   the copy of this mesh
    */
  def copy(isStatic: Boolean, removeDuplicates: Boolean, usage: Nullable[Array[Int]]): Mesh = {
    val vertexSize     = getVertexSize() / 4
    var numVerticesVal = getNumVertices()
    var verts          = new Array[Float](numVerticesVal * vertexSize)
    getVertices(0, verts.length, verts)
    var checks: Array[Short]           = null.asInstanceOf[Array[Short]] // @nowarn — local temp only
    var attrs:  Array[VertexAttribute] = null.asInstanceOf[Array[VertexAttribute]] // @nowarn — local temp only
    var newVertexSize = 0
    usage.foreach { usageArr =>
      var size = 0
      var as   = 0
      for (i <- usageArr.indices)
        getVertexAttribute(usageArr(i)).foreach { a =>
          size += a.numComponents
          as += 1
        }
      if (size > 0) {
        attrs = new Array[VertexAttribute](as)
        checks = new Array[Short](size)
        var idx = -1
        var ai  = -1
        for (i <- usageArr.indices)
          getVertexAttribute(usageArr(i)).foreach { a =>
            for (j <- 0 until a.numComponents) {
              idx += 1
              checks(idx) = (a.offset + j).toShort
            }
            ai += 1
            attrs(ai) = a.copy()
            newVertexSize += a.numComponents
          }
      }
    }
    if (checks == null.asInstanceOf[Array[Short]]) { // @nowarn — null check for local temp
      checks = new Array[Short](vertexSize)
      for (i <- 0 until vertexSize)
        checks(i) = i.toShort
      newVertexSize = vertexSize
    }

    val numIndicesVal = getNumIndices()
    var indices: Array[Short] = null.asInstanceOf[Array[Short]] // @nowarn — local temp only
    if (numIndicesVal > 0) {
      indices = new Array[Short](numIndicesVal)
      getIndices(indices)
      if (removeDuplicates || newVertexSize != vertexSize) {
        val tmp  = new Array[Float](verts.length)
        var size = 0
        for (i <- 0 until numIndicesVal) {
          val idx1     = indices(i) * vertexSize
          var newIndex = -1.toShort
          if (removeDuplicates) {
            var j = 0.toShort
            while (j < size && newIndex < 0) {
              val idx2  = j * newVertexSize
              var found = true
              var k     = 0
              while (k < checks.length && found) {
                if (tmp(idx2 + k) != verts(idx1 + checks(k))) found = false
                k += 1
              }
              if (found) newIndex = j
              j = (j + 1).toShort
            }
          }
          if (newIndex > 0) {
            indices(i) = newIndex
          } else {
            val idx = size * newVertexSize
            for (j <- checks.indices)
              tmp(idx + j) = verts(idx1 + checks(j))
            indices(i) = size.toShort
            size += 1
          }
        }
        verts = tmp
        numVerticesVal = size
      }
    }

    val numInd = if (indices == null.asInstanceOf[Array[Short]]) 0 else indices.length // @nowarn — null check for local temp
    val result =
      if (attrs == null.asInstanceOf[Array[VertexAttribute]]) { // @nowarn — null check for local temp
        Mesh(isStatic, numVerticesVal, numInd, getVertexAttributes())
      } else {
        Mesh(isStatic, numVerticesVal, numInd)(attrs*)
      }
    result.setVertices(verts, 0, numVerticesVal * newVertexSize)
    if (indices != null.asInstanceOf[Array[Short]]) result.setIndices(indices)
    result
  }

  /** Copies this mesh.
    * @param isStatic
    *   whether the new mesh is static or not. Allows for internal optimizations.
    * @return
    *   the copy of this mesh
    */
  def copy(isStatic: Boolean): Mesh =
    copy(isStatic, false, Nullable.empty)
}

object Mesh {
  enum VertexDataType {
    case VertexArray, VertexBufferObject, VertexBufferObjectSubData, VertexBufferObjectWithVAO
  }

  /** list of all meshes * */
  private val meshes = mutable.Map[Application, DynamicArray[Mesh]]()

  private def makeVertexBuffer(isStatic: Boolean, maxVertices: Int, vertexAttributes: VertexAttributes)(using Sge): VertexData =
    if (Sge().graphics.gl30.isDefined) {
      VertexBufferObjectWithVAO(isStatic, maxVertices, vertexAttributes)
    } else {
      VertexBufferObject(isStatic, maxVertices, vertexAttributes)
    }

  private def createVertexData(meshType: VertexDataType, isStatic: Boolean, maxVertices: Int, attributes: VertexAttributes)(using Sge): VertexData =
    meshType match {
      case VertexDataType.VertexBufferObject =>
        VertexBufferObject(isStatic, maxVertices, attributes)
      case VertexDataType.VertexBufferObjectSubData =>
        VertexBufferObjectSubData(isStatic, maxVertices, attributes)
      case VertexDataType.VertexBufferObjectWithVAO =>
        VertexBufferObjectWithVAO(isStatic, maxVertices, attributes)
      case VertexDataType.VertexArray =>
        VertexArray(maxVertices, attributes)
    }

  private def createIndexData(meshType: VertexDataType, isStatic: Boolean, maxIndices: Int)(using Sge): IndexData =
    meshType match {
      case VertexDataType.VertexBufferObject =>
        IndexBufferObject(isStatic, maxIndices)
      case VertexDataType.VertexBufferObjectSubData =>
        IndexBufferObjectSubData(isStatic, maxIndices)
      case VertexDataType.VertexBufferObjectWithVAO =>
        IndexBufferObjectSubData(isStatic, maxIndices)
      case VertexDataType.VertexArray =>
        IndexArray(maxIndices)
    }

  /** Method to transform the positions in the float array. Normals will be kept as is. This is a potentially slow operation, use with care.
    * @param matrix
    *   the transformation matrix
    * @param vertices
    *   the float array
    * @param vertexSize
    *   the number of floats in each vertex
    * @param offset
    *   the offset within a vertex to the position
    * @param dimensions
    *   the size of the position
    * @param start
    *   the vertex to start with
    * @param count
    *   the amount of vertices to transform
    */
  def transform(matrix: Matrix4, vertices: Array[Float], vertexSize: Int, offset: Int, dimensions: Int, start: Int, count: Int): Unit = {
    if (offset < 0 || dimensions < 1 || (offset + dimensions) > vertexSize) throw new IndexOutOfBoundsException()
    if (start < 0 || count < 1 || ((start + count) * vertexSize) > vertices.length)
      throw new IndexOutOfBoundsException(
        s"start = $start, count = $count, vertexSize = $vertexSize, length = ${vertices.length}"
      )

    val tmp = Vector3()
    var idx = offset + (start * vertexSize)
    dimensions match {
      case 1 =>
        for (_ <- 0 until count) {
          tmp.set(vertices(idx), 0, 0).mul(matrix)
          vertices(idx) = tmp.x
          idx += vertexSize
        }
      case 2 =>
        for (_ <- 0 until count) {
          tmp.set(vertices(idx), vertices(idx + 1), 0).mul(matrix)
          vertices(idx) = tmp.x
          vertices(idx + 1) = tmp.y
          idx += vertexSize
        }
      case 3 =>
        for (_ <- 0 until count) {
          tmp.set(vertices(idx), vertices(idx + 1), vertices(idx + 2)).mul(matrix)
          vertices(idx) = tmp.x
          vertices(idx + 1) = tmp.y
          vertices(idx + 2) = tmp.z
          idx += vertexSize
        }
      case _ => // do nothing
    }
  }

  /** Method to transform the texture coordinates (UV) in the float array. This is a potentially slow operation, use with care.
    * @param matrix
    *   the transformation matrix
    * @param vertices
    *   the float array
    * @param vertexSize
    *   the number of floats in each vertex
    * @param offset
    *   the offset within a vertex to the texture location
    * @param start
    *   the vertex to start with
    * @param count
    *   the amount of vertices to transform
    */
  def transformUV(matrix: Matrix3, vertices: Array[Float], vertexSize: Int, offset: Int, start: Int, count: Int): Unit = {
    if (start < 0 || count < 1 || ((start + count) * vertexSize) > vertices.length)
      throw new IndexOutOfBoundsException(
        s"start = $start, count = $count, vertexSize = $vertexSize, length = ${vertices.length}"
      )

    val tmp = Vector2()
    var idx = offset + (start * vertexSize)
    for (_ <- 0 until count) {
      tmp.set(vertices(idx), vertices(idx + 1)).*(matrix)
      vertices(idx) = tmp.x
      vertices(idx + 1) = tmp.y
      idx += vertexSize
    }
  }

  private def addManagedMesh(app: Application, mesh: Mesh): Unit = {
    val managedResources = meshes.getOrElseUpdate(app, DynamicArray[Mesh]())
    managedResources.add(mesh)
  }

  /** Invalidates all meshes so the next time they are rendered new VBO handles are generated.
    * @param app
    */
  def invalidateAllMeshes(app: Application): Unit =
    meshes.get(app) match {
      case Some(meshesArray) =>
        for (mesh <- meshesArray) {
          mesh.vertices.invalidate()
          mesh.indices.invalidate()
        }
      case None => // do nothing
    }

  /** Will clear the managed mesh cache. I wouldn't use this if i was you :) */
  def clearAllMeshes(app: Application): Unit =
    meshes.remove(app)

  def getManagedStatus(): String = {
    val builder = new StringBuilder()
    builder.append("Managed meshes/app: { ")
    for ((_, meshArray) <- meshes) {
      builder.append(meshArray.size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }
}
