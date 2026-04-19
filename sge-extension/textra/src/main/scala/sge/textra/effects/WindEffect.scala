/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_INTENSITY,DEFAULT_SPACING,DISTANCE_X_RATIO,DISTANCE_Y_RATIO,IDEAL_DELTA,WindEffect,changeAmount,distanceX,distanceY,fadeout,indexOffset,intensity,lineHeight,noiseCursorX,noiseCursorY,noiseX,noiseY,normalSpacing,onApply,progress,progressModifier,progressOffset,spacing,update,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/WindEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.textra.utils.NoiseUtils

/** Moves the text as if it is being blown around by wind. */
class WindEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_SPACING   = 10f
  private val DEFAULT_DISTANCE  = 0.33f
  private val DEFAULT_INTENSITY = 0.375f
  private val DISTANCE_X_RATIO  = 1.5f
  private val DISTANCE_Y_RATIO  = 1.0f
  private val IDEAL_DELTA       = 60f
  private var noiseCursorX      = 0f
  private var noiseCursorY      = 0f
  private var distanceX         = 1f
  private var distanceY         = 1f
  private var spacing           = 1f
  private var intensity         = 1f

  if (params.length > 0) distanceX = paramAsFloat(params(0), 1f)
  if (params.length > 1) distanceY = paramAsFloat(params(1), 1f)
  if (params.length > 2) spacing = paramAsFloat(params(2), 1f)
  if (params.length > 3) intensity = paramAsFloat(params(3), 1f)
  if (params.length > 4) duration = paramAsFloat(params(4), Float.PositiveInfinity)

  override def update(delta: Float): Unit = {
    super.update(delta)
    val changeAmount = 0.15f * intensity * DEFAULT_INTENSITY * delta * IDEAL_DELTA
    noiseCursorX += changeAmount
    noiseCursorY += changeAmount
  }

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val progressModifier = DEFAULT_INTENSITY / intensity
    val normalSpacing    = DEFAULT_SPACING / spacing
    val progressOffset   = localIndex / normalSpacing
    val progress         = calculateProgress(progressModifier, progressOffset)
    val indexOffset      = localIndex * 0.05f * spacing
    val noiseX           = NoiseUtils.octaveNoise1D(noiseCursorX + indexOffset, 123)
    val noiseY           = NoiseUtils.octaveNoise1D(noiseCursorY + indexOffset, -4321)
    val lineHeight       = label.getLineHeight(globalIndex)
    var x                = lineHeight * noiseX * progress * distanceX * DISTANCE_X_RATIO * DEFAULT_DISTANCE
    var y                = lineHeight * noiseY * progress * distanceY * DISTANCE_Y_RATIO * DEFAULT_DISTANCE
    val fadeout          = calculateFadeout()
    x *= fadeout; y *= fadeout
    x = Math.abs(x) * -Math.signum(distanceX)
    label.getOffsets.incr(globalIndex << 1, x)
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
