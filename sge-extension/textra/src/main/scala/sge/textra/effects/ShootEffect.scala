/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: DEFAULT_FREQUENCY,ShootEffect,onApply,s,underline
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/ShootEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.textra.utils.NoiseUtils

class ShootEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 3f
  private var underline         = true; private var strikethrough = false; private var distance = 0.3f; private var frequency = 1f

  if (params.length > 0) underline = paramAsBoolean(params(0))
  if (params.length > 1) strikethrough = paramAsBoolean(params(1))
  if (params.length > 2) distance = paramAsFloat(params(2), 0.3f)
  if (params.length > 3) frequency = paramAsFloat(params(3), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val s = NoiseUtils.triangleWave((totalTime - localIndex * 0.03f) * frequency * DEFAULT_FREQUENCY)
    if (s > 1f - distance)
      label.setInWorkingLayout(globalIndex, glyph | (if (underline) Font.UNDERLINE else 0L) | (if (strikethrough) Font.STRIKETHROUGH else 0L))
    else
      label.setInWorkingLayout(globalIndex, glyph & (if (underline) ~Font.UNDERLINE else -1L) & (if (strikethrough) ~Font.STRIKETHROUGH else -1L))
  }
}
