/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: GLTFAnimation,channels,samplers
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package animation

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

class GLTFAnimation extends GLTFEntity {
  var channels: Nullable[ArrayBuffer[GLTFAnimationChannel]] = Nullable.empty
  var samplers: Nullable[ArrayBuffer[GLTFAnimationSampler]] = Nullable.empty
}
