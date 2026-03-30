/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 */
package sge
package textra
package effects

import sge.math.Interpolation

/** Moves the text vertically in a sine wave pattern. */
class WaveEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 15f
  private val DEFAULT_DISTANCE  = 0.33f
  private val DEFAULT_SPEED     = 0.5f

  private var distance  = 1f
  private var frequency = 1f
  private var speed     = 1f

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) frequency = paramAsFloat(params(1), 1f)
  if (params.length > 2) speed = paramAsFloat(params(2), 1f)
  if (params.length > 3) duration = paramAsFloat(params(3), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val progressModifier = (1f / speed) * DEFAULT_SPEED
    val normalFrequency  = (1f / frequency) * DEFAULT_FREQUENCY
    val progressOffset   = localIndex / normalFrequency
    val progress         = calculateProgress(progressModifier, progressOffset)
    var y                = label.getLineHeight(globalIndex) * distance * Interpolation.sine(1f, -1f, progress) * DEFAULT_DISTANCE
    y *= calculateFadeout()
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
