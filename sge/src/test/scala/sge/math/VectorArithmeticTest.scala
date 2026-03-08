/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class VectorArithmeticTest extends munit.ScalaCheckSuite {

  private val epsilon = 1e-4f

  private def assertVec2Equals(a: Vector2, b: Vector2, eps: Float = epsilon): Unit = {
    assert(Math.abs(a.x - b.x) < eps, s"x: ${a.x} vs ${b.x}")
    assert(Math.abs(a.y - b.y) < eps, s"y: ${a.y} vs ${b.y}")
  }

  private def assertVec3Equals(a: Vector3, b: Vector3, eps: Float = epsilon): Unit = {
    assert(Math.abs(a.x - b.x) < eps, s"x: ${a.x} vs ${b.x}")
    assert(Math.abs(a.y - b.y) < eps, s"y: ${a.y} vs ${b.y}")
    assert(Math.abs(a.z - b.z) < eps, s"z: ${a.z} vs ${b.z}")
  }

  private val smallFloat:   Gen[Float] = Gen.choose(-100f, 100f)
  private val nonZeroFloat: Gen[Float] = Gen.choose(0.1f, 100f)

  // ---- Vector2 basic arithmetic ----

  test("Vector2 add") {
    val v = Vector2(1, 2)
    v + Vector2(3, 4)
    assertVec2Equals(v, Vector2(4, 6))
  }

  test("Vector2 sub") {
    val v = Vector2(5, 7)
    v - Vector2(2, 3)
    assertVec2Equals(v, Vector2(3, 4))
  }

  test("Vector2 scale by scalar") {
    val v = Vector2(3, 4)
    v.scale(2f)
    assertVec2Equals(v, Vector2(6, 8))
  }

  test("Vector2 scale by vector") {
    val v = Vector2(3, 4)
    v.scale(Vector2(2, 3))
    assertVec2Equals(v, Vector2(6, 12))
  }

  test("Vector2 dot product") {
    val v = Vector2(1, 2)
    val w = Vector2(3, 4)
    assertEqualsFloat(v.dot(w), 11f, epsilon)
  }

  test("Vector2 cross product") {
    val v = Vector2(1, 0)
    val w = Vector2(0, 1)
    assertEqualsFloat(v.cross(w), 1f, epsilon)
  }

  test("Vector2 length") {
    val v = Vector2(3, 4)
    assertEqualsFloat(v.length, 5f, epsilon)
  }

  test("Vector2 lengthSq") {
    val v = Vector2(3, 4)
    assertEqualsFloat(v.lengthSq, 25f, epsilon)
  }

  test("Vector2 normalize") {
    val v = Vector2(3, 4)
    v.normalize()
    assertEqualsFloat(v.length, 1f, epsilon)
    assertEqualsFloat(v.x, 3f / 5f, epsilon)
    assertEqualsFloat(v.y, 4f / 5f, epsilon)
  }

  test("Vector2 normalize zero vector does nothing") {
    val v = Vector2(0, 0)
    v.normalize()
    assertVec2Equals(v, Vector2(0, 0))
  }

  test("Vector2 distance") {
    val v = Vector2(1, 1)
    val w = Vector2(4, 5)
    assertEqualsFloat(v.distance(w), 5f, epsilon)
  }

  test("Vector2 distanceSq") {
    val v = Vector2(1, 1)
    val w = Vector2(4, 5)
    assertEqualsFloat(v.distanceSq(w), 25f, epsilon)
  }

  test("Vector2 lerp at 0") {
    val v = Vector2(1, 2)
    val w = Vector2(5, 6)
    v.lerp(w, 0f)
    assertVec2Equals(v, Vector2(1, 2))
  }

  test("Vector2 lerp at 1") {
    val v = Vector2(1, 2)
    val w = Vector2(5, 6)
    v.lerp(w, 1f)
    assertVec2Equals(v, Vector2(5, 6))
  }

  test("Vector2 lerp at 0.5") {
    val v = Vector2(0, 0)
    val w = Vector2(10, 20)
    v.lerp(w, 0.5f)
    assertVec2Equals(v, Vector2(5, 10))
  }

  test("Vector2 limit") {
    val v = Vector2(30, 40) // length=50
    v.limit(10f)
    assertEqualsFloat(v.length, 10f, epsilon)
  }

  test("Vector2 limit does nothing if shorter") {
    val v = Vector2(3, 4) // length=5
    v.limit(10f)
    assertEqualsFloat(v.length, 5f, epsilon)
  }

  test("Vector2 clamp") {
    val v = Vector2(30, 40) // length=50
    v.clamp(0f, 10f)
    assertEqualsFloat(v.length, 10f, epsilon)
  }

  test("Vector2 clamp min") {
    val v = Vector2(0.3f, 0.4f) // length=0.5
    v.clamp(5f, 10f)
    assertEqualsFloat(v.length, 5f, epsilon)
  }

  test("Vector2 setLength") {
    val v = Vector2(3, 4)
    v.setLength(10f)
    assertEqualsFloat(v.length, 10f, epsilon)
  }

  test("Vector2 rotateDeg 90") {
    val v = Vector2(1, 0)
    v.rotateDeg(90f)
    assertVec2Equals(v, Vector2(0, 1), 1e-3f)
  }

  test("Vector2 rotateDeg 180") {
    val v = Vector2(1, 0)
    v.rotateDeg(180f)
    assertVec2Equals(v, Vector2(-1, 0), 1e-3f)
  }

  test("Vector2 rotate90 CCW") {
    val v = Vector2(1, 0)
    v.rotate90(1)
    assertVec2Equals(v, Vector2(0, 1))
  }

  test("Vector2 rotate90 CW") {
    val v = Vector2(1, 0)
    v.rotate90(-1)
    assertVec2Equals(v, Vector2(0, -1))
  }

  test("Vector2 angleDeg") {
    assertEqualsFloat(Vector2(1, 0).angleDeg(), 0f, epsilon)
    assertEqualsFloat(Vector2(0, 1).angleDeg(), 90f, epsilon)
    assertEqualsFloat(Vector2(-1, 0).angleDeg(), 180f, epsilon)
    assertEqualsFloat(Vector2(0, -1).angleDeg(), 270f, epsilon)
  }

  test("Vector2 isZero") {
    assert(Vector2(0, 0).isZero)
    assert(!Vector2(1, 0).isZero)
  }

  test("Vector2 isUnit") {
    assert(Vector2(1, 0).isUnit)
    assert(Vector2(0, 1).isUnit)
    assert(!Vector2(2, 0).isUnit)
  }

  test("Vector2 isOnLine") {
    given Epsilon = Epsilon(MathUtils.FLOAT_ROUNDING_ERROR)
    assert(Vector2(1, 2).isOnLine(Vector2(2, 4)))
    assert(Vector2(1, 2).isOnLine(Vector2(-1, -2)))
    assert(!Vector2(1, 2).isOnLine(Vector2(1, 3)))
  }

  test("Vector2 isPerpendicular") {
    given Epsilon = Epsilon(MathUtils.FLOAT_ROUNDING_ERROR)
    assert(Vector2(1, 0).isPerpendicular(Vector2(0, 1)))
    assert(!Vector2(1, 1).isPerpendicular(Vector2(1, 0)))
  }

  test("Vector2 epsilonEquals") {
    given Epsilon = Epsilon(0.01f)
    assert(Vector2(1f, 2f).epsilonEquals(Vector2(1.005f, 2.005f)))
    assert(!Vector2(1f, 2f).epsilonEquals(Vector2(1.02f, 2f)))
  }

  test("Vector2 mulAdd") {
    val v = Vector2(1, 2)
    v.mulAdd(Vector2(3, 4), 2f)
    assertVec2Equals(v, Vector2(7, 10))
  }

  test("Vector2 copy") {
    val v = Vector2(1, 2)
    val c = v.copy
    assertVec2Equals(v, c)
    c.x = 99
    assertEqualsFloat(v.x, 1f, epsilon)
  }

  // ---- Vector3 basic arithmetic ----

  test("Vector3 add") {
    val v = Vector3(1, 2, 3)
    v + Vector3(4, 5, 6)
    assertVec3Equals(v, Vector3(5, 7, 9))
  }

  test("Vector3 sub") {
    val v = Vector3(5, 7, 9)
    v - Vector3(1, 2, 3)
    assertVec3Equals(v, Vector3(4, 5, 6))
  }

  test("Vector3 scale by scalar") {
    val v = Vector3(1, 2, 3)
    v.scale(3f)
    assertVec3Equals(v, Vector3(3, 6, 9))
  }

  test("Vector3 dot product") {
    assertEqualsFloat(Vector3(1, 2, 3).dot(Vector3(4, 5, 6)), 32f, epsilon)
  }

  test("Vector3 cross product") {
    val v = Vector3(1, 0, 0)
    v.cross(Vector3(0, 1, 0))
    assertVec3Equals(v, Vector3(0, 0, 1))
  }

  test("Vector3 cross product anti-commutativity") {
    val v1 = Vector3(1, 2, 3)
    val v2 = Vector3(4, 5, 6)
    val c1 = v1.copy.cross(v2)
    val c2 = v2.copy.cross(v1)
    // c1 should equal -c2
    assertVec3Equals(c1, Vector3(-c2.x, -c2.y, -c2.z))
  }

  test("Vector3 length") {
    assertEqualsFloat(Vector3(1, 2, 2).length, 3f, epsilon)
  }

  test("Vector3 normalize") {
    val v = Vector3(0, 0, 5)
    v.normalize()
    assertEqualsFloat(v.length, 1f, epsilon)
    assertVec3Equals(v, Vector3(0, 0, 1), epsilon)
  }

  test("Vector3 normalize zero vector") {
    val v = Vector3(0, 0, 0)
    v.normalize()
    assertVec3Equals(v, Vector3(0, 0, 0))
  }

  test("Vector3 distance") {
    val v = Vector3(1, 2, 2)
    val w = Vector3(4, 6, 2)
    assertEqualsFloat(v.distance(w), 5f, epsilon)
  }

  test("Vector3 lerp at 0") {
    val v = Vector3(1, 2, 3)
    v.lerp(Vector3(5, 6, 7), 0f)
    assertVec3Equals(v, Vector3(1, 2, 3))
  }

  test("Vector3 lerp at 1") {
    val v = Vector3(1, 2, 3)
    v.lerp(Vector3(5, 6, 7), 1f)
    assertVec3Equals(v, Vector3(5, 6, 7))
  }

  test("Vector3 lerp at 0.5") {
    val v = Vector3(0, 0, 0)
    v.lerp(Vector3(10, 20, 30), 0.5f)
    assertVec3Equals(v, Vector3(5, 10, 15))
  }

  test("Vector3 limit") {
    val v = Vector3(0, 0, 50)
    v.limit(10f)
    assertEqualsFloat(v.length, 10f, epsilon)
  }

  test("Vector3 clamp") {
    val v = Vector3(0, 0, 50)
    v.clamp(0f, 10f)
    assertEqualsFloat(v.length, 10f, epsilon)
  }

  test("Vector3 isZero") {
    assert(Vector3(0, 0, 0).isZero)
    assert(!Vector3(0, 0, 1).isZero)
  }

  test("Vector3 isUnit") {
    assert(Vector3(1, 0, 0).isUnit)
    assert(!Vector3(1, 1, 0).isUnit)
  }

  test("Vector3 isOnLine") {
    given Epsilon = Epsilon(MathUtils.FLOAT_ROUNDING_ERROR)
    assert(Vector3(1, 2, 3).isOnLine(Vector3(2, 4, 6)))
    assert(!Vector3(1, 2, 3).isOnLine(Vector3(1, 2, 4)))
  }

  test("Vector3 hasSameDirection") {
    assert(Vector3(1, 0, 0).hasSameDirection(Vector3(2, 0, 0)))
    assert(!Vector3(1, 0, 0).hasSameDirection(Vector3(-1, 0, 0)))
  }

  test("Vector3 hasOppositeDirection") {
    assert(Vector3(1, 0, 0).hasOppositeDirection(Vector3(-1, 0, 0)))
  }

  test("Vector3 mulAdd") {
    val v = Vector3(1, 2, 3)
    v.mulAdd(Vector3(1, 1, 1), 5f)
    assertVec3Equals(v, Vector3(6, 7, 8))
  }

  // ---- Vector4 basic arithmetic ----

  test("Vector4 add") {
    val v = Vector4(1, 2, 3, 4)
    v + Vector4(5, 6, 7, 8)
    assert(Math.abs(v.x - 6) < epsilon)
    assert(Math.abs(v.y - 8) < epsilon)
    assert(Math.abs(v.z - 10) < epsilon)
    assert(Math.abs(v.w - 12) < epsilon)
  }

  test("Vector4 dot") {
    val v = Vector4(1, 2, 3, 4)
    assertEqualsFloat(v.dot(Vector4(5, 6, 7, 8)), 70f, epsilon)
  }

  test("Vector4 length") {
    val v = Vector4(1, 2, 2, 0)
    assertEqualsFloat(v.length, 3f, epsilon)
  }

  test("Vector4 normalize") {
    val v = Vector4(0, 0, 0, 5)
    v.normalize()
    assertEqualsFloat(v.length, 1f, epsilon)
  }

  test("Vector4 lerp") {
    val v = Vector4(0, 0, 0, 0)
    v.lerp(Vector4(10, 20, 30, 40), 0.5f)
    assert(Math.abs(v.x - 5) < epsilon)
    assert(Math.abs(v.y - 10) < epsilon)
  }

  // ---- Property-based tests ----

  property("Vector2: v + w - w ≈ v") {
    forAll(smallFloat, smallFloat, smallFloat, smallFloat) { (x1: Float, y1: Float, x2: Float, y2: Float) =>
      val v        = Vector2(x1, y1)
      val w        = Vector2(x2, y2)
      val original = v.copy
      v + w
      v - w
      Math.abs(v.x - original.x) < 0.01f && Math.abs(v.y - original.y) < 0.01f
    }
  }

  property("Vector2: normalize produces unit length (non-zero vectors)") {
    forAll(nonZeroFloat, nonZeroFloat) { (x: Float, y: Float) =>
      val v = Vector2(x, y)
      v.normalize()
      Math.abs(v.length - 1f) < 0.001f
    }
  }

  property("Vector2: v.dot(v) ≈ v.lengthSq") {
    forAll(smallFloat, smallFloat) { (x: Float, y: Float) =>
      val v = Vector2(x, y)
      Math.abs(v.dot(v) - v.lengthSq) < 0.01f
    }
  }

  property("Vector3: v + w - w ≈ v") {
    forAll(smallFloat, smallFloat, smallFloat, smallFloat, smallFloat, smallFloat) { (x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) =>
      val v        = Vector3(x1, y1, z1)
      val original = v.copy
      v + Vector3(x2, y2, z2)
      v - Vector3(x2, y2, z2)
      Math.abs(v.x - original.x) < 0.01f && Math.abs(v.y - original.y) < 0.01f && Math.abs(v.z - original.z) < 0.01f
    }
  }

  property("Vector3: normalize produces unit length (non-zero vectors)") {
    forAll(nonZeroFloat, nonZeroFloat, nonZeroFloat) { (x: Float, y: Float, z: Float) =>
      val v = Vector3(x, y, z)
      v.normalize()
      Math.abs(v.length - 1f) < 0.001f
    }
  }

  property("Vector3: v.dot(v) ≈ v.lengthSq") {
    forAll(smallFloat, smallFloat, smallFloat) { (x: Float, y: Float, z: Float) =>
      val v = Vector3(x, y, z)
      Math.abs(v.dot(v) - v.lengthSq) < 0.01f
    }
  }
}
