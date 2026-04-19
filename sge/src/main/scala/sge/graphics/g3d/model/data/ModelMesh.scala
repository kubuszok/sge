/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelMesh.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - Java VertexAttribute[] -> Array[VertexAttribute] (import resolved via split package sge.graphics)
 * - All fields use scala.compiletime.uninitialized (Java null defaults)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: ModelMesh,attributes,id,parts,vertices
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelMesh.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

class ModelMesh {
  var id:         String                 = scala.compiletime.uninitialized
  var attributes: Array[VertexAttribute] = scala.compiletime.uninitialized
  var vertices:   Array[Float]           = scala.compiletime.uninitialized
  var parts:      Array[ModelMeshPart]   = scala.compiletime.uninitialized
}
