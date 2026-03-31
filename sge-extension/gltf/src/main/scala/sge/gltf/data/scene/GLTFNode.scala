/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package scene

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

class GLTFNode extends GLTFEntity {
  var children:    Nullable[ArrayBuffer[Int]] = Nullable.empty
  var matrix:      Nullable[Array[Float]]     = Nullable.empty
  var translation: Nullable[Array[Float]]     = Nullable.empty
  var rotation:    Nullable[Array[Float]]     = Nullable.empty
  var scale:       Nullable[Array[Float]]     = Nullable.empty

  var mesh:   Nullable[Int] = Nullable.empty
  var camera: Nullable[Int] = Nullable.empty
  var skin:   Nullable[Int] = Nullable.empty

  var weights: Nullable[Array[Float]] = Nullable.empty
}
