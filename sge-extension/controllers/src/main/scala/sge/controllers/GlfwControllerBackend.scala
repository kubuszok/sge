/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: GlfwControllerBackend,getConnectedControllers,maxControllers,pollController,uniqueIdFor
 * Covenant-source-reference: com/badlogic/gdx/controllers/desktop/support/JamepadController.java
 * Covenant-verified: 2026-07-17
 *
 * upstream-commit: 124b68125c7ef9c552085865379f77e8bee2ae3b
 */
package sge
package controllers

/** [[ControllerOps]] implementation for desktop platforms using GLFW joystick/gamepad APIs.
  *
  * On Scala Native, this calls GLFW functions directly via @extern bindings ([[GlfwControllerNativeInit]]). On JVM, [[GlfwControllerJvmInit.init]] installs a Panama FFM downcall implementation that
  * loads GLFW itself and degrades to Disconnected when GLFW is unavailable/uninitialized. Until the platform's `init()` runs, the seam holds the Disconnected default below.
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

  /** Platform-specific polling implementation. Installed by the platform `init()`: [[GlfwControllerNativeInit.init]] on Scala Native, [[GlfwControllerJvmInit.init]] (Panama FFM) on JVM. Until then it
    * is the Disconnected default below.
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
