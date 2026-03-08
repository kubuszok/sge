/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package math

class InterpolationTest extends munit.FunSuite {

  private val epsilon = 1e-4f

  // All non-oscillating interpolations should satisfy f(0)=0 and f(1)=1
  private val boundaryInterpolations: List[(String, Interpolation)] = List(
    "linear" -> Interpolation.linear,
    "smooth" -> Interpolation.smooth,
    "smooth2" -> Interpolation.smooth2,
    "smoother" -> Interpolation.smoother,
    "fade" -> Interpolation.fade,
    "pow2" -> Interpolation.pow2,
    "pow2In" -> Interpolation.pow2In,
    "pow2Out" -> Interpolation.pow2Out,
    "pow3" -> Interpolation.pow3,
    "pow3In" -> Interpolation.pow3In,
    "pow3Out" -> Interpolation.pow3Out,
    "pow4" -> Interpolation.pow4,
    "pow4In" -> Interpolation.pow4In,
    "pow4Out" -> Interpolation.pow4Out,
    "pow5" -> Interpolation.pow5,
    "pow5In" -> Interpolation.pow5In,
    "pow5Out" -> Interpolation.pow5Out,
    "sine" -> Interpolation.sine,
    "sineIn" -> Interpolation.sineIn,
    "sineOut" -> Interpolation.sineOut,
    "exp10" -> Interpolation.exp10,
    "exp10In" -> Interpolation.exp10In,
    "exp10Out" -> Interpolation.exp10Out,
    "exp5" -> Interpolation.exp5,
    "exp5In" -> Interpolation.exp5In,
    "exp5Out" -> Interpolation.exp5Out,
    "circle" -> Interpolation.circle,
    "circleIn" -> Interpolation.circleIn,
    "circleOut" -> Interpolation.circleOut,
    "swing" -> Interpolation.swing,
    "swingIn" -> Interpolation.swingIn,
    "swingOut" -> Interpolation.swingOut,
    "bounce" -> Interpolation.bounce,
    "bounceIn" -> Interpolation.bounceIn,
    "bounceOut" -> Interpolation.bounceOut,
    "elastic" -> Interpolation.elastic,
    "elasticIn" -> Interpolation.elasticIn,
    "elasticOut" -> Interpolation.elasticOut
  )

  for ((name, interp) <- boundaryInterpolations) {
    test(s"$name: f(0) ≈ 0") {
      assertEqualsFloat(interp.apply(0f), 0f, 0.01f)
    }

    test(s"$name: f(1) ≈ 1") {
      assertEqualsFloat(interp.apply(1f), 1f, 0.01f)
    }
  }

  // Monotonic interpolations: f(0.25) <= f(0.5) <= f(0.75)
  private val monotonicInterpolations: List[(String, Interpolation)] = List(
    "linear" -> Interpolation.linear,
    "smooth" -> Interpolation.smooth,
    "smooth2" -> Interpolation.smooth2,
    "smoother" -> Interpolation.smoother,
    "pow2In" -> Interpolation.pow2In,
    "pow2Out" -> Interpolation.pow2Out,
    "pow3In" -> Interpolation.pow3In,
    "pow3Out" -> Interpolation.pow3Out,
    "sineIn" -> Interpolation.sineIn,
    "sineOut" -> Interpolation.sineOut,
    "sine" -> Interpolation.sine,
    "exp10In" -> Interpolation.exp10In,
    "exp10Out" -> Interpolation.exp10Out,
    "exp5In" -> Interpolation.exp5In,
    "exp5Out" -> Interpolation.exp5Out,
    "circleIn" -> Interpolation.circleIn,
    "circleOut" -> Interpolation.circleOut
  )

  for ((name, interp) <- monotonicInterpolations)
    test(s"$name: monotonic f(0.25) ≤ f(0.5) ≤ f(0.75)") {
      val f25 = interp.apply(0.25f)
      val f50 = interp.apply(0.5f)
      val f75 = interp.apply(0.75f)
      assert(f25 <= f50 + epsilon, s"$name: f(0.25)=$f25 > f(0.5)=$f50")
      assert(f50 <= f75 + epsilon, s"$name: f(0.5)=$f50 > f(0.75)=$f75")
    }

  test("linear is identity") {
    assertEqualsFloat(Interpolation.linear(0.5f), 0.5f, epsilon)
    assertEqualsFloat(Interpolation.linear(0.25f), 0.25f, epsilon)
  }

  test("apply(start, end, alpha) works") {
    assertEqualsFloat(Interpolation.linear.apply(10f, 20f, 0.5f), 15f, epsilon)
    assertEqualsFloat(Interpolation.linear.apply(0f, 100f, 0.25f), 25f, epsilon)
  }

  test("pow2InInverse(pow2In(x)) ≈ x") {
    for (x <- List(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
      val y         = Interpolation.pow2In(x)
      val roundtrip = Interpolation.pow2InInverse(y)
      assertEqualsFloat(roundtrip, x, 0.01f)
    }
  }

  test("pow2OutInverse(pow2Out(x)) ≈ x") {
    for (x <- List(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
      val y         = Interpolation.pow2Out(x)
      val roundtrip = Interpolation.pow2OutInverse(y)
      assertEqualsFloat(roundtrip, x, 0.01f)
    }
  }

  test("pow3InInverse(pow3In(x)) ≈ x") {
    for (x <- List(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)) {
      val y         = Interpolation.pow3In(x)
      val roundtrip = Interpolation.pow3InInverse(y)
      assertEqualsFloat(roundtrip, x, 0.01f)
    }
  }

  test("slowFast is same as pow2In") {
    for (x <- List(0f, 0.25f, 0.5f, 0.75f, 1f))
      assertEqualsFloat(Interpolation.slowFast(x), Interpolation.pow2In(x), epsilon)
  }

  test("fastSlow is same as pow2Out") {
    for (x <- List(0f, 0.25f, 0.5f, 0.75f, 1f))
      assertEqualsFloat(Interpolation.fastSlow(x), Interpolation.pow2Out(x), epsilon)
  }

  test("fade is same as smoother") {
    for (x <- List(0f, 0.25f, 0.5f, 0.75f, 1f))
      assertEqualsFloat(Interpolation.fade(x), Interpolation.smoother(x), epsilon)
  }
}
