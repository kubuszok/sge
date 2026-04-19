/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 26
 * Covenant-baseline-methods: EXT,KHRMaterialsSpecular,specularColorFactor,specularColorTexture,specularFactor,specularTexture
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package extensions

import sge.gltf.data.texture.GLTFTextureInfo
import sge.utils.Nullable

/** [[sge.gltf.data.material.GLTFMaterial]] extension. See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_specular/README.md
  */
object KHRMaterialsSpecular {
  val EXT: String = "KHR_materials_specular"
}

class KHRMaterialsSpecular {
  var specularFactor:       Float                     = 1f
  var specularTexture:      Nullable[GLTFTextureInfo] = Nullable.empty
  var specularColorFactor:  Array[Float]              = Array(1f, 1f, 1f)
  var specularColorTexture: Nullable[GLTFTextureInfo] = Nullable.empty
}
