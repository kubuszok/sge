/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 21
 * Covenant-baseline-methods: GLTFPrimitive,attributes,indices,material,mode,targets
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package geometry

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import sge.utils.Nullable

class GLTFPrimitive extends GLTFObject {
  var attributes: Nullable[HashMap[String, Int]]         = Nullable.empty
  var indices:    Nullable[Int]                          = Nullable.empty
  var mode:       Nullable[Int]                          = Nullable.empty
  var material:   Nullable[Int]                          = Nullable.empty
  var targets:    Nullable[ArrayBuffer[GLTFMorphTarget]] = Nullable.empty
}
