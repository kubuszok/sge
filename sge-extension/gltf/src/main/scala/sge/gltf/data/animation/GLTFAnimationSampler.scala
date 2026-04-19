/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: GLTFAnimationSampler,input,interpolation,output
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package animation

import sge.utils.Nullable

class GLTFAnimationSampler extends GLTFObject {
  var input:         Nullable[Int]    = Nullable.empty
  var output:        Nullable[Int]    = Nullable.empty
  var interpolation: Nullable[String] = Nullable.empty
}
