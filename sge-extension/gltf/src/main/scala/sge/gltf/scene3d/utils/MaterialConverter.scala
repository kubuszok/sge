/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/MaterialConverter.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Utility to convert PBR materials to default materials in order to be used with DefaultShader.
 * Some conversion are approximation because PBR and Gouraud lighting models are very different.
 */
package sge
package gltf
package scene3d
package utils

import sge.gltf.scene3d.attributes.{ PBRColorAttribute, PBRFloatAttribute }
import sge.gltf.scene3d.scene.Scene
import sge.graphics.Color
import sge.graphics.g3d.Material
import sge.graphics.g3d.attributes.{ ColorAttribute, FloatAttribute }

object MaterialConverter {

  def makeCompatible(scene: Scene): Unit = {
    val mats = scene.modelInstance.materials
    var i    = 0
    while (i < mats.size) {
      makeCompatible(mats(i))
      i += 1
    }
  }

  def makeCompatible(materials: Iterable[Material]): Unit =
    materials.foreach(makeCompatible)

  def makeCompatible(material: Material): Unit = {
    val baseColorAttribute = material.getAs[PBRColorAttribute](PBRColorAttribute.BaseColorFactor)
    val baseColor          = baseColorAttribute.map(_.color).getOrElse(Color.WHITE)
    material.set(ColorAttribute.createDiffuse(baseColor))
    material.set(ColorAttribute.createSpecular(baseColor))

    val roughnessAttribute = material.getAs[PBRFloatAttribute](PBRFloatAttribute.Roughness)
    // default roughness is 1 as per GLTF specification.
    val roughness = roughnessAttribute.map(_.value).getOrElse(1f)

    // Conversion approximation based on Blender FBX export plugin.
    val shininess = (1f - roughness) * 10f
    if (shininess > 0f) {
      material.set(FloatAttribute.createShininess(shininess * shininess))
    }
  }
}
