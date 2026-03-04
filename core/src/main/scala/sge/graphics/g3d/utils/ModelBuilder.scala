/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/ModelBuilder.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Array<MeshBuilder> -> DynamicArray[MeshBuilder]
 *   - model/node fields: null -> Nullable[Model]/Nullable[Node]
 *   - manage(Disposable) -> manage(AutoCloseable)
 *   - end() returns Model, requires (using Sge)
 *   - Static rebuildReferences -> companion object method
 *   - All factory methods (createBox, createRect, createCylinder, createCone,
 *     createSphere, createCapsule, createXYZCoordinates, createArrow, createLineGrid) ported
 *   - All part() overloads (5 total) ported
 *   - @nowarn("msg=deprecated") on class to suppress deprecated shape builder calls
 *   - Minor: `new Matrix4()` orphan on line 43 (appears to be unused tmpTransform allocation)
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import scala.annotation.nowarn
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.{ Color, GL20, Mesh, VertexAttributes }
import sge.graphics.g3d.model.{ MeshPart, Node, NodePart }
import sge.math.Vector3
import sge.Sge
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** Helper class to create {@link Model}s from code. To start building use the {@link #begin()} method, when finished building use the {@link #end()} method. The end method returns the model just
  * build. Building cannot be nested, only one model (per ModelBuilder) can be build at the time. The same ModelBuilder can be used to build multiple models sequential. Use the {@link #node()} method
  * to start a new node. Use one of the #part(...) methods to add a part within a node. The {@link #part(String, int, VertexAttributes, Material)} method will return a {@link MeshPartBuilder} which
  * can be used to build the node part.
  * @author
  *   Xoppa
  */
@nowarn("msg=deprecated")
class ModelBuilder()(using Sge) {

  /** The model currently being build */
  protected var model: Nullable[Model] = Nullable.empty

  /** The node currently being build */
  protected var currentNode: Nullable[Node] = Nullable.empty

  /** The mesh builders created between begin and end */
  protected var builders: DynamicArray[MeshBuilder] = DynamicArray[MeshBuilder]()

  private def getBuilder(attributes: VertexAttributes): MeshBuilder = boundary {
    for (mb <- builders)
      if (mb.getAttributes().equals(attributes) && mb.lastIndex() < MeshBuilder.MAX_VERTICES / 2) break(mb)
    val result = MeshBuilder()
    result.begin(attributes)
    builders.add(result)
    result
  }

  /** Begin building a new model */
  def begin(): Unit = {
    if (model.isDefined) throw SgeError.InvalidInput("Call end() first")
    currentNode = Nullable.empty
    model = Nullable(Model())
    builders.clear()
  }

  /** End building the model.
    * @return
    *   The newly created model. Call the {@link Model#close()} method when no longer used.
    */
  def end(): Model = {
    if (model.isEmpty) throw SgeError.InvalidInput("Call begin() first")
    val result = model.getOrElse(throw SgeError.InvalidInput("Call begin() first"))
    endnode()
    model = Nullable.empty

    for (mb <- builders)
      mb.end()
    builders.clear()

    ModelBuilder.rebuildReferences(result)
    result
  }

  private def endnode(): Unit =
    currentNode.foreach { _ =>
      currentNode = Nullable.empty
    }

  /** Adds the {@link Node} to the model and sets it active for building. Use any of the part(...) method to add a NodePart. */
  protected def node(node: Node): Node = {
    if (model.isEmpty) throw SgeError.InvalidInput("Call begin() first")

    endnode()

    model.foreach(_.nodes.add(node))
    this.currentNode = Nullable(node)

    node
  }

  /** Add a node to the model. Use any of the part(...) method to add a NodePart.
    * @return
    *   The node being created.
    */
  def node(): Node = {
    val n = Node()
    node(n)
    model.foreach(m => n.id = "node" + m.nodes.size)
    n
  }

  /** Adds the nodes of the specified model to a new node of the model being build. After this method the given model can no longer be used. Do not call the {@link Model#close()} method on that model.
    * @return
    *   The newly created node containing the nodes of the given model.
    */
  def node(id: String, model: Model): Node = {
    val n = Node()
    n.id = id
    n.addChildren(model.nodes)
    node(n)
    for (disposable <- model.getManagedDisposables)
      manage(disposable)
    n
  }

  /** Add the {@link AutoCloseable} object to the model, causing it to be disposed when the model is disposed. */
  def manage(disposable: AutoCloseable): Unit = {
    if (model.isEmpty) throw SgeError.InvalidInput("Call begin() first")
    model.foreach(_.manageDisposable(disposable))
  }

  /** Adds the specified MeshPart to the current Node. The Mesh will be managed by the model and disposed when the model is disposed. The resources the Material might contain are not managed, use
    * {@link #manage(AutoCloseable)} to add those to the model.
    */
  def part(meshpart: MeshPart, material: Material): Unit = {
    if (currentNode.isEmpty) this.node()
    currentNode.foreach(_.parts.add(NodePart(meshpart, material)))
  }

  /** Adds the specified mesh part to the current node. The Mesh will be managed by the model and disposed when the model is disposed. The resources the Material might contain are not managed, use
    * {@link #manage(AutoCloseable)} to add those to the model.
    * @return
    *   The added MeshPart.
    */
  def part(id: String, mesh: Mesh, primitiveType: Int, offset: Int, size: Int, material: Material): MeshPart = {
    val meshPart = MeshPart()
    meshPart.id = Nullable(id)
    meshPart.primitiveType = primitiveType
    meshPart.mesh = mesh
    meshPart.offset = offset
    meshPart.size = size
    part(meshPart, material)
    meshPart
  }

  /** Adds the specified mesh part to the current node. The Mesh will be managed by the model and disposed when the model is disposed. The resources the Material might contain are not managed, use
    * {@link #manage(AutoCloseable)} to add those to the model.
    * @return
    *   The added MeshPart.
    */
  def part(id: String, mesh: Mesh, primitiveType: Int, material: Material): MeshPart =
    part(id, mesh, primitiveType, 0, mesh.getNumIndices(), material)

  /** Creates a new MeshPart within the current Node and returns a {@link MeshPartBuilder} which can be used to build the shape of the part. If possible a previously used {@link MeshPartBuilder} will
    * be reused, to reduce the number of mesh binds. Therefore you can only build one part at a time. The resources the Material might contain are not managed, use {@link #manage(AutoCloseable)} to
    * add those to the model.
    * @return
    *   The {@link MeshPartBuilder} you can use to build the MeshPart.
    */
  def part(id: String, primitiveType: Int, attributes: VertexAttributes, material: Material): MeshPartBuilder = {
    val builder = getBuilder(attributes)
    part(builder.part(id, primitiveType), material)
    builder
  }

  /** Creates a new MeshPart within the current Node and returns a {@link MeshPartBuilder} which can be used to build the shape of the part. If possible a previously used {@link MeshPartBuilder} will
    * be reused, to reduce the number of mesh binds. Therefore you can only build one part at a time. The resources the Material might contain are not managed, use {@link #manage(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    * @return
    *   The {@link MeshPartBuilder} you can use to build the MeshPart.
    */
  def part(id: String, primitiveType: Int, attributes: Long, material: Material): MeshPartBuilder =
    part(id, primitiveType, MeshBuilder.createAttributes(attributes), material)

  /** Convenience method to create a model with a single node containing a box shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to add
    * those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createBox(width: Float, height: Float, depth: Float, material: Material, attributes: Long): Model =
    createBox(width, height, depth, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a box shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to add
    * those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createBox(width: Float, height: Float, depth: Float, primitiveType: Int, material: Material, attributes: Long): Model = {
    begin()
    part("box", primitiveType, attributes, material).box(width, height, depth)
    end()
  }

  /** Convenience method to create a model with a single node containing a rectangle shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createRect(
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
    material:   Material,
    attributes: Long
  ): Model =
    createRect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a rectangle shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createRect(
    x00:           Float,
    y00:           Float,
    z00:           Float,
    x10:           Float,
    y10:           Float,
    z10:           Float,
    x11:           Float,
    y11:           Float,
    z11:           Float,
    x01:           Float,
    y01:           Float,
    z01:           Float,
    normalX:       Float,
    normalY:       Float,
    normalZ:       Float,
    primitiveType: Int,
    material:      Material,
    attributes:    Long
  ): Model = {
    begin()
    part("rect", primitiveType, attributes, material).rect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ)
    end()
  }

  /** Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCylinder(width: Float, height: Float, depth: Float, divisions: Int, material: Material, attributes: Long): Model =
    createCylinder(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCylinder(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long): Model =
    createCylinder(width, height, depth, divisions, primitiveType, material, attributes, 0, 360)

  /** Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCylinder(width: Float, height: Float, depth: Float, divisions: Int, material: Material, attributes: Long, angleFrom: Float, angleTo: Float): Model =
    createCylinder(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes, angleFrom, angleTo)

  /** Convenience method to create a model with a single node containing a cylinder shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCylinder(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long, angleFrom: Float, angleTo: Float): Model = {
    begin()
    part("cylinder", primitiveType, attributes, material).cylinder(width, height, depth, divisions, angleFrom, angleTo)
    end()
  }

  /** Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCone(width: Float, height: Float, depth: Float, divisions: Int, material: Material, attributes: Long): Model =
    createCone(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCone(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long): Model =
    createCone(width, height, depth, divisions, primitiveType, material, attributes, 0, 360)

  /** Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCone(width: Float, height: Float, depth: Float, divisions: Int, material: Material, attributes: Long, angleFrom: Float, angleTo: Float): Model =
    createCone(width, height, depth, divisions, GL20.GL_TRIANGLES, material, attributes, angleFrom, angleTo)

  /** Convenience method to create a model with a single node containing a cone shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCone(width: Float, height: Float, depth: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long, angleFrom: Float, angleTo: Float): Model = {
    begin()
    part("cone", primitiveType, attributes, material).cone(width, height, depth, divisions, angleFrom, angleTo)
    end()
  }

  /** Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, material: Material, attributes: Long): Model =
    createSphere(width, height, depth, divisionsU, divisionsV, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createSphere(width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, primitiveType: Int, material: Material, attributes: Long): Model =
    createSphere(width, height, depth, divisionsU, divisionsV, primitiveType, material, attributes, 0, 360, 0, 180)

  /** Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createSphere(
    width:      Float,
    height:     Float,
    depth:      Float,
    divisionsU: Int,
    divisionsV: Int,
    material:   Material,
    attributes: Long,
    angleUFrom: Float,
    angleUTo:   Float,
    angleVFrom: Float,
    angleVTo:   Float
  ): Model =
    createSphere(
      width,
      height,
      depth,
      divisionsU,
      divisionsV,
      GL20.GL_TRIANGLES,
      material,
      attributes,
      angleUFrom,
      angleUTo,
      angleVFrom,
      angleVTo
    )

  /** Convenience method to create a model with a single node containing a sphere shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createSphere(
    width:         Float,
    height:        Float,
    depth:         Float,
    divisionsU:    Int,
    divisionsV:    Int,
    primitiveType: Int,
    material:      Material,
    attributes:    Long,
    angleUFrom:    Float,
    angleUTo:      Float,
    angleVFrom:    Float,
    angleVTo:      Float
  ): Model = {
    begin()
    part("sphere", primitiveType, attributes, material).sphere(width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)
    end()
  }

  /** Convenience method to create a model with a single node containing a capsule shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCapsule(radius: Float, height: Float, divisions: Int, material: Material, attributes: Long): Model =
    createCapsule(radius, height, divisions, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with a single node containing a capsule shape. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to
    * add those to the model.
    * @param attributes
    *   bitwise mask of the {@link com.badlogic.gdx.graphics.VertexAttributes.Usage}, only Position, Color, Normal and TextureCoordinates is supported.
    */
  def createCapsule(radius: Float, height: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long): Model = {
    begin()
    part("capsule", primitiveType, attributes, material).capsule(radius, height, divisions)
    end()
  }

  /** Convenience method to create a model with three orthonormal vectors shapes. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to add
    * those to the model.
    * @param axisLength
    *   Length of each axis.
    * @param capLength
    *   is the height of the cap in percentage, must be in (0,1)
    * @param stemThickness
    *   is the percentage of stem diameter compared to cap diameter, must be in (0,1]
    * @param divisions
    *   the amount of vertices used to generate the cap and stem ellipsoidal bases
    */
  def createXYZCoordinates(axisLength: Float, capLength: Float, stemThickness: Float, divisions: Int, primitiveType: Int, material: Material, attributes: Long): Model = {
    begin()
    node()

    val partBuilder = part("xyz", primitiveType, attributes, material)
    partBuilder.setColor(Nullable(Color.RED))
    partBuilder.arrow(0, 0, 0, axisLength, 0, 0, capLength, stemThickness, divisions)
    partBuilder.setColor(Nullable(Color.GREEN))
    partBuilder.arrow(0, 0, 0, 0, axisLength, 0, capLength, stemThickness, divisions)
    partBuilder.setColor(Nullable(Color.BLUE))
    partBuilder.arrow(0, 0, 0, 0, 0, axisLength, capLength, stemThickness, divisions)

    end()
  }

  def createXYZCoordinates(axisLength: Float, material: Material, attributes: Long): Model =
    createXYZCoordinates(axisLength, 0.1f, 0.1f, 5, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model with an arrow. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to add those to the model.
    * @param material
    * @param capLength
    *   is the height of the cap in percentage, must be in (0,1)
    * @param stemThickness
    *   is the percentage of stem diameter compared to cap diameter, must be in (0,1]
    * @param divisions
    *   the amount of vertices used to generate the cap and stem ellipsoidal bases
    */
  def createArrow(
    x1:            Float,
    y1:            Float,
    z1:            Float,
    x2:            Float,
    y2:            Float,
    z2:            Float,
    capLength:     Float,
    stemThickness: Float,
    divisions:     Int,
    primitiveType: Int,
    material:      Material,
    attributes:    Long
  ): Model = {
    begin()
    part("arrow", primitiveType, attributes, material).arrow(x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions)
    end()
  }

  /** Convenience method to create a model with an arrow. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)} to add those to the model.
    */
  def createArrow(from: Vector3, to: Vector3, material: Material, attributes: Long): Model =
    createArrow(from.x, from.y, from.z, to.x, to.y, to.z, 0.1f, 0.1f, 5, GL20.GL_TRIANGLES, material, attributes)

  /** Convenience method to create a model which represents a grid of lines on the XZ plane. The resources the Material might contain are not managed, use {@link Model#manageDisposable(AutoCloseable)}
    * to add those to the model.
    * @param xDivisions
    *   row count along x axis.
    * @param zDivisions
    *   row count along z axis.
    * @param xSize
    *   Length of a single row on x.
    * @param zSize
    *   Length of a single row on z.
    */
  def createLineGrid(xDivisions: Int, zDivisions: Int, xSize: Float, zSize: Float, material: Material, attributes: Long): Model = {
    begin()
    val partBuilder = part("lines", GL20.GL_LINES, attributes, material)
    val xlength     = xDivisions * xSize
    val zlength     = zDivisions * zSize
    val hxlength    = xlength / 2
    val hzlength    = zlength / 2
    var x1          = -hxlength
    var y1          = 0f
    var z1          = hzlength
    var x2          = -hxlength
    var y2          = 0f
    var z2          = -hzlength
    for (_ <- 0 to xDivisions) {
      partBuilder.line(x1, y1, z1, x2, y2, z2)
      x1 += xSize
      x2 += xSize
    }

    x1 = -hxlength
    y1 = 0
    z1 = -hzlength
    x2 = hxlength
    y2 = 0
    z2 = -hzlength
    for (_ <- 0 to zDivisions) {
      partBuilder.line(x1, y1, z1, x2, y2, z2)
      z1 += zSize
      z2 += zSize
    }

    end()
  }
}

object ModelBuilder {

  /** Resets the references to {@link Material}s, {@link Mesh}es and {@link MeshPart}s within the model to the ones used within it's nodes. This will make the model responsible for disposing all
    * referenced meshes.
    */
  def rebuildReferences(model: Model): Unit = {
    model.materials.clear()
    model.meshes.clear()
    model.meshParts.clear()
    for (node <- model.nodes)
      rebuildReferences(model, node)
  }

  private def rebuildReferences(model: Model, node: Node): Unit = {
    for (mpm <- node.parts) {
      if (!model.materials.containsByRef(mpm.material)) model.materials.add(mpm.material)
      if (!model.meshParts.containsByRef(mpm.meshPart)) {
        model.meshParts.add(mpm.meshPart)
        if (!model.meshes.containsByRef(mpm.meshPart.mesh)) model.meshes.add(mpm.meshPart.mesh)
        model.manageDisposable(mpm.meshPart.mesh)
      }
    }
    for (child <- node.children)
      rebuildReferences(model, child)
  }
}
