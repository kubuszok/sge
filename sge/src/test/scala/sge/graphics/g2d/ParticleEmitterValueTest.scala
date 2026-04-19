/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package g2d

class ParticleEmitterValueTest extends munit.FunSuite {

  private val epsilon = 1e-4f

  // --- ParticleValue ---

  test("ParticleValue isActive false by default") {
    val pv = ParticleEmitter.ParticleValue()
    assert(!pv.isActive)
  }

  test("ParticleValue isActive true when active set") {
    val pv = ParticleEmitter.ParticleValue()
    pv.active = true
    assert(pv.isActive)
  }

  test("ParticleValue isActive true when alwaysActive") {
    val pv = ParticleEmitter.ParticleValue()
    pv.alwaysActive = true
    assert(pv.isActive)
  }

  test("ParticleValue isActive true when alwaysActive even if active is false") {
    val pv = ParticleEmitter.ParticleValue()
    pv.alwaysActive = true
    pv.active = false
    assert(pv.isActive)
  }

  test("ParticleValue load copies active and alwaysActive") {
    val src = ParticleEmitter.ParticleValue()
    src.active = true
    src.alwaysActive = true
    val dst = ParticleEmitter.ParticleValue()
    dst.load(src)
    assert(dst.active)
    assert(dst.alwaysActive)
  }

  // --- NumericValue ---

  test("NumericValue default value is 0") {
    val nv = ParticleEmitter.NumericValue()
    assertEqualsFloat(nv.value, 0f, epsilon)
  }

  test("NumericValue load copies value") {
    val src = ParticleEmitter.NumericValue()
    src.value = 42f
    val dst = ParticleEmitter.NumericValue()
    dst.load(src)
    assertEqualsFloat(dst.value, 42f, epsilon)
  }

  // --- RangedNumericValue ---

  test("RangedNumericValue setLow with single value") {
    val rn = ParticleEmitter.RangedNumericValue()
    rn.setLow(5f)
    assertEqualsFloat(rn.lowMin, 5f, epsilon)
    assertEqualsFloat(rn.lowMax, 5f, epsilon)
  }

  test("RangedNumericValue setLow with min and max") {
    val rn = ParticleEmitter.RangedNumericValue()
    rn.setLow(2f, 8f)
    assertEqualsFloat(rn.lowMin, 2f, epsilon)
    assertEqualsFloat(rn.lowMax, 8f, epsilon)
  }

  test("RangedNumericValue scale multiplies both") {
    val rn = ParticleEmitter.RangedNumericValue()
    rn.setLow(2f, 4f)
    rn.scale(3f)
    assertEqualsFloat(rn.lowMin, 6f, epsilon)
    assertEqualsFloat(rn.lowMax, 12f, epsilon)
  }

  test("RangedNumericValue newLowValue in range") {
    val rn = ParticleEmitter.RangedNumericValue()
    rn.setLow(10f, 20f)
    for (_ <- 0 until 100) {
      val v = rn.newLowValue()
      assert(v >= 10f && v <= 20f, s"Expected 10..20, got $v")
    }
  }

  test("RangedNumericValue newLowValue equals value when min equals max") {
    val rn = ParticleEmitter.RangedNumericValue()
    rn.setLow(5f)
    assertEqualsFloat(rn.newLowValue(), 5f, epsilon)
  }

  test("RangedNumericValue set copies from another") {
    val src = ParticleEmitter.RangedNumericValue()
    src.setLow(3f, 7f)
    val dst = ParticleEmitter.RangedNumericValue()
    dst.set(src)
    assertEqualsFloat(dst.lowMin, 3f, epsilon)
    assertEqualsFloat(dst.lowMax, 7f, epsilon)
  }

  // --- ScaledNumericValue ---

  test("ScaledNumericValue setHigh with single value") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.setHigh(10f)
    assertEqualsFloat(sv.highMin, 10f, epsilon)
    assertEqualsFloat(sv.highMax, 10f, epsilon)
  }

  test("ScaledNumericValue setHigh with min and max") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.setHigh(5f, 15f)
    assertEqualsFloat(sv.highMin, 5f, epsilon)
    assertEqualsFloat(sv.highMax, 15f, epsilon)
  }

  test("ScaledNumericValue scale multiplies low and high") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.setLow(1f, 2f)
    sv.setHigh(3f, 4f)
    sv.scale(2f)
    assertEqualsFloat(sv.lowMin, 2f, epsilon)
    assertEqualsFloat(sv.lowMax, 4f, epsilon)
    assertEqualsFloat(sv.highMin, 6f, epsilon)
    assertEqualsFloat(sv.highMax, 8f, epsilon)
  }

  test("ScaledNumericValue getScale with single scaling returns that value") {
    val sv = ParticleEmitter.ScaledNumericValue()
    // default: scaling = Array(1f), timeline = Array(0f)
    assertEqualsFloat(sv.getScale(0f), 1f, epsilon)
    assertEqualsFloat(sv.getScale(0.5f), 1f, epsilon)
    assertEqualsFloat(sv.getScale(1f), 1f, epsilon)
  }

  test("ScaledNumericValue getScale interpolates between keyframes") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.scaling = Array(0f, 1f)
    sv.timeline = Array(0f, 1f)
    assertEqualsFloat(sv.getScale(0f), 0f, epsilon)
    assertEqualsFloat(sv.getScale(0.5f), 0.5f, epsilon)
    assertEqualsFloat(sv.getScale(1f), 1f, epsilon)
  }

  test("ScaledNumericValue getScale with three keyframes") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.scaling = Array(0f, 1f, 0f)
    sv.timeline = Array(0f, 0.5f, 1f)
    assertEqualsFloat(sv.getScale(0f), 0f, epsilon)
    assertEqualsFloat(sv.getScale(0.25f), 0.5f, epsilon)
    assertEqualsFloat(sv.getScale(0.5f), 1f, epsilon)
    assertEqualsFloat(sv.getScale(0.75f), 0.5f, epsilon)
    assertEqualsFloat(sv.getScale(1f), 0f, epsilon)
  }

  test("ScaledNumericValue getScale returns last value past end") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.scaling = Array(0f, 1f)
    sv.timeline = Array(0f, 0.5f)
    // percent=0.8 is past the last timeline entry
    assertEqualsFloat(sv.getScale(0.8f), 1f, epsilon)
  }

  test("ScaledNumericValue set copies from ScaledNumericValue") {
    val src = ParticleEmitter.ScaledNumericValue()
    src.setLow(1f, 2f)
    src.setHigh(3f, 4f)
    src.scaling = Array(0f, 0.5f, 1f)
    src.timeline = Array(0f, 0.5f, 1f)
    src.relative = true

    val dst = ParticleEmitter.ScaledNumericValue()
    dst.set(src)
    assertEqualsFloat(dst.lowMin, 1f, epsilon)
    assertEqualsFloat(dst.lowMax, 2f, epsilon)
    assertEqualsFloat(dst.highMin, 3f, epsilon)
    assertEqualsFloat(dst.highMax, 4f, epsilon)
    assertEquals(dst.scaling.toSeq, Seq(0f, 0.5f, 1f))
    assertEquals(dst.timeline.toSeq, Seq(0f, 0.5f, 1f))
    assert(dst.relative)
  }

  test("ScaledNumericValue set from RangedNumericValue only copies low") {
    val src = ParticleEmitter.RangedNumericValue()
    src.setLow(10f, 20f)

    val dst = ParticleEmitter.ScaledNumericValue()
    dst.setHigh(99f)
    dst.set(src)
    assertEqualsFloat(dst.lowMin, 10f, epsilon)
    assertEqualsFloat(dst.lowMax, 20f, epsilon)
    // high values should remain unchanged
    assertEqualsFloat(dst.highMin, 99f, epsilon)
  }

  test("ScaledNumericValue newHighValue in range") {
    val sv = ParticleEmitter.ScaledNumericValue()
    sv.setHigh(10f, 20f)
    for (_ <- 0 until 100) {
      val v = sv.newHighValue()
      assert(v >= 10f && v <= 20f, s"Expected 10..20, got $v")
    }
  }

  // --- GradientColorValue ---

  test("GradientColorValue default is white at timeline 0") {
    val gv     = ParticleEmitter.GradientColorValue()
    val result = gv.getColor(0f)
    assertEqualsFloat(result(0), 1f, epsilon)
    assertEqualsFloat(result(1), 1f, epsilon)
    assertEqualsFloat(result(2), 1f, epsilon)
  }

  test("GradientColorValue with single color returns that color at any percent") {
    val gv = ParticleEmitter.GradientColorValue()
    gv.colors = Array(0.5f, 0.25f, 0.75f)
    gv.timeline = Array(0f)
    val result = gv.getColor(0.5f)
    assertEqualsFloat(result(0), 0.5f, epsilon)
    assertEqualsFloat(result(1), 0.25f, epsilon)
    assertEqualsFloat(result(2), 0.75f, epsilon)
  }

  test("GradientColorValue interpolates between two colors") {
    val gv = ParticleEmitter.GradientColorValue()
    gv.colors = Array(0f, 0f, 0f, 1f, 1f, 1f)
    gv.timeline = Array(0f, 1f)

    val mid = gv.getColor(0.5f)
    assertEqualsFloat(mid(0), 0.5f, epsilon)
    assertEqualsFloat(mid(1), 0.5f, epsilon)
    assertEqualsFloat(mid(2), 0.5f, epsilon)

    val start = gv.getColor(0f)
    assertEqualsFloat(start(0), 0f, epsilon)

    val end = gv.getColor(1f)
    assertEqualsFloat(end(0), 1f, epsilon)
  }

  test("GradientColorValue interpolates with three stops") {
    val gv = ParticleEmitter.GradientColorValue()
    // red → green → blue
    gv.colors = Array(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    gv.timeline = Array(0f, 0.5f, 1f)

    val atQuarter = gv.getColor(0.25f)
    // Halfway between red (1,0,0) and green (0,1,0)
    assertEqualsFloat(atQuarter(0), 0.5f, epsilon)
    assertEqualsFloat(atQuarter(1), 0.5f, epsilon)
    assertEqualsFloat(atQuarter(2), 0f, epsilon)

    val atThreeQuarters = gv.getColor(0.75f)
    // Halfway between green (0,1,0) and blue (0,0,1)
    assertEqualsFloat(atThreeQuarters(0), 0f, epsilon)
    assertEqualsFloat(atThreeQuarters(1), 0.5f, epsilon)
    assertEqualsFloat(atThreeQuarters(2), 0.5f, epsilon)
  }

  test("GradientColorValue is alwaysActive by default") {
    val gv = ParticleEmitter.GradientColorValue()
    assert(gv.isActive)
    assert(gv.alwaysActive)
  }

  // --- IndependentScaledNumericValue ---

  test("IndependentScaledNumericValue default independent is false") {
    val iv = ParticleEmitter.IndependentScaledNumericValue()
    assert(!iv.independent)
  }

  test("IndependentScaledNumericValue set copies independent flag") {
    val src = ParticleEmitter.IndependentScaledNumericValue()
    src.independent = true
    src.setLow(1f, 2f)
    src.setHigh(3f, 4f)

    val dst = ParticleEmitter.IndependentScaledNumericValue()
    dst.set(src)
    assert(dst.independent)
    assertEqualsFloat(dst.lowMin, 1f, epsilon)
    assertEqualsFloat(dst.highMin, 3f, epsilon)
  }
}
