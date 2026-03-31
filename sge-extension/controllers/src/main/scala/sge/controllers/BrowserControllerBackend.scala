/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** Stub [[ControllerOps]] implementation documenting the Web Gamepad API approach for browser controller support.
  *
  * The Web Gamepad API (`navigator.getGamepads()`) provides access to connected gamepads in the browser. In Scala.js, this is
  * available via `org.scalajs.dom`.
  *
  * Implementation approach:
  *   - Poll `navigator.getGamepads()` each frame (returns up to 4 gamepads in most browsers)
  *   - Each `Gamepad` object has: `id: String`, `index: Int`, `connected: Boolean`, `buttons: Array[GamepadButton]`,
  *     `axes: Array[Double]`, `mapping: String` ("standard" for Xbox layout)
  *   - `GamepadButton` has `pressed: Boolean` and `value: Double` (for analog triggers)
  *   - The "standard" mapping matches the W3C Standard Gamepad layout (same as Xbox/SDL)
  *
  * Browser quirks:
  *   - Gamepads are only visible after the user interacts with them (press a button)
  *   - `gamepadconnected` / `gamepaddisconnected` events can supplement polling
  *   - Some browsers return `null` entries in the gamepads array for disconnected slots
  *   - The `navigator.getGamepads()` API may not be available in all contexts (e.g. iframes)
  *
  * When implementing for Scala.js, this class should be replaced with actual `scalajs-dom` calls:
  * {{{
  *   import org.scalajs.dom
  *   val gamepads = dom.window.navigator.getGamepads()
  *   // Filter nulls and map to ControllerState
  * }}}
  */
class BrowserControllerBackend extends ControllerOps {

  /** Most browsers support up to 4 gamepads. */
  override def maxControllers: Int = 4

  override def getConnectedControllers(): Array[ControllerState] = {
    // Stub: no controllers detected until Scala.js Gamepad API bindings are wired
    Array.empty
  }

  override def pollController(index: Int): ControllerState = {
    // Stub: always returns disconnected until Scala.js Gamepad API bindings are wired
    ControllerState.Disconnected
  }
}
