/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_EXTENT,SlideEffect,distance,elastic,extent,interpolatedValue,interpolation,onApply,progress,realExtent,timePassed,timePassedByGlyphIndex,x
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/SlideEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

/** Moves the text horizontally easing it into the final position. Doesn't repeat itself. */
class SlideEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 2f
  private val DEFAULT_EXTENT         = 0.375f
  private var distance               = 1f
  private var extent                 = 1f
  private var elastic                = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1f)
  if (params.length > 2) elastic = paramAsBoolean(params(2))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * (if (elastic) 3f else 1f) * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
    val interpolatedValue = interpolation(1f, 0f, progress)
    val x                 = label.getLineHeight(globalIndex) * distance * interpolatedValue * DEFAULT_DISTANCE
    label.getOffsets.incr(globalIndex << 1, x)
  }
}
