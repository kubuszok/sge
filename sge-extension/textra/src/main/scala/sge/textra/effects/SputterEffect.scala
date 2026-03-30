/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.textra.utils.NoiseUtils

class SputterEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_WIDEN = 5f; private val DEFAULT_HEIGHTEN = 5f; private val DEFAULT_SPEED = 0.001f
  private var widen         = 0.25f; private var heighten      = 0.25f; private var speed      = 1f

  if (params.length > 0) widen = paramAsFloat(params(0), 0.25f)
  if (params.length > 1) heighten = paramAsFloat(params(1), 0.25f)
  if (params.length > 2) speed = paramAsFloat(params(2), 1f)
  if (params.length > 3) duration = paramAsFloat(params(3), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val timing  = (System.currentTimeMillis() & 0xffffff) * speed * DEFAULT_SPEED + globalIndex * 0.1f
    val h       = NoiseUtils.octaveNoise1D(timing, globalIndex)
    val v       = NoiseUtils.octaveNoise1D(timing, ~globalIndex)
    var hSharp  = h * h * h * widen * DEFAULT_WIDEN - v * 0.25f
    var vSharp  = v * v * v * heighten * DEFAULT_HEIGHTEN - h * 0.25f
    val fadeout = calculateFadeout()
    hSharp *= fadeout; vSharp *= fadeout
    label.getSizing.incr(globalIndex << 1, hSharp)
    label.getSizing.incr(globalIndex << 1 | 1, vSharp)
  }
}
