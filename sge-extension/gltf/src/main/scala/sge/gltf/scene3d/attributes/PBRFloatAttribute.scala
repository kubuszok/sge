/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRFloatAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 77
 * Covenant-baseline-methods: EmissiveIntensity,EmissiveIntensityAlias,IOR,IORAlias,Metallic,MetallicAlias,NormalScale,NormalScaleAlias,OcclusionStrength,OcclusionStrengthAlias,PBRFloatAttribute,Roughness,RoughnessAlias,ShadowBias,ShadowBiasAlias,SpecularFactor,SpecularFactorAlias,TransmissionFactor,TransmissionFactorAlias,copy,createEmissiveIntensity,createIOR,createMetallic,createNormalScale,createOcclusionStrength,createRoughness,createSpecularFactor,createTransmissionFactor
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute
import sge.graphics.g3d.attributes.FloatAttribute

class PBRFloatAttribute(
  `type`: Long,
  value:  Float
) extends FloatAttribute(`type`, value) {

  override def copy(): Attribute =
    PBRFloatAttribute(`type`, this.value)
}

object PBRFloatAttribute {

  val MetallicAlias: String = "Metallic"
  val Metallic:      Long   = Attribute.register(MetallicAlias)

  val RoughnessAlias: String = "Roughness"
  val Roughness:      Long   = Attribute.register(RoughnessAlias)

  val NormalScaleAlias: String = "NormalScale"
  val NormalScale:      Long   = Attribute.register(NormalScaleAlias)

  val OcclusionStrengthAlias: String = "OcclusionStrength"
  val OcclusionStrength:      Long   = Attribute.register(OcclusionStrengthAlias)

  val ShadowBiasAlias: String = "ShadowBias"
  val ShadowBias:      Long   = Attribute.register(ShadowBiasAlias)

  val EmissiveIntensityAlias: String = "EmissiveIntensity"
  val EmissiveIntensity:      Long   = Attribute.register(EmissiveIntensityAlias)

  val TransmissionFactorAlias: String = "TransmissionFactor"
  val TransmissionFactor:      Long   = Attribute.register(TransmissionFactorAlias)

  val IORAlias: String = "IOR"
  val IOR:      Long   = Attribute.register(IORAlias)

  val SpecularFactorAlias: String = "SpecularFactor"
  val SpecularFactor:      Long   = Attribute.register(SpecularFactorAlias)

  def createMetallic(value: Float): Attribute =
    PBRFloatAttribute(Metallic, value)

  def createRoughness(value: Float): Attribute =
    PBRFloatAttribute(Roughness, value)

  def createNormalScale(value: Float): Attribute =
    PBRFloatAttribute(NormalScale, value)

  def createOcclusionStrength(value: Float): Attribute =
    PBRFloatAttribute(OcclusionStrength, value)

  def createEmissiveIntensity(value: Float): Attribute =
    PBRFloatAttribute(EmissiveIntensity, value)

  def createTransmissionFactor(value: Float): Attribute =
    PBRFloatAttribute(TransmissionFactor, value)

  def createIOR(value: Float): Attribute =
    PBRFloatAttribute(IOR, value)

  def createSpecularFactor(value: Float): Attribute =
    PBRFloatAttribute(SpecularFactor, value)
}
