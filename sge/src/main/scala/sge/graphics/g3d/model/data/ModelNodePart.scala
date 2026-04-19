/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodePart.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - All fields use scala.compiletime.uninitialized (Java null defaults)
 * - ArrayMap[String, Matrix4] matches Java ArrayMap<String, Matrix4>
 * - Array[Array[Int]] matches Java int[][] for uvMapping
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: ModelNodePart,bones,materialId,meshPartId,uvMapping
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelNodePart.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

import sge.math.Matrix4
import sge.utils.ArrayMap

class ModelNodePart {
  var materialId: String                    = scala.compiletime.uninitialized
  var meshPartId: String                    = scala.compiletime.uninitialized
  var bones:      ArrayMap[String, Matrix4] = scala.compiletime.uninitialized
  var uvMapping:  Array[Array[Int]]         = scala.compiletime.uninitialized
}
