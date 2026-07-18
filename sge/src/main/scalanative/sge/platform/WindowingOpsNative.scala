/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original Scala Native implementation of WindowingOps
 *   Convention: @extern C FFI to GLFW shared library
 *   Convention: GLFW opaque pointers (window, monitor, cursor) -> Ptr[Byte], Long via intrinsics
 *   Convention: Callbacks via CFuncPtr.fromScalaFunction
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import java.nio.charset.StandardCharsets

import lowlevel.Nullable
import scala.scalanative.runtime.{ Intrinsics, fromRawPtr, toRawPtr }
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichLong

// ─── GLFW C bindings ──────────────────────────────────────────────────────

@link("glfw3")
@extern
private object GlfwC {

  // Init / terminate
  def glfwInitHint(hint: CInt, value: CInt): Unit = extern
  def glfwInit():                            CInt = extern
  def glfwTerminate():                       Unit = extern
  def glfwGetPlatform():                     CInt = extern

  // Window lifecycle
  def glfwCreateWindow(
    width:   CInt,
    height:  CInt,
    title:   CString,
    monitor: Ptr[Byte],
    share:   Ptr[Byte]
  ):                                            Ptr[Byte] = extern
  def glfwDestroyWindow(window:     Ptr[Byte]): Unit      = extern
  def glfwWindowShouldClose(window: Ptr[Byte]): CInt      = extern
  def glfwSwapBuffers(window:       Ptr[Byte]): Unit      = extern
  def glfwPollEvents():                         Unit      = extern

  // Window properties
  def glfwSetWindowTitle(window:     Ptr[Byte], title: CString):                 Unit = extern
  def glfwGetWindowSize(window:      Ptr[Byte], w:     Ptr[CInt], h: Ptr[CInt]): Unit = extern
  def glfwSetWindowSize(window:      Ptr[Byte], w:     CInt, h:      CInt):      Unit = extern
  def glfwGetWindowPos(window:       Ptr[Byte], x:     Ptr[CInt], y: Ptr[CInt]): Unit = extern
  def glfwSetWindowPos(window:       Ptr[Byte], x:     CInt, y:      CInt):      Unit = extern
  def glfwGetFramebufferSize(window: Ptr[Byte], w:     Ptr[CInt], h: Ptr[CInt]): Unit = extern
  def glfwIconifyWindow(window:      Ptr[Byte]):                                 Unit = extern
  def glfwRestoreWindow(window:      Ptr[Byte]):                                 Unit = extern
  def glfwMaximizeWindow(window:     Ptr[Byte]):                                 Unit = extern
  def glfwShowWindow(window:         Ptr[Byte]):                                 Unit = extern
  def glfwHideWindow(window:         Ptr[Byte]):                                 Unit = extern
  def glfwFocusWindow(window:        Ptr[Byte]):                                 Unit = extern

  // Clipboard
  def glfwGetClipboardString(window: Ptr[Byte]):             CString = extern
  def glfwSetClipboardString(window: Ptr[Byte], s: CString): Unit    = extern

  // Input mode
  def glfwGetInputMode(window: Ptr[Byte], mode: CInt):              CInt = extern
  def glfwSetInputMode(window: Ptr[Byte], mode: CInt, value: CInt): Unit = extern

  // Cursor
  def glfwCreateStandardCursor(shape: CInt): Ptr[Byte] = extern
  // glfwCreateCursor(const GLFWimage* image, int xhot, int yhot) -> GLFWcursor*. Core GLFW (not
  // platform-specific like Wayland), so the symbol is present in the shipped sn-provider libglfw and
  // links cleanly at nativeLink time on every platform (contrast glfwGetWaylandWindow, ISS-761).
  def glfwCreateCursor(image:   Ptr[Byte], xhot:   CInt, yhot: CInt): Ptr[Byte] = extern
  def glfwSetCursor(window:     Ptr[Byte], cursor: Ptr[Byte]):        Unit      = extern
  def glfwDestroyCursor(cursor: Ptr[Byte]):                           Unit      = extern

  // Monitor
  def glfwGetPrimaryMonitor():                                            Ptr[Byte]      = extern
  def glfwGetMonitors(count:      Ptr[CInt]):                             Ptr[Ptr[Byte]] = extern
  def glfwGetMonitorName(monitor: Ptr[Byte]):                             CString        = extern
  def glfwGetMonitorPos(monitor:  Ptr[Byte], x: Ptr[CInt], y: Ptr[CInt]): Unit           = extern

  // Fullscreen / window attribs
  def glfwSetWindowMonitor(
    window:      Ptr[Byte],
    monitor:     Ptr[Byte],
    x:           CInt,
    y:           CInt,
    w:           CInt,
    h:           CInt,
    refreshRate: CInt
  ):                                                                                Unit      = extern
  def glfwGetWindowMonitor(window:     Ptr[Byte]):                                  Ptr[Byte] = extern
  def glfwSetWindowAttrib(window:      Ptr[Byte], attrib: CInt, value:  CInt):      Unit      = extern
  def glfwGetWindowAttrib(window:      Ptr[Byte], attrib: CInt):                    CInt      = extern
  def glfwSetWindowIcon(window:        Ptr[Byte], count:  CInt, images: Ptr[Byte]): Unit      = extern
  def glfwSetWindowShouldClose(window: Ptr[Byte], value:  CInt):                    Unit      = extern
  def glfwSetWindowSizeLimits(
    window:    Ptr[Byte],
    minWidth:  CInt,
    minHeight: CInt,
    maxWidth:  CInt,
    maxHeight: CInt
  ):                                                 Unit = extern
  def glfwRequestWindowAttention(window: Ptr[Byte]): Unit = extern

  // Window hints
  def glfwWindowHint(hint: CInt, value: CInt): Unit = extern
  def glfwDefaultWindowHints():                Unit = extern

  // Context
  def glfwMakeContextCurrent(window:    Ptr[Byte]): Unit = extern
  def glfwSwapInterval(interval:        CInt):      Unit = extern
  def glfwExtensionSupported(extension: CString):   CInt = extern

  // Monitor extended
  def glfwGetMonitorPhysicalSize(monitor: Ptr[Byte], wMM:   Ptr[CInt], hMM: Ptr[CInt]): Unit      = extern
  def glfwGetVideoModes(monitor:          Ptr[Byte], count: Ptr[CInt]):                 Ptr[Byte] = extern
  def glfwGetVideoMode(monitor:           Ptr[Byte]):                                   Ptr[Byte] = extern

  // Callbacks
  def glfwSetFramebufferSizeCallback(window: Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], CInt, CInt, Unit]):         CFuncPtr3[Ptr[Byte], CInt, CInt, Unit]         = extern
  def glfwSetWindowFocusCallback(window:     Ptr[Byte], cb: CFuncPtr2[Ptr[Byte], CInt, Unit]):               CFuncPtr2[Ptr[Byte], CInt, Unit]               = extern
  def glfwSetWindowIconifyCallback(window:   Ptr[Byte], cb: CFuncPtr2[Ptr[Byte], CInt, Unit]):               CFuncPtr2[Ptr[Byte], CInt, Unit]               = extern
  def glfwSetWindowMaximizeCallback(window:  Ptr[Byte], cb: CFuncPtr2[Ptr[Byte], CInt, Unit]):               CFuncPtr2[Ptr[Byte], CInt, Unit]               = extern
  def glfwSetWindowCloseCallback(window:     Ptr[Byte], cb: CFuncPtr1[Ptr[Byte], Unit]):                     CFuncPtr1[Ptr[Byte], Unit]                     = extern
  def glfwSetDropCallback(window:            Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], CInt, Ptr[CString], Unit]): CFuncPtr3[Ptr[Byte], CInt, Ptr[CString], Unit] = extern
  def glfwSetWindowRefreshCallback(window:   Ptr[Byte], cb: CFuncPtr1[Ptr[Byte], Unit]):                     CFuncPtr1[Ptr[Byte], Unit]                     = extern

  // Error callback: GLFWerrorfun signature void(*)(int error, const char* description).
  def glfwSetErrorCallback(cb: CFuncPtr2[CInt, CString, Unit]): CFuncPtr2[CInt, CString, Unit] = extern

  // Input callbacks
  def glfwSetKeyCallback(window:         Ptr[Byte], cb: CFuncPtr5[Ptr[Byte], CInt, CInt, CInt, CInt, Unit]): CFuncPtr5[Ptr[Byte], CInt, CInt, CInt, CInt, Unit] = extern
  def glfwSetCharCallback(window:        Ptr[Byte], cb: CFuncPtr2[Ptr[Byte], CUnsignedInt, Unit]):           CFuncPtr2[Ptr[Byte], CUnsignedInt, Unit]           = extern
  def glfwSetScrollCallback(window:      Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], CDouble, CDouble, Unit]):       CFuncPtr3[Ptr[Byte], CDouble, CDouble, Unit]       = extern
  def glfwSetCursorPosCallback(window:   Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], CDouble, CDouble, Unit]):       CFuncPtr3[Ptr[Byte], CDouble, CDouble, Unit]       = extern
  def glfwSetMouseButtonCallback(window: Ptr[Byte], cb: CFuncPtr4[Ptr[Byte], CInt, CInt, CInt, Unit]):       CFuncPtr4[Ptr[Byte], CInt, CInt, CInt, Unit]       = extern

  // Input polling
  def glfwGetMouseButton(window: Ptr[Byte], button: CInt):                CInt = extern
  def glfwSetCursorPos(window:   Ptr[Byte], x:      CDouble, y: CDouble): Unit = extern

  // Time
  def glfwGetTime(): CDouble = extern

  // Native window handle (glfw3native.h) — platform-specific, may not be available on all targets.
  // NOTE: glfwGetWaylandWindow is intentionally NOT declared here — the shipped sn-provider GLFW
  // build lacks Wayland support, and Scala Native links @extern symbols eagerly at nativeLink time,
  // so binding it would break every native link with an undefined reference (ISS-761).
  def glfwGetCocoaWindow(window: Ptr[Byte]): Ptr[Byte]         = extern
  def glfwGetX11Window(window:   Ptr[Byte]): CUnsignedLongLong = extern
  def glfwGetWin32Window(window: Ptr[Byte]): Ptr[Byte]         = extern
}

// ─── Objective-C runtime bindings (macOS only) ───────────────────────────────
// Used to extract CALayer from NSWindow for ANGLE's Metal backend.
// These symbols are already loaded by GLFW's Cocoa backend.

@link("objc")
@extern
private object ObjCRuntime {
  @name("sel_registerName")
  def selRegisterName(name: CString): Ptr[Byte] = extern

  // objc_msgSend(receiver, selector, arg) — single declaration covering both 0-arg and 1-arg
  // Objective-C calls. On arm64, objc_msgSend ignores unused register arguments, so passing
  // null as the third arg for 0-arg selectors (e.g. [obj contentView]) is ABI-safe.
  // For [view setWantsLayer:YES], pass the BOOL as a pointer-sized value.
  @name("objc_msgSend")
  def msgSend(obj: Ptr[Byte], sel: Ptr[Byte], arg: Ptr[Byte]): Ptr[Byte] = extern

  // objc_msgSend variant for sending a CGFloat (Double on 64-bit) argument — setContentsScale:
  @name("objc_msgSend")
  def msgSendDouble(obj: Ptr[Byte], sel: Ptr[Byte], arg: CDouble): Ptr[Byte] = extern

  // objc_msgSend variant returning NSUInteger — used for [NSArray count].
  // ABI: NSUInteger == unsigned long == 64-bit on macOS (LP64), so the return is CSize,
  // NOT CInt.
  @name("objc_msgSend")
  def msgSendCount(obj: Ptr[Byte], sel: Ptr[Byte]): CSize = extern

  // objc_msgSend variant taking an NSUInteger index and returning id — used for
  // [NSArray objectAtIndex:]. ABI: NSUInteger index == unsigned long == 64-bit (CSize).
  @name("objc_msgSend")
  def msgSendIndex(obj: Ptr[Byte], sel: Ptr[Byte], idx: CSize): Ptr[Byte] = extern

  // objc_getClass — look up an Objective-C class by name (used for CATransaction).
  @name("objc_getClass")
  def objcGetClass(name: CString): Ptr[Byte] = extern
}

// ─── GLFWvidmode struct layout ─────────────────────────────────────────────
// struct GLFWvidmode { int width, height, redBits, greenBits, blueBits, refreshRate; }
// Size = 6 * sizeof(int) = 24 bytes

// ─── WindowingOps implementation ──────────────────────────────────────────

private[sge] object WindowingOpsNative extends WindowingOps {

  private inline def ptrFromLong(h: Long): Ptr[Byte] =
    if (h == 0L) null else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(h))

  private inline def longFromPtr(p: Ptr[Byte]): Long =
    if (p == null) 0L else Intrinsics.castRawPtrToLong(toRawPtr(p))

  private val UTF8 = StandardCharsets.UTF_8

  // Cached CALayer address — set once per window at creation, reused by updateNativeLayerScale.
  // Map from GLFW window handle → CALayer address (macOS only).
  private val cachedLayerAddresses: java.util.HashMap[Long, Long] = new java.util.HashMap()

  // ─── Initialization ──────────────────────────────────────────────────

  // Tracks whether the caller explicitly chose a GLFW_PLATFORM (e.g. headless tests pin
  // GLFW_PLATFORM_NULL). If so, init() must not override it with the Linux X11 default (ISS-761).
  private var platformHintSet: Boolean = false

  override def setInitHint(hint: Int, value: Int): Unit = {
    if (hint == WindowingOps.GLFW_PLATFORM) platformHintSet = true
    GlfwC.glfwInitHint(hint, value)
  }

  override def init(): Boolean = {
    // Install a GLFW error callback BEFORE glfwInit so init-time errors surface
    // (glfwSetErrorCallback is valid before glfwInit). LibGDX installs a
    // GLFWErrorCallback at init so GLFW failures are logged rather than dropped.
    GlfwC.glfwSetErrorCallback(fnError)
    // ISS-761: on Linux, force the X11 GLFW platform before glfwInit. SGE's GL context is created
    // by ANGLE/EGL against the native window handle (getNativeWindowHandle), and that EGL path is
    // wired for X11 window IDs; letting GLFW auto-select Wayland would hand back a wl_surface* that
    // the ANGLE/EGL setup does not consume. This is an SGE-original choice (no LibGDX analogue) —
    // upstream lwjgl3 also defaults to X11 in practice. Wayland support stays behind ISS-761.
    // Skipped when the caller already pinned a platform (e.g. headless tests use GLFW_PLATFORM_NULL).
    if (!platformHintSet && System.getProperty("os.name", "").toLowerCase.contains("linux")) {
      GlfwC.glfwInitHint(WindowingOps.GLFW_PLATFORM, WindowingOps.GLFW_PLATFORM_X11)
    }
    GlfwC.glfwInit() != 0
  }

  override def terminate(): Unit =
    GlfwC.glfwTerminate()

  override def platform: Int =
    GlfwC.glfwGetPlatform()

  override def setErrorCallback(callback: Nullable[(Int, String) => Unit]): Unit = {
    // fnError (already installed in init()) dispatches to this field, so just record the callback.
    errorCallback = callback
    GlfwC.glfwSetErrorCallback(fnError)
  }

  // ─── Window lifecycle ────────────────────────────────────────────────

  override def createWindow(width: Int, height: Int, title: String): Long = {
    val zone = Zone.open()
    try {
      val cTitle = toCString(title)(using zone)
      longFromPtr(GlfwC.glfwCreateWindow(width, height, cTitle, null, null))
    } finally zone.close()
  }

  override def createWindow(width: Int, height: Int, title: String, monitorHandle: Long, refreshRate: Int): Long =
    if (monitorHandle == 0L) createWindow(width, height, title)
    else {
      // Faithful to Lwjgl3Application.createGlfwWindow (Lwjgl3Application.java:515-518): set the
      // GLFW_REFRESH_RATE hint, then create the window directly on the target monitor so it opens
      // fullscreen there.
      GlfwC.glfwWindowHint(WindowingOps.GLFW_REFRESH_RATE, refreshRate)
      val zone = Zone.open()
      try {
        val cTitle = toCString(title)(using zone)
        longFromPtr(GlfwC.glfwCreateWindow(width, height, cTitle, ptrFromLong(monitorHandle), null))
      } finally zone.close()
    }

  override def destroyWindow(windowHandle: Long): Unit =
    GlfwC.glfwDestroyWindow(ptrFromLong(windowHandle))

  override def windowShouldClose(windowHandle: Long): Boolean =
    GlfwC.glfwWindowShouldClose(ptrFromLong(windowHandle)) != 0

  override def swapBuffers(windowHandle: Long): Unit =
    GlfwC.glfwSwapBuffers(ptrFromLong(windowHandle))

  override def pollEvents(): Unit =
    GlfwC.glfwPollEvents()

  override def getNativeWindowHandle(windowHandle: Long): Long = {
    val platform = this.platform
    if (platform == WindowingOps.GLFW_PLATFORM_COCOA) {
      val nsWindow = GlfwC.glfwGetCocoaWindow(ptrFromLong(windowHandle))
      // ANGLE's Metal backend needs a CALayer, not an NSWindow.
      // Extract the contentView's layer via Objective-C runtime, matching the JVM implementation.
      val zone = Zone.open()
      try {
        val selContentView = ObjCRuntime.selRegisterName(toCString("contentView")(using zone))
        val contentView    = ObjCRuntime.msgSend(nsWindow, selContentView, null)

        // Enable layer-backing on the contentView (needed for ANGLE Metal)
        val selSetWantsLayer = ObjCRuntime.selRegisterName(toCString("setWantsLayer:")(using zone))
        // YES = 1, encoded as pointer-sized value (ABI-safe on arm64)
        val yes = fromRawPtr[Byte](Intrinsics.castLongToRawPtr(1L))
        ObjCRuntime.msgSend(contentView, selSetWantsLayer, yes)

        // Get the CALayer
        val selLayer = ObjCRuntime.selRegisterName(toCString("layer")(using zone))
        val layer    = ObjCRuntime.msgSend(contentView, selLayer, null)

        // Set contentsScale to match the display's backing scale factor (Retina support).
        // Without this, ANGLE's Metal backend renders at 1x into the CALayer, producing
        // a tiny canvas in the bottom-left corner on HiDPI displays.
        val (fbW, _)  = getFramebufferSize(windowHandle)
        val (winW, _) = getWindowSize(windowHandle)
        val scale: CDouble = if (winW > 0) fbW.toDouble / winW.toDouble else 1.0
        val selSetContentsScale = ObjCRuntime.selRegisterName(toCString("setContentsScale:")(using zone))
        ObjCRuntime.msgSendDouble(layer, selSetContentsScale, scale)

        cachedLayerAddresses.put(windowHandle, longFromPtr(layer))
        longFromPtr(layer)
      } finally zone.close()
    } else if (platform == WindowingOps.GLFW_PLATFORM_X11)
      GlfwC.glfwGetX11Window(ptrFromLong(windowHandle)).toLong
    else if (platform == WindowingOps.GLFW_PLATFORM_WIN32)
      longFromPtr(GlfwC.glfwGetWin32Window(ptrFromLong(windowHandle)))
    else if (platform == WindowingOps.GLFW_PLATFORM_WAYLAND)
      // Wayland (ISS-761): a Wayland session would need the wl_surface* (glfwGetWaylandWindow) for
      // EGL. The shipped sn-provider GLFW build does NOT include Wayland support, so that extern
      // symbol is absent — and Scala Native links @extern symbols EAGERLY at nativeLink time, which
      // would break every native link (undefined reference to glfwGetWaylandWindow). We therefore do
      // NOT bind that extern here. SGE forces the X11 GLFW platform on Linux in init() (see above),
      // so this branch stays dormant on the ANGLE/EGL path; keeping it total (ISS-761's contract)
      // means throwing rather than silently mislinking.
      throw new UnsupportedOperationException(
        "Wayland window handles require a Wayland-enabled GLFW build, which the shipped sn-provider does not include (ISS-761); the X11 init hint keeps GLFW off Wayland by default"
      )
    else
      throw new UnsupportedOperationException(s"getNativeWindowHandle not supported on platform $platform")
  }

  override def beginNoAnimationTransaction(): Unit =
    if (this.platform == WindowingOps.GLFW_PLATFORM_COCOA) {
      val zone = Zone.open()
      try {
        val caTransaction        = ObjCRuntime.objcGetClass(toCString("CATransaction")(using zone))
        val selBegin             = ObjCRuntime.selRegisterName(toCString("begin")(using zone))
        val selSetDisableActions = ObjCRuntime.selRegisterName(toCString("setDisableActions:")(using zone))
        ObjCRuntime.msgSend(caTransaction, selBegin, null) // [CATransaction begin]
        // YES = 1, encoded as a pointer-sized value (ABI-safe on arm64)
        val yes = fromRawPtr[Byte](Intrinsics.castLongToRawPtr(1L))
        ObjCRuntime.msgSend(caTransaction, selSetDisableActions, yes) // [CATransaction setDisableActions:YES]
      } finally zone.close()
    }

  override def commitTransaction(): Unit =
    if (this.platform == WindowingOps.GLFW_PLATFORM_COCOA) {
      val zone = Zone.open()
      try {
        val caTransaction = ObjCRuntime.objcGetClass(toCString("CATransaction")(using zone))
        val selCommit     = ObjCRuntime.selRegisterName(toCString("commit")(using zone))
        ObjCRuntime.msgSend(caTransaction, selCommit, null) // [CATransaction commit]
      } finally zone.close()
    }

  override def updateNativeLayerScale(windowHandle: Long): Unit =
    if (this.platform == WindowingOps.GLFW_PLATFORM_COCOA) {
      // Re-fetch the layer from the NSWindow for THIS window handle each call, so
      // multi-window apps scale the correct window (not whichever was created last).
      val nsWindow  = GlfwC.glfwGetCocoaWindow(ptrFromLong(windowHandle))
      val (fbW, _)  = getFramebufferSize(windowHandle)
      val (winW, _) = getWindowSize(windowHandle)
      val scale: CDouble = if (winW > 0) fbW.toDouble / winW.toDouble else 1.0
      val zone = Zone.open()
      try {
        val selContentView = ObjCRuntime.selRegisterName(toCString("contentView")(using zone))
        val contentView    = ObjCRuntime.msgSend(nsWindow, selContentView, null)
        val selLayer       = ObjCRuntime.selRegisterName(toCString("layer")(using zone))
        val layer          = ObjCRuntime.msgSend(contentView, selLayer, null)
        val selScale       = ObjCRuntime.selRegisterName(toCString("setContentsScale:")(using zone))
        ObjCRuntime.msgSendDouble(layer, selScale, scale)
        // ANGLE's Metal backend creates a CAMetalLayer sublayer whose contentsScale
        // must also be updated — it only reads the parent's scale at creation time.
        val selSublayers = ObjCRuntime.selRegisterName(toCString("sublayers")(using zone))
        val sublayers    = ObjCRuntime.msgSend(layer, selSublayers, null)
        if (longFromPtr(sublayers) != 0L) {
          val selCount = ObjCRuntime.selRegisterName(toCString("count")(using zone))
          // NSUInteger count is 64-bit on macOS (LP64) — widen to Long for the loop.
          val count            = ObjCRuntime.msgSendCount(sublayers, selCount).toLong
          val selObjectAtIndex = ObjCRuntime.selRegisterName(toCString("objectAtIndex:")(using zone))
          var i                = 0L
          while (i < count) {
            val sublayer = ObjCRuntime.msgSendIndex(sublayers, selObjectAtIndex, i.toCSize)
            ObjCRuntime.msgSendDouble(sublayer, selScale, scale)
            i += 1
          }
        }
        cachedLayerAddresses.put(windowHandle, longFromPtr(layer))
      } finally zone.close()
    }

  // ─── Window properties ──────────────────────────────────────────────

  override def setWindowTitle(windowHandle: Long, title: String): Unit = {
    val zone = Zone.open()
    try GlfwC.glfwSetWindowTitle(ptrFromLong(windowHandle), toCString(title)(using zone))
    finally zone.close()
  }

  override def getWindowSize(windowHandle: Long): (Int, Int) = {
    val w = stackalloc[CInt]()
    val h = stackalloc[CInt]()
    GlfwC.glfwGetWindowSize(ptrFromLong(windowHandle), w, h)
    (!w, !h)
  }

  override def setWindowSize(windowHandle: Long, width: Int, height: Int): Unit =
    GlfwC.glfwSetWindowSize(ptrFromLong(windowHandle), width, height)

  override def getWindowPos(windowHandle: Long): (Int, Int) = {
    val x = stackalloc[CInt]()
    val y = stackalloc[CInt]()
    GlfwC.glfwGetWindowPos(ptrFromLong(windowHandle), x, y)
    (!x, !y)
  }

  override def setWindowPos(windowHandle: Long, x: Int, y: Int): Unit =
    GlfwC.glfwSetWindowPos(ptrFromLong(windowHandle), x, y)

  override def getFramebufferSize(windowHandle: Long): (Int, Int) = {
    val w = stackalloc[CInt]()
    val h = stackalloc[CInt]()
    GlfwC.glfwGetFramebufferSize(ptrFromLong(windowHandle), w, h)
    (!w, !h)
  }

  override def iconifyWindow(windowHandle: Long): Unit =
    GlfwC.glfwIconifyWindow(ptrFromLong(windowHandle))

  override def restoreWindow(windowHandle: Long): Unit =
    GlfwC.glfwRestoreWindow(ptrFromLong(windowHandle))

  override def maximizeWindow(windowHandle: Long): Unit =
    GlfwC.glfwMaximizeWindow(ptrFromLong(windowHandle))

  override def showWindow(windowHandle: Long): Unit =
    GlfwC.glfwShowWindow(ptrFromLong(windowHandle))

  override def hideWindow(windowHandle: Long): Unit =
    GlfwC.glfwHideWindow(ptrFromLong(windowHandle))

  override def focusWindow(windowHandle: Long): Unit =
    GlfwC.glfwFocusWindow(ptrFromLong(windowHandle))

  // ─── Clipboard ──────────────────────────────────────────────────────

  override def getClipboardString(windowHandle: Long): String = {
    val cs = GlfwC.glfwGetClipboardString(ptrFromLong(windowHandle))
    if (cs == null) null else fromCString(cs, UTF8)
  }

  override def setClipboardString(windowHandle: Long, content: String): Unit = {
    val zone = Zone.open()
    try GlfwC.glfwSetClipboardString(ptrFromLong(windowHandle), toCString(content)(using zone))
    finally zone.close()
  }

  // ─── Input mode ─────────────────────────────────────────────────────

  override def getInputMode(windowHandle: Long, mode: Int): Int =
    GlfwC.glfwGetInputMode(ptrFromLong(windowHandle), mode)

  override def setInputMode(windowHandle: Long, mode: Int, value: Int): Unit =
    GlfwC.glfwSetInputMode(ptrFromLong(windowHandle), mode, value)

  // ─── Cursor ─────────────────────────────────────────────────────────

  override def createStandardCursor(shape: Int): Long =
    longFromPtr(GlfwC.glfwCreateStandardCursor(shape))

  override def createCursor(pixmap: sge.graphics.Pixmap, xHotspot: Int, yHotspot: Int): Long = {
    // Build a GLFWimage { int width; int height; unsigned char* pixels; } (16 bytes on 64-bit) from
    // the pixmap and call glfwCreateCursor(image, xhot, yhot) (Lwjgl3Cursor.java:72-76). GLFW copies
    // the pixel data before returning, so the zone-allocated buffer is freed once the call returns.
    val zone = Zone.open()
    try {
      // pixmap.pixels is SHARED with the caller and with the Pixmap's own state; read it WITHOUT
      // advancing its position by draining a duplicate() view (independent position/limit/mark),
      // mirroring the JVM twin's non-consuming MemorySegment.ofBuffer(pixels) copy
      // (WindowingOpsJvm.scala:609-613). Draining the shared buffer here left its position at the
      // limit, corrupting any later read of the same pixmap (ISS-809).
      val pixels = pixmap.pixels.duplicate()
      pixels.position(0)
      val numBytes  = pixels.remaining()
      val nativeBuf = zone.alloc(numBytes)
      var j         = 0
      while (j < numBytes) {
        !(nativeBuf + j.toLong) = pixels.get().toByte
        j += 1
      }
      val image  = zone.alloc(16)
      val intPtr = image.asInstanceOf[Ptr[CInt]]
      !intPtr = pixmap.width.toInt
      !(intPtr + 1) = pixmap.height.toInt
      val ptrField = (image + 8L).asInstanceOf[Ptr[Ptr[Byte]]]
      !ptrField = nativeBuf
      longFromPtr(GlfwC.glfwCreateCursor(image, xHotspot, yHotspot))
    } finally zone.close()
  }

  override def setCursor(windowHandle: Long, cursorHandle: Long): Unit =
    GlfwC.glfwSetCursor(ptrFromLong(windowHandle), ptrFromLong(cursorHandle))

  override def destroyCursor(cursorHandle: Long): Unit =
    GlfwC.glfwDestroyCursor(ptrFromLong(cursorHandle))

  // ─── Monitor ────────────────────────────────────────────────────────

  override def primaryMonitor: Long =
    longFromPtr(GlfwC.glfwGetPrimaryMonitor())

  override def monitors: Array[Long] = {
    val count = stackalloc[CInt]()
    val ptrs  = GlfwC.glfwGetMonitors(count)
    if (ptrs == null || !count <= 0) Array.empty
    else Array.tabulate(!count)(i => longFromPtr(ptrs(i)))
  }

  override def getMonitorName(monitorHandle: Long): String = {
    val cs = GlfwC.glfwGetMonitorName(ptrFromLong(monitorHandle))
    if (cs == null) "" else fromCString(cs, UTF8)
  }

  override def getMonitorPos(monitorHandle: Long): (Int, Int) = {
    val x = stackalloc[CInt]()
    val y = stackalloc[CInt]()
    GlfwC.glfwGetMonitorPos(ptrFromLong(monitorHandle), x, y)
    (!x, !y)
  }

  // ─── Fullscreen ─────────────────────────────────────────────────────

  override def setWindowMonitor(
    windowHandle:  Long,
    monitorHandle: Long,
    x:             Int,
    y:             Int,
    width:         Int,
    height:        Int,
    refreshRate:   Int
  ): Unit =
    GlfwC.glfwSetWindowMonitor(
      ptrFromLong(windowHandle),
      ptrFromLong(monitorHandle),
      x,
      y,
      width,
      height,
      refreshRate
    )

  override def getWindowMonitor(windowHandle: Long): Long =
    longFromPtr(GlfwC.glfwGetWindowMonitor(ptrFromLong(windowHandle)))

  override def setWindowAttrib(windowHandle: Long, attrib: Int, value: Int): Unit =
    GlfwC.glfwSetWindowAttrib(ptrFromLong(windowHandle), attrib, value)

  override def getWindowAttrib(windowHandle: Long, attrib: Int): Int =
    GlfwC.glfwGetWindowAttrib(ptrFromLong(windowHandle), attrib)

  override def setWindowIcon(windowHandle: Long, images: Array[sge.graphics.Pixmap]): Unit =
    // GLFWimage struct: { int width; int height; unsigned char* pixels; }
    // Each struct is 16 bytes on 64-bit (4 + 4 + 8 for pointer)
    if (images.isEmpty) {
      GlfwC.glfwSetWindowIcon(ptrFromLong(windowHandle), 0, null)
    } else {
      val zone = Zone.open()
      try {
        val structSize = 16 // sizeof(GLFWimage) on 64-bit with alignment
        val buf        = zone.alloc(structSize * images.length)
        var i          = 0
        while (i < images.length) {
          val base   = buf + (i.toLong * structSize.toLong)
          val pixmap = images(i)
          // Drain a duplicate() view so the caller's shared pixmap buffer position is left untouched
          // (ISS-833, same non-destructive read as createCursor above at :494-495).
          val pixels = pixmap.pixels.duplicate()
          pixels.position(0)
          val numBytes  = pixels.remaining()
          val nativeBuf = zone.alloc(numBytes)
          var j         = 0
          while (j < numBytes) {
            !(nativeBuf + j.toLong) = pixels.get().toByte
            j += 1
          }
          // Write GLFWimage fields: width (int), height (int), pixels (ptr)
          val intPtr = base.asInstanceOf[Ptr[CInt]]
          !intPtr = pixmap.width.toInt
          !(intPtr + 1) = pixmap.height.toInt
          val ptrField = (base + 8L).asInstanceOf[Ptr[Ptr[Byte]]]
          !ptrField = nativeBuf
          i += 1
        }
        GlfwC.glfwSetWindowIcon(ptrFromLong(windowHandle), images.length, buf)
      } finally zone.close()
    }

  override def setWindowShouldClose(windowHandle: Long, value: Boolean): Unit =
    GlfwC.glfwSetWindowShouldClose(ptrFromLong(windowHandle), if (value) 1 else 0)

  override def setWindowSizeLimits(
    windowHandle: Long,
    minWidth:     Int,
    minHeight:    Int,
    maxWidth:     Int,
    maxHeight:    Int
  ): Unit =
    GlfwC.glfwSetWindowSizeLimits(ptrFromLong(windowHandle), minWidth, minHeight, maxWidth, maxHeight)

  override def requestWindowAttention(windowHandle: Long): Unit =
    GlfwC.glfwRequestWindowAttention(ptrFromLong(windowHandle))

  // ─── Window hints ───────────────────────────────────────────────────

  override def setWindowHint(hint: Int, value: Int): Unit =
    GlfwC.glfwWindowHint(hint, value)

  override def defaultWindowHints(): Unit =
    GlfwC.glfwDefaultWindowHints()

  // ─── Context ────────────────────────────────────────────────────────

  override def makeContextCurrent(windowHandle: Long): Unit =
    GlfwC.glfwMakeContextCurrent(ptrFromLong(windowHandle))

  override def setSwapInterval(interval: Int): Unit =
    GlfwC.glfwSwapInterval(interval)

  override def extensionSupported(extension: String): Boolean = {
    val zone = Zone.open()
    try GlfwC.glfwExtensionSupported(toCString(extension)(using zone)) != 0
    finally zone.close()
  }

  // ─── Monitor extended ──────────────────────────────────────────────

  override def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int) = {
    val w = stackalloc[CInt]()
    val h = stackalloc[CInt]()
    GlfwC.glfwGetMonitorPhysicalSize(ptrFromLong(monitorHandle), w, h)
    (!w, !h)
  }

  override def getVideoModes(monitorHandle: Long): Array[(Int, Int, Int, Int, Int, Int)] = {
    val count = stackalloc[CInt]()
    val modes = GlfwC.glfwGetVideoModes(ptrFromLong(monitorHandle), count)
    if (modes == null || !count <= 0) Array.empty
    else {
      // GLFWvidmode = 6 consecutive ints (24 bytes)
      val n    = !count
      val ints = modes.asInstanceOf[Ptr[CInt]]
      Array.tabulate(n) { i =>
        val base = i * 6
        (
          ints(base), // width
          ints(base + 1), // height
          ints(base + 5), // refreshRate
          ints(base + 2), // redBits
          ints(base + 3), // greenBits
          ints(base + 4) // blueBits
        )
      }
    }
  }

  override def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int) = {
    val mode = GlfwC.glfwGetVideoMode(ptrFromLong(monitorHandle))
    if (mode == null) (0, 0, 0, 0, 0, 0)
    else {
      val ints = mode.asInstanceOf[Ptr[CInt]]
      (
        ints(0), // width
        ints(1), // height
        ints(5), // refreshRate
        ints(2), // redBits
        ints(3), // greenBits
        ints(4) // blueBits
      )
    }
  }

  // ─── Callback registry ──────────────────────────────────────────────
  // Scala Native CFuncPtr cannot close over local state. We use a global
  // registry keyed by window handle, and static CFuncPtrs that dispatch
  // through the registry.

  import scala.collection.mutable

  private val cbFramebufferSize = mutable.HashMap.empty[Long, (Long, Int, Int) => Unit]
  private val cbWindowFocus     = mutable.HashMap.empty[Long, (Long, Boolean) => Unit]
  private val cbWindowIconify   = mutable.HashMap.empty[Long, (Long, Boolean) => Unit]
  private val cbWindowMaximize  = mutable.HashMap.empty[Long, (Long, Boolean) => Unit]
  private val cbWindowClose     = mutable.HashMap.empty[Long, Long => Unit]
  private val cbDrop            = mutable.HashMap.empty[Long, (Long, Array[String]) => Unit]
  private val cbWindowRefresh   = mutable.HashMap.empty[Long, Long => Unit]
  private val cbKey             = mutable.HashMap.empty[Long, (Long, Int, Int, Int, Int) => Unit]
  private val cbChar            = mutable.HashMap.empty[Long, (Long, Int) => Unit]
  private val cbScroll          = mutable.HashMap.empty[Long, (Long, Double, Double) => Unit]
  private val cbCursorPos       = mutable.HashMap.empty[Long, (Long, Double, Double) => Unit]
  private val cbMouseButton     = mutable.HashMap.empty[Long, (Long, Int, Int, Int) => Unit]

  // Static CFuncPtrs — no closures, dispatch via registry
  private val fnFramebufferSize = CFuncPtr3.fromScalaFunction[Ptr[Byte], CInt, CInt, Unit] { (win, w, h) =>
    val handle = longFromPtr(win); cbFramebufferSize.get(handle).foreach(_(handle, w, h))
  }
  private val fnWindowFocus = CFuncPtr2.fromScalaFunction[Ptr[Byte], CInt, Unit] { (win, f) =>
    val handle = longFromPtr(win); cbWindowFocus.get(handle).foreach(_(handle, f != 0))
  }
  private val fnWindowIconify = CFuncPtr2.fromScalaFunction[Ptr[Byte], CInt, Unit] { (win, i) =>
    val handle = longFromPtr(win); cbWindowIconify.get(handle).foreach(_(handle, i != 0))
  }
  private val fnWindowMaximize = CFuncPtr2.fromScalaFunction[Ptr[Byte], CInt, Unit] { (win, m) =>
    val handle = longFromPtr(win); cbWindowMaximize.get(handle).foreach(_(handle, m != 0))
  }
  private val fnWindowClose = CFuncPtr1.fromScalaFunction[Ptr[Byte], Unit] { win =>
    val handle = longFromPtr(win); cbWindowClose.get(handle).foreach(_(handle))
  }
  private val fnDrop = CFuncPtr3.fromScalaFunction[Ptr[Byte], CInt, Ptr[CString], Unit] { (win, count, paths) =>
    val handle = longFromPtr(win)
    cbDrop.get(handle).foreach { cb =>
      val arr = Array.tabulate(count)(i => fromCString(paths(i), UTF8))
      cb(handle, arr)
    }
  }
  private val fnWindowRefresh = CFuncPtr1.fromScalaFunction[Ptr[Byte], Unit] { win =>
    val handle = longFromPtr(win); cbWindowRefresh.get(handle).foreach(_(handle))
  }
  // Application-installed error callback (via setErrorCallback), or Nullable.empty to fall back to
  // logging. Scala Native CFuncPtr cannot close over local state, so the static fnError below
  // dispatches through this field (SGE null idiom: Nullable, not a bare null sentinel — ISS-808).
  private var errorCallback: Nullable[(Int, String) => Unit] = Nullable.empty

  // GLFW error callback: dispatches to the application-installed callback if present, else logs so
  // GLFW failures surface (mirrors LibGDX's GLFWErrorCallback). Static CFuncPtr — outlives init()
  // for the process lifetime.
  private val fnError = CFuncPtr2.fromScalaFunction[CInt, CString, Unit] { (error, description) =>
    val message = if (description == null) "" else fromCString(description, UTF8)
    errorCallback.fold {
      utils.Log.error(s"GLFW error 0x${java.lang.Integer.toHexString(error)}: $message")
    }(cb => cb(error, message))
  }
  private val fnKey = CFuncPtr5.fromScalaFunction[Ptr[Byte], CInt, CInt, CInt, CInt, Unit] { (win, key, scancode, action, mods) =>
    val handle = longFromPtr(win); cbKey.get(handle).foreach(_(handle, key, scancode, action, mods))
  }
  private val fnChar = CFuncPtr2.fromScalaFunction[Ptr[Byte], CUnsignedInt, Unit] { (win, codepoint) =>
    val handle = longFromPtr(win); cbChar.get(handle).foreach(_(handle, codepoint.toInt))
  }
  private val fnScroll = CFuncPtr3.fromScalaFunction[Ptr[Byte], CDouble, CDouble, Unit] { (win, xOff, yOff) =>
    val handle = longFromPtr(win); cbScroll.get(handle).foreach(_(handle, xOff, yOff))
  }
  private val fnCursorPos = CFuncPtr3.fromScalaFunction[Ptr[Byte], CDouble, CDouble, Unit] { (win, x, y) =>
    val handle = longFromPtr(win); cbCursorPos.get(handle).foreach(_(handle, x, y))
  }
  private val fnMouseButton = CFuncPtr4.fromScalaFunction[Ptr[Byte], CInt, CInt, CInt, Unit] { (win, button, action, mods) =>
    val handle = longFromPtr(win); cbMouseButton.get(handle).foreach(_(handle, button, action, mods))
  }

  // ─── Callbacks ──────────────────────────────────────────────────────

  override def setFramebufferSizeCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int) => Unit]): Unit =
    if (callback.isEmpty) { cbFramebufferSize.remove(windowHandle); GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), null) }
    else { cbFramebufferSize(windowHandle) = callback.get; GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), fnFramebufferSize) }

  override def setWindowFocusCallback(windowHandle: Long, callback: Nullable[(Long, Boolean) => Unit]): Unit =
    if (callback.isEmpty) { cbWindowFocus.remove(windowHandle); GlfwC.glfwSetWindowFocusCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowFocus(windowHandle) = callback.get; GlfwC.glfwSetWindowFocusCallback(ptrFromLong(windowHandle), fnWindowFocus) }

  override def setWindowIconifyCallback(windowHandle: Long, callback: Nullable[(Long, Boolean) => Unit]): Unit =
    if (callback.isEmpty) { cbWindowIconify.remove(windowHandle); GlfwC.glfwSetWindowIconifyCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowIconify(windowHandle) = callback.get; GlfwC.glfwSetWindowIconifyCallback(ptrFromLong(windowHandle), fnWindowIconify) }

  override def setWindowMaximizeCallback(windowHandle: Long, callback: Nullable[(Long, Boolean) => Unit]): Unit =
    if (callback.isEmpty) { cbWindowMaximize.remove(windowHandle); GlfwC.glfwSetWindowMaximizeCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowMaximize(windowHandle) = callback.get; GlfwC.glfwSetWindowMaximizeCallback(ptrFromLong(windowHandle), fnWindowMaximize) }

  override def setWindowCloseCallback(windowHandle: Long, callback: Nullable[Long => Unit]): Unit =
    if (callback.isEmpty) { cbWindowClose.remove(windowHandle); GlfwC.glfwSetWindowCloseCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowClose(windowHandle) = callback.get; GlfwC.glfwSetWindowCloseCallback(ptrFromLong(windowHandle), fnWindowClose) }

  override def setDropCallback(windowHandle: Long, callback: Nullable[(Long, Array[String]) => Unit]): Unit =
    if (callback.isEmpty) { cbDrop.remove(windowHandle); GlfwC.glfwSetDropCallback(ptrFromLong(windowHandle), null) }
    else { cbDrop(windowHandle) = callback.get; GlfwC.glfwSetDropCallback(ptrFromLong(windowHandle), fnDrop) }

  override def setWindowRefreshCallback(windowHandle: Long, callback: Nullable[Long => Unit]): Unit =
    if (callback.isEmpty) { cbWindowRefresh.remove(windowHandle); GlfwC.glfwSetWindowRefreshCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowRefresh(windowHandle) = callback.get; GlfwC.glfwSetWindowRefreshCallback(ptrFromLong(windowHandle), fnWindowRefresh) }

  // ─── Input callbacks ────────────────────────────────────────────────

  override def setKeyCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int, Int, Int) => Unit]): Unit =
    if (callback.isEmpty) { cbKey.remove(windowHandle); GlfwC.glfwSetKeyCallback(ptrFromLong(windowHandle), null) }
    else { cbKey(windowHandle) = callback.get; GlfwC.glfwSetKeyCallback(ptrFromLong(windowHandle), fnKey) }

  override def setCharCallback(windowHandle: Long, callback: Nullable[(Long, Int) => Unit]): Unit =
    if (callback.isEmpty) { cbChar.remove(windowHandle); GlfwC.glfwSetCharCallback(ptrFromLong(windowHandle), null) }
    else { cbChar(windowHandle) = callback.get; GlfwC.glfwSetCharCallback(ptrFromLong(windowHandle), fnChar) }

  override def setScrollCallback(windowHandle: Long, callback: Nullable[(Long, Double, Double) => Unit]): Unit =
    if (callback.isEmpty) { cbScroll.remove(windowHandle); GlfwC.glfwSetScrollCallback(ptrFromLong(windowHandle), null) }
    else { cbScroll(windowHandle) = callback.get; GlfwC.glfwSetScrollCallback(ptrFromLong(windowHandle), fnScroll) }

  override def setCursorPosCallback(windowHandle: Long, callback: Nullable[(Long, Double, Double) => Unit]): Unit =
    if (callback.isEmpty) { cbCursorPos.remove(windowHandle); GlfwC.glfwSetCursorPosCallback(ptrFromLong(windowHandle), null) }
    else { cbCursorPos(windowHandle) = callback.get; GlfwC.glfwSetCursorPosCallback(ptrFromLong(windowHandle), fnCursorPos) }

  override def setMouseButtonCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int, Int) => Unit]): Unit =
    if (callback.isEmpty) { cbMouseButton.remove(windowHandle); GlfwC.glfwSetMouseButtonCallback(ptrFromLong(windowHandle), null) }
    else { cbMouseButton(windowHandle) = callback.get; GlfwC.glfwSetMouseButtonCallback(ptrFromLong(windowHandle), fnMouseButton) }

  // ─── Input polling ──────────────────────────────────────────────────

  override def getMouseButton(windowHandle: Long, button: Int): Int =
    GlfwC.glfwGetMouseButton(ptrFromLong(windowHandle), button)

  override def setCursorPos(windowHandle: Long, x: Double, y: Double): Unit =
    GlfwC.glfwSetCursorPos(ptrFromLong(windowHandle), x, y)

  // ─── Time ───────────────────────────────────────────────────────────

  override def time: Double =
    GlfwC.glfwGetTime()
}

/** Platform seam giving shared desktop code a default [[WindowingOps]] for pre-launch monitor/display-mode queries (see `DesktopApplicationConfig` companion). A same-named object exists in the JVM
  * source tree so shared code can reference `sge.platform.DesktopWindowing` uniformly — the same expect/actual pattern SGE uses for `HttpBackendFactoryImpl`.
  */
private[sge] object DesktopWindowing {

  /** Returns the default windowing ops. The caller is responsible for `init()`. On Native the ops are a singleton object, so this simply hands it back. */
  def default(): WindowingOps = WindowingOpsNative
}
