/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: DEFAULT_FREQUENCY,DEFAULT_JUMP_HEIGHT,DEFAULT_SPEED,JumpEffect,frequency,interpolation,jumpHeight,normalFrequency,onApply,progress,progressModifier,progressOffset,speed,split,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/JumpEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.Interpolation

/** Makes the text jump and fall as if there was gravity. */
class JumpEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY   = 50f
  private val DEFAULT_JUMP_HEIGHT = 1.33f
  private val DEFAULT_SPEED       = 1f
  private var jumpHeight          = 1f
  private var frequency           = 1f
  private var speed               = 1f

  if (params.length > 0) jumpHeight = paramAsFloat(params(0), 1f)
  if (params.length > 1) frequency = paramAsFloat(params(1), 1f)
  if (params.length > 2) speed = paramAsFloat(params(2), 1f)
  if (params.length > 3) duration = paramAsFloat(params(3), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val progressModifier = (1f / speed) * DEFAULT_SPEED
    val normalFrequency  = (1f / frequency) * DEFAULT_FREQUENCY
    val progressOffset   = localIndex / normalFrequency
    val progress         = calculateProgress(progressModifier, -progressOffset, pingpong = false)
    val split            = 0.2f
    val interpolation    =
      if (progress < split) Interpolation.pow2Out(0f, 1f, progress / split)
      else Interpolation.bounceOut(1f, 0f, (progress - split) / (1f - split))
    var y = label.getLineHeight(globalIndex) * jumpHeight * interpolation * DEFAULT_JUMP_HEIGHT
    y *= calculateFadeout()
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
