/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/TextureAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.graphics.g3d.utils.TextureDescriptor
import sge.math.MathUtils
import sge.utils.{ Nullable, NumberUtils, SgeError }

class TextureAttribute(
  `type`:                 Long,
  val textureDescription: TextureDescriptor[Texture],
  var offsetU:            Float,
  var offsetV:            Float,
  var scaleU:             Float,
  var scaleV:             Float,
  /** The index of the texture coordinate vertex attribute to use for this TextureAttribute. Whether this value is used, depends on the shader and [[Attribute.`type`]] value. For basic (model
    * specific) types (e.g. [[TextureAttribute.Diffuse]], [[TextureAttribute.Normal]], etc.), this value is usually ignored and the first texture coordinate vertex attribute is used.
    */
  var uvIndex: Int
) extends Attribute(`type`) {

  if (!TextureAttribute.is(`type`)) throw SgeError.InvalidInput("Invalid type specified")

  def this(`type`: Long) =
    this(`type`, new TextureDescriptor[Texture](), 0f, 0f, 1f, 1f, 0)

  def this(`type`: Long, texture: Texture) = {
    this(`type`)
    textureDescription.texture = Nullable(texture)
  }

  def this(`type`: Long, region: TextureRegion) = {
    this(`type`)
    set(region)
  }

  def this(copyFrom: TextureAttribute) = {
    this(copyFrom.`type`)
    this.textureDescription.set(copyFrom.textureDescription)
    this.offsetU = copyFrom.offsetU
    this.offsetV = copyFrom.offsetV
    this.scaleU = copyFrom.scaleU
    this.scaleV = copyFrom.scaleV
    this.uvIndex = copyFrom.uvIndex
  }

  def set(region: TextureRegion): Unit = {
    textureDescription.texture = Nullable(region.getTexture())
    offsetU = region.getU()
    offsetV = region.getV()
    scaleU = region.getU2() - offsetU
    scaleV = region.getV2() - offsetV
  }

  override def copy(): Attribute =
    new TextureAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 991 * result + textureDescription.hashCode()
    result = 991 * result + NumberUtils.floatToRawIntBits(offsetU)
    result = 991 * result + NumberUtils.floatToRawIntBits(offsetV)
    result = 991 * result + NumberUtils.floatToRawIntBits(scaleU)
    result = 991 * result + NumberUtils.floatToRawIntBits(scaleV)
    result = 991 * result + uvIndex
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other = that.asInstanceOf[TextureAttribute]
      val c     = textureDescription.compareTo(other.textureDescription)
      if (c != 0) c
      else if (uvIndex != other.uvIndex) uvIndex - other.uvIndex
      else if (!MathUtils.isEqual(scaleU, other.scaleU)) { if (scaleU > other.scaleU) 1 else -1 }
      else if (!MathUtils.isEqual(scaleV, other.scaleV)) { if (scaleV > other.scaleV) 1 else -1 }
      else if (!MathUtils.isEqual(offsetU, other.offsetU)) { if (offsetU > other.offsetU) 1 else -1 }
      else if (!MathUtils.isEqual(offsetV, other.offsetV)) { if (offsetV > other.offsetV) 1 else -1 }
      else 0
    }
}

object TextureAttribute {

  val DiffuseAlias:    String = "diffuseTexture"
  val Diffuse:         Long   = Attribute.register(DiffuseAlias)
  val SpecularAlias:   String = "specularTexture"
  val Specular:        Long   = Attribute.register(SpecularAlias)
  val BumpAlias:       String = "bumpTexture"
  val Bump:            Long   = Attribute.register(BumpAlias)
  val NormalAlias:     String = "normalTexture"
  val Normal:          Long   = Attribute.register(NormalAlias)
  val AmbientAlias:    String = "ambientTexture"
  val Ambient:         Long   = Attribute.register(AmbientAlias)
  val EmissiveAlias:   String = "emissiveTexture"
  val Emissive:        Long   = Attribute.register(EmissiveAlias)
  val ReflectionAlias: String = "reflectionTexture"
  val Reflection:      Long   = Attribute.register(ReflectionAlias)

  protected var Mask: Long = Diffuse | Specular | Bump | Normal | Ambient | Emissive | Reflection

  def is(mask: Long): Boolean =
    (mask & Mask) != 0

  def createDiffuse(texture: Texture): TextureAttribute =
    new TextureAttribute(Diffuse, texture)

  def createDiffuse(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Diffuse, region)

  def createSpecular(texture: Texture): TextureAttribute =
    new TextureAttribute(Specular, texture)

  def createSpecular(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Specular, region)

  def createNormal(texture: Texture): TextureAttribute =
    new TextureAttribute(Normal, texture)

  def createNormal(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Normal, region)

  def createBump(texture: Texture): TextureAttribute =
    new TextureAttribute(Bump, texture)

  def createBump(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Bump, region)

  def createAmbient(texture: Texture): TextureAttribute =
    new TextureAttribute(Ambient, texture)

  def createAmbient(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Ambient, region)

  def createEmissive(texture: Texture): TextureAttribute =
    new TextureAttribute(Emissive, texture)

  def createEmissive(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Emissive, region)

  def createReflection(texture: Texture): TextureAttribute =
    new TextureAttribute(Reflection, texture)

  def createReflection(region: TextureRegion): TextureAttribute =
    new TextureAttribute(Reflection, region)
}
