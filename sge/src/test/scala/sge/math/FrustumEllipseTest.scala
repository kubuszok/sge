/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package math

import sge.math.collision.BoundingBox

class FrustumTest extends munit.FunSuite {

  /** Creates an orthographic frustum centered at origin with the given half-extents. */
  private def orthoFrustum(hw: Float, hh: Float, near: Float, far: Float): Frustum = {
    val f    = Frustum()
    val proj = Matrix4()
    proj.setToOrtho(-hw, hw, -hh, hh, near, far)
    val invProj = Matrix4().set(proj).inv()
    f.update(invProj)
    f
  }

  test("construction initialises six planes and eight plane points") {
    val f = Frustum()
    assertEquals(f.planes.length, 6)
    assertEquals(f.planePoints.length, 8)
  }

  test("update with orthographic projection sets planePoints correctly") {
    val f = orthoFrustum(10f, 10f, 1f, 100f)
    // Near-plane bottom-left should be (-10, -10, -1) in an ortho projection
    assertEqualsFloat(f.planePoints(0).x, -10f, 0.001f)
    assertEqualsFloat(f.planePoints(0).y, -10f, 0.001f)
    assertEqualsFloat(f.planePoints(0).z, -1f, 0.001f)
  }

  test("pointInFrustum") {
    val f = orthoFrustum(10f, 10f, 1f, 100f)
    assert(f.pointInFrustum(Vector3(0f, 0f, -50f)))
    assert(f.pointInFrustum(0f, 0f, -50f))
    assert(!f.pointInFrustum(Vector3(20f, 0f, -50f)))
    assert(!f.pointInFrustum(Vector3(0f, 0f, 10f)))
  }

  test("sphereInFrustum") {
    val f = orthoFrustum(10f, 10f, 1f, 100f)
    // Sphere centered inside
    assert(f.sphereInFrustum(Vector3(0f, 0f, -50f), 5f))
    // Sphere centered outside but radius reaches inside
    assert(f.sphereInFrustum(Vector3(12f, 0f, -50f), 5f))
    // Sphere completely outside
    assert(!f.sphereInFrustum(Vector3(20f, 0f, -50f), 2f))
  }

  test("boundsInFrustum with BoundingBox") {
    val f   = orthoFrustum(10f, 10f, 1f, 100f)
    val box = BoundingBox(Vector3(-5f, -5f, -60f), Vector3(5f, 5f, -40f))
    assert(f.boundsInFrustum(box))
    val outside = BoundingBox(Vector3(20f, 20f, -60f), Vector3(30f, 30f, -40f))
    assert(!f.boundsInFrustum(outside))
  }

  test("boundsInFrustum with center and half-extents") {
    val f = orthoFrustum(10f, 10f, 1f, 100f)
    assert(f.boundsInFrustum(0f, 0f, -50f, 5f, 5f, 5f))
    assert(!f.boundsInFrustum(30f, 30f, -50f, 2f, 2f, 2f))
  }
}

class EllipseTest extends munit.FunSuite {
  private val delta = 0.001f

  test("construction and field values") {
    val e = Ellipse(1f, 2f, 6f, 4f)
    assertEqualsFloat(e.x, 1f, delta)
    assertEqualsFloat(e.y, 2f, delta)
    assertEqualsFloat(e.width, 6f, delta)
    assertEqualsFloat(e.height, 4f, delta)
  }

  test("copy constructor") {
    val e1 = Ellipse(3f, 4f, 10f, 8f)
    val e2 = Ellipse(e1)
    assertEquals(e1, e2)
  }

  test("contains point") {
    val e = Ellipse(0f, 0f, 10f, 6f)
    // Center
    assert(e.contains(0f, 0f))
    // On semi-major axis edge (width/2 = 5)
    assert(e.contains(4.9f, 0f))
    // Outside
    assert(!e.contains(6f, 0f))
    assert(!e.contains(0f, 4f))
    // Vector2 overload
    assert(e.contains(Vector2(0f, 0f)))
  }

  test("area") {
    val e = Ellipse(0f, 0f, 4f, 6f)
    // area = PI * width * height / 4 = PI * 4 * 6 / 4 = 6*PI
    val expected = MathUtils.PI * 6f
    assertEqualsFloat(e.area(), expected, delta)
  }

  test("circumference approximation") {
    // For a circle (width == height), circumference should be 2*PI*r
    val e        = Ellipse(0f, 0f, 10f, 10f)
    val expected = MathUtils.PI2 * 5f
    assertEqualsFloat(e.circumference(), expected, 0.1f)
  }

  test("set methods") {
    val e = Ellipse()
    e.set(1f, 2f, 3f, 4f)
    assertEqualsFloat(e.x, 1f, delta)
    assertEqualsFloat(e.width, 3f, delta)

    e.setPosition(10f, 20f)
    assertEqualsFloat(e.x, 10f, delta)
    assertEqualsFloat(e.y, 20f, delta)
    // width/height unchanged
    assertEqualsFloat(e.width, 3f, delta)

    e.setSize(50f, 60f)
    assertEqualsFloat(e.width, 50f, delta)
    assertEqualsFloat(e.height, 60f, delta)
  }

  test("equals and hashCode") {
    val e1 = Ellipse(1f, 2f, 3f, 4f)
    val e2 = Ellipse(1f, 2f, 3f, 4f)
    val e3 = Ellipse(1f, 2f, 3f, 5f)
    assertEquals(e1, e2)
    assertEquals(e1.hashCode(), e2.hashCode())
    assertNotEquals(e1, e3)
    assert(!e1.equals("not an ellipse"))
  }
}
