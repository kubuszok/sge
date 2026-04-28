/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Original source: gdx-controllers-android/src/com/badlogic/gdx/controllers/android/AndroidControllerMapping.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses Android KeyEvent keycodes for button mapping
 *   Idiom: singleton via lazy val instead of mutable static + null check
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 69
 * Covenant-baseline-methods: AndroidControllerMapping,KEYCODE_BACK,KEYCODE_BUTTON_A,KEYCODE_BUTTON_B,KEYCODE_BUTTON_L1,KEYCODE_BUTTON_L2,KEYCODE_BUTTON_R1,KEYCODE_BUTTON_R2,KEYCODE_BUTTON_START,KEYCODE_BUTTON_THUMBL,KEYCODE_BUTTON_THUMBR,KEYCODE_BUTTON_X,KEYCODE_BUTTON_Y,KEYCODE_DPAD_DOWN,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_RIGHT,KEYCODE_DPAD_UP,instance
 * Covenant-source-reference: com/badlogic/gdx/controllers/android/AndroidControllerMapping.java
 *   Convention: uses Android KeyEvent keycodes for button mapping
 *   Idiom: singleton via lazy val instead of mutable static + null check
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 69
 * Covenant-baseline-methods: AndroidControllerMapping,KEYCODE_BACK,KEYCODE_BUTTON_A,KEYCODE_BUTTON_B,KEYCODE_BUTTON_L1,KEYCODE_BUTTON_L2,KEYCODE_BUTTON_R1,KEYCODE_BUTTON_R2,KEYCODE_BUTTON_START,KEYCODE_BUTTON_THUMBL,KEYCODE_BUTTON_THUMBR,KEYCODE_BUTTON_X,KEYCODE_BUTTON_Y,KEYCODE_DPAD_DOWN,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_RIGHT,KEYCODE_DPAD_UP,instance
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 124b68125c7ef9c552085865379f77e8bee2ae3b
 */
package sge
package controllers

/** Android-specific controller mapping using [[android.view.KeyEvent]] keycodes for button indices and standard axis indices 0-3 for sticks.
  *
  * The axis layout follows the Android convention where axes are re-ordered so that:
  *   - Index 0: Left stick X (AXIS_X)
  *   - Index 1: Left stick Y (AXIS_Y)
  *   - Index 2: Right stick X (AXIS_Z)
  *   - Index 3: Right stick Y (AXIS_RZ)
  *
  * Button codes use Android KeyEvent constants (e.g. KEYCODE_BUTTON_A = 96).
  */
object AndroidControllerMapping {

  // Android KeyEvent button keycodes
  // These match android.view.KeyEvent constants
  private val KEYCODE_BUTTON_A:      Int = 96
  private val KEYCODE_BUTTON_B:      Int = 97
  private val KEYCODE_BUTTON_X:      Int = 99
  private val KEYCODE_BUTTON_Y:      Int = 100
  private val KEYCODE_BACK:          Int = 4
  private val KEYCODE_BUTTON_START:  Int = 108
  private val KEYCODE_BUTTON_L1:     Int = 102
  private val KEYCODE_BUTTON_L2:     Int = 104
  private val KEYCODE_BUTTON_R1:     Int = 103
  private val KEYCODE_BUTTON_R2:     Int = 105
  private val KEYCODE_BUTTON_THUMBL: Int = 106
  private val KEYCODE_BUTTON_THUMBR: Int = 107
  private val KEYCODE_DPAD_UP:       Int = 19
  private val KEYCODE_DPAD_DOWN:     Int = 20
  private val KEYCODE_DPAD_LEFT:     Int = 21
  private val KEYCODE_DPAD_RIGHT:    Int = 22

  /** The singleton Android controller mapping instance. */
  val instance: ControllerMapping = ControllerMapping(
    axisLeftX = 0,
    axisLeftY = 1,
    axisRightX = 2,
    axisRightY = 3,
    buttonA = KEYCODE_BUTTON_A,
    buttonB = KEYCODE_BUTTON_B,
    buttonX = KEYCODE_BUTTON_X,
    buttonY = KEYCODE_BUTTON_Y,
    buttonBack = KEYCODE_BACK,
    buttonStart = KEYCODE_BUTTON_START,
    buttonL1 = KEYCODE_BUTTON_L1,
    buttonL2 = KEYCODE_BUTTON_L2,
    buttonR1 = KEYCODE_BUTTON_R1,
    buttonR2 = KEYCODE_BUTTON_R2,
    buttonDpadUp = KEYCODE_DPAD_UP,
    buttonDpadDown = KEYCODE_DPAD_DOWN,
    buttonDpadLeft = KEYCODE_DPAD_LEFT,
    buttonDpadRight = KEYCODE_DPAD_RIGHT,
    buttonLeftStick = KEYCODE_BUTTON_THUMBL,
    buttonRightStick = KEYCODE_BUTTON_THUMBR
  )
}
