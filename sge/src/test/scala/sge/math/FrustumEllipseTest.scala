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
