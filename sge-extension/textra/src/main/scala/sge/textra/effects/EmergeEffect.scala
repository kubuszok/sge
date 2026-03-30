/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class EmergeEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_SPEED          = 0.125f
  private var speed                  = 4f; private var elastic = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) speed = 1.0f / paramAsFloat(params(0), 0.25f)
  if (params.length > 1) elastic = paramAsBoolean(params(1))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realIntensity = speed * (if (elastic) 3f else 1f) * DEFAULT_SPEED
    val timePassed    = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realIntensity, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
    val interpolatedValue = interpolation(progress) - 1f
    label.getSizing.incr(globalIndex << 1 | 1, interpolatedValue)
    label.getOffsets.incr(globalIndex << 1 | 1, interpolatedValue * 0.5f * label.getLineHeight(globalIndex))
  }
}
