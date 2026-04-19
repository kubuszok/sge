/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: DEFAULT_STRENGTH,PinchEffect,determineFloat,onApply,s,strength,time
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/PinchEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.Interpolation

class PinchEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_STRENGTH = 0.5f; private val DEFAULT_LIKELIHOOD     = 0.1f
  private var strength         = DEFAULT_STRENGTH; private var likelihood = DEFAULT_LIKELIHOOD; private var elastic = false

  if (params.length > 0) strength = paramAsFloat(params(0), 0.5f)
  if (params.length > 1) duration = paramAsFloat(params(1), Float.PositiveInfinity)
  if (params.length > 2) likelihood = paramAsFloat(params(2), DEFAULT_LIKELIHOOD)
  if (params.length > 3) elastic = paramAsBoolean(params(3))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val time = System.currentTimeMillis()
    if (likelihood > determineFloat((time >>> 10) * globalIndex + localIndex)) {
      val lineHeight = label.getLineHeight(globalIndex)
      val progress   = (time & 1023) * 9.765625e-4f
      val fadeout    = calculateFadeout() * strength
      if (progress < 0.4f) {
        val interpolatedValue = 1f - Interpolation.sine(progress * 2.5f) * fadeout
        label.getOffsets.incr(globalIndex << 1, lineHeight * (0.25f * (1.0f - interpolatedValue)))
        label.getOffsets.incr(globalIndex << 1 | 1, (interpolatedValue - 1f) * 0.5f * lineHeight)
        label.getSizing.incr(globalIndex << 1, interpolatedValue - 1f)
        label.getSizing.incr(globalIndex << 1 | 1, 1.0f - interpolatedValue)
      } else {
        val interpolation     = if (elastic) Interpolation.elasticOut else Interpolation.sine
        val interpolatedValue = interpolation((progress - 0.4f) * 1.666f) * fadeout + 1f - fadeout
        label.getOffsets.incr(globalIndex << 1, lineHeight * (0.25f * (1.0f - interpolatedValue)))
        label.getOffsets.incr(globalIndex << 1 | 1, (interpolatedValue - 1f) * 0.5f * lineHeight)
        label.getSizing.incr(globalIndex << 1, interpolatedValue - 1f)
        label.getSizing.incr(globalIndex << 1 | 1, 1.0f - interpolatedValue)
      }
    }
  }

  private def determineFloat(state: Long): Float = {
    var s = state
    s = ((s * 0x632be59bd9b4e019L) ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L
    (((s ^ s >>> 27) * 0xaef17502108ef2d9L) >>> 40) * 5.9604645e-8f
  }
}
