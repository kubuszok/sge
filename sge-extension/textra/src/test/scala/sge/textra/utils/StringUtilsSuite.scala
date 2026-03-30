package sge
package textra
package utils

class StringUtilsSuite extends munit.FunSuite {

  test("join with comma delimiter") {
    assertEquals(StringUtils.join(", ", "a", "b", "c"), "a, b, c")
  }

  test("join with empty items returns empty string") {
    assertEquals(StringUtils.join(", "), "")
  }

  test("join with single item returns that item") {
    assertEquals(StringUtils.join(", ", "hello"), "hello")
  }

  test("hexCode converts hex digit chars correctly") {
    assertEquals(StringUtils.hexCode('0'), 0)
    assertEquals(StringUtils.hexCode('9'), 9)
    assertEquals(StringUtils.hexCode('a'), 10)
    assertEquals(StringUtils.hexCode('f'), 15)
    assertEquals(StringUtils.hexCode('A'), 10)
    assertEquals(StringUtils.hexCode('F'), 15)
  }

  test("unsignedHex produces 8-char hex string for Int") {
    assertEquals(StringUtils.unsignedHex(0), "00000000")
    assertEquals(StringUtils.unsignedHex(0xff), "000000FF")
    assertEquals(StringUtils.unsignedHex(0xdeadbeef.toInt), "DEADBEEF")
  }

  test("intFromDec parses decimal strings") {
    assertEquals(StringUtils.intFromDec("12345", 0, 5), 12345)
    assertEquals(StringUtils.intFromDec("-42", 0, 3), -42)
    assertEquals(StringUtils.intFromDec("0", 0, 1), 0)
  }

  test("intFromHex parses hexadecimal strings") {
    assertEquals(StringUtils.intFromHex("FF", 0, 2), 0xff)
    assertEquals(StringUtils.intFromHex("0", 0, 1), 0)
    assertEquals(StringUtils.intFromHex("DEAD", 0, 4), 0xdead)
  }
}
