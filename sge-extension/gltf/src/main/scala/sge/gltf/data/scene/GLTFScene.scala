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

class GLTFScene extends GLTFEntity {
  var nodes: Nullable[ArrayBuffer[Int]] = Nullable.empty
}
