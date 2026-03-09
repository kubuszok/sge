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

import scala.scalanative.runtime.{ Intrinsics, fromRawPtr, toRawPtr }
import scala.scalanative.unsafe.*

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
  def glfwCreateStandardCursor(shape: CInt):                         Ptr[Byte] = extern
  def glfwSetCursor(window:           Ptr[Byte], cursor: Ptr[Byte]): Unit      = extern
  def glfwDestroyCursor(cursor:       Ptr[Byte]):                    Unit      = extern

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

  // Native window handle (glfw3native.h) — platform-specific, may not be available on all targets
  def glfwGetCocoaWindow(window: Ptr[Byte]): Ptr[Byte]  = extern
  def glfwGetX11Window(window:   Ptr[Byte]): CUnsignedLongLong = extern
  def glfwGetWin32Window(window: Ptr[Byte]): Ptr[Byte]  = extern
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

  // ─── Initialization ──────────────────────────────────────────────────

  override def setInitHint(hint: Int, value: Int): Unit =
    GlfwC.glfwInitHint(hint, value)

  override def init(): Boolean =
    GlfwC.glfwInit() != 0

  override def terminate(): Unit =
    GlfwC.glfwTerminate()

  override def getPlatform(): Int =
    GlfwC.glfwGetPlatform()

  // ─── Window lifecycle ────────────────────────────────────────────────

  override def createWindow(width: Int, height: Int, title: String): Long = {
    val zone = Zone.open()
    try {
      val cTitle = toCString(title)(using zone)
      longFromPtr(GlfwC.glfwCreateWindow(width, height, cTitle, null, null))
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
    val platform = getPlatform()
    if (platform == WindowingOps.GLFW_PLATFORM_COCOA)
      longFromPtr(GlfwC.glfwGetCocoaWindow(ptrFromLong(windowHandle)))
    else if (platform == WindowingOps.GLFW_PLATFORM_X11)
      GlfwC.glfwGetX11Window(ptrFromLong(windowHandle)).toLong
    else if (platform == WindowingOps.GLFW_PLATFORM_WIN32)
      longFromPtr(GlfwC.glfwGetWin32Window(ptrFromLong(windowHandle)))
    else
      throw new UnsupportedOperationException(s"getNativeWindowHandle not supported on platform $platform")
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

  override def setCursor(windowHandle: Long, cursorHandle: Long): Unit =
    GlfwC.glfwSetCursor(ptrFromLong(windowHandle), ptrFromLong(cursorHandle))

  override def destroyCursor(cursorHandle: Long): Unit =
    GlfwC.glfwDestroyCursor(ptrFromLong(cursorHandle))

  // ─── Monitor ────────────────────────────────────────────────────────

  override def getPrimaryMonitor(): Long =
    longFromPtr(GlfwC.glfwGetPrimaryMonitor())

  override def getMonitors(): Array[Long] = {
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
          val pixels = pixmap.getPixels()
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
          !intPtr = pixmap.getWidth().toInt
          !(intPtr + 1) = pixmap.getHeight().toInt
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

  override def setFramebufferSizeCallback(windowHandle: Long, callback: (Long, Int, Int) => Unit): Unit =
    if (callback == null) { cbFramebufferSize.remove(windowHandle); GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), null) }
    else { cbFramebufferSize(windowHandle) = callback; GlfwC.glfwSetFramebufferSizeCallback(ptrFromLong(windowHandle), fnFramebufferSize) }

  override def setWindowFocusCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit =
    if (callback == null) { cbWindowFocus.remove(windowHandle); GlfwC.glfwSetWindowFocusCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowFocus(windowHandle) = callback; GlfwC.glfwSetWindowFocusCallback(ptrFromLong(windowHandle), fnWindowFocus) }

  override def setWindowIconifyCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit =
    if (callback == null) { cbWindowIconify.remove(windowHandle); GlfwC.glfwSetWindowIconifyCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowIconify(windowHandle) = callback; GlfwC.glfwSetWindowIconifyCallback(ptrFromLong(windowHandle), fnWindowIconify) }

  override def setWindowMaximizeCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit =
    if (callback == null) { cbWindowMaximize.remove(windowHandle); GlfwC.glfwSetWindowMaximizeCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowMaximize(windowHandle) = callback; GlfwC.glfwSetWindowMaximizeCallback(ptrFromLong(windowHandle), fnWindowMaximize) }

  override def setWindowCloseCallback(windowHandle: Long, callback: Long => Unit): Unit =
    if (callback == null) { cbWindowClose.remove(windowHandle); GlfwC.glfwSetWindowCloseCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowClose(windowHandle) = callback; GlfwC.glfwSetWindowCloseCallback(ptrFromLong(windowHandle), fnWindowClose) }

  override def setDropCallback(windowHandle: Long, callback: (Long, Array[String]) => Unit): Unit =
    if (callback == null) { cbDrop.remove(windowHandle); GlfwC.glfwSetDropCallback(ptrFromLong(windowHandle), null) }
    else { cbDrop(windowHandle) = callback; GlfwC.glfwSetDropCallback(ptrFromLong(windowHandle), fnDrop) }

  override def setWindowRefreshCallback(windowHandle: Long, callback: Long => Unit): Unit =
    if (callback == null) { cbWindowRefresh.remove(windowHandle); GlfwC.glfwSetWindowRefreshCallback(ptrFromLong(windowHandle), null) }
    else { cbWindowRefresh(windowHandle) = callback; GlfwC.glfwSetWindowRefreshCallback(ptrFromLong(windowHandle), fnWindowRefresh) }

  // ─── Input callbacks ────────────────────────────────────────────────

  override def setKeyCallback(windowHandle: Long, callback: (Long, Int, Int, Int, Int) => Unit): Unit =
    if (callback == null) { cbKey.remove(windowHandle); GlfwC.glfwSetKeyCallback(ptrFromLong(windowHandle), null) }
    else { cbKey(windowHandle) = callback; GlfwC.glfwSetKeyCallback(ptrFromLong(windowHandle), fnKey) }

  override def setCharCallback(windowHandle: Long, callback: (Long, Int) => Unit): Unit =
    if (callback == null) { cbChar.remove(windowHandle); GlfwC.glfwSetCharCallback(ptrFromLong(windowHandle), null) }
    else { cbChar(windowHandle) = callback; GlfwC.glfwSetCharCallback(ptrFromLong(windowHandle), fnChar) }

  override def setScrollCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit =
    if (callback == null) { cbScroll.remove(windowHandle); GlfwC.glfwSetScrollCallback(ptrFromLong(windowHandle), null) }
    else { cbScroll(windowHandle) = callback; GlfwC.glfwSetScrollCallback(ptrFromLong(windowHandle), fnScroll) }

  override def setCursorPosCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit =
    if (callback == null) { cbCursorPos.remove(windowHandle); GlfwC.glfwSetCursorPosCallback(ptrFromLong(windowHandle), null) }
    else { cbCursorPos(windowHandle) = callback; GlfwC.glfwSetCursorPosCallback(ptrFromLong(windowHandle), fnCursorPos) }

  override def setMouseButtonCallback(windowHandle: Long, callback: (Long, Int, Int, Int) => Unit): Unit =
    if (callback == null) { cbMouseButton.remove(windowHandle); GlfwC.glfwSetMouseButtonCallback(ptrFromLong(windowHandle), null) }
    else { cbMouseButton(windowHandle) = callback; GlfwC.glfwSetMouseButtonCallback(ptrFromLong(windowHandle), fnMouseButton) }

  // ─── Input polling ──────────────────────────────────────────────────

  override def getMouseButton(windowHandle: Long, button: Int): Int =
    GlfwC.glfwGetMouseButton(ptrFromLong(windowHandle), button)

  override def setCursorPos(windowHandle: Long, x: Double, y: Double): Unit =
    GlfwC.glfwSetCursorPos(ptrFromLong(windowHandle), x, y)

  // ─── Time ───────────────────────────────────────────────────────────

  override def getTime(): Double =
    GlfwC.glfwGetTime()
}
