/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRHDRColorAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * HDR Color attribute only contains RGB values with High Dynamic Range.
 * RGB values are not clamped to 0-1 range.
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute

class PBRHDRColorAttribute(
  `type`: Long,
  var r:  Float,
  var g:  Float,
  var b:  Float
) extends Attribute(`type`) {

  def set(r: Float, g: Float, b: Float): PBRHDRColorAttribute = {
    this.r = r
    this.g = g
    this.b = b
    this
  }

  override def copy(): Attribute =
    PBRHDRColorAttribute(`type`, r, g, b)

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else {
      val a  = that.asInstanceOf[PBRHDRColorAttribute]
      val cr = java.lang.Float.compare(r, a.r)
      if (cr != 0) cr
      else {
        val cg = java.lang.Float.compare(g, a.g)
        if (cg != 0) cg
        else java.lang.Float.compare(b, a.b)
      }
    }
}

object PBRHDRColorAttribute {

  val SpecularAlias: String = "specularColorHDR"
  val Specular:      Long   = Attribute.register(SpecularAlias)
}
