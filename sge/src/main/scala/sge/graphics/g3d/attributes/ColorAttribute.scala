/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/ColorAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful 1:1 port
 *   - compareTo -> compare (Ordered[Attribute])
 *   - Java (type, color) ctor has null guard (`if (color != null)`); Scala version
 *     always calls color.set — acceptable for no-null convention
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - All 7 alias/type pairs, all 14 create* factory methods, all constructors accounted for
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 123
 * Covenant-baseline-methods: Ambient,AmbientAlias,AmbientLight,AmbientLightAlias,ColorAttribute,Diffuse,DiffuseAlias,Emissive,EmissiveAlias,Fog,FogAlias,Mask,Reflection,ReflectionAlias,Specular,SpecularAlias,color,compare,copy,createAmbient,createAmbientLight,createDiffuse,createEmissive,createFog,createReflection,createSpecular,hashCode,is,result,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/attributes/ColorAttribute.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.Color
import sge.utils.SgeError

class ColorAttribute(
  `type`: Long
) extends Attribute(`type`) {

  if (!ColorAttribute.is(`type`)) throw SgeError.InvalidInput("Invalid type specified")

  val color: Color = Color()

  def this(`type`: Long, color: Color) = {
    this(`type`)
    this.color.set(color)
  }

  def this(`type`: Long, r: Float, g: Float, b: Float, a: Float) = {
    this(`type`)
    this.color.set(r, g, b, a)
  }

  def this(copyFrom: ColorAttribute) =
    this(copyFrom.`type`, copyFrom.color)

  override def copy(): Attribute =
    ColorAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 953 * result + color.toIntBits()
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else that.asInstanceOf[ColorAttribute].color.toIntBits() - color.toIntBits()
}

object ColorAttribute {

  val DiffuseAlias:      String = "diffuseColor"
  val Diffuse:           Long   = Attribute.register(DiffuseAlias)
  val SpecularAlias:     String = "specularColor"
  val Specular:          Long   = Attribute.register(SpecularAlias)
  val AmbientAlias:      String = "ambientColor"
  val Ambient:           Long   = Attribute.register(AmbientAlias)
  val EmissiveAlias:     String = "emissiveColor"
  val Emissive:          Long   = Attribute.register(EmissiveAlias)
  val ReflectionAlias:   String = "reflectionColor"
  val Reflection:        Long   = Attribute.register(ReflectionAlias)
  val AmbientLightAlias: String = "ambientLightColor"
  val AmbientLight:      Long   = Attribute.register(AmbientLightAlias)
  val FogAlias:          String = "fogColor"
  val Fog:               Long   = Attribute.register(FogAlias)

  private[sge] var Mask: Long = Ambient | Diffuse | Specular | Emissive | Reflection | AmbientLight | Fog

  def is(mask: Long): Boolean =
    (mask & Mask) != 0

  def createAmbient(color: Color): ColorAttribute =
    ColorAttribute(Ambient, color)

  def createAmbient(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Ambient, r, g, b, a)

  def createDiffuse(color: Color): ColorAttribute =
    ColorAttribute(Diffuse, color)

  def createDiffuse(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Diffuse, r, g, b, a)

  def createSpecular(color: Color): ColorAttribute =
    ColorAttribute(Specular, color)

  def createSpecular(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Specular, r, g, b, a)

  def createReflection(color: Color): ColorAttribute =
    ColorAttribute(Reflection, color)

  def createReflection(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Reflection, r, g, b, a)

  def createEmissive(color: Color): ColorAttribute =
    ColorAttribute(Emissive, color)

  def createEmissive(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Emissive, r, g, b, a)

  def createAmbientLight(color: Color): ColorAttribute =
    ColorAttribute(AmbientLight, color)

  def createAmbientLight(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(AmbientLight, r, g, b, a)

  def createFog(color: Color): ColorAttribute =
    ColorAttribute(Fog, color)

  def createFog(r: Float, g: Float, b: Float, a: Float): ColorAttribute =
    ColorAttribute(Fog, r, g, b, a)
}
