package sge
package math

class PlaneTest extends munit.FunSuite {

  private val eps = 0.0001

  test("construct from normal and distance") {
    val p = new Plane(new Vector3(0, 1, 0), 5f)
    assertEqualsDouble(p.normal.y.toDouble, 1.0, eps)
    assertEqualsDouble(p.d.toDouble, 5.0, eps)
  }

  test("construct from normal and point") {
    // Plane with normal (0,1,0) passing through (0,3,0) => d = -dot(normal, point) = -3
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 3, 0))
    assertEqualsDouble(p.normal.y.toDouble, 1.0, eps)
    assertEqualsDouble(p.d.toDouble, -3.0, eps)
  }

  test("construct from three points") {
    // Three points on the XZ plane: (0,0,0), (1,0,0), (0,0,1) -> normal should be (0,1,0) or (0,-1,0)
    val p = new Plane(new Vector3(0, 0, 0), new Vector3(1, 0, 0), new Vector3(0, 0, 1))
    // Normal should be along Y axis
    assertEqualsDouble(Math.abs(p.normal.y).toDouble, 1.0, eps)
    assertEqualsDouble(p.d.toDouble, 0.0, eps)
  }

  test("distance from point to plane") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    // Point (0, 5, 0) is 5 units above the XZ plane
    assertEqualsDouble(p.distance(new Vector3(0, 5, 0)).toDouble, 5.0, eps)
    // Point (0, -3, 0) is 3 units below
    assertEqualsDouble(p.distance(new Vector3(0, -3, 0)).toDouble, -3.0, eps)
  }

  test("testPoint Front") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    assertEquals(p.testPoint(new Vector3(0, 5, 0)), Plane.PlaneSide.Front)
  }

  test("testPoint Back") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    assertEquals(p.testPoint(new Vector3(0, -5, 0)), Plane.PlaneSide.Back)
  }

  test("testPoint OnPlane") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    assertEquals(p.testPoint(new Vector3(5, 0, 3)), Plane.PlaneSide.OnPlane)
  }

  test("testPoint with xyz overload") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    assertEquals(p.testPoint(0, 5, 0), Plane.PlaneSide.Front)
    assertEquals(p.testPoint(0, -5, 0), Plane.PlaneSide.Back)
  }

  test("isFrontFacing") {
    val p = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    // Camera looking down (0, -1, 0) should see the front of a floor plane
    assert(p.isFrontFacing(new Vector3(0, -1, 0)))
    // Camera looking up should not see the front
    assert(!p.isFrontFacing(new Vector3(0, 1, 0)))
  }

  test("set from point and normal") {
    val p = new Plane()
    p.set(new Vector3(0, 2, 0), new Vector3(0, 1, 0))
    assertEqualsDouble(p.normal.y.toDouble, 1.0, eps)
    assertEqualsDouble(p.d.toDouble, -2.0, eps)
  }

  test("set from another plane") {
    val p1 = new Plane(new Vector3(0, 1, 0), 5f)
    val p2 = new Plane()
    p2.set(p1)
    assertEqualsDouble(p2.normal.y.toDouble, p1.normal.y.toDouble, eps)
    assertEqualsDouble(p2.d.toDouble, p1.d.toDouble, eps)
  }

  test("set nx ny nz d") {
    val p = new Plane()
    p.set(0f, 1f, 0f, -3f)
    assertEqualsDouble(p.normal.y.toDouble, 1.0, eps)
    assertEqualsDouble(p.d.toDouble, -3.0, eps)
  }
}
