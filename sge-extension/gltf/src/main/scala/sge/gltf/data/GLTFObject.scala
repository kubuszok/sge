/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data

import sge.utils.Nullable

abstract class GLTFObject {
  var extensions: Nullable[GLTFExtensions] = Nullable.empty
  var extras:     Nullable[GLTFExtras]     = Nullable.empty
}
