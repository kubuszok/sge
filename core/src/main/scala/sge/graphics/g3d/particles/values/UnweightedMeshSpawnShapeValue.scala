/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/UnweightedMeshSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - All public methods ported: setMesh, spawnAux, copy
 * - Java `indices` is bare null; Scala uses Nullable[Array[Short]] (correct pattern)
 * - spawnAux logic: Java uses `if (indices == null)` for no-index path first;
 *   Scala uses `indices.fold { noIndex } { withIndex }` -- fold reverses the branch order
 *   but logic is equivalent (noIndex is the "empty/null" case)
 * - Java `triangleCount = indices.length / 3`; Scala correctly uses `indicesCount / 3`
 *   after assigning to `idx` local
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package values

import sge.graphics.{ Mesh, VertexAttributes }
import sge.graphics.g3d.Model
import sge.math.{ MathUtils, Vector3 }
import sge.utils.{ Nullable, SgeError }

/** Encapsulate the formulas to spawn a particle on a mesh shape.
  * @author
  *   Inferno
  */
final class UnweightedMeshSpawnShapeValue extends MeshSpawnShapeValue {
  private var vertices:       Array[Float]           = Array.empty
  private var indices:        Nullable[Array[Short]] = Nullable.empty
  private var positionOffset: Int                    = 0
  private var vertexSize:     Int                    = 0
  private var vertexCount:    Int                    = 0
  private var triangleCount:  Int                    = 0

  def this(value: UnweightedMeshSpawnShapeValue) = {
    this()
    load(value)
  }

  override def setMesh(mesh: Mesh, model: Nullable[Model]): Unit = {
    super.setMesh(mesh, model)
    vertexSize = mesh.getVertexSize() / 4
    positionOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).fold(throw SgeError.InvalidInput("Mesh must have Usage.Position"))(_.offset / 4)
    val indicesCount = mesh.getNumIndices()
    if (indicesCount > 0) {
      val idx = new Array[Short](indicesCount)
      mesh.getIndices(idx)
      indices = Nullable(idx)
      triangleCount = indicesCount / 3
    } else {
      indices = Nullable.empty
    }
    vertexCount = mesh.getNumVertices()
    vertices = new Array[Float](vertexCount * vertexSize)
    mesh.getVertices(vertices)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit =
    indices.fold {
      // Triangles
      val triangleIndex = MathUtils.random(vertexCount - 3) * vertexSize
      val p1Offset      = triangleIndex + positionOffset
      val p2Offset      = p1Offset + vertexSize
      val p3Offset      = p2Offset + vertexSize
      val x1            = vertices(p1Offset); val y1 = vertices(p1Offset + 1); val z1 = vertices(p1Offset + 2)
      val x2            = vertices(p2Offset); val y2 = vertices(p2Offset + 1); val z2 = vertices(p2Offset + 2)
      val x3            = vertices(p3Offset); val y3 = vertices(p3Offset + 1); val z3 = vertices(p3Offset + 2)
      MeshSpawnShapeValue.Triangle.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector)
    } { idx =>
      // Indices
      val triangleIndex = MathUtils.random(triangleCount - 1) * 3
      val p1Offset      = idx(triangleIndex) * vertexSize + positionOffset
      val p2Offset      = idx(triangleIndex + 1) * vertexSize + positionOffset
      val p3Offset      = idx(triangleIndex + 2) * vertexSize + positionOffset
      val x1            = vertices(p1Offset); val y1 = vertices(p1Offset + 1); val z1 = vertices(p1Offset + 2)
      val x2            = vertices(p2Offset); val y2 = vertices(p2Offset + 1); val z2 = vertices(p2Offset + 2)
      val x3            = vertices(p3Offset); val y3 = vertices(p3Offset + 1); val z3 = vertices(p3Offset + 2)
      MeshSpawnShapeValue.Triangle.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector)
    }

  override def copy(): SpawnShapeValue =
    new UnweightedMeshSpawnShapeValue(this)
}
