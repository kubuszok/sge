/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

class Matrix3Test extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertMatrixEquals(a: Matrix3, b: Matrix3, eps: Float = epsilon): Unit =
    for (i <- 0 until 9)
      assert(
        Math.abs(a.values(i) - b.values(i)) < eps,
        s"Matrix3 mismatch at index $i: ${a.values(i)} vs ${b.values(i)}"
      )

  private def identityMatrix(): Matrix3 = new Matrix3().idt()

  test("identity determinant is 1") {
    assertEqualsFloat(identityMatrix().det(), 1f, epsilon)
  }

  test("singular matrix determinant is 0") {
    val m = new Matrix3()
    for (i <- 0 until 9) m.values(i) = 0f
    assertEqualsFloat(m.det(), 0f, epsilon)
  }

  test("inv identity") {
    val m = identityMatrix().inv()
    assertMatrixEquals(m, identityMatrix())
  }

  test("inv translation") {
    val m = new Matrix3().idt()
    m.setToTranslation(3, 4)
    m.inv()
    val expected = new Matrix3().idt()
    expected.setToTranslation(-3, -4)
    assertMatrixEquals(m, expected, 1e-3f)
  }

  test("M * M.inv() = I") {
    val m = new Matrix3().idt()
    m.values(Matrix3.M00) = 1; m.values(Matrix3.M01) = 2; m.values(Matrix3.M02) = 3
    m.values(Matrix3.M10) = 0; m.values(Matrix3.M11) = 4; m.values(Matrix3.M12) = 5
    m.values(Matrix3.M20) = 1; m.values(Matrix3.M21) = 0; m.values(Matrix3.M22) = 6
    val copy    = new Matrix3().set(m)
    val mInv    = copy.inv()
    val product = new Matrix3().set(m).mul(mInv)
    assertMatrixEquals(product, identityMatrix(), 1e-3f)
  }

  test("transpose of identity is identity") {
    assertMatrixEquals(identityMatrix().transpose(), identityMatrix())
  }

  test("double transpose is original") {
    val m = new Matrix3().idt()
    m.values(Matrix3.M00) = 1; m.values(Matrix3.M01) = 2; m.values(Matrix3.M02) = 3
    m.values(Matrix3.M10) = 4; m.values(Matrix3.M11) = 5; m.values(Matrix3.M12) = 6
    m.values(Matrix3.M20) = 7; m.values(Matrix3.M21) = 8; m.values(Matrix3.M22) = 9
    val original = new Matrix3().set(m)
    m.transpose().transpose()
    assertMatrixEquals(m, original)
  }

  test("setToRotation 90 degrees") {
    val m = new Matrix3().setToRotation(90f)
    val v = Vector2(1, 0)
    v * m
    assert(Math.abs(v.x - 0f) < 1e-3f, s"x: ${v.x}")
    assert(Math.abs(v.y - 1f) < 1e-3f, s"y: ${v.y}")
  }

  test("setToScaling") {
    val m = new Matrix3().setToScaling(2f, 3f)
    val v = Vector2(1, 1)
    v * m
    assert(Math.abs(v.x - 2f) < epsilon, s"x: ${v.x}")
    assert(Math.abs(v.y - 3f) < epsilon, s"y: ${v.y}")
  }

  test("setToTranslation") {
    val m = new Matrix3().setToTranslation(5f, 7f)
    val v = Vector2(0, 0)
    v * m
    assert(Math.abs(v.x - 5f) < epsilon, s"x: ${v.x}")
    assert(Math.abs(v.y - 7f) < epsilon, s"y: ${v.y}")
  }

  test("mul combines translations") {
    val m1 = new Matrix3().setToTranslation(3f, 0f)
    val m2 = new Matrix3().setToTranslation(0f, 4f)
    m1.mul(m2)
    val v = Vector2(0, 0)
    v * m1
    assert(Math.abs(v.x - 3f) < epsilon, s"x: ${v.x}")
    assert(Math.abs(v.y - 4f) < epsilon, s"y: ${v.y}")
  }

  test("getTranslation") {
    val m   = new Matrix3().setToTranslation(10f, 20f)
    val pos = m.getTranslation(Vector2())
    assertEqualsFloat(pos.x, 10f, epsilon)
    assertEqualsFloat(pos.y, 20f, epsilon)
  }

  test("getScale from scaling matrix") {
    val m     = new Matrix3().setToScaling(3f, 4f)
    val scale = m.getScale(Vector2())
    assertEqualsFloat(scale.x, 3f, epsilon)
    assertEqualsFloat(scale.y, 4f, epsilon)
  }

  test("det(A * B) ≈ det(A) * det(B)") {
    val a = new Matrix3().idt()
    a.values(Matrix3.M00) = 1; a.values(Matrix3.M01) = 2; a.values(Matrix3.M02) = 0
    a.values(Matrix3.M10) = 3; a.values(Matrix3.M11) = 4; a.values(Matrix3.M12) = 0
    a.values(Matrix3.M20) = 0; a.values(Matrix3.M21) = 0; a.values(Matrix3.M22) = 1
    val b = new Matrix3().idt()
    b.values(Matrix3.M00) = 5; b.values(Matrix3.M01) = 6; b.values(Matrix3.M02) = 0
    b.values(Matrix3.M10) = 7; b.values(Matrix3.M11) = 8; b.values(Matrix3.M12) = 0
    b.values(Matrix3.M20) = 0; b.values(Matrix3.M21) = 0; b.values(Matrix3.M22) = 1
    val detA  = a.det()
    val detB  = b.det()
    val detAB = new Matrix3().set(a).mul(b).det()
    assertEqualsFloat(detAB, detA * detB, 1e-2f)
  }

  test("scl scales the matrix") {
    val m = identityMatrix()
    m.scl(2f)
    val v = Vector2(1, 1)
    v * m
    assertEqualsFloat(v.x, 2f, epsilon)
    assertEqualsFloat(v.y, 2f, epsilon)
  }

  test("set from Matrix4") {
    val m4 = new Matrix4().idt()
    m4.values(Matrix4.M00) = 1; m4.values(Matrix4.M01) = 2; m4.values(Matrix4.M02) = 3
    m4.values(Matrix4.M10) = 4; m4.values(Matrix4.M11) = 5; m4.values(Matrix4.M12) = 6
    m4.values(Matrix4.M20) = 7; m4.values(Matrix4.M21) = 8; m4.values(Matrix4.M22) = 9
    val m3 = new Matrix3().set(m4)
    assertEqualsFloat(m3.values(Matrix3.M00), 1f, epsilon)
    assertEqualsFloat(m3.values(Matrix3.M11), 5f, epsilon)
    assertEqualsFloat(m3.values(Matrix3.M22), 9f, epsilon)
  }

  test("getRotation from rotation matrix") {
    val m   = new Matrix3().setToRotation(45f)
    val deg = m.getRotation
    assertEqualsFloat(deg, 45f, 0.1f)
  }

  test("getRotationRad from rotation matrix") {
    val m   = new Matrix3().setToRotationRad(MathUtils.HALF_PI)
    val rad = m.getRotationRad
    assertEqualsFloat(rad, MathUtils.HALF_PI, 0.01f)
  }
}
