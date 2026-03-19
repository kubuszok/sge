/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics

import sge.math.{ Matrix4, Vector3 }
import sge.noop.NoopGraphics

class OrthographicCameraTest extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float): Unit =
    assert(Math.abs(actual - expected) < delta, s"expected $expected but got $actual (delta=$delta)")

  private def makeContext(w: Int, h: Int): Sge = {
    val graphics = new NoopGraphics(w, h) {
      override def gl20: GL20 = NoopGL20
    }
    SgeTestFixture.testSge(graphics = graphics)
  }

  test("default state: zoom=1, near=0, far=100, direction=(0,0,-1), up=(0,1,0)") {
    given Sge = makeContext(640, 480)
    val cam   = OrthographicCamera()
    assertEqualsFloat(cam.zoom, 1f, epsilon)
    assertEqualsFloat(cam.near, 0f, epsilon)
    assertEqualsFloat(cam.far, 100f, epsilon)
    assertEqualsFloat(cam.direction.x, 0f, epsilon)
    assertEqualsFloat(cam.direction.y, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, -1f, epsilon)
    assertEqualsFloat(cam.up.x, 0f, epsilon)
    assertEqualsFloat(cam.up.y, 1f, epsilon)
    assertEqualsFloat(cam.up.z, 0f, epsilon)
  }

  test("secondary constructor sets viewportWidth/Height and calls update") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera(WorldUnits(800f), WorldUnits(600f))
    assertEqualsFloat(cam.viewportWidth.toFloat, 800f, epsilon)
    assertEqualsFloat(cam.viewportHeight.toFloat, 600f, epsilon)
    // combined should not be identity after update
    val identity = Matrix4()
    val differs  = (0 until 16).exists(i => Math.abs(cam.combined.values(i) - identity.values(i)) > epsilon)
    assert(differs, "combined matrix should differ from identity after secondary constructor")
  }

  test("update produces valid combined matrix (not identity)") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.viewportWidth = WorldUnits(800f)
    cam.viewportHeight = WorldUnits(600f)
    cam.update()
    val identity = Matrix4()
    // At least one element should differ from identity
    val combinedVals = cam.combined.values
    val identityVals = identity.values
    val differs      = (0 until 16).exists(i => Math.abs(combinedVals(i) - identityVals(i)) > epsilon)
    assert(differs, "combined matrix should differ from identity after update")
  }

  test("setToOrtho yDown=false centers camera and sets up=(0,1,0)") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.setToOrtho(false, WorldUnits(800f), WorldUnits(600f))
    assertEqualsFloat(cam.position.x, 400f, epsilon)
    assertEqualsFloat(cam.position.y, 300f, epsilon)
    assertEqualsFloat(cam.up.x, 0f, epsilon)
    assertEqualsFloat(cam.up.y, 1f, epsilon)
    assertEqualsFloat(cam.up.z, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, -1f, epsilon)
  }

  test("setToOrtho yDown=true sets up=(0,-1,0) and direction=(0,0,1)") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.setToOrtho(true, WorldUnits(800f), WorldUnits(600f))
    assertEqualsFloat(cam.up.x, 0f, epsilon)
    assertEqualsFloat(cam.up.y, -1f, epsilon)
    assertEqualsFloat(cam.up.z, 0f, epsilon)
    assertEqualsFloat(cam.direction.x, 0f, epsilon)
    assertEqualsFloat(cam.direction.y, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, 1f, epsilon)
  }

  test("zoom affects projection matrix") {
    given Sge = makeContext(800, 600)
    val cam1  = OrthographicCamera(WorldUnits(800f), WorldUnits(600f))
    val proj1 = cam1.projection.values.clone()

    val cam2 = OrthographicCamera()
    cam2.viewportWidth = WorldUnits(800f)
    cam2.viewportHeight = WorldUnits(600f)
    cam2.zoom = 2f
    cam2.update()
    val proj2 = cam2.projection.values

    val differs = (0 until 16).exists(i => Math.abs(proj1(i) - proj2(i)) > epsilon)
    assert(differs, "projection with zoom=2 should differ from zoom=1")
  }

  test("translate 2D adds to position") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.position.set(0, 0, 0)
    cam.translate(10f, 20f)
    assertEqualsFloat(cam.position.x, 10f, epsilon)
    assertEqualsFloat(cam.position.y, 20f, epsilon)
    assertEqualsFloat(cam.position.z, 0f, epsilon)
  }

  test("translate 2D accumulates") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.position.set(5, 10, 0)
    cam.translate(3f, 7f)
    assertEqualsFloat(cam.position.x, 8f, epsilon)
    assertEqualsFloat(cam.position.y, 17f, epsilon)
  }

  test("rotate around direction rotates up vector") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    // direction is (0,0,-1), up is (0,1,0)
    cam.rotate(90f)
    // Rotating (0,1,0) by 90 degrees around (0,0,-1) should yield (1,0,0) or (-1,0,0)
    // depending on rotation convention (right-hand rule around negative z)
    val upLen = Math.sqrt(cam.up.x * cam.up.x + cam.up.y * cam.up.y + cam.up.z * cam.up.z).toFloat
    assertEqualsFloat(upLen, 1f, epsilon)
    assertEqualsFloat(cam.up.z, 0f, epsilon)
    // The up vector should have rotated away from (0,1,0)
    assert(Math.abs(cam.up.y) < epsilon, s"up.y should be ~0 after 90 degree rotation, was ${cam.up.y}")
  }

  test("project/unproject roundtrip recovers original point") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera(WorldUnits(800f), WorldUnits(600f))
    cam.setToOrtho(false, WorldUnits(800f), WorldUnits(600f))

    val original  = Vector3(200f, 150f, 0f)
    val projected = cam.project(Vector3(200f, 150f, 0f))
    // project outputs bottom-left screen coords; unproject expects top-left touch coords
    // so we must flip y before calling unproject
    projected.y = 600f - projected.y
    val unprojected = cam.unproject(projected)

    assertEqualsFloat(unprojected.x, original.x, 0.1f)
    assertEqualsFloat(unprojected.y, original.y, 0.1f)
  }

  test("lookAt changes direction") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    cam.position.set(0, 0, 0)
    cam.lookAt(1f, 0f, 0f)
    // direction should now point towards (1,0,0)
    assertEqualsFloat(cam.direction.x, 1f, epsilon)
    assertEqualsFloat(cam.direction.y, 0f, epsilon)
    assertEqualsFloat(cam.direction.z, 0f, epsilon)
  }

  test("frustum is updated after update") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera(WorldUnits(800f), WorldUnits(600f))
    // At least one frustum plane should have a non-zero normal or d
    val hasNonZero = cam.frustum.planes.exists { p =>
      Math.abs(p.normal.x) > epsilon || Math.abs(p.normal.y) > epsilon ||
      Math.abs(p.normal.z) > epsilon || Math.abs(p.d) > epsilon
    }
    assert(hasNonZero, "frustum planes should be set after update")
  }

  test("viewportWidth and viewportHeight default to 0") {
    given Sge = makeContext(640, 480)
    val cam   = OrthographicCamera()
    assertEqualsFloat(cam.viewportWidth.toFloat, 0f, epsilon)
    assertEqualsFloat(cam.viewportHeight.toFloat, 0f, epsilon)
  }
}
