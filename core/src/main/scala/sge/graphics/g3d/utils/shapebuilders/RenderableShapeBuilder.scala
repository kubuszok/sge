/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/RenderableShapeBuilder.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import scala.collection.mutable.ArrayBuffer

import sge.graphics.{ Color, Mesh }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.{ Renderable, RenderableProvider }
import sge.graphics.g3d.utils.MeshPartBuilder
import sge.utils.{ Nullable, Pool }

/** RenderableShapeBuilder builds various properties of a renderable.
  * @author
  *   realitix
  */
object RenderableShapeBuilder {
  import BaseShapeBuilder._

  private class RenderablePoolImpl extends Pool.Flushable[Renderable] {
    override protected val max:             Int = Int.MaxValue
    override protected val initialCapacity: Int = 16

    override protected def newObject(): Renderable = new Renderable()

    override def obtain(): Renderable = {
      val renderable = super.obtain()
      renderable.environment = Nullable.empty
      renderable.material = Nullable.empty
      renderable.meshPart.set(Nullable(""), null.asInstanceOf[Mesh], 0, 0, 0)
      renderable.shader = Nullable.empty
      renderable.userData = Nullable.empty
      renderable
    }
  }

  private val renderablesPool: RenderablePoolImpl = new RenderablePoolImpl()

  private var indices:     Array[Short]            = Array.empty[Short]
  private var vertices:    Array[Float]            = Array.empty[Float]
  private val renderables: ArrayBuffer[Renderable] = ArrayBuffer[Renderable]()
  final private val FLOAT_BYTES = 4

  /** Builds normal, tangent and binormal of a RenderableProvider with default colors (normal blue, tangent red, binormal green).
    * @param builder
    * @param renderableProvider
    * @param vectorSize
    *   Size of the normal vector
    */
  def buildNormals(builder: MeshPartBuilder, renderableProvider: RenderableProvider, vectorSize: Float): Unit =
    buildNormals(builder, renderableProvider, vectorSize, tmpColor0.set(0, 0, 1, 1), tmpColor1.set(1, 0, 0, 1), tmpColor2.set(0, 1, 0, 1))

  /** Builds normal, tangent and binormal of a RenderableProvider.
    * @param builder
    * @param renderableProvider
    * @param vectorSize
    *   Size of the normal vector
    * @param normalColor
    *   Normal vector's color
    * @param tangentColor
    *   Tangent vector's color
    * @param binormalColor
    *   Binormal vector's color
    */
  def buildNormals(builder: MeshPartBuilder, renderableProvider: RenderableProvider, vectorSize: Float, normalColor: Color, tangentColor: Color, binormalColor: Color): Unit = {

    renderableProvider.getRenderables(renderables, renderablesPool)

    for (renderable <- renderables)
      buildNormals(builder, renderable, vectorSize, normalColor, tangentColor, binormalColor)

    renderablesPool.flush()
    renderables.clear()
  }

  /** Builds normal, tangent and binormal of a Renderable.
    * @param builder
    * @param renderable
    * @param vectorSize
    *   Size of the normal vector
    * @param normalColor
    *   Normal vector's color
    * @param tangentColor
    *   Tangent vector's color
    * @param binormalColor
    *   Binormal vector's color
    */
  def buildNormals(builder: MeshPartBuilder, renderable: Renderable, vectorSize: Float, normalColor: Color, tangentColor: Color, binormalColor: Color): Unit = {
    val mesh = renderable.meshPart.mesh

    // Position
    var positionOffset = -1
    val posAttr        = mesh.getVertexAttribute(Usage.Position)
    if (posAttr.isDefined)
      positionOffset = posAttr.getOrElse(throw new RuntimeException).offset / FLOAT_BYTES

    // Normal
    var normalOffset = -1
    val norAttr      = mesh.getVertexAttribute(Usage.Normal)
    if (norAttr.isDefined)
      normalOffset = norAttr.getOrElse(throw new RuntimeException).offset / FLOAT_BYTES

    // Tangent
    var tangentOffset = -1
    val tanAttr       = mesh.getVertexAttribute(Usage.Tangent)
    if (tanAttr.isDefined)
      tangentOffset = tanAttr.getOrElse(throw new RuntimeException).offset / FLOAT_BYTES

    // Binormal
    var binormalOffset = -1
    val biNorAttr      = mesh.getVertexAttribute(Usage.BiNormal)
    if (biNorAttr.isDefined)
      binormalOffset = biNorAttr.getOrElse(throw new RuntimeException).offset / FLOAT_BYTES

    val attributesSize   = mesh.getVertexSize() / FLOAT_BYTES
    var verticesOffset   = 0
    var verticesQuantity = 0

    if (mesh.getNumIndices() > 0) {
      // Get min vertice to max vertice in indices array
      ensureIndicesCapacity(mesh.getNumIndices())
      mesh.getIndices(renderable.meshPart.offset, renderable.meshPart.size, indices, 0)

      val minVertice = minVerticeInIndices()
      val maxVertice = maxVerticeInIndices()

      verticesOffset = minVertice
      verticesQuantity = maxVertice - minVertice
    } else {
      verticesOffset = renderable.meshPart.offset
      verticesQuantity = renderable.meshPart.size
    }

    ensureVerticesCapacity(verticesQuantity * attributesSize)
    mesh.getVertices(verticesOffset * attributesSize, verticesQuantity * attributesSize, vertices, 0)

    for (i <- verticesOffset until verticesQuantity) {
      val id = i * attributesSize

      // Vertex position
      tmpV0.set(vertices(id + positionOffset), vertices(id + positionOffset + 1), vertices(id + positionOffset + 2))

      // Vertex normal, tangent, binormal
      if (normalOffset != -1) {
        tmpV1.set(vertices(id + normalOffset), vertices(id + normalOffset + 1), vertices(id + normalOffset + 2))
        tmpV2.set(tmpV0).add(tmpV1.scl(vectorSize))
      }

      if (tangentOffset != -1) {
        tmpV3.set(vertices(id + tangentOffset), vertices(id + tangentOffset + 1), vertices(id + tangentOffset + 2))
        tmpV4.set(tmpV0).add(tmpV3.scl(vectorSize))
      }

      if (binormalOffset != -1) {
        tmpV5.set(vertices(id + binormalOffset), vertices(id + binormalOffset + 1), vertices(id + binormalOffset + 2))
        tmpV6.set(tmpV0).add(tmpV5.scl(vectorSize))
      }

      // World transform
      tmpV0.mul(renderable.worldTransform)
      tmpV2.mul(renderable.worldTransform)
      tmpV4.mul(renderable.worldTransform)
      tmpV6.mul(renderable.worldTransform)

      // Draws normal, tangent, binormal
      if (normalOffset != -1) {
        builder.setColor(Nullable(normalColor))
        builder.line(tmpV0, tmpV2)
      }

      if (tangentOffset != -1) {
        builder.setColor(Nullable(tangentColor))
        builder.line(tmpV0, tmpV4)
      }

      if (binormalOffset != -1) {
        builder.setColor(Nullable(binormalColor))
        builder.line(tmpV0, tmpV6)
      }
    }
  }

  private def ensureVerticesCapacity(capacity: Int): Unit =
    if (vertices.length < capacity) vertices = new Array[Float](capacity)

  private def ensureIndicesCapacity(capacity: Int): Unit =
    if (indices.length < capacity) indices = new Array[Short](capacity)

  private def minVerticeInIndices(): Short = {
    var min: Short = Short.MaxValue
    for (i <- indices.indices)
      if (indices(i) < min) min = indices(i)
    min
  }

  private def maxVerticeInIndices(): Short = {
    var max: Short = Short.MinValue
    for (i <- indices.indices)
      if (indices(i) > max) max = indices(i)
    max
  }
}
