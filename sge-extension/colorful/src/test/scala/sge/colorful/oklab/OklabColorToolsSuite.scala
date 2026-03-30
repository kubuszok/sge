package sge
package colorful
package oklab

class OklabColorToolsSuite extends munit.FunSuite {

  private val Epsilon = 0.02f

  @scala.annotation.nowarn("msg=unused private member")
  private def assertApprox(actual: Float, expected: Float, tolerance: Float = Epsilon, clue: String = "")(using
    munit.Location
  ): Unit = {
    val msg = if (clue.nonEmpty) s"$clue: " else ""
    assert(
      Math.abs(actual - expected) <= tolerance,
      s"${msg}expected ~$expected but got $actual (diff=${Math.abs(actual - expected)}, tolerance=$tolerance)"
    )
  }

  test("oklab packs and channelL/channelA/channelB/alpha unpack correctly") {
    val packed = ColorTools.oklab(0.6f, 0.4f, 0.7f, 0.9f)
    assertApprox(ColorTools.channelL(packed), 0.6f, clue = "L")
    assertApprox(ColorTools.channelA(packed), 0.4f, clue = "A")
    assertApprox(ColorTools.channelB(packed), 0.7f, clue = "B")
    assertApprox(ColorTools.alpha(packed), 0.9f, clue = "alpha")
  }

  test("oklab pack/unpack roundtrip at extremes") {
    val black = ColorTools.oklab(0f, 0.5f, 0.5f, 1f)
    assertApprox(ColorTools.channelL(black), 0f, clue = "black L")
    assertApprox(ColorTools.channelA(black), 0.5f, clue = "black A")
    assertApprox(ColorTools.channelB(black), 0.5f, clue = "black B")

    val white = ColorTools.oklab(1f, 0.5f, 0.5f, 1f)
    assertApprox(ColorTools.channelL(white), 1f, clue = "white L")
  }

  test("pure white in Oklab has high lightness") {
    // White is RGB (1,1,1) => should have L close to 1 in Oklab
    val whiteOklab = ColorTools.fromRGBA(1f, 1f, 1f, 1f)
    assert(ColorTools.channelL(whiteOklab) > 0.9f, s"white L=${ColorTools.channelL(whiteOklab)}")
  }

  test("pure black in Oklab has zero lightness") {
    val blackOklab = ColorTools.fromRGBA(0f, 0f, 0f, 1f)
    assert(ColorTools.channelL(blackOklab) < 0.01f, s"black L=${ColorTools.channelL(blackOklab)}")
  }

  test("RGB to Oklab to RGB roundtrip is close to original") {
    // Test with a known mid-range color: pure red
    val redRGBA8888 = 0xff0000ff // R=255, G=0, B=0, A=255
    val oklabPacked = ColorTools.fromRGBA8888(redRGBA8888)
    val backRGBA = ColorTools.toRGBA8888(oklabPacked)
    val rOrig = (redRGBA8888 >>> 24) & 0xff
    val gOrig = (redRGBA8888 >>> 16) & 0xff
    val bOrig = (redRGBA8888 >>> 8) & 0xff
    val rBack = (backRGBA >>> 24) & 0xff
    val gBack = (backRGBA >>> 16) & 0xff
    val bBack = (backRGBA >>> 8) & 0xff
    assert(Math.abs(rOrig - rBack) <= 3, s"red channel: orig=$rOrig, back=$rBack")
    assert(Math.abs(gOrig - gBack) <= 3, s"green channel: orig=$gOrig, back=$gBack")
    assert(Math.abs(bOrig - bBack) <= 3, s"blue channel: orig=$bOrig, back=$bBack")
  }

  test("toRGBA8888 and fromRGBA8888 roundtrip for gray") {
    val grayRGBA = 0x808080ff
    val oklab = ColorTools.fromRGBA8888(grayRGBA)
    val back = ColorTools.toRGBA8888(oklab)
    val rBack = (back >>> 24) & 0xff
    val gBack = (back >>> 16) & 0xff
    val bBack = (back >>> 8) & 0xff
    assert(Math.abs(0x80 - rBack) <= 3, s"gray R: expected ~128, got $rBack")
    assert(Math.abs(0x80 - gBack) <= 3, s"gray G: expected ~128, got $gBack")
    assert(Math.abs(0x80 - bBack) <= 3, s"gray B: expected ~128, got $bBack")
  }

  test("lighten increases L channel") {
    val midGray = ColorTools.oklab(0.5f, 0.5f, 0.5f, 1f)
    val lighter = ColorTools.lighten(midGray, 0.3f)
    assert(
      ColorTools.channelL(lighter) > ColorTools.channelL(midGray),
      s"lighten should increase L: original=${ColorTools.channelL(midGray)}, lighter=${ColorTools.channelL(lighter)}"
    )
  }

  test("darken decreases L channel") {
    val midGray = ColorTools.oklab(0.5f, 0.5f, 0.5f, 1f)
    val darker = ColorTools.darken(midGray, 0.3f)
    assert(
      ColorTools.channelL(darker) < ColorTools.channelL(midGray),
      s"darken should decrease L: original=${ColorTools.channelL(midGray)}, darker=${ColorTools.channelL(darker)}"
    )
  }

  test("lighten preserves A and B channels") {
    val color = ColorTools.oklab(0.5f, 0.3f, 0.7f, 0.8f)
    val lighter = ColorTools.lighten(color, 0.2f)
    assertApprox(ColorTools.channelA(lighter), ColorTools.channelA(color), clue = "A preserved")
    assertApprox(ColorTools.channelB(lighter), ColorTools.channelB(color), clue = "B preserved")
  }

  test("darken preserves A and B channels") {
    val color = ColorTools.oklab(0.5f, 0.3f, 0.7f, 0.8f)
    val darker = ColorTools.darken(color, 0.2f)
    assertApprox(ColorTools.channelA(darker), ColorTools.channelA(color), clue = "A preserved")
    assertApprox(ColorTools.channelB(darker), ColorTools.channelB(color), clue = "B preserved")
  }

  test("alpha is preserved through lighten/darken") {
    val color = ColorTools.oklab(0.5f, 0.5f, 0.5f, 0.6f)
    val lighter = ColorTools.lighten(color, 0.3f)
    val darker = ColorTools.darken(color, 0.3f)
    assertApprox(ColorTools.alpha(lighter), ColorTools.alpha(color), clue = "lighten alpha")
    assertApprox(ColorTools.alpha(darker), ColorTools.alpha(color), clue = "darken alpha")
  }

  test("chroma is zero for gray colors") {
    val gray = ColorTools.oklab(0.5f, 0.5f, 0.5f, 1f)
    assertApprox(ColorTools.chroma(gray), 0f, tolerance = 0.01f, clue = "gray chroma")
  }

  test("red/green/blue accessors return valid sRGB for known color") {
    val whiteOklab = ColorTools.fromRGBA(1f, 1f, 1f, 1f)
    assertApprox(ColorTools.red(whiteOklab), 1f, tolerance = 0.05f, clue = "white red")
    assertApprox(ColorTools.green(whiteOklab), 1f, tolerance = 0.05f, clue = "white green")
    assertApprox(ColorTools.blue(whiteOklab), 1f, tolerance = 0.05f, clue = "white blue")
  }

  test("fromRGBA preserves alpha") {
    val packed = ColorTools.fromRGBA(0.5f, 0.5f, 0.5f, 0.75f)
    assertApprox(ColorTools.alpha(packed), 0.75f, tolerance = 0.02f, clue = "alpha")
  }

  test("inGamut returns true for gray center") {
    assert(ColorTools.inGamut(0.5f, 0.5f, 0.5f), "mid-gray should be in gamut")
  }

  test("limitToGamut returns valid color for extreme values") {
    val clamped = ColorTools.limitToGamut(0.5f, 0f, 1f, 1f)
    assert(!java.lang.Float.isNaN(clamped), "limitToGamut should not produce NaN")
  }
}
