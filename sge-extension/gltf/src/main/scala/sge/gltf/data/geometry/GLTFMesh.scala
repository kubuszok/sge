/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package geometry

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

class GLTFMesh extends GLTFEntity {
  var primitives: Nullable[ArrayBuffer[GLTFPrimitive]] = Nullable.empty
  var weights: Nullable[Array[Float]] = Nullable.empty
}
