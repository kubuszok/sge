package sge
package colorful

class FloatColorsSuite extends munit.FunSuite {

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

  private def redChannel(packed: Float): Float = {
    val bits = java.lang.Float.floatToRawIntBits(packed)
    (bits & 0xff) / 255f
  }

  private def greenChannel(packed: Float): Float = {
    val bits = java.lang.Float.floatToRawIntBits(packed)
    (bits >>> 8 & 0xff) / 255f
  }

  private def blueChannel(packed: Float): Float = {
    val bits = java.lang.Float.floatToRawIntBits(packed)
    (bits >>> 16 & 0xff) / 255f
  }

  test("hsl2rgb produces red at hue=0, full saturation, mid lightness") {
    val packed = FloatColors.hsl2rgb(0f, 1f, 0.5f, 1f)
    val r = redChannel(packed)
    val g = greenChannel(packed)
    val b = blueChannel(packed)
    assert(r > 0.9f, s"red should be high, got $r")
    assert(g < 0.1f, s"green should be low, got $g")
    assert(b < 0.1f, s"blue should be low, got $b")
  }

  test("hsl2rgb produces white at lightness=1") {
    val packed = FloatColors.hsl2rgb(0f, 0f, 1f, 1f)
    val r = redChannel(packed)
    val g = greenChannel(packed)
    val b = blueChannel(packed)
    assertApprox(r, 1f, tolerance = 0.05f, clue = "white R")
    assertApprox(g, 1f, tolerance = 0.05f, clue = "white G")
    assertApprox(b, 1f, tolerance = 0.05f, clue = "white B")
  }

  test("hsl2rgb produces black at lightness=0") {
    val packed = FloatColors.hsl2rgb(0f, 0f, 0f, 1f)
    val r = redChannel(packed)
    val g = greenChannel(packed)
    val b = blueChannel(packed)
    assertApprox(r, 0f, tolerance = 0.01f, clue = "black R")
    assertApprox(g, 0f, tolerance = 0.01f, clue = "black G")
    assertApprox(b, 0f, tolerance = 0.01f, clue = "black B")
  }

  test("lerpFloatColors at 0 returns start") {
    val start = FloatColors.hsl2rgb(0f, 1f, 0.5f, 1f)
    val end = FloatColors.hsl2rgb(0.33f, 1f, 0.5f, 1f)
    val result = FloatColors.lerpFloatColors(start, end, 0f)
    assertEquals(
      java.lang.Float.floatToRawIntBits(result),
      java.lang.Float.floatToRawIntBits(start)
    )
  }

  test("lerpFloatColors at 1 returns end") {
    val start = FloatColors.hsl2rgb(0f, 1f, 0.5f, 1f)
    val end = FloatColors.hsl2rgb(0.33f, 1f, 0.5f, 1f)
    val result = FloatColors.lerpFloatColors(start, end, 1f)
    val rBits = java.lang.Float.floatToRawIntBits(result)
    val eBits = java.lang.Float.floatToRawIntBits(end)
    // Check channels individually to allow for minor rounding
    assert(Math.abs((rBits & 0xff) - (eBits & 0xff)) <= 1, "R channel")
    assert(Math.abs((rBits >>> 8 & 0xff) - (eBits >>> 8 & 0xff)) <= 1, "G channel")
    assert(Math.abs((rBits >>> 16 & 0xff) - (eBits >>> 16 & 0xff)) <= 1, "B channel")
  }

  test("setAlpha changes alpha without affecting color channels") {
    val original = FloatColors.hsl2rgb(0.5f, 1f, 0.5f, 1f)
    val modified = FloatColors.setAlpha(original, 0.5f)
    val oBits = java.lang.Float.floatToRawIntBits(original)
    val mBits = java.lang.Float.floatToRawIntBits(modified)
    // Color channels should be identical
    assertEquals(oBits & 0xffffff, mBits & 0xffffff)
    // Alpha should be different
    assert((mBits >>> 24) < (oBits >>> 24), "alpha should be reduced")
  }

  test("multiplyAlpha scales alpha") {
    val original = FloatColors.hsl2rgb(0f, 0f, 0.5f, 1f)
    val halved = FloatColors.multiplyAlpha(original, 0.5f)
    val oBits = java.lang.Float.floatToRawIntBits(original)
    val hBits = java.lang.Float.floatToRawIntBits(halved)
    val origAlpha = oBits >>> 24
    val halfAlpha = hBits >>> 24
    assert(halfAlpha < origAlpha, s"halved alpha $halfAlpha should be less than original $origAlpha")
    // Color channels should be identical
    assertEquals(oBits & 0xffffff, hBits & 0xffffff)
  }
}
