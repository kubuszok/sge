/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/MeshBuilder.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - implements MeshPartBuilder -> extends MeshPartBuilder
 *   - Static constants (MAX_VERTICES, MAX_INDEX) -> companion object vals
 *   - Static tmpIndices/tmpVertices -> companion object members
 *   - Static helper methods (transformPosition, transformNormal, createAttributes) -> companion object
 *   - FloatArray/ShortArray -> DynamicArray[Float]/DynamicArray[Short]
 *   - Array<MeshPart> -> DynamicArray[MeshPart]
 *   - indicesMap: IntIntMap -> Nullable[ObjectMap[Int,Int]]
 *   - VertexAttributes field: Nullable wrapping
 *   - Null params (Color, TextureRegion, Matrix4) -> Nullable[T]
 *   - All deprecated shape builder delegate methods ported
 *   - All core methods (begin, end, part, vertex, index, line, triangle, rect, addMesh) ported
 *   - end() requires (using Sge) for Mesh creation
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1359
 * Covenant-baseline-methods: MAX_INDEX,MAX_VERTICES,MeshBuilder,_attributes,_lastIndex,_primitiveType,a,aBiNor,aCol,aCp,aNor,aTangent,aUv,addMesh,addVertex,arr,arrow,attributes,attrs,begin,biNorOffset,bounds,box,capsule,circle,clear,colOffset,colSize,color,cone,cpOffset,createAttributes,cylinder,ellipse,end,endpart,ensureCapacity,ensureIndices,ensureRectangleIndices,ensureRectangles,ensureTriangleIndices,ensureTriangles,ensureVertices,floatsPerVertex,getIndices,getIndicesArray,getVertexTransform,getVertices,getVerticesArray,hasColor,hasUVTransform,i,index,indices,indicesMap,isVertexTransformationEnabled,istart,lastIndex,line,meshPart,n,norOffset,normalTransform,numIndices,numVertices,o,offset,part,parts,patch,posOffset,posSize,positionTransform,primitiveType,rect,setColor,setUVRange,setVertexTransform,setVertexTransformationEnabled,sphere,stride,tangentOffset,tempC1,tmpIndices,tmpNormal,tmpVertices,transformNormal,transformPosition,triangle,uOffset,uScale,uvOffset,v,vOffset,vScale,vTmp,vertTmp1,vertTmp2,vertTmp3,vertTmp4,vertex,vertexTransformationEnabled,vertices,vindex,x,y,z
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/MeshBuilder.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.{ Color, Mesh, PrimitiveMode, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g2d.TextureRegion
import sge.graphics.g3d.model.MeshPart
import sge.graphics.g3d.utils.shapebuilders._
import sge.graphics.glutils.ShaderProgram
import sge.math.{ MathUtils, Matrix3, Matrix4, Vector2, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.{ DynamicArray, Nullable, ObjectMap, SgeError }

/** Class to construct a mesh, optionally splitting it into one or more mesh parts. Before you can call any other method you must call {@link #begin(VertexAttributes)} or
  * {@link #begin(VertexAttributes, int)}. To use mesh parts you must call {@link #part(String, int)} before you start building the part. The MeshPart itself is only valid after the call to
  * {@link #end()}.
  * @author
  *   Xoppa
  */
class MeshBuilder()(using Sge) extends MeshPartBuilder {
  import MeshPartBuilder.VertexInfo

  protected val vertTmp1: VertexInfo = VertexInfo()
  protected val vertTmp2: VertexInfo = VertexInfo()
  protected val vertTmp3: VertexInfo = VertexInfo()
  protected val vertTmp4: VertexInfo = VertexInfo()

  protected val tempC1: Color = Color()

  /** The vertex attributes of the resulting mesh */
  protected var _attributes: Nullable[VertexAttributes] = Nullable.empty

  /** The vertices to construct, no size checking is done */
  protected var vertices: DynamicArray[Float] = DynamicArray[Float]()

  /** The indices to construct, no size checking is done */
  protected var indices: DynamicArray[Short] = DynamicArray[Short]()

  /** The size (in number of floats) of each vertex */
  protected var stride: Int = 0

  /** The current vertex index, used for indexing */
  protected var vindex: Int = 0

  /** The offset in the indices array when begin() was called, used to define a meshpart. */
  protected var istart: Int = 0

  /** The offset within an vertex to position */
  protected var posOffset: Int = 0

  /** The size (in number of floats) of the position attribute */
  protected var posSize: Int = 0

  /** The offset within an vertex to normal, or -1 if not available */
  protected var norOffset: Int = -1

  /** The offset within a vertex to binormal, or -1 if not available */
  protected var biNorOffset: Int = -1

  /** The offset within a vertex to tangent, or -1 if not available */
  protected var tangentOffset: Int = -1

  /** The offset within an vertex to color, or -1 if not available */
  protected var colOffset: Int = -1

  /** The size (in number of floats) of the color attribute */
  protected var colSize: Int = 0

  /** The offset within an vertex to packed color, or -1 if not available */
  protected var cpOffset: Int = -1

  /** The offset within an vertex to texture coordinates, or -1 if not available */
  protected var uvOffset: Int = -1

  /** The meshpart currently being created */
  protected var part: Nullable[MeshPart] = Nullable.empty

  /** The parts created between begin and end */
  protected var parts: DynamicArray[MeshPart] = DynamicArray[MeshPart]()

  /** The color used if no vertex color is specified. */
  protected val color:    Color   = Color(Color.WHITE)
  protected var hasColor: Boolean = false

  /** The current primitiveType */
  protected var _primitiveType: PrimitiveMode = PrimitiveMode(0)

  /** The UV range used when building */
  protected var uOffset:        Float        = 0f
  protected var uScale:         Float        = 1f
  protected var vOffset:        Float        = 0f
  protected var vScale:         Float        = 1f
  protected var hasUVTransform: Boolean      = false
  protected var vertex:         Array[Float] = scala.compiletime.uninitialized

  protected var vertexTransformationEnabled: Boolean     = false
  protected val positionTransform:           Matrix4     = Matrix4()
  protected val normalTransform:             Matrix3     = Matrix3()
  protected val bounds:                      BoundingBox = BoundingBox()

  private var _lastIndex: Int = -1

  /** Begin building a mesh. Call {@link #part(String, int)} to start a {@link MeshPart}.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def begin(attributes: Long): Unit =
    begin(MeshBuilder.createAttributes(attributes), PrimitiveMode(-1))

  /** Begin building a mesh. Call {@link #part(String, int)} to start a {@link MeshPart}. */
  def begin(attributes: VertexAttributes): Unit =
    begin(attributes, PrimitiveMode(-1))

  /** Begin building a mesh.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def begin(attributes: Long, primitiveType: PrimitiveMode): Unit =
    begin(MeshBuilder.createAttributes(attributes), primitiveType)

  /** Begin building a mesh */
  def begin(attributes: VertexAttributes, primitiveType: PrimitiveMode): Unit = {
    if (this._attributes.isDefined) throw new RuntimeException("Call end() first")
    this._attributes = Nullable(attributes)
    this.vertices.clear()
    this.indices.clear()
    this.parts.clear()
    this.vindex = 0
    this._lastIndex = -1
    this.istart = 0
    this.part = Nullable.empty
    this.stride = attributes.vertexSize / 4
    if (Nullable(this.vertex).forall(_.length < stride)) this.vertex = new Array[Float](stride)
    val a = attributes.findByUsage(Usage.Position)
    if (a.isEmpty) throw SgeError.InvalidInput("Cannot build mesh without position attribute")
    a.foreach { attr =>
      posOffset = attr.offset / 4
      posSize = attr.numComponents
    }
    val aNor = attributes.findByUsage(Usage.Normal)
    norOffset = aNor.map(_.offset / 4).getOrElse(-1)
    val aBiNor = attributes.findByUsage(Usage.BiNormal)
    biNorOffset = aBiNor.map(_.offset / 4).getOrElse(-1)
    val aTangent = attributes.findByUsage(Usage.Tangent)
    tangentOffset = aTangent.map(_.offset / 4).getOrElse(-1)
    val aCol = attributes.findByUsage(Usage.ColorUnpacked)
    colOffset = aCol.map(_.offset / 4).getOrElse(-1)
    colSize = aCol.map(_.numComponents).getOrElse(0)
    val aCp = attributes.findByUsage(Usage.ColorPacked)
    cpOffset = aCp.map(_.offset / 4).getOrElse(-1)
    val aUv = attributes.findByUsage(Usage.TextureCoordinates)
    uvOffset = aUv.map(_.offset / 4).getOrElse(-1)
    setColor(Nullable.empty)
    setVertexTransform(Nullable.empty)
    setUVRange(Nullable.empty[TextureRegion])
    this._primitiveType = primitiveType
    bounds.inf()
  }

  private def endpart(): Unit = {
    part.foreach { p =>
      bounds.center(p.center)
      bounds.dimensions(p.halfExtents).scl(0.5f)
      p.radius = p.halfExtents.length
      bounds.inf()
      p.offset = istart
      p.size = indices.size - istart
      istart = indices.size
    }
    part = Nullable.empty
  }

  /** Starts a new MeshPart. The mesh part is not usable until end() is called. This will reset the current color and vertex transformation.
    * @see
    *   #part(String, int, MeshPart)
    */
  def part(id: String, primitiveType: PrimitiveMode): MeshPart =
    part(id, primitiveType, MeshPart())

  /** Starts a new MeshPart. The mesh part is not usable until end() is called. This will reset the current color and vertex transformation.
    * @param id
    *   The id (name) of the part
    * @param primitiveType
    *   e.g. {@link GL20#GL_TRIANGLES} or {@link GL20#GL_LINES}
    * @param meshPart
    *   The part to receive the result
    */
  def part(id: String, primitiveType: PrimitiveMode, meshPart: MeshPart): MeshPart = {
    if (this._attributes.isEmpty) throw new RuntimeException("Call begin() first")
    endpart()

    part = Nullable(meshPart)
    meshPart.id = Nullable(id)
    this._primitiveType = primitiveType
    meshPart.primitiveType = primitiveType
    parts.add(meshPart)

    setColor(Nullable.empty)
    setVertexTransform(Nullable.empty)
    setUVRange(Nullable.empty[TextureRegion])

    meshPart
  }

  /** End building the mesh and returns the mesh
    * @param mesh
    *   The mesh to receive the built vertices and indices, must have the same attributes and must be big enough to hold the data, any existing data will be overwritten.
    */
  def end(mesh: Mesh): Mesh = {
    endpart()

    if (_attributes.isEmpty) throw SgeError.InvalidInput("Call begin() first")
    _attributes.foreach { attrs =>
      if (!attrs.equals(mesh.vertexAttributes)) throw SgeError.InvalidInput("Mesh attributes don't match")
      if ((mesh.maxVertices * stride) < vertices.size)
        throw SgeError.InvalidInput(
          "Mesh can't hold enough vertices: " + mesh.maxVertices + " * " + stride + " < " + vertices.size
        )
      if (mesh.maxIndices < indices.size)
        throw SgeError.InvalidInput("Mesh can't hold enough indices: " + mesh.maxIndices + " < " + indices.size)
    }

    mesh.setVertices(vertices.items, 0, vertices.size)
    mesh.setIndices(indices.items, 0, indices.size)

    for (p <- parts)
      p.mesh = mesh
    parts.clear()

    _attributes = Nullable.empty
    vertices.clear()
    indices.clear()

    mesh
  }

  /** End building the mesh and returns the mesh */
  def end(): Mesh =
    end(
      Mesh(
        true,
        Math.min(vertices.size / stride, MeshBuilder.MAX_VERTICES),
        indices.size,
        _attributes.getOrElse(throw SgeError.InvalidInput("Call begin() first"))
      )
    )

  /** Clears the data being built up until now, including the vertices, indices and all parts. Must be called in between the call to #begin and #end. Any builder calls made from the last call to
    * #begin up until now are practically discarded. The state (e.g. UV region, color, vertex transform) will remain unchanged.
    */
  def clear(): Unit = {
    this.vertices.clear()
    this.indices.clear()
    this.parts.clear()
    this.vindex = 0
    this._lastIndex = -1
    this.istart = 0
    this.part = Nullable.empty
  }

  /** @return the size in number of floats of one vertex, multiply by four to get the size in bytes. */
  def floatsPerVertex: Int = stride

  /** @return The number of vertices built up until now, only valid in between the call to begin() and end(). */
  def numVertices: Int = vertices.size / stride

  /** Get a copy of the vertices built so far.
    * @param out
    *   The float array to receive the copy of the vertices, must be at least `destOffset` + {@link #numVertices} * {@link #getFloatsPerVertex()} in size.
    * @param destOffset
    *   The offset (number of floats) in the out array where to start copying
    */
  def getVertices(out: Array[Float], destOffset: Int): Unit = {
    if (_attributes.isEmpty) throw SgeError.InvalidInput("Must be called in between #begin and #end")
    if ((destOffset < 0) || (destOffset > out.length - vertices.size))
      throw SgeError.InvalidInput("Array too small or offset out of range")
    System.arraycopy(vertices.items, 0, out, destOffset, vertices.size)
  }

  /** Provides direct access to the vertices array being built, use with care. The size of the array might be bigger, do not rely on the length of the array. Instead use {@link #getFloatsPerVertex()}
    * * {@link #numVertices} to calculate the usable size of the array. Must be called in between the call to #begin and #end.
    */
  protected def getVerticesArray: Array[Float] = vertices.items

  /** @return The number of indices built up until now, only valid in between the call to begin() and end(). */
  def numIndices: Int = indices.size

  /** Get a copy of the indices built so far.
    * @param out
    *   The short array to receive the copy of the indices, must be at least `destOffset` + {@link #numIndices} in size.
    * @param destOffset
    *   The offset (number of shorts) in the out array where to start copying
    */
  def getIndices(out: Array[Short], destOffset: Int): Unit = {
    if (_attributes.isEmpty) throw SgeError.InvalidInput("Must be called in between #begin and #end")
    if ((destOffset < 0) || (destOffset > out.length - indices.size))
      throw SgeError.InvalidInput("Array too small or offset out of range")
    System.arraycopy(indices.items, 0, out, destOffset, indices.size)
  }

  /** Provides direct access to the indices array being built, use with care. The size of the array might be bigger, do not rely on the length of the array. Instead use {@link #numIndices} to
    * calculate the usable size of the array. Must be called in between the call to #begin and #end.
    */
  protected def getIndicesArray: Array[Short] = indices.items

  override def attributes: VertexAttributes =
    _attributes.getOrElse(throw SgeError.InvalidInput("Call begin() first"))

  override def meshPart: MeshPart =
    part.getOrElse(throw SgeError.InvalidInput("No active mesh part"))

  override def primitiveType: PrimitiveMode = _primitiveType

  override def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    color.set(r, g, b, a)
    hasColor = !color.equals(Color.WHITE)
  }

  override def setColor(color: Nullable[Color]): Unit = {
    hasColor = color.isDefined
    this.color.set(if (!hasColor) Color.WHITE else color.getOrElse(Color.WHITE))
  }

  override def setUVRange(u1: Float, v1: Float, u2: Float, v2: Float): Unit = {
    uOffset = u1
    vOffset = v1
    uScale = u2 - u1
    vScale = v2 - v1
    hasUVTransform = !(MathUtils.isZero(u1) && MathUtils.isZero(v1) && MathUtils.isEqual(u2, 1f) && MathUtils.isEqual(v2, 1f))
  }

  override def setUVRange(region: Nullable[TextureRegion]): Unit =
    region.fold {
      hasUVTransform = false
      uOffset = 0f
      vOffset = 0f
      uScale = 1f
      vScale = 1f
    } { r =>
      hasUVTransform = true
      setUVRange(r.u, r.v, r.u2, r.v2)
    }

  override def getVertexTransform(out: Matrix4): Matrix4 =
    out.set(positionTransform)

  override def setVertexTransform(transform: Nullable[Matrix4]): Unit = {
    vertexTransformationEnabled = transform.isDefined
    transform.fold {
      positionTransform.idt()
      normalTransform.idt()
    } { t =>
      positionTransform.set(t)
      normalTransform.set(t).inv().transpose()
    }
  }

  override def isVertexTransformationEnabled: Boolean = vertexTransformationEnabled

  override def setVertexTransformationEnabled(enabled: Boolean): Unit =
    vertexTransformationEnabled = enabled

  override def ensureVertices(numVertices: Int): Unit =
    vertices.ensureCapacity(stride * numVertices)

  override def ensureIndices(numIndices: Int): Unit =
    indices.ensureCapacity(numIndices)

  override def ensureCapacity(numVertices: Int, numIndices: Int): Unit = {
    ensureVertices(numVertices)
    ensureIndices(numIndices)
  }

  override def ensureTriangleIndices(numTriangles: Int): Unit =
    if (primitiveType == PrimitiveMode.Lines)
      ensureIndices(6 * numTriangles)
    else if (primitiveType == PrimitiveMode.Triangles || primitiveType == PrimitiveMode.Points)
      ensureIndices(3 * numTriangles)
    else
      throw SgeError.InvalidInput("Incorrect primitive type")

  /** @deprecated use {@link #ensureVertices(int)} followed by {@link #ensureTriangleIndices(int)} instead. */
  @deprecated("use ensureVertices followed by ensureTriangleIndices instead", "")
  def ensureTriangles(numVertices: Int, numTriangles: Int): Unit = {
    ensureVertices(numVertices)
    ensureTriangleIndices(numTriangles)
  }

  /** @deprecated use {@link #ensureVertices(int)} followed by {@link #ensureTriangleIndices(int)} instead. */
  @deprecated("use ensureVertices followed by ensureTriangleIndices instead", "")
  def ensureTriangles(numTriangles: Int): Unit = {
    ensureVertices(3 * numTriangles)
    ensureTriangleIndices(numTriangles)
  }

  override def ensureRectangleIndices(numRectangles: Int): Unit =
    if (primitiveType == PrimitiveMode.Points)
      ensureIndices(4 * numRectangles)
    else if (primitiveType == PrimitiveMode.Lines)
      ensureIndices(8 * numRectangles)
    else
      // GL_TRIANGLES
      ensureIndices(6 * numRectangles)

  /** @deprecated use {@link #ensureVertices(int)} followed by {@link #ensureRectangleIndices(int)} instead. */
  @deprecated("use ensureVertices followed by ensureRectangleIndices instead", "")
  def ensureRectangles(numVertices: Int, numRectangles: Int): Unit = {
    ensureVertices(numVertices)
    ensureRectangleIndices(numRectangles)
  }

  /** @deprecated use {@link #ensureVertices(int)} followed by {@link #ensureRectangleIndices(int)} instead. */
  @deprecated("use ensureVertices followed by ensureRectangleIndices instead", "")
  def ensureRectangles(numRectangles: Int): Unit = {
    ensureVertices(4 * numRectangles)
    ensureRectangleIndices(numRectangles)
  }

  override def lastIndex(): Int = _lastIndex

  private def addVertex(values: Array[Float], offset: Int): Unit = {
    val o = vertices.size
    vertices.addAll(values, offset, stride)
    _lastIndex = vindex
    vindex += 1

    if (vertexTransformationEnabled) {
      MeshBuilder.transformPosition(vertices.items, o + posOffset, posSize, positionTransform)
      if (norOffset >= 0) MeshBuilder.transformNormal(vertices.items, o + norOffset, 3, normalTransform)
      if (biNorOffset >= 0) MeshBuilder.transformNormal(vertices.items, o + biNorOffset, 3, normalTransform)
      if (tangentOffset >= 0) MeshBuilder.transformNormal(vertices.items, o + tangentOffset, 3, normalTransform)
    }

    val x = vertices.items(o + posOffset)
    val y = if (posSize > 1) vertices.items(o + posOffset + 1) else 0f
    val z = if (posSize > 2) vertices.items(o + posOffset + 2) else 0f
    bounds.ext(x, y, z)

    if (hasColor) {
      if (colOffset >= 0) {
        vertices.items(o + colOffset) *= color.r
        vertices.items(o + colOffset + 1) *= color.g
        vertices.items(o + colOffset + 2) *= color.b
        if (colSize > 3) vertices.items(o + colOffset + 3) *= color.a
      } else if (cpOffset >= 0) {
        Color.abgr8888ToColor(tempC1, vertices.items(o + cpOffset))
        vertices.items(o + cpOffset) = tempC1.mul(color).toFloatBits()
      }
    }

    if (hasUVTransform && uvOffset >= 0) {
      vertices.items(o + uvOffset) = uOffset + uScale * vertices.items(o + uvOffset)
      vertices.items(o + uvOffset + 1) = vOffset + vScale * vertices.items(o + uvOffset + 1)
    }
  }

  private val tmpNormal: Vector3 = Vector3()

  override def vertex(pos: Vector3, nor: Nullable[Vector3], col: Nullable[Color], uv: Nullable[Vector2]): Short = {
    if (vindex > MeshBuilder.MAX_INDEX) throw SgeError.InvalidInput("Too many vertices used")

    vertex(posOffset) = pos.x
    if (posSize > 1) vertex(posOffset + 1) = pos.y
    if (posSize > 2) vertex(posOffset + 2) = pos.z

    if (norOffset >= 0) {
      val n = nor.getOrElse(tmpNormal.set(pos).nor())
      vertex(norOffset) = n.x
      vertex(norOffset + 1) = n.y
      vertex(norOffset + 2) = n.z
    }

    if (colOffset >= 0) {
      val c = col.getOrElse(Color.WHITE)
      vertex(colOffset) = c.r
      vertex(colOffset + 1) = c.g
      vertex(colOffset + 2) = c.b
      if (colSize > 3) vertex(colOffset + 3) = c.a
    } else if (cpOffset > 0) {
      val c = col.getOrElse(Color.WHITE)
      vertex(cpOffset) = c.toFloatBits() // FIXME cache packed color?
    }

    uv.foreach { uvVal =>
      if (uvOffset >= 0) {
        vertex(uvOffset) = uvVal.x
        vertex(uvOffset + 1) = uvVal.y
      }
    }

    addVertex(vertex, 0)
    _lastIndex.toShort
  }

  override def vertex(values: Float*): Short = {
    val arr = values.toArray
    val n   = arr.length - stride
    var i   = 0
    while (i <= n) {
      addVertex(arr, i)
      i += stride
    }
    _lastIndex.toShort
  }

  override def vertex(info: VertexInfo): Short =
    vertex(
      info.position,
      if (info.hasNormal) Nullable(info.normal) else Nullable.empty,
      if (info.hasColor) Nullable(info.color) else Nullable.empty,
      if (info.hasUV) Nullable(info.uv) else Nullable.empty
    )

  override def index(value: Short): Unit =
    indices.add(value)

  override def index(value1: Short, value2: Short): Unit = {
    ensureIndices(2)
    indices.add(value1)
    indices.add(value2)
  }

  override def index(value1: Short, value2: Short, value3: Short): Unit = {
    ensureIndices(3)
    indices.add(value1)
    indices.add(value2)
    indices.add(value3)
  }

  override def index(value1: Short, value2: Short, value3: Short, value4: Short): Unit = {
    ensureIndices(4)
    indices.add(value1)
    indices.add(value2)
    indices.add(value3)
    indices.add(value4)
  }

  override def index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short): Unit = {
    ensureIndices(6)
    indices.add(value1)
    indices.add(value2)
    indices.add(value3)
    indices.add(value4)
    indices.add(value5)
    indices.add(value6)
  }

  override def index(
    value1: Short,
    value2: Short,
    value3: Short,
    value4: Short,
    value5: Short,
    value6: Short,
    value7: Short,
    value8: Short
  ): Unit = {
    ensureIndices(8)
    indices.add(value1)
    indices.add(value2)
    indices.add(value3)
    indices.add(value4)
    indices.add(value5)
    indices.add(value6)
    indices.add(value7)
    indices.add(value8)
  }

  override def line(index1: Short, index2: Short): Unit = {
    if (primitiveType != PrimitiveMode.Lines) throw SgeError.InvalidInput("Incorrect primitive type")
    index(index1, index2)
  }

  override def line(p1: VertexInfo, p2: VertexInfo): Unit = {
    ensureVertices(2)
    line(vertex(p1), vertex(p2))
  }

  override def line(p1: Vector3, p2: Vector3): Unit =
    line(
      vertTmp1.set(Nullable(p1), Nullable.empty, Nullable.empty, Nullable.empty),
      vertTmp2.set(Nullable(p2), Nullable.empty, Nullable.empty, Nullable.empty)
    )

  override def line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Unit =
    line(
      vertTmp1.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x1, y1, z1),
      vertTmp2.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x2, y2, z2)
    )

  override def line(p1: Vector3, c1: Color, p2: Vector3, c2: Color): Unit =
    line(
      vertTmp1.set(Nullable(p1), Nullable.empty, Nullable(c1), Nullable.empty),
      vertTmp2.set(Nullable(p2), Nullable.empty, Nullable(c2), Nullable.empty)
    )

  override def triangle(index1: Short, index2: Short, index3: Short): Unit =
    if (primitiveType == PrimitiveMode.Triangles || primitiveType == PrimitiveMode.Points) {
      index(index1, index2, index3)
    } else if (primitiveType == PrimitiveMode.Lines) {
      index(index1, index2, index2, index3, index3, index1)
    } else
      throw SgeError.InvalidInput("Incorrect primitive type")

  override def triangle(p1: VertexInfo, p2: VertexInfo, p3: VertexInfo): Unit = {
    ensureVertices(3)
    triangle(vertex(p1), vertex(p2), vertex(p3))
  }

  override def triangle(p1: Vector3, p2: Vector3, p3: Vector3): Unit =
    triangle(
      vertTmp1.set(Nullable(p1), Nullable.empty, Nullable.empty, Nullable.empty),
      vertTmp2.set(Nullable(p2), Nullable.empty, Nullable.empty, Nullable.empty),
      vertTmp3.set(Nullable(p3), Nullable.empty, Nullable.empty, Nullable.empty)
    )

  override def triangle(p1: Vector3, c1: Color, p2: Vector3, c2: Color, p3: Vector3, c3: Color): Unit =
    triangle(
      vertTmp1.set(Nullable(p1), Nullable.empty, Nullable(c1), Nullable.empty),
      vertTmp2.set(Nullable(p2), Nullable.empty, Nullable(c2), Nullable.empty),
      vertTmp3.set(Nullable(p3), Nullable.empty, Nullable(c3), Nullable.empty)
    )

  override def rect(corner00: Short, corner10: Short, corner11: Short, corner01: Short): Unit =
    if (primitiveType == PrimitiveMode.Triangles) {
      index(corner00, corner10, corner11, corner11, corner01, corner00)
    } else if (primitiveType == PrimitiveMode.Lines) {
      index(corner00, corner10, corner10, corner11, corner11, corner01, corner01, corner00)
    } else if (primitiveType == PrimitiveMode.Points) {
      index(corner00, corner10, corner11, corner01)
    } else
      throw SgeError.InvalidInput("Incorrect primitive type")

  override def rect(corner00: VertexInfo, corner10: VertexInfo, corner11: VertexInfo, corner01: VertexInfo): Unit = {
    ensureVertices(4)
    rect(vertex(corner00), vertex(corner10), vertex(corner11), vertex(corner01))
  }

  override def rect(corner00: Vector3, corner10: Vector3, corner11: Vector3, corner01: Vector3, normal: Vector3): Unit =
    rect(
      vertTmp1.set(Nullable(corner00), Nullable(normal), Nullable.empty, Nullable.empty).setUV(0f, 1f),
      vertTmp2.set(Nullable(corner10), Nullable(normal), Nullable.empty, Nullable.empty).setUV(1f, 1f),
      vertTmp3.set(Nullable(corner11), Nullable(normal), Nullable.empty, Nullable.empty).setUV(1f, 0f),
      vertTmp4.set(Nullable(corner01), Nullable(normal), Nullable.empty, Nullable.empty).setUV(0f, 0f)
    )

  override def rect(
    x00:     Float,
    y00:     Float,
    z00:     Float,
    x10:     Float,
    y10:     Float,
    z10:     Float,
    x11:     Float,
    y11:     Float,
    z11:     Float,
    x01:     Float,
    y01:     Float,
    z01:     Float,
    normalX: Float,
    normalY: Float,
    normalZ: Float
  ): Unit =
    rect(
      vertTmp1.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x00, y00, z00).setNor(normalX, normalY, normalZ).setUV(0f, 1f),
      vertTmp2.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x10, y10, z10).setNor(normalX, normalY, normalZ).setUV(1f, 1f),
      vertTmp3.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x11, y11, z11).setNor(normalX, normalY, normalZ).setUV(1f, 0f),
      vertTmp4.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x01, y01, z01).setNor(normalX, normalY, normalZ).setUV(0f, 0f)
    )

  override def addMesh(mesh: Mesh): Unit =
    addMesh(mesh, 0, mesh.numIndices)

  override def addMesh(meshpart: MeshPart): Unit = {
    if (meshpart.primitiveType != primitiveType) throw SgeError.InvalidInput("Primitive type doesn't match")
    addMesh(meshpart.mesh, meshpart.offset, meshpart.size)
  }

  override def addMesh(mesh: Mesh, indexOffset: Int, numIndices: Int): Unit = {
    _attributes.foreach { attrs =>
      if (!attrs.equals(mesh.vertexAttributes)) throw SgeError.InvalidInput("Vertex attributes do not match")
    }
    if (numIndices <= 0) {} // silently ignore an empty mesh part
    else {
      // FIXME don't triple copy, instead move the copy to jni
      val numFloats = mesh.numVertices * stride
      MeshBuilder.tmpVertices.clear()
      MeshBuilder.tmpVertices.ensureCapacity(numFloats)
      MeshBuilder.tmpVertices.setSize(numFloats)
      mesh.getVertices(MeshBuilder.tmpVertices.items)

      MeshBuilder.tmpIndices.clear()
      MeshBuilder.tmpIndices.ensureCapacity(numIndices)
      MeshBuilder.tmpIndices.setSize(numIndices)
      mesh.getIndices(indexOffset, numIndices, MeshBuilder.tmpIndices.items, 0)

      addMesh(MeshBuilder.tmpVertices.items, MeshBuilder.tmpIndices.items, 0, numIndices)
    }
  }

  override def addMesh(vertices: Array[Float], indices: Array[Short], indexOffset: Int, numIndices: Int): Unit = {
    if (MeshBuilder.indicesMap.isEmpty) {
      MeshBuilder.indicesMap = Nullable(ObjectMap[Int, Int](numIndices))
    } else {
      MeshBuilder.indicesMap.foreach { m =>
        m.clear()
        m.ensureCapacity(numIndices)
      }
    }
    ensureIndices(numIndices)
    val numVertices = vertices.length / stride
    ensureVertices(if (numVertices < numIndices) numVertices else numIndices)
    for (i <- 0.until(numIndices)) {
      val sidx = indices(indexOffset + i) & 0xffff
      var didx = MeshBuilder.indicesMap.flatMap(_.get(sidx)).getOrElse(-1)
      if (didx < 0) {
        addVertex(vertices, sidx * stride)
        didx = _lastIndex
        MeshBuilder.indicesMap.foreach(_.put(sidx, didx))
      }
      index(didx.toShort)
    }
  }

  override def addMesh(vertices: Array[Float], indices: Array[Short]): Unit = {
    val offset = _lastIndex + 1

    val numVertices = vertices.length / stride
    ensureVertices(numVertices)
    var v = 0
    while (v < vertices.length) {
      addVertex(vertices, v)
      v += stride
    }

    ensureIndices(indices.length)
    for (i <- indices.indices)
      index(((indices(i) & 0xffff) + offset).toShort)
  }

  @deprecated("use PatchShapeBuilder.build instead", "")
  override def patch(
    corner00:   VertexInfo,
    corner10:   VertexInfo,
    corner11:   VertexInfo,
    corner01:   VertexInfo,
    divisionsU: Int,
    divisionsV: Int
  ): Unit =
    PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, divisionsU, divisionsV)

  @deprecated("use PatchShapeBuilder.build instead", "")
  override def patch(
    corner00:   Vector3,
    corner10:   Vector3,
    corner11:   Vector3,
    corner01:   Vector3,
    normal:     Vector3,
    divisionsU: Int,
    divisionsV: Int
  ): Unit =
    PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, normal, divisionsU, divisionsV)

  @deprecated("use PatchShapeBuilder.build instead", "")
  override def patch(
    x00:        Float,
    y00:        Float,
    z00:        Float,
    x10:        Float,
    y10:        Float,
    z10:        Float,
    x11:        Float,
    y11:        Float,
    z11:        Float,
    x01:        Float,
    y01:        Float,
    z01:        Float,
    normalX:    Float,
    normalY:    Float,
    normalZ:    Float,
    divisionsU: Int,
    divisionsV: Int
  ): Unit =
    PatchShapeBuilder.build(this, x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, divisionsU, divisionsV)

  @deprecated("use BoxShapeBuilder.build instead", "")
  override def box(
    corner000: VertexInfo,
    corner010: VertexInfo,
    corner100: VertexInfo,
    corner110: VertexInfo,
    corner001: VertexInfo,
    corner011: VertexInfo,
    corner101: VertexInfo,
    corner111: VertexInfo
  ): Unit =
    BoxShapeBuilder.build(this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111)

  @deprecated("use BoxShapeBuilder.build instead", "")
  override def box(
    corner000: Vector3,
    corner010: Vector3,
    corner100: Vector3,
    corner110: Vector3,
    corner001: Vector3,
    corner011: Vector3,
    corner101: Vector3,
    corner111: Vector3
  ): Unit =
    BoxShapeBuilder.build(this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111)

  @deprecated("use BoxShapeBuilder.build instead", "")
  override def box(transform: Matrix4): Unit =
    BoxShapeBuilder.build(this, transform)

  @deprecated("use BoxShapeBuilder.build instead", "")
  override def box(width: Float, height: Float, depth: Float): Unit =
    BoxShapeBuilder.build(this, width, height, depth)

  @deprecated("use BoxShapeBuilder.build instead", "")
  override def box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float): Unit =
    BoxShapeBuilder.build(this, x, y, z, width, height, depth)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float): Unit =
    EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3): Unit =
    EllipseShapeBuilder.build(this, radius, divisions, center, normal)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3): Unit =
    EllipseShapeBuilder.build(this, radius, divisions, center, normal, tangent, binormal)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float
  ): Unit =
    EllipseShapeBuilder.build(
      this,
      radius,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ
    )

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3, angleFrom: Float, angleTo: Float): Unit =
    EllipseShapeBuilder.build(this, radius, divisions, center, normal, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(
    radius:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    circle(
      radius,
      divisions,
      center.x,
      center.y,
      center.z,
      normal.x,
      normal.y,
      normal.z,
      tangent.x,
      tangent.y,
      tangent.z,
      binormal.x,
      binormal.y,
      binormal.z,
      angleFrom,
      angleTo
    )

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def circle(
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(
      this,
      radius,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      angleFrom,
      angleTo
    )

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, center, normal)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, center, normal, tangent, binormal)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float
  ): Unit =
    EllipseShapeBuilder.build(
      this,
      width,
      height,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ
    )

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, center, normal, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, divisions, center, normal, tangent, binormal, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    EllipseShapeBuilder.build(
      this,
      width,
      height,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      angleFrom,
      angleTo
    )

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    center:      Vector3,
    normal:      Vector3
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, center, normal)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float,
    angleFrom:   Float,
    angleTo:     Float
  ): Unit =
    EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)

  @deprecated("Use EllipseShapeBuilder.build instead", "")
  override def ellipse(
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float,
    tangentX:    Float,
    tangentY:    Float,
    tangentZ:    Float,
    binormalX:   Float,
    binormalY:   Float,
    binormalZ:   Float,
    angleFrom:   Float,
    angleTo:     Float
  ): Unit =
    EllipseShapeBuilder.build(
      this,
      width,
      height,
      innerWidth,
      innerHeight,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      angleFrom,
      angleTo
    )

  @deprecated("Use CylinderShapeBuilder.build instead", "")
  override def cylinder(width: Float, height: Float, depth: Float, divisions: Int): Unit =
    CylinderShapeBuilder.build(this, width, height, depth, divisions)

  @deprecated("Use CylinderShapeBuilder.build instead", "")
  override def cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit =
    CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo)

  @deprecated("Use CylinderShapeBuilder.build instead", "")
  override def cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float, close: Boolean): Unit =
    CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo, close)

  @deprecated("Use ConeShapeBuilder.build instead", "")
  override def cone(width: Float, height: Float, depth: Float, divisions: Int): Unit =
    cone(width, height, depth, divisions, 0, 360)

  @deprecated("Use ConeShapeBuilder.build instead", "")
  override def cone(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit =
    ConeShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo)

  @deprecated("Use SphereShapeBuilder.build instead", "")
  override def sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit =
    SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV)

  @deprecated("Use SphereShapeBuilder.build instead", "")
  override def sphere(transform: Matrix4, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit =
    SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV)

  @deprecated("Use SphereShapeBuilder.build instead", "")
  override def sphere(
    width:      Float,
    height:     Float,
    depth:      Float,
    divisionsU: Int,
    divisionsV: Int,
    angleUFrom: Float,
    angleUTo:   Float,
    angleVFrom: Float,
    angleVTo:   Float
  ): Unit =
    SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)

  @deprecated("Use SphereShapeBuilder.build instead", "")
  override def sphere(
    transform:  Matrix4,
    width:      Float,
    height:     Float,
    depth:      Float,
    divisionsU: Int,
    divisionsV: Int,
    angleUFrom: Float,
    angleUTo:   Float,
    angleVFrom: Float,
    angleVTo:   Float
  ): Unit =
    SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)

  @deprecated("Use CapsuleShapeBuilder.build instead", "")
  override def capsule(radius: Float, height: Float, divisions: Int): Unit =
    CapsuleShapeBuilder.build(this, radius, height, divisions)

  @deprecated("Use ArrowShapeBuilder.build instead", "")
  override def arrow(
    x1:            Float,
    y1:            Float,
    z1:            Float,
    x2:            Float,
    y2:            Float,
    z2:            Float,
    capLength:     Float,
    stemThickness: Float,
    divisions:     Int
  ): Unit =
    ArrowShapeBuilder.build(this, x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions)
}

object MeshBuilder {

  /** maximum number of vertices mesh builder can hold (64k) */
  final val MAX_VERTICES = 1 << 16

  /** highest index mesh builder can get (64k - 1) */
  final val MAX_INDEX = MAX_VERTICES - 1

  protected val tmpIndices:  DynamicArray[Short] = DynamicArray[Short]()
  protected val tmpVertices: DynamicArray[Float] = DynamicArray[Float]()

  private val vTmp: Vector3 = Vector3()

  private var indicesMap: Nullable[ObjectMap[Int, Int]] = Nullable.empty

  private def transformPosition(values: Array[Float], offset: Int, size: Int, transform: Matrix4): Unit =
    if (size > 2) {
      vTmp.set(values(offset), values(offset + 1), values(offset + 2)).mul(transform)
      values(offset) = vTmp.x
      values(offset + 1) = vTmp.y
      values(offset + 2) = vTmp.z
    } else if (size > 1) {
      vTmp.set(values(offset), values(offset + 1), 0).mul(transform)
      values(offset) = vTmp.x
      values(offset + 1) = vTmp.y
    } else
      values(offset) = vTmp.set(values(offset), 0, 0).mul(transform).x

  private def transformNormal(values: Array[Float], offset: Int, size: Int, transform: Matrix3): Unit =
    if (size > 2) {
      vTmp.set(values(offset), values(offset + 1), values(offset + 2)).mul(transform).nor()
      values(offset) = vTmp.x
      values(offset + 1) = vTmp.y
      values(offset + 2) = vTmp.z
    } else if (size > 1) {
      vTmp.set(values(offset), values(offset + 1), 0).mul(transform).nor()
      values(offset) = vTmp.x
      values(offset + 1) = vTmp.y
    } else
      values(offset) = vTmp.set(values(offset), 0, 0).mul(transform).nor().x

  /** @param usage
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createAttributes(usage: Long): VertexAttributes = {
    val attrs = DynamicArray[VertexAttribute]()
    if ((usage & Usage.Position) == Usage.Position)
      attrs.add(VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE))
    if ((usage & Usage.ColorUnpacked) == Usage.ColorUnpacked)
      attrs.add(VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE))
    if ((usage & Usage.ColorPacked) == Usage.ColorPacked)
      attrs.add(VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE))
    if ((usage & Usage.Normal) == Usage.Normal)
      attrs.add(VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE))
    if ((usage & Usage.TextureCoordinates) == Usage.TextureCoordinates)
      attrs.add(VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
    VertexAttributes(attrs.toArray*)
  }
}
