/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: DEFAULT_SPEED,SquashEffect,lineHeight,onApply,progress,realSpeed,speed,timePassed,timePassedByGlyphIndex
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/SquashEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class SquashEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_SPEED          = 0.125f
  private var speed                  = 4f; private var elastic = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) speed = 1.0f / paramAsFloat(params(0), 0.25f)
  if (params.length > 1) elastic = paramAsBoolean(params(1))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realSpeed  = speed * (if (elastic) 3f else 1f) * DEFAULT_SPEED
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress   = MathUtils.clamp(timePassed / realSpeed, 0f, 1f)
    val lineHeight = label.getLineHeight(globalIndex)
    if (progress < 0.4f) {
      val interpolatedValue = 1f - Interpolation.sine(progress * 2.5f) * 0.5f
      label.getOffsets.incr(globalIndex << 1, lineHeight * (-0.25f * (1.0f - interpolatedValue)))
      label.getOffsets.incr(globalIndex << 1 | 1, (interpolatedValue - 1f) * 0.5f * lineHeight)
      label.getSizing.incr(globalIndex << 1, 1.0f - interpolatedValue)
      label.getSizing.incr(globalIndex << 1 | 1, interpolatedValue - 1f)
    } else {
      val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
      val interpolatedValue = interpolation((progress - 0.4f) * 1.666f) * 0.5f + 0.5f
      label.getOffsets.incr(globalIndex << 1, lineHeight * (-0.25f * (1.0f - interpolatedValue)))
      label.getOffsets.incr(globalIndex << 1 | 1, (interpolatedValue - 1f) * 0.5f * lineHeight)
      label.getSizing.incr(globalIndex << 1, 1.0f - interpolatedValue)
      label.getSizing.incr(globalIndex << 1 | 1, interpolatedValue - 1f)
    }
  }
}
