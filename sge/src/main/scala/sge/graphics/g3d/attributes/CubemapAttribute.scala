/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/CubemapAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful port, all constructors present
 *   - compareTo -> compare (Ordered[Attribute])
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - Java generic ctor (type, TextureDescriptor<T>) erases to same as primary → covered by
 *     primary constructor (type, TextureDescriptor[Cubemap]); copy ctor uses .set() for deep copy
 *   - textureDescription.texture assignment uses Nullable() wrapper (no-null convention)
 *   - All constants, factory methods, constructors, and instance methods accounted for
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: CubemapAttribute,EnvironmentMap,EnvironmentMapAlias,Mask,compare,copy,hashCode,is,result,textureDescription,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/attributes/CubemapAttribute.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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
    this(`type`, TextureDescriptor[Cubemap]())

  def this(`type`: Long, texture: Cubemap) = {
    this(`type`)
    textureDescription.texture = Nullable(texture)
  }

  def this(copyFrom: CubemapAttribute) = {
    this(copyFrom.`type`)
    this.textureDescription.set(copyFrom.textureDescription)
  }

  override def copy(): Attribute =
    CubemapAttribute(this)

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

  private[sge] var Mask: Long = EnvironmentMap

  def is(mask: Long): Boolean =
    (mask & Mask) != 0
}
