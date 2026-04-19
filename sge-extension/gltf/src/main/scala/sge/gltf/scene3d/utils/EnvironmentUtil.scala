/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/EnvironmentUtil.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 69
 * Covenant-baseline-methods: EnvironmentUtil,FACE_NAMES_FULL,FACE_NAMES_NEG_POS,FACE_NAMES_NP,count,createCubemap,cubemap,data,files,getLightCount,level
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package utils

import sge.Sge
import sge.assets.loaders.FileHandleResolver
import sge.files.FileHandle
import sge.graphics.{ Cubemap, Texture }
import sge.graphics.g3d.Environment
import sge.graphics.g3d.attributes.{ DirectionalLightsAttribute, PointLightsAttribute, SpotLightsAttribute }

object EnvironmentUtil {

  val FACE_NAMES_FULL:    Array[String] = Array("right", "left", "top", "bottom", "front", "back")
  val FACE_NAMES_NP:      Array[String] = Array("px", "nx", "py", "ny", "pz", "nz")
  val FACE_NAMES_NEG_POS: Array[String] = Array("posx", "negx", "posy", "negy", "posz", "negz")

  def createCubemap(resolver: FileHandleResolver, baseName: String, extension: String, faceNames: Array[String])(using Sge): Cubemap = {
    val cubemap = Cubemap(
      resolver.resolve(baseName + faceNames(0) + extension),
      resolver.resolve(baseName + faceNames(1) + extension),
      resolver.resolve(baseName + faceNames(2) + extension),
      resolver.resolve(baseName + faceNames(3) + extension),
      resolver.resolve(baseName + faceNames(4) + extension),
      resolver.resolve(baseName + faceNames(5) + extension)
    )
    cubemap.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    cubemap
  }

  def createCubemap(resolver: FileHandleResolver, baseName: String, midName: String, extension: String, lods: Int, faceNames: Array[String])(using Sge): Cubemap = {
    val files = new Array[FileHandle](6 * lods)
    var level = 0
    while (level < lods) {
      var face = 0
      while (face < 6) {
        files(level * 6 + face) = resolver.resolve(baseName + faceNames(face) + midName + level + extension)
        face += 1
      }
      level += 1
    }
    val data    = FacedMultiCubemapData(files, lods)
    val cubemap = Cubemap(data)
    cubemap.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Linear)
    cubemap
  }

  def getLightCount(environment: Environment): Int = {
    var count = 0
    environment.getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type).foreach { dla =>
      count += dla.lights.size
    }
    environment.getAs[PointLightsAttribute](PointLightsAttribute.Type).foreach { pla =>
      count += pla.lights.size
    }
    environment.getAs[SpotLightsAttribute](SpotLightsAttribute.Type).foreach { sla =>
      count += sla.lights.size
    }
    count
  }
}
