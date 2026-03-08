/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Input.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Input -> DesktopInput
 *   Merged with: DefaultLwjgl3Input interface portion (Disposable -> AutoCloseable)
 *   Convention: Java interface -> Scala trait; AutoCloseable replaces Disposable
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Desktop-specific extension of [[Input]] with lifecycle methods for the window event loop.
  *
  * The desktop application calls these methods each frame to coordinate input state with the windowing system. The implementation registers callbacks (e.g. GLFW/SDL3 key, mouse, scroll) and buffers
  * events between frames.
  *
  * @author
  *   See AUTHORS file (original implementation)
  */
trait DesktopInput extends Input with AutoCloseable {

  /** Called when the native window handle changes (e.g. window recreation). The implementation should re-register callbacks on the new handle.
    *
    * @param windowHandle
    *   the new native window handle
    */
  def windowHandleChanged(windowHandle: Long): Unit

  /** Called once per frame to process buffered input events and update polling state. */
  def update(): Unit

  /** Called before [[update]] to swap event buffers for the next frame. */
  def prepareNext(): Unit

  /** Resets all polling states (key pressed, button pressed, etc.) to their default values. Called when the window regains focus to prevent stale state.
    */
  def resetPollingStates(): Unit
}
