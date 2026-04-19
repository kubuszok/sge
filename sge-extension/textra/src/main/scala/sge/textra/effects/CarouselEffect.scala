/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: CarouselEffect,DEFAULT_FREQUENCY,font,frequency,onApply,progress,s,timePassed,timePassedByGlyphIndex
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/CarouselEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.MathUtils
import scala.collection.mutable.HashMap

class CarouselEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY      = 0.5f
  private var frequency              = 1f
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) frequency = paramAsFloat(params(0), 1.0f)
  if (params.length > 1) duration = paramAsFloat(params(1), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    var timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    timePassed -= delta
    if (timePassed >= duration) timePassed = 0f
    val progress = timePassed * 360.0f * frequency * DEFAULT_FREQUENCY
    val s        = MathUtils.sinDeg(progress)
    val font     = label.getFont
    label.getSizing.incr(globalIndex << 1, s - 1.0f)
    label.getOffsets.incr(globalIndex << 1, font.mapping.getOrElse(glyph.toChar, font.defaultValue).xAdvance * (0.125f * s))
  }
}
