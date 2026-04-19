/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/animation/NodeAnimationHack.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * NodeAnimation hack to store morph targets weights
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: NodeAnimationHack,rotationMode,scalingMode,translationMode,weights,weightsMode
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package animation

import sge.gltf.scene3d.model.WeightVector
import sge.graphics.g3d.model.{ NodeAnimation, NodeKeyframe }
import sge.utils.{ DynamicArray, Nullable }

class NodeAnimationHack extends NodeAnimation {

  var translationMode: Nullable[Interpolation] = Nullable.empty
  var rotationMode:    Nullable[Interpolation] = Nullable.empty
  var scalingMode:     Nullable[Interpolation] = Nullable.empty
  var weightsMode:     Nullable[Interpolation] = Nullable.empty

  var weights: DynamicArray[NodeKeyframe[WeightVector]] = scala.compiletime.uninitialized // @nowarn — null when no morph targets
}
