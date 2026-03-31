/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package extensions

import sge.gltf.data.texture.GLTFTextureInfo
import sge.utils.Nullable

/**
 * [[sge.gltf.data.material.GLTFMaterial]] extension.
 * See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_iridescence/README.md
 */
object KHRMaterialsIridescence {
  val EXT: String = "KHR_materials_iridescence"
}

class KHRMaterialsIridescence {
  var iridescenceFactor: Float = 0f
  var iridescenceTexture: Nullable[GLTFTextureInfo] = Nullable.empty
  var iridescenceIor: Float = 1.3f
  var iridescenceThicknessMinimum: Float = 100f
  var iridescenceThicknessMaximum: Float = 400f
  var iridescenceThicknessTexture: Nullable[GLTFTextureInfo] = Nullable.empty
}
