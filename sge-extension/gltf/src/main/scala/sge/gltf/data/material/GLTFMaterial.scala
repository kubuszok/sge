/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package material

import sge.gltf.data.texture.{ GLTFNormalTextureInfo, GLTFOcclusionTextureInfo, GLTFTextureInfo }
import sge.utils.Nullable

class GLTFMaterial extends GLTFEntity {
  var emissiveFactor: Nullable[Array[Float]] = Nullable.empty

  var normalTexture:    Nullable[GLTFNormalTextureInfo]    = Nullable.empty
  var occlusionTexture: Nullable[GLTFOcclusionTextureInfo] = Nullable.empty
  var emissiveTexture:  Nullable[GLTFTextureInfo]          = Nullable.empty

  var alphaMode:   Nullable[String] = Nullable.empty
  var alphaCutoff: Nullable[Float]  = Nullable.empty

  var doubleSided: Nullable[Boolean] = Nullable.empty

  var pbrMetallicRoughness: Nullable[GLTFpbrMetallicRoughness] = Nullable.empty
}
