/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: GLTFTextureInfo,index,texCoord
 * Covenant-source-reference: net/mgsx/gltf/data/texture/GLTFTextureInfo.java
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data
package texture

import lowlevel.Nullable

class GLTFTextureInfo extends GLTFObject {
  var index:    Nullable[Int] = Nullable.empty
  var texCoord: Int           = 0
}
