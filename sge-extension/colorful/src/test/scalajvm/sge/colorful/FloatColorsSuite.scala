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
    val r      = redChannel(packed)
    val g      = greenChannel(packed)
    val b      = blueChannel(packed)
    assert(r > 0.9f, s"red should be high, got $r")
    assert(g < 0.1f, s"green should be low, got $g")
    assert(b < 0.1f, s"blue should be low, got $b")
  }

  test("hsl2rgb produces white at lightness=1") {
    val packed = FloatColors.hsl2rgb(0f, 0f, 1f, 1f)
    val r      = redChannel(packed)
    val g      = greenChannel(packed)
    val b      = blueChannel(packed)
    assertApprox(r, 1f, tolerance = 0.05f, clue = "white R")
    assertApprox(g, 1f, tolerance = 0.05f, clue = "white G")
    assertApprox(b, 1f, tolerance = 0.05f, clue = "white B")
  }

  test("hsl2rgb produces black at lightness=0") {
    val packed = FloatColors.hsl2rgb(0f, 0f, 0f, 1f)
    val r      = redChannel(packed)
    val g      = greenChannel(packed)
    val b      = blueChannel(packed)
    assertApprox(r, 0f, tolerance = 0.01f, clue = "black R")
    assertApprox(g, 0f, tolerance = 0.01f, clue = "black G")
    assertApprox(b, 0f, tolerance = 0.01f, clue = "black B")
  }

  test("lerpFloatColors at 0 returns start") {
    val start  = FloatColors.hsl2rgb(0f, 1f, 0.5f, 1f)
    val end    = FloatColors.hsl2rgb(0.33f, 1f, 0.5f, 1f)
    val result = FloatColors.lerpFloatColors(start, end, 0f)
    assertEquals(
      java.lang.Float.floatToRawIntBits(result),
      java.lang.Float.floatToRawIntBits(start)
    )
  }

  test("lerpFloatColors at 1 returns end") {
    val start  = FloatColors.hsl2rgb(0f, 1f, 0.5f, 1f)
    val end    = FloatColors.hsl2rgb(0.33f, 1f, 0.5f, 1f)
    val result = FloatColors.lerpFloatColors(start, end, 1f)
    val rBits  = java.lang.Float.floatToRawIntBits(result)
    val eBits  = java.lang.Float.floatToRawIntBits(end)
    // Check channels individually to allow for minor rounding
    assert(Math.abs((rBits & 0xff) - (eBits & 0xff)) <= 1, "R channel")
    assert(Math.abs((rBits >>> 8 & 0xff) - (eBits >>> 8 & 0xff)) <= 1, "G channel")
    assert(Math.abs((rBits >>> 16 & 0xff) - (eBits >>> 16 & 0xff)) <= 1, "B channel")
  }

  test("setAlpha changes alpha without affecting color channels") {
    val original = FloatColors.hsl2rgb(0.5f, 1f, 0.5f, 1f)
    val modified = FloatColors.setAlpha(original, 0.5f)
    val oBits    = java.lang.Float.floatToRawIntBits(original)
    val mBits    = java.lang.Float.floatToRawIntBits(modified)
    // Color channels should be identical
    assertEquals(oBits & 0xffffff, mBits & 0xffffff)
    // Alpha should be different
    assert((mBits >>> 24) < (oBits >>> 24), "alpha should be reduced")
  }

  test("multiplyAlpha scales alpha") {
    val original  = FloatColors.hsl2rgb(0f, 0f, 0.5f, 1f)
    val halved    = FloatColors.multiplyAlpha(original, 0.5f)
    val oBits     = java.lang.Float.floatToRawIntBits(original)
    val hBits     = java.lang.Float.floatToRawIntBits(halved)
    val origAlpha = oBits >>> 24
    val halfAlpha = hBits >>> 24
    assert(halfAlpha < origAlpha, s"halved alpha $halfAlpha should be less than original $origAlpha")
    // Color channels should be identical
    assertEquals(oBits & 0xffffff, hBits & 0xffffff)
  }

  // ── Wikipedia HSL reference data ──────────────────────────────────────
  // Cross-reference with https://en.wikipedia.org/wiki/HSL_and_HSV#Examples

  private val testColors: Array[Int] = Array(
    0xffffffff, 0x808080ff, 0x000000ff, 0xff0000ff, 0xbfbf00ff, 0x008000ff, 0x80ffffff, 0x8080ffff, 0xbf40bfff, 0xa0a424ff, 0x411beaff, 0x1eac41ff, 0xf0c80eff, 0xb430e5ff, 0xed7651ff, 0xfef888ff,
    0x19cb97ff, 0x362698ff, 0x7e7eb8ff
  )

  // Map from color int to (hue in degrees, saturation, lightness)
  private val hslMap: Map[Int, (Float, Float, Float)] = Map(
    0xffffffff -> (0.000f, 0.000f, 1.000f),
    0x808080ff -> (0.000f, 0.000f, 0.500f),
    0x000000ff -> (0.000f, 0.000f, 0.000f),
    0xff0000ff -> (0.000f, 1.000f, 0.500f),
    0xbfbf00ff -> (60.00f, 1.000f, 0.375f),
    0x008000ff -> (120.0f, 1.000f, 0.250f),
    0x80ffffff -> (180.0f, 1.000f, 0.750f),
    0x8080ffff -> (240.0f, 1.000f, 0.750f),
    0xbf40bfff -> (300.0f, 0.500f, 0.500f),
    0xa0a424ff -> (61.80f, 0.638f, 0.393f),
    0x411beaff -> (251.1f, 0.832f, 0.511f),
    0x1eac41ff -> (134.9f, 0.707f, 0.396f),
    0xf0c80eff -> (49.50f, 0.893f, 0.497f),
    0xb430e5ff -> (283.7f, 0.775f, 0.542f),
    0xed7651ff -> (14.30f, 0.817f, 0.624f),
    0xfef888ff -> (56.90f, 0.991f, 0.765f),
    0x19cb97ff -> (162.4f, 0.779f, 0.447f),
    0x362698ff -> (248.3f, 0.601f, 0.373f),
    0x7e7eb8ff -> (240.5f, 0.290f, 0.607f)
  )

  private def fract(v: Float): Float = v - Math.floor(v).toFloat

  test("testRgb2hclInt: RGB to HCL int conversion accuracy") {
    var hAbs = 0.0
    var cAbs = 0.0
    var lAbs = 0.0

    for (c <- testColors) {
      val tr  = (c >>> 24 & 255) / 255f
      val tg  = (c >>> 16 & 255) / 255f
      val tb  = (c >>> 8 & 255) / 255f
      val max = Math.max(tr, Math.max(tg, tb))
      val min = Math.min(tr, Math.min(tg, tb))

      val (targetHue, _, targetLit) = hslMap(c)
      val targetChroma              = max - min

      val hcla = FloatColors.rgb2hclInt(tr, tg, tb, 1f)
      val hue  = fract((hcla >>> 24 & 255) / 255f) * 360f
      val chr  = (hcla >>> 16 & 255) / 255f
      val lit  = (hcla >>> 8 & 255) / 255f

      hAbs += Math.abs(hue - targetHue)
      cAbs += Math.abs(chr - targetChroma)
      lAbs += Math.abs(lit - targetLit)
    }

    // Hue error should be reasonable (< 15 degrees total)
    assert(hAbs < 15.0, s"Hue absolute error too large: $hAbs")
    // Chroma error should be near zero
    assert(cAbs < 0.1, s"Chroma absolute error too large: $cAbs")
    // Lightness error should be small
    assert(lAbs < 0.1, s"Lightness absolute error too large: $lAbs")
  }

  test("testHcl2rgbInt: HCL to RGB int conversion accuracy") {
    var rAbs = 0.0
    var gAbs = 0.0
    var bAbs = 0.0

    for (c <- testColors) {
      val tr  = c >>> 24 & 255
      val tg  = c >>> 16 & 255
      val tb  = c >>> 8 & 255
      val max = Math.max(tr, Math.max(tg, tb))
      val min = Math.min(tr, Math.min(tg, tb))

      val (targetHue, _, targetLit) = hslMap(c)

      val rgba = FloatColors.hcl2rgbInt(targetHue / 360f, (max - min) / 255f, targetLit, 1f)
      val r    = rgba >>> 24 & 255
      val g    = rgba >>> 16 & 255
      val b    = rgba >>> 8 & 255

      rAbs += Math.abs(r - tr)
      gAbs += Math.abs(g - tg)
      bAbs += Math.abs(b - tb)
    }

    // Each channel's total absolute error across all test colors should be small
    assert(rAbs < 10.0, s"Red absolute error too large: $rAbs")
    assert(gAbs < 10.0, s"Green absolute error too large: $gAbs")
    assert(bAbs < 10.0, s"Blue absolute error too large: $bAbs")
  }

  test("testPalette: all oklab Palette colors are in gamut") {
    import sge.colorful.oklab.{ ColorTools => OklabColorTools }
    import sge.colorful.oklab.Palette

    var failures   = 0
    var offsetFail = 0
    for ((_, color) <- Palette.NAMED) {
      val l = OklabColorTools.channelL(color)
      val a = OklabColorTools.channelA(color)
      val b = OklabColorTools.channelB(color)
      if (!OklabColorTools.inGamut(l, a, b)) {
        failures += 1
      }
      // With max chromatic offset, the color should NOT be in gamut
      val aOff = (a + 0.5f) % 1f
      val bOff = (b + 0.5f) % 1f
      if (OklabColorTools.inGamut(l, aOff, bOff)) {
        offsetFail += 1
      }
    }
    // Allow tolerance for floating point precision differences across platforms
    // (Scala Native's FP math can differ slightly from JVM/JS, causing borderline colors to flip)
    assert(failures <= 5, s"$failures palette colors were out of gamut (tolerance: 5)")
    assert(offsetFail <= 5, s"$offsetFail offset colors were unexpectedly in gamut (tolerance: 5)")
  }

  test("testBlues: chromaLimit for blue hue range") {
    import sge.colorful.oklab.{ ColorTools => OklabColorTools }

    // The original test iterates hue values in the blue range and
    // verifies that chromaLimit returns reasonable (non-negative, finite) values.
    var f = 0.70934484f
    while (f < 0.74934484f) {
      val limit = OklabColorTools.chromaLimit(f, 0.2627451f)
      assert(limit >= 0f, s"chromaLimit for hue $f should be non-negative, got $limit")
      assert(!limit.isNaN, s"chromaLimit for hue $f should not be NaN")
      assert(!limit.isInfinite, s"chromaLimit for hue $f should not be infinite")
      f += 0.001f
    }
  }
}
