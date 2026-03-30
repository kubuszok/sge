/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 */
package sge
package textra
package effects

import sge.math.Interpolation
import scala.collection.mutable.ArrayBuffer

/** Drips the text down and back up from its normal position in a random pattern. */
class SickEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_FREQUENCY = 50f
  private val DEFAULT_DISTANCE  = .125f
  private val DEFAULT_SPEED     = 1f
  var distance: Float = 1f
  var speed:    Float = 1f
  private val indices = ArrayBuffer[Int]()

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) speed = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val progressModifier = (1f / speed) * DEFAULT_SPEED
    val progressOffset   = localIndex / DEFAULT_FREQUENCY
    val progress         = calculateProgress(progressModifier, -progressOffset, pingpong = false)
    if (progress < .01f && Math.random() > .25f && !indices.contains(localIndex)) indices += localIndex
    if (progress > .95f) indices -= localIndex
    if (!indices.contains(localIndex) && !indices.contains(localIndex - 1) && !indices.contains(localIndex - 2) && !indices.contains(localIndex + 2) && !indices.contains(localIndex + 1)) return
    val split         = 0.5f
    val interpolation = if (progress < split) Interpolation.pow2Out(0f, 1f, progress / split) else Interpolation.pow2In(1f, 0f, (progress - split) / (1f - split))
    var y             = label.getLineHeight(globalIndex) * distance * interpolation * DEFAULT_DISTANCE
    if (indices.contains(localIndex)) y *= 2.15f
    if (indices.contains(localIndex - 1) || indices.contains(localIndex + 1)) y *= 1.35f
    y *= calculateFadeout()
    label.getOffsets.incr(globalIndex << 1 | 1, -y)
  }
}
