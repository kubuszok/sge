/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: com/badlogic/gdx/controllers/desktop/support/JamepadController.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - JVM polling stub: GLFW joystick functions are not exposed through the sge-core Panama
 *     downcall layer yet. The Scala Native variant uses @extern bindings directly and is
 *     functional. JVM polling returns disconnected state until the Panama downcalls land.
 *
 * upstream-commit: 124b68125c7ef9c552085865379f77e8bee2ae3b
 */
package sge
package controllers

/** [[ControllerOps]] implementation for desktop platforms using GLFW joystick/gamepad APIs.
  *
  * On Scala Native, this calls GLFW functions directly via @extern bindings. On JVM, this is a polling stub pending Panama downcall wiring (GLFW is loaded but joystick functions aren't exposed
  * through the sge-core Panama layer yet).
  *
  * GLFW supports up to 16 joysticks (GLFW_JOYSTICK_1 through GLFW_JOYSTICK_LAST). The gamepad state structure contains 15 buttons and 6 axes matching the SDL GameController layout.
  */
class GlfwControllerBackend extends ControllerOps {

  /** GLFW supports joystick IDs 0-15. */
  override def maxControllers: Int = 16

  override def getConnectedControllers(): Array[ControllerState] = {
    val connected = scala.collection.mutable.ArrayBuffer[ControllerState]()
    var i         = 0
    while (i < 16) {
      val state = pollController(i)
      if (state.connected) connected += state
      i += 1
    }
    connected.toArray
  }

  override def pollController(index: Int): ControllerState =
    GlfwControllerBackend.pollControllerImpl(index)
}

object GlfwControllerBackend {

  /** Platform-specific polling implementation. On Scala Native, this is overridden by the actual GLFW FFI bindings in scalanative/. On JVM, this returns Disconnected (stub).
    */
  private[controllers] var pollControllerImpl: Int => ControllerState = _ => ControllerState.Disconnected

  /** Builds the [[ControllerState.uniqueId]] for a GLFW joystick from its model GUID and its joystick slot index.
    *
    * Two physically distinct pads of the same model report the SAME GLFW GUID, so a GUID-only id makes [[DefaultControllerManager.poll]] (which matches controllers by `uniqueId`) MERGE them into one
    * controller. Incorporating the slot index disambiguates them, mirroring the browser backend's `s"gamepad-$gpIndex"` (BrowserControllerImpl). Pure helper so the distinctness is unit-testable
    * without GLFW.
    *
    * @param guid
    *   the GLFW joystick GUID (identical across same-model pads)
    * @param slot
    *   the GLFW joystick slot index (0..15), which is unique per connected pad
    * @return
    *   a uniqueId distinct per slot even for identical GUIDs
    */
  def uniqueIdFor(guid: String, slot: Int): String =
    s"$guid-$slot"
}
