/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package blender

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.geometry.GLTFMesh
import sge.utils.{Json, Nullable}

/**
 * Blender stores shape key names in mesh extras.
 *
 * TODO: Implement once GLTF JSON parsing is available.
 * The original Java reads targetNames from the mesh extras JSON.
 */
object BlenderShapeKeys {

  def parse(glMesh: GLTFMesh): Nullable[ArrayBuffer[String]] = {
    // TODO: parse targetNames from glMesh.extras once GLTF JSON codecs are available
    Nullable.empty[ArrayBuffer[String]]
  }
}
