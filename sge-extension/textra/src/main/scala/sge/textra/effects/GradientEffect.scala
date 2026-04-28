/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_FREQUENCY,GradientEffect,color1,color2,distance,distanceMod,frequency,frequencyMod,onApply,progress
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/GradientEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.textra.utils.ColorUtils

/** Tints the text in a gradient pattern; never ends. */
class GradientEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE  = 0.975f
  private val DEFAULT_FREQUENCY = 2f
  private var color1            = 0xffffffff
  private var color2            = 0x888888ff
  private var distance          = 1f
  private var frequency         = 1f

  if (params.length > 0) { val c = paramAsColor(params(0)); if (c != 256) color1 = c }
  if (params.length > 1) { val c = paramAsColor(params(1)); if (c != 256) color2 = c }
  if (params.length > 2) distance = paramAsFloat(params(2), 1f)
  if (params.length > 3) frequency = paramAsFloat(params(3), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val distanceMod  = (1f / distance) * (1f - DEFAULT_DISTANCE)
    val frequencyMod = (1f / frequency) * DEFAULT_FREQUENCY
    val progress     = calculateProgress(frequencyMod, distanceMod * localIndex, pingpong = true)
    label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (ColorUtils.lerpColors(color1, color2, progress).toLong << 32))
  }
}
