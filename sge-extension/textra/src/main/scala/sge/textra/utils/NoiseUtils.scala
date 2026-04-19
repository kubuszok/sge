/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/NoiseUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 67
 * Covenant-baseline-methods: NoiseUtils,a,b,bicubicNoise1D,c,d,floor,h,imul,m,n,noise1D,o,octaveBicubicNoise1D,octaveNoise1D,p,rise,seed,triangleWave,x,xFloor
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/NoiseUtils.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package utils

import sge.math.MathUtils
import sge.utils.NumberUtils

/** Some 1D noise methods to be used when an effect needs smooth but random changes. */
object NoiseUtils {

  /** Quilez' 1D noise, with some changes to work on the CPU. Takes a distance x and any int seed, and produces a smoothly-changing value as x goes up or down and seed stays the same. Uses a quartic
    * curve.
    */
  def noise1D(xIn: Float, seed: Int): Float = {
    var x      = xIn + seed * 5.9604645e-8f
    val xFloor = MathUtils.floor(x)
    val rise   = 1 - (MathUtils.floor(x + x) & 2)
    x -= xFloor
    // gets a random float between -16 and 16. Magic.
    val h = NumberUtils.intBitsToFloat(
      ((seed + (xFloor.toLong ^ 0x9e3779b97f4a7c15L)) * 0xd1b54a32d192ed03L >>> 41).toInt | 0x42000000
    ) - 48f
    x *= x - 1f
    rise * x * x * h
  }

  /** A more natural 1D noise that uses two octaves of [[noise1D]]; still has a range of -1 to 1. */
  def octaveNoise1D(x: Float, seed: Int): Float =
    noise1D(x, seed) * 0.6666667f + noise1D(x * 1.9f, ~seed) * 0.33333333f

  /** Sway smoothly using bicubic interpolation between 4 points. */
  def bicubicNoise1D(xIn: Float, seedIn: Int): Float = {
    val floor = (xIn + 16384.0).toInt - 16384
    val seed  = (seedIn ^ (seedIn << 11 | seedIn >>> 21) ^ (seedIn << 25 | seedIn >>> 7)) + floor

    val m = imul(seed, 0xd1b54a33)
    val n = imul(seed, 0xabc98383)
    val o = imul(seed, 0x8cb92ba7)

    val a = (m ^ n ^ o).toFloat
    val b = (m + 0xd1b54a33 ^ n + 0xabc98383 ^ o + 0x8cb92ba7).toFloat
    val c = (m + 0xa36a9466 ^ n + 0x57930716 ^ o + 0x1972574e).toFloat
    val d = (m + 0x751fde99 ^ n + 0x035c8a99 ^ o + 0xa62b82f5).toFloat

    val x = xIn - floor
    val p = (d - c) - (a - b)
    (x * (x * x * p + x * (a - b - p) + c - a) + b) * 3.1044084e-10f
  }

  /** A more natural 1D noise that uses two octaves of [[bicubicNoise1D]]; has a range of -1 to 1. */
  def octaveBicubicNoise1D(x: Float, seed: Int): Float =
    bicubicNoise1D(x, seed) * 0.6666667f + bicubicNoise1D(x * 1.9f, ~seed) * 0.33333333f

  /** A standard triangle wave with a period of 1 and a range of -1 to 1. */
  def triangleWave(t: Float): Float =
    Math.abs(t - ((t + 16384.5).toInt - 16384)) * 4f - 1f

  /** Integer multiplication; replacement for RegExodus Compatibility.imul(). */
  private def imul(a: Int, b: Int): Int = a * b
}
