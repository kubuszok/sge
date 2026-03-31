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

/** [[sge.gltf.data.material.GLTFMaterial]] extension. See https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_ior/README.md
  */
object KHRMaterialsIOR {
  val EXT: String = "KHR_materials_ior"
}

class KHRMaterialsIOR {
  var ior: Float = 1.5f
}
