/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/ShapeCache.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.{ GL20, Mesh, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.math.Matrix4
import sge.utils.{ DynamicArray, Nullable, Pool, SgeError }

/** A relatively lightweight class which can be used to render basic shapes which don't need a node structure and alike. Can be used for batching both static and dynamic shapes which share the same
  * {@link Material} and transformation {@link Matrix4} within the world. Use {@link ModelBatch} to render the `ShapeCache`. Must be disposed when no longer needed to release native resources. <p> How
  * to use it : </p>
  *
  * <pre> // Create cache ShapeCache cache = new ShapeCache(); // Build the cache, for dynamic shapes, this would be in the render method. MeshPartBuilder builder = cache.begin();
  * FrustumShapeBuilder.build(builder, camera); BoxShapeBuilder.build(builder, box); cache.end() // Render modelBatch.render(cache); // After using it cache.close(); </pre>
  *
  * @author
  *   realitix
  */
class ShapeCache(maxVertices: Int, maxIndices: Int, attributes: VertexAttributes, defaultPrimitiveType: Int)(using sge: Sge) extends AutoCloseable with RenderableProvider {

  /** Builder used to update the mesh */
  private val builder: MeshBuilder = new MeshBuilder()

  /** Mesh being rendered */
  private val mesh: Mesh = new Mesh(false, maxVertices, maxIndices, attributes)

  private var building:   Boolean    = false
  private val id:         String     = "id"
  private val renderable: Renderable = new Renderable()

  // Init renderable
  renderable.meshPart.mesh = mesh
  renderable.meshPart.primitiveType = defaultPrimitiveType
  renderable.material = Nullable(new Material())

  /** Create a ShapeCache with default values */
  def this()(using sge: Sge) = {
    this(
      5000,
      5000,
      new VertexAttributes(
        new VertexAttribute(Usage.Position, 3, "a_position"),
        new VertexAttribute(Usage.ColorPacked, 4, "a_color")
      ),
      GL20.GL_LINES
    )
  }

  /** Initialize ShapeCache for mesh generation with GL_LINES primitive type */
  def begin(): MeshPartBuilder =
    begin(GL20.GL_LINES)

  /** Initialize ShapeCache for mesh generation
    * @param primitiveType
    *   OpenGL primitive type
    */
  def begin(primitiveType: Int): MeshPartBuilder = {
    if (building) throw SgeError.InvalidInput("Call end() after calling begin()")
    building = true

    builder.begin(mesh.getVertexAttributes())
    builder.part(id, primitiveType, renderable.meshPart)
    builder
  }

  /** Generate mesh and renderable */
  def end(): Unit = {
    if (!building) throw SgeError.InvalidInput("Call begin() prior to calling end()")
    building = false

    builder.end(mesh)
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    renderables.add(renderable)

  /** Allows to customize the material.
    * @return
    *   material
    */
  def getMaterial(): Material = renderable.material.getOrElse(throw SgeError.InvalidInput("No material"))

  /** Allows to customize the world transform matrix.
    * @return
    *   world transform
    */
  def getWorldTransform(): Matrix4 = renderable.worldTransform

  override def close(): Unit =
    mesh.close()
}
