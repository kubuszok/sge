/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics

import sge.math.{ Matrix4, Vector3 }
import sge.noop.NoopGraphics

class PerspectiveCameraTest extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float): Unit =
    assert(Math.abs(actual - expected) < delta, s"expected $expected but got $actual (delta=$delta)")

  private def makeContext(w: Int, h: Int): Sge = {
    val graphics = new NoopGraphics(w, h) {
      override def gl20: GL20 = NoopGL20
    }
    SgeTestFixture.testSge(graphics = graphics)
  }

  test("default state: fieldOfView=67, near=1, far=100") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera()
    assertEqualsFloat(cam.fieldOfView, 67f, epsilon)
    assertEqualsFloat(cam.near, 1f, epsilon)
    assertEqualsFloat(cam.far, 100f, epsilon)
  }

  test("default direction and up vectors") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera()
    assertEqualsFloat(cam.direction.x, 0f, epsilon)
    assertEqualsFloat(cam.direction.y, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, -1f, epsilon)
    assertEqualsFloat(cam.up.x, 0f, epsilon)
    assertEqualsFloat(cam.up.y, 1f, epsilon)
    assertEqualsFloat(cam.up.z, 0f, epsilon)
  }

  test("secondary constructor sets fov, viewport, and calls update") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera(90f, WorldUnits(800f), WorldUnits(600f))
    assertEqualsFloat(cam.fieldOfView, 90f, epsilon)
    assertEqualsFloat(cam.viewportWidth.toFloat, 800f, epsilon)
    assertEqualsFloat(cam.viewportHeight.toFloat, 600f, epsilon)
    // combined matrix should not be identity
    val identity     = Matrix4()
    val combinedVals = cam.combined.values
    val identityVals = identity.values
    val differs      = (0 until 16).exists(i => Math.abs(combinedVals(i) - identityVals(i)) > epsilon)
    assert(differs, "combined matrix should differ from identity after construction with update")
  }

  test("update produces valid combined matrix") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera()
    cam.viewportWidth = WorldUnits(800f)
    cam.viewportHeight = WorldUnits(600f)
    cam.update()
    val identity     = Matrix4()
    val combinedVals = cam.combined.values
    val identityVals = identity.values
    val differs      = (0 until 16).exists(i => Math.abs(combinedVals(i) - identityVals(i)) > epsilon)
    assert(differs, "combined matrix should differ from identity after update")
  }

  test("position changes affect view matrix") {
    given Sge      = makeContext(800, 600)
    val cam        = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    val viewBefore = cam.view.values.clone()

    cam.translate(5f, 10f, 3f)
    cam.update()
    val viewAfter = cam.view.values

    val differs = (0 until 16).exists(i => Math.abs(viewBefore(i) - viewAfter(i)) > epsilon)
    assert(differs, "view matrix should change after translating camera")
  }

  test("lookAt changes direction") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    cam.position.set(0, 0, 0)
    cam.lookAt(1f, 0f, 0f)
    assertEqualsFloat(cam.direction.x, 1f, epsilon)
    assertEqualsFloat(cam.direction.y, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, 0f, epsilon)
  }

  test("project/unproject roundtrip approximately recovers original") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    cam.position.set(0, 0, 5)
    cam.lookAt(0f, 0f, 0f)
    cam.update()

    // Project the origin, then unproject it
    val worldPoint = Vector3(0f, 0f, 0f)
    val projected  = cam.project(Vector3(0f, 0f, 0f))
    // project outputs bottom-left screen coords; unproject expects top-left touch coords
    projected.y = 600f - projected.y
    val unprojected = cam.unproject(projected)

    assertEqualsFloat(unprojected.x, worldPoint.x, 0.5f)
    assertEqualsFloat(unprojected.y, worldPoint.y, 0.5f)
    assertEqualsFloat(unprojected.z, worldPoint.z, 0.5f)
  }

  test("frustum planes are set after update") {
    given Sge      = makeContext(800, 600)
    val cam        = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    val hasNonZero = cam.frustum.planes.exists { p =>
      Math.abs(p.normal.x) > epsilon || Math.abs(p.normal.y) > epsilon ||
      Math.abs(p.normal.z) > epsilon || Math.abs(p.d) > epsilon
    }
    assert(hasNonZero, "frustum planes should be set after update")
  }

  test("fieldOfView affects projection matrix") {
    given Sge = makeContext(800, 600)
    val cam1  = PerspectiveCamera(60f, WorldUnits(800f), WorldUnits(600f))
    val proj1 = cam1.projection.values.clone()

    val cam2  = PerspectiveCamera(90f, WorldUnits(800f), WorldUnits(600f))
    val proj2 = cam2.projection.values

    val differs = (0 until 16).exists(i => Math.abs(proj1(i) - proj2(i)) > epsilon)
    assert(differs, "projection with fov=90 should differ from fov=60")
  }

  test("near/far affect projection matrix") {
    given Sge = makeContext(800, 600)
    val cam1  = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    val proj1 = cam1.projection.values.clone()

    val cam2 = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    cam2.near = 0.5f
    cam2.far = 500f
    cam2.update()
    val proj2 = cam2.projection.values

    val differs = (0 until 16).exists(i => Math.abs(proj1(i) - proj2(i)) > epsilon)
    assert(differs, "projection with different near/far should differ")
  }

  test("rotate changes direction and up") {
    given Sge     = makeContext(800, 600)
    val cam       = PerspectiveCamera(67f, WorldUnits(800f), WorldUnits(600f))
    val dirBefore = Vector3().set(cam.direction)
    cam.rotate(cam.up, 90f)
    // direction should change
    val dirChanged = Math.abs(cam.direction.x - dirBefore.x) > epsilon ||
      Math.abs(cam.direction.y - dirBefore.y) > epsilon ||
      Math.abs(cam.direction.z - dirBefore.z) > epsilon
    assert(dirChanged, "direction should change after rotation")
  }

  test("translate 3D adds to position") {
    given Sge = makeContext(800, 600)
    val cam   = PerspectiveCamera()
    cam.position.set(0, 0, 0)
    cam.translate(1f, 2f, 3f)
    assertEqualsFloat(cam.position.x, 1f, epsilon)
    assertEqualsFloat(cam.position.y, 2f, epsilon)
    assertEqualsFloat(cam.position.z, 3f, epsilon)
  }
}
