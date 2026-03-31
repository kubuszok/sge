/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** Platform operations trait for controller polling. Platform backends implement this to provide raw controller state from the underlying system (GLFW, Web Gamepad API, Android, etc.).
  *
  * This follows the SGE pattern used by other extensions (AudioOps, WindowingOps, etc.) where a trait defines the FFI contract and platform-specific implementations provide the actual hardware
  * access.
  */
trait ControllerOps {

  /** Returns the number of controller slots supported by the platform. For GLFW this is typically 16 (GLFW_JOYSTICK_1 through GLFW_JOYSTICK_LAST). For Web Gamepad API this varies by browser.
    */
  def maxControllers: Int

  /** Polls the state of all connected controllers.
    *
    * @return
    *   an array of [[ControllerState]] for each connected controller. Disconnected slots are not included.
    */
  def getConnectedControllers(): Array[ControllerState]

  /** Polls the state of a specific controller slot.
    *
    * @param index
    *   the controller slot index (0-based)
    * @return
    *   the current state of the controller at the given index, or [[ControllerState.Disconnected]] if no controller is present
    */
  def pollController(index: Int): ControllerState
}

/** Snapshot of a controller's state at a point in time. Returned by [[ControllerOps.pollController]]. */
final case class ControllerState(
  name:       String,
  uniqueId:   String,
  connected:  Boolean,
  buttons:    Array[Boolean],
  axes:       Array[Float],
  powerLevel: ControllerPowerLevel
)

object ControllerState {

  /** A sentinel state representing an empty/disconnected controller slot. */
  val Disconnected: ControllerState = ControllerState(
    name = "",
    uniqueId = "",
    connected = false,
    buttons = Array.empty,
    axes = Array.empty,
    powerLevel = ControllerPowerLevel.Unknown
  )
}
