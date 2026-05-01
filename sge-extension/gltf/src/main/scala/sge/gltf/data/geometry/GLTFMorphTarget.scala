/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: GLTFMorphTarget
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package geometry

import scala.collection.mutable.HashMap

/** A morph target is a map of attribute names to accessor indices. In the original Java, this extends ObjectMap and implements Json.Serializable; in SGE we use a simple HashMap since SGE uses
  * jsoniter-scala, not LibGDX Json.
  */
@scala.annotation.nowarn("msg=inheritance from class HashMap.*is deprecated")
class GLTFMorphTarget extends HashMap[String, Int]
