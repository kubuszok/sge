/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: AttentionEffect,distance,onApply,spread
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/AttentionEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.math.MathUtils

class AttentionEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var spread = 5f; private var stretchY = 2f
  label.trackingInput = true
  if (params.length > 0) spread = paramAsFloat(params(0), 100.0f) * 0.01f
  if (params.length > 1) stretchY = paramAsFloat(params(1), 100.0f) * 0.01f

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val distance = Math.abs(globalIndex - label.overIndex)
    if (label.overIndex >= 0 && distance <= spread)
      label.getSizing.incr(globalIndex << 1 | 1, (stretchY - 1f) * MathUtils.cosDeg((90f * distance) / spread))
  }
}
