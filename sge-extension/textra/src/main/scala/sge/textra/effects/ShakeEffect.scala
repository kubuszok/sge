/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.ArrayBuffer

/** Shakes the text in a random pattern. */
class ShakeEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE = 0.12f
  private val DEFAULT_SPEED    = 0.5f
  private val lastOffsets      = ArrayBuffer[Float]()

  private var distance = 1f
  private var speed    = 1f

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) speed = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    while (localIndex >= lastOffsets.size / 2) { var i = 0; while (i < 16) { lastOffsets += 0f; i += 1 } }
    val lastX       = lastOffsets(localIndex * 2)
    val lastY       = lastOffsets(localIndex * 2 + 1)
    val shakeMul    = label.getLineHeight(globalIndex) * distance * DEFAULT_DISTANCE
    var x           = MathUtils.random(-1f, 1f) * shakeMul
    var y           = MathUtils.random(-1f, 1f) * shakeMul
    val normalSpeed = MathUtils.clamp(speed * DEFAULT_SPEED, 0f, 1f)
    x = Interpolation.linear(lastX, x, normalSpeed)
    y = Interpolation.linear(lastY, y, normalSpeed)
    val fadeout = calculateFadeout()
    x *= fadeout; y *= fadeout
    lastOffsets(localIndex * 2) = x
    lastOffsets(localIndex * 2 + 1) = y
    label.getOffsets.incr(globalIndex << 1, x)
    label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
