/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 */
package sge
package textra
package effects

import sge.math.Interpolation

/** Moves the text vertically in a sine wave pattern.
  *
  * Parameters: `distance;frequency;speed;duration`
  *
  * The `distance` is how many line-heights each glyph should move up and down by; defaults to 1.0 . The `frequency` is how often the glyphs should rise and fall, in a wave; defaults to 1.0 . The
  * `speed` is how quickly each glyph should move; defaults to 1.0 . The `duration` is how many seconds the wave should go on, or `_` to repeat forever; defaults to positive infinity.
  *
  * Example usage:
  * {{{
  * {WAVE=0.5;1.5;0.8;_}Each glyph here will rise/fall a little and with slower movement, but more often; the wave will go on forever.{ENDWAVE}
  * {WAVE=2.5;0.25;1.0;5}Each glyph here will rise/fall a lot, infrequently, at normal speed, for 5 seconds total.{ENDWAVE}
  * }}}
  */
class WaveEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 15f
  private val DEFAULT_DISTANCE  = 0.33f
  private val DEFAULT_SPEED     = 0.5f

  private var distance  = 1f // How much of their height they should move
  private var frequency = 1f // How frequently the wave pattern repeats
  private var speed     = 1f // How fast the glyphs should move

  // Distance
  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  // Frequency
  if (params.length > 1) frequency = paramAsFloat(params(1), 1f)
  // Speed
  if (params.length > 2) speed = paramAsFloat(params(2), 1f)
  // Duration
  if (params.length > 3) duration = paramAsFloat(params(3), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    // Calculate progress
    val progressModifier = (1f / speed) * DEFAULT_SPEED
    val normalFrequency  = (1f / frequency) * DEFAULT_FREQUENCY
    val progressOffset   = localIndex / normalFrequency
    val progress         = calculateProgress(progressModifier, progressOffset)

    // Calculate offset
    var y = label.getLineHeight(globalIndex) * distance * Interpolation.sine(-1f, 1f, progress) * DEFAULT_DISTANCE

    // Calculate fadeout
    y *= calculateFadeout()

    // Apply changes
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
