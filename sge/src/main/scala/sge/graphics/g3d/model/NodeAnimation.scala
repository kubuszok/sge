/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/NodeAnimation.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - Java null -> Nullable.empty for translation/rotation/scaling
 * - node field uses scala.compiletime.uninitialized (Java null default)
 * - Array -> DynamicArray is standard SGE collection mapping
 * - No API differences from Java source
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 44
 * Covenant-baseline-methods: NodeAnimation,node,rotation,scaling,translation
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/NodeAnimation.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package model

import sge.math.Quaternion
import sge.math.Vector3
import sge.utils.{ DynamicArray, Nullable }

/** A NodeAnimation defines keyframes for a {@link Node} in a {@link Model}. The keyframes are given as a translation vector, a rotation quaternion and a scale vector. Keyframes are interpolated
  * linearly for now. Keytimes are given in seconds.
  * @author
  *   badlogic, Xoppa
  */
class NodeAnimation {

  /** the Node affected by this animation * */
  var node: Node = scala.compiletime.uninitialized

  /** the translation keyframes if any (might be null), sorted by time ascending * */
  var translation: Nullable[DynamicArray[NodeKeyframe[Vector3]]] = Nullable.empty

  /** the rotation keyframes if any (might be null), sorted by time ascending * */
  var rotation: Nullable[DynamicArray[NodeKeyframe[Quaternion]]] = Nullable.empty

  /** the scaling keyframes if any (might be null), sorted by time ascending * */
  var scaling: Nullable[DynamicArray[NodeKeyframe[Vector3]]] = Nullable.empty
}
