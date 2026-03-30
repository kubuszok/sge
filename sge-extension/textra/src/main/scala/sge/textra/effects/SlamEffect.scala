/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.{ ArrayBuffer, HashMap }

class SlamEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_HANG_TIME      = 1f; private val DEFAULT_EXTENT = 1.5f; private val DEFAULT_HEIGHT = 1f; private val DEFAULT_POWER = 1f
  private var hangTime               = 0.25f; private var extent      = 1f; private var height           = 1f; private var shakeDuration = 2f; private var shakePower = 1f
  private val lastOffsets            = ArrayBuffer[Float]()
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) hangTime = paramAsFloat(params(0), 0.25f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1.0f)
  if (params.length > 2) height = paramAsFloat(params(2), 1.0f)
  if (params.length > 3) shakeDuration = paramAsFloat(params(3), 2.0f)
  if (params.length > 4) shakePower = paramAsFloat(params(4), 1.0f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * DEFAULT_EXTENT
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, -hangTime * DEFAULT_HANG_TIME + 1f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    var progress = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    progress = progress * progress * progress
    val shakeProgress = if (progress >= 0.9f && shakeDuration != 0f) MathUtils.clamp((timePassed / realExtent - 1f) / shakeDuration, 0f, 1f) else 0f
    if (shakeProgress == 0f) {
      val yMove = Interpolation.exp10In(label.getLineHeight(globalIndex) * height * DEFAULT_HEIGHT, 0f, progress * progress)
      label.getOffsets.incr(globalIndex << 1 | 1, yMove)
    } else {
      while (localIndex >= lastOffsets.size / 2) { var i = 0; while (i < 16) { lastOffsets += 0f; i += 1 } }
      val lastX           = lastOffsets(localIndex * 2); val lastY                = lastOffsets(localIndex * 2 + 1)
      val lineHeight      = label.getLineHeight(globalIndex)
      var x               = lineHeight * MathUtils.random(-0.125f, 0.125f); var y = lineHeight * MathUtils.random(-0.125f, 0.125f)
      val normalIntensity = MathUtils.clamp(shakePower * DEFAULT_POWER, 0f, 1f)
      x = Interpolation.linear(lastX, x, normalIntensity); y = Interpolation.linear(lastY, y, normalIntensity)
      val fadeout = 1f - Interpolation.sineOut(shakeProgress)
      x *= fadeout; y *= fadeout
      lastOffsets(localIndex * 2) = x; lastOffsets(localIndex * 2 + 1) = y
      label.getOffsets.incr(globalIndex << 1, x); label.getOffsets.incr(globalIndex << 1 | 1, y)
    }
  }
}
