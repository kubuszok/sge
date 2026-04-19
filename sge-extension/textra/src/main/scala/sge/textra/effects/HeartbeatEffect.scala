/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: DEFAULT_FREQUENCY,HeartbeatEffect,c,expansion,onApply,progress,x,xOff
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/HeartbeatEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.MathUtils

class HeartbeatEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 1f; private val DEFAULT_EXPANSION = 0.5f
  private var expansion         = 1f; private var frequency         = 1f

  if (params.length > 0) expansion = paramAsFloat(params(0), 1f)
  if (params.length > 1) frequency = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val progress = totalTime * frequency * 360.0f * DEFAULT_FREQUENCY
    val c        = MathUtils.cosDeg(progress); val s = MathUtils.sinDeg(progress)
    var x        = expansion * Math.max(-0.125f, Math.max(c * c * c, s * s * s)) * DEFAULT_EXPANSION
    x *= calculateFadeout()
    label.getSizing.incr(globalIndex << 1, x)
    label.getSizing.incr(globalIndex << 1 | 1, x)
    val xOff = label.getLineHeight(globalIndex) * -0.5f * x
    label.getOffsets.incr(globalIndex << 1, xOff * 0.5f)
    label.getOffsets.incr(globalIndex << 1 | 1, xOff)
  }
}
