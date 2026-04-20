package sge
package utils

class NumberUtilsJvmTest extends munit.FunSuite {

  test("floatToRawIntBits preserves NaN payload") {
    // JS canonicalizes all NaN payloads to 0x7FC00000, so this test is JVM-only.
    val customNan = java.lang.Float.intBitsToFloat(0x7f800001)
    val rawBits   = NumberUtils.floatToRawIntBits(customNan)
    assertEquals(rawBits, 0x7f800001)
  }
}
