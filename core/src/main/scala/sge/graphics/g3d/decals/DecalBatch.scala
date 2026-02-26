/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/DecalBatch.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import sge.graphics.Mesh
import sge.graphics.VertexAttribute
import sge.graphics.VertexAttributes
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable
import sge.utils.Pool

/** <p> Renderer for {@link Decal} objects. </p> <p> New objects are added using {@link DecalBatch#add(Decal)}, there is no limit on how many decals can be added.<br/> Once all the decals have been
  * submitted a call to {@link DecalBatch#flush()} will batch them together and send big chunks of geometry to the GL. </p> <p> The size of the batch specifies the maximum number of decals that can be
  * batched together before they have to be submitted to the graphics pipeline. The default size is {@link DecalBatch#DEFAULT_SIZE}. If it is known before hand that not as many will be needed on
  * average the batch can be downsized to save memory. If the game is basically 3d based and decals will only be needed for an orthogonal HUD it makes sense to tune the size down. </p> <p> The way the
  * batch handles things depends on the {@link GroupStrategy}. Different strategies can be used to customize shaders, states, culling etc. for more details see the {@link GroupStrategy} java doc.<br/>
  * While it shouldn't be necessary to change strategies, if you have to do so, do it before calling {@link #add(Decal)}, and if you already did, call {@link #flush()} first. </p>
  */
class DecalBatch(size: Int, private var groupStrategy: GroupStrategy)(using sge: Sge) extends AutoCloseable {

  private var vertices: Array[Float] = scala.compiletime.uninitialized
  private var mesh:     Mesh         = scala.compiletime.uninitialized

  private val groupList: mutable.TreeMap[Int, ArrayBuffer[Decal]] = mutable.TreeMap.empty
  private val groupPool: Pool[ArrayBuffer[Decal]]                 = new Pool.Default[ArrayBuffer[Decal]](
    () => new ArrayBuffer[Decal](100),
    initialCapacity = 16
  )
  private val usedGroups: ArrayBuffer[ArrayBuffer[Decal]] = new ArrayBuffer[ArrayBuffer[Decal]](16)

  initialize(size)

  /** Creates a new DecalBatch using the given {@link GroupStrategy}. The most commong strategy to use is a {@link CameraGroupStrategy}
    * @param groupStrategy
    */
  def this(groupStrategy: GroupStrategy)(using sge: Sge) =
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

    val vertexDataType: Mesh.VertexDataType = if (sge.graphics.gl30.isDefined) {
      Mesh.VertexDataType.VertexBufferObjectWithVAO
    } else {
      Mesh.VertexDataType.VertexArray
    }
    mesh = new Mesh(
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
                                                  usedGroups += group
                                                  group
                                                }
    )
    targetGroup += decal
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
  private def render(shader: Nullable[ShaderProgram], decals: ArrayBuffer[Decal]): Unit = {
    // batch vertices
    var lastMaterial: Nullable[DecalMaterial] = Nullable.empty
    var idx = 0
    for (decal <- decals) {
      if (lastMaterial.isEmpty || !lastMaterial.getOrElse(null).equals(decal.getMaterial)) {
        if (idx > 0) {
          flush(shader, idx)
          idx = 0
        }
        decal.material.set()
        lastMaterial = Nullable(decal.material)
      }
      decal.update()
      System.arraycopy(decal.vertices, 0, vertices, idx, decal.vertices.length)
      idx += decal.vertices.length
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
      mesh.render(s, GL20.GL_TRIANGLES, 0, verticesPosition / 4)
    }
  }

  /** Remove all decals from batch */
  protected def clear(): Unit = {
    groupList.clear()
    groupPool.freeAll(usedGroups)
    usedGroups.clear()
  }

  /** Frees up memory by dropping the buffer and underlying resources. If the batch is needed again after disposing it can be {@link #initialize(int) initialized} again.
    */
  def close(): Unit = {
    clear()
    vertices = null
    mesh.close()
  }
}

object DecalBatch {
  final private val DEFAULT_SIZE = 1000
}
