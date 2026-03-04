/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/attributes/DepthTestAttribute.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audited 2026-03-03: faithful 1:1 port
 *   - compareTo -> compare (Ordered[Attribute])
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - All constants, constructors, and instance methods accounted for
 */
package sge
package graphics
package g3d
package attributes

import sge.graphics.GL20
import sge.math.MathUtils
import sge.utils.{ NumberUtils, SgeError }

class DepthTestAttribute(
  `type`: Long,
  /** The depth test function, or 0 to disable depth test (default: GL10.GL_LEQUAL) */
  var depthFunc: Int,
  /** Mapping of near clipping plane to window coordinates (default: 0) */
  var depthRangeNear: Float,
  /** Mapping of far clipping plane to window coordinates (default: 1) */
  var depthRangeFar: Float,
  /** Whether to write to the depth buffer (default: true) */
  var depthMask: Boolean
) extends Attribute(`type`) {

  if (!DepthTestAttribute.is(`type`)) throw SgeError.InvalidInput("Invalid type specified")

  def this() = {
    this(DepthTestAttribute.Type, GL20.GL_LEQUAL, 0f, 1f, true)
  }

  def this(depthMask: Boolean) = {
    this(DepthTestAttribute.Type, GL20.GL_LEQUAL, 0f, 1f, depthMask)
  }

  def this(depthFunc: Int) = {
    this(DepthTestAttribute.Type, depthFunc, 0f, 1f, true)
  }

  def this(depthFunc: Int, depthMask: Boolean) = {
    this(DepthTestAttribute.Type, depthFunc, 0f, 1f, depthMask)
  }

  def this(depthFunc: Int, depthRangeNear: Float, depthRangeFar: Float) = {
    this(DepthTestAttribute.Type, depthFunc, depthRangeNear, depthRangeFar, true)
  }

  def this(depthFunc: Int, depthRangeNear: Float, depthRangeFar: Float, depthMask: Boolean) = {
    this(DepthTestAttribute.Type, depthFunc, depthRangeNear, depthRangeFar, depthMask)
  }

  def this(rhs: DepthTestAttribute) = {
    this(rhs.`type`, rhs.depthFunc, rhs.depthRangeNear, rhs.depthRangeFar, rhs.depthMask)
  }

  override def copy(): Attribute =
    new DepthTestAttribute(this)

  override def hashCode(): Int = {
    var result = super.hashCode()
    result = 971 * result + depthFunc
    result = 971 * result + NumberUtils.floatToRawIntBits(depthRangeNear)
    result = 971 * result + NumberUtils.floatToRawIntBits(depthRangeFar)
    result = 971 * result + (if (depthMask) 1 else 0)
    result
  }

  override def compare(that: Attribute): Int =
    if (`type` != that.`type`) (`type` - that.`type`).toInt
    else {
      val other = that.asInstanceOf[DepthTestAttribute]
      if (depthFunc != other.depthFunc) depthFunc - other.depthFunc
      else if (depthMask != other.depthMask) { if (depthMask) -1 else 1 }
      else if (!MathUtils.isEqual(depthRangeNear, other.depthRangeNear)) {
        if (depthRangeNear < other.depthRangeNear) -1 else 1
      } else if (!MathUtils.isEqual(depthRangeFar, other.depthRangeFar)) {
        if (depthRangeFar < other.depthRangeFar) -1 else 1
      } else 0
    }
}

object DepthTestAttribute {

  val Alias: String = "depthStencil"
  val Type:  Long   = Attribute.register(Alias)

  protected var Mask: Long = Type

  def is(mask: Long): Boolean =
    (mask & Mask) != 0
}
