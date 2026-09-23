/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 11
 * Covenant-baseline-methods: GLTFMorphTarget
 * Covenant-source-reference: net/mgsx/gltf/data/geometry/GLTFMorphTarget.java
 * Covenant-verified: 2026-09-23
 */
package sge.gltf.data.geometry

/** A morph target is a map of attribute names to accessor indices. */
@scala.annotation.nowarn("msg=inheritance from class HashMap.*is deprecated")
class GLTFMorphTarget extends scala.collection.mutable.HashMap[String, Int]
