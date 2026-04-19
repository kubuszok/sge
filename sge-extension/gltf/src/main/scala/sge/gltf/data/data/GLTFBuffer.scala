/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: GLTFBuffer,byteLength,uri
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package data

import sge.utils.Nullable

class GLTFBuffer extends GLTFEntity {
  // "uri": "data:application/octet-stream;base64,uoV6P5Ff/z2imSc+KG
  var uri:        Nullable[String] = Nullable.empty
  var byteLength: Int              = 0
}
