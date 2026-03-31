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

class GLTFOrthographic extends GLTFObject {
  var znear: Nullable[Float] = Nullable.empty
  var zfar:  Nullable[Float] = Nullable.empty
  var xmag:  Nullable[Float] = Nullable.empty
  var ymag:  Nullable[Float] = Nullable.empty
}
