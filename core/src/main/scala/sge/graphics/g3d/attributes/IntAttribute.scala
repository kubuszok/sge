/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/IntAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package attributes

class IntAttribute(
  `type`:    Long,
  var value: Int = 0
) extends Attribute(`type`) {

  override def copy(): Attribute =
    new IntAttribute(`type`, value)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 983 * result + value
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else value - that.asInstanceOf[IntAttribute].value
}

object IntAttribute {

  val CullFaceAlias: String = "cullface"
  val CullFace:      Long   = Attribute.register(CullFaceAlias)

  /** create a cull face attribute to be used in a material
    * @param value
    *   cull face value, possible values are 0 (render both faces), GL_FRONT_AND_BACK (render nothing), GL_BACK (render front faces only), GL_FRONT (render back faces only), or -1 to inherit default
    * @return
    *   an attribute
    */
  def createCullFace(value: Int): IntAttribute =
    new IntAttribute(CullFace, value)
}
