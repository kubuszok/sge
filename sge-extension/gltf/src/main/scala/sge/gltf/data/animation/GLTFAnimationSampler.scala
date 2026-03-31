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

class GLTFAnimationSampler extends GLTFObject {
  var input: Nullable[Int] = Nullable.empty
  var output: Nullable[Int] = Nullable.empty
  var interpolation: Nullable[String] = Nullable.empty
}
