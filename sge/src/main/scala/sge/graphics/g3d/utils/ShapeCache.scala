/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/ShapeCache.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - implements Disposable, RenderableProvider -> extends AutoCloseable with RenderableProvider
 *   - dispose() -> close()
 *   - Array<Renderable> -> DynamicArray[Renderable] in getRenderables
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - renderable.material = new Material() -> renderable.material = Nullable(new Material())
 *   - getMaterial() returns via Nullable fold (material is Nullable in Renderable)
 *   - Convention: typed GL enums (PrimitiveMode)
 *   - All methods (begin, end, getRenderables, getMaterial, getWorldTransform, close) ported
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 120
 * Covenant-baseline-methods: ShapeCache,b,build,builder,building,close,getRenderables,id,material,mesh,renderable,this,worldTransform
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/ShapeCache.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.{ Mesh, PrimitiveMode, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.math.Matrix4
import sge.utils.{ DynamicArray, Nullable, Pool, SgeError }

import scala.annotation.publicInBinary

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
class ShapeCache(maxVertices: Int, maxIndices: Int, attributes: VertexAttributes, defaultPrimitiveType: PrimitiveMode)(using Sge) extends AutoCloseable with RenderableProvider {

  /** Builder used to update the mesh */
  private val builder: MeshBuilder = MeshBuilder()

  /** Mesh being rendered */
  private val mesh: Mesh = Mesh(false, maxVertices, maxIndices, attributes)

  private var building:   Boolean    = false
  private val id:         String     = "id"
  private val renderable: Renderable = Renderable()

  // Init renderable
  renderable.meshPart.mesh = mesh
  renderable.meshPart.primitiveType = defaultPrimitiveType
  renderable.material = Nullable(Material())

  /** Create a ShapeCache with default values */
  def this()(using Sge) =
    this(
      5000,
      5000,
      VertexAttributes(
        VertexAttribute(Usage.Position, 3, "a_position"),
        VertexAttribute(Usage.ColorPacked, 4, "a_color")
      ),
      PrimitiveMode.Lines
    )

  /** Initialize ShapeCache for mesh generation with GL_LINES primitive type */
  @publicInBinary private[sge] def begin(): MeshPartBuilder =
    begin(PrimitiveMode.Lines)

  /** Initialize ShapeCache for mesh generation
    * @param primitiveType
    *   OpenGL primitive type
    */
  @publicInBinary private[sge] def begin(primitiveType: PrimitiveMode): MeshPartBuilder = {
    if (building) throw SgeError.InvalidInput("Call end() after calling begin()")
    building = true

    builder.begin(mesh.vertexAttributes)
    builder.part(id, primitiveType, renderable.meshPart)
    builder
  }

  /** Generate mesh and renderable */
  @publicInBinary private[sge] def end(): Unit = {
    if (!building) throw SgeError.InvalidInput("Call begin() prior to calling end()")
    building = false

    builder.end(mesh)
  }

  /** Executes `body` with the [[MeshPartBuilder]] from [[begin]], ensuring [[end]] is called even if `body` throws. */
  inline def build[A](inline body: MeshPartBuilder => A): A = {
    val b = begin()
    try body(b)
    finally end()
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    renderables.add(renderable)

  /** Allows to customize the material.
    * @return
    *   material
    */
  def material: Material = renderable.material.getOrElse(throw SgeError.InvalidInput("No material"))

  /** Allows to customize the world transform matrix.
    * @return
    *   world transform
    */
  def worldTransform: Matrix4 = renderable.worldTransform

  override def close(): Unit =
    mesh.close()
}
