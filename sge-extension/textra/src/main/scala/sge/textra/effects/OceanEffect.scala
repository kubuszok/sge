/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: DEFAULT_DISTANCE,OceanEffect,distance,distanceMod,frequencyMod,onApply,progress
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/OceanEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.textra.utils.{ ColorUtils, NoiseUtils }

class OceanEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE = 0.975f; private val DEFAULT_FREQUENCY = 2f
  private var distance         = 1f; private var frequency             = 0.25f; private var hue = 0.5f; private var saturation = 0.8f; private var lightness = 0.25f

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) frequency = paramAsFloat(params(1), 0.25f)
  if (params.length > 2) hue = paramAsFloat(params(2), 0.5f)
  if (params.length > 3) saturation = paramAsFloat(params(3), 0.8f)
  if (params.length > 4) lightness = paramAsFloat(params(4), 0.25f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val distanceMod  = (1f / distance) * (1f - DEFAULT_DISTANCE)
    val frequencyMod = (1f / frequency) * DEFAULT_FREQUENCY
    val progress     = calculateProgress(frequencyMod, distanceMod * localIndex, pingpong = false)
    label.setInWorkingLayout(
      globalIndex,
      (glyph & 0xffffffffL) |
        (ColorUtils
          .hsl2rgb(
            NoiseUtils.octaveNoise1D(progress * 5f, 12345) * 0.15f + hue,
            saturation,
            0.15f - Math.abs(NoiseUtils.noise1D(progress * 3f + progress * progress, -123456789)) * 0.3f + lightness,
            1f
          )
          .toLong << 32)
    )
  }
}
