/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: DEFAULT_DISTANCE,JoltEffect,baseColor,determineFloat,lastOffsets,lastX,onApply,s,shakeDistance,x
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/JoltEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.ArrayBuffer

class JoltEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE = 0.12f; private val DEFAULT_SPEED = 0.5f; private val DEFAULT_LIKELIHOOD = 0.05f
  private val lastOffsets      = ArrayBuffer[Float]()
  private var shakeDistance    = 1f; private var shakeSpeed       = 1f; private var likelihood           = DEFAULT_LIKELIHOOD
  private var baseColor        = 256; private var joltColor       = 0xffff88ff

  if (params.length > 0) shakeDistance = paramAsFloat(params(0), 1f)
  if (params.length > 1) shakeSpeed = paramAsFloat(params(1), 1f)
  if (params.length > 2) duration = paramAsFloat(params(2), Float.PositiveInfinity)
  if (params.length > 3) likelihood = paramAsFloat(params(3), DEFAULT_LIKELIHOOD)
  if (params.length > 4) baseColor = paramAsColor(params(4))
  if (params.length > 5) { val c = paramAsColor(params(5)); if (c != 256) joltColor = c }

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    while (localIndex >= lastOffsets.size / 2) { var i = 0; while (i < 16) { lastOffsets += 0f; i += 1 } }
    val lastX = lastOffsets(localIndex * 2); val lastY = lastOffsets(localIndex * 2 + 1)
    var x     = 0f; var y                              = 0f
    if (likelihood > determineFloat((System.currentTimeMillis() >>> 10) * globalIndex + localIndex)) {
      val shakeMul = label.getLineHeight(globalIndex) * shakeDistance * DEFAULT_DISTANCE
      x = shakeMul * MathUtils.random(-1f, 1f); y = shakeMul * MathUtils.random(-1f, 1f)
      val normalIntensity = MathUtils.clamp(shakeSpeed * DEFAULT_SPEED, 0f, 1f)
      x = Interpolation.linear(lastX, x, normalIntensity); y = Interpolation.linear(lastY, y, normalIntensity)
      val fadeout = calculateFadeout(); x *= fadeout; y *= fadeout
      if (fadeout > 0) {
        if (baseColor == 256) label.setInWorkingLayout(globalIndex, glyph)
        else label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (joltColor.toLong << 32))
      }
    } else {
      if (baseColor == 256) label.setInWorkingLayout(globalIndex, glyph)
      else label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (baseColor.toLong << 32))
    }
    lastOffsets(localIndex * 2) = x; lastOffsets(localIndex * 2 + 1) = y
    label.getOffsets.incr(globalIndex << 1, x); label.getOffsets.incr(globalIndex << 1 | 1, y)
  }

  private def determineFloat(state: Long): Float = {
    var s = state
    s = ((s * 0x632be59bd9b4e019L) ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L
    (((s ^ s >>> 27) * 0xaef17502108ef2d9L) >>> 40) * 5.9604645e-8f
  }
}
