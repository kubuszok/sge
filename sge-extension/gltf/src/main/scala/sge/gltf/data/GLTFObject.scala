/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 16
 * Covenant-baseline-methods: GLTFObject,extensions,extras
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data

import sge.utils.Nullable

abstract class GLTFObject {
  var extensions: Nullable[GLTFExtensions] = Nullable.empty
  var extras:     Nullable[GLTFExtras]     = Nullable.empty
}
