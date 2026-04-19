/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/CubicWeightVector.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: CubicWeightVector,tangentIn,tangentOut
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package model

class CubicWeightVector(count: Int) extends WeightVector(count) {

  val tangentIn:  WeightVector = WeightVector(count)
  val tangentOut: WeightVector = WeightVector(count)
}
