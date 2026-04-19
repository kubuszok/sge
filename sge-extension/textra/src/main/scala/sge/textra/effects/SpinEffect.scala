/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: DEFAULT_EXTENT,SpinEffect,extent,interpolatedValue,interpolation,onApply,progress,realExtent,timePassed,timePassedByGlyphIndex
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/SpinEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class SpinEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_EXTENT         = 1.0f
  private var extent                 = 1f; private var rotations = 1f; private var elastic = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) extent = paramAsFloat(params(0), 1f)
  if (params.length > 1) rotations = paramAsFloat(params(1), 1f)
  if (params.length > 2) elastic = paramAsBoolean(params(2))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * (if (elastic) 3f else 1f) * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.bounceOut else Interpolation.pow3Out
    val interpolatedValue = interpolation(progress) * 360.0f * rotations
    label.getRotations.incr(globalIndex, interpolatedValue)
  }
}
