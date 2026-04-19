/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRFlagAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: PBRFlagAttribute,Unlit,UnlitAlias,compare,copy
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
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
