/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/material/PBRMaterialLoader.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Full PBR attribute support (PBRTextureAttribute, PBRFloatAttribute, PBRColorAttribute, etc.)
 *     not yet ported from net.mgsx.gltf.scene3d.attributes.* — currently creates simplified
 *     materials with just BlendingAttribute, ColorAttribute, FloatAttribute, IntAttribute.
 */
package sge
package gltf
package loaders
package shared
package material

import sge.graphics.{ Color, Texture }
import sge.graphics.g3d.Material
import sge.graphics.g3d.attributes.{ BlendingAttribute, ColorAttribute, FloatAttribute, IntAttribute }
import sge.graphics.g3d.utils.TextureDescriptor
import sge.gltf.data.extensions.*
import sge.gltf.data.material.{ GLTFMaterial, GLTFpbrMetallicRoughness }
import sge.gltf.data.texture.GLTFTextureInfo
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.loaders.shared.texture.TextureResolver
import sge.utils.Nullable

/** PBR Material loader. Creates materials with basic color/texture attributes.
  *
  * TODO: Full PBR attribute support requires PBRTextureAttribute, PBRFloatAttribute, PBRColorAttribute etc. from the scene3d subsystem (not yet ported). Currently creates simplified materials with
  * standard g3d attributes.
  */
class PBRMaterialLoader(textureResolver: TextureResolver)
    extends MaterialLoaderBase(
      textureResolver,
      new Material(new ColorAttribute(ColorAttribute.Diffuse, Color.WHITE))
    ) {

  override def loadMaterial(glMaterial: GLTFMaterial): Material = {
    val material = new Material()
    glMaterial.name.foreach { n => material.id = n }

    glMaterial.emissiveFactor.foreach { ef =>
      material.set(new ColorAttribute(ColorAttribute.Emissive, GLTFTypes.mapColor(Nullable(ef), Color.BLACK)))
    }

    glMaterial.doubleSided.foreach { ds =>
      if (ds) {
        material.set(IntAttribute.createCullFace(0)) // 0 to disable culling
      }
    }

    var alphaBlend = false
    glMaterial.alphaMode.foreach {
      case "OPAQUE" => // nothing to do
      case "MASK"   =>
        val value = glMaterial.alphaCutoff.getOrElse(0.5f)
        material.set(FloatAttribute.createAlphaTest(value))
        material.set(new BlendingAttribute()) // necessary
      case "BLEND" =>
        material.set(new BlendingAttribute())
        alphaBlend = true
      case am =>
        throw new GLTFIllegalException("unknown alpha mode : " + am)
    }

    glMaterial.pbrMetallicRoughness.foreach { p =>
      val baseColorFactor = GLTFTypes.mapColor(p.baseColorFactor, Color.WHITE)
      material.set(new ColorAttribute(ColorAttribute.Diffuse, baseColorFactor))
      material.set(FloatAttribute.createShininess(p.metallicFactor))

      if (alphaBlend) {
        material.get(BlendingAttribute.Type).foreach { attr =>
          attr.asInstanceOf[BlendingAttribute].opacity = baseColorFactor.a
        }
      }
    }

    material
  }
}
