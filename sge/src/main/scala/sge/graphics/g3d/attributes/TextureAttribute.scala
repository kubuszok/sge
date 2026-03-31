/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/TextureAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful port, all constructors present
 *   - compareTo -> compare (Ordered[Attribute])
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - Java generic ctor (type, TextureDescriptor<T>) → Scala (type, TextureDescriptor[? <: Texture])
 *   - textureDescription.texture assignment uses Nullable() wrapper (no-null convention)
 *   - All 7 alias/type pairs, all 14 create* factory methods, set(region) accounted for
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
    this(`type`, TextureDescriptor[Texture](), 0f, 0f, 1f, 1f, 0)

  def this(`type`: Long, textureDescription: TextureDescriptor[? <: Texture]) = {
    this(`type`)
    this.textureDescription.set(textureDescription)
  }

  def this(`type`: Long, textureDescription: TextureDescriptor[? <: Texture], offsetU: Float, offsetV: Float, scaleU: Float, scaleV: Float) = {
    this(`type`, textureDescription)
    this.offsetU = offsetU
    this.offsetV = offsetV
    this.scaleU = scaleU
    this.scaleV = scaleV
  }

  def this(`type`: Long, texture: Texture) = {
    this(`type`)
    textureDescription.texture = Nullable(texture)
  }

  def this(`type`: Long, region: TextureRegion) = {
    this(`type`)
    set(region)
  }

  def this(copyFrom: TextureAttribute) =
    this(
      copyFrom.`type`,
      copyFrom.textureDescription,
      copyFrom.offsetU,
      copyFrom.offsetV,
      copyFrom.scaleU,
      copyFrom.scaleV,
      copyFrom.uvIndex
    )

  def set(region: TextureRegion): Unit = {
    textureDescription.texture = Nullable(region.texture)
    offsetU = region.u
    offsetV = region.v
    scaleU = region.u2 - offsetU
    scaleV = region.v2 - offsetV
  }

  override def copy(): Attribute =
    TextureAttribute(this)

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

  private[sge] var Mask: Long = Diffuse | Specular | Bump | Normal | Ambient | Emissive | Reflection

  def is(mask: Long): Boolean =
    (mask & Mask) != 0

  def createDiffuse(texture: Texture): TextureAttribute =
    TextureAttribute(Diffuse, texture)

  def createDiffuse(region: TextureRegion): TextureAttribute =
    TextureAttribute(Diffuse, region)

  def createSpecular(texture: Texture): TextureAttribute =
    TextureAttribute(Specular, texture)

  def createSpecular(region: TextureRegion): TextureAttribute =
    TextureAttribute(Specular, region)

  def createNormal(texture: Texture): TextureAttribute =
    TextureAttribute(Normal, texture)

  def createNormal(region: TextureRegion): TextureAttribute =
    TextureAttribute(Normal, region)

  def createBump(texture: Texture): TextureAttribute =
    TextureAttribute(Bump, texture)

  def createBump(region: TextureRegion): TextureAttribute =
    TextureAttribute(Bump, region)

  def createAmbient(texture: Texture): TextureAttribute =
    TextureAttribute(Ambient, texture)

  def createAmbient(region: TextureRegion): TextureAttribute =
    TextureAttribute(Ambient, region)

  def createEmissive(texture: Texture): TextureAttribute =
    TextureAttribute(Emissive, texture)

  def createEmissive(region: TextureRegion): TextureAttribute =
    TextureAttribute(Emissive, region)

  def createReflection(texture: Texture): TextureAttribute =
    TextureAttribute(Reflection, texture)

  def createReflection(region: TextureRegion): TextureAttribute =
    TextureAttribute(Reflection, region)
}
