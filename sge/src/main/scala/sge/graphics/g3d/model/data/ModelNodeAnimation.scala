/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeAnimation.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - Array -> DynamicArray for translation/rotation/scaling
 * - FIXME comment preserved from Java source
 * - All fields use scala.compiletime.uninitialized (Java null defaults)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 40
 * Covenant-baseline-methods: ModelNodeAnimation,nodeId,rotation,scaling,translation
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeAnimation.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

import sge.math.Quaternion
import sge.math.Vector3
import sge.utils.DynamicArray

class ModelNodeAnimation {

  /** the id of the node animated by this animation FIXME should be nodeId * */
  var nodeId: String = scala.compiletime.uninitialized

  /** the keyframes, defining the translation of a node for a specific timestamp * */
  var translation: DynamicArray[ModelNodeKeyframe[Vector3]] = scala.compiletime.uninitialized

  /** the keyframes, defining the rotation of a node for a specific timestamp * */
  var rotation: DynamicArray[ModelNodeKeyframe[Quaternion]] = scala.compiletime.uninitialized

  /** the keyframes, defining the scaling of a node for a specific timestamp * */
  var scaling: DynamicArray[ModelNodeKeyframe[Vector3]] = scala.compiletime.uninitialized
}
