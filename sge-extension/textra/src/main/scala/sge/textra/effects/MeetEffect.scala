/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.HashMap

class MeetEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 1f; private val DEFAULT_EXTEND = 1f
  private var distance               = 2f; private var extent         = 1f; private var elastic = false; private var inside = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 2f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1f)
  if (params.length > 2) elastic = paramAsBoolean(params(2))
  if (params.length > 3) inside = paramAsBoolean(params(3))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val realExtent = extent * (if (elastic) 3f else 1f) * DEFAULT_EXTEND
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress          = MathUtils.clamp(timePassed / realExtent, 0f, 1f)
    val interpolation     = if (elastic) Interpolation.swingOut else Interpolation.sine
    val interpolatedValue = interpolation(1f, 0f, progress)
    val random            = ((globalIndex ^ 0xde82ef95) * 0xd1343 ^ 0xde82ef95) * 0xd1343
    val angle             = (random >>> 9) * 1.1920929e-7f * MathUtils.PI2
    val random2           = ((random ^ 0xde82ef95) * 0xd1343 ^ 0xde82ef95) * 0xd1343
    val dist              = label.getLineHeight(globalIndex) * distance * DEFAULT_DISTANCE *
      (if (inside) Math.sqrt((random2 >>> 9) * 1.1920929e-7f).toFloat else 1f)
    val x = MathUtils.cos(angle) * dist
    val y = MathUtils.sin(angle) * dist
    label.getOffsets.incr(globalIndex << 1, x * interpolatedValue)
    label.getOffsets.incr(globalIndex << 1 | 1, y * interpolatedValue)
  }
}
