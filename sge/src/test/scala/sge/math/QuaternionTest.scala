/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class QuaternionTest extends munit.ScalaCheckSuite {

  private val epsilon = 1e-3f

  private def assertQuatEquals(a: Quaternion, b: Quaternion, eps: Float = epsilon): Unit = {
    assert(Math.abs(a.x - b.x) < eps, s"x: ${a.x} vs ${b.x}")
    assert(Math.abs(a.y - b.y) < eps, s"y: ${a.y} vs ${b.y}")
    assert(Math.abs(a.z - b.z) < eps, s"z: ${a.z} vs ${b.z}")
    assert(Math.abs(a.w - b.w) < eps, s"w: ${a.w} vs ${b.w}")
  }

  private def assertVec3Equals(a: Vector3, b: Vector3, eps: Float): Unit = {
    assert(Math.abs(a.x - b.x) < eps, s"x: ${a.x} vs ${b.x}")
    assert(Math.abs(a.y - b.y) < eps, s"y: ${a.y} vs ${b.y}")
    assert(Math.abs(a.z - b.z) < eps, s"z: ${a.z} vs ${b.z}")
  }

  // ---- Basic operations ----

  test("identity quaternion") {
    val q = Quaternion()
    assert(q.isIdentity())
    assertEqualsFloat(q.len(), 1f, epsilon)
  }

  test("idt resets to identity") {
    val q = Quaternion(1, 2, 3, 4)
    q.idt()
    assertQuatEquals(q, Quaternion(0, 0, 0, 1))
  }

  test("len of identity is 1") {
    assertEqualsFloat(Quaternion().len(), 1f, epsilon)
  }

  test("len2 of identity is 1") {
    assertEqualsFloat(Quaternion().len2(), 1f, epsilon)
  }

  test("normalize") {
    val q = Quaternion(1, 2, 3, 4)
    q.nor()
    assertEqualsFloat(q.len(), 1f, epsilon)
  }

  test("conjugate") {
    val q = Quaternion(1, 2, 3, 4)
    q.conjugate()
    assertQuatEquals(q, Quaternion(-1, -2, -3, 4))
  }

  test("copy") {
    val q = Quaternion(1, 2, 3, 4)
    val c = q.cpy()
    assertQuatEquals(q, c)
    c.x = 99
    assertEqualsFloat(q.x, 1f, epsilon)
  }

  // ---- Multiplication ----

  test("mul identity produces same quaternion") {
    val q = Quaternion(1, 2, 3, 4).nor()
    val r = q.cpy()
    q.mul(Quaternion()) // multiply by identity
    assertQuatEquals(q, r)
  }

  test("mulLeft identity produces same quaternion") {
    val q = Quaternion(1, 2, 3, 4).nor()
    val r = q.cpy()
    q.mulLeft(Quaternion()) // premultiply by identity
    assertQuatEquals(q, r)
  }

  test("q * q.conjugate() ≈ identity (for unit quaternions)") {
    val q    = Quaternion(1, 2, 3, 4).nor()
    val conj = q.cpy().conjugate()
    q.mul(conj)
    assert(q.isIdentity(0.01f), s"q * q.conjugate() = $q")
  }

  // ---- Euler angles ----

  test("setEulerAngles yaw 90") {
    val q = Quaternion()
    q.setEulerAngles(90, 0, 0)
    assertEqualsFloat(q.yaw, 90f, 1f)
  }

  test("setEulerAngles pitch 45") {
    val q = Quaternion()
    q.setEulerAngles(0, 45, 0)
    assertEqualsFloat(q.pitch, 45f, 1f)
  }

  test("setEulerAngles roll 30") {
    val q = Quaternion()
    q.setEulerAngles(0, 0, 30)
    assertEqualsFloat(q.roll, 30f, 1f)
  }

  test("euler angle roundtrip (no gimbal lock)") {
    val yaw   = 30f
    val pitch = 20f
    val roll  = 10f
    val q     = Quaternion().setEulerAngles(yaw, pitch, roll)
    assertEqualsFloat(q.yaw, yaw, 1f)
    assertEqualsFloat(q.pitch, pitch, 1f)
    assertEqualsFloat(q.roll, roll, 1f)
  }

  test("gimbal lock at north pole") {
    // getGimbalPole checks y*x + z*w > 0.499
    // Construct quaternion where y*x + z*w = 0.5 exactly
    val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f) // y*x=0.25, z*w=0.25, sum=0.5
    assertEquals(q.gimbalPole, 1)
  }

  test("gimbal lock at south pole") {
    // y*x + z*w = -0.5
    val q = Quaternion(-0.5f, 0.5f, -0.5f, 0.5f) // y*x=-0.25, z*w=-0.25, sum=-0.5
    assertEquals(q.gimbalPole, -1)
  }

  test("no gimbal lock for normal rotation") {
    val q = Quaternion().setEulerAngles(30, 20, 10)
    assertEquals(q.gimbalPole, 0)
  }

  // ---- Axis-angle ----

  test("setFromAxis 90 deg around Z") {
    val q = Quaternion()
    q.setFromAxis(0, 0, 1, 90)
    val v = Vector3(1, 0, 0)
    q.transform(v)
    assertVec3Equals(v, Vector3(0, 1, 0), 0.01f)
  }

  test("setFromAxis 180 deg around Y") {
    val q = Quaternion()
    q.setFromAxis(0, 1, 0, 180)
    val v = Vector3(1, 0, 0)
    q.transform(v)
    assertVec3Equals(v, Vector3(-1, 0, 0), 0.01f)
  }

  test("setFromAxis 360 deg ≈ identity") {
    val q = Quaternion()
    q.setFromAxis(1, 0, 0, 360)
    // Should be approximately ±identity
    assert(q.isIdentity(0.01f) || (Math.abs(q.w + 1f) < 0.01f && Math.abs(q.x) < 0.01f))
  }

  // ---- Transform preserves length ----

  test("transform preserves vector length") {
    val q           = Quaternion().setFromAxis(1, 1, 1, 60).nor()
    val v           = Vector3(3, 4, 0)
    val originalLen = v.length
    q.transform(v)
    assertEqualsFloat(v.length, originalLen, 0.01f)
  }

  // ---- Slerp ----

  test("slerp at 0 returns start") {
    val q1 = Quaternion().setFromAxis(0, 0, 1, 0)
    val q2 = Quaternion().setFromAxis(0, 0, 1, 90)
    val q  = q1.cpy()
    q.slerp(q2, 0f)
    assertQuatEquals(q, q1, 0.01f)
  }

  test("slerp at 1 returns end") {
    val q1 = Quaternion().setFromAxis(0, 0, 1, 0)
    val q2 = Quaternion().setFromAxis(0, 0, 1, 90)
    val q  = q1.cpy()
    q.slerp(q2, 1f)
    assertQuatEquals(q, q2, 0.01f)
  }

  test("slerp at 0.5 is midpoint") {
    val q1 = Quaternion().setFromAxis(0, 0, 1, 0)
    val q2 = Quaternion().setFromAxis(0, 0, 1, 90)
    val q  = q1.cpy()
    q.slerp(q2, 0.5f)
    // The midpoint should represent ~45 degree rotation around Z
    val v = Vector3(1, 0, 0)
    q.transform(v)
    assertEqualsFloat(v.x, Math.cos(Math.toRadians(45)).toFloat, 0.05f)
    assertEqualsFloat(v.y, Math.sin(Math.toRadians(45)).toFloat, 0.05f)
  }

  // ---- toMatrix ----

  test("identity quaternion produces identity matrix") {
    val q   = Quaternion()
    val mat = new Array[Float](16)
    q.toMatrix(mat)
    assertEqualsFloat(mat(Matrix4.M00), 1f, epsilon)
    assertEqualsFloat(mat(Matrix4.M11), 1f, epsilon)
    assertEqualsFloat(mat(Matrix4.M22), 1f, epsilon)
    assertEqualsFloat(mat(Matrix4.M33), 1f, epsilon)
    assertEqualsFloat(mat(Matrix4.M01), 0f, epsilon)
    assertEqualsFloat(mat(Matrix4.M10), 0f, epsilon)
  }

  // ---- Dot product ----

  test("dot of identical quaternions is len2") {
    val q = Quaternion(1, 2, 3, 4)
    assertEqualsFloat(q.dot(q), q.len2(), epsilon)
  }

  test("dot of orthogonal quaternions is 0") {
    val q1 = Quaternion(1, 0, 0, 0)
    val q2 = Quaternion(0, 1, 0, 0)
    assertEqualsFloat(q1.dot(q2), 0f, epsilon)
  }

  // ---- Add ----

  test("add quaternions") {
    val q = Quaternion(1, 2, 3, 4)
    q.add(Quaternion(5, 6, 7, 8))
    assertQuatEquals(q, Quaternion(6, 8, 10, 12))
  }

  // ---- getAngle ----

  test("getAngle for 90 degree rotation") {
    val q = Quaternion().setFromAxis(0, 0, 1, 90)
    assertEqualsFloat(q.angle, 90f, 1f)
  }

  test("getAngle for identity is 0") {
    assertEqualsFloat(Quaternion().angle, 0f, 1f)
  }

  // ---- swingTwist ----

  test("swingTwist decomposes rotation") {
    val q     = Quaternion().setFromAxis(0, 1, 0, 90)
    val swing = Quaternion()
    val twist = Quaternion()
    q.swingTwist(0f, 1f, 0f, swing, twist)
    // twist should contain the Y-axis rotation; swing should be identity-ish
    val recomposed = swing.cpy().mul(twist)
    assertQuatEquals(q, recomposed, 0.05f)
  }

  // ---- Property-based ----

  private val smallAngle: Gen[Float] = Gen.choose(-180f, 180f)

  property("normalize produces unit length") {
    forAll(Gen.choose(-10f, 10f), Gen.choose(-10f, 10f), Gen.choose(-10f, 10f), Gen.choose(0.1f, 10f)) { (x: Float, y: Float, z: Float, w: Float) =>
      val q = Quaternion(x, y, z, w)
      q.nor()
      Math.abs(q.len() - 1f) < 0.001f
    }
  }

  property("transform preserves vector length") {
    forAll(smallAngle) { (angle: Float) =>
      val q           = Quaternion().setFromAxis(0, 0, 1, angle).nor()
      val v           = Vector3(3, 4, 5)
      val originalLen = v.length
      q.transform(v)
      Math.abs(v.length - originalLen) < 0.1f
    }
  }

  property("slerp(q, q, t) ≈ q for any t") {
    forAll(smallAngle, Gen.choose(0f, 1f)) { (angle: Float, t: Float) =>
      val q = Quaternion().setFromAxis(0, 1, 0, angle).nor()
      val r = q.cpy()
      q.slerp(r, t)
      Math.abs(q.x - r.x) < 0.01f && Math.abs(q.y - r.y) < 0.01f &&
      Math.abs(q.z - r.z) < 0.01f && Math.abs(q.w - r.w) < 0.01f
    }
  }
}
