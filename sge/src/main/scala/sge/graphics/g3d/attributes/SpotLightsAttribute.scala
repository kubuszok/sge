/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/SpotLightsAttribute.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful 1:1 port
 *   - compareTo -> compare (Ordered[Attribute])
 *   - Array<SpotLight> -> DynamicArray[SpotLight]
 *   - null check in hashCode -> Nullable(light).fold(0)(_.hashCode())
 *   - Java Array(1) initial capacity; Scala DynamicArray() default capacity — minor
 *   - compareTo compares lights.size when types match (Java source left as FIXME stub)
 *   - All constants, factory methods, constructors, and instance methods accounted for
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
    SpotLightsAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    for (light <- lights)
      result = 1237 * result + Nullable(light).map(_.hashCode()).getOrElse(0)
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else lights.size - that.asInstanceOf[SpotLightsAttribute].lights.size
}

object SpotLightsAttribute {

  val Alias: String = "spotLights"
  val Type:  Long   = Attribute.register(Alias)

  def is(mask: Long): Boolean =
    (mask & Type) == mask
}
