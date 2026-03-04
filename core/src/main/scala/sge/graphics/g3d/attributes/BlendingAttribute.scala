/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/BlendingAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-03: faithful port, minor null-safety difference
 *   - compareTo -> compare (Ordered[Attribute])
 *   - Java copy-ctor has null-safe fallback (copyFrom==null -> defaults); Scala version
 *     does not accept null — acceptable for no-null convention
 *   - Java no-arg ctor delegates to BlendingAttribute(null) for the null path;
 *     Scala no-arg ctor uses explicit defaults — same runtime behavior
 *   - All constants, factory methods, constructors, and instance methods accounted for
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.GL20
import sge.math.MathUtils
import sge.utils.NumberUtils

class BlendingAttribute(
  /** Whether this material should be considered blended (default: true). This is used for sorting (back to front instead of front to back).
    */
  var blended: Boolean,
  /** Specifies how the (incoming) red, green, blue, and alpha source blending factors are computed (default: GL_SRC_ALPHA) */
  var sourceFunction: Int,
  /** Specifies how the (existing) red, green, blue, and alpha destination blending factors are computed (default: GL_ONE_MINUS_SRC_ALPHA)
    */
  var destFunction: Int,
  /** The opacity used as source alpha value, ranging from 0 (fully transparent) to 1 (fully opaque), (default: 1). */
  var opacity: Float
) extends Attribute(BlendingAttribute.Type) {

  def this() = {
    this(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f)
  }

  def this(sourceFunc: Int, destFunc: Int, opacity: Float) = {
    this(true, sourceFunc, destFunc, opacity)
  }

  def this(sourceFunc: Int, destFunc: Int) = {
    this(true, sourceFunc, destFunc, 1f)
  }

  def this(blended: Boolean, opacity: Float) = {
    this(blended, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity)
  }

  def this(opacity: Float) = {
    this(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity)
  }

  def this(copyFrom: BlendingAttribute) = {
    this(copyFrom.blended, copyFrom.sourceFunction, copyFrom.destFunction, copyFrom.opacity)
  }

  override def copy(): BlendingAttribute =
    new BlendingAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 947 * result + (if (blended) 1 else 0)
    result = 947 * result + sourceFunction
    result = 947 * result + destFunction
    result = 947 * result + NumberUtils.floatToRawIntBits(opacity)
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else {
      val other = that.asInstanceOf[BlendingAttribute]
      if (blended != other.blended) { if (blended) 1 else -1 }
      else if (sourceFunction != other.sourceFunction) sourceFunction - other.sourceFunction
      else if (destFunction != other.destFunction) destFunction - other.destFunction
      else if (MathUtils.isEqual(opacity, other.opacity)) 0
      else if (opacity < other.opacity) 1
      else -1
    }
}

object BlendingAttribute {

  val Alias: String = "blended"
  val Type:  Long   = Attribute.register(Alias)

  def is(mask: Long): Boolean =
    (mask & Type) == mask
}
