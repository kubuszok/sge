package sge
package colorful
package rgb

class RgbColorToolsSuite extends munit.FunSuite {

  private val Epsilon = 0.02f

  @scala.annotation.nowarn("msg=unused private member")
  private def assertApprox(actual: Float, expected: Float, tolerance: Float = Epsilon, clue: String = "")(using
    munit.Location
  ): Unit = {
    val msg = if (clue.nonEmpty) s"$clue: " else ""
    assert(
      Math.abs(actual - expected) <= tolerance,
      s"${msg}expected ~$expected but got $actual (diff=${Math.abs(actual - expected)})"
    )
  }

  test("rgb packs and red/green/blue/alpha unpack correctly") {
    val packed = ColorTools.rgb(0.8f, 0.4f, 0.2f, 1f)
    assertApprox(ColorTools.red(packed), 0.8f, clue = "red")
    assertApprox(ColorTools.green(packed), 0.4f, clue = "green")
    assertApprox(ColorTools.blue(packed), 0.2f, clue = "blue")
    assertApprox(ColorTools.alpha(packed), 1f, clue = "alpha")
  }

  test("rgb pack/unpack at extremes") {
    val black = ColorTools.rgb(0f, 0f, 0f, 1f)
    assertApprox(ColorTools.red(black), 0f, clue = "black R")
    assertApprox(ColorTools.green(black), 0f, clue = "black G")
    assertApprox(ColorTools.blue(black), 0f, clue = "black B")

    val white = ColorTools.rgb(1f, 1f, 1f, 1f)
    assertApprox(ColorTools.red(white), 1f, clue = "white R")
    assertApprox(ColorTools.green(white), 1f, clue = "white G")
    assertApprox(ColorTools.blue(white), 1f, clue = "white B")
  }

  test("redInt/greenInt/blueInt return 0-255 range values") {
    val packed = ColorTools.rgb(1f, 0.5f, 0f, 1f)
    assertEquals(ColorTools.redInt(packed), 255)
    assert(Math.abs(ColorTools.greenInt(packed) - 127) <= 1, s"greenInt=${ColorTools.greenInt(packed)}")
    assertEquals(ColorTools.blueInt(packed), 0)
  }

  test("alpha channel packing and unpacking") {
    val packed = ColorTools.rgb(0.5f, 0.5f, 0.5f, 0.75f)
    assertApprox(ColorTools.alpha(packed), 0.75f, tolerance = 0.02f, clue = "alpha")
  }

  test("lighten moves color towards white") {
    val red = ColorTools.rgb(1f, 0f, 0f, 1f)
    val lighter = ColorTools.lighten(red, 0.5f)
    assert(ColorTools.green(lighter) > 0f, s"green should increase: ${ColorTools.green(lighter)}")
    assert(ColorTools.blue(lighter) > 0f, s"blue should increase: ${ColorTools.blue(lighter)}")
    assertApprox(ColorTools.red(lighter), 1f, clue = "red stays at max")
  }

  test("darken moves color towards black") {
    val white = ColorTools.rgb(1f, 1f, 1f, 1f)
    val darker = ColorTools.darken(white, 0.5f)
    assert(ColorTools.red(darker) < 1f, s"red should decrease: ${ColorTools.red(darker)}")
    assert(ColorTools.green(darker) < 1f, s"green should decrease: ${ColorTools.green(darker)}")
    assert(ColorTools.blue(darker) < 1f, s"blue should decrease: ${ColorTools.blue(darker)}")
  }

  test("lighten and darken preserve alpha") {
    val color = ColorTools.rgb(0.5f, 0.5f, 0.5f, 0.6f)
    val lighter = ColorTools.lighten(color, 0.3f)
    val darker = ColorTools.darken(color, 0.3f)
    assertApprox(ColorTools.alpha(lighter), ColorTools.alpha(color), clue = "lighten alpha")
    assertApprox(ColorTools.alpha(darker), ColorTools.alpha(color), clue = "darken alpha")
  }

  test("hue of pure red is approximately 0") {
    val red = ColorTools.rgb(1f, 0f, 0f, 1f)
    val h = ColorTools.hue(red)
    assert(h < 0.05f || h > 0.95f, s"red hue should be ~0 or ~1, got $h")
  }

  test("lightness of white is close to 1") {
    val white = ColorTools.rgb(1f, 1f, 1f, 1f)
    assertApprox(ColorTools.lightness(white), 1f, tolerance = 0.05f, clue = "white lightness")
  }

  test("lightness of black is 0") {
    val black = ColorTools.rgb(0f, 0f, 0f, 1f)
    assertApprox(ColorTools.lightness(black), 0f, tolerance = 0.01f, clue = "black lightness")
  }

  test("toRGBA8888 and fromRGBA8888 roundtrip") {
    val packed = ColorTools.rgb(0.6f, 0.3f, 0.9f, 1f)
    val rgba = ColorTools.toRGBA8888(packed)
    val back = ColorTools.fromRGBA8888(rgba)
    assertApprox(ColorTools.red(back), ColorTools.red(packed), tolerance = 0.01f, clue = "red roundtrip")
    assertApprox(ColorTools.green(back), ColorTools.green(packed), tolerance = 0.01f, clue = "green roundtrip")
    assertApprox(ColorTools.blue(back), ColorTools.blue(packed), tolerance = 0.01f, clue = "blue roundtrip")
  }
}
