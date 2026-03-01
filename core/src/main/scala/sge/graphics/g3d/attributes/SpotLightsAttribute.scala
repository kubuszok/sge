/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/SpotLightsAttribute.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.g3d.environment.SpotLight
import sge.utils.DynamicArray
import sge.utils.Nullable

/** An [[Attribute]] which can be used to send a [[DynamicArray]] of [[SpotLight]] instances to the Shader. The lights are stored by reference, the [[copy]] or
  * [[SpotLightsAttribute(SpotLightsAttribute)]] method will not create new lights.
  * @author
  *   Xoppa (original implementation)
  */
class SpotLightsAttribute(
  val lights: DynamicArray[SpotLight]
) extends Attribute(SpotLightsAttribute.Type) {

  def this() = {
    this(DynamicArray[SpotLight]())
  }

  def this(copyFrom: SpotLightsAttribute) = {
    this()
    lights.addAll(copyFrom.lights)
  }

  override def copy(): SpotLightsAttribute =
    new SpotLightsAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    for (light <- lights)
      result = 1237 * result + Nullable(light).fold(0)(_.hashCode())
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else 0 // FIXME implement comparing
}

object SpotLightsAttribute {

  val Alias: String = "spotLights"
  val Type:  Long   = Attribute.register(Alias)

  def is(mask: Long): Boolean =
    (mask & Type) == mask
}
