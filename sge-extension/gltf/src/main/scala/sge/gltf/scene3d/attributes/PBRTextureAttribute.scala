/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRTextureAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 183
 * Covenant-baseline-methods: BRDFLUTTexture,BRDFLUTTextureAlias,BaseColorTexture,BaseColorTextureAlias,EmissiveTexture,EmissiveTextureAlias,IridescenceTexture,IridescenceTextureAlias,IridescenceThicknessTexture,IridescenceThicknessTextureAlias,MetallicRoughnessTexture,MetallicRoughnessTextureAlias,NormalTexture,NormalTextureAlias,OcclusionTexture,OcclusionTextureAlias,PBRTextureAttribute,SpecularColorTexture,SpecularColorTextureAlias,SpecularFactorTexture,SpecularFactorTextureAlias,ThicknessTexture,ThicknessTextureAlias,TransmissionSourceTexture,TransmissionSourceTextureAlias,TransmissionTexture,TransmissionTextureAlias,compare,copy,createBRDFLookupTexture,createBaseColorTexture,createEmissiveTexture,createIridescenceTexture,createIridescenceThicknessTexture,createMetallicRoughnessTexture,createNormalTexture,createOcclusionTexture,createSpecularFactorTexture,createThicknessTexture,createTransmissionTexture,r,rotationUV,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.graphics.g3d.Attribute
import sge.graphics.g3d.attributes.TextureAttribute
import sge.graphics.g3d.utils.TextureDescriptor
import sge.math.MathUtils

class PBRTextureAttribute(
  `type`: Long
) extends TextureAttribute(`type`) {

  var rotationUV: Float = 0f

  def this(`type`: Long, textureDescription: TextureDescriptor[Texture]) = {
    this(`type`)
    this.textureDescription.set(textureDescription)
  }

  def this(`type`: Long, texture: Texture) = {
    this(`type`)
    this.textureDescription.texture = sge.utils.Nullable(texture)
  }

  def this(`type`: Long, region: TextureRegion) = {
    this(`type`)
    set(region)
  }

  def this(attribute: PBRTextureAttribute) = {
    this(attribute.`type`)
    this.textureDescription.set(attribute.textureDescription)
    this.offsetU = attribute.offsetU
    this.offsetV = attribute.offsetV
    this.scaleU = attribute.scaleU
    this.scaleV = attribute.scaleV
    this.uvIndex = attribute.uvIndex
    this.rotationUV = attribute.rotationUV
  }

  override def copy(): Attribute =
    PBRTextureAttribute(this)

  override def compare(that: Attribute): Int = {
    val r = super.compare(that)
    if (r != 0) r
    else {
      that match {
        case other: PBRTextureAttribute =>
          if (!MathUtils.isEqual(rotationUV, other.rotationUV)) {
            if (rotationUV < other.rotationUV) -1 else 1
          } else 0
        case _ => 0
      }
    }
  }
}

object PBRTextureAttribute {

  val BaseColorTextureAlias: String = "diffuseTexture"
  val BaseColorTexture:      Long   = Attribute.register(BaseColorTextureAlias)

  val EmissiveTextureAlias: String = "emissiveTexture"
  val EmissiveTexture:      Long   = Attribute.register(EmissiveTextureAlias)

  val NormalTextureAlias: String = "normalTexture"
  val NormalTexture:      Long   = Attribute.register(NormalTextureAlias)

  val MetallicRoughnessTextureAlias: String = "MetallicRoughnessSampler"
  val MetallicRoughnessTexture:      Long   = Attribute.register(MetallicRoughnessTextureAlias)

  val OcclusionTextureAlias: String = "OcclusionSampler"
  val OcclusionTexture:      Long   = Attribute.register(OcclusionTextureAlias)

  // IBL environment only
  val BRDFLUTTextureAlias: String = "brdfLUTSampler"
  val BRDFLUTTexture:      Long   = Attribute.register(BRDFLUTTextureAlias)

  val TransmissionTextureAlias: String = "TransmissionTexture"
  val TransmissionTexture:      Long   = Attribute.register(TransmissionTextureAlias)

  val ThicknessTextureAlias: String = "ThicknessTexture"
  val ThicknessTexture:      Long   = Attribute.register(ThicknessTextureAlias)

  val SpecularFactorTextureAlias: String = "SpecularFactorTexture"
  val SpecularFactorTexture:      Long   = Attribute.register(SpecularFactorTextureAlias)

  val IridescenceTextureAlias: String = "IridescenceTexture"
  val IridescenceTexture:      Long   = Attribute.register(IridescenceTextureAlias)

  val IridescenceThicknessTextureAlias: String = "IridescenceThicknessTexture"
  val IridescenceThicknessTexture:      Long   = Attribute.register(IridescenceThicknessTextureAlias)

  val TransmissionSourceTextureAlias: String = "TransmissionSourceTexture"
  val TransmissionSourceTexture:      Long   = Attribute.register(TransmissionSourceTextureAlias)

  val SpecularColorTextureAlias: String = "SpecularColorTexture"
  val SpecularColorTexture:      Long   = Attribute.register(SpecularColorTextureAlias)

  // Extend the Mask so TextureAttribute.is() recognizes PBR types
  TextureAttribute.Mask |= MetallicRoughnessTexture | OcclusionTexture | BaseColorTexture |
    NormalTexture | EmissiveTexture | BRDFLUTTexture | TransmissionTexture | ThicknessTexture |
    SpecularFactorTexture | IridescenceTexture | IridescenceThicknessTexture |
    TransmissionSourceTexture | SpecularColorTexture

  def createBaseColorTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(BaseColorTexture, texture)

  def createEmissiveTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(EmissiveTexture, texture)

  def createNormalTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(NormalTexture, texture)

  def createMetallicRoughnessTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(MetallicRoughnessTexture, texture)

  def createOcclusionTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(OcclusionTexture, texture)

  def createBRDFLookupTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(BRDFLUTTexture, texture)

  def createTransmissionTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(TransmissionTexture, texture)

  def createThicknessTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(ThicknessTexture, texture)

  def createSpecularFactorTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(SpecularFactorTexture, texture)

  def createIridescenceTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(IridescenceTexture, texture)

  def createIridescenceThicknessTexture(texture: Texture): PBRTextureAttribute =
    PBRTextureAttribute(IridescenceThicknessTexture, texture)

  def createBaseColorTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(BaseColorTexture, region)

  def createEmissiveTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(EmissiveTexture, region)

  def createNormalTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(NormalTexture, region)

  def createMetallicRoughnessTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(MetallicRoughnessTexture, region)

  def createOcclusionTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(OcclusionTexture, region)

  def createBRDFLookupTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(BRDFLUTTexture, region)

  def createTransmissionTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(TransmissionTexture, region)

  def createThicknessTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(ThicknessTexture, region)

  def createSpecularFactorTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(SpecularFactorTexture, region)

  def createIridescenceTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(IridescenceTexture, region)

  def createIridescenceThicknessTexture(region: TextureRegion): PBRTextureAttribute =
    PBRTextureAttribute(IridescenceThicknessTexture, region)
}
