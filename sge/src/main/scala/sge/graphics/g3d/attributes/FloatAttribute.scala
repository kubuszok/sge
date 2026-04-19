/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/FloatAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-04: faithful 1:1 port
 *   - compareTo -> compare (Ordered[Attribute])
 *   - Two Java constructors merged into primary constructor with default value = 0f
 *   - All constants, factory methods, and instance methods accounted for
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: AlphaTest,AlphaTestAlias,FloatAttribute,Shininess,ShininessAlias,compare,copy,createAlphaTest,createShininess,hashCode,result,value
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/attributes/FloatAttribute.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package attributes

import sge.math.MathUtils
import sge.utils.NumberUtils

class FloatAttribute(
  `type`:    Long,
  var value: Float = 0f
) extends Attribute(`type`) {

  override def copy(): Attribute =
    FloatAttribute(`type`, value)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 977 * result + NumberUtils.floatToRawIntBits(value)
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else {
      val v = that.asInstanceOf[FloatAttribute].value
      if (MathUtils.isEqual(value, v)) 0 else if (value < v) -1 else 1
    }
}

object FloatAttribute {

  val ShininessAlias: String = "shininess"
  val Shininess:      Long   = Attribute.register(ShininessAlias)

  def createShininess(value: Float): FloatAttribute =
    FloatAttribute(Shininess, value)

  val AlphaTestAlias: String = "alphaTest"
  val AlphaTest:      Long   = Attribute.register(AlphaTestAlias)

  def createAlphaTest(value: Float): FloatAttribute =
    FloatAttribute(AlphaTest, value)
}
