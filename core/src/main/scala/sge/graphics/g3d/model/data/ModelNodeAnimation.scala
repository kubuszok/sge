/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeAnimation.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
