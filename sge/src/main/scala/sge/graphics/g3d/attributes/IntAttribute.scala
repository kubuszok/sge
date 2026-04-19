/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/IntAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful 1:1 port
 *   - compareTo -> compare (Ordered[Attribute])
 *   - Two Java constructors merged into primary constructor with default value = 0
 *   - All constants, factory methods, and instance methods accounted for
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 52
 * Covenant-baseline-methods: CullFace,CullFaceAlias,IntAttribute,compare,copy,createCullFace,hashCode,result,value
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/attributes/IntAttribute.java
 * Covenant-verified: 2026-04-19
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
    IntAttribute(`type`, value)

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
    IntAttribute(CullFace, value)
}
