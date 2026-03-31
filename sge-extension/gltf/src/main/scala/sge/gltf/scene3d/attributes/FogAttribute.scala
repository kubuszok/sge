/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/FogAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute
import sge.math.{ MathUtils, Vector3 }

class FogAttribute(
  `type`: Long
) extends Attribute(`type`) {

  val value: Vector3 = Vector3()

  def set(value: Vector3): FogAttribute = {
    this.value.set(value)
    this
  }

  def set(near: Float, far: Float, exponent: Float): FogAttribute = {
    this.value.set(near, far, exponent)
    this
  }

  override def copy(): Attribute =
    FogAttribute(`type`).set(value)

  override def compare(that: Attribute): Int = {
    if (`type` != that.`type`) { if (`type` < that.`type`) -1 else 1 }
    else {
      val other = that.asInstanceOf[FogAttribute]
      if (!MathUtils.isEqual(value.x, other.value.x)) { if (value.x < other.value.x) -1 else 1 }
      else if (!MathUtils.isEqual(value.y, other.value.y)) { if (value.y < other.value.y) -1 else 1 }
      else if (!MathUtils.isEqual(value.z, other.value.z)) { if (value.z < other.value.z) -1 else 1 }
      else 0
    }
  }
}

object FogAttribute {

  val FogEquationAlias: String = "fogEquation"
  val FogEquation:      Long   = Attribute.register(FogEquationAlias)

  def createFog(near: Float, far: Float, exponent: Float): FogAttribute =
    FogAttribute(FogEquation).set(near, far, exponent)
}
