/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/MeshPartBuilder.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java interface -> Scala trait
 *   - VertexInfo inner static class -> companion object class
 *   - VertexInfo implements Poolable -> extends Pool.Poolable
 *   - Null params in VertexInfo.set(Vector3, Vector3, Color, Vector2) -> Nullable[T]
 *   - setColor(Color) -> setColor(Nullable[Color])
 *   - setUVRange(TextureRegion) -> setUVRange(Nullable[TextureRegion])
 *   - setVertexTransform(Matrix4) -> setVertexTransform(Nullable[Matrix4])
 *   - vertex(float...) -> vertex(Float*) (Scala varargs)
 *   - All deprecated shape methods preserved with @deprecated annotations
 *   - All 70+ method signatures match Java source
 *   - TODO: VertexInfo extends Pool.Poolable → define given Poolable[VertexInfo]
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.{ Color, Mesh, PrimitiveMode, VertexAttributes }
import sge.graphics.g2d.TextureRegion
import sge.graphics.g3d.model.MeshPart
import sge.math.{ Matrix4, Vector2, Vector3 }
import sge.utils.{ Nullable, Pool }

trait MeshPartBuilder {

  /** @return The {@link MeshPart} currently building. */
  def getMeshPart(): MeshPart

  /** @return The primitive type used for building, e.g. {@link GL20#GL_TRIANGLES} or {@link GL20#GL_LINES}. */
  def getPrimitiveType(): PrimitiveMode

  /** @return The {@link VertexAttributes} available for building. */
  def getAttributes(): VertexAttributes

  /** Set the color used to tint the vertex color, defaults to white. Only applicable for {@link Usage#ColorPacked} or {@link Usage#ColorUnpacked}.
    */
  def setColor(color: Nullable[Color]): Unit

  /** Set the color used to tint the vertex color, defaults to white. Only applicable for {@link Usage#ColorPacked} or {@link Usage#ColorUnpacked}.
    */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit

  /** Set range of texture coordinates used (default is 0,0,1,1). */
  def setUVRange(u1: Float, v1: Float, u2: Float, v2: Float): Unit

  /** Set range of texture coordinates from the specified TextureRegion. */
  def setUVRange(r: Nullable[TextureRegion]): Unit

  /** Get the current vertex transformation matrix. */
  def getVertexTransform(out: Matrix4): Matrix4

  /** Set the current vertex transformation matrix and enables vertex transformation. */
  def setVertexTransform(transform: Nullable[Matrix4]): Unit

  /** Indicates whether vertex transformation is enabled. */
  def isVertexTransformationEnabled(): Boolean

  /** Sets whether vertex transformation is enabled. */
  def setVertexTransformationEnabled(enabled: Boolean): Unit

  /** Increases the size of the backing vertices array to accommodate the specified number of additional vertices. Useful before adding many vertices to avoid multiple backing array resizes.
    * @param numVertices
    *   The number of vertices you are about to add
    */
  def ensureVertices(numVertices: Int): Unit

  /** Increases the size of the backing indices array to accommodate the specified number of additional indices. Useful before adding many indices to avoid multiple backing array resizes.
    * @param numIndices
    *   The number of indices you are about to add
    */
  def ensureIndices(numIndices: Int): Unit

  /** Increases the size of the backing vertices and indices arrays to accommodate the specified number of additional vertices and indices. Useful before adding many vertices and indices to avoid
    * multiple backing array resizes.
    * @param numVertices
    *   The number of vertices you are about to add
    * @param numIndices
    *   The number of indices you are about to add
    */
  def ensureCapacity(numVertices: Int, numIndices: Int): Unit

  /** Increases the size of the backing indices array to accommodate the specified number of additional triangles. Useful before adding many triangles using {@link #triangle(short, short, short)} to
    * avoid multiple backing array resizes. The actual number of indices accounted for depends on the primitive type (see {@link #getPrimitiveType()}).
    * @param numTriangles
    *   The number of triangles you are about to add
    */
  def ensureTriangleIndices(numTriangles: Int): Unit

  /** Increases the size of the backing indices array to accommodate the specified number of additional rectangles. Useful before adding many rectangles using {@link #rect(short, short, short, short)}
    * to avoid multiple backing array resizes.
    * @param numRectangles
    *   The number of rectangles you are about to add
    */
  def ensureRectangleIndices(numRectangles: Int): Unit

  /** Add one or more vertices, returns the index of the last vertex added. The length of values must a power of the vertex size.
    */
  def vertex(values: Float*): Short

  /** Add a vertex, returns the index. Null values are allowed. Use {@link #getAttributes} to check which values are available.
    */
  def vertex(pos: Vector3, nor: Nullable[Vector3], col: Nullable[Color], uv: Nullable[Vector2]): Short

  /** Add a vertex, returns the index. Use {@link #getAttributes} to check which values are available. */
  def vertex(info: MeshPartBuilder.VertexInfo): Short

  /** @return The index of the last added vertex. */
  def lastIndex(): Int

  /** Add an index, MeshPartBuilder expects all meshes to be indexed. */
  def index(value: Short): Unit

  /** Add multiple indices, MeshPartBuilder expects all meshes to be indexed. */
  def index(value1: Short, value2: Short): Unit

  /** Add multiple indices, MeshPartBuilder expects all meshes to be indexed. */
  def index(value1: Short, value2: Short, value3: Short): Unit

  /** Add multiple indices, MeshPartBuilder expects all meshes to be indexed. */
  def index(value1: Short, value2: Short, value3: Short, value4: Short): Unit

  /** Add multiple indices, MeshPartBuilder expects all meshes to be indexed. */
  def index(value1: Short, value2: Short, value3: Short, value4: Short, value5: Short, value6: Short): Unit

  /** Add multiple indices, MeshPartBuilder expects all meshes to be indexed. */
  def index(
    value1: Short,
    value2: Short,
    value3: Short,
    value4: Short,
    value5: Short,
    value6: Short,
    value7: Short,
    value8: Short
  ): Unit

  /** Add a line by indices. Requires GL_LINES primitive type. */
  def line(index1: Short, index2: Short): Unit

  /** Add a line. Requires GL_LINES primitive type. */
  def line(p1: MeshPartBuilder.VertexInfo, p2: MeshPartBuilder.VertexInfo): Unit

  /** Add a line. Requires GL_LINES primitive type. */
  def line(p1: Vector3, p2: Vector3): Unit

  /** Add a line. Requires GL_LINES primitive type. */
  def line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Unit

  /** Add a line. Requires GL_LINES primitive type. */
  def line(p1: Vector3, c1: Color, p2: Vector3, c2: Color): Unit

  /** Add a triangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def triangle(index1: Short, index2: Short, index3: Short): Unit

  /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def triangle(p1: MeshPartBuilder.VertexInfo, p2: MeshPartBuilder.VertexInfo, p3: MeshPartBuilder.VertexInfo): Unit

  /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def triangle(p1: Vector3, p2: Vector3, p3: Vector3): Unit

  /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def triangle(p1: Vector3, c1: Color, p2: Vector3, c2: Color, p3: Vector3, c3: Color): Unit

  /** Add a rectangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def rect(corner00: Short, corner10: Short, corner11: Short, corner01: Short): Unit

  /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def rect(
    corner00: MeshPartBuilder.VertexInfo,
    corner10: MeshPartBuilder.VertexInfo,
    corner11: MeshPartBuilder.VertexInfo,
    corner01: MeshPartBuilder.VertexInfo
  ): Unit

  /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def rect(corner00: Vector3, corner10: Vector3, corner11: Vector3, corner01: Vector3, normal: Vector3): Unit

  /** Add a rectangle Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def rect(
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
  ): Unit

  /** Copies a mesh to the mesh (part) currently being build.
    * @param mesh
    *   The mesh to copy, must have the same vertex attributes and must be indexed.
    */
  def addMesh(mesh: Mesh): Unit

  /** Copies a MeshPart to the mesh (part) currently being build.
    * @param meshpart
    *   The MeshPart to copy, must have the same vertex attributes, primitive type and must be indexed.
    */
  def addMesh(meshpart: MeshPart): Unit

  /** Copies a (part of a) mesh to the mesh (part) currently being build.
    * @param mesh
    *   The mesh to (partly) copy, must have the same vertex attributes and must be indexed.
    * @param indexOffset
    *   The zero-based offset of the first index of the part of the mesh to copy.
    * @param numIndices
    *   The number of indices of the part of the mesh to copy.
    */
  def addMesh(mesh: Mesh, indexOffset: Int, numIndices: Int): Unit

  /** Copies a mesh to the mesh (part) currently being build. The entire vertices array is added, even if some of the vertices are not indexed by the indices array. If you want to add only the
    * vertices that are actually indexed, then use the {@link #addMesh(float[], short[], int, int)} method instead.
    * @param vertices
    *   The vertices to copy, must be in the same vertex layout as the mesh being build.
    * @param indices
    *   Array containing the indices to copy, each index should be valid in the vertices array.
    */
  def addMesh(vertices: Array[Float], indices: Array[Short]): Unit

  /** Copies a (part of a) mesh to the mesh (part) currently being build.
    * @param vertices
    *   The vertices to (partly) copy, must be in the same vertex layout as the mesh being build.
    * @param indices
    *   Array containing the indices to (partly) copy, each index should be valid in the vertices array.
    * @param indexOffset
    *   The zero-based offset of the first index of the part of indices array to copy.
    * @param numIndices
    *   The number of indices of the part of the indices array to copy.
    */
  def addMesh(vertices: Array[Float], indices: Array[Short], indexOffset: Int, numIndices: Int): Unit

  // TODO: The following methods are deprecated and will be removed in a future release

  /** @deprecated use PatchShapeBuilder.build instead. */
  @deprecated("use PatchShapeBuilder.build instead", "")
  def patch(
    corner00:   MeshPartBuilder.VertexInfo,
    corner10:   MeshPartBuilder.VertexInfo,
    corner11:   MeshPartBuilder.VertexInfo,
    corner01:   MeshPartBuilder.VertexInfo,
    divisionsU: Int,
    divisionsV: Int
  ): Unit

  /** @deprecated use PatchShapeBuilder.build instead. */
  @deprecated("use PatchShapeBuilder.build instead", "")
  def patch(
    corner00:   Vector3,
    corner10:   Vector3,
    corner11:   Vector3,
    corner01:   Vector3,
    normal:     Vector3,
    divisionsU: Int,
    divisionsV: Int
  ): Unit

  /** @deprecated use PatchShapeBuilder.build instead. */
  @deprecated("use PatchShapeBuilder.build instead", "")
  def patch(
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
  ): Unit

  /** @deprecated use BoxShapeBuilder.build instead. */
  @deprecated("use BoxShapeBuilder.build instead", "")
  def box(
    corner000: MeshPartBuilder.VertexInfo,
    corner010: MeshPartBuilder.VertexInfo,
    corner100: MeshPartBuilder.VertexInfo,
    corner110: MeshPartBuilder.VertexInfo,
    corner001: MeshPartBuilder.VertexInfo,
    corner011: MeshPartBuilder.VertexInfo,
    corner101: MeshPartBuilder.VertexInfo,
    corner111: MeshPartBuilder.VertexInfo
  ): Unit

  /** @deprecated use BoxShapeBuilder.build instead. */
  @deprecated("use BoxShapeBuilder.build instead", "")
  def box(
    corner000: Vector3,
    corner010: Vector3,
    corner100: Vector3,
    corner110: Vector3,
    corner001: Vector3,
    corner011: Vector3,
    corner101: Vector3,
    corner111: Vector3
  ): Unit

  /** @deprecated use BoxShapeBuilder.build instead. */
  @deprecated("use BoxShapeBuilder.build instead", "")
  def box(transform: Matrix4): Unit

  /** @deprecated use BoxShapeBuilder.build instead. */
  @deprecated("use BoxShapeBuilder.build instead", "")
  def box(width: Float, height: Float, depth: Float): Unit

  /** @deprecated use BoxShapeBuilder.build instead. */
  @deprecated("use BoxShapeBuilder.build instead", "")
  def box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(radius: Float, divisions: Int, center: Vector3, normal: Vector3, angleFrom: Float, angleTo: Float): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(
    radius:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def circle(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
    width:     Float,
    height:    Float,
    divisions: Int,
    center:    Vector3,
    normal:    Vector3,
    tangent:   Vector3,
    binormal:  Vector3,
    angleFrom: Float,
    angleTo:   Float
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
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
  ): Unit

  /** @deprecated Use EllipseShapeBuilder.build instead. */
  @deprecated("Use EllipseShapeBuilder.build instead", "")
  def ellipse(
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    center:      Vector3,
    normal:      Vector3
  ): Unit

  /** @deprecated Use CylinderShapeBuilder.build instead. */
  @deprecated("Use CylinderShapeBuilder.build instead", "")
  def cylinder(width: Float, height: Float, depth: Float, divisions: Int): Unit

  /** @deprecated Use CylinderShapeBuilder.build instead. */
  @deprecated("Use CylinderShapeBuilder.build instead", "")
  def cylinder(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit

  /** @deprecated Use CylinderShapeBuilder.build instead. */
  @deprecated("Use CylinderShapeBuilder.build instead", "")
  def cylinder(
    width:     Float,
    height:    Float,
    depth:     Float,
    divisions: Int,
    angleFrom: Float,
    angleTo:   Float,
    close:     Boolean
  ): Unit

  /** @deprecated Use ConeShapeBuilder.build instead. */
  @deprecated("Use ConeShapeBuilder.build instead", "")
  def cone(width: Float, height: Float, depth: Float, divisions: Int): Unit

  /** @deprecated Use ConeShapeBuilder.build instead. */
  @deprecated("Use ConeShapeBuilder.build instead", "")
  def cone(width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit

  /** @deprecated Use SphereShapeBuilder.build instead. */
  @deprecated("Use SphereShapeBuilder.build instead", "")
  def sphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit

  /** @deprecated Use SphereShapeBuilder.build instead. */
  @deprecated("Use SphereShapeBuilder.build instead", "")
  def sphere(transform: Matrix4, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit

  /** @deprecated Use SphereShapeBuilder.build instead. */
  @deprecated("Use SphereShapeBuilder.build instead", "")
  def sphere(
    width:      Float,
    height:     Float,
    depth:      Float,
    divisionsU: Int,
    divisionsV: Int,
    angleUFrom: Float,
    angleUTo:   Float,
    angleVFrom: Float,
    angleVTo:   Float
  ): Unit

  /** @deprecated Use SphereShapeBuilder.build instead. */
  @deprecated("Use SphereShapeBuilder.build instead", "")
  def sphere(
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
  ): Unit

  /** @deprecated Use CapsuleShapeBuilder.build instead. */
  @deprecated("Use CapsuleShapeBuilder.build instead", "")
  def capsule(radius: Float, height: Float, divisions: Int): Unit

  /** @deprecated Use ArrowShapeBuilder.build instead. */
  @deprecated("Use ArrowShapeBuilder.build instead", "")
  def arrow(
    x1:            Float,
    y1:            Float,
    z1:            Float,
    x2:            Float,
    y2:            Float,
    z2:            Float,
    capLength:     Float,
    stemThickness: Float,
    divisions:     Int
  ): Unit
}

object MeshPartBuilder {

  /** Class that contains all vertex information the builder can use.
    * @author
    *   Xoppa
    */
  class VertexInfo extends Pool.Poolable {
    val position:    Vector3 = Vector3()
    var hasPosition: Boolean = false
    val normal:      Vector3 = Vector3(0, 1, 0)
    var hasNormal:   Boolean = false
    val color:       Color   = Color(1, 1, 1, 1)
    var hasColor:    Boolean = false
    val uv:          Vector2 = Vector2()
    var hasUV:       Boolean = false

    override def reset(): Unit = {
      position.set(0, 0, 0)
      normal.set(0, 1, 0)
      color.set(1, 1, 1, 1)
      uv.set(0, 0)
    }

    def set(pos: Nullable[Vector3], nor: Nullable[Vector3], col: Nullable[Color], uv: Nullable[Vector2]): VertexInfo = {
      reset()
      hasPosition = pos.isDefined
      pos.foreach(p => position.set(p))
      hasNormal = nor.isDefined
      nor.foreach(n => normal.set(n))
      hasColor = col.isDefined
      col.foreach(c => color.set(c))
      hasUV = uv.isDefined
      uv.foreach(u => this.uv.set(u))
      this
    }

    def set(other: Nullable[VertexInfo]): VertexInfo =
      other.fold(set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)) { o =>
        hasPosition = o.hasPosition
        position.set(o.position)
        hasNormal = o.hasNormal
        normal.set(o.normal)
        hasColor = o.hasColor
        color.set(o.color)
        hasUV = o.hasUV
        uv.set(o.uv)
        this
      }

    def setPos(x: Float, y: Float, z: Float): VertexInfo = {
      position.set(x, y, z)
      hasPosition = true
      this
    }

    def setPos(pos: Nullable[Vector3]): VertexInfo = {
      hasPosition = pos.isDefined
      pos.foreach(p => position.set(p))
      this
    }

    def setNor(x: Float, y: Float, z: Float): VertexInfo = {
      normal.set(x, y, z)
      hasNormal = true
      this
    }

    def setNor(nor: Nullable[Vector3]): VertexInfo = {
      hasNormal = nor.isDefined
      nor.foreach(n => normal.set(n))
      this
    }

    def setCol(r: Float, g: Float, b: Float, a: Float): VertexInfo = {
      color.set(r, g, b, a)
      hasColor = true
      this
    }

    def setCol(col: Nullable[Color]): VertexInfo = {
      hasColor = col.isDefined
      col.foreach(c => color.set(c))
      this
    }

    def setUV(u: Float, v: Float): VertexInfo = {
      uv.set(u, v)
      hasUV = true
      this
    }

    def setUV(uv: Nullable[Vector2]): VertexInfo = {
      hasUV = uv.isDefined
      uv.foreach(u => this.uv.set(u))
      this
    }

    def lerp(target: VertexInfo, alpha: Float): VertexInfo = {
      if (hasPosition && target.hasPosition) position.lerp(target.position, alpha)
      if (hasNormal && target.hasNormal) normal.lerp(target.normal, alpha)
      if (hasColor && target.hasColor) color.lerp(target.color, alpha)
      if (hasUV && target.hasUV) uv.lerp(target.uv, alpha)
      this
    }
  }
}
