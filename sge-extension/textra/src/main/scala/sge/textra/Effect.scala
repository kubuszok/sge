/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Effect.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra

import sge.math.{ Interpolation, MathUtils }
import sge.utils.Nullable

/** Abstract text effect. */
abstract class Effect(protected val label: TypingLabel) {

  private val FADEOUT_SPLIT = 0.25f

  var indexStart:          Int   = -1
  var indexEnd:            Int   = -1
  var duration:            Float = Float.PositiveInfinity
  protected var totalTime: Float = 0f

  /** A unique identifier to be used by the start token for this Effect. This is expected to be assigned by [[TypingConfig.registerEffect]], not manually.
    */
  var name: Nullable[String] = Nullable.empty

  def assignTokenName(tokenName: String): Effect = {
    this.name = Nullable(tokenName)
    this
  }

  def update(delta: Float): Unit =
    totalTime += delta

  /** Applies the effect to the given glyph. */
  final def apply(glyph: Long, glyphIndex: Int, delta: Float): Unit = {
    val localIndex = glyphIndex - indexStart
    onApply(glyph, localIndex, glyphIndex, delta)
  }

  /** Called when this effect should be applied to the given glyph. */
  protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit

  /** Returns whether this effect is finished and should be removed. Note that effects are infinite by default. */
  def isFinished: Boolean = duration < 0 || totalTime > duration

  /** Calculates the fadeout of this effect, if any. Only considers the second half of the duration. */
  protected def calculateFadeout(): Float =
    if (duration < 0 || duration == Float.PositiveInfinity) {
      1f
    } else {
      // Calculate raw progress
      val progress = MathUtils.clamp(totalTime / duration, 0f, 1f)
      // If progress is before the split point, return a full factor
      if (progress < FADEOUT_SPLIT) {
        1f
      } else {
        // Otherwise calculate from the split point
        Interpolation.smooth(1f, 0f, (progress - FADEOUT_SPLIT) / (1f - FADEOUT_SPLIT))
      }
    }

  /** Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that loops in a ping-pong mode.
    */
  protected def calculateProgress(modifier: Float): Float =
    calculateProgress(modifier, 0f, pingpong = true)

  /** Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that loops in a ping-pong mode.
    */
  protected def calculateProgress(modifier: Float, offset: Float): Float =
    calculateProgress(modifier, offset, pingpong = true)

  /** Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1. */
  protected def calculateProgress(modifier: Float, offset: Float, pingpong: Boolean): Float = {
    var progress = totalTime / modifier + offset
    while (progress < 0f)
      progress += 2f
    if (pingpong) {
      progress = progress % 2f
      if (progress > 1f) progress = 1f - (progress - 1f)
    } else {
      progress = progress % 1f
    }
    progress = Math.min(Math.max(progress, 0f), 1f)
    progress
  }

  /** Returns a float value parsed from the given String, or the default value if the string couldn't be parsed. */
  protected def paramAsFloat(str: String, defaultValue: Float): Float =
    Parser.stringToFloat(str, defaultValue)

  /** Returns a boolean value parsed from the given String, or false if the string couldn't be parsed. */
  protected def paramAsBoolean(str: String): Boolean =
    Parser.stringToBoolean(str)

  /** Parses a color from the given string. Returns 256 if the color couldn't be parsed. */
  protected def paramAsColor(str: String): Int =
    Parser.stringToColor(label, str)
}

object Effect {

  /** A functional interface that is meant to be used with the 2-parameter constructor available for all Effects here, using a method reference such as `WaveEffect(_, _)`.
    */
  trait EffectBuilder {
    def produce(label: TypingLabel, params: Array[String]): Effect
  }
}
