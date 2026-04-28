/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 21
 * Covenant-baseline-methods: DEFAULT_DISTANCE,SlipEffect,distance,onApply,slip
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/SlipEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.textra.utils.NoiseUtils

class SlipEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE = 0.5f; private val DEFAULT_SPEED = 0.001f
  private var distance         = 5f; private var speed           = 1f

  if (params.length > 0) distance = paramAsFloat(params(0), 5f)
  if (params.length > 1) speed = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    var slip = (1f + NoiseUtils.octaveNoise1D((System.currentTimeMillis() & 0xffffff) * speed * DEFAULT_SPEED + globalIndex * 0.2357f, 0x12345678)) * distance * DEFAULT_DISTANCE
    slip *= calculateFadeout()
    label.getOffsets.incr(globalIndex << 1, slip)
  }
}
