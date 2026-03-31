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

class GLTFAccessorSparseIndices extends GLTFObject {
  var bufferView: Int = 0
  var byteOffset: Int = 0
  var componentType: Int = 0
}
