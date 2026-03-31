/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package data

import sge.utils.Nullable

class GLTFAccessor extends GLTFEntity {
  var bufferView: Nullable[Int] = Nullable.empty
  var normalized: Boolean = false
  var byteOffset: Int = 0
  var componentType: Int = 0
  var count: Int = 0
  var `type`: Nullable[String] = Nullable.empty
  var min: Nullable[Array[Float]] = Nullable.empty
  var max: Nullable[Array[Float]] = Nullable.empty

  var sparse: Nullable[GLTFAccessorSparse] = Nullable.empty
}
