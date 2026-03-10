package sge
package math

class Vector3RotTest extends munit.FunSuite {

  private val Eps = 0.0001f

  private def assertVecEquals(actual: Vector3, expected: Vector3, msg: String = ""): Unit = {
    assert(Math.abs(actual.x - expected.x) < Eps, s"$msg x: expected ${expected.x}, got ${actual.x}")
    assert(Math.abs(actual.y - expected.y) < Eps, s"$msg y: expected ${expected.y}, got ${actual.y}")
    assert(Math.abs(actual.z - expected.z) < Eps, s"$msg z: expected ${expected.z}, got ${actual.z}")
  }

  // ---- rot vs mul ----

  test("rot with identity matrix does not change vector") {
    val v        = Vector3(1.0f, 2.0f, 3.0f)
    val identity = new Matrix4()
    v.rot(identity)
    assertVecEquals(v, Vector3(1.0f, 2.0f, 3.0f))
  }

  test("mul with identity matrix does not change vector") {
    val v        = Vector3(1.0f, 2.0f, 3.0f)
    val identity = new Matrix4()
    v.mul(identity)
    assertVecEquals(v, Vector3(1.0f, 2.0f, 3.0f))
  }

  test("rot ignores translation component of matrix") {
    val v = Vector3(1.0f, 0.0f, 0.0f)
    val m = new Matrix4()
    m.setToTranslation(10.0f, 20.0f, 30.0f) // pure translation, no rotation
    v.rot(m)
    // rot should ignore the translation — vector should be unchanged
    assertVecEquals(v, Vector3(1.0f, 0.0f, 0.0f), "rot should ignore translation")
  }

  test("mul includes translation component of matrix") {
    val v = Vector3(1.0f, 0.0f, 0.0f)
    val m = new Matrix4()
    m.setToTranslation(10.0f, 20.0f, 30.0f) // pure translation, no rotation
    v.mul(m)
    // mul includes translation: (1+10, 0+20, 0+30) = (11, 20, 30)
    assertVecEquals(v, Vector3(11.0f, 20.0f, 30.0f), "mul should include translation")
  }

  test("rot and mul give same result for pure rotation matrix") {
    val vRot = Vector3(1.0f, 0.0f, 0.0f)
    val vMul = Vector3(1.0f, 0.0f, 0.0f)
    // 90 degrees around Z axis
    val m = new Matrix4()
    m.setToRotation(0.0f, 0.0f, 1.0f, 90.0f)
    vRot.rot(m)
    vMul.mul(m)
    // Both should give the same result for a pure rotation (no translation)
    assertVecEquals(vRot, vMul, "rot and mul should agree for pure rotation")
  }

  test("rot and mul differ for translation+rotation matrix") {
    val vRot = Vector3(1.0f, 0.0f, 0.0f)
    val vMul = Vector3(1.0f, 0.0f, 0.0f)
    // Create a matrix with both rotation and translation
    val m = new Matrix4()
    m.setToRotation(0.0f, 0.0f, 1.0f, 90.0f)
    m.trn(10.0f, 20.0f, 30.0f) // add translation
    vRot.rot(m)
    vMul.mul(m)
    // rot should NOT include translation, mul SHOULD
    // So they must differ
    val differs = Math.abs(vRot.x - vMul.x) > Eps ||
      Math.abs(vRot.y - vMul.y) > Eps ||
      Math.abs(vRot.z - vMul.z) > Eps
    assert(differs, s"rot ($vRot) and mul ($vMul) should differ when matrix has translation")
  }

  test("rot with 90-degree Z rotation transforms (1,0,0) to (0,1,0)") {
    val v = Vector3(1.0f, 0.0f, 0.0f)
    val m = new Matrix4()
    m.setToRotation(0.0f, 0.0f, 1.0f, 90.0f)
    v.rot(m)
    assertVecEquals(v, Vector3(0.0f, 1.0f, 0.0f), "90° Z rotation of (1,0,0)")
  }

  test("rot with 90-degree Y rotation transforms (1,0,0) to (0,0,-1)") {
    val v = Vector3(1.0f, 0.0f, 0.0f)
    val m = new Matrix4()
    m.setToRotation(0.0f, 1.0f, 0.0f, 90.0f)
    v.rot(m)
    assertVecEquals(v, Vector3(0.0f, 0.0f, -1.0f), "90° Y rotation of (1,0,0)")
  }

  test("rotateAroundRad 90° around Z axis") {
    // Regression: rotateAroundRad was a stub returning this unchanged
    val v = Vector3(1.0f, 0.0f, 0.0f)
    v.rotateAroundRad(Vector3.Z, MathUtils.HALF_PI)
    assertVecEquals(v, Vector3(0.0f, 1.0f, 0.0f), "90° Z rotation of (1,0,0)")
  }

  test("rotateAroundDeg 90° around Y axis") {
    val v = Vector3(1.0f, 0.0f, 0.0f)
    v.rotateAroundDeg(Vector3.Y, 90f)
    assertVecEquals(v, Vector3(0.0f, 0.0f, -1.0f), "90° Y rotation of (1,0,0)")
  }

  test("rotateAroundRad 360° returns to original") {
    val v        = Vector3(3.0f, 4.0f, 5.0f)
    val original = Vector3(v.x, v.y, v.z)
    v.rotateAroundRad(Vector3.Z, MathUtils.PI2)
    assertVecEquals(v, original, "360° rotation should be identity")
  }

  test("rot returns this for chaining") {
    val v      = Vector3(1.0f, 2.0f, 3.0f)
    val m      = new Matrix4()
    val result = v.rot(m)
    assert(result eq v, "rot should return the same vector instance")
  }
}
