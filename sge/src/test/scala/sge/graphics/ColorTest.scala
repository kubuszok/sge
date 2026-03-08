/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics

class ColorTest extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertColorEquals(a: Color, b: Color, eps: Float = epsilon): Unit = {
    assertEqualsFloat(a.r, b.r, eps)
    assertEqualsFloat(a.g, b.g, eps)
    assertEqualsFloat(a.b, b.b, eps)
    assertEqualsFloat(a.a, b.a, eps)
  }

  test("constructor clamps") {
    val c = new Color(1.5f, -0.5f, 0.5f, 2f)
    assertEqualsFloat(c.r, 1f, epsilon)
    assertEqualsFloat(c.g, 0f, epsilon)
    assertEqualsFloat(c.b, 0.5f, epsilon)
    assertEqualsFloat(c.a, 1f, epsilon)
  }

  test("set from color") {
    val src = new Color(0.1f, 0.2f, 0.3f, 0.4f)
    val dst = new Color()
    dst.set(src)
    assertColorEquals(dst, src)
  }

  test("set from rgba components") {
    val c = new Color()
    c.set(0.5f, 0.6f, 0.7f, 0.8f)
    assertEqualsFloat(c.r, 0.5f, epsilon)
    assertEqualsFloat(c.g, 0.6f, epsilon)
    assertEqualsFloat(c.b, 0.7f, epsilon)
    assertEqualsFloat(c.a, 0.8f, epsilon)
  }

  test("add colors") {
    val c = new Color(0.2f, 0.3f, 0.4f, 0.5f)
    c.add(new Color(0.1f, 0.1f, 0.1f, 0.1f))
    assertEqualsFloat(c.r, 0.3f, epsilon)
    assertEqualsFloat(c.g, 0.4f, epsilon)
    assertEqualsFloat(c.b, 0.5f, epsilon)
    assertEqualsFloat(c.a, 0.6f, epsilon)
  }

  test("add clamps to 1") {
    val c = new Color(0.9f, 0.9f, 0.9f, 0.9f)
    c.add(new Color(0.5f, 0.5f, 0.5f, 0.5f))
    assertEqualsFloat(c.r, 1f, epsilon)
    assertEqualsFloat(c.g, 1f, epsilon)
    assertEqualsFloat(c.b, 1f, epsilon)
    assertEqualsFloat(c.a, 1f, epsilon)
  }

  test("sub colors") {
    val c = new Color(0.5f, 0.6f, 0.7f, 0.8f)
    c.sub(new Color(0.1f, 0.1f, 0.1f, 0.1f))
    assertEqualsFloat(c.r, 0.4f, epsilon)
    assertEqualsFloat(c.g, 0.5f, epsilon)
    assertEqualsFloat(c.b, 0.6f, epsilon)
    assertEqualsFloat(c.a, 0.7f, epsilon)
  }

  test("sub clamps to 0") {
    val c = new Color(0.1f, 0.1f, 0.1f, 0.1f)
    c.sub(new Color(0.5f, 0.5f, 0.5f, 0.5f))
    assertEqualsFloat(c.r, 0f, epsilon)
    assertEqualsFloat(c.g, 0f, epsilon)
    assertEqualsFloat(c.b, 0f, epsilon)
    assertEqualsFloat(c.a, 0f, epsilon)
  }

  test("mul by color") {
    val c = new Color(0.5f, 0.5f, 0.5f, 1f)
    c.mul(new Color(0.5f, 0.5f, 0.5f, 1f))
    assertEqualsFloat(c.r, 0.25f, epsilon)
    assertEqualsFloat(c.g, 0.25f, epsilon)
    assertEqualsFloat(c.b, 0.25f, epsilon)
    assertEqualsFloat(c.a, 1f, epsilon)
  }

  test("mul by scalar") {
    val c = new Color(0.5f, 0.5f, 0.5f, 0.5f)
    c.mul(0.5f)
    assertEqualsFloat(c.r, 0.25f, epsilon)
    assertEqualsFloat(c.g, 0.25f, epsilon)
    assertEqualsFloat(c.b, 0.25f, epsilon)
    assertEqualsFloat(c.a, 0.25f, epsilon)
  }

  test("lerp at 0") {
    val c = new Color(0f, 0f, 0f, 1f)
    c.lerp(new Color(1f, 1f, 1f, 1f), 0f)
    assertEqualsFloat(c.r, 0f, epsilon)
    assertEqualsFloat(c.g, 0f, epsilon)
    assertEqualsFloat(c.b, 0f, epsilon)
  }

  test("lerp at 1") {
    val c = new Color(0f, 0f, 0f, 0f)
    c.lerp(new Color(1f, 1f, 1f, 1f), 1f)
    assertEqualsFloat(c.r, 1f, epsilon)
    assertEqualsFloat(c.g, 1f, epsilon)
    assertEqualsFloat(c.b, 1f, epsilon)
    assertEqualsFloat(c.a, 1f, epsilon)
  }

  test("lerp at 0.5") {
    val c = new Color(0f, 0f, 0f, 0f)
    c.lerp(new Color(1f, 1f, 1f, 1f), 0.5f)
    assertEqualsFloat(c.r, 0.5f, epsilon)
    assertEqualsFloat(c.g, 0.5f, epsilon)
    assertEqualsFloat(c.b, 0.5f, epsilon)
    assertEqualsFloat(c.a, 0.5f, epsilon)
  }

  test("premultiplyAlpha") {
    val c = new Color(1f, 0.5f, 0.25f, 0.5f)
    c.premultiplyAlpha()
    assertEqualsFloat(c.r, 0.5f, epsilon)
    assertEqualsFloat(c.g, 0.25f, epsilon)
    assertEqualsFloat(c.b, 0.125f, epsilon)
    assertEqualsFloat(c.a, 0.5f, epsilon) // alpha unchanged
  }

  test("toIntBits and rgba8888ToColor roundtrip") {
    val c    = new Color(1f, 0f, 0f, 1f) // red
    val bits = c.toIntBits()
    val c2   = new Color()
    Color.rgba8888ToColor(c2, bits)
    assertColorEquals(c, c2, 0.01f)
  }

  test("toIntBits roundtrip preserves approximate values") {
    val c       = new Color(1f, 0f, 0f, 1f)
    val intBits = c.toIntBits()
    val fromInt = new Color()
    Color.rgba8888ToColor(fromInt, intBits)
    assertColorEquals(c, fromInt, 0.01f)
  }

  test("valueOf hex string") {
    val c = Color.valueOf("FF0000FF") // red, full alpha
    assertEqualsFloat(c.r, 1f, 0.01f)
    assertEqualsFloat(c.g, 0f, 0.01f)
    assertEqualsFloat(c.b, 0f, 0.01f)
    assertEqualsFloat(c.a, 1f, 0.01f)
  }

  test("valueOf hex string green") {
    val c = Color.valueOf("00FF00FF")
    assertEqualsFloat(c.r, 0f, 0.01f)
    assertEqualsFloat(c.g, 1f, 0.01f)
    assertEqualsFloat(c.b, 0f, 0.01f)
    assertEqualsFloat(c.a, 1f, 0.01f)
  }

  test("valueOf hex string with hash prefix") {
    val c = Color.valueOf("#0000FFFF") // blue
    assertEqualsFloat(c.r, 0f, 0.01f)
    assertEqualsFloat(c.g, 0f, 0.01f)
    assertEqualsFloat(c.b, 1f, 0.01f)
    assertEqualsFloat(c.a, 1f, 0.01f)
  }

  test("equals based on toIntBits") {
    val c1 = new Color(1f, 0f, 0f, 1f)
    val c2 = new Color(1f, 0f, 0f, 1f)
    assertEquals(c1, c2)
  }

  test("not equals for different colors") {
    val c1 = new Color(1f, 0f, 0f, 1f)
    val c2 = new Color(0f, 1f, 0f, 1f)
    assert(c1 != c2)
  }

  test("clamp") {
    val c = new Color()
    c.r = -1f
    c.g = 2f
    c.b = 0.5f
    c.a = 1.5f
    c.clamp()
    assertEqualsFloat(c.r, 0f, epsilon)
    assertEqualsFloat(c.g, 1f, epsilon)
    assertEqualsFloat(c.b, 0.5f, epsilon)
    assertEqualsFloat(c.a, 1f, epsilon)
  }

  test("add component-wise") {
    val c = new Color(0.1f, 0.2f, 0.3f, 0.4f)
    c.add(0.1f, 0.1f, 0.1f, 0.1f)
    assertEqualsFloat(c.r, 0.2f, epsilon)
    assertEqualsFloat(c.g, 0.3f, epsilon)
    assertEqualsFloat(c.b, 0.4f, epsilon)
    assertEqualsFloat(c.a, 0.5f, epsilon)
  }

  test("sub component-wise") {
    val c = new Color(0.5f, 0.5f, 0.5f, 0.5f)
    c.sub(0.1f, 0.2f, 0.3f, 0.4f)
    assertEqualsFloat(c.r, 0.4f, epsilon)
    assertEqualsFloat(c.g, 0.3f, epsilon)
    assertEqualsFloat(c.b, 0.2f, epsilon)
    assertEqualsFloat(c.a, 0.1f, epsilon)
  }

  test("mul component-wise") {
    val c = new Color(0.5f, 0.5f, 0.5f, 0.5f)
    c.mul(0.5f, 1f, 0.5f, 1f)
    assertEqualsFloat(c.r, 0.25f, epsilon)
    assertEqualsFloat(c.g, 0.5f, epsilon)
    assertEqualsFloat(c.b, 0.25f, epsilon)
    assertEqualsFloat(c.a, 0.5f, epsilon)
  }

  test("lerp component-wise") {
    val c = new Color(0f, 0f, 0f, 0f)
    c.lerp(1f, 1f, 1f, 1f, 0.25f)
    assertEqualsFloat(c.r, 0.25f, epsilon)
    assertEqualsFloat(c.g, 0.25f, epsilon)
    assertEqualsFloat(c.b, 0.25f, epsilon)
    assertEqualsFloat(c.a, 0.25f, epsilon)
  }
}
