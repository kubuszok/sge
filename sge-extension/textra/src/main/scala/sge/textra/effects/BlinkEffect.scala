/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: BlinkEffect,DEFAULT_FREQUENCY,alpha1,color1,frequency,frequencyMod,onApply,progress
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/BlinkEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

/** Blinks the entire text in two different colors at once, without interpolation. */
class BlinkEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 1f
  private var color1            = 256; private var color2   = 256
  private var alpha1            = 1f; private var alpha2    = 0f
  private var frequency         = 1f; private var threshold = 0.5f

  if (params.length > 0) { color1 = paramAsColor(params(0)); if (color1 == 256) alpha1 = paramAsFloat(params(0), 0f) }
  if (params.length > 1) { color2 = paramAsColor(params(1)); if (color2 == 256) alpha2 = paramAsFloat(params(1), 1f) }
  if (params.length > 2) frequency = paramAsFloat(params(2), 1f)
  if (params.length > 3) threshold = paramAsFloat(params(3), 0.5f)
  threshold = Math.min(Math.max(threshold, 0f), 1f)
  alpha1 = Math.min(Math.max(alpha1, 0f), 1f)
  alpha2 = Math.min(Math.max(alpha2, 0f), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val frequencyMod = (1f / frequency) * DEFAULT_FREQUENCY
    val progress     = calculateProgress(frequencyMod)
    if (progress <= threshold) {
      if (color1 == 256) label.setInWorkingLayout(globalIndex, (glyph & 0xffffff00ffffffffL) | ((alpha1 * 255).toLong << 32))
      else label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (color1.toLong << 32))
    } else {
      if (color1 == 256) label.setInWorkingLayout(globalIndex, (glyph & 0xffffff00ffffffffL) | ((alpha2 * 255).toLong << 32))
      else label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (color2.toLong << 32))
    }
  }
}
