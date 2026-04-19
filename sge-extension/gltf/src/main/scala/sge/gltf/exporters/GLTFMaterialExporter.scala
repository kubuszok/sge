/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 274
 * Covenant-baseline-methods: GLTFMaterialExporter,defaultNullAttr,defaultNullBool,defaultNullColor,existing,exportMaterial,exportMaterials,ext,extEmissive,extIOR,extIridescence,extSpecular,extTransmission,extVolume,getTexture,i,imageIndex,mapMag,mapMin,mapWrap,normalTexture,occlusionTexture,pbr,rgb,s,sampler,source,t,texture,ti
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.Sge
import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.g3d.{ Attribute, Material }
import sge.graphics.g3d.attributes.{ BlendingAttribute, ColorAttribute, FloatAttribute, IntAttribute, TextureAttribute }
import sge.graphics.g3d.model.{ Node, NodePart }
import sge.gltf.data.GLTFExtensions
import sge.gltf.data.extensions.{ KHRMaterialsEmissiveStrength, KHRMaterialsIOR, KHRMaterialsIridescence, KHRMaterialsSpecular, KHRMaterialsTransmission, KHRMaterialsUnlit, KHRMaterialsVolume }
import sge.gltf.data.material.{ GLTFMaterial, GLTFpbrMetallicRoughness }
import sge.gltf.data.texture.{ GLTFImage, GLTFNormalTextureInfo, GLTFOcclusionTextureInfo, GLTFSampler, GLTFTexture, GLTFTextureInfo }
import sge.gltf.scene3d.attributes.{ PBRColorAttribute, PBRFlagAttribute, PBRFloatAttribute, PBRHDRColorAttribute, PBRIridescenceAttribute, PBRTextureAttribute, PBRVolumeAttribute }
import sge.utils.{ DynamicArray, Nullable, SgeError }

private[exporters] class GLTFMaterialExporter(private val base: GLTFExporter)(using Sge) {

  def exportMaterials(nodes: DynamicArray[Node]): Unit = {
    var i = 0
    while (i < nodes.size) {
      val node = nodes(i)
      var j    = 0
      while (j < node.parts.size) {
        exportMaterial(node.parts(j).material)
        j += 1
      }
      exportMaterials(node.children)
      i += 1
    }
  }

  private def exportMaterial(material: Material): Unit =
    if (base.materialMapping.containsByRef(material)) {
      // already exported
    } else {
      base.materialMapping.add(material)

      val m = new GLTFMaterial()
      if (base.root.materials.isEmpty) base.root.materials = Nullable(ArrayBuffer[GLTFMaterial]())
      base.root.materials.get += m

      m.name = Nullable(material.id)

      var blending = false
      for (a <- material)
        if (a.`type` == ColorAttribute.Diffuse) {
          pbr(m).baseColorFactor = GLTFExportTypes.rgba(defaultNullAttr(Color.WHITE, a.asInstanceOf[ColorAttribute]))
        } else if (a.`type` == PBRColorAttribute.BaseColorFactor) {
          pbr(m).baseColorFactor = GLTFExportTypes.rgba(defaultNullAttr(Color.WHITE, a.asInstanceOf[ColorAttribute]))
        } else if (a.`type` == ColorAttribute.Emissive) {
          m.emissiveFactor = GLTFExportTypes.rgb(defaultNullAttr(Color.BLACK, a.asInstanceOf[ColorAttribute]))
        } else if (a.`type` == BlendingAttribute.Type) {
          blending = true
        } else if (a.`type` == IntAttribute.CullFace) {
          m.doubleSided = defaultNullBool(true, a.asInstanceOf[IntAttribute].value == 0)
        } else if (a.`type` == FloatAttribute.AlphaTest) {
          m.alphaCutoff = Nullable(a.asInstanceOf[FloatAttribute].value)
        } else if (a.`type` == PBRFloatAttribute.Metallic) {
          pbr(m).metallicFactor = a.asInstanceOf[PBRFloatAttribute].value
        } else if (a.`type` == PBRFloatAttribute.Roughness) {
          pbr(m).roughnessFactor = a.asInstanceOf[PBRFloatAttribute].value
        } else if (a.`type` == PBRTextureAttribute.BaseColorTexture) {
          pbr(m).baseColorTexture = Nullable(texture(a.asInstanceOf[TextureAttribute]))
        } else if (a.`type` == PBRTextureAttribute.MetallicRoughnessTexture) {
          pbr(m).metallicRoughnessTexture = Nullable(texture(a.asInstanceOf[TextureAttribute]))
        } else if (a.`type` == PBRTextureAttribute.EmissiveTexture) {
          m.emissiveTexture = Nullable(texture(a.asInstanceOf[TextureAttribute]))
        } else if (a.`type` == PBRTextureAttribute.NormalTexture) {
          m.normalTexture = Nullable(normalTexture(a.asInstanceOf[PBRTextureAttribute], material))
        } else if (a.`type` == PBRTextureAttribute.OcclusionTexture) {
          m.occlusionTexture = Nullable(occlusionTexture(a.asInstanceOf[PBRTextureAttribute], material))
        }
        // Extensions
        // Unlit
        else if (a.`type` == PBRFlagAttribute.Unlit) {
          ext(m, classOf[KHRMaterialsUnlit], KHRMaterialsUnlit.EXT)
        }
        // Transmission
        else if (a.`type` == PBRFloatAttribute.TransmissionFactor) {
          extTransmission(m).transmissionFactor = a.asInstanceOf[PBRFloatAttribute].value
        } else if (a.`type` == PBRTextureAttribute.TransmissionTexture) {
          extTransmission(m).transmissionTexture = Nullable(texture(a.asInstanceOf[PBRTextureAttribute]))
        }
        // Volume
        else if (a.`type` == PBRVolumeAttribute.Type) {
          val extV = extVolume(m)
          val v    = a.asInstanceOf[PBRVolumeAttribute]
          extV.thicknessFactor = v.thicknessFactor
          extV.attenuationDistance = if (v.attenuationDistance > 0) Nullable(v.attenuationDistance) else Nullable.empty
          extV.attenuationColor = rgb(v.attenuationColor)
        } else if (a.`type` == PBRTextureAttribute.ThicknessTexture) {
          extVolume(m).thicknessTexture = Nullable(texture(a.asInstanceOf[PBRTextureAttribute]))
        }
        // IOR
        else if (a.`type` == PBRFloatAttribute.IOR) {
          extIOR(m).ior = a.asInstanceOf[PBRFloatAttribute].value
        }
        // Specular
        else if (a.`type` == PBRFloatAttribute.SpecularFactor) {
          extSpecular(m).specularFactor = a.asInstanceOf[PBRFloatAttribute].value
        } else if (a.`type` == PBRHDRColorAttribute.Specular) {
          val v = a.asInstanceOf[PBRHDRColorAttribute]
          extSpecular(m).specularColorFactor = Array(v.r, v.g, v.b)
        } else if (a.`type` == PBRTextureAttribute.SpecularFactorTexture) {
          extSpecular(m).specularTexture = Nullable(texture(a.asInstanceOf[PBRTextureAttribute]))
        } else if (a.`type` == PBRTextureAttribute.SpecularColorTexture) {
          extSpecular(m).specularColorTexture = Nullable(texture(a.asInstanceOf[TextureAttribute]))
        }
        // Iridescence
        else if (a.`type` == PBRIridescenceAttribute.Type) {
          val v    = a.asInstanceOf[PBRIridescenceAttribute]
          val extI = extIridescence(m)
          extI.iridescenceFactor = v.factor
          extI.iridescenceIor = v.ior
          extI.iridescenceThicknessMinimum = v.thicknessMin
          extI.iridescenceThicknessMaximum = v.thicknessMax
        } else if (a.`type` == PBRTextureAttribute.IridescenceTexture) {
          extIridescence(m).iridescenceTexture = Nullable(texture(a.asInstanceOf[PBRTextureAttribute]))
        } else if (a.`type` == PBRTextureAttribute.IridescenceThicknessTexture) {
          extIridescence(m).iridescenceThicknessTexture = Nullable(texture(a.asInstanceOf[PBRTextureAttribute]))
        }
        // Emissive strength
        else if (a.`type` == PBRFloatAttribute.EmissiveIntensity) {
          extEmissive(m).emissiveStrength = a.asInstanceOf[PBRFloatAttribute].value
        }
      if (blending) {
        if (m.alphaCutoff.isDefined) {
          m.alphaMode = Nullable("MASK")
        } else {
          m.alphaMode = Nullable("BLEND")
        }
      }
    }

  private def rgb(color: Color): Array[Float] =
    Array(color.r, color.g, color.b)

  private def extTransmission(m: GLTFMaterial): KHRMaterialsTransmission =
    ext(m, classOf[KHRMaterialsTransmission], KHRMaterialsTransmission.EXT)

  private def extIOR(m: GLTFMaterial): KHRMaterialsIOR =
    ext(m, classOf[KHRMaterialsIOR], KHRMaterialsIOR.EXT)

  private def extEmissive(m: GLTFMaterial): KHRMaterialsEmissiveStrength =
    ext(m, classOf[KHRMaterialsEmissiveStrength], KHRMaterialsEmissiveStrength.EXT)

  private def extSpecular(m: GLTFMaterial): KHRMaterialsSpecular =
    ext(m, classOf[KHRMaterialsSpecular], KHRMaterialsSpecular.EXT)

  private def extIridescence(m: GLTFMaterial): KHRMaterialsIridescence =
    ext(m, classOf[KHRMaterialsIridescence], KHRMaterialsIridescence.EXT)

  private def extVolume(m: GLTFMaterial): KHRMaterialsVolume =
    ext(m, classOf[KHRMaterialsVolume], KHRMaterialsVolume.EXT)

  private def ext[T <: AnyRef](m: GLTFMaterial, tpe: Class[T], extName: String): T = {
    if (m.extensions.isEmpty) {
      m.extensions = Nullable(new GLTFExtensions())
    }
    val existing = m.extensions.get.get(tpe, extName)
    existing.fold {
      base.useExtension(extName, false)
      val e = tpe.getDeclaredConstructor().newInstance()
      m.extensions.get.set(extName, e)
      e
    }(identity)
  }

  private def defaultNullBool(defValue: Boolean, value: Boolean): Nullable[Boolean] =
    if (defValue == value) Nullable.empty else Nullable(value)

  protected def defaultNullColor(defaultColor: Color, color: Color): Nullable[Color] =
    if (color.equals(defaultColor)) Nullable.empty else Nullable(color)

  private def defaultNullAttr(defaultColor: Color, a: ColorAttribute): Nullable[ColorAttribute] =
    if (a.color.equals(defaultColor)) Nullable.empty else Nullable(a)

  private def occlusionTexture(a: PBRTextureAttribute, material: Material): GLTFOcclusionTextureInfo = {
    val ti = new GLTFOcclusionTextureInfo()
    ti.strength = material.get(PBRFloatAttribute.OcclusionStrength).get.asInstanceOf[PBRFloatAttribute].value
    ti.texCoord = a.uvIndex
    ti.index = Nullable(getTexture(a))
    ti
  }

  private def normalTexture(a: PBRTextureAttribute, material: Material): GLTFNormalTextureInfo = {
    val ti = new GLTFNormalTextureInfo()
    ti.scale = material.get(PBRFloatAttribute.NormalScale).get.asInstanceOf[PBRFloatAttribute].value
    ti.texCoord = a.uvIndex
    ti.index = Nullable(getTexture(a))
    ti
  }

  private def texture(a: TextureAttribute): GLTFTextureInfo = {
    val ti = new GLTFTextureInfo()
    ti.texCoord = a.uvIndex
    ti.index = Nullable(getTexture(a))
    ti
  }

  private def getTexture(a: TextureAttribute): Int = {
    val t = new GLTFTexture()
    t.sampler = sampler(a)
    t.source = a.textureDescription.texture.flatMap(tex => source(tex))

    if (base.root.textures.isEmpty) base.root.textures = Nullable(ArrayBuffer[GLTFTexture]())
    base.root.textures.get += t
    base.root.textures.get.size - 1
  }

  private def source(texture: Texture): Nullable[Int] = {
    val imageIndex = base.textureMapping.indexOfByRef(texture)
    if (imageIndex >= 0) {
      Nullable(imageIndex)
    } else {
      val image = new GLTFImage()
      if (base.root.images.isEmpty) base.root.images = Nullable(ArrayBuffer[GLTFImage]())
      base.root.images.get += image
      base.textureMapping.add(texture)
      base.binManager.exportImage(image, texture, base.getImageName(texture))
      Nullable(base.root.images.get.size - 1)
    }
  }

  private def sampler(a: TextureAttribute): Nullable[Int] = {
    val s = new GLTFSampler()
    s.minFilter = a.textureDescription.minFilter.flatMap(mapMin)
    s.magFilter = a.textureDescription.magFilter.flatMap(mapMag)
    s.wrapS = a.textureDescription.uWrap.flatMap(mapWrap)
    s.wrapT = a.textureDescription.vWrap.flatMap(mapWrap)
    if (s.minFilter.isEmpty && s.magFilter.isEmpty && s.wrapS.isEmpty && s.wrapT.isEmpty) {
      Nullable.empty
    } else {
      if (base.root.samplers.isEmpty) base.root.samplers = Nullable(ArrayBuffer[GLTFSampler]())
      base.root.samplers.get += s
      Nullable(base.root.samplers.get.size - 1)
    }
  }

  private def mapWrap(wrap: TextureWrap): Nullable[Int] =
    if (wrap == TextureWrap.ClampToEdge) Nullable(33071)
    else if (wrap == TextureWrap.MirroredRepeat) Nullable(33648)
    else Nullable.empty // Repeat is default

  private def mapMag(filter: TextureFilter): Nullable[Int] =
    if (filter == TextureFilter.Nearest) Nullable(9728)
    else Nullable.empty // Linear is default

  private def mapMin(filter: TextureFilter): Nullable[Int] =
    if (filter == TextureFilter.Nearest) Nullable(9728)
    else if (filter == TextureFilter.MipMap || filter == TextureFilter.MipMapLinearLinear) Nullable(9987)
    else if (filter == TextureFilter.MipMapLinearNearest) Nullable(9985)
    else if (filter == TextureFilter.MipMapNearestLinear) Nullable(9986)
    else if (filter == TextureFilter.MipMapNearestNearest) Nullable(9984)
    else Nullable.empty // Linear is default

  private def pbr(m: GLTFMaterial): GLTFpbrMetallicRoughness = {
    if (m.pbrMetallicRoughness.isEmpty) {
      m.pbrMetallicRoughness = Nullable(new GLTFpbrMetallicRoughness())
    }
    m.pbrMetallicRoughness.get
  }
}
