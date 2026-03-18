/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

/** Focused regression tests for transform bugs fixed in Phase 1:
  *   - Matrix3 identity constructor
  *   - Matrix3.mul correctness
  *   - Matrix4.mulLeft actually storing the result
  *   - Affine2.set(Matrix3) column-major indexing
  *   - Affine2.set(Matrix4) column-major indexing
  *   - Vector3.rotateRad via Rodrigues' formula
  */
class TransformTest extends munit.FunSuite {

  private val epsilon = 0.001f

  // ---------------------------------------------------------------------------
  // Matrix3 identity constructor
  // ---------------------------------------------------------------------------

  test("Matrix3() default constructor produces identity matrix") {
    val m = new Matrix3()
    assertEqualsFloat(m.values(Matrix3.M00), 1f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M11), 1f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M22), 1f, epsilon)
    // off-diagonal zeros
    assertEqualsFloat(m.values(Matrix3.M01), 0f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M02), 0f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M10), 0f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M12), 0f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M20), 0f, epsilon)
    assertEqualsFloat(m.values(Matrix3.M21), 0f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Matrix3.mul
  // ---------------------------------------------------------------------------

  test("Matrix3.mul produces correct result for known matrices") {
    // A = [[2,0,0],[0,3,0],[0,0,1]] (scaling)
    // B = translation(5,7)
    // A*B should scale then translate: diagonal preserved, translation scaled
    val a = new Matrix3()
    a.values(Matrix3.M00) = 2f
    a.values(Matrix3.M11) = 3f
    // M22 already 1 from identity

    val b = new Matrix3().setToTranslation(5f, 7f)

    a.mul(b)

    // Result: scaling * translation
    // M00=2, M11=3, M22=1 (diagonal unchanged)
    // translation column: M02 = 2*5 = 10, M12 = 3*7 = 21
    assertEqualsFloat(a.values(Matrix3.M00), 2f, epsilon)
    assertEqualsFloat(a.values(Matrix3.M11), 3f, epsilon)
    assertEqualsFloat(a.values(Matrix3.M22), 1f, epsilon)
    assertEqualsFloat(a.values(Matrix3.M02), 10f, epsilon)
    assertEqualsFloat(a.values(Matrix3.M12), 21f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Matrix4.mulLeft stores result
  // ---------------------------------------------------------------------------

  test("Matrix4.mulLeft modifies the matrix (regression: was returning unchanged)") {
    val a = new Matrix4().idt().setTranslation(1f, 2f, 3f)
    val b = new Matrix4().idt().setTranslation(10f, 20f, 30f)

    // a.mulLeft(b) computes b * a, stores into a
    a.mulLeft(b)

    val pos = a.translation(new Vector3())
    // translations add: (1+10, 2+20, 3+30) = (11, 22, 33)
    assertEqualsFloat(pos.x, 11f, epsilon)
    assertEqualsFloat(pos.y, 22f, epsilon)
    assertEqualsFloat(pos.z, 33f, epsilon)
  }

  test("Matrix4.mulLeft with non-commutative transforms gives correct order") {
    // scale(2) then translate(5,0,0) via mulLeft:
    // result = translate * scale, so (1,0,0) -> scale to (2,0,0) -> translate to (7,0,0)
    val scale = new Matrix4().idt().scale(2f, 2f, 2f)
    val trans = new Matrix4().idt().setTranslation(5f, 0f, 0f)
    scale.mulLeft(trans) // scale := trans * scale

    val v = new Vector3(1f, 0f, 0f)
    v.mul(scale)
    assertEqualsFloat(v.x, 7f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Affine2.set(Matrix3) column-major indexing
  // ---------------------------------------------------------------------------

  test("Affine2.set(Matrix3) uses column-major constants correctly") {
    val m3 = new Matrix3().setToTranslation(5f, 7f)
    val a  = new Affine2()
    a.set(m3)

    // Identity diagonal
    assertEqualsFloat(a.m00, 1f, epsilon)
    assertEqualsFloat(a.m11, 1f, epsilon)
    // Translation
    assertEqualsFloat(a.m02, 5f, epsilon)
    assertEqualsFloat(a.m12, 7f, epsilon)
    // Off-diagonal zeros
    assertEqualsFloat(a.m01, 0f, epsilon)
    assertEqualsFloat(a.m10, 0f, epsilon)
  }

  test("Affine2.set(Matrix3) preserves rotation") {
    val m3 = new Matrix3().setToRotation(90f)
    val a  = new Affine2()
    a.set(m3)

    // Apply to (1,0): should get (0,1) for 90-degree CCW rotation
    val v = Vector2(1f, 0f)
    a.applyTo(v)
    assert(Math.abs(v.x) < epsilon, s"x should be ~0, got ${v.x}")
    assert(Math.abs(v.y - 1f) < epsilon, s"y should be ~1, got ${v.y}")
  }

  // ---------------------------------------------------------------------------
  // Affine2.set(Matrix4) column-major indexing
  // ---------------------------------------------------------------------------

  test("Affine2.set(Matrix4) uses column-major constants correctly") {
    val m4 = new Matrix4().idt().setTranslation(3f, 9f, 0f)
    val a  = new Affine2()
    a.set(m4)

    // Identity diagonal
    assertEqualsFloat(a.m00, 1f, epsilon)
    assertEqualsFloat(a.m11, 1f, epsilon)
    // Translation (from M03, M13)
    assertEqualsFloat(a.m02, 3f, epsilon)
    assertEqualsFloat(a.m12, 9f, epsilon)
  }

  test("Affine2.set(Matrix4) preserves 2D scaling") {
    val m4 = new Matrix4().idt().scale(2f, 3f, 1f)
    val a  = new Affine2()
    a.set(m4)

    assertEqualsFloat(a.m00, 2f, epsilon)
    assertEqualsFloat(a.m11, 3f, epsilon)
    assertEqualsFloat(a.m01, 0f, epsilon)
    assertEqualsFloat(a.m10, 0f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Vector3.rotateRad
  // ---------------------------------------------------------------------------

  test("Vector3.rotateRad 90 degrees around Z axis") {
    val v = Vector3(1f, 0f, 0f)
    v.rotateRad(MathUtils.HALF_PI, 0f, 0f, 1f)
    // (1,0,0) rotated 90° CCW around Z -> (0,1,0)
    assert(Math.abs(v.x) < epsilon, s"x should be ~0, got ${v.x}")
    assert(Math.abs(v.y - 1f) < epsilon, s"y should be ~1, got ${v.y}")
    assert(Math.abs(v.z) < epsilon, s"z should be ~0, got ${v.z}")
  }

  test("Vector3.rotateRad 90 degrees around Y axis") {
    val v = Vector3(1f, 0f, 0f)
    v.rotateRad(MathUtils.HALF_PI, 0f, 1f, 0f)
    // (1,0,0) rotated 90° around Y -> (0,0,-1)
    assert(Math.abs(v.x) < epsilon, s"x should be ~0, got ${v.x}")
    assert(Math.abs(v.y) < epsilon, s"y should be ~0, got ${v.y}")
    assert(Math.abs(v.z + 1f) < epsilon, s"z should be ~-1, got ${v.z}")
  }

  test("Vector3.rotateRad 360 degrees returns to original") {
    val v = Vector3(3f, 4f, 5f)
    v.rotateRad(MathUtils.PI2, 0f, 0f, 1f)
    assert(Math.abs(v.x - 3f) < epsilon, s"x should be ~3, got ${v.x}")
    assert(Math.abs(v.y - 4f) < epsilon, s"y should be ~4, got ${v.y}")
    assert(Math.abs(v.z - 5f) < epsilon, s"z should be ~5, got ${v.z}")
  }

  test("Vector3.rotateRad around arbitrary axis") {
    // Rotate (1,0,0) by 180° around (1,1,0)/sqrt(2) axis
    // Rodrigues: v' = v*cos(pi) + (k x v)*sin(pi) + k*(k.v)*(1-cos(pi))
    // cos(pi)=-1, sin(pi)=0, so v' = -v + 2*k*(k.v)
    // k = (1/sqrt2, 1/sqrt2, 0), k.v = 1/sqrt2
    // v' = (-1,0,0) + 2*(1/sqrt2, 1/sqrt2, 0)*(1/sqrt2)
    //     = (-1,0,0) + (1,1,0) = (0,1,0)
    val len = Math.sqrt(2.0).toFloat
    val v   = Vector3(1f, 0f, 0f)
    v.rotateRad(MathUtils.PI, 1f / len, 1f / len, 0f)
    assert(Math.abs(v.x) < epsilon, s"x should be ~0, got ${v.x}")
    assert(Math.abs(v.y - 1f) < epsilon, s"y should be ~1, got ${v.y}")
    assert(Math.abs(v.z) < epsilon, s"z should be ~0, got ${v.z}")
  }
}
