/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package camera

import sge.utils.Nullable

class GLTFPerspective extends GLTFObject {
  var yfov: Float = 0f
  var znear: Float = 0f
  var aspectRatio: Nullable[Float] = Nullable.empty
  var zfar: Nullable[Float] = Nullable.empty
}
