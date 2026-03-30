/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class ShrinkEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_EXPANSION      = 3f; private val DEFAULT_EXTENT = 0.15f
  private var expansion              = 1f; private var extent         = 1f; private var elastic = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) expansion = paramAsFloat(params(0), 1.0f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1.0f)
  if (params.length > 2) elastic = paramAsBoolean(params(2))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * (if (elastic) 3f else 1f) * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
    val interpolatedValue = interpolation(expansion * DEFAULT_EXPANSION, 0f, progress)
    label.getSizing.incr(globalIndex << 1, interpolatedValue)
    label.getSizing.incr(globalIndex << 1 | 1, interpolatedValue)
  }
}
