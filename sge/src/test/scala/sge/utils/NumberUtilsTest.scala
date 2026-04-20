/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class NumberUtilsTest extends munit.FunSuite {

  test("floatToIntBits and intBitsToFloat round-trip") {
    val f      = 3.14f
    val bits   = NumberUtils.floatToIntBits(f)
    val result = NumberUtils.intBitsToFloat(bits)
    assertEqualsFloat(result, f, 0f)
  }

  test("floatToRawIntBits matches floatToIntBits for normal values") {
    val f = 42.0f
    assertEquals(NumberUtils.floatToRawIntBits(f), NumberUtils.floatToIntBits(f))
  }

  test("floatToIntBits of zero") {
    assertEquals(NumberUtils.floatToIntBits(0f), 0)
  }

  test("floatToIntBits of negative zero") {
    val bits = NumberUtils.floatToIntBits(-0f)
    assertEquals(bits, java.lang.Float.floatToIntBits(-0f))
  }

  test("floatToIntBits of NaN") {
    val bits = NumberUtils.floatToIntBits(Float.NaN)
    assertEquals(bits, java.lang.Float.floatToIntBits(Float.NaN))
  }

  test("floatToIntBits of positive infinity") {
    val bits = NumberUtils.floatToIntBits(Float.PositiveInfinity)
    assertEquals(bits, 0x7f800000)
  }

  test("floatToIntBits of negative infinity") {
    val bits = NumberUtils.floatToIntBits(Float.NegativeInfinity)
    assertEquals(bits, 0xff800000)
  }

  test("intBitsToFloat of known pattern") {
    // IEEE 754: 1.0f = 0x3f800000
    val f = NumberUtils.intBitsToFloat(0x3f800000)
    assertEqualsFloat(f, 1.0f, 0f)
  }

  test("doubleToLongBits and longBitsToDouble round-trip") {
    val d      = 2.718281828459045
    val bits   = NumberUtils.doubleToLongBits(d)
    val result = NumberUtils.longBitsToDouble(bits)
    assertEqualsDouble(result, d, 0.0)
  }

  test("doubleToLongBits of zero") {
    assertEquals(NumberUtils.doubleToLongBits(0.0), 0L)
  }

  test("doubleToLongBits of NaN") {
    val bits = NumberUtils.doubleToLongBits(Double.NaN)
    assertEquals(bits, java.lang.Double.doubleToLongBits(Double.NaN))
  }

  test("floatToIntColor preserves alpha expansion") {
    // Create a float color from a known int
    val intColor   = 0xaabbccdd // ABGR
    val floatColor = NumberUtils.intToFloatColor(intColor)
    val backToInt  = NumberUtils.floatToIntColor(floatColor)
    // Alpha channel gets expanded from 0-254 to 0-255 range
    // Lower 24 bits should match after masking the alpha channel expansion
    assertEquals(backToInt & 0x00ffffff, intColor & 0x00ffffff)
  }

  test("intToFloatColor masks high alpha bit") {
    // intToFloatColor should mask out the highest alpha bit (0xfe mask)
    val fullyOpaque = 0xffffffff
    val floatColor  = NumberUtils.intToFloatColor(fullyOpaque)
    val bits        = java.lang.Float.floatToRawIntBits(floatColor)
    // The top bit of the alpha byte should be masked off
    assertEquals(bits & 0x01000000, 0)
  }

  test("intToFloatColor of zero") {
    val floatColor = NumberUtils.intToFloatColor(0)
    val bits       = java.lang.Float.floatToRawIntBits(floatColor)
    assertEquals(bits, 0)
  }

  test("floatToIntColor and intToFloatColor near-round-trip") {
    // Due to alpha compression, round-trip is slightly lossy but values should be close
    val original   = 0x80402010 // moderate alpha, low RGB
    val floatColor = NumberUtils.intToFloatColor(original)
    val result     = NumberUtils.floatToIntColor(floatColor)
    // Lower 24 bits should be identical
    assertEquals(result & 0x00ffffff, original & 0x00ffffff)
  }

  test("longBitsToDouble of known pattern") {
    // IEEE 754: 1.0 = 0x3FF0000000000000L
    val d = NumberUtils.longBitsToDouble(0x3ff0000000000000L)
    assertEqualsDouble(d, 1.0, 0.0)
  }

  // NaN payload test is in scalajvm/sge/utils/NumberUtilsJvmTest.scala
  // (JS canonicalizes all NaN payloads, Native may vary)
}
