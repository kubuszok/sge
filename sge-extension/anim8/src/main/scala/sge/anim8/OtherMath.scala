/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

import sge.math.Interpolation

/** Various math functions that don't fit anywhere else, mostly relating to the shape of a distribution. These include the parameterizable 0-1 curve produced by [[barronSpline]], the bell curve
  * produced from a 0-1 input but with a larger output range by [[probit]], and both an accurate approximation of the cube root, [[cbrt]] and an inaccurate but similarly-shaped method, [[cbrtShape]].
  */
object OtherMath {

  /** Given a byte, pushes any value that isn't extreme toward the center of the 0 to 255 range, and keeps extreme values (such as the channel values in the colors max green or black) as they are.
    * @param v
    *   a byte value that will be treated as if in the 0-255 range (as if unsigned)
    * @return
    *   a modified version of `v` that is more often near the center of the range than the extremes
    */
  def centralize(v: Byte): Byte =
    (barronSpline((v & 255) * 0.003921569f, 0.5f, 0.5f) * 255.999f).toByte

  /** Approximates the natural logarithm of `x` (that is, with base E), using single-precision, somewhat roughly.
    * @param x
    *   the argument to the logarithm; must be greater than 0
    * @return
    *   an approximation of the logarithm of x with base E; can be any float
    */
  def logRough(x: Float): Float = {
    val vx = java.lang.Float.floatToIntBits(x)
    val mx = java.lang.Float.intBitsToFloat((vx & 0x007fffff) | 0x3f000000)
    vx * 8.262958e-8f - 86.10657f - 1.0383555f * mx - 1.1962888f / (0.3520887068f + mx)
  }

  // constants used by probitF()
  private val a0f = 0.195740115269792f
  private val a1f = -0.652871358365296f
  private val a2f = 1.246899760652504f
  private val b0f = 0.155331081623168f
  private val b1f = -0.839293158122257f
  private val c3f = -1.000182518730158122f
  private val c0f = 16.682320830719986527f
  private val c1f = 4.120411523939115059f
  private val c2f = 0.029814187308200211f
  private val d0f = 7.173787663925508066f
  private val d1f = 8.759693508958633869f

  // constants used by probit()
  private val a0 = 0.195740115269792
  private val a1 = -0.652871358365296
  private val a2 = 1.246899760652504
  private val b0 = 0.155331081623168
  private val b1 = -0.839293158122257
  private val c3 = -1.000182518730158122
  private val c0 = 16.682320830719986527
  private val c1 = 4.120411523939115059
  private val c2 = 0.029814187308200211
  private val d0 = 7.173787663925508066
  private val d1 = 8.759693508958633869

  /** A single-precision probit() approximation that takes a float between 0 and 1 inclusive and returns an approximately-Gaussian-distributed float between -9.080134 and 9.080134 .
    * [[https://www.researchgate.net/publication/46462650_A_New_Approximation_to_the_Normal_Distribution_Quantile_Function Uses this algorithm by Paul Voutier]].
    * @param p
    *   should be between 0 and 1, inclusive.
    * @return
    *   an approximately-Gaussian-distributed float between -9.080134 and 9.080134
    */
  def probitF(p: Float): Float =
    if (0.0465f > p) {
      val r = Math.sqrt(logRough(1f / (p * p))).toFloat
      c3f * r + c2f + (c1f * r + c0f) / (r * (r + d1f) + d0f)
    } else if (0.9535f < p) {
      val q = 1f - p
      val r = Math.sqrt(logRough(1f / (q * q))).toFloat
      -c3f * r - c2f - (c1f * r + c0f) / (r * (r + d1f) + d0f)
    } else {
      val q = p - 0.5f
      val r = q * q
      q * (a2f + (a1f * r + a0f) / (r * (r + b1f) + b0f))
    }

  /** A double-precision probit() approximation that takes a double between 0 and 1 inclusive and returns an approximately-Gaussian-distributed double between -26.48372928592822 and 26.48372928592822
    * . [[https://www.researchgate.net/publication/46462650_A_New_Approximation_to_the_Normal_Distribution_Quantile_Function Uses this algorithm by Paul Voutier]].
    * @param p
    *   should be between 0 and 1, inclusive.
    * @return
    *   an approximately-Gaussian-distributed double between -26.48372928592822 and 26.48372928592822
    */
  def probit(p: Double): Double =
    if (0.0465 > p) {
      val q = p + 7.458340731200208e-155
      val r = Math.sqrt(Math.log(1.0 / (q * q)))
      c3 * r + c2 + (c1 * r + c0) / (r * (r + d1) + d0)
    } else if (0.9535 < p) {
      val q = 1.0 - p + 7.458340731200208e-155
      val r = Math.sqrt(Math.log(1.0 / (q * q)))
      -c3 * r - c2 - (c1 * r + c0) / (r * (r + d1) + d0)
    } else {
      val q = p - 0.5
      val r = q * q
      q * (a2 + (a1 * r + a0) / (r * (r + b1) + b0))
    }

  /** An approximation of the cube-root function for float inputs and outputs. This can be over twice as fast as [[Math.cbrt]]. It correctly returns negative results when given negative inputs.
    *
    * This was adjusted very slightly so `cbrt(1f) == 1f`. While this corrects the behavior for one of the most commonly-expected inputs, it may change results for (very) large positive or negative
    * inputs.
    *
    * If you need to work with doubles, or need higher precision, use [[Math.cbrt]].
    * @param cube
    *   any finite float to find the cube root of
    * @return
    *   the cube root of `cube`, approximated
    */
  def cbrt(cube: Float): Float = {
    val ix = java.lang.Float.floatToIntBits(cube)
    var x  = java.lang.Float.intBitsToFloat((ix & 0x7fffffff) / 3 + 0x2a51379a | (ix & 0x80000000))
    x = 0.66666657f * x + 0.333333334f * cube / (x * x)
    x = 0.66666657f * x + 0.333333334f * cube / (x * x)
    x
  }

  /** An approximation of the cube-root function for float inputs and outputs. This version does not tolerate negative inputs, because in the narrow use case it has in this class, it is never given
    * negative inputs.
    * @param cube
    *   any positive finite float to find the cube root of
    * @return
    *   the cube root of x, approximated
    */
  def cbrtPositive(cube: Float): Float = {
    var x = java.lang.Float.intBitsToFloat(java.lang.Float.floatToIntBits(cube) / 3 + 0x2a51379a)
    x = 0.66666657f * x + 0.333333334f * cube / (x * x)
    x = 0.66666657f * x + 0.333333334f * cube / (x * x)
    x
  }

  /** A function that loosely approximates the cube root of `x`, but is much smaller and probably faster than [[OtherMath.cbrt]]. This is meant to be used when you want the shape of a cbrt() function,
    * but don't actually care about it being the accurate mathematical cube-root.
    * @param x
    *   any finite float
    * @return
    *   a loose approximation of the cube root of x; mostly useful for its shape
    */
  def cbrtShape(x: Float): Float =
    x * 1.25f / (0.25f + Math.abs(x))

  /** The quantile for the triangular distribution, this takes a float u in the range 0 to limit, both inclusive, and produces a float between 0 and 1 that should be triangular-mapped if u was
    * uniformly-distributed.
    * @param u
    *   should be between 0 (inclusive) and limit (inclusive)
    * @param limit
    *   the upper (inclusive) bound for u
    * @return
    *   a float between 0 and 1, both inclusive, that will be triangular-distributed if u was uniform
    */
  def triangularRemap(u: Float, limit: Float): Float =
    if (u <= 0.5f * limit)
      Math.sqrt((0.5f / limit) * u).toFloat
    else
      1f - Math.sqrt(0.5f - (0.5f / limit) * u).toFloat

  /** A standard [[https://en.wikipedia.org/wiki/Triangle_wave triangle wave]] with a period of 1 and a range of -1 to 1 (both inclusive). Every integer input given to this will produce -1 as its
    * output. Every input that is exactly 0.5 plus an integer will produce 1 as its output.
    * @param t
    *   the input to the triangle wave; can be any float from -16384 to 4194304
    * @return
    *   a float between -1f and 1f, both inclusive
    */
  def triangleWave(t: Float): Float =
    Math.abs(t - ((t + 16384.5f).toInt - 16384)) * 4f - 1

  /** A variant on [[Math.atan]] that does not tolerate infinite inputs and takes/returns floats.
    * @param i
    *   any finite float
    * @return
    *   an output from the inverse tangent function, from PI/-2.0 to PI/2.0 inclusive
    */
  private def atanUnchecked(i: Float): Float = {
    val n  = Math.abs(i)
    val c  = (n - 1f) / (n + 1f)
    val c2 = c * c
    val c3 = c * c2
    val c5 = c3 * c2
    val c7 = c5 * c2
    Math.signum(i) * (0.7853981633974483f +
      (0.999215f * c - 0.3211819f * c3 + 0.1462766f * c5 - 0.0389929f * c7))
  }

  /** Close approximation of the frequently-used trigonometric method atan2, with higher precision than libGDX's atan2 approximation. Maximum error is below 0.00009 radians.
    * @param y
    *   y-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @param x
    *   x-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @return
    *   the angle to the given point, in radians as a float; ranges from -PI to PI
    */
  def atan2(y: Float, x0: Float): Float = {
    var x = x0
    var n = y / x
    if (n != n) n = if (y == x) 1f else -1f // if both y and x are infinite, n would be NaN
    else if (n - n != n - n) x = 0f // if n is infinite, y is infinitely larger than x.
    if (x > 0)
      atanUnchecked(n)
    else if (x < 0) {
      if (y >= 0)
        atanUnchecked(n) + 3.14159265358979323846f
      else
        atanUnchecked(n) - 3.14159265358979323846f
    } else if (y > 0) x + 1.5707963267948966f
    else if (y < 0) x - 1.5707963267948966f
    else x + y // returns 0 for 0,0 or NaN if either y or x is NaN
  }

  /** Returns arcsine in radians; less accurate than Math.asin but may be faster.
    * @param a
    *   asin is defined only when `a` is between -1f and 1f, inclusive
    * @return
    *   between `-0.5 * PI` and `0.5 * PI` when `a` is in the defined range
    */
  def asin(a: Float): Float =
    if (a >= 0f) {
      ((Math.PI * 0.5) - Math.sqrt(1.0 - a) * (1.5707288 + a * (-0.2121144 + a * (0.0742610 + a * -0.0187293)))).toFloat
    } else {
      (Math.sqrt(1.0 + a) * (1.5707288 + a * (0.2121144 + a * (0.0742610 + a * 0.0187293))) - (Math.PI * 0.5)).toFloat
    }

  /** A generalization on bias and gain functions that can represent both; this version is branch-less. This is based on [[https://arxiv.org/abs/2010.09714 this micro-paper]] by Jon Barron, which
    * generalizes the earlier bias and gain rational functions by Schlick.
    * @param x
    *   progress through the spline, from 0 to 1, inclusive
    * @param shape
    *   must be greater than or equal to 0; values greater than 1 are "normal interpolations"
    * @param turning
    *   a value between 0.0 and 1.0, inclusive, where the shape changes
    * @return
    *   a float between 0 and 1, inclusive
    */
  def barronSpline(x: Float, shape: Float, turning: Float): Float = {
    val d = turning - x
    val f = java.lang.Float.floatToIntBits(d) >> 31
    val n = f | 1
    ((turning * n - f) * (x + f)) / (java.lang.Float.MIN_VALUE - f + (x + shape * d) * n) - f
  }

  /** A wrapper around [[barronSpline]] to use it as an Interpolation. Useful because it can imitate the wide variety of symmetrical Interpolations by setting turning to 0.5 and shape to some value
    * greater than 1, while also being able to produce the inverse of those interpolations by setting shape to some value between 0 and 1.
    */
  class BiasGain(val shape: Float = 2f, val turning: Float = 0.5f) extends Interpolation {
    override def apply(a: Float): Float = barronSpline(a, shape, turning)
  }
}
