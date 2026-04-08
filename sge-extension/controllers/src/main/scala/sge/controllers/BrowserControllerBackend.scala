/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-controllers/gwt/src/com/badlogic/gdx/controllers/gwt/GwtControllerManager.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - On JVM/Native this backend is a no-op stub. Real polling lives in
 *     [[BrowserControllerImpl]] which only compiles on Scala.js.
 */
package sge
package controllers

/** [[ControllerOps]] implementation for browser platforms using the Web Gamepad API.
  *
  * On Scala.js, this delegates to [[BrowserControllerImpl]] which calls `navigator.getGamepads()`. On JVM/Native this is a no-op stub (browsers not available).
  *
  * The Web Gamepad API provides access to connected gamepads with the W3C Standard Gamepad layout (same as Xbox/SDL): 17 buttons and 4 axes. Gamepads are only visible after the user interacts with
  * them (press a button).
  */
class BrowserControllerBackend extends ControllerOps {

  /** Most browsers support up to 4 gamepads. */
  override def maxControllers: Int = 4

  override def getConnectedControllers(): Array[ControllerState] = {
    val connected = scala.collection.mutable.ArrayBuffer[ControllerState]()
    var i         = 0
    while (i < maxControllers) {
      val state = pollController(i)
      if (state.connected) connected += state
      i += 1
    }
    connected.toArray
  }

  override def pollController(index: Int): ControllerState =
    BrowserControllerBackend.pollControllerImpl(index)
}

object BrowserControllerBackend {

  /** Platform-specific polling implementation. On Scala.js, this is set to BrowserControllerImpl.pollController by the JS-platform source set. On other platforms, this is a stub returning
    * Disconnected.
    */
  private[controllers] var pollControllerImpl: Int => ControllerState = _ => ControllerState.Disconnected
}
