package sge
package math

class Vector2Test extends munit.FunSuite {

  test("toString") {
    // Use exact float values (binary fractions, non-integer) to avoid JVM/JS formatting differences
    assertEquals(new Vector2(-5.5f, 42.5f).toString(), "(-5.5,42.5)")
  }

  test("fromString") {
    assertEquals(new Vector2().fromString("(-5,42.00055)"), new Vector2(-5f, 42.00055f))
  }

  test("angle") {
    assertEqualsDouble(new Vector2(0, -1f).angleDeg().toDouble, 270.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }

  test("angleRelative") {
    assertEqualsDouble(new Vector2(0, -1f).angleDeg(Vector2(1, 0)).toDouble, 270.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }

  test("angleStatic") {
    assertEqualsDouble(Vector2.angleDeg(0, -1f).toDouble, 270.0, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }

  test("angleRad") {
    assertEqualsDouble(new Vector2(0, -1f).angleRad().toDouble, (-MathUtils.HALF_PI).toDouble, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }

  test("angleRadRelative") {
    assertEqualsDouble(
      new Vector2(0, -1f).angleRad(Vector2(1, 0)).toDouble,
      (-MathUtils.HALF_PI).toDouble,
      MathUtils.FLOAT_ROUNDING_ERROR.toDouble
    )
  }

  test("angleRadStatic") {
    assertEqualsDouble(Vector2.angleRad(0, -1f).toDouble, (-MathUtils.HALF_PI).toDouble, MathUtils.FLOAT_ROUNDING_ERROR.toDouble)
  }
}
