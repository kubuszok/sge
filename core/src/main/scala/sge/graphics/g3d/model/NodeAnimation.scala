/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/NodeAnimation.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package model

import scala.collection.mutable.ArrayBuffer

import sge.math.Quaternion
import sge.math.Vector3
import sge.utils.Nullable

/** A NodeAnimation defines keyframes for a {@link Node} in a {@link Model}. The keyframes are given as a translation vector, a rotation quaternion and a scale vector. Keyframes are interpolated
  * linearly for now. Keytimes are given in seconds.
  * @author
  *   badlogic, Xoppa
  */
class NodeAnimation {

  /** the Node affected by this animation * */
  var node: Node = scala.compiletime.uninitialized

  /** the translation keyframes if any (might be null), sorted by time ascending * */
  var translation: Nullable[ArrayBuffer[NodeKeyframe[Vector3]]] = Nullable.empty

  /** the rotation keyframes if any (might be null), sorted by time ascending * */
  var rotation: Nullable[ArrayBuffer[NodeKeyframe[Quaternion]]] = Nullable.empty

  /** the scaling keyframes if any (might be null), sorted by time ascending * */
  var scaling: Nullable[ArrayBuffer[NodeKeyframe[Vector3]]] = Nullable.empty
}
