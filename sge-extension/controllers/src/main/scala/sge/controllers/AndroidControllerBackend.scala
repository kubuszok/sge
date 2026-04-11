/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Original source: gdx-controllers-android/src/com/badlogic/gdx/controllers/android/AndroidControllers.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidControllers -> AndroidControllerBackend
 *   Convention: polling-based ControllerOps (replaces event-queue architecture)
 *   Idiom: split packages; Nullable; swappable pollControllerImpl like BrowserControllerBackend
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** [[ControllerOps]] implementation for Android platforms using Android's [[android.view.InputDevice]] APIs.
  *
  * On JVM (Android builds), this delegates to [[AndroidControllerInit]] which tracks connected controllers via [[android.view.InputDevice]] and [[android.view.MotionEvent]] / [[android.view.KeyEvent]]
  * events. On other platforms, this is a no-op stub (Android APIs not available).
  *
  * The Android input system identifies controllers by device ID. Each connected game controller or joystick is tracked as a separate controller with its own axis and button state.
  */
class AndroidControllerBackend extends ControllerOps {

  /** Android can have a variable number of controllers. We poll up to 16 slots. */
  override def maxControllers: Int = 16

  override def getConnectedControllers(): Array[ControllerState] =
    AndroidControllerBackend.getConnectedControllersImpl()

  override def pollController(index: Int): ControllerState =
    AndroidControllerBackend.pollControllerImpl(index)
}

object AndroidControllerBackend {

  /** Platform-specific polling implementation. On JVM (Android), this is set by [[AndroidControllerInit.init]]. On other platforms, this returns Disconnected (stub).
    */
  private[controllers] var pollControllerImpl: Int => ControllerState = _ => ControllerState.Disconnected

  /** Platform-specific implementation that returns all connected controllers. On JVM (Android), this is set by [[AndroidControllerInit.init]]. On other platforms, this returns an empty array (stub).
    */
  private[controllers] var getConnectedControllersImpl: () => Array[ControllerState] = () => Array.empty
}
