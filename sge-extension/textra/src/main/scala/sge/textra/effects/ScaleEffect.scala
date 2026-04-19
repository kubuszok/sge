/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: ScaleEffect,onApply,sizeX
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/ScaleEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

class ScaleEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var sizeX = 0.75f; private var sizeY = 2f

  if (params.length > 0) { sizeX = paramAsFloat(params(0), 100.0f) * 0.01f; sizeY = sizeX }
  if (params.length > 1) sizeY = paramAsFloat(params(1), 100.0f) * 0.01f

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    label.getSizing.incr(globalIndex << 1, sizeX - 1f)
    label.getSizing.incr(globalIndex << 1 | 1, sizeY - 1f)
    label.getOffsets.incr(globalIndex << 1 | 1, label.getLineHeight(globalIndex) * -0.5f * (sizeY - 1f))
    label.getAdvances.incr(globalIndex, sizeX - 1f)
  }
}
