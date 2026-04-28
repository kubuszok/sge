/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: DEFAULT_DISTANCE,ThinkingEffect,alpha,angle,dist,distance,driftAmount,driftAngle,lineHeight,onApply,progress,random,random2,randomizedProgress,timePassed,timePassedByGlyphIndex,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/ThinkingEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.math.MathUtils
import scala.collection.mutable.HashMap

class ThinkingEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE       = 1f; private val DEFAULT_DRIFT = 1f
  private var distance               = 2f; private var extent        = 1f; private var drift = 1f; private var inside = false
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) distance = paramAsFloat(params(0), 2f)
  if (params.length > 1) extent = paramAsFloat(params(1), 1f)
  if (params.length > 2) drift = paramAsFloat(params(2), 1f)
  if (params.length > 3) inside = paramAsBoolean(params(3))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress           = MathUtils.clamp(timePassed / extent, 0f, 1f)
    val random             = ((globalIndex ^ 0xde82ef95) * 0xd1343 ^ 0xde82ef95) * 0xd1343
    val angle              = (random >>> 9) * 1.1920929e-7f * MathUtils.PI2
    val driftAngle         = (random & 0x7fffff) * 1.1920929e-7f * MathUtils.PI2
    val random2            = ((random ^ 0xde82ef95) * 0xd1343 ^ 0xde82ef95) * 0xd1343
    val randomizedProgress = Math.pow(progress, (((random2 ^ 0xde82ef95) * 0xd1343 ^ 0xde82ef95) * 0xd1343 >>> 9) * 1.1920929e-7f + 1.25f).toFloat
    val alpha              = Math.max(0f, MathUtils.cos(randomizedProgress * MathUtils.PI2))
    val lineHeight         = label.getLineHeight(globalIndex)
    val dist               =
      if (randomizedProgress > 0.5f) 0f
      else lineHeight * distance * DEFAULT_DISTANCE * (if (inside) Math.sqrt((random2 >>> 9) * 1.1920929e-7f).toFloat else 1f)
    val driftAmount =
      if (randomizedProgress > 0.5f || drift == 0f) 0f
      else lineHeight * drift * DEFAULT_DRIFT * ((random2 & 0x7fffff) * 1.1920929e-7f) * progress
    val x = MathUtils.cos(angle) * dist + MathUtils.cos(driftAngle) * driftAmount
    val y = MathUtils.sin(angle) * dist + MathUtils.sin(driftAngle) * driftAmount
    label.getOffsets.incr(globalIndex << 1, x)
    label.getOffsets.incr(globalIndex << 1 | 1, y)
    label.setInWorkingLayout(globalIndex, (glyph & 0xffffff00ffffffffL) | ((255 * alpha).toLong << 32))
  }
}
