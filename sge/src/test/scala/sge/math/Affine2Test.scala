/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

class Affine2Test extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertAffineEquals(a: Affine2, b: Affine2, eps: Float = epsilon): Unit = {
    assert(Math.abs(a.m00 - b.m00) < eps, s"m00: ${a.m00} vs ${b.m00}")
    assert(Math.abs(a.m01 - b.m01) < eps, s"m01: ${a.m01} vs ${b.m01}")
    assert(Math.abs(a.m02 - b.m02) < eps, s"m02: ${a.m02} vs ${b.m02}")
    assert(Math.abs(a.m10 - b.m10) < eps, s"m10: ${a.m10} vs ${b.m10}")
    assert(Math.abs(a.m11 - b.m11) < eps, s"m11: ${a.m11} vs ${b.m11}")
    assert(Math.abs(a.m12 - b.m12) < eps, s"m12: ${a.m12} vs ${b.m12}")
  }

  private def applyAndGet(a: Affine2, x: Float, y: Float): Vector2 = {
    val v = Vector2(x, y)
    a.applyTo(v)
    v
  }

  test("identity") {
    val a = new Affine2()
    assert(a.isIdt())
    assertEqualsFloat(a.det(), 1f, epsilon)
  }

  test("idt resets to identity") {
    val a = new Affine2()
    a.setToTranslation(5, 10)
    a.idt()
    assert(a.isIdt())
  }

  test("setToTranslation") {
    val a = new Affine2()
    a.setToTranslation(3f, 7f)
    assert(a.isTranslation())
    val pos = a.getTranslation(Vector2())
    assertEqualsFloat(pos.x, 3f, epsilon)
    assertEqualsFloat(pos.y, 7f, epsilon)
  }

  test("setToScaling") {
    val a = new Affine2()
    a.setToScaling(2f, 3f)
    assertEqualsFloat(a.m00, 2f, epsilon)
    assertEqualsFloat(a.m11, 3f, epsilon)
    assertEqualsFloat(a.det(), 6f, epsilon)
  }

  test("setToRotation") {
    val a = new Affine2()
    a.setToRotation(90f)
    val v = applyAndGet(a, 1, 0)
    assert(Math.abs(v.x) < 1e-3f, s"x: ${v.x}")
    assert(Math.abs(v.y - 1f) < 1e-3f, s"y: ${v.y}")
  }

  test("setToShearing") {
    val a = new Affine2()
    a.setToShearing(1f, 0f)
    val v = applyAndGet(a, 0, 1)
    assertEqualsFloat(v.x, 1f, epsilon)
    assertEqualsFloat(v.y, 1f, epsilon)
  }

  test("det of identity is 1") {
    assertEqualsFloat(new Affine2().det(), 1f, epsilon)
  }

  test("det of scaling is product of scales") {
    val a = new Affine2()
    a.setToScaling(3f, 5f)
    assertEqualsFloat(a.det(), 15f, epsilon)
  }

  test("translate") {
    val a = new Affine2()
    a.translate(3f, 4f)
    val pos = a.getTranslation(Vector2())
    assertEqualsFloat(pos.x, 3f, epsilon)
    assertEqualsFloat(pos.y, 4f, epsilon)
  }

  test("scale") {
    val a = new Affine2()
    a.scale(2f, 3f)
    assertEqualsFloat(a.m00, 2f, epsilon)
    assertEqualsFloat(a.m11, 3f, epsilon)
  }

  test("rotate") {
    val a = new Affine2()
    a.rotate(180f)
    val v = applyAndGet(a, 1, 0)
    assert(Math.abs(v.x + 1f) < 1e-3f, s"x: ${v.x}")
    assert(Math.abs(v.y) < 1e-3f, s"y: ${v.y}")
  }

  test("inv of identity is identity") {
    val a = new Affine2()
    a.inv()
    assert(a.isIdt())
  }

  test("A * A.inv() ≈ I") {
    val a = new Affine2()
    a.setToTranslation(5f, 3f)
    a.rotate(45f)
    a.scale(2f, 1.5f)
    val original = new Affine2(a)
    val inverse  = new Affine2(a)
    inverse.inv()
    original.mul(inverse)
    assert(
      original.isIdt() ||
        Math.abs(original.m00 - 1) < 0.01f && Math.abs(original.m11 - 1) < 0.01f &&
        Math.abs(original.m01) < 0.01f && Math.abs(original.m10) < 0.01f &&
        Math.abs(original.m02) < 0.01f && Math.abs(original.m12) < 0.01f
    )
  }

  test("mul combines translations") {
    val t1 = new Affine2()
    t1.setToTranslation(10f, 0f)
    val t2 = new Affine2()
    t2.setToTranslation(0f, 20f)
    t1.mul(t2)
    val v = applyAndGet(t1, 0, 0)
    assertEqualsFloat(v.x, 10f, epsilon)
    assertEqualsFloat(v.y, 20f, epsilon)
  }

  test("preMul") {
    val t1 = new Affine2()
    t1.setToTranslation(10f, 0f)
    val t2 = new Affine2()
    t2.setToTranslation(0f, 20f)
    t1.preMul(t2)
    val v = applyAndGet(t1, 0, 0)
    assertEqualsFloat(v.x, 10f, epsilon)
    assertEqualsFloat(v.y, 20f, epsilon)
  }

  test("set from another Affine2") {
    val a = new Affine2()
    a.setToTranslation(1f, 2f)
    val b = new Affine2()
    b.set(a)
    assertAffineEquals(a, b)
  }

  test("same scaling produces same values") {
    val a = new Affine2()
    a.setToScaling(2f, 3f)
    val b = new Affine2()
    b.setToScaling(2f, 3f)
    assertAffineEquals(a, b)
  }

  test("preTranslate") {
    val a = new Affine2()
    a.setToRotation(90f)
    a.preTranslate(5f, 0f)
    val v = applyAndGet(a, 0, 0)
    assertEqualsFloat(v.x, 5f, 0.01f)
  }

  test("preScale") {
    val a = new Affine2()
    a.setToTranslation(2f, 3f)
    a.preScale(2f, 2f)
    val v = applyAndGet(a, 0, 0)
    assertEqualsFloat(v.x, 4f, epsilon)
    assertEqualsFloat(v.y, 6f, epsilon)
  }
}
