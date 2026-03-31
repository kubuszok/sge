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

class GLTFSampler extends GLTFEntity {
  var minFilter: Nullable[Int] = Nullable.empty
  var magFilter: Nullable[Int] = Nullable.empty
  var wrapS:     Nullable[Int] = Nullable.empty
  var wrapT:     Nullable[Int] = Nullable.empty
}
