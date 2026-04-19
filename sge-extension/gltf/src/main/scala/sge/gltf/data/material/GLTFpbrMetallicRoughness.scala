/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 21
 * Covenant-baseline-methods: GLTFpbrMetallicRoughness,baseColorFactor,baseColorTexture,metallicFactor,metallicRoughnessTexture,roughnessFactor
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package material

import sge.gltf.data.texture.GLTFTextureInfo
import sge.utils.Nullable

class GLTFpbrMetallicRoughness extends GLTFObject {
  var baseColorFactor:          Nullable[Array[Float]]    = Nullable.empty
  var metallicFactor:           Float                     = 1f
  var roughnessFactor:          Float                     = 1f
  var baseColorTexture:         Nullable[GLTFTextureInfo] = Nullable.empty
  var metallicRoughnessTexture: Nullable[GLTFTextureInfo] = Nullable.empty
}
