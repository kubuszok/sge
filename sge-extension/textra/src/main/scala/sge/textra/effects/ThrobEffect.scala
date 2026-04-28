/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: DEFAULT_DISTANCE,DEFAULT_FREQUENCY,ThrobEffect,distance,fadeout,frequency,lastOffsets,lastX,normalSpeed,onApply,wave,x
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/ThrobEffect.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package effects

import sge.math.{ Interpolation, MathUtils }
import scala.collection.mutable.ArrayBuffer

class ThrobEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE  = 0.12f; private val DEFAULT_SPEED = 0.5f
  private val DEFAULT_FREQUENCY = MathUtils.PI * 0.001f
  private val lastOffsets       = ArrayBuffer[Float]()
  private var distance          = 1f; private var speed            = 1f
  @annotation.nowarn("msg=mutated but not read") // matches original; frequency parsed but used globally
  private var frequency = 1f

  if (params.length > 0) distance = paramAsFloat(params(0), 1f)
  if (params.length > 1) speed = paramAsFloat(params(1), 1f)
  if (params.length > 2) frequency = paramAsFloat(params(2), 1f)
  if (params.length > 3) duration = paramAsFloat(params(3), Float.PositiveInfinity)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    while (localIndex >= lastOffsets.size / 2) { var i = 0; while (i < 16) { lastOffsets += 0f; i += 1 } }
    val lastX = lastOffsets(localIndex * 2); val lastY = lastOffsets(localIndex * 2 + 1)
    var wave  = MathUtils.sin((System.currentTimeMillis() & 0xffffff) * DEFAULT_FREQUENCY)
    wave *= wave
    wave *= label.getLineHeight(globalIndex) * distance * DEFAULT_DISTANCE
    var x           = MathUtils.random(-1f, 1f) * wave; var y = MathUtils.random(-1f, 1f) * wave
    val normalSpeed = MathUtils.clamp(speed * DEFAULT_SPEED, 0f, 1f)
    x = Interpolation.linear(lastX, x, normalSpeed); y = Interpolation.linear(lastY, y, normalSpeed)
    val fadeout = calculateFadeout(); x *= fadeout; y *= fadeout
    lastOffsets(localIndex * 2) = x; lastOffsets(localIndex * 2 + 1) = y
    label.getOffsets.incr(globalIndex << 1, x); label.getOffsets.incr(globalIndex << 1 | 1, y)
  }
}
