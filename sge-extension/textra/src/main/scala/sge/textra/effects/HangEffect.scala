/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_EXTENT,HangEffect,distance,distanceFactor,extent,height,interpolation,onApply,progress,realIntensity,split,timePassed,timePassedByGlyphIndex,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/HangEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

/** Hangs the text in midair and suddenly drops it. Doesn't repeat itself. */
class HangEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 0.7f
  private val DEFAULT_EXTENT         = 1.5f
  private var distance               = 1f
  private var extent                 = 1f
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realIntensity = extent * DEFAULT_EXTENT
    val timePassed    = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress      = MathUtils.clamp(timePassed / realIntensity, 0f, 1f)
    val split         = 0.7f
    val interpolation =
      if (progress < split) Interpolation.pow3Out(0f, 1f, progress / split)
      else Interpolation.swing(1f, 0f, (progress - split) / (1f - split))
    val distanceFactor = Interpolation.linear(1.0f, 1.5f, progress)
    val height         = label.getLineHeight(globalIndex)
    var y              = height * distance * distanceFactor * interpolation * DEFAULT_DISTANCE
    y *= calculateFadeout()
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
