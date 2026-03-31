/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** Stub [[ControllerOps]] implementation documenting the GLFW functions needed for desktop controller support.
  *
  * When the sge-deps/native-components Rust module is extended to expose GLFW gamepad functions, this stub should be replaced with
  * actual FFI calls. The required GLFW functions are:
  *
  *   - `glfwJoystickPresent(jid: Int): Boolean` -- is joystick/gamepad connected?
  *   - `glfwJoystickIsGamepad(jid: Int): Boolean` -- does it have a gamepad mapping?
  *   - `glfwGetGamepadState(jid: Int, state: *GLFWgamepadstate): Boolean` -- Xbox-style button/axis state
  *   - `glfwGetJoystickName(jid: Int): String` -- device name
  *   - `glfwGetJoystickGUID(jid: Int): String` -- persistent ID for mapping
  *   - `glfwSetJoystickCallback(callback: GLFWjoystickfun): Unit` -- connect/disconnect events
  *
  * GLFW supports up to 16 joysticks (GLFW_JOYSTICK_1 through GLFW_JOYSTICK_LAST). The gamepad state structure contains 15 buttons
  * and 6 axes matching the SDL GameController layout.
  *
  * The Rust FFI bridge in sge-deps/native-components would need:
  * {{{
  *   #[no_mangle]
  *   pub extern "C" fn sge_controller_get_gamepad_state(jid: i32, buttons: *mut u8, axes: *mut f32) -> i32
  *   #[no_mangle]
  *   pub extern "C" fn sge_controller_get_joystick_name(jid: i32) -> *const c_char
  *   #[no_mangle]
  *   pub extern "C" fn sge_controller_get_joystick_guid(jid: i32) -> *const c_char
  *   #[no_mangle]
  *   pub extern "C" fn sge_controller_is_joystick_present(jid: i32) -> i32
  * }}}
  */
class GlfwControllerBackend extends ControllerOps {

  /** GLFW supports joystick IDs 0-15. */
  override def maxControllers: Int = 16

  override def getConnectedControllers(): Array[ControllerState] = {
    // Stub: no controllers detected until GLFW FFI bindings are wired
    Array.empty
  }

  override def pollController(index: Int): ControllerState = {
    // Stub: always returns disconnected until GLFW FFI bindings are wired
    ControllerState.Disconnected
  }
}
