/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_FREQUENCY,RainbowEffect,distance,distanceMod,frequency,frequencyMod,lightness,onApply,progress,saturation
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/RainbowEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.textra.utils.ColorUtils

/** Tints the text in a rainbow pattern. */
class RainbowEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE  = 0.975f
  private val DEFAULT_FREQUENCY = 2f

  private var distance   = 1f
  private var frequency  = 1f
  private var saturation = 1f
  private var lightness  = 0.5f

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) frequency = paramAsFloat(params(1), 1f)
  if (params.length > 2) saturation = paramAsFloat(params(2), 1f)
  if (params.length > 3) lightness = paramAsFloat(params(3), 0.5f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val distanceMod  = (1f / distance) * (1f - DEFAULT_DISTANCE)
    val frequencyMod = (1f / frequency) * DEFAULT_FREQUENCY
    val progress     = calculateProgress(frequencyMod, distanceMod * localIndex, pingpong = false)
    label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (ColorUtils.hsl2rgb(progress, saturation, lightness, 1f).toLong << 32))
  }
}
