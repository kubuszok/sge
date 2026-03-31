/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRIridescenceAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute
import sge.math.MathUtils

class PBRIridescenceAttribute(
  var factor:       Float,
  var ior:          Float,
  var thicknessMin: Float,
  var thicknessMax: Float
) extends Attribute(PBRIridescenceAttribute.Type) {

  def this() =
    this(1f, 1.3f, 100f, 400f)

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other = that.asInstanceOf[PBRIridescenceAttribute]
      if (!MathUtils.isEqual(factor, other.factor)) { if (factor < other.factor) -1 else 1 }
      else if (!MathUtils.isEqual(ior, other.ior)) { if (ior < other.ior) -1 else 1 }
      else if (!MathUtils.isEqual(thicknessMin, other.thicknessMin)) { if (thicknessMin < other.thicknessMin) -1 else 1 }
      else if (!MathUtils.isEqual(thicknessMax, other.thicknessMax)) { if (thicknessMax < other.thicknessMax) -1 else 1 }
      else 0
    }

  override def copy(): Attribute =
    PBRIridescenceAttribute(factor, ior, thicknessMin, thicknessMax)
}

object PBRIridescenceAttribute {

  val Alias: String = "iridescence"
  val Type:  Long   = Attribute.register(Alias)
}
