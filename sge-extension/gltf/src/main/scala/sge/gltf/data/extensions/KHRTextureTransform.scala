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

import sge.utils.Nullable

/**
 * [[sge.gltf.data.texture.GLTFTextureInfo]] extension.
 * See https://github.com/KhronosGroup/glTF/blob/master/extensions/2.0/Khronos/KHR_texture_transform/README.md
 */
object KHRTextureTransform {
  val EXT: String = "KHR_texture_transform"
}

class KHRTextureTransform {
  var offset: Array[Float] = Array(0f, 0f)
  var rotation: Float = 0f
  var scale: Array[Float] = Array(1f, 1f)
  var texCoord: Nullable[Int] = Nullable.empty
}
