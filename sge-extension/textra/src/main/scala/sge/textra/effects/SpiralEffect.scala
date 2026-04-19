/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: DEFAULT_DISTANCE,SpiralEffect,distance,lineHeight,onApply,progress,realSpeed,spin,timePassed,timePassedByGlyphIndex,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/SpiralEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.MathUtils
import scala.collection.mutable.HashMap

class SpiralEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 1f; private val DEFAULT_SPEED = 0.75f
  private var distance               = 1f; private var speed         = 1f; private var rotations = 1f
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) speed = 1f / paramAsFloat(params(1), 1f)
  if (params.length > 2) rotations = paramAsFloat(params(2), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realSpeed  = speed * DEFAULT_SPEED
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress   = MathUtils.clamp(timePassed / realSpeed, 0f, 1f)
    val spin       = 360f * rotations * progress
    val lineHeight = label.getLineHeight(globalIndex)
    val x          = lineHeight * distance * DEFAULT_DISTANCE * MathUtils.cosDeg(spin) * (1f - progress)
    val y          = lineHeight * distance * DEFAULT_DISTANCE * MathUtils.sinDeg(spin) * (1f - progress)
    label.getOffsets.incr(globalIndex << 1, x)
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
