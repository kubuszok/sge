package sge
package math

class BSplineTest extends munit.FunSuite {

  test("cubicSplineNonContinuous") {
    val controlPoints = Array(new Vector3(0, 0, 0), new Vector3(1, 1, 0), new Vector3(2, 0, 0), new Vector3(3, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, false)

    val result = new Vector3()
    spline.valueAt(result, 0.5f)

    val expected = new Vector3(1.5f, 0.5f, 0)
    assertEqualsDouble(result.x.toDouble, expected.x.toDouble, 0.1) // Error tolerance is large because the curves are... curvy.
    assertEqualsDouble(result.y.toDouble, expected.y.toDouble, 0.1)
    assertEqualsDouble(result.z.toDouble, expected.z.toDouble, 0.1)
  }

  test("cubicSplineContinuous") {
    // Define a rough circle based on the 4 cardinal directions.
    val controlPoints = Array(new Vector3(1, 0, 0), new Vector3(0, 1, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, true)

    val result = new Vector3()
    // 0.875f turns around the circle takes us to the southeast quadrant.
    spline.valueAt(result, 0.875f)

    // The BSpline does not travel through the control points.
    val expected = new Vector3(0.45f, -0.45f, 0)
    assertEqualsDouble(result.x.toDouble, expected.x.toDouble, 0.1)
    assertEqualsDouble(result.y.toDouble, expected.y.toDouble, 0.1)
    assertEqualsDouble(result.z.toDouble, expected.z.toDouble, 0.1)
  }

  test("cubicDerivative") {
    val controlPoints = Array(new Vector3(0, 0, 0), new Vector3(1, 1, 0), new Vector3(2, 0, 0), new Vector3(3, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, true)

    val derivative = new Vector3()
    spline.derivativeAt(derivative, 0.5f)

    val expectedDerivative = new Vector3(1, -1, 0)
    assertEqualsDouble(derivative.x.toDouble, expectedDerivative.x.toDouble, 0.001)
    assertEqualsDouble(derivative.y.toDouble, expectedDerivative.y.toDouble, 0.001)
    assertEqualsDouble(derivative.z.toDouble, expectedDerivative.z.toDouble, 0.001)
  }

  test("continuousApproximation") {
    val controlPoints = Array(new Vector3(1, 0, 0), new Vector3(0, 1, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, true)

    val point = new Vector3(0.45f, -0.45f, 0.0f)
    val t     = spline.approximate(point)

    // 0.875 turns corresponds to the southeast quadrant, where point is.
    assertEqualsDouble(t.toDouble, 0.875, 0.1)
  }

  test("nonContinuousApproximation") {
    val controlPoints = Array(new Vector3(1, 0, 0), new Vector3(0, 1, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, false)

    var point = new Vector3(0.0f, 0.666f, 0.0f)
    var t     = spline.approximate(point)
    assertEqualsDouble(t.toDouble, 0.0, 0.1)

    point = new Vector3(-0.666f, 0.0f, 0.0f)
    t = spline.approximate(point)
    assertEqualsDouble(t.toDouble, 1.0, 0.1)

    point = new Vector3(-0.45f, 0.45f, 0.0f)
    t = spline.approximate(point)
    assertEqualsDouble(t.toDouble, 0.5, 0.1)
  }

  test("splineContinuity") {
    val controlPoints = Array(new Vector3(0, 0, 0), new Vector3(1, 1, 0), new Vector3(2, 0, 0), new Vector3(3, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, true)

    val start = new Vector3()
    val end   = new Vector3()
    spline.valueAt(start, 0.0f)
    spline.valueAt(end, 1.0f)

    // For a continuous spline, the start and end points should be equal
    assertEqualsDouble(start.x.toDouble, end.x.toDouble, 0.001)
    assertEqualsDouble(start.y.toDouble, end.y.toDouble, 0.001)
    assertEqualsDouble(start.z.toDouble, end.z.toDouble, 0.001)
  }

  /** Test to validate calculation with edge cases (t = 0 and t = 1). */
  test("edgeCases") {
    // The first and last control points aren't on the path.
    val controlPoints = Array(new Vector3(0, 0, 0), new Vector3(1, 1, 0), new Vector3(2, 0, 0), new Vector3(3, -1, 0))
    val spline        = new BSpline[Vector3](controlPoints, 3, false)

    val start         = new Vector3()
    val expectedStart = new Vector3(1f, 0.666f, 0f)
    val end           = new Vector3()
    val expectedEnd   = new Vector3(2f, 0f, 0f)
    spline.valueAt(start, 0.0f)

    assertEqualsDouble(start.x.toDouble, expectedStart.x.toDouble, 0.001)
    assertEqualsDouble(start.y.toDouble, expectedStart.y.toDouble, 0.001)
    assertEqualsDouble(start.z.toDouble, expectedStart.z.toDouble, 0.001)

    spline.valueAt(end, 1.0f)

    assertEqualsDouble(end.x.toDouble, expectedEnd.x.toDouble, 0.001)
    assertEqualsDouble(end.y.toDouble, expectedEnd.y.toDouble, 0.001)
    assertEqualsDouble(end.z.toDouble, expectedEnd.z.toDouble, 0.001)
  }
}
