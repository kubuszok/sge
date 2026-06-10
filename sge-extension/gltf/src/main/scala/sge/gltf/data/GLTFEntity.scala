/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 15
 * Covenant-baseline-methods: GLTFEntity,name
 * Covenant-source-reference: net/mgsx/gltf/data/GLTFEntity.java
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data

import lowlevel.Nullable

abstract class GLTFEntity extends GLTFObject {
  var name: Nullable[String] = Nullable.empty
}
