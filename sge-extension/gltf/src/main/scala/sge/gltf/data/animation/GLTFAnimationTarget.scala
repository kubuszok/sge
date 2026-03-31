/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package animation

import sge.utils.Nullable

class GLTFAnimationTarget extends GLTFObject {
  var node: Nullable[Int]    = Nullable.empty
  var path: Nullable[String] = Nullable.empty
}
