/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: GLTFSkin,inverseBindMatrices,joints,skeleton
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package scene

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

class GLTFSkin extends GLTFEntity {
  var inverseBindMatrices: Nullable[Int]              = Nullable.empty
  var joints:              Nullable[ArrayBuffer[Int]] = Nullable.empty
  var skeleton:            Nullable[Int]              = Nullable.empty
}
