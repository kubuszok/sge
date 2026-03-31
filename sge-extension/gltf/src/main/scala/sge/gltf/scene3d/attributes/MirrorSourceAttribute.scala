/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/MirrorSourceAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.Texture
import sge.graphics.g3d.Attribute
import sge.graphics.g3d.utils.TextureDescriptor
import sge.math.{ MathUtils, Vector3 }

class MirrorSourceAttribute extends Attribute(MirrorSourceAttribute.Type) {

  val textureDescription: TextureDescriptor[Texture] = TextureDescriptor[Texture]()
  val normal:             Vector3                    = Vector3()

  def set(textureDescription: TextureDescriptor[Texture], normal: Vector3): MirrorSourceAttribute = {
    this.textureDescription.set(textureDescription)
    this.normal.set(normal)
    this
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other = that.asInstanceOf[MirrorSourceAttribute]
      val c     = textureDescription.compareTo(other.textureDescription)
      if (c != 0) c
      else {
        val otherNormal = other.normal
        if (!MathUtils.isEqual(normal.x, otherNormal.x)) { if (normal.x < otherNormal.x) -1 else 1 }
        else if (!MathUtils.isEqual(normal.y, otherNormal.y)) { if (normal.y < otherNormal.y) -1 else 1 }
        else if (!MathUtils.isEqual(normal.z, otherNormal.z)) { if (normal.z < otherNormal.z) -1 else 1 }
        else 0
      }
    }

  override def copy(): Attribute =
    MirrorSourceAttribute().set(textureDescription, normal)
}

object MirrorSourceAttribute {

  val TypeAlias: String = "mirrorSource"
  val Type:      Long   = Attribute.register(TypeAlias)
}
