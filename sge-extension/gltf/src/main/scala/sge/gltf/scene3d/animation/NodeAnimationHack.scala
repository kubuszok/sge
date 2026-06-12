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
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: NodeAnimationHack,rotationMode,scalingCubic,scalingMode,translationCubic,translationMode,weights,weightsMode
 * Covenant-source-reference: net/mgsx/gltf/scene3d/animation/NodeAnimationHack.java
 * Covenant-verified: 2026-06-12
 */
package sge
package gltf
package scene3d
package animation

import sge.gltf.scene3d.model.{ CubicVector3, WeightVector }
import sge.graphics.g3d.model.{ NodeAnimation, NodeKeyframe }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class NodeAnimationHack extends NodeAnimation {

  var translationMode: Nullable[Interpolation] = Nullable.empty
  var rotationMode:    Nullable[Interpolation] = Nullable.empty
  var scalingMode:     Nullable[Interpolation] = Nullable.empty
  var weightsMode:     Nullable[Interpolation] = Nullable.empty

  var weights: DynamicArray[NodeKeyframe[WeightVector]] = scala.compiletime.uninitialized // @nowarn — null when no morph targets

  /** Cubic-spline tangent data for translation/scaling keyframes, index-aligned with the inherited NodeKeyframe[Vector3] translation/scaling arrays. In upstream gdx-gltf the keyframe value itself is
    * a CubicVector3 (CubicVector3 extends Vector3); SGE's Vector3 is a final case class, so the cubic objects are kept here instead and recovered by index during CUBICSPLINE evaluation. Empty for
    * non-cubic channels.
    */
  var translationCubic: Nullable[DynamicArray[CubicVector3]] = Nullable.empty
  var scalingCubic:     Nullable[DynamicArray[CubicVector3]] = Nullable.empty
}
