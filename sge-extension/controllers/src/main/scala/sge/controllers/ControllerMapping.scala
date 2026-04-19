/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 74
 * Covenant-baseline-methods: ControllerMapping,StandardMapping,Undefined,axisLeftX,axisLeftY,axisRightX,axisRightY,buttonA,buttonB,buttonBack,buttonDpadDown,buttonDpadLeft,buttonDpadRight,buttonDpadUp,buttonL1,buttonL2,buttonLeftStick,buttonR1,buttonR2,buttonRightStick,buttonStart,buttonX,buttonY
 * Covenant-source-reference: com/badlogic/gdx/controllers/ControllerMapping.java
 * Covenant-verified: 2026-04-19
 */
package sge
package controllers

/** Default axis and button constants returned by [[Controller.mapping]].
  *
  * Note that on some platforms, this may return the general platform mapping. A connected controller might not have all the features, check with [[Controller.axisCount]] and
  * [[Controller.maxButtonIndex]].
  */
class ControllerMapping(
  val axisLeftX:  Int,
  val axisLeftY:  Int,
  val axisRightX: Int,
  val axisRightY: Int,

  val buttonA:     Int,
  val buttonB:     Int,
  val buttonX:     Int,
  val buttonY:     Int,
  val buttonBack:  Int,
  val buttonStart: Int,

  val buttonL1: Int,
  val buttonL2: Int,
  val buttonR1: Int,
  val buttonR2: Int,

  val buttonDpadUp:    Int,
  val buttonDpadDown:  Int,
  val buttonDpadLeft:  Int,
  val buttonDpadRight: Int,

  val buttonLeftStick:  Int,
  val buttonRightStick: Int
)

object ControllerMapping {

  /** Sentinel value indicating a button or axis is not mapped. */
  val Undefined: Int = -1

  /** Standard Xbox/SDL gamepad mapping used by GLFW's gamepad API. This matches the GLFW_GAMEPAD_* constants and the SDL GameController database layout.
    *
    * Axes: 0=LeftX, 1=LeftY, 2=RightX, 3=RightY Buttons: 0=A, 1=B, 2=X, 3=Y, 4=Back, 5=Guide(unused), 6=Start, 7=LeftStick, 8=RightStick, 9=LB, 10=RB, 11=DpadUp, 12=DpadDown, 13=DpadLeft,
    * 14=DpadRight
    */
  val StandardMapping: ControllerMapping = ControllerMapping(
    axisLeftX = 0,
    axisLeftY = 1,
    axisRightX = 2,
    axisRightY = 3,
    buttonA = 0,
    buttonB = 1,
    buttonX = 2,
    buttonY = 3,
    buttonBack = 4,
    buttonStart = 6,
    buttonL1 = 9,
    buttonL2 = Undefined,
    buttonR1 = 10,
    buttonR2 = Undefined,
    buttonDpadUp = 11,
    buttonDpadDown = 12,
    buttonDpadLeft = 13,
    buttonDpadRight = 14,
    buttonLeftStick = 7,
    buttonRightStick = 8
  )
}
