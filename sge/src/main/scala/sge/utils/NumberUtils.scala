/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/NumberUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `final class` with static methods -> Scala `object`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: NumberUtils,doubleToLongBits,floatToIntBits,floatToIntColor,floatToRawIntBits,intBits,intBitsToFloat,intToFloatColor,longBitsToDouble
 * Covenant-source-reference: com/badlogic/gdx/utils/NumberUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package utils

object NumberUtils {
  def floatToIntBits(value: Float): Int =
    java.lang.Float.floatToIntBits(value)

  def floatToRawIntBits(value: Float): Int =
    java.lang.Float.floatToRawIntBits(value)

  /** Converts the color from a float ABGR encoding to an int ABGR encoding. The alpha is expanded from 0-254 in the float encoding (see {@link #intToFloatColor(int)} ) to 0-255, which means
    * converting from int to float and back to int can be lossy.
    */
  def floatToIntColor(value: Float): Int = {
    var intBits = java.lang.Float.floatToRawIntBits(value)
    intBits |= ((intBits >>> 24) * (255f / 254f)).toInt << 24
    intBits
  }

  /** Encodes the ABGR int color as a float. The alpha is compressed to use only even numbers between 0-254 to avoid using bits in the NaN range (see {@link Float#intBitsToFloat(int)} javadocs).
    * Rendering which uses colors encoded as floats should expand the 0-254 back to 0-255, else colors cannot be fully opaque.
    */
  def intToFloatColor(value: Int): Float =
    java.lang.Float.intBitsToFloat(value & 0xfeffffff)

  def intBitsToFloat(value: Int): Float =
    java.lang.Float.intBitsToFloat(value)

  def doubleToLongBits(value: Double): Long =
    java.lang.Double.doubleToLongBits(value)

  def longBitsToDouble(value: Long): Double =
    java.lang.Double.longBitsToDouble(value)
}
