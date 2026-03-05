/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original platform abstraction for windowing system (GLFW/SDL3)
 *   Convention: trait defines FFI contract; JVM uses Panama, Native uses @extern
 *   Idiom: split packages
 */
package sge
package platform

/** Windowing system operations (GLFW/SDL3). Defines the FFI contract for creating windows, handling input, cursors, and clipboard.
  *
  * Platform implementations:
  *   - JVM: Panama downcall handles to GLFW/SDL3 shared library
  *   - Native: @extern C FFI to GLFW/SDL3
  *
  * Methods are added incrementally as backend files are ported. This starts minimal and grows.
  */
private[sge] trait WindowingOps {

  // ─── Initialization ────────────────────────────────────────────────────

  /** Initializes the windowing library. Must be called before any other windowing ops.
    * @return
    *   true on success
    */
  def init(): Boolean

  /** Terminates the windowing library and frees all resources. */
  def terminate(): Unit

  // ─── Window lifecycle ──────────────────────────────────────────────────

  /** Creates a new window with the given dimensions and title.
    * @return
    *   a native window handle, or 0 on failure
    */
  def createWindow(width: Int, height: Int, title: String): Long

  /** Destroys a previously created window. */
  def destroyWindow(windowHandle: Long): Unit

  /** Returns true if the window has been requested to close. */
  def windowShouldClose(windowHandle: Long): Boolean

  /** Swaps the front and back framebuffers. */
  def swapBuffers(windowHandle: Long): Unit

  /** Processes all pending events (input, resize, etc.). */
  def pollEvents(): Unit

  // ─── Window properties ─────────────────────────────────────────────────

  /** Sets the window title. */
  def setWindowTitle(windowHandle: Long, title: String): Unit

  /** Gets the window size in screen coordinates.
    * @return
    *   (width, height)
    */
  def getWindowSize(windowHandle: Long): (Int, Int)

  /** Sets the window size in screen coordinates. */
  def setWindowSize(windowHandle: Long, width: Int, height: Int): Unit

  /** Gets the window position in screen coordinates.
    * @return
    *   (x, y)
    */
  def getWindowPos(windowHandle: Long): (Int, Int)

  /** Sets the window position in screen coordinates. */
  def setWindowPos(windowHandle: Long, x: Int, y: Int): Unit

  /** Gets the framebuffer size in pixels (may differ from window size on HiDPI).
    * @return
    *   (width, height)
    */
  def getFramebufferSize(windowHandle: Long): (Int, Int)

  /** Iconifies (minimizes) the window. */
  def iconifyWindow(windowHandle: Long): Unit

  /** Restores an iconified or maximized window to its normal state. */
  def restoreWindow(windowHandle: Long): Unit

  /** Maximizes the window. */
  def maximizeWindow(windowHandle: Long): Unit

  /** Makes the window visible. */
  def showWindow(windowHandle: Long): Unit

  /** Hides the window. */
  def hideWindow(windowHandle: Long): Unit

  /** Brings the window to front and sets input focus. */
  def focusWindow(windowHandle: Long): Unit

  // ─── Clipboard ─────────────────────────────────────────────────────────

  /** Gets the clipboard string content.
    * @return
    *   the clipboard text, or null if empty
    */
  def getClipboardString(windowHandle: Long): String

  /** Sets the clipboard string content. */
  def setClipboardString(windowHandle: Long, content: String): Unit

  // ─── Input mode ────────────────────────────────────────────────────────

  /** Gets the value of an input mode for the specified window. */
  def getInputMode(windowHandle: Long, mode: Int): Int

  /** Sets an input mode for the specified window. */
  def setInputMode(windowHandle: Long, mode: Int, value: Int): Unit

  // ─── Cursor ────────────────────────────────────────────────────────────

  /** Creates a standard system cursor.
    * @param shape
    *   cursor shape constant
    * @return
    *   a native cursor handle, or 0 on failure
    */
  def createStandardCursor(shape: Int): Long

  /** Sets the cursor for the specified window. Pass 0 to reset to default. */
  def setCursor(windowHandle: Long, cursorHandle: Long): Unit

  /** Destroys a previously created cursor. */
  def destroyCursor(cursorHandle: Long): Unit

  // ─── Monitor ───────────────────────────────────────────────────────────

  /** Returns the primary monitor handle. */
  def getPrimaryMonitor(): Long

  /** Returns handles of all currently connected monitors. */
  def getMonitors(): Array[Long]

  /** Gets the human-readable name of a monitor. */
  def getMonitorName(monitorHandle: Long): String

  /** Gets the virtual position of a monitor's viewport on the virtual screen.
    * @return
    *   (x, y)
    */
  def getMonitorPos(monitorHandle: Long): (Int, Int)

  // ─── Time ──────────────────────────────────────────────────────────────

  /** Returns the GLFW/SDL time in seconds since initialization. */
  def getTime(): Double
}
