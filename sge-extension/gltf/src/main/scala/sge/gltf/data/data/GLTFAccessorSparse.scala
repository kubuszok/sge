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

class GLTFAccessorSparse extends GLTFObject {
  var count:   Int                                 = 0
  var indices: Nullable[GLTFAccessorSparseIndices] = Nullable.empty
  var values:  Nullable[GLTFAccessorSparseValues]  = Nullable.empty
}
