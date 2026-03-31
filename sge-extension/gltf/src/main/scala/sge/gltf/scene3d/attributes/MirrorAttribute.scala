/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/MirrorAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute

class MirrorAttribute(
  `type`: Long
) extends Attribute(`type`) {

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else 0

  override def copy(): Attribute =
    MirrorAttribute(`type`)
}

object MirrorAttribute {

  val SpecularAlias: String = "specularMirror"
  val Specular:      Long   = Attribute.register(SpecularAlias)

  def createSpecular(): MirrorAttribute =
    MirrorAttribute(Specular)
}
