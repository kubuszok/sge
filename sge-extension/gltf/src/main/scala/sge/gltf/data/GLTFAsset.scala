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

class GLTFAsset extends GLTFObject {
  var generator:  Nullable[String] = Nullable.empty
  var version:    Nullable[String] = Nullable.empty
  var copyright:  Nullable[String] = Nullable.empty
  var minVersion: Nullable[String] = Nullable.empty
}
