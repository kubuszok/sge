/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/ModelCache.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Disposable -> AutoCloseable (dispose -> close).
 *   - FlushablePool -> Pool.Flushable.
 *   - Inner types MeshPool, SimpleMeshPool, TightMeshPool, Sorter all in companion object.
 *   - Constructor requires (using Sge) for Mesh creation.
 *   - end(): uses boundary/break for early return instead of bare return.
 *   - Sorter.compare: material accessed via .getOrElse with error throw (Java direct access).
 *   - meshBuilder.setVertexTransform wrapped in Nullable.
 *   - All public methods present: begin (2), end, add (3), getRenderables, close.
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.{ Camera, Mesh, PrimitiveMode, VertexAttributes }
import sge.graphics.g3d.model.MeshPart
import sge.graphics.g3d.utils.{ MeshBuilder, RenderableSorter }
import sge.utils.{ DynamicArray, Nullable, Pool, SgeError }

/** ModelCache tries to combine multiple render calls into a single render call by merging them where possible. Can be used for multiple type of models (e.g. varying vertex attributes or materials),
  * the ModelCache will combine where possible. Can be used dynamically (e.g. every frame) or statically (e.g. to combine part of scenery). Be aware that any combined vertices are directly
  * transformed, therefore the resulting {@link Renderable#worldTransform} might not be suitable for sorting anymore (such as the default sorter of ModelBatch does).
  * @author
  *   Xoppa
  */
class ModelCache(sorter: RenderableSorter, meshPool: ModelCache.MeshPool)(using Sge) extends AutoCloseable with RenderableProvider {

  private val renderables:     DynamicArray[Renderable]   = DynamicArray[Renderable]()
  private val renderablesPool: Pool.Flushable[Renderable] = new Pool.Flushable[Renderable] {
    override protected val max:             Int        = Int.MaxValue
    override protected val initialCapacity: Int        = 16
    override def newObject():               Renderable = Renderable()
  }
  private val meshPartPool: Pool.Flushable[MeshPart] = new Pool.Flushable[MeshPart] {
    override protected val max:             Int      = Int.MaxValue
    override protected val initialCapacity: Int      = 16
    override def newObject():               MeshPart = MeshPart()
  }

  private val items: DynamicArray[Renderable] = DynamicArray[Renderable]()
  private val tmp:   DynamicArray[Renderable] = DynamicArray[Renderable]()

  private val meshBuilder: MeshBuilder      = MeshBuilder()
  private var building:    Boolean          = false
  private var camera:      Nullable[Camera] = Nullable.empty

  /** Create a ModelCache using the default {@link Sorter} and the {@link SimpleMeshPool} implementation. This might not be the most optimal implementation for you use-case, but should be good to
    * start with.
    */
  def this()(using Sge) =
    this(ModelCache.Sorter(), ModelCache.SimpleMeshPool())

  /** Begin creating the cache, must be followed by a call to {@link #end()}, in between these calls one or more calls to one of the add(...) methods can be made. Calling this method will clear the
    * cache and prepare it for creating a new cache. The cache is not valid until the call to {@link #end()} is made. Use one of the add methods (e.g. {@link #add(Renderable)} or
    * {@link #add(RenderableProvider)}) to add renderables to the cache.
    */
  def begin(): Unit =
    begin(Nullable.empty)

  /** Begin creating the cache, must be followed by a call to {@link #end()}, in between these calls one or more calls to one of the add(...) methods can be made. Calling this method will clear the
    * cache and prepare it for creating a new cache. The cache is not valid until the call to {@link #end()} is made. Use one of the add methods (e.g. {@link #add(Renderable)} or
    * {@link #add(RenderableProvider)}) to add renderables to the cache.
    * @param camera
    *   The {@link Camera} that will passed to the {@link RenderableSorter}
    */
  def begin(camera: Nullable[Camera]): Unit = {
    if (building) throw SgeError.InvalidInput("Call end() after calling begin()")
    building = true

    this.camera = camera
    renderablesPool.flush()
    renderables.clear()
    items.clear()
    meshPartPool.flush()
    meshPool.flush()
  }

  private def obtainRenderable(material: Nullable[Material], primitiveType: PrimitiveMode): Renderable = {
    val result = renderablesPool.obtain()
    result.bones = Nullable.empty
    result.environment = Nullable.empty
    result.material = material
    result.meshPart.mesh = null.asInstanceOf[Mesh] // pool reset: mesh is always set before use in end()
    result.meshPart.offset = 0
    result.meshPart.size = 0
    result.meshPart.primitiveType = primitiveType
    result.meshPart.center.set(0, 0, 0)
    result.meshPart.halfExtents.set(0, 0, 0)
    result.meshPart.radius = -1f
    result.shader = Nullable.empty
    result.userData = Nullable.empty
    result.worldTransform.idt()
    result
  }

  /** Finishes creating the cache, must be called after a call to {@link #begin()}, only after this call the cache will be valid (until the next call to {@link #begin()}). Calling this method will
    * process all renderables added using one of the add(...) methods and will combine them if possible.
    */
  def end(): Unit = boundary {
    if (!building) throw SgeError.InvalidInput("Call begin() prior to calling end()")
    building = false

    if (items.isEmpty) break(())
    sorter.sort(camera, items)

    val first            = items(0)
    var vertexAttributes = first.meshPart.mesh.getVertexAttributes()
    var material         = first.material
    var primitiveType    = first.meshPart.primitiveType
    var offset           = renderables.size

    meshBuilder.begin(vertexAttributes)
    var part = meshBuilder.part("", primitiveType, meshPartPool.obtain())
    renderables.add(obtainRenderable(material, primitiveType))

    var i = 0
    val n = items.size
    while (i < n) {
      val renderable = items(i)
      val va         = renderable.meshPart.mesh.getVertexAttributes()
      val mat        = renderable.material
      val pt         = renderable.meshPart.primitiveType

      val sameAttributes = va.equals(vertexAttributes)
      val indexedMesh    =
        renderable.meshPart.mesh.getNumIndices() > 0
      val verticesToAdd =
        if (indexedMesh)
          renderable.meshPart.mesh.getNumVertices()
        else renderable.meshPart.size
      val canHoldVertices = meshBuilder.getNumVertices() + verticesToAdd <= MeshBuilder.MAX_VERTICES
      val sameMesh        = sameAttributes && canHoldVertices
      val samePart = sameMesh && pt == primitiveType && mat.getOrElse(throw SgeError.InvalidInput("Material is null")).same(material.getOrElse(throw SgeError.InvalidInput("Material is null")), true)

      if (!samePart) {
        if (!sameMesh) {
          val mesh = meshBuilder.end(meshPool.obtain(vertexAttributes, meshBuilder.getNumVertices(), meshBuilder.getNumIndices()))
          while (offset < renderables.size) {
            renderables(offset).meshPart.mesh = mesh
            offset += 1
          }
          vertexAttributes = va
          meshBuilder.begin(vertexAttributes)
        }

        val newPart  = meshBuilder.part("", pt, meshPartPool.obtain())
        val previous = renderables(renderables.size - 1)
        previous.meshPart.offset = part.offset
        previous.meshPart.size = part.size
        part = newPart

        material = mat
        primitiveType = pt
        renderables.add(obtainRenderable(material, primitiveType))
      }

      meshBuilder.setVertexTransform(Nullable(renderable.worldTransform))
      meshBuilder.addMesh(
        renderable.meshPart.mesh,
        renderable.meshPart.offset,
        renderable.meshPart.size
      )
      i += 1
    }

    val mesh = meshBuilder.end(meshPool.obtain(vertexAttributes, meshBuilder.getNumVertices(), meshBuilder.getNumIndices()))
    while (offset < renderables.size) {
      renderables(offset).meshPart.mesh = mesh
      offset += 1
    }

    val previous = renderables(renderables.size - 1)
    previous.meshPart.offset = part.offset
    previous.meshPart.size = part.size
  }

  /** Adds the specified {@link Renderable} to the cache. Must be called in between a call to {@link #begin()} and {@link #end()}. All member objects might (depending on possibilities) be used by
    * reference and should not change while the cache is used. If the {@link Renderable#bones} member is not null then skinning is assumed and the renderable will be added as-is, by reference.
    * Otherwise the renderable will be merged with other renderables as much as possible, depending on the {@link Mesh#getVertexAttributes()}, {@link Renderable#material} and primitiveType (in that
    * order). The {@link Renderable#environment}, {@link Renderable#shader} and {@link Renderable#userData} values (if any) are removed.
    * @param renderable
    *   The {@link Renderable} to add, should not change while the cache is needed.
    */
  def add(renderable: Renderable): Unit = {
    if (!building) throw SgeError.InvalidInput("Can only add items to the ModelCache in between .begin() and .end()")
    if (renderable.bones.isEmpty)
      items.add(renderable)
    else
      renderables.add(renderable)
  }

  /** Adds the specified {@link RenderableProvider} to the cache, see {@link #add(Renderable)}. */
  def add(renderableProvider: RenderableProvider): Unit = {
    renderableProvider.getRenderables(tmp, renderablesPool)
    var i = 0
    val n = tmp.size
    while (i < n) {
      add(tmp(i))
      i += 1
    }
    tmp.clear()
  }

  /** Adds the specified {@link RenderableProvider}s to the cache, see {@link #add(Renderable)}. */
  def add[T <: RenderableProvider](renderableProviders: Iterable[T]): Unit =
    for (renderableProvider <- renderableProviders)
      add(renderableProvider)

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit = {
    if (building) throw SgeError.InvalidInput("Cannot render a ModelCache in between .begin() and .end()")
    for (r <- this.renderables) {
      r.shader = Nullable.empty
      r.environment = Nullable.empty
    }
    renderables.addAll(this.renderables)
  }

  override def close(): Unit = {
    if (building) throw SgeError.InvalidInput("Cannot dispose a ModelCache in between .begin() and .end()")
    meshPool.close()
  }
}

object ModelCache {

  /** Allows to reuse one or more meshes while avoiding creating new objects. Depending on the implementation it might add memory optimizations as well. Call the
    * {@link #obtain(VertexAttributes, int, int)} method to obtain a mesh which can at minimum the specified amount of vertices and indices. Call the {@link #flush()} method to flush the pool ant
    * release all previously obtained meshes.
    */
  trait MeshPool extends AutoCloseable {

    /** Will try to reuse or, when not possible to reuse, optionally create a {@link Mesh} that meets the specified criteria.
      * @param vertexAttributes
      *   the vertex attributes of the mesh to obtain
      * @param vertexCount
      *   the minimum amount vertices the mesh should be able to store
      * @param indexCount
      *   the minimum amount of indices the mesh should be able to store
      * @return
      *   the obtained Mesh, or null when no mesh could be obtained.
      */
    def obtain(vertexAttributes: VertexAttributes, vertexCount: Int, indexCount: Int): Mesh

    /** Releases all previously obtained {@link Mesh}es using the the {@link #obtain(VertexAttributes, int, int)} method. */
    def flush(): Unit
  }

  /** A basic {@link MeshPool} implementation that avoids creating new meshes at the cost of memory usage. It does this by making the mesh always the maximum (64k) size. Use this when for dynamic
    * caching where you need to obtain meshes very frequently (typically every frame).
    * @author
    *   Xoppa
    */
  class SimpleMeshPool(using Sge) extends MeshPool {
    // FIXME Make a better (preferable JNI) MeshPool implementation
    private val freeMeshes: DynamicArray[Mesh] = DynamicArray[Mesh]()
    private val usedMeshes: DynamicArray[Mesh] = DynamicArray[Mesh]()

    override def flush(): Unit = {
      freeMeshes.addAll(usedMeshes)
      usedMeshes.clear()
    }

    override def obtain(vertexAttributes: VertexAttributes, vertexCount: Int, indexCount: Int): Mesh =
      boundary {
        var i = 0
        val n = freeMeshes.size
        while (i < n) {
          val mesh = freeMeshes(i)
          if (
            mesh.getVertexAttributes().equals(vertexAttributes) && mesh.getMaxVertices() >= vertexCount
            && mesh.getMaxIndices() >= indexCount
          ) {
            freeMeshes.removeIndex(i)
            usedMeshes.add(mesh)
            break(mesh)
          }
          i += 1
        }
        val vc     = MeshBuilder.MAX_VERTICES
        val ic     = Math.max(vc, 1 << (32 - Integer.numberOfLeadingZeros(indexCount - 1)))
        val result = Mesh(false, vc, ic, vertexAttributes)
        usedMeshes.add(result)
        result
      }

    override def close(): Unit = {
      for (m <- usedMeshes)
        m.close()
      usedMeshes.clear()
      for (m <- freeMeshes)
        m.close()
      freeMeshes.clear()
    }
  }

  /** A tight {@link MeshPool} implementation, which is typically used for static meshes (create once, use many).
    * @author
    *   Xoppa
    */
  class TightMeshPool(using Sge) extends MeshPool {
    private val freeMeshes: DynamicArray[Mesh] = DynamicArray[Mesh]()
    private val usedMeshes: DynamicArray[Mesh] = DynamicArray[Mesh]()

    override def flush(): Unit = {
      freeMeshes.addAll(usedMeshes)
      usedMeshes.clear()
    }

    override def obtain(vertexAttributes: VertexAttributes, vertexCount: Int, indexCount: Int): Mesh =
      boundary {
        var i = 0
        val n = freeMeshes.size
        while (i < n) {
          val mesh = freeMeshes(i)
          if (
            mesh.getVertexAttributes().equals(vertexAttributes) && mesh.getMaxVertices() == vertexCount
            && mesh.getMaxIndices() == indexCount
          ) {
            freeMeshes.removeIndex(i)
            usedMeshes.add(mesh)
            break(mesh)
          }
          i += 1
        }
        val result = Mesh(true, vertexCount, indexCount, vertexAttributes)
        usedMeshes.add(result)
        result
      }

    override def close(): Unit = {
      for (m <- usedMeshes)
        m.close()
      usedMeshes.clear()
      for (m <- freeMeshes)
        m.close()
      freeMeshes.clear()
    }
  }

  /** A {@link RenderableSorter} that sorts by vertex attributes, material attributes and primitive types (in that order), so that meshes can be easily merged.
    * @author
    *   Xoppa
    */
  class Sorter extends RenderableSorter with Ordering[Renderable] {
    override def sort(camera: Nullable[Camera], renderables: DynamicArray[Renderable]): Unit =
      renderables.sort()(using this)

    override def compare(arg0: Renderable, arg1: Renderable): Int = {
      val va0 = arg0.meshPart.mesh.getVertexAttributes()
      val va1 = arg1.meshPart.mesh.getVertexAttributes()
      val vc  = va0.compareTo(va1)
      if (vc == 0) {
        val mc = arg0.material.getOrElse(throw SgeError.InvalidInput("Material is null")).compareTo(arg1.material.getOrElse(throw SgeError.InvalidInput("Material is null")))
        if (mc == 0) {
          arg0.meshPart.primitiveType.toInt - arg1.meshPart.primitiveType.toInt
        } else mc
      } else vc
    }
  }
}
