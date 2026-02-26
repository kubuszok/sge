/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/DirectionalLightsAttribute.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

import scala.collection.mutable.ArrayBuffer

import sge.graphics.g3d.environment.DirectionalLight

/** An [[Attribute]] which can be used to send an [[ArrayBuffer]] of [[DirectionalLight]] instances to the Shader. The lights are stored by reference, the [[copy]] or
  * [[DirectionalLightsAttribute(DirectionalLightsAttribute)]] method will not create new lights.
  * @author
  *   Xoppa (original implementation)
  */
class DirectionalLightsAttribute(
  val lights: ArrayBuffer[DirectionalLight]
) extends Attribute(DirectionalLightsAttribute.Type) {

  def this() =
    this(ArrayBuffer.empty[DirectionalLight])

  def this(copyFrom: DirectionalLightsAttribute) = {
    this()
    lights ++= copyFrom.lights
  }

  override def copy(): DirectionalLightsAttribute =
    new DirectionalLightsAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    for (light <- lights)
      result = 1229 * result + (if (light == null) 0 else light.hashCode())
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else 0 // FIXME implement comparing
}

object DirectionalLightsAttribute {

  val Alias: String = "directionalLights"
  val Type:  Long   = Attribute.register(Alias)

  def is(mask: Long): Boolean =
    (mask & Type) == mask
}
