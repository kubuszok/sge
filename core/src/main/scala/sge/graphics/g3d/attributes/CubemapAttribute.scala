/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/CubemapAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.Cubemap
import sge.graphics.g3d.utils.TextureDescriptor
import sge.utils.{ Nullable, SgeError }

class CubemapAttribute(
  `type`:                 Long,
  val textureDescription: TextureDescriptor[Cubemap]
) extends Attribute(`type`) {

  if (!CubemapAttribute.is(`type`)) throw SgeError.InvalidInput("Invalid type specified")

  def this(`type`: Long) =
    this(`type`, new TextureDescriptor[Cubemap]())

  def this(`type`: Long, texture: Cubemap) = {
    this(`type`)
    textureDescription.texture = Nullable(texture)
  }

  def this(copyFrom: CubemapAttribute) = {
    this(copyFrom.`type`)
    this.textureDescription.set(copyFrom.textureDescription)
  }

  override def copy(): Attribute =
    new CubemapAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 967 * result + textureDescription.hashCode()
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else textureDescription.compareTo(that.asInstanceOf[CubemapAttribute].textureDescription)
}

object CubemapAttribute {

  val EnvironmentMapAlias: String = "environmentCubemap"
  val EnvironmentMap:      Long   = Attribute.register(EnvironmentMapAlias)

  protected var Mask: Long = EnvironmentMap

  def is(mask: Long): Boolean =
    (mask & Mask) != 0
}
