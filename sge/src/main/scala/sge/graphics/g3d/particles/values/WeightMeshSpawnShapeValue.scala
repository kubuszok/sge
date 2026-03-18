/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/WeightMeshSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - All public methods ported: init, calculateWeights, spawnAux, copy
 * - Java `mesh` accessed directly (protected field); Scala uses `mesh.getOrElse(...)` via Nullable
 * - Java uses `attributes.findByUsage(Usage.Position).offset`; Scala uses `.fold(throw ...)(_.offset)`
 * - Java casts vertexSize/positionOffset to short then stores as int; Scala stores as Int directly (correct)
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package values

import sge.graphics.VertexAttributes
import sge.math.{ CumulativeDistribution, MathUtils, Vector3 }
import sge.utils.{ Nullable, SgeError }

/** Encapsulate the formulas to spawn a particle on a mesh shape dealing with not uniform area triangles.
  * @author
  *   Inferno
  */
final class WeightMeshSpawnShapeValue extends MeshSpawnShapeValue {

  private var distribution: CumulativeDistribution[MeshSpawnShapeValue.Triangle] =
    CumulativeDistribution[MeshSpawnShapeValue.Triangle]()

  def this(value: WeightMeshSpawnShapeValue) = {
    this()
    distribution = CumulativeDistribution[MeshSpawnShapeValue.Triangle]()
    load(value)
  }

  override def init(): Unit =
    calculateWeights()

  /** Calculate the weights of each triangle of the wrapped mesh. If the mesh has indices: the function will calculate the weight of those triangles. If the mesh has not indices: the function will
    * consider the vertices as a triangle strip.
    */
  def calculateWeights(): Unit = {
    distribution.clear()
    val m              = mesh.getOrElse(throw SgeError.InvalidInput("mesh is null"))
    val attributes     = m.vertexAttributes
    val indicesCount   = m.numIndices
    val vertexCount    = m.numVertices
    val vertexSize     = attributes.vertexSize / 4
    val positionOffset =
      attributes.findByUsage(VertexAttributes.Usage.Position).fold(throw SgeError.InvalidInput("Mesh must have Usage.Position"))(_.offset / 4)
    val vertices = new Array[Float](vertexCount * vertexSize)
    m.getVertices(vertices)
    if (indicesCount > 0) {
      val indices = new Array[Short](indicesCount)
      m.getIndices(indices)

      // Calculate the Area
      var i = 0
      while (i < indicesCount) {
        val p1Offset = indices(i) * vertexSize + positionOffset
        val p2Offset = indices(i + 1) * vertexSize + positionOffset
        val p3Offset = indices(i + 2) * vertexSize + positionOffset
        val x1       = vertices(p1Offset); val y1 = vertices(p1Offset + 1); val z1 = vertices(p1Offset + 2)
        val x2       = vertices(p2Offset); val y2 = vertices(p2Offset + 1); val z2 = vertices(p2Offset + 2)
        val x3       = vertices(p3Offset); val y3 = vertices(p3Offset + 1); val z3 = vertices(p3Offset + 2)
        val area     = Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2f)
        distribution.add(
          MeshSpawnShapeValue.Triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3),
          area
        )
        i += 3
      }
    } else {
      // Calculate the Area
      var i = 0
      while (i < vertexCount) {
        val p1Offset = i + positionOffset
        val p2Offset = p1Offset + vertexSize
        val p3Offset = p2Offset + vertexSize
        val x1       = vertices(p1Offset); val y1 = vertices(p1Offset + 1); val z1 = vertices(p1Offset + 2)
        val x2       = vertices(p2Offset); val y2 = vertices(p2Offset + 1); val z2 = vertices(p2Offset + 2)
        val x3       = vertices(p3Offset); val y3 = vertices(p3Offset + 1); val z3 = vertices(p3Offset + 2)
        val area     = Math.abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2f)
        distribution.add(
          MeshSpawnShapeValue.Triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3),
          area
        )
        i += vertexSize
      }
    }

    // Generate cumulative distribution
    distribution.generateNormalized()
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = {
    val t = distribution.value()
    val a = MathUtils.random()
    val b = MathUtils.random()
    vector.set(
      t.x1 + a * (t.x2 - t.x1) + b * (t.x3 - t.x1),
      t.y1 + a * (t.y2 - t.y1) + b * (t.y3 - t.y1),
      t.z1 + a * (t.z2 - t.z1) + b * (t.z3 - t.z1)
    )
  }

  override def copy(): SpawnShapeValue =
    WeightMeshSpawnShapeValue(this)
}
