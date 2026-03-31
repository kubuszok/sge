/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRCubemapAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.Cubemap
import sge.graphics.g3d.Attribute
import sge.graphics.g3d.attributes.CubemapAttribute
import sge.graphics.g3d.utils.TextureDescriptor

class PBRCubemapAttribute(
  `type`:             Long,
  textureDescription: TextureDescriptor[Cubemap]
) extends CubemapAttribute(`type`, textureDescription) {

  def this(`type`: Long, cubemap: Cubemap) =
    this(`type`, {
      val td = TextureDescriptor[Cubemap]()
      td.texture = sge.utils.Nullable(cubemap)
      td
    })

  override def copy(): Attribute =
    PBRCubemapAttribute(`type`, textureDescription)
}

object PBRCubemapAttribute {

  val DiffuseEnvAlias: String = "DiffuseEnvSampler"
  val DiffuseEnv:      Long   = Attribute.register(DiffuseEnvAlias)

  val SpecularEnvAlias: String = "SpecularEnvSampler"
  val SpecularEnv:      Long   = Attribute.register(SpecularEnvAlias)

  // Also register as an alias on the regular EnvironmentMap type
  val EnvironmentMap: Long = CubemapAttribute.EnvironmentMap

  // Extend the Mask so CubemapAttribute.is() recognizes PBR types
  CubemapAttribute.Mask |= DiffuseEnv | SpecularEnv

  def createDiffuseEnv(diffuseCubemap: Cubemap): Attribute =
    PBRCubemapAttribute(DiffuseEnv, diffuseCubemap)

  def createSpecularEnv(specularCubemap: Cubemap): Attribute =
    PBRCubemapAttribute(SpecularEnv, specularCubemap)
}
