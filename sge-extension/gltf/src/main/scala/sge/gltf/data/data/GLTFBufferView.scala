/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 20
 * Covenant-baseline-methods: GLTFBufferView,buffer,byteLength,byteOffset,byteStride,target
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package data

import sge.utils.Nullable

class GLTFBufferView extends GLTFEntity {
  var byteOffset: Int           = 0
  var byteLength: Int           = 0
  var buffer:     Nullable[Int] = Nullable.empty
  var byteStride: Nullable[Int] = Nullable.empty
  var target:     Nullable[Int] = Nullable.empty
}
