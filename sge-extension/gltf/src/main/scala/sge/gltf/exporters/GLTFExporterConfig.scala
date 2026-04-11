/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

class GLTFExporterConfig {

  /** max binary file size (default 10 MB) */
  var maxBinaryFileSize: Int = 10 * 1024 * 1024

  var exportCameras: Boolean = true
  var exportLights:  Boolean = true
}
