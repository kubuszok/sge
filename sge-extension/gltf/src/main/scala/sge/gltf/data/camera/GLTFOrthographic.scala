/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: GLTFOrthographic,xmag,ymag,zfar,znear
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
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
