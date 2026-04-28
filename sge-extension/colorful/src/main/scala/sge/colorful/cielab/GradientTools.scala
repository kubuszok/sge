/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 88
 * Covenant-baseline-methods: GradientTools,appendGradient,appendGradientChain,appendPartialGradient,appending,makeGradient
 * Covenant-source-reference: com/github/tommyettinger/colorful/cielab/GradientTools.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e4a5fd960eef746ca5aa826063432fb79666d74f
 */
package sge
package colorful
package cielab

import sge.colorful.FloatColors
import sge.math.{ Interpolation, MathUtils }

import scala.collection.mutable.ArrayBuffer

/** Static methods for handling gradients of smoothly-changing colors, typically inside of ArrayBuffers. The intent is for the ArrayBuffer to be used as a sequence of packed float CIELAB colors. This
  * class does some special handling for CIELAB colors, limiting to the correct gamut.
  */
object GradientTools {

  /** Creates an ArrayBuffer gradient from the packed float CIELAB color `start` to `end`, taking the specified number of steps and using linear interpolation.
    */
  def makeGradient(start: Float, end: Float, steps: Int, interpolation: Interpolation = Interpolation.linear): ArrayBuffer[Float] = {
    val appending = ArrayBuffer.empty[Float]
    if (steps <= 0) {
      appending
    } else if (steps == 1) {
      appending += start
    } else {
      appendPartialGradient(appending, start, end, steps - 1, interpolation)
      appending += end
    }
  }

  /** Appends a gradient from `start` to `end`, taking the specified number of steps.
    */
  def appendGradient(appending: ArrayBuffer[Float], start: Float, end: Float, steps: Int, interpolation: Interpolation = Interpolation.linear): ArrayBuffer[Float] =
    if (steps <= 0) {
      appending
    } else if (steps == 1) {
      appending += start
    } else {
      appendPartialGradient(appending, start, end, steps - 1, interpolation)
      appending += end
    }

  /** Appends a gradient between several packed float CIELAB colors provided in `chain`.
    */
  def appendGradientChain(appending: ArrayBuffer[Float], steps: Int, interpolation: Interpolation = Interpolation.linear, chain: Float*): ArrayBuffer[Float] =
    if (steps <= 0 || chain.isEmpty) {
      appending
    } else if (steps == 1 || chain.length == 1) {
      appending += chain.head
    } else {
      val limit  = steps - 1
      val splits = chain.length - 1
      val step   = 1f / steps
      var change = 0f
      var i      = 0
      while (i < limit) {
        val interp = interpolation.apply(change)
        val splint = Math.min(Math.max(interp * splits, 0f), splits - 0.000001f)
        val idx    = splint.toInt
        appending += ColorTools.limitToGamut(FloatColors.lerpFloatColors(chain(idx), chain(idx + 1), MathUtils.norm(idx.toFloat, (idx + 1).toFloat, splint)))
        change += step
        i += 1
      }
      appending += chain(splits)
    }

  /** Like appendGradient, but does not include `end`. Intended for chained gradients.
    */
  def appendPartialGradient(appending: ArrayBuffer[Float], start: Float, end: Float, steps: Int, interpolation: Interpolation = Interpolation.linear): ArrayBuffer[Float] =
    if (steps <= 0) {
      appending
    } else {
      val step   = 1f / steps
      var change = 0f
      var i      = 0
      while (i < steps) {
        appending += ColorTools.limitToGamut(FloatColors.lerpFloatColors(start, end, interpolation.apply(change)))
        change += step
        i += 1
      }
      appending
    }
}
