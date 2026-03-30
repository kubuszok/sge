/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.textra.utils.NoiseUtils

class CrowdEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_ROTATION_STRENGTH = 1f; private val DEFAULT_SPEED = 0.001f
  private var rotationAmount            = 15f; private var speed        = 1f

  if (params.length > 0) rotationAmount = paramAsFloat(params(0), 15f)
  if (params.length > 1) speed = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    var rot = NoiseUtils.octaveNoise1D((System.currentTimeMillis() & 0xffffff) * speed * DEFAULT_SPEED + globalIndex * 0.42f, globalIndex) * rotationAmount * DEFAULT_ROTATION_STRENGTH
    rot *= calculateFadeout()
    label.getRotations.incr(globalIndex, rot)
  }
}
