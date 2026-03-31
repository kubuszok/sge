/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package texture

import sge.utils.Nullable

class GLTFTextureInfo extends GLTFObject {
  var index: Nullable[Int] = Nullable.empty
  var texCoord: Int = 0
}
