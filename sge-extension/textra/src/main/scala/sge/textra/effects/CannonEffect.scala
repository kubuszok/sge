/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: CannonEffect,DEFAULT_HEIGHT,DEFAULT_STRETCH,initialStretch,lastOffsets,onApply,progress,realExtent,shakeDuration,shakeProgress,timePassed,timePassedByGlyphIndex
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/CannonEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.{ ArrayBuffer, HashMap }

class CannonEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_STRETCH        = 3f; private val DEFAULT_EXTENT  = 0.9f
  private val DEFAULT_HEIGHT         = 2.5f; private val DEFAULT_POWER = 1f
  private var initialStretch         = 1f; private var extent          = 1f; private var height = 1f
  private var shakeDuration          = 2f; private var shakePower      = 1f
  private val lastOffsets            = ArrayBuffer[Float]()
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) initialStretch = paramAsFloat(params(0), 1.0f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1.0f)
  if (params.length > 2) height = paramAsFloat(params(2), 1.0f)
  if (params.length > 3) shakeDuration = paramAsFloat(params(3), 2.0f)
  if (params.length > 4) shakePower = paramAsFloat(params(4), 1.0f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    var progress = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    progress = Math.sqrt(progress).toFloat
    val shakeProgress = if (progress >= 0.9f && shakeDuration != 0f) MathUtils.clamp((timePassed / realExtent - 1f) / shakeDuration, 0f, 1f) else 0f
    if (shakeProgress == 0f) {
      val interpolatedValue = Interpolation.sine(initialStretch * DEFAULT_STRETCH, 0f, progress)
      val arcHeight         = MathUtils.sin(MathUtils.PI * progress) * label.getLineHeight(globalIndex) * height * DEFAULT_HEIGHT
      label.getSizing.incr(globalIndex << 1, interpolatedValue)
      label.getSizing.incr(globalIndex << 1 | 1, interpolatedValue)
      label.getOffsets.incr(globalIndex << 1 | 1, arcHeight)
    } else {
      while (localIndex >= lastOffsets.size / 2) { var i = 0; while (i < 16) { lastOffsets += 0f; i += 1 } }
      val lastX           = lastOffsets(localIndex * 2); val lastY              = lastOffsets(localIndex * 2 + 1)
      val shakeMul        = label.getLineHeight(globalIndex) * initialStretch
      var x               = shakeMul * MathUtils.random(-0.125f, 0.125f); var y = shakeMul * MathUtils.random(-0.125f, 0.125f)
      val normalIntensity = MathUtils.clamp(shakePower * DEFAULT_POWER, 0f, 1f)
      x = Interpolation.linear(lastX, x, normalIntensity); y = Interpolation.linear(lastY, y, normalIntensity)
      val fadeout = 1f - Interpolation.sineOut(shakeProgress)
      x *= fadeout; y *= fadeout
      lastOffsets(localIndex * 2) = x; lastOffsets(localIndex * 2 + 1) = y
      label.getOffsets.incr(globalIndex << 1, x); label.getOffsets.incr(globalIndex << 1 | 1, y)
    }
  }
}
