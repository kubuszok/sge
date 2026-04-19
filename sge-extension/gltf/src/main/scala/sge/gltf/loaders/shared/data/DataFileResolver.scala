/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: DataFileResolver,getBuffer,getRoot,load
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package data

import java.nio.ByteBuffer
import sge.files.FileHandle
import sge.gltf.data.GLTF
import sge.gltf.data.texture.GLTFImage
import sge.graphics.Pixmap

trait DataFileResolver {
  def load(file:        FileHandle): Unit
  def getRoot:                       GLTF
  def getBuffer(buffer: Int):        ByteBuffer
  def load(glImage:     GLTFImage):  Pixmap
}
