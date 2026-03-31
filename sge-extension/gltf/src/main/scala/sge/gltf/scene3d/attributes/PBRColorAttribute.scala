/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRColorAttribute.java
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
import sge.graphics.g3d.attributes.ColorAttribute

class PBRColorAttribute(
  `type`: Long,
  color:  Color
) extends ColorAttribute(`type`, color) {

  override def copy(): Attribute =
    PBRColorAttribute(`type`, this.color)
}

object PBRColorAttribute {

  val BaseColorFactorAlias: String = "BaseColorFactor"
  val BaseColorFactor:      Long   = Attribute.register(BaseColorFactorAlias)

  // Extend the Mask so ColorAttribute.is() recognizes PBR types
  ColorAttribute.Mask |= BaseColorFactor

  def createBaseColorFactor(color: Color): PBRColorAttribute =
    PBRColorAttribute(BaseColorFactor, color)
}
