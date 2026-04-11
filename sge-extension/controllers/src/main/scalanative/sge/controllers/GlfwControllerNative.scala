/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala Native GLFW joystick/gamepad implementation.
 * GLFW is already linked by sge-core (libglfw3).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

@link("glfw3")
@extern
private object GlfwJoystickC {
  def glfwJoystickPresent(jid:    CInt):                   CInt               = extern
  def glfwJoystickIsGamepad(jid:  CInt):                   CInt               = extern
  def glfwGetGamepadState(jid:    CInt, state: Ptr[Byte]): CInt               = extern
  def glfwGetJoystickName(jid:    CInt):                   CString            = extern
  def glfwGetJoystickGUID(jid:    CInt):                   CString            = extern
  def glfwGetJoystickAxes(jid:    CInt, count: Ptr[CInt]): Ptr[CFloat]        = extern
  def glfwGetJoystickButtons(jid: CInt, count: Ptr[CInt]): Ptr[CUnsignedChar] = extern
}

/** Initializes the GlfwControllerBackend companion object's polling function to use actual GLFW FFI calls on Scala Native.
  *
  * This object's clinit (static initializer) replaces the stub implementation.
  */
object GlfwControllerNativeInit {

  // GLFWgamepadstate has 15 bytes (buttons) + 6 floats (axes) = 15 + 24 = 39 bytes
  // but C struct alignment pads buttons[15] to 16 bytes, so total = 40 bytes.
  // Actually: buttons is u8[15], then 1 byte padding, then float[6] = 24 bytes.
  // Total: 40 bytes.
  private val GAMEPAD_STATE_SIZE = 40
  private val BUTTONS_OFFSET     = 0
  private val AXES_OFFSET        = 16 // 15 bytes buttons + 1 padding byte

  /** Call this once at startup to wire the GlfwControllerBackend to real GLFW calls. */
  def init(): Unit =
    GlfwControllerBackend.pollControllerImpl = pollController

  private def pollController(index: Int): ControllerState =
    if (index < 0 || index > 15) ControllerState.Disconnected
    else if (GlfwJoystickC.glfwJoystickPresent(index) == 0) ControllerState.Disconnected
    else {
      val namePtr = GlfwJoystickC.glfwGetJoystickName(index)
      val name    = if (namePtr == null) "" else fromCString(namePtr)

      val guidPtr = GlfwJoystickC.glfwGetJoystickGUID(index)
      val guid    = if (guidPtr == null) "" else fromCString(guidPtr)

      // Try gamepad API first (normalized Xbox layout)
      if (GlfwJoystickC.glfwJoystickIsGamepad(index) != 0) {
        val stateBytes = stackalloc[Byte](GAMEPAD_STATE_SIZE.toUInt)
        if (GlfwJoystickC.glfwGetGamepadState(index, stateBytes) != 0) {
          val buttons = new Array[Boolean](15)
          var bi      = 0
          while (bi < 15) {
            buttons(bi) = !(stateBytes + BUTTONS_OFFSET + bi) != 0.toByte
            bi += 1
          }
          val axes    = new Array[Float](6)
          val axesPtr = (stateBytes + AXES_OFFSET).asInstanceOf[Ptr[CFloat]]
          var ai      = 0
          while (ai < 6) {
            axes(ai) = !(axesPtr + ai)
            ai += 1
          }
          ControllerState.fromDigitalButtons(name, guid, connected = true, buttons, axes, ControllerPowerLevel.Unknown)
        } else {
          // Gamepad state failed, fall back to raw joystick
          pollRawJoystick(index, name, guid)
        }
      } else {
        // Not a gamepad, use raw joystick API
        pollRawJoystick(index, name, guid)
      }
    }

  private def pollRawJoystick(index: Int, name: String, guid: String): ControllerState = {
    val axisCountPtr   = stackalloc[CInt]()
    val buttonCountPtr = stackalloc[CInt]()

    val axesPtr    = GlfwJoystickC.glfwGetJoystickAxes(index, axisCountPtr)
    val buttonsPtr = GlfwJoystickC.glfwGetJoystickButtons(index, buttonCountPtr)

    val axisCount   = !axisCountPtr
    val buttonCount = !buttonCountPtr

    val axes = new Array[Float](axisCount)
    var ai   = 0
    while (ai < axisCount) {
      axes(ai) = !(axesPtr + ai)
      ai += 1
    }

    val buttons = new Array[Boolean](buttonCount)
    var bi      = 0
    while (bi < buttonCount) {
      buttons(bi) = !(buttonsPtr + bi) != 0.toUByte
      bi += 1
    }

    ControllerState.fromDigitalButtons(name, guid, connected = true, buttons, axes, ControllerPowerLevel.Unknown)
  }
}
