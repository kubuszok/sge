package sge
package graphics

import sge.math.{ Matrix4, Vector3 }

class CameraRotateTest extends munit.FunSuite {

  private val Eps = 0.0001f

  private def assertVecEquals(actual: Vector3, expected: Vector3, msg: String): Unit = {
    assert(Math.abs(actual.x - expected.x) < Eps, s"$msg x: expected ${expected.x}, got ${actual.x}")
    assert(Math.abs(actual.y - expected.y) < Eps, s"$msg y: expected ${expected.y}, got ${actual.y}")
    assert(Math.abs(actual.z - expected.z) < Eps, s"$msg z: expected ${expected.z}, got ${actual.z}")
  }

  private def makeCamera(): OrthographicCamera = {
    given Sge = SgeTestFixture.testSge()
    new OrthographicCamera(100f, 100f)
  }

  // ---- rotate(Matrix4) uses rot, not mul ----

  test("rotate(Matrix4) with pure translation does not move direction") {
    val cam = makeCamera()
    // Save original direction
    val origDir = Vector3(cam.direction.x, cam.direction.y, cam.direction.z)
    val origUp  = Vector3(cam.up.x, cam.up.y, cam.up.z)

    val translation = new Matrix4()
    translation.setToTranslation(100.0f, 200.0f, 300.0f)

    cam.rotate(translation)

    // rot ignores translation — direction should be unchanged
    assertVecEquals(cam.direction, origDir, "direction after translate-only rotate")
    assertVecEquals(cam.up, origUp, "up after translate-only rotate")
  }

  test("rotate(Matrix4) with 90-degree Z rotation transforms direction") {
    val cam = makeCamera()
    // Default direction is (0, 0, -1), up is (0, 1, 0)
    val m = new Matrix4()
    m.setToRotation(0.0f, 0.0f, 1.0f, 90.0f) // 90° around Z

    cam.rotate(m)

    // direction (0, 0, -1) rotated 90° around Z → (0, 0, -1) (Z unchanged by Z rotation)
    assertVecEquals(cam.direction, Vector3(0.0f, 0.0f, -1.0f), "direction after Z rotation")
    // up (0, 1, 0) rotated 90° around Z → (-1, 0, 0)
    assertVecEquals(cam.up, Vector3(-1.0f, 0.0f, 0.0f), "up after Z rotation")
  }

  test("rotate(Matrix4) with 90-degree X rotation transforms direction and up") {
    val cam = makeCamera()
    // Default direction is (0, 0, -1), up is (0, 1, 0)
    val m = new Matrix4()
    m.setToRotation(1.0f, 0.0f, 0.0f, 90.0f) // 90° around X

    cam.rotate(m)

    // direction (0, 0, -1) rotated 90° around X → (0, 1, 0)
    assertVecEquals(cam.direction, Vector3(0.0f, 1.0f, 0.0f), "direction after X rotation")
    // up (0, 1, 0) rotated 90° around X → (0, 0, 1)
    assertVecEquals(cam.up, Vector3(0.0f, 0.0f, 1.0f), "up after X rotation")
  }

  test("rotate(Matrix4) with translation+rotation ignores translation component") {
    val cam1 = makeCamera()
    val cam2 = makeCamera()

    // Pure rotation matrix
    val rotation = new Matrix4()
    rotation.setToRotation(0.0f, 1.0f, 0.0f, 45.0f) // 45° around Y

    // Rotation + translation
    val rotAndTrans = new Matrix4()
    rotAndTrans.setToRotation(0.0f, 1.0f, 0.0f, 45.0f)
    rotAndTrans.trn(999.0f, 888.0f, 777.0f)

    cam1.rotate(rotation)
    cam2.rotate(rotAndTrans)

    // Both cameras should have the same direction and up because rot ignores translation
    assertVecEquals(cam1.direction, cam2.direction, "direction should match with or without translation")
    assertVecEquals(cam1.up, cam2.up, "up should match with or without translation")
  }

  test("rotate(Matrix4) with 90-degree Y rotation transforms direction") {
    val cam = makeCamera()
    // Default direction is (0, 0, -1)
    val m = new Matrix4()
    m.setToRotation(0.0f, 1.0f, 0.0f, 90.0f) // 90° around Y

    cam.rotate(m)

    // direction (0, 0, -1) rotated 90° around Y → (-1, 0, 0)
    assertVecEquals(cam.direction, Vector3(-1.0f, 0.0f, 0.0f), "direction after Y rotation")
    // up (0, 1, 0) rotated 90° around Y → (0, 1, 0) (unchanged)
    assertVecEquals(cam.up, Vector3(0.0f, 1.0f, 0.0f), "up after Y rotation")
  }
}
