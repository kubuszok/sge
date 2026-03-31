/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala.js Web Gamepad API implementation.
 * Uses scalajs-dom to access navigator.getGamepads().
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ global => g }

/** Web Gamepad API implementation for the [[BrowserControllerBackend]].
  *
  * Uses `navigator.getGamepads()` to poll connected gamepads. The "standard" mapping
  * matches the W3C Standard Gamepad layout (same as Xbox/SDL: 17 buttons, 4 axes).
  *
  * Browser quirks:
  *   - Gamepads are only visible after the user interacts with them (press a button)
  *   - Some browsers return null entries in the gamepads array for disconnected slots
  *   - The API may not be available in all contexts (e.g. cross-origin iframes)
  */
object BrowserControllerImpl {

  /** Polls a specific gamepad slot using the Web Gamepad API.
    * Returns [[ControllerState.Disconnected]] if the slot is empty or the API is unavailable.
    */
  def pollController(index: Int): ControllerState = {
    try {
      val nav = g.navigator
      if (js.isUndefined(nav) || js.isUndefined(nav.getGamepads)) {
        ControllerState.Disconnected
      } else {
        val gamepads = nav.getGamepads().asInstanceOf[js.Array[js.Dynamic]]
        if (index < 0 || index >= gamepads.length) ControllerState.Disconnected
        else {
          val gp = gamepads(index)
          if (gp == null || js.isUndefined(gp) || !gp.connected.asInstanceOf[Boolean]) {
            ControllerState.Disconnected
          } else {
            val id = gp.id.asInstanceOf[String]
            val gpIndex = gp.index.asInstanceOf[Int]

            val jsButtons = gp.buttons.asInstanceOf[js.Array[js.Dynamic]]
            val buttons = new Array[Boolean](jsButtons.length)
            var bi = 0
            while (bi < jsButtons.length) {
              buttons(bi) = jsButtons(bi).pressed.asInstanceOf[Boolean]
              bi += 1
            }

            val jsAxes = gp.axes.asInstanceOf[js.Array[Double]]
            val axes = new Array[Float](jsAxes.length)
            var ai = 0
            while (ai < jsAxes.length) {
              axes(ai) = jsAxes(ai).toFloat
              ai += 1
            }

            ControllerState(
              name = id,
              uniqueId = s"gamepad-$gpIndex",
              connected = true,
              buttons = buttons,
              axes = axes,
              powerLevel = ControllerPowerLevel.Unknown
            )
          }
        }
      }
    } catch {
      case _: Exception => ControllerState.Disconnected
    }
  }

  /** Returns the maximum number of gamepads the browser supports (typically 4). */
  def maxControllers: Int = {
    try {
      val nav = g.navigator
      if (js.isUndefined(nav) || js.isUndefined(nav.getGamepads)) 0
      else nav.getGamepads().asInstanceOf[js.Array[js.Dynamic]].length
    } catch {
      case _: Exception => 0
    }
  }
}
