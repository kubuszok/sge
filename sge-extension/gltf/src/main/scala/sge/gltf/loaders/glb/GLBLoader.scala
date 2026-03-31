/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package glb

import sge.Sge
import sge.files.FileHandle
import sge.graphics.g3d.Model
import sge.gltf.loaders.shared.GLTFLoaderBase

class GLBLoader extends GLTFLoaderBase() {

  def load(file: FileHandle)(using Sge): Model = {
    load(file, false)
  }

  def load(file: FileHandle, withData: Boolean)(using Sge): Model = {
    val dataFileResolver = new BinaryDataFileResolver()
    dataFileResolver.load(file)
    load(dataFileResolver, withData)
  }

  def load(bytes: Array[Byte])(using Sge): Model = {
    load(bytes, false)
  }

  def load(bytes: Array[Byte], withData: Boolean)(using Sge): Model = {
    val dataFileResolver = new BinaryDataFileResolver()
    dataFileResolver.load(bytes)
    load(dataFileResolver, withData)
  }
}
