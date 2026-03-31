/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRFlagAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute

class PBRFlagAttribute(
  `type`: Long
) extends Attribute(`type`) {

  override def copy(): Attribute =
    PBRFlagAttribute(`type`)

  override def compare(that: Attribute): Int =
    (`type` - that.`type`).toInt
}

object PBRFlagAttribute {

  val UnlitAlias: String = "unlit"
  val Unlit:      Long   = Attribute.register(UnlitAlias)
}
