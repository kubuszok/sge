/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: MaterialLoaderBase,defaultMaterial,get,getDefaultMaterial,loadMaterial,loadMaterials,materials,textureResolver
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
import sge.gltf.loaders.shared.texture.TextureResolver
import sge.utils.Nullable

abstract class MaterialLoaderBase(
  protected val textureResolver: TextureResolver,
  private val defaultMaterial:   Material
) extends MaterialLoader {

  private val materials: ArrayBuffer[Material] = ArrayBuffer.empty

  override def getDefaultMaterial: Material = defaultMaterial

  override def get(index: Int): Material = materials(index)

  override def loadMaterials(glMaterials: Nullable[ArrayBuffer[GLTFMaterial]]): Unit =
    glMaterials.foreach { mats =>
      var i = 0
      while (i < mats.size) {
        val glMaterial = mats(i)
        val material   = loadMaterial(glMaterial)
        materials += material
        i += 1
      }
    }

  protected def loadMaterial(glMaterial: GLTFMaterial): Material
}
