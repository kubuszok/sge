package sge
package textra
package utils

import java.util.Random as JRandom

class StringUtilsSuite extends munit.FunSuite {

  // ---------- join ----------

  test("join with comma delimiter") {
    assertEquals(StringUtils.join(", ", "a", "b", "c"), "a, b, c")
  }

  test("join with empty items returns empty string") {
    assertEquals(StringUtils.join(", "), "")
  }

  test("join with single item returns that item") {
    assertEquals(StringUtils.join(", ", "hello"), "hello")
  }

  // ---------- hexCode ----------

  test("hexCode converts hex digit chars correctly") {
    assertEquals(StringUtils.hexCode('0'), 0)
    assertEquals(StringUtils.hexCode('9'), 9)
    assertEquals(StringUtils.hexCode('a'), 10)
    assertEquals(StringUtils.hexCode('f'), 15)
    assertEquals(StringUtils.hexCode('A'), 10)
    assertEquals(StringUtils.hexCode('F'), 15)
  }

  // ---------- unsignedHex (String) ----------

  test("unsignedHex produces 8-char hex string for Int") {
    assertEquals(StringUtils.unsignedHex(0), "00000000")
    assertEquals(StringUtils.unsignedHex(0xff), "000000FF")
    assertEquals(StringUtils.unsignedHex(0xdeadbeef.toInt), "DEADBEEF")
  }

  test("unsignedHex produces 16-char hex string for Long") {
    assertEquals(StringUtils.unsignedHex(0L), "0000000000000000")
    assertEquals(StringUtils.unsignedHex(0xffL), "00000000000000FF")
    assertEquals(StringUtils.unsignedHex(0x123456789abcdefL), "0123456789ABCDEF")
  }

  // ---------- unsignedHexArray ----------

  test("unsignedHexArray for Int returns 8-char array") {
    val arr = StringUtils.unsignedHexArray(0)
    assertEquals(arr.length, 8)
    assertEquals(new String(arr), "00000000")
  }

  test("unsignedHexArray for Int matches unsignedHex string") {
    val values = Array(0, 1, 0xff, 0xdeadbeef.toInt, Int.MaxValue, Int.MinValue)
    values.foreach { v =>
      assertEquals(new String(StringUtils.unsignedHexArray(v)), StringUtils.unsignedHex(v))
    }
  }

  test("unsignedHexArray for Long returns 16-char array") {
    val arr = StringUtils.unsignedHexArray(0L)
    assertEquals(arr.length, 16)
    assertEquals(new String(arr), "0000000000000000")
  }

  test("unsignedHexArray for Long matches unsignedHex string") {
    val values = Array(0L, 1L, 0xffL, 0x123456789abcdefL, Long.MaxValue, Long.MinValue)
    values.foreach { v =>
      assertEquals(new String(StringUtils.unsignedHexArray(v)), StringUtils.unsignedHex(v))
    }
  }

  // ---------- shuffleWords ----------

  test("shuffleWords with seeded random produces deterministic result") {
    val text   = "the quick brown fox jumps over the lazy dog"
    val gen    = new JRandom(42L)
    val result = StringUtils.shuffleWords(text, gen)
    // Verify it contains all the same words
    val originalWords = text.split("\\s+").sorted.toList
    val shuffledWords = result.split("\\s+").sorted.toList
    assertEquals(shuffledWords, originalWords)
  }

  test("shuffleWords with same seed produces same result") {
    val text = "alpha beta gamma delta epsilon"
    val r1   = StringUtils.shuffleWords(text, new JRandom(123L))
    val r2   = StringUtils.shuffleWords(text, new JRandom(123L))
    assertEquals(r1, r2)
  }

  test("shuffleWords with single word returns that word") {
    val result = StringUtils.shuffleWords("hello", new JRandom(0L))
    assertEquals(result, "hello")
  }

  test("shuffleWords with empty string returns empty") {
    val result = StringUtils.shuffleWords("", new JRandom(0L))
    assertEquals(result, "")
  }

  // ---------- intFromDec ----------

  test("intFromDec parses decimal strings") {
    assertEquals(StringUtils.intFromDec("12345", 0, 5), 12345)
    assertEquals(StringUtils.intFromDec("-42", 0, 3), -42)
    assertEquals(StringUtils.intFromDec("0", 0, 1), 0)
  }

  test("intFromDec with plus sign") {
    assertEquals(StringUtils.intFromDec("+99", 0, 3), 99)
  }

  test("intFromDec with non-digit returns 0") {
    assertEquals(StringUtils.intFromDec("abc", 0, 3), 0)
  }

  test("intFromDec with partial range") {
    assertEquals(StringUtils.intFromDec("xx123yy", 2, 5), 123)
  }

  // ---------- intFromHex ----------

  test("intFromHex parses hexadecimal strings") {
    assertEquals(StringUtils.intFromHex("FF", 0, 2), 0xff)
    assertEquals(StringUtils.intFromHex("0", 0, 1), 0)
    assertEquals(StringUtils.intFromHex("DEAD", 0, 4), 0xdead)
  }

  test("intFromHex with lowercase") {
    assertEquals(StringUtils.intFromHex("ff", 0, 2), 0xff)
    assertEquals(StringUtils.intFromHex("abcdef", 0, 6), 0xabcdef)
  }

  test("intFromHex with negative sign") {
    assertEquals(StringUtils.intFromHex("-FF", 0, 3), -0xff)
  }

  // ---------- longFromDec ----------

  test("longFromDec parses long values") {
    assertEquals(StringUtils.longFromDec("1234567890123", 0, 13), 1234567890123L)
    assertEquals(StringUtils.longFromDec("-42", 0, 3), -42L)
    assertEquals(StringUtils.longFromDec("0", 0, 1), 0L)
  }

  // ---------- longFromHex ----------

  test("longFromHex parses long hex values") {
    assertEquals(StringUtils.longFromHex("FF", 0, 2), 0xffL)
    assertEquals(StringUtils.longFromHex("ABCDEF01", 0, 8), 0xabcdef01L)
  }

  // ---------- floatFromDec ----------

  test("floatFromDec parses float values") {
    assertEqualsFloat(StringUtils.floatFromDec("3.14", 0, 4), 3.14f, 0.01f)
    assertEqualsFloat(StringUtils.floatFromDec("-2.5", 0, 4), -2.5f, 0.01f)
    assertEqualsFloat(StringUtils.floatFromDec("42", 0, 2), 42f, 0.01f)
  }

  test("floatFromDec returns 0 for invalid input") {
    assertEqualsFloat(StringUtils.floatFromDec("abc", 0, 3), 0f, 0.001f)
  }

  // ---------- indexAfter ----------

  test("indexAfter finds position after search string") {
    assertEquals(StringUtils.indexAfter("hello world", "lo", 0), 5)
  }

  test("indexAfter returns text length when not found") {
    assertEquals(StringUtils.indexAfter("hello", "xyz", 0), 5)
  }

  // ---------- safeSubstring ----------

  test("safeSubstring returns empty for null-like cases") {
    assertEquals(StringUtils.safeSubstring("", 0, 5), "")
  }

  test("safeSubstring clamps indices") {
    assertEquals(StringUtils.safeSubstring("hello", -1, 100), "hello")
    assertEquals(StringUtils.safeSubstring("hello", 2, 4), "ll")
  }

  test("safeSubstring returns empty when begin >= end") {
    assertEquals(StringUtils.safeSubstring("hello", 3, 2), "")
  }

  // ---------- isLowerCase / isUpperCase ----------

  test("isLowerCase and isUpperCase") {
    assert(StringUtils.isLowerCase('a'))
    assert(!StringUtils.isLowerCase('A'))
    assert(StringUtils.isUpperCase('A'))
    assert(!StringUtils.isUpperCase('a'))
  }

  // ---------- appendUnsignedHex ----------

  test("appendUnsignedHex for Int appends 8 hex chars") {
    val sb = new StringBuilder
    StringUtils.appendUnsignedHex(sb, 0xff)
    assertEquals(sb.toString, "000000FF")
  }

  test("appendUnsignedHex for Long appends 16 hex chars") {
    val sb = new StringBuilder
    StringUtils.appendUnsignedHex(sb, 0xffL)
    assertEquals(sb.toString, "00000000000000FF")
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(using munit.Location): Unit =
    assert(Math.abs(actual - expected) <= delta, s"expected $expected +/- $delta but got $actual")
}
