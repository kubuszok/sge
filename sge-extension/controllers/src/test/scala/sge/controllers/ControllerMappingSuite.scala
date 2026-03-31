/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

class ControllerMappingSuite extends munit.FunSuite {

  test("StandardMapping has valid axis indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.axisLeftX >= 0, "axisLeftX must be non-negative")
    assert(m.axisLeftY >= 0, "axisLeftY must be non-negative")
    assert(m.axisRightX >= 0, "axisRightX must be non-negative")
    assert(m.axisRightY >= 0, "axisRightY must be non-negative")
  }

  test("StandardMapping has valid face button indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.buttonA >= 0, "buttonA must be non-negative")
    assert(m.buttonB >= 0, "buttonB must be non-negative")
    assert(m.buttonX >= 0, "buttonX must be non-negative")
    assert(m.buttonY >= 0, "buttonY must be non-negative")
  }

  test("StandardMapping has valid shoulder button indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.buttonL1 >= 0, "buttonL1 must be non-negative")
    assert(m.buttonR1 >= 0, "buttonR1 must be non-negative")
  }

  test("StandardMapping has valid dpad button indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.buttonDpadUp >= 0, "buttonDpadUp must be non-negative")
    assert(m.buttonDpadDown >= 0, "buttonDpadDown must be non-negative")
    assert(m.buttonDpadLeft >= 0, "buttonDpadLeft must be non-negative")
    assert(m.buttonDpadRight >= 0, "buttonDpadRight must be non-negative")
  }

  test("StandardMapping has valid stick button indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.buttonLeftStick >= 0, "buttonLeftStick must be non-negative")
    assert(m.buttonRightStick >= 0, "buttonRightStick must be non-negative")
  }

  test("StandardMapping has valid start/back indices") {
    val m = ControllerMapping.StandardMapping
    assert(m.buttonBack >= 0, "buttonBack must be non-negative")
    assert(m.buttonStart >= 0, "buttonStart must be non-negative")
  }

  test("Undefined sentinel is negative") {
    assert(ControllerMapping.Undefined < 0, "Undefined must be negative")
  }

  test("StandardMapping trigger buttons use Undefined when not mapped as buttons") {
    val m = ControllerMapping.StandardMapping
    // L2/R2 are mapped as axes on the standard GLFW gamepad layout, not as buttons
    assertEquals(m.buttonL2, ControllerMapping.Undefined)
    assertEquals(m.buttonR2, ControllerMapping.Undefined)
  }

  test("StandardMapping all face buttons have distinct indices") {
    val m = ControllerMapping.StandardMapping
    val faceButtons = Set(m.buttonA, m.buttonB, m.buttonX, m.buttonY)
    assertEquals(faceButtons.size, 4, "All face buttons must have distinct indices")
  }

  test("StandardMapping all axes have distinct indices") {
    val m = ControllerMapping.StandardMapping
    val axes = Set(m.axisLeftX, m.axisLeftY, m.axisRightX, m.axisRightY)
    assertEquals(axes.size, 4, "All axes must have distinct indices")
  }
}
