/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: DEFAULT_DISTANCE,ZipperEffect,distance,interpolatedValue,interpolation,onApply,progress,realExtent,timePassed,timePassedByGlyphIndex,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/ZipperEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class ZipperEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 0.75f; private val DEFAULT_EXTENT = 0.5f
  private var distance               = 2f; private var extent            = 1f; private var elastic = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 2f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1f)
  if (params.length > 2) elastic = paramAsBoolean(params(2))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * (if (elastic) 3f else 1f) * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
    val interpolatedValue = interpolation(1f, 0f, progress)
    val y                 = label.getLineHeight(globalIndex) * distance * interpolatedValue * DEFAULT_DISTANCE * ((globalIndex & 1) - 0.5f)
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
