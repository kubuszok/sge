/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: GLTFTexture,sampler,source
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package texture

import sge.utils.Nullable

class GLTFTexture extends GLTFEntity {
  var source:  Nullable[Int] = Nullable.empty
  var sampler: Nullable[Int] = Nullable.empty
}
