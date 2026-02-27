/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/PointLightsAttribute.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.g3d.environment.PointLight
import sge.utils.DynamicArray

/** An [[Attribute]] which can be used to send a [[DynamicArray]] of [[PointLight]] instances to the Shader. The lights are stored by reference, the [[copy]] or
  * [[PointLightsAttribute(PointLightsAttribute)]] method will not create new lights.
  * @author
  *   Xoppa (original implementation)
  */
class PointLightsAttribute(
  val lights: DynamicArray[PointLight]
) extends Attribute(PointLightsAttribute.Type) {

  def this() =
    this(DynamicArray[PointLight]())

  def this(copyFrom: PointLightsAttribute) = {
    this()
    lights.addAll(copyFrom.lights)
  }

  override def copy(): PointLightsAttribute =
    new PointLightsAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    for (light <- lights)
      result = 1231 * result + (if (light == null) 0 else light.hashCode())
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else 0 // FIXME implement comparing
}

object PointLightsAttribute {

  val Alias: String = "pointLights"
  val Type:  Long   = Attribute.register(Alias)

  def is(mask: Long): Boolean =
    (mask & Type) == mask
}
