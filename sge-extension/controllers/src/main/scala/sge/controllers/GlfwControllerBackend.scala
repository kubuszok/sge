/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
}
