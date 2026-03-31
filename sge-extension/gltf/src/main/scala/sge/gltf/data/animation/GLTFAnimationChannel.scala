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

class GLTFAnimationChannel extends GLTFObject {
  var sampler: Nullable[Int]                 = Nullable.empty
  var target:  Nullable[GLTFAnimationTarget] = Nullable.empty
}
