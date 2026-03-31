/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump
package util

/** Math utilities used internally by jbump. */
object MathUtils {

  val DELTA: Float = 1e-5f

  def sign(x: Float): Int =
    if (x > 0) 1
    else if (x < 0) -1
    else 0

  def nearest(x: Float, a: Float, b: Float): Float =
    if (Math.abs(a - x) < Math.abs(b - x)) a
    else b
}
