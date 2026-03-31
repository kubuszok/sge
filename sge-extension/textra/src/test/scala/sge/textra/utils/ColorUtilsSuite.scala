package sge
package textra
package utils

import sge.utils.NumberUtils

class ColorUtilsSuite extends munit.FunSuite {

  test("multiplyAlpha(Int) scales alpha channel") {
    val color  = 0xff0000ff // red, full alpha (RGBA)
    val result = ColorUtils.multiplyAlpha(color, 0.5f)
    // alpha = 0xff * 0.5 = ~0x7e (126, rounded to even via & 0xfe)
    val alpha = result & 0xfe
    assert(alpha > 0x70 && alpha < 0x82, s"Alpha $alpha should be ~0x7e")
    // RGB should be unchanged
    assertEquals(result & 0xffffff00, color & 0xffffff00)
  }

  test("multiplyAlpha(Float) scales packed float alpha") {
    // ABGR packed float: alpha stored in bits 25-31 (7 bits, max 127)
    // Construct: alpha=63 (half), R=G=B=255
    val color  = java.lang.Float.intBitsToFloat((63 << 25) | 0x00_ff_ff_ff)
    val result = ColorUtils.multiplyAlpha(color, 0.5f)
    val bits   = NumberUtils.floatToIntBits(result)
    val alpha  = bits >>> 25
    // 63 * 0.5 = ~31
    assert(alpha > 25 && alpha < 38, s"Alpha $alpha should be ~31")
  }

  test("lerpColors(Float) interpolates packed float colors") {
    // Black with alpha 63
    val black = java.lang.Float.intBitsToFloat((63 << 25) | 0x00_00_00_00)
    // White with alpha 63
    val white = java.lang.Float.intBitsToFloat((63 << 25) | 0x00_ff_ff_ff)
    val mid   = ColorUtils.lerpColors(black, white, 0.5f)
    val bits  = NumberUtils.floatToIntBits(mid)
    val r     = bits & 0xff
    val g     = (bits >>> 8) & 0xff
    val b     = (bits >>> 16) & 0xff
    // Should be approximately halfway (~127)
    assert(r > 100 && r < 156, s"Red $r should be ~127")
    assert(g > 100 && g < 156, s"Green $g should be ~127")
    assert(b > 100 && b < 156, s"Blue $b should be ~127")
  }

  test("lerpColorsMultiplyAlpha blends and scales alpha") {
    val black  = java.lang.Float.intBitsToFloat((63 << 25) | 0x00_00_00_00)
    val white  = java.lang.Float.intBitsToFloat((63 << 25) | 0x00_ff_ff_ff)
    val result = ColorUtils.lerpColorsMultiplyAlpha(black, white, 0.5f, 0.5f)
    val bits   = NumberUtils.floatToIntBits(result)
    val alpha  = bits >>> 25
    // Alpha from start (63) * 0.5 = ~31
    assert(alpha > 25 && alpha < 38, s"Alpha $alpha should be ~31")
  }

  test("hsl2rgb roundtrip via rgb2hsl") {
    // Pure red: H=0, S=1, L=0.5
    val rgba = ColorUtils.hsl2rgb(0f, 1f, 0.5f, 1f)
    val r    = (rgba >>> 24) & 0xff
    val g    = (rgba >>> 16) & 0xff
    val b    = (rgba >>> 8) & 0xff
    assert(r > 240, s"Red channel $r should be ~255")
    assert(g < 15, s"Green channel $g should be ~0")
    assert(b < 15, s"Blue channel $b should be ~0")
  }
}
