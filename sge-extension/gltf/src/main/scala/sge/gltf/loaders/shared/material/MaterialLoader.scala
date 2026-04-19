/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: MaterialLoader,get,getDefaultMaterial,loadMaterials
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package material

import scala.collection.mutable.ArrayBuffer
import sge.graphics.g3d.Material
import sge.gltf.data.material.GLTFMaterial
import sge.utils.Nullable

trait MaterialLoader {

  def getDefaultMaterial: Material

  def get(index: Int): Material

  def loadMaterials(materials: Nullable[ArrayBuffer[GLTFMaterial]]): Unit
}
