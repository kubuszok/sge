/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: EXT,KHRMaterialsPBRSpecularGlossiness,diffuseFactor,diffuseTexture,glossinessFactor,specularFactor,specularGlossinessTexture
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package extensions

import sge.gltf.data.texture.GLTFTextureInfo
import sge.utils.Nullable

/** [[sge.gltf.data.material.GLTFMaterial]] extension (deprecated now). See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Archived/KHR_materials_pbrSpecularGlossiness/README.md
  */
object KHRMaterialsPBRSpecularGlossiness {
  val EXT: String = "KHR_materials_pbrSpecularGlossiness"
}

class KHRMaterialsPBRSpecularGlossiness {
  var diffuseFactor:    Nullable[Array[Float]] = Nullable.empty
  var specularFactor:   Nullable[Array[Float]] = Nullable.empty
  var glossinessFactor: Float                  = 1f

  var diffuseTexture:            Nullable[GLTFTextureInfo] = Nullable.empty
  var specularGlossinessTexture: Nullable[GLTFTextureInfo] = Nullable.empty
}
