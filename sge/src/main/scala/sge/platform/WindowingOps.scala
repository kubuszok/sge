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

  /** Sets a GLFW init hint before calling init(). Must be called before init().
    * @param hint
    *   the init hint (e.g. GLFW_PLATFORM)
    * @param value
    *   the value (e.g. GLFW_PLATFORM_NULL)
    */
  def setInitHint(hint: Int, value: Int): Unit

  /** Initializes the windowing library. Must be called before any other windowing ops.
    * @return
    *   true on success
    */
  def init(): Boolean

  /** Terminates the windowing library and frees all resources. */
  def terminate(): Unit

  /** Returns the platform that was selected during initialization (e.g. GLFW_PLATFORM_COCOA, GLFW_PLATFORM_WAYLAND). */
  def getPlatform(): Int

  // ─── Window lifecycle ──────────────────────────────────────────────────

  /** Creates a new window with the given dimensions and title.
    * @return
    *   a native window handle, or 0 on failure
    */
  def createWindow(width: Int, height: Int, title: String): Long

  /** Destroys a previously created window. */
  def destroyWindow(windowHandle: Long): Unit

  /** Returns the platform-native window handle for EGL surface creation.
    *
    * On macOS: returns the NSWindow pointer (via `glfwGetCocoaWindow`). On Linux/X11: returns the X11 Window ID (via `glfwGetX11Window`). On Windows: returns the HWND (via `glfwGetWin32Window`).
    *
    * @return
    *   a native window handle suitable for `eglCreateWindowSurface`
    */
  def getNativeWindowHandle(windowHandle: Long): Long

  /** Updates the native rendering surface scale factor to match the current display. On macOS, this sets the CALayer's `contentsScale` so ANGLE renders at the correct resolution on HiDPI/Retina
    * displays. No-op on other platforms.
    *
    * Should be called whenever the framebuffer size changes (e.g. when moving between displays).
    */
  def updateNativeLayerScale(windowHandle: Long): Unit = ()

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

  // ─── Fullscreen ────────────────────────────────────────────────────────

  /** Sets the window to fullscreen on the given monitor, or windowed mode if monitorHandle is 0. */
  def setWindowMonitor(windowHandle: Long, monitorHandle: Long, x: Int, y: Int, width: Int, height: Int, refreshRate: Int): Unit

  /** Returns the monitor handle the window is fullscreen on, or 0 if windowed. */
  def getWindowMonitor(windowHandle: Long): Long

  /** Sets a window boolean attribute (e.g. decorated, resizable). */
  def setWindowAttrib(windowHandle: Long, attrib: Int, value: Int): Unit

  /** Gets a window attribute value. */
  def getWindowAttrib(windowHandle: Long, attrib: Int): Int

  /** Sets the window icon from one or more Pixmap images. Pass an empty array to reset to default. */
  def setWindowIcon(windowHandle: Long, images: Array[sge.graphics.Pixmap]): Unit

  /** Marks the window as wanting to close. */
  def setWindowShouldClose(windowHandle: Long, value: Boolean): Unit

  /** Sets minimum and maximum size limits for the window. Use -1 for "don't care". */
  def setWindowSizeLimits(windowHandle: Long, minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int): Unit

  /** Requests user attention (e.g. taskbar flash). */
  def requestWindowAttention(windowHandle: Long): Unit

  // ─── Window hints ───────────────────────────────────────────────────────

  /** Sets a window creation hint for the next call to [[createWindow]].
    * @param hint
    *   the hint constant (e.g. GLFW_VISIBLE, GLFW_CLIENT_API)
    * @param value
    *   the value to set
    */
  def setWindowHint(hint: Int, value: Int): Unit

  /** Resets all window hints to their default values. */
  def defaultWindowHints(): Unit

  // ─── Context ──────────────────────────────────────────────────────────

  /** Makes the OpenGL context of the given window current on the calling thread. */
  def makeContextCurrent(windowHandle: Long): Unit

  /** Sets the swap interval (0 = no vsync, 1 = vsync). */
  def setSwapInterval(interval: Int): Unit

  /** Returns true if the given GL/windowing extension is supported. */
  def extensionSupported(extension: String): Boolean

  // ─── Monitor extended ─────────────────────────────────────────────────

  /** Gets the physical size of a monitor in millimeters.
    * @return
    *   (widthMM, heightMM)
    */
  def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int)

  /** Returns the video modes available on the given monitor. Each entry is (width, height, refreshRate, redBits, greenBits, blueBits). */
  def getVideoModes(monitorHandle: Long): Array[(Int, Int, Int, Int, Int, Int)]

  /** Returns the current video mode of a monitor as (width, height, refreshRate, redBits, greenBits, blueBits). */
  def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int)

  // ─── Callbacks ────────────────────────────────────────────────────────

  /** Sets the framebuffer size callback. Called when the framebuffer is resized.
    * @param callback
    *   `(windowHandle, width, height) => Unit`, or null to remove
    */
  def setFramebufferSizeCallback(windowHandle: Long, callback: (Long, Int, Int) => Unit): Unit

  /** Sets the window focus callback.
    * @param callback
    *   `(windowHandle, focused) => Unit`, or null to remove
    */
  def setWindowFocusCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit

  /** Sets the window iconify callback.
    * @param callback
    *   `(windowHandle, iconified) => Unit`, or null to remove
    */
  def setWindowIconifyCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit

  /** Sets the window maximize callback.
    * @param callback
    *   `(windowHandle, maximized) => Unit`, or null to remove
    */
  def setWindowMaximizeCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit

  /** Sets the window close callback.
    * @param callback
    *   `(windowHandle) => Unit`, or null to remove
    */
  def setWindowCloseCallback(windowHandle: Long, callback: Long => Unit): Unit

  /** Sets the file drop callback.
    * @param callback
    *   `(windowHandle, filePaths) => Unit`, or null to remove
    */
  def setDropCallback(windowHandle: Long, callback: (Long, Array[String]) => Unit): Unit

  /** Sets the window refresh callback.
    * @param callback
    *   `(windowHandle) => Unit`, or null to remove
    */
  def setWindowRefreshCallback(windowHandle: Long, callback: Long => Unit): Unit

  // ─── Input callbacks ─────────────────────────────────────────────────

  /** Sets the key callback.
    * @param callback
    *   `(windowHandle, key, scancode, action, mods) => Unit`, or null to remove
    */
  def setKeyCallback(windowHandle: Long, callback: (Long, Int, Int, Int, Int) => Unit): Unit

  /** Sets the character input callback.
    * @param callback
    *   `(windowHandle, codepoint) => Unit`, or null to remove
    */
  def setCharCallback(windowHandle: Long, callback: (Long, Int) => Unit): Unit

  /** Sets the scroll callback.
    * @param callback
    *   `(windowHandle, xOffset, yOffset) => Unit`, or null to remove
    */
  def setScrollCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit

  /** Sets the cursor position callback.
    * @param callback
    *   `(windowHandle, xPos, yPos) => Unit`, or null to remove
    */
  def setCursorPosCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit

  /** Sets the mouse button callback.
    * @param callback
    *   `(windowHandle, button, action, mods) => Unit`, or null to remove
    */
  def setMouseButtonCallback(windowHandle: Long, callback: (Long, Int, Int, Int) => Unit): Unit

  // ─── Input polling ──────────────────────────────────────────────────

  /** Returns the last reported state of a mouse button for the given window.
    * @return
    *   GLFW_PRESS or GLFW_RELEASE
    */
  def getMouseButton(windowHandle: Long, button: Int): Int

  /** Sets the cursor position, in screen coordinates, relative to the upper-left corner of the content area. */
  def setCursorPos(windowHandle: Long, x: Double, y: Double): Unit

  // ─── Time ──────────────────────────────────────────────────────────────

  /** Returns the GLFW/SDL time in seconds since initialization. */
  def getTime(): Double
}

object WindowingOps {
  // GLFW window hint / attribute constants
  val GLFW_FOCUSED:                 Int = 0x00020001
  val GLFW_ICONIFIED:               Int = 0x00020002
  val GLFW_RESIZABLE:               Int = 0x00020003
  val GLFW_VISIBLE:                 Int = 0x00020004
  val GLFW_DECORATED:               Int = 0x00020005
  val GLFW_AUTO_ICONIFY:            Int = 0x00020006
  val GLFW_FLOATING:                Int = 0x00020007
  val GLFW_MAXIMIZED:               Int = 0x00020008
  val GLFW_TRANSPARENT_FRAMEBUFFER: Int = 0x0002000a
  val GLFW_FOCUS_ON_SHOW:           Int = 0x0002000c

  // GLFW context creation hints
  val GLFW_CLIENT_API:            Int = 0x00022001
  val GLFW_NO_API:                Int = 0
  val GLFW_OPENGL_ES_API:         Int = 0x00030002
  val GLFW_CONTEXT_VERSION_MAJOR: Int = 0x00022002
  val GLFW_CONTEXT_VERSION_MINOR: Int = 0x00022003

  // GLFW platform constants (3.4+)
  val GLFW_PLATFORM:         Int = 0x00050003
  val GLFW_ANY_PLATFORM:     Int = 0x00060000
  val GLFW_PLATFORM_WIN32:   Int = 0x00060001
  val GLFW_PLATFORM_COCOA:   Int = 0x00060002
  val GLFW_PLATFORM_WAYLAND: Int = 0x00060003
  val GLFW_PLATFORM_X11:     Int = 0x00060004
  val GLFW_PLATFORM_NULL:    Int = 0x00060005

  // GLFW boolean constants
  val GLFW_TRUE:  Int = 1
  val GLFW_FALSE: Int = 0

  // GLFW input action constants
  val GLFW_RELEASE: Int = 0
  val GLFW_PRESS:   Int = 1
  val GLFW_REPEAT:  Int = 2

  // GLFW cursor mode constants
  val GLFW_CURSOR:          Int = 0x00033001
  val GLFW_CURSOR_NORMAL:   Int = 0x00034001
  val GLFW_CURSOR_HIDDEN:   Int = 0x00034002
  val GLFW_CURSOR_DISABLED: Int = 0x00034003

  // GLFW mouse button constants
  val GLFW_MOUSE_BUTTON_1: Int = 0
  val GLFW_MOUSE_BUTTON_2: Int = 1
  val GLFW_MOUSE_BUTTON_3: Int = 2
  val GLFW_MOUSE_BUTTON_4: Int = 3
  val GLFW_MOUSE_BUTTON_5: Int = 4

  // GLFW key constants
  val GLFW_KEY_SPACE:         Int = 32
  val GLFW_KEY_APOSTROPHE:    Int = 39
  val GLFW_KEY_COMMA:         Int = 44
  val GLFW_KEY_MINUS:         Int = 45
  val GLFW_KEY_PERIOD:        Int = 46
  val GLFW_KEY_SLASH:         Int = 47
  val GLFW_KEY_0:             Int = 48
  val GLFW_KEY_1:             Int = 49
  val GLFW_KEY_2:             Int = 50
  val GLFW_KEY_3:             Int = 51
  val GLFW_KEY_4:             Int = 52
  val GLFW_KEY_5:             Int = 53
  val GLFW_KEY_6:             Int = 54
  val GLFW_KEY_7:             Int = 55
  val GLFW_KEY_8:             Int = 56
  val GLFW_KEY_9:             Int = 57
  val GLFW_KEY_SEMICOLON:     Int = 59
  val GLFW_KEY_EQUAL:         Int = 61
  val GLFW_KEY_A:             Int = 65
  val GLFW_KEY_B:             Int = 66
  val GLFW_KEY_C:             Int = 67
  val GLFW_KEY_D:             Int = 68
  val GLFW_KEY_E:             Int = 69
  val GLFW_KEY_F:             Int = 70
  val GLFW_KEY_G:             Int = 71
  val GLFW_KEY_H:             Int = 72
  val GLFW_KEY_I:             Int = 73
  val GLFW_KEY_J:             Int = 74
  val GLFW_KEY_K:             Int = 75
  val GLFW_KEY_L:             Int = 76
  val GLFW_KEY_M:             Int = 77
  val GLFW_KEY_N:             Int = 78
  val GLFW_KEY_O:             Int = 79
  val GLFW_KEY_P:             Int = 80
  val GLFW_KEY_Q:             Int = 81
  val GLFW_KEY_R:             Int = 82
  val GLFW_KEY_S:             Int = 83
  val GLFW_KEY_T:             Int = 84
  val GLFW_KEY_U:             Int = 85
  val GLFW_KEY_V:             Int = 86
  val GLFW_KEY_W:             Int = 87
  val GLFW_KEY_X:             Int = 88
  val GLFW_KEY_Y:             Int = 89
  val GLFW_KEY_Z:             Int = 90
  val GLFW_KEY_LEFT_BRACKET:  Int = 91
  val GLFW_KEY_BACKSLASH:     Int = 92
  val GLFW_KEY_RIGHT_BRACKET: Int = 93
  val GLFW_KEY_GRAVE_ACCENT:  Int = 96
  val GLFW_KEY_WORLD_1:       Int = 161
  val GLFW_KEY_WORLD_2:       Int = 162
  val GLFW_KEY_ESCAPE:        Int = 256
  val GLFW_KEY_ENTER:         Int = 257
  val GLFW_KEY_TAB:           Int = 258
  val GLFW_KEY_BACKSPACE:     Int = 259
  val GLFW_KEY_INSERT:        Int = 260
  val GLFW_KEY_DELETE:        Int = 261
  val GLFW_KEY_RIGHT:         Int = 262
  val GLFW_KEY_LEFT:          Int = 263
  val GLFW_KEY_DOWN:          Int = 264
  val GLFW_KEY_UP:            Int = 265
  val GLFW_KEY_PAGE_UP:       Int = 266
  val GLFW_KEY_PAGE_DOWN:     Int = 267
  val GLFW_KEY_HOME:          Int = 268
  val GLFW_KEY_END:           Int = 269
  val GLFW_KEY_CAPS_LOCK:     Int = 280
  val GLFW_KEY_SCROLL_LOCK:   Int = 281
  val GLFW_KEY_NUM_LOCK:      Int = 282
  val GLFW_KEY_PRINT_SCREEN:  Int = 283
  val GLFW_KEY_PAUSE:         Int = 284
  val GLFW_KEY_F1:            Int = 290
  val GLFW_KEY_F2:            Int = 291
  val GLFW_KEY_F3:            Int = 292
  val GLFW_KEY_F4:            Int = 293
  val GLFW_KEY_F5:            Int = 294
  val GLFW_KEY_F6:            Int = 295
  val GLFW_KEY_F7:            Int = 296
  val GLFW_KEY_F8:            Int = 297
  val GLFW_KEY_F9:            Int = 298
  val GLFW_KEY_F10:           Int = 299
  val GLFW_KEY_F11:           Int = 300
  val GLFW_KEY_F12:           Int = 301
  val GLFW_KEY_F13:           Int = 302
  val GLFW_KEY_F14:           Int = 303
  val GLFW_KEY_F15:           Int = 304
  val GLFW_KEY_F16:           Int = 305
  val GLFW_KEY_F17:           Int = 306
  val GLFW_KEY_F18:           Int = 307
  val GLFW_KEY_F19:           Int = 308
  val GLFW_KEY_F20:           Int = 309
  val GLFW_KEY_F21:           Int = 310
  val GLFW_KEY_F22:           Int = 311
  val GLFW_KEY_F23:           Int = 312
  val GLFW_KEY_F24:           Int = 313
  val GLFW_KEY_F25:           Int = 314
  val GLFW_KEY_KP_0:          Int = 320
  val GLFW_KEY_KP_1:          Int = 321
  val GLFW_KEY_KP_2:          Int = 322
  val GLFW_KEY_KP_3:          Int = 323
  val GLFW_KEY_KP_4:          Int = 324
  val GLFW_KEY_KP_5:          Int = 325
  val GLFW_KEY_KP_6:          Int = 326
  val GLFW_KEY_KP_7:          Int = 327
  val GLFW_KEY_KP_8:          Int = 328
  val GLFW_KEY_KP_9:          Int = 329
  val GLFW_KEY_KP_DECIMAL:    Int = 330
  val GLFW_KEY_KP_DIVIDE:     Int = 331
  val GLFW_KEY_KP_MULTIPLY:   Int = 332
  val GLFW_KEY_KP_SUBTRACT:   Int = 333
  val GLFW_KEY_KP_ADD:        Int = 334
  val GLFW_KEY_KP_ENTER:      Int = 335
  val GLFW_KEY_KP_EQUAL:      Int = 336
  val GLFW_KEY_LEFT_SHIFT:    Int = 340
  val GLFW_KEY_LEFT_CONTROL:  Int = 341
  val GLFW_KEY_LEFT_ALT:      Int = 342
  val GLFW_KEY_LEFT_SUPER:    Int = 343
  val GLFW_KEY_RIGHT_SHIFT:   Int = 344
  val GLFW_KEY_RIGHT_CONTROL: Int = 345
  val GLFW_KEY_RIGHT_ALT:     Int = 346
  val GLFW_KEY_RIGHT_SUPER:   Int = 347
  val GLFW_KEY_MENU:          Int = 348
}
