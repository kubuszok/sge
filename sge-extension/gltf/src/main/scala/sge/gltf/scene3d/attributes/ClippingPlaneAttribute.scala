/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/ClippingPlaneAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute
import sge.math.{ MathUtils, Plane, Vector3 }

class ClippingPlaneAttribute(
  val plane: Plane
) extends Attribute(ClippingPlaneAttribute.Type) {

  def this(normal: Vector3, d: Float) =
    this(Plane(normal, d))

  override def compare(that: Attribute): Int = {
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other       = that.asInstanceOf[ClippingPlaneAttribute]
      val normal      = plane.normal
      val otherNormal = other.plane.normal
      if (!MathUtils.isEqual(normal.x, otherNormal.x)) { if (normal.x < otherNormal.x) -1 else 1 }
      else if (!MathUtils.isEqual(normal.y, otherNormal.y)) { if (normal.y < otherNormal.y) -1 else 1 }
      else if (!MathUtils.isEqual(normal.z, otherNormal.z)) { if (normal.z < otherNormal.z) -1 else 1 }
      else if (!MathUtils.isEqual(plane.d, other.plane.d)) { if (plane.d < other.plane.d) -1 else 1 }
      else 0
    }
  }

  override def copy(): Attribute =
    ClippingPlaneAttribute(plane.normal, plane.d)
}

object ClippingPlaneAttribute {

  val TypeAlias: String = "clippingPlane"
  val Type:      Long   = Attribute.register(TypeAlias)
}
