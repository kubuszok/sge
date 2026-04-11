/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/material/PBRMaterialLoader.java
 * Covenant-verified: 2026-04-11
 */
package sge
package gltf
package loaders
package shared
package material

import sge.graphics.{ Color, Texture }
import sge.graphics.g3d.Material
import sge.graphics.g3d.attributes.{ BlendingAttribute, ColorAttribute, FloatAttribute, IntAttribute, TextureAttribute }
import sge.graphics.g3d.utils.TextureDescriptor
import sge.gltf.data.extensions.*
import sge.gltf.data.material.{ GLTFMaterial, GLTFpbrMetallicRoughness }
import sge.gltf.data.texture.GLTFTextureInfo
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.loaders.shared.texture.TextureResolver
import sge.gltf.scene3d.attributes.{ PBRColorAttribute, PBRFlagAttribute, PBRFloatAttribute, PBRHDRColorAttribute, PBRIridescenceAttribute, PBRTextureAttribute, PBRVolumeAttribute }
import sge.math.MathUtils
import sge.utils.{ Log, Nullable }

class PBRMaterialLoader(textureResolver: TextureResolver)
    extends MaterialLoaderBase(
      textureResolver,
      new Material(new PBRColorAttribute(PBRColorAttribute.BaseColorFactor, Color.WHITE))
    ) {

  override def loadMaterial(glMaterial: GLTFMaterial): Material = {
    val material = new Material()
    glMaterial.name.foreach { n => material.id = n }

    glMaterial.emissiveFactor.foreach { ef =>
      material.set(new ColorAttribute(ColorAttribute.Emissive, GLTFTypes.mapColor(Nullable(ef), Color.BLACK)))
    }

    glMaterial.emissiveTexture.foreach { et =>
      material.set(getTexureMap(PBRTextureAttribute.EmissiveTexture, et))
    }

    glMaterial.doubleSided.foreach { ds =>
      if (ds) {
        material.set(IntAttribute.createCullFace(0)) // 0 to disable culling
      }
    }

    glMaterial.normalTexture.foreach { nt =>
      material.set(getTexureMap(PBRTextureAttribute.NormalTexture, nt))
      material.set(PBRFloatAttribute.createNormalScale(nt.scale))
    }

    glMaterial.occlusionTexture.foreach { ot =>
      material.set(getTexureMap(PBRTextureAttribute.OcclusionTexture, ot))
      material.set(PBRFloatAttribute.createOcclusionStrength(ot.strength))
    }

    var alphaBlend = false
    glMaterial.alphaMode.foreach {
      case "OPAQUE" => // nothing to do
      case "MASK"   =>
        val value = glMaterial.alphaCutoff.getOrElse(0.5f)
        material.set(FloatAttribute.createAlphaTest(value))
        material.set(new BlendingAttribute()) // necessary
      case "BLEND" =>
        material.set(new BlendingAttribute()) // opacity is set by pbrMetallicRoughness below
        alphaBlend = true
      case am =>
        throw new GLTFIllegalException("unknow alpha mode : " + am)
    }

    glMaterial.pbrMetallicRoughness.foreach { p =>
      val baseColorFactor = GLTFTypes.mapColor(p.baseColorFactor, Color.WHITE)

      material.set(new PBRColorAttribute(PBRColorAttribute.BaseColorFactor, baseColorFactor))

      material.set(PBRFloatAttribute.createMetallic(p.metallicFactor))
      material.set(PBRFloatAttribute.createRoughness(p.roughnessFactor))

      p.metallicRoughnessTexture.foreach { mrt =>
        material.set(getTexureMap(PBRTextureAttribute.MetallicRoughnessTexture, mrt))
      }

      p.baseColorTexture.foreach { bct =>
        material.set(getTexureMap(PBRTextureAttribute.BaseColorTexture, bct))
      }

      if (alphaBlend) {
        material.getAs[BlendingAttribute](BlendingAttribute.Type).foreach { ba =>
          ba.opacity = baseColorFactor.a
        }
      }
    }

    // can have both PBR base and ext
    glMaterial.extensions.foreach { extensions =>
      // KHR_materials_pbrSpecularGlossiness (deprecated)
      extensions.get(classOf[KHRMaterialsPBRSpecularGlossiness], KHRMaterialsPBRSpecularGlossiness.EXT).foreach { ext =>
        Log.error(
          "GLTF: " + KHRMaterialsPBRSpecularGlossiness.EXT + " extension is deprecated by glTF 2.0 specification and not fully supported."
        )

        material.set(new ColorAttribute(ColorAttribute.Diffuse, GLTFTypes.mapColor(ext.diffuseFactor, Color.WHITE)))
        material.set(new ColorAttribute(ColorAttribute.Specular, GLTFTypes.mapColor(ext.specularFactor, Color.WHITE)))

        // not sure how to map normalized gloss to exponent ...
        material.set(new FloatAttribute(FloatAttribute.Shininess, MathUtils.lerp(1, 100, ext.glossinessFactor)))
        ext.diffuseTexture.foreach { dt =>
          material.set(getTexureMap(PBRTextureAttribute.BaseColorTexture, dt))
        }
        ext.specularGlossinessTexture.foreach { sgt =>
          material.set(getTexureMap(TextureAttribute.Specular, sgt))
        }
      }

      // KHR_materials_unlit
      extensions.get(classOf[KHRMaterialsUnlit], KHRMaterialsUnlit.EXT).foreach { _ =>
        material.set(new PBRFlagAttribute(PBRFlagAttribute.Unlit))
      }

      // KHR_materials_transmission
      extensions.get(classOf[KHRMaterialsTransmission], KHRMaterialsTransmission.EXT).foreach { ext =>
        material.set(PBRFloatAttribute.createTransmissionFactor(ext.transmissionFactor))
        ext.transmissionTexture.foreach { tt =>
          material.set(getTexureMap(PBRTextureAttribute.TransmissionTexture, tt))
        }
      }

      // KHR_materials_volume
      extensions.get(classOf[KHRMaterialsVolume], KHRMaterialsVolume.EXT).foreach { ext =>
        material.set(
          new PBRVolumeAttribute(
            ext.thicknessFactor,
            ext.attenuationDistance.getOrElse(0f),
            GLTFTypes.mapColor(Nullable(ext.attenuationColor), Color.WHITE)
          )
        )
        ext.thicknessTexture.foreach { tt =>
          material.set(getTexureMap(PBRTextureAttribute.ThicknessTexture, tt))
        }
      }

      // KHR_materials_ior
      extensions.get(classOf[KHRMaterialsIOR], KHRMaterialsIOR.EXT).foreach { ext =>
        material.set(PBRFloatAttribute.createIOR(ext.ior))
      }

      // KHR_materials_specular
      extensions.get(classOf[KHRMaterialsSpecular], KHRMaterialsSpecular.EXT).foreach { ext =>
        material.set(PBRFloatAttribute.createSpecularFactor(ext.specularFactor))
        material.set(
          new PBRHDRColorAttribute(
            PBRHDRColorAttribute.Specular,
            ext.specularColorFactor(0),
            ext.specularColorFactor(1),
            ext.specularColorFactor(2)
          )
        )
        ext.specularTexture.foreach { st =>
          material.set(getTexureMap(PBRTextureAttribute.SpecularFactorTexture, st))
        }
        ext.specularColorTexture.foreach { sct =>
          material.set(getTexureMap(PBRTextureAttribute.SpecularColorTexture, sct))
        }
      }

      // KHR_materials_iridescence
      extensions.get(classOf[KHRMaterialsIridescence], KHRMaterialsIridescence.EXT).foreach { ext =>
        material.set(
          new PBRIridescenceAttribute(
            ext.iridescenceFactor,
            ext.iridescenceIor,
            ext.iridescenceThicknessMinimum,
            ext.iridescenceThicknessMaximum
          )
        )
        ext.iridescenceTexture.foreach { it =>
          material.set(getTexureMap(PBRTextureAttribute.IridescenceTexture, it))
        }
        ext.iridescenceThicknessTexture.foreach { itt =>
          material.set(getTexureMap(PBRTextureAttribute.IridescenceThicknessTexture, itt))
        }
      }

      // KHR_materials_emissive_strength
      extensions.get(classOf[KHRMaterialsEmissiveStrength], KHRMaterialsEmissiveStrength.EXT).foreach { ext =>
        material.set(PBRFloatAttribute.createEmissiveIntensity(ext.emissiveStrength))
      }
    }

    material
  }

  protected def getTexureMap(`type`: Long, glMap: GLTFTextureInfo): PBRTextureAttribute = {
    val textureDescriptor = textureResolver.getTexture(glMap)

    val attribute = new PBRTextureAttribute(`type`, textureDescriptor)
    attribute.uvIndex = glMap.texCoord

    glMap.extensions.foreach { extensions =>
      extensions.get(classOf[KHRTextureTransform], KHRTextureTransform.EXT).foreach { ext =>
        attribute.offsetU = ext.offset(0)
        attribute.offsetV = ext.offset(1)
        attribute.scaleU = ext.scale(0)
        attribute.scaleV = ext.scale(1)
        attribute.rotationUV = ext.rotation
        ext.texCoord.foreach { tc =>
          attribute.uvIndex = tc
        }
      }
    }

    attribute
  }
}
