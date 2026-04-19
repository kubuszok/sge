/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNode.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - All fields use scala.compiletime.uninitialized (Java null defaults)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: ModelNode,children,id,meshId,parts,rotation,scale,translation
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelNode.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

import sge.math.Quaternion
import sge.math.Vector3

class ModelNode {
  var id:          String               = scala.compiletime.uninitialized
  var translation: Vector3              = scala.compiletime.uninitialized
  var rotation:    Quaternion           = scala.compiletime.uninitialized
  var scale:       Vector3              = scala.compiletime.uninitialized
  var meshId:      String               = scala.compiletime.uninitialized
  var parts:       Array[ModelNodePart] = scala.compiletime.uninitialized
  var children:    Array[ModelNode]     = scala.compiletime.uninitialized
}
