/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRVolumeAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.Color
import sge.graphics.g3d.Attribute
import sge.math.MathUtils

class PBRVolumeAttribute(
  var thicknessFactor: Float,
  /** a value of zero means positive infinity (no attenuation) */
  var attenuationDistance: Float,
  val attenuationColor:    Color
) extends Attribute(PBRVolumeAttribute.Type) {

  def this() =
    this(0f, 0f, Color(Color.WHITE))

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other = that.asInstanceOf[PBRVolumeAttribute]
      if (!MathUtils.isEqual(thicknessFactor, other.thicknessFactor)) {
        if (thicknessFactor < other.thicknessFactor) -1 else 1
      } else if (!MathUtils.isEqual(attenuationDistance, other.attenuationDistance)) {
        if (attenuationDistance < other.attenuationDistance) -1 else 1
      } else {
        attenuationColor.toIntBits() - other.attenuationColor.toIntBits()
      }
    }

  override def copy(): Attribute =
    PBRVolumeAttribute(thicknessFactor, attenuationDistance, attenuationColor)
}

object PBRVolumeAttribute {

  val Alias: String = "volume"
  val Type:  Long   = Attribute.register(Alias)
}
