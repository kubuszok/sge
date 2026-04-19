package sge
package math

class CatmullRomSplineTest extends munit.FunSuite {

  private val eps = 0.01f

  private def v2(x: Float, y: Float): Vector2 = new Vector2(x, y)

  private def assertV2Near(actual: Vector2, expectedX: Float, expectedY: Float, tolerance: Float = eps): Unit = {
    assertEqualsDouble(actual.x.toDouble, expectedX.toDouble, tolerance.toDouble)
    assertEqualsDouble(actual.y.toDouble, expectedY.toDouble, tolerance.toDouble)
  }

  test("valueAt endpoints non-continuous") {
    // With 4 control points and non-continuous, spanCount = 1
    val pts    = Array(v2(0, 0), v2(1, 1), v2(2, 0), v2(3, -1))
    val spline = new CatmullRomSpline[Vector2](pts, false)

    val out0 = v2(0, 0)
    spline.valueAt(out0, 0f)
    // At t=0, the spline evaluates at span 0, u=0 -> should be at control point 1 (index 1)
    assertV2Near(out0, 1f, 1f)

    val out1 = v2(0, 0)
    spline.valueAt(out1, 1f)
    // At t=1, the spline evaluates at span 0, u=1 -> should be at control point 2 (index 2)
    assertV2Near(out1, 2f, 0f)
  }

  test("valueAt midpoint non-continuous") {
    val pts    = Array(v2(0, 0), v2(1, 1), v2(2, 0), v2(3, -1))
    val spline = new CatmullRomSpline[Vector2](pts, false)

    val mid = v2(0, 0)
    spline.valueAt(mid, 0.5f)
    // Midpoint should be approximately between control points 1 and 2
    assert(mid.x > 1f && mid.x < 2f, s"x=${mid.x} should be between 1 and 2")
  }

  test("derivativeAt returns non-zero for non-linear spline") {
    val pts    = Array(v2(0, 0), v2(1, 2), v2(3, 1), v2(4, 3))
    val spline = new CatmullRomSpline[Vector2](pts, false)
    val deriv  = v2(0, 0)
    spline.derivativeAt(deriv, 0.5f)
    // Derivative should be non-zero for a non-trivial spline
    assert(deriv.length > 0f, s"derivative length should be > 0, got ${deriv.length}")
  }

  test("approxLength positive") {
    val pts    = Array(v2(0, 0), v2(1, 1), v2(2, 0), v2(3, -1))
    val spline = new CatmullRomSpline[Vector2](pts, false)
    val length = spline.approxLength(20)
    assert(length > 0f, s"approxLength should be positive, got $length")
  }

  test("spanCount non-continuous") {
    val pts    = Array(v2(0, 0), v2(1, 1), v2(2, 0), v2(3, -1))
    val spline = new CatmullRomSpline[Vector2](pts, false)
    assertEquals(spline.spanCount, 1) // length - 3 = 4 - 3 = 1
  }

  test("spanCount continuous") {
    val pts    = Array(v2(0, 0), v2(1, 1), v2(2, 0), v2(3, -1))
    val spline = new CatmullRomSpline[Vector2](pts, true)
    assertEquals(spline.spanCount, 4) // length = 4
  }
}
