/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package math

import sge.math.collision.Ray

class GeometryTest extends munit.FunSuite {

  val Delta: Double = 0.001d

  // --- Circle ---

  test("Circle: construction sets fields") {
    val c = Circle(3f, 4f, 5f)
    assertEqualsDouble(c.x.toDouble, 3.0, Delta)
    assertEqualsDouble(c.y.toDouble, 4.0, Delta)
    assertEqualsDouble(c.radius.toDouble, 5.0, Delta)
  }

  test("Circle: contains point inside") {
    val c = Circle(0f, 0f, 10f)
    assert(c.contains(3f, 4f))
    assert(c.contains(0f, 10f)) // on edge
    assert(!c.contains(10f, 10f))
  }

  test("Circle: overlaps another circle") {
    val c1 = Circle(0f, 0f, 5f)
    val c2 = Circle(8f, 0f, 5f)
    assert(c1.overlaps(c2))

    val c3 = Circle(20f, 0f, 5f)
    assert(!c1.overlaps(c3))
  }

  test("Circle: area and circumference") {
    val c = Circle(0f, 0f, 2f)
    assertEqualsDouble(c.area().toDouble, (MathUtils.PI * 4f).toDouble, Delta)
    assertEqualsDouble(c.circumference().toDouble, (MathUtils.PI2 * 2f).toDouble, Delta)
  }

  test("Circle: set from center and edge") {
    val c = Circle()
    c.set(Vector2(0f, 0f), Vector2(3f, 4f))
    assertEqualsDouble(c.radius.toDouble, 5.0, Delta)
  }

  // --- Plane ---

  test("Plane: construction from normal and distance") {
    val p = Plane(Vector3(0f, 1f, 0f), 5f)
    assertEqualsDouble(p.normal.y.toDouble, 1.0, Delta)
    assertEqualsDouble(p.d.toDouble, 5.0, Delta)
  }

  test("Plane: construction from 3 points") {
    val p = Plane(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(0f, 0f, 1f))
    // Three points on the XZ plane, normal should be (0, -1, 0) or (0, 1, 0)
    assertEqualsDouble(Math.abs(p.normal.y), 1.0, Delta)
    assertEqualsDouble(p.normal.x.toDouble, 0.0, Delta)
    assertEqualsDouble(p.normal.z.toDouble, 0.0, Delta)
  }

  test("Plane: distance to point") {
    val p = Plane(Vector3(0f, 1f, 0f), 0f)
    // Point above the plane
    assertEqualsDouble(p.distance(Vector3(0f, 5f, 0f)).toDouble, 5.0, Delta)
    // Point below the plane
    assertEqualsDouble(p.distance(Vector3(0f, -3f, 0f)).toDouble, -3.0, Delta)
  }

  test("Plane: testPoint returns correct side") {
    val p = Plane(Vector3(0f, 1f, 0f), 0f)
    assertEquals(p.testPoint(Vector3(0f, 5f, 0f)), Plane.PlaneSide.Front)
    assertEquals(p.testPoint(Vector3(0f, -5f, 0f)), Plane.PlaneSide.Back)
    assertEquals(p.testPoint(Vector3(0f, 0f, 0f)), Plane.PlaneSide.OnPlane)
  }

  test("Plane: set from plane copies values") {
    val p1 = Plane(Vector3(0f, 0f, 1f), 7f)
    val p2 = Plane()
    p2.set(p1)
    assertEqualsDouble(p2.normal.z.toDouble, p1.normal.z.toDouble, Delta)
    assertEqualsDouble(p2.d.toDouble, 7.0, Delta)
  }

  // --- Ray ---

  test("Ray: getEndPoint at distance") {
    val r   = Ray(Vector3(1f, 2f, 3f), Vector3(1f, 0f, 0f))
    val out = Vector3()
    r.getEndPoint(out, 5f)
    assertEqualsDouble(out.x.toDouble, 6.0, Delta)
    assertEqualsDouble(out.y.toDouble, 2.0, Delta)
    assertEqualsDouble(out.z.toDouble, 3.0, Delta)
  }

  test("Ray: set normalizes direction") {
    val r = Ray()
    r.set(0f, 0f, 0f, 3f, 0f, 0f)
    assertEqualsDouble(r.direction.length.toDouble, 1.0, Delta)
  }

  test("Ray: copy produces independent ray") {
    val r   = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 1f, 0f))
    val cpy = r.cpy()
    assertEquals(r, cpy)
    cpy.origin.x = 99f
    assert(r.origin.x != cpy.origin.x)
  }

  test("Ray: equals and hashCode") {
    val r1 = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 0f, 1f))
    val r2 = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 0f, 1f))
    assertEquals(r1, r2)
    assertEquals(r1.hashCode(), r2.hashCode())
  }

  test("Ray: toString format") {
    val r = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 1f, 0f))
    assert(r.toString.startsWith("ray ["))
  }

  // --- Rectangle (additional tests beyond RectangleTest) ---

  test("Rectangle: overlaps") {
    val r1 = Rectangle(0f, 0f, 10f, 10f)
    val r2 = Rectangle(5f, 5f, 10f, 10f)
    assert(r1.overlaps(r2))

    val r3 = Rectangle(20f, 20f, 5f, 5f)
    assert(!r1.overlaps(r3))
  }

  test("Rectangle: contains point") {
    val r = Rectangle(0f, 0f, 10f, 10f)
    assert(r.contains(5f, 5f))
    assert(r.contains(0f, 0f)) // corner
    assert(r.contains(10f, 10f)) // opposite corner
    assert(!r.contains(11f, 5f))
  }

  test("Rectangle: merge expands to cover both") {
    val r1 = Rectangle(0f, 0f, 5f, 5f)
    val r2 = Rectangle(3f, 3f, 5f, 5f)
    r1.merge(r2)
    assertEqualsDouble(r1.x.toDouble, 0.0, Delta)
    assertEqualsDouble(r1.y.toDouble, 0.0, Delta)
    assertEqualsDouble(r1.width.toDouble, 8.0, Delta)
    assertEqualsDouble(r1.height.toDouble, 8.0, Delta)
  }

  test("Rectangle: area and perimeter") {
    val r = Rectangle(0f, 0f, 4f, 3f)
    assertEqualsDouble(r.area().toDouble, 12.0, Delta)
    assertEqualsDouble(r.perimeter().toDouble, 14.0, Delta)
  }

  test("Rectangle: getCenter") {
    val r = Rectangle(2f, 4f, 10f, 6f)
    val c = Vector2()
    r.getCenter(c)
    assertEqualsDouble(c.x.toDouble, 7.0, Delta)
    assertEqualsDouble(c.y.toDouble, 7.0, Delta)
  }
}
