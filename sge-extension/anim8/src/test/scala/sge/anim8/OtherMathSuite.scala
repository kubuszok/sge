package sge
package anim8

class OtherMathSuite extends munit.FunSuite {

  test("barronSpline endpoints: 0 maps to 0, 1 maps to 1") {
    assertEqualsFloat(OtherMath.barronSpline(0f, 0.5f, 0.5f), 0f, 0.001f)
    assertEqualsFloat(OtherMath.barronSpline(1f, 0.5f, 0.5f), 1f, 0.001f)
  }

  test("barronSpline midpoint with shape=0.5 turning=0.5") {
    val mid = OtherMath.barronSpline(0.5f, 0.5f, 0.5f)
    // With shape=turning=0.5, midpoint should be 0.5
    assertEqualsFloat(mid, 0.5f, 0.01f)
  }

  test("atan2 approximation close to Math.atan2") {
    val testCases = Seq(
      (0f, 1f),
      (1f, 0f),
      (1f, 1f),
      (-1f, 1f),
      (0f, -1f),
      (-1f, -1f)
    )
    testCases.foreach { (y, x) =>
      val approx = OtherMath.atan2(y, x)
      val exact  = Math.atan2(y.toDouble, x.toDouble).toFloat
      assertEqualsFloat(approx, exact, 0.001f)
    }
  }

  test("probit: 0.5 maps to ~0 (center of distribution)") {
    val result = OtherMath.probit(0.5)
    assert(Math.abs(result) < 0.01, s"probit(0.5) should be ~0, was $result")
  }

  test("probit: symmetric around 0.5") {
    val low  = OtherMath.probit(0.1)
    val high = OtherMath.probit(0.9)
    assert(Math.abs(low + high) < 0.01, s"probit(0.1)=$low and probit(0.9)=$high should be symmetric")
  }

  test("probitF: 0.5 maps to ~0") {
    val result = OtherMath.probitF(0.5f)
    assert(Math.abs(result) < 0.01f, s"probitF(0.5) should be ~0, was $result")
  }

  test("cbrt approximates cube root") {
    assertEqualsFloat(OtherMath.cbrt(1f), 1f, 0.001f)
    assertEqualsFloat(OtherMath.cbrt(8f), 2f, 0.01f)
    assertEqualsFloat(OtherMath.cbrt(27f), 3f, 0.02f)
    assertEqualsFloat(OtherMath.cbrt(-8f), -2f, 0.01f)
  }

  test("centralize pushes values toward center") {
    // 127/128 should stay near center
    val mid = OtherMath.centralize(128.toByte) & 0xff
    // Center value should be near 128
    assert(Math.abs(mid - 128) < 30, s"centralize(128) should be near 128, was $mid")
    // Extreme values should remain near extremes
    val high = OtherMath.centralize(255.toByte) & 0xff
    assert(high > 200, s"centralize(255) should stay high, was $high")
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(using munit.Location): Unit =
    assert(Math.abs(actual - expected) <= delta, s"expected $expected +/- $delta but got $actual")
}
