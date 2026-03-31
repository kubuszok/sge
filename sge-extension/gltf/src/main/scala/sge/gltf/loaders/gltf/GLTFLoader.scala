/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package gltf

import sge.Sge
import sge.files.FileHandle
import sge.graphics.g3d.Model
import sge.gltf.loaders.shared.GLTFLoaderBase

class GLTFLoader extends GLTFLoaderBase() {

  def load(glFile: FileHandle)(using Sge): Model = {
    load(glFile, false)
  }

  def load(glFile: FileHandle, withData: Boolean)(using Sge): Model = {
    val dataFileResolver = new SeparatedDataFileResolver()
    dataFileResolver.load(glFile)
    load(dataFileResolver, withData)
  }
}
