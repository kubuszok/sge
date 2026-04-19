/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: GLTFCamera,orthographic,perspective
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package camera

import sge.utils.Nullable

class GLTFCamera extends GLTFEntity {
  var `type`:       Nullable[String]           = Nullable.empty
  var perspective:  Nullable[GLTFPerspective]  = Nullable.empty
  var orthographic: Nullable[GLTFOrthographic] = Nullable.empty
}
