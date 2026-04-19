/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/DecalBatch.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 * - Disposable -> AutoCloseable, dispose() -> close(): correct
 * - SortedIntList<Array<Decal>> -> mutable.TreeMap[Int, DynamicArray[Decal]]:
 *   correct substitution (both provide sorted iteration by Int key)
 * - Pool anonymous subclass -> Pool.Default lambda: correct
 * - add(): SortedIntList.get/insert -> TreeMap.getOrElseUpdate: correct equivalent
 * - render(shader, decals): flush protected method wraps shader in Nullable.foreach,
 *   so render is skipped when shader is Nullable.empty; Java version passes null
 *   shader to mesh.render() which could NPE — Scala is safer, minor behavioral delta
 * - clear(): groupPool.freeAll(usedGroups) -> usedGroups.foreach(groupPool.free):
 *   correct (DynamicArray is not Iterable, so manual iteration needed)
 * - Constructor order: Scala primary constructor takes (size, groupStrategy), calls
 *   initialize(size); Java calls initialize then setGroupStrategy — equivalent
 * - Gdx.gl30 null check -> Sge().graphics.gl30.isDefined: correct
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 214
 * Covenant-baseline-methods: DEFAULT_SIZE,DecalBatch,add,clear,close,flush,getSize,groupIndex,groupList,groupPool,i,idx,indices,initialize,lastMaterial,mesh,render,setGroupStrategy,targetGroup,this,usedGroups,v,vertexDataType,vertices
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/decals/DecalBatch.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package decals

import scala.collection.mutable

import sge.graphics.{ Mesh, PrimitiveMode }
import sge.graphics.VertexAttribute
import sge.graphics.VertexAttributes
import sge.graphics.glutils.ShaderProgram
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.Pool

/** <p> Renderer for {@link Decal} objects. </p> <p> New objects are added using {@link DecalBatch#add(Decal)}, there is no limit on how many decals can be added.<br/> Once all the decals have been
  * submitted a call to {@link DecalBatch#flush()} will batch them together and send big chunks of geometry to the GL. </p> <p> The size of the batch specifies the maximum number of decals that can be
  * batched together before they have to be submitted to the graphics pipeline. The default size is {@link DecalBatch#DEFAULT_SIZE}. If it is known before hand that not as many will be needed on
  * average the batch can be downsized to save memory. If the game is basically 3d based and decals will only be needed for an orthogonal HUD it makes sense to tune the size down. </p> <p> The way the
  * batch handles things depends on the {@link GroupStrategy}. Different strategies can be used to customize shaders, states, culling etc. for more details see the {@link GroupStrategy} java doc.<br/>
  * While it shouldn't be necessary to change strategies, if you have to do so, do it before calling {@link #add(Decal)}, and if you already did, call {@link #flush()} first. </p>
  */
class DecalBatch(size: Int, private var groupStrategy: GroupStrategy)(using Sge) extends AutoCloseable {

  private var vertices: Array[Float] = scala.compiletime.uninitialized
  private var mesh:     Mesh         = scala.compiletime.uninitialized

  private val groupList: mutable.TreeMap[Int, DynamicArray[Decal]] = mutable.TreeMap.empty
  private val groupPool: Pool[DynamicArray[Decal]]                 = new Pool.Default[DynamicArray[Decal]](
    () => DynamicArray[Decal](100),
    initialCapacity = 16
  )
  private val usedGroups: DynamicArray[DynamicArray[Decal]] = DynamicArray[DynamicArray[Decal]](16)

  initialize(size)

  /** Creates a new DecalBatch using the given {@link GroupStrategy}. The most commong strategy to use is a {@link CameraGroupStrategy}
    * @param groupStrategy
    */
  def this(groupStrategy: GroupStrategy)(using Sge) =
    this(DecalBatch.DEFAULT_SIZE, groupStrategy)

  /** Sets the {@link GroupStrategy} used
    * @param groupStrategy
    *   Group strategy to use
    */
  def setGroupStrategy(groupStrategy: GroupStrategy): Unit =
    this.groupStrategy = groupStrategy

  /** Initializes the batch with the given amount of decal objects the buffer is able to hold when full.
    *
    * @param size
    *   Maximum size of decal objects to hold in memory
    */
  def initialize(size: Int): Unit = {
    vertices = new Array[Float](size * Decal.SIZE)

    val vertexDataType: Mesh.VertexDataType = if (Sge().graphics.gl30.isDefined) {
      Mesh.VertexDataType.VertexBufferObjectWithVAO
    } else {
      Mesh.VertexDataType.VertexBufferObject
    }
    mesh = Mesh(
      vertexDataType,
      false,
      size * 4,
      size * 6,
      VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
      VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
    )

    val indices = new Array[Short](size * 6)
    var v       = 0
    var i       = 0
    while (i < indices.length) {
      indices(i) = v.toShort
      indices(i + 1) = (v + 2).toShort
      indices(i + 2) = (v + 1).toShort
      indices(i + 3) = (v + 1).toShort
      indices(i + 4) = (v + 2).toShort
      indices(i + 5) = (v + 3).toShort
      i += 6
      v += 4
    }
    mesh.setIndices(indices)
  }

  /** @return maximum amount of decal objects this buffer can hold in memory */
  def getSize: Int = vertices.length / Decal.SIZE

  /** Add a decal to the batch, marking it for later rendering
    *
    * @param decal
    *   Decal to add for rendering
    */
  def add(decal: Decal): Unit = {
    val groupIndex  = groupStrategy.decideGroup(decal)
    val targetGroup = groupList.getOrElseUpdate(groupIndex, {
                                                  val group = groupPool.obtain()
                                                  group.clear()
                                                  usedGroups.add(group)
                                                  group
                                                }
    )
    targetGroup.add(decal)
  }

  /** Flush this batch sending all contained decals to GL. After flushing the batch is empty once again. */
  def flush(): Unit = {
    render()
    clear()
  }

  /** Renders all decals to the buffer and flushes the buffer to the GL when full/done */
  protected def render(): Unit = {
    groupStrategy.beforeGroups()
    for ((groupIndex, groupValue) <- groupList) {
      groupStrategy.beforeGroup(groupIndex, groupValue)
      val shader = groupStrategy.getGroupShader(groupIndex)
      render(shader, groupValue)
      groupStrategy.afterGroup(groupIndex)
    }
    groupStrategy.afterGroups()
  }

  /** Renders a group of vertices to the buffer, flushing them to GL when done/full
    *
    * @param decals
    *   Decals to render
    */
  private def render(shader: Nullable[ShaderProgram], decals: DynamicArray[Decal]): Unit = {
    // batch vertices
    var lastMaterial: Nullable[DecalMaterial] = Nullable.empty
    var idx = 0
    for (decal <- decals) {
      if (!lastMaterial.exists(_.equals(decal.material))) {
        if (idx > 0) {
          flush(shader, idx)
          idx = 0
        }
        decal.material.set()
        lastMaterial = Nullable(decal.material)
      }
      decal.update()
      System.arraycopy(decal._vertices, 0, vertices, idx, decal._vertices.length)
      idx += decal._vertices.length
      // if our batch is full we have to flush it
      if (idx == vertices.length) {
        flush(shader, idx)
        idx = 0
      }
    }
    // at the end if there is stuff left in the batch we render that
    if (idx > 0) {
      flush(shader, idx)
    }
  }

  /** Flushes vertices[0,verticesPosition[ to GL verticesPosition % Decal.SIZE must equal 0
    *
    * @param verticesPosition
    *   Amount of elements from the vertices array to flush
    */
  protected def flush(shader: Nullable[ShaderProgram], verticesPosition: Int): Unit = {
    mesh.setVertices(vertices, 0, verticesPosition)
    shader.foreach { s =>
      mesh.render(s, PrimitiveMode.Triangles, 0, verticesPosition / 4)
    }
  }

  /** Remove all decals from batch */
  protected def clear(): Unit = {
    groupList.clear()
    usedGroups.foreach(groupPool.free)
    usedGroups.clear()
  }

  /** Frees up memory by dropping the buffer and underlying resources. If the batch is needed again after disposing it can be {@link #initialize(int) initialized} again.
    */
  def close(): Unit = {
    clear()
    vertices = null // @nowarn — disposal: release reference for GC after close()
    mesh.close()
  }
}

object DecalBatch {
  final private val DEFAULT_SIZE = 1000
}
