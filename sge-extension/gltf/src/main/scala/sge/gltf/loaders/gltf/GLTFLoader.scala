/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: GLTFLoader,dataFileResolver,load
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package gltf

import sge.Sge
import sge.files.FileHandle
import sge.gltf.loaders.shared.GLTFLoaderBase
import sge.gltf.scene3d.scene.SceneAsset

class GLTFLoader extends GLTFLoaderBase() {

  def load(glFile: FileHandle)(using Sge): SceneAsset =
    load(glFile, false)

  def load(glFile: FileHandle, withData: Boolean)(using Sge): SceneAsset = {
    val dataFileResolver = new SeparatedDataFileResolver()
    dataFileResolver.load(glFile)
    load(dataFileResolver, withData)
  }
}
