package sge
package math

class MathUtilsTest extends munit.FunSuite {

  // Math.copySign is not available in Scala.js javalib, so we inline it here.
  private def copySign(magnitude: Float, sign: Float): Float =
    if (sign < 0f) -Math.abs(magnitude) else Math.abs(magnitude)

  test("lerpAngle") {
    assertEqualsDouble(MathUtils.lerpAngle(MathUtils.PI / 18f, MathUtils.PI / 6f, 0.0f).toDouble, (MathUtils.PI / 18f).toDouble, 0.01)
    assertEqualsDouble(MathUtils.lerpAngle(MathUtils.PI / 18f, MathUtils.PI / 6f, 0.5f).toDouble, (MathUtils.PI / 9f).toDouble, 0.01)
    assertEqualsDouble(MathUtils.lerpAngle(MathUtils.PI / 18f, MathUtils.PI / 6f, 1.0f).toDouble, (MathUtils.PI / 6f).toDouble, 0.01)

    // checks both negative c, which should produce a result close to HALF_PI, and
    // positive c, which should be close to PI + HALF_PI.
    // intentionally skips where c == 0, because there are two equally-valid results for that case.
    var c = -1f
    while (c <= 1f) {
      val expected = MathUtils.PI + copySign(MathUtils.HALF_PI, c) + c
      assertEqualsDouble(
        MathUtils.lerpAngle(0, MathUtils.PI2 + MathUtils.PI + c + c, 0.5f).toDouble,
        expected.toDouble,
        0.01
      )
      assertEqualsDouble(
        MathUtils.lerpAngle(MathUtils.PI2 + MathUtils.PI + c + c, 0, 0.5f).toDouble,
        expected.toDouble,
        0.01
      )
      c += 0.003f
    }
  }

  test("lerpAngleDeg") {
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 30, 0.0f).toDouble, 10.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 30, 0.5f).toDouble, 20.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 30, 1.0f).toDouble, 30.0, 0.01)

    // checks both negative c, which should produce a result close to 90, and
    // positive c, which should be close to 270.
    // intentionally skips where c == 0, because there are two equally-valid results for that case.
    var c = -80f
    while (c <= 80f) {
      val expected = 180f + copySign(90f, c) + c
      assertEqualsDouble(
        MathUtils.lerpAngleDeg(0, 540 + c + c, 0.5f).toDouble,
        expected.toDouble,
        0.01
      )
      assertEqualsDouble(
        MathUtils.lerpAngleDeg(540 + c + c, 0, 0.5f).toDouble,
        expected.toDouble,
        0.01
      )
      c += 0.3f
    }
  }

  test("lerpAngleDegCrossingZero") {
    assertEqualsDouble(MathUtils.lerpAngleDeg(350, 10, 0.0f).toDouble, 350.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(350, 10, 0.5f).toDouble, 0.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(350, 10, 1.0f).toDouble, 10.0, 0.01)
  }

  test("lerpAngleDegCrossingZeroBackwards") {
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 350, 0.0f).toDouble, 10.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 350, 0.5f).toDouble, 0.0, 0.01)
    assertEqualsDouble(MathUtils.lerpAngleDeg(10, 350, 1.0f).toDouble, 350.0, 0.01)
  }

  test("norm") {
    assertEqualsDouble(MathUtils.norm(10f, 20f, 0f).toDouble, -1.0, 0.01)
    assertEqualsDouble(MathUtils.norm(10f, 20f, 10f).toDouble, 0.0, 0.01)
    assertEqualsDouble(MathUtils.norm(10f, 20f, 15f).toDouble, 0.5, 0.01)
    assertEqualsDouble(MathUtils.norm(10f, 20f, 20f).toDouble, 1.0, 0.01)
    assertEqualsDouble(MathUtils.norm(10f, 20f, 30f).toDouble, 2.0, 0.01)
  }

  test("map") {
    assertEqualsDouble(MathUtils.map(10f, 20f, 100f, 200f, 0f).toDouble, 0.0, 0.01)
    assertEqualsDouble(MathUtils.map(10f, 20f, 100f, 200f, 10f).toDouble, 100.0, 0.01)
    assertEqualsDouble(MathUtils.map(10f, 20f, 100f, 200f, 15f).toDouble, 150.0, 0.01)
    assertEqualsDouble(MathUtils.map(10f, 20f, 100f, 200f, 20f).toDouble, 200.0, 0.01)
    assertEqualsDouble(MathUtils.map(10f, 20f, 100f, 200f, 30f).toDouble, 300.0, 0.01)
  }

  test("randomLong") {
    var r: Long = 0L
    for (_ <- 0 until 512) {
      r = MathUtils.random(1L, 5L); assert(r >= 1L && r <= 5L)
      r = MathUtils.random(6L, 1L); assert(r >= 1L && r <= 6L)
      r = MathUtils.random(-1L, -7L); assert(r <= -1L && r >= -7L)
      r = MathUtils.random(-8L, -1L); assert(r <= -1L && r >= -8L)
    }
  }

  test("sinDeg") {
    assertEqualsDouble(MathUtils.sinDeg(0f).toDouble, 0.0, 0.0)
    assertEqualsDouble(MathUtils.sinDeg(90f).toDouble, 1.0, 0.0)
    assertEqualsDouble(MathUtils.sinDeg(180f).toDouble, 0.0, 0.0)
    assertEqualsDouble(MathUtils.sinDeg(270f).toDouble, -1.0, 0.0)
  }

  test("cosDeg") {
    assertEqualsDouble(MathUtils.cosDeg(0f).toDouble, 1.0, 0.1)
    assertEqualsDouble(MathUtils.cosDeg(90f).toDouble, 0.0, 0.1)
    assertEqualsDouble(MathUtils.cosDeg(180f).toDouble, -1.0, 0.1)
    assertEqualsDouble(MathUtils.cosDeg(270f).toDouble, 0.0, 0.1)
  }

  test("tanDeg") {
    assertEqualsDouble(MathUtils.tanDeg(0f).toDouble, 0.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.tanDeg(45f).toDouble, Math.tan(Math.toRadians(45.0)), MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    // assertEquals(Float.POSITIVE_INFINITY, MathUtils.tanDeg(90f), 0f); // near infinite, maximum error here
    assertEqualsDouble(MathUtils.tanDeg(135f).toDouble, Math.tan(Math.toRadians(135.0)), MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.tanDeg(180f).toDouble, 0.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }

  test("atan2Deg360") {
    assertEqualsDouble(MathUtils.atan2Deg360(0f, 1f).toDouble, 0.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(1f, 1f).toDouble, 45.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(1f, 0f).toDouble, 90.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(1f, -1f).toDouble, 135.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(0f, -1f).toDouble, 180.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(-1f, -1f).toDouble, 225.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(-1f, 0f).toDouble, 270.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
    assertEqualsDouble(MathUtils.atan2Deg360(-1f, 1f).toDouble, 315.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }
}
