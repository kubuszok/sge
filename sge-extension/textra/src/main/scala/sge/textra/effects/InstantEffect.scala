/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 9
 * Covenant-baseline-methods: InstantEffect,onApply
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/InstantEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

class InstantEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    if (indexEnd >= 0) label.setTextSpeed(TypingConfig.DEFAULT_SPEED_PER_CHAR) else label.setTextSpeed(0f)
}
