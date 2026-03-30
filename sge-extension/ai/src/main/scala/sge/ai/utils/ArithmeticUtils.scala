/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/ArithmeticUtils.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `MathUtils` -> `sge.math.MathUtils`
 *   Convention: split packages, `object` instead of `final class` with private constructor
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package utils

import sge.math.MathUtils

/** Some useful math functions.
  *
  * @author
  *   davebaol (original implementation)
  */
object ArithmeticUtils {

  /** Wraps the given angle to the range [-PI, PI]
    * @param a
    *   the angle in radians
    * @return
    *   the given angle wrapped to the range [-PI, PI]
    */
  def wrapAngleAroundZero(a: Float): Float =
    if (a >= 0) {
      var rotation = a % MathUtils.PI2
      if (rotation > MathUtils.PI) rotation -= MathUtils.PI2
      rotation
    } else {
      var rotation = -a % MathUtils.PI2
      if (rotation > MathUtils.PI) rotation -= MathUtils.PI2
      -rotation
    }

  /** Returns the greatest common divisor of two positive numbers using the "binary gcd" method which avoids division and modulo operations. See Knuth 4.5.2 algorithm B. The algorithm is due to Josef
    * Stein (1961).
    *
    * Special cases:
    *   - The result of `gcd(x, x)`, `gcd(0, x)` and `gcd(x, 0)` is the value of `x`.
    *   - The invocation `gcd(0, 0)` is the only one which returns `0`.
    *
    * @param a
    *   a non-negative number.
    * @param b
    *   a non-negative number.
    * @return
    *   the greatest common divisor.
    */
  def gcdPositive(a: Int, b: Int): Int = {
    if (a == 0) return b
    if (b == 0) return a

    var va = a
    var vb = b

    // Make "a" and "b" odd, keeping track of common power of 2.
    val aTwos = Integer.numberOfTrailingZeros(va)
    va >>= aTwos
    val bTwos = Integer.numberOfTrailingZeros(vb)
    vb >>= bTwos
    val shift = if (aTwos <= bTwos) aTwos else bTwos // min(aTwos, bTwos)

    // "a" and "b" are positive.
    // If a > b then "gcd(a, b)" is equal to "gcd(a - b, b)".
    // If a < b then "gcd(a, b)" is equal to "gcd(b - a, a)".
    // Hence, in the successive iterations:
    // "a" becomes the absolute difference of the current values,
    // "b" becomes the minimum of the current values.
    while (va != vb) {
      val delta = va - vb
      vb = if (va <= vb) va else vb // min(a, b)
      va = if (delta < 0) -delta else delta // abs(delta)

      // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
      va >>= Integer.numberOfTrailingZeros(va)
    }

    // Recover the common power of 2.
    va << shift
  }

  /** Returns the greatest common divisor of the given absolute values. This implementation uses [[gcdPositive(Int,Int)]] and has the same special cases.
    *
    * @param args
    *   non-negative numbers
    * @return
    *   the greatest common divisor.
    */
  def gcdPositive(args: Int*): Int = {
    require(args.length >= 2, "gcdPositive requires at least two arguments")
    var result = args(0)
    var i      = 1
    while (i < args.length) {
      result = gcdPositive(result, args(i))
      i += 1
    }
    result
  }

  /** Returns the least common multiple of the absolute value of two numbers, using the formula `lcm(a, b) = (a / gcd(a, b)) * b`.
    *
    * Special cases:
    *   - The result of `lcm(0, x)` and `lcm(x, 0)` is `0` for any `x`.
    *
    * @param a
    *   a non-negative number.
    * @param b
    *   a non-negative number.
    * @return
    *   the least common multiple, never negative.
    * @throws ArithmeticException
    *   if the result cannot be represented as a non-negative `int` value.
    */
  def lcmPositive(a: Int, b: Int): Int = {
    if (a == 0 || b == 0) return 0
    val lcm = Math.abs(mulAndCheck(a / gcdPositive(a, b), b))
    if (lcm == Int.MinValue) {
      throw new ArithmeticException("overflow: lcm(" + a + ", " + b + ") > 2^31")
    }
    lcm
  }

  /** Returns the least common multiple of the given absolute values. This implementation uses [[lcmPositive(Int,Int)]] and has the same special cases.
    *
    * @param args
    *   non-negative numbers
    * @return
    *   the least common multiple, never negative.
    * @throws ArithmeticException
    *   if the result cannot be represented as a non-negative `int` value.
    */
  def lcmPositive(args: Int*): Int = {
    require(args.length >= 2, "lcmPositive requires at least two arguments")
    var result = args(0)
    var i      = 1
    while (i < args.length) {
      result = lcmPositive(result, args(i))
      i += 1
    }
    result
  }

  /** Multiply two integers, checking for overflow.
    *
    * @param x
    *   first factor
    * @param y
    *   second factor
    * @return
    *   the product `x * y`.
    * @throws ArithmeticException
    *   if the result can not be represented as an `int`.
    */
  def mulAndCheck(x: Int, y: Int): Int = {
    val m: Long = x.toLong * y.toLong
    if (m < Int.MinValue || m > Int.MaxValue) {
      throw new ArithmeticException()
    }
    m.toInt
  }
}
