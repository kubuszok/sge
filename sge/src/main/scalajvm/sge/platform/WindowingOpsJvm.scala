/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original JVM implementation of WindowingOps
 *   Convention: Panama FFM downcall handles to GLFW shared library
 *   Convention: GLFW callbacks use Panama upcall stubs
 *   Convention: String params allocated in confined arenas (auto-freed)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import scala.jdk.CollectionConverters.*

/** JVM implementation of [[WindowingOps]] via Panama FFM downcall handles to the GLFW shared library.
  *
  * Each method creates a downcall handle to the corresponding GLFW C function. Callbacks use Panama upcall stubs that delegate to Scala functions.
  *
  * @param lib
  *   a `SymbolLookup` for the GLFW shared library (e.g. libglfw.dylib / glfw3.dll)
  */
class WindowingOpsJvm(lib: SymbolLookup) extends WindowingOps {

  private val linker: Linker = Linker.nativeLinker()

  private def h(name: String, desc: FunctionDescriptor): MethodHandle =
    linker.downcallHandle(
      lib.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GLFW symbol not found: $name")),
      desc
    )

  // ─── Layout aliases ────────────────────────────────────────────────────

  private val I: ValueLayout.OfInt    = JAVA_INT
  private val D: ValueLayout.OfDouble = JAVA_DOUBLE
  private val P: AddressLayout        = ADDRESS

  // ─── Method handles (lazy to avoid loading unused symbols) ──────────

  private lazy val hInit           = h("glfwInit", FunctionDescriptor.of(I))
  private lazy val hTerminate      = h("glfwTerminate", FunctionDescriptor.ofVoid())
  private lazy val hCreateWindow   = h("glfwCreateWindow", FunctionDescriptor.of(P, I, I, P, P, P))
  private lazy val hDestroyWindow  = h("glfwDestroyWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hShouldClose    = h("glfwWindowShouldClose", FunctionDescriptor.of(I, P))
  private lazy val hSetShouldClose = h("glfwSetWindowShouldClose", FunctionDescriptor.ofVoid(P, I))
  private lazy val hSwapBuffers    = h("glfwSwapBuffers", FunctionDescriptor.ofVoid(P))
  private lazy val hPollEvents     = h("glfwPollEvents", FunctionDescriptor.ofVoid())
  private lazy val hSetTitle       = h("glfwSetWindowTitle", FunctionDescriptor.ofVoid(P, P))
  private lazy val hGetWinSize     = h("glfwGetWindowSize", FunctionDescriptor.ofVoid(P, P, P))
  private lazy val hSetWinSize     = h("glfwSetWindowSize", FunctionDescriptor.ofVoid(P, I, I))
  private lazy val hGetWinPos      = h("glfwGetWindowPos", FunctionDescriptor.ofVoid(P, P, P))
  private lazy val hSetWinPos      = h("glfwSetWindowPos", FunctionDescriptor.ofVoid(P, I, I))
  private lazy val hGetFbSize      = h("glfwGetFramebufferSize", FunctionDescriptor.ofVoid(P, P, P))
  private lazy val hIconify        = h("glfwIconifyWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hRestore        = h("glfwRestoreWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hMaximize       = h("glfwMaximizeWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hShowWindow     = h("glfwShowWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hHideWindow     = h("glfwHideWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hFocusWindow    = h("glfwFocusWindow", FunctionDescriptor.ofVoid(P))
  private lazy val hGetClip        = h("glfwGetClipboardString", FunctionDescriptor.of(P, P))
  private lazy val hSetClip        = h("glfwSetClipboardString", FunctionDescriptor.ofVoid(P, P))
  private lazy val hGetInputMode   = h("glfwGetInputMode", FunctionDescriptor.of(I, P, I))
  private lazy val hSetInputMode   = h("glfwSetInputMode", FunctionDescriptor.ofVoid(P, I, I))
  private lazy val hCreateCursor   = h("glfwCreateStandardCursor", FunctionDescriptor.of(P, I))
  private lazy val hSetCursor      = h("glfwSetCursor", FunctionDescriptor.ofVoid(P, P))
  private lazy val hDestroyCursor  = h("glfwDestroyCursor", FunctionDescriptor.ofVoid(P))
  private lazy val hGetPrimMon     = h("glfwGetPrimaryMonitor", FunctionDescriptor.of(P))
  private lazy val hGetMonitors    = h("glfwGetMonitors", FunctionDescriptor.of(P, P))
  private lazy val hGetMonName     = h("glfwGetMonitorName", FunctionDescriptor.of(P, P))
  private lazy val hGetMonPos      = h("glfwGetMonitorPos", FunctionDescriptor.ofVoid(P, P, P))
  private lazy val hSetWinMon      = h("glfwSetWindowMonitor", FunctionDescriptor.ofVoid(P, P, I, I, I, I, I))
  private lazy val hGetWinMon      = h("glfwGetWindowMonitor", FunctionDescriptor.of(P, P))
  private lazy val hSetWinAttrib   = h("glfwSetWindowAttrib", FunctionDescriptor.ofVoid(P, I, I))
  private lazy val hGetWinAttrib   = h("glfwGetWindowAttrib", FunctionDescriptor.of(I, P, I))
  private lazy val hSetSizeLimits  = h("glfwSetWindowSizeLimits", FunctionDescriptor.ofVoid(P, I, I, I, I))
  private lazy val hReqAttention   = h("glfwRequestWindowAttention", FunctionDescriptor.ofVoid(P))
  private lazy val hMakeCtxCurr    = h("glfwMakeContextCurrent", FunctionDescriptor.ofVoid(P))
  private lazy val hSwapInterval   = h("glfwSwapInterval", FunctionDescriptor.ofVoid(I))
  private lazy val hExtSupported   = h("glfwExtensionSupported", FunctionDescriptor.of(I, P))
  private lazy val hGetMonPhys     = h("glfwGetMonitorPhysicalSize", FunctionDescriptor.ofVoid(P, P, P))
  private lazy val hGetVidModes    = h("glfwGetVideoModes", FunctionDescriptor.of(P, P, P))
  private lazy val hGetVidMode     = h("glfwGetVideoMode", FunctionDescriptor.of(P, P))
  private lazy val hWindowHint     = h("glfwWindowHint", FunctionDescriptor.ofVoid(I, I))
  private lazy val hDefaultHints   = h("glfwDefaultWindowHints", FunctionDescriptor.ofVoid())
  private lazy val hGetMouseBtn    = h("glfwGetMouseButton", FunctionDescriptor.of(I, P, I))
  private lazy val hSetCursorPos   = h("glfwSetCursorPos", FunctionDescriptor.ofVoid(P, D, D))
  private lazy val hGetTime        = h("glfwGetTime", FunctionDescriptor.of(D))
  private lazy val hGetPlatform    = h("glfwGetPlatform", FunctionDescriptor.of(I))
  private lazy val hSetWinIcon     = h("glfwSetWindowIcon", FunctionDescriptor.ofVoid(P, I, P))

  // Callback setters
  private lazy val hSetFbSizeCb   = h("glfwSetFramebufferSizeCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetFocusCb    = h("glfwSetWindowFocusCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetIconifyCb  = h("glfwSetWindowIconifyCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetMaximizeCb = h("glfwSetWindowMaximizeCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetCloseCb    = h("glfwSetWindowCloseCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetDropCb     = h("glfwSetDropCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetRefreshCb  = h("glfwSetWindowRefreshCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetKeyCb      = h("glfwSetKeyCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetCharCb     = h("glfwSetCharCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetScrollCb   = h("glfwSetScrollCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetCurPosCb   = h("glfwSetCursorPosCallback", FunctionDescriptor.of(P, P, P))
  private lazy val hSetMouseBtnCb = h("glfwSetMouseButtonCallback", FunctionDescriptor.of(P, P, P))

  // ─── Helpers ────────────────────────────────────────────────────────

  private def ptr(handle: Long): MemorySegment =
    MemorySegment.ofAddress(handle)

  private def ptrVal(seg: MemorySegment): Long =
    seg.address()

  /** Allocate a UTF-8 C string in the given arena. */
  private def cStr(arena: Arena, s: String): MemorySegment =
    arena.allocateFrom(s)

  /** Read a null-terminated UTF-8 C string from a pointer. */
  private def readCStr(seg: MemorySegment): String =
    seg.reinterpret(Long.MaxValue).getString(0)

  /** Read two ints from output pointers. */
  private def readTwoInts(arena: Arena, invoke: (MemorySegment, MemorySegment) => Unit): (Int, Int) = {
    val px = arena.allocate(I)
    val py = arena.allocate(I)
    invoke(px, py)
    (px.get(I, 0), py.get(I, 0))
  }

  // Upcall stub arena — kept alive for the process lifetime
  private val upcallArena: Arena = Arena.ofAuto()

  // ─── Initialization ────────────────────────────────────────────────────

  private lazy val hInitHint = h("glfwInitHint", FunctionDescriptor.ofVoid(I, I))

  override def setInitHint(hint: Int, value: Int): Unit =
    hInitHint.invoke(hint, value)

  override def init(): Boolean = {
    val result = hInit.invoke().asInstanceOf[Int]
    result != 0
  }

  override def terminate(): Unit =
    hTerminate.invoke()

  override def getPlatform(): Int =
    hGetPlatform.invoke().asInstanceOf[Int]

  // ─── Window lifecycle ──────────────────────────────────────────────────

  override def createWindow(width: Int, height: Int, title: String): Long = {
    val arena = Arena.ofConfined()
    try {
      val result = hCreateWindow.invoke(width, height, cStr(arena, title), MemorySegment.NULL, MemorySegment.NULL)
      ptrVal(result.asInstanceOf[MemorySegment])
    } finally arena.close()
  }

  override def destroyWindow(windowHandle: Long): Unit =
    hDestroyWindow.invoke(ptr(windowHandle))

  override def windowShouldClose(windowHandle: Long): Boolean = {
    val result = hShouldClose.invoke(ptr(windowHandle)).asInstanceOf[Int]
    result != 0
  }

  override def setWindowShouldClose(windowHandle: Long, value: Boolean): Unit =
    hSetShouldClose.invoke(ptr(windowHandle), if (value) 1 else 0)

  override def swapBuffers(windowHandle: Long): Unit =
    hSwapBuffers.invoke(ptr(windowHandle))

  override def pollEvents(): Unit =
    hPollEvents.invoke()

  // ─── Window properties ─────────────────────────────────────────────────

  override def setWindowTitle(windowHandle: Long, title: String): Unit = {
    val arena = Arena.ofConfined()
    try hSetTitle.invoke(ptr(windowHandle), cStr(arena, title))
    finally arena.close()
  }

  override def getWindowSize(windowHandle: Long): (Int, Int) = {
    val arena = Arena.ofConfined()
    try readTwoInts(arena, (px, py) => hGetWinSize.invoke(ptr(windowHandle), px, py))
    finally arena.close()
  }

  override def setWindowSize(windowHandle: Long, width: Int, height: Int): Unit =
    hSetWinSize.invoke(ptr(windowHandle), width, height)

  override def getWindowPos(windowHandle: Long): (Int, Int) = {
    val arena = Arena.ofConfined()
    try readTwoInts(arena, (px, py) => hGetWinPos.invoke(ptr(windowHandle), px, py))
    finally arena.close()
  }

  override def setWindowPos(windowHandle: Long, x: Int, y: Int): Unit =
    hSetWinPos.invoke(ptr(windowHandle), x, y)

  override def getFramebufferSize(windowHandle: Long): (Int, Int) = {
    val arena = Arena.ofConfined()
    try readTwoInts(arena, (px, py) => hGetFbSize.invoke(ptr(windowHandle), px, py))
    finally arena.close()
  }

  override def iconifyWindow(windowHandle: Long): Unit =
    hIconify.invoke(ptr(windowHandle))

  override def restoreWindow(windowHandle: Long): Unit =
    hRestore.invoke(ptr(windowHandle))

  override def maximizeWindow(windowHandle: Long): Unit =
    hMaximize.invoke(ptr(windowHandle))

  override def showWindow(windowHandle: Long): Unit =
    hShowWindow.invoke(ptr(windowHandle))

  override def hideWindow(windowHandle: Long): Unit =
    hHideWindow.invoke(ptr(windowHandle))

  override def focusWindow(windowHandle: Long): Unit =
    hFocusWindow.invoke(ptr(windowHandle))

  override def setWindowSizeLimits(windowHandle: Long, minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int): Unit =
    hSetSizeLimits.invoke(ptr(windowHandle), minWidth, minHeight, maxWidth, maxHeight)

  override def requestWindowAttention(windowHandle: Long): Unit =
    hReqAttention.invoke(ptr(windowHandle))

  override def setWindowAttrib(windowHandle: Long, attrib: Int, value: Int): Unit =
    hSetWinAttrib.invoke(ptr(windowHandle), attrib, value)

  override def getWindowAttrib(windowHandle: Long, attrib: Int): Int =
    hGetWinAttrib.invoke(ptr(windowHandle), attrib).asInstanceOf[Int]

  // ─── Clipboard ─────────────────────────────────────────────────────────

  override def getClipboardString(windowHandle: Long): String = {
    val result = hGetClip.invoke(ptr(windowHandle)).asInstanceOf[MemorySegment]
    if (result == MemorySegment.NULL || result.address() == 0L) null
    else readCStr(result)
  }

  override def setClipboardString(windowHandle: Long, content: String): Unit = {
    val arena = Arena.ofConfined()
    try hSetClip.invoke(ptr(windowHandle), cStr(arena, content))
    finally arena.close()
  }

  // ─── Input mode ────────────────────────────────────────────────────────

  override def getInputMode(windowHandle: Long, mode: Int): Int =
    hGetInputMode.invoke(ptr(windowHandle), mode).asInstanceOf[Int]

  override def setInputMode(windowHandle: Long, mode: Int, value: Int): Unit =
    hSetInputMode.invoke(ptr(windowHandle), mode, value)

  // ─── Cursor ────────────────────────────────────────────────────────────

  override def createStandardCursor(shape: Int): Long =
    ptrVal(hCreateCursor.invoke(shape).asInstanceOf[MemorySegment])

  override def setCursor(windowHandle: Long, cursorHandle: Long): Unit =
    hSetCursor.invoke(ptr(windowHandle), ptr(cursorHandle))

  override def destroyCursor(cursorHandle: Long): Unit =
    hDestroyCursor.invoke(ptr(cursorHandle))

  // ─── Monitor ───────────────────────────────────────────────────────────

  override def getPrimaryMonitor(): Long =
    ptrVal(hGetPrimMon.invoke().asInstanceOf[MemorySegment])

  override def getMonitors(): Array[Long] = {
    val arena = Arena.ofConfined()
    try {
      val countSeg = arena.allocate(I)
      val result   = hGetMonitors.invoke(countSeg).asInstanceOf[MemorySegment]
      if (result == MemorySegment.NULL || result.address() == 0L) Array.empty
      else {
        val count = countSeg.get(I, 0)
        val ptrs  = result.reinterpret(P.byteSize() * count.toLong)
        Array.tabulate(count)(i => ptrs.getAtIndex(P, i.toLong).address())
      }
    } finally arena.close()
  }

  override def getMonitorName(monitorHandle: Long): String = {
    val result = hGetMonName.invoke(ptr(monitorHandle)).asInstanceOf[MemorySegment]
    if (result == MemorySegment.NULL || result.address() == 0L) ""
    else readCStr(result)
  }

  override def getMonitorPos(monitorHandle: Long): (Int, Int) = {
    val arena = Arena.ofConfined()
    try readTwoInts(arena, (px, py) => hGetMonPos.invoke(ptr(monitorHandle), px, py))
    finally arena.close()
  }

  override def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int) = {
    val arena = Arena.ofConfined()
    try readTwoInts(arena, (px, py) => hGetMonPhys.invoke(ptr(monitorHandle), px, py))
    finally arena.close()
  }

  // ─── Fullscreen ────────────────────────────────────────────────────────

  override def setWindowMonitor(windowHandle: Long, monitorHandle: Long, x: Int, y: Int, width: Int, height: Int, refreshRate: Int): Unit =
    hSetWinMon.invoke(ptr(windowHandle), ptr(monitorHandle), x, y, width, height, refreshRate)

  override def getWindowMonitor(windowHandle: Long): Long =
    ptrVal(hGetWinMon.invoke(ptr(windowHandle)).asInstanceOf[MemorySegment])

  // ─── Video modes ───────────────────────────────────────────────────────

  // GLFWvidmode struct: { int width, height, redBits, greenBits, blueBits, refreshRate } = 6 ints = 24 bytes
  private val VidModeSize: Long = 24L

  override def getVideoModes(monitorHandle: Long): Array[(Int, Int, Int, Int, Int, Int)] = {
    val arena = Arena.ofConfined()
    try {
      val countSeg = arena.allocate(I)
      val result   = hGetVidModes.invoke(ptr(monitorHandle), countSeg).asInstanceOf[MemorySegment]
      if (result == MemorySegment.NULL || result.address() == 0L) Array.empty
      else {
        val count = countSeg.get(I, 0)
        val buf   = result.reinterpret(VidModeSize * count.toLong)
        Array.tabulate(count) { i =>
          val off = VidModeSize * i.toLong
          (
            buf.get(I, off),
            buf.get(I, off + 4),
            buf.get(I, off + 20),
            buf.get(I, off + 8),
            buf.get(I, off + 12),
            buf.get(I, off + 16)
          )
        }
      }
    } finally arena.close()
  }

  override def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int) = {
    val result = hGetVidMode.invoke(ptr(monitorHandle)).asInstanceOf[MemorySegment]
    if (result == MemorySegment.NULL || result.address() == 0L) (0, 0, 0, 0, 0, 0)
    else {
      val buf = result.reinterpret(VidModeSize)
      (
        buf.get(I, 0),
        buf.get(I, 4),
        buf.get(I, 20),
        buf.get(I, 8),
        buf.get(I, 12),
        buf.get(I, 16)
      )
    }
  }

  // ─── Window hints ───────────────────────────────────────────────────────

  override def setWindowHint(hint: Int, value: Int): Unit =
    hWindowHint.invoke(hint, value)

  override def defaultWindowHints(): Unit =
    hDefaultHints.invoke()

  // ─── Context ──────────────────────────────────────────────────────────

  override def makeContextCurrent(windowHandle: Long): Unit =
    hMakeCtxCurr.invoke(ptr(windowHandle))

  override def setSwapInterval(interval: Int): Unit =
    hSwapInterval.invoke(interval)

  override def extensionSupported(extension: String): Boolean = {
    val arena = Arena.ofConfined()
    try {
      val result = hExtSupported.invoke(cStr(arena, extension)).asInstanceOf[Int]
      result != 0
    } finally arena.close()
  }

  // ─── Input polling ──────────────────────────────────────────────────

  override def getMouseButton(windowHandle: Long, button: Int): Int =
    hGetMouseBtn.invoke(ptr(windowHandle), button).asInstanceOf[Int]

  override def setCursorPos(windowHandle: Long, x: Double, y: Double): Unit =
    hSetCursorPos.invoke(ptr(windowHandle), x, y)

  // ─── Time ──────────────────────────────────────────────────────────────

  override def getTime(): Double =
    hGetTime.invoke().asInstanceOf[Double]

  // ─── Callbacks (upcall stubs) ──────────────────────────────────────────

  override def setFramebufferSizeCallback(windowHandle: Long, callback: (Long, Int, Int) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, w: Int, h: Int): Unit = callback(win.address(), w, h)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetFbSizeCb.invoke(ptr(windowHandle), stub)
  }

  override def setWindowFocusCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, focused: Int): Unit = callback(win.address(), focused != 0)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetFocusCb.invoke(ptr(windowHandle), stub)
  }

  override def setWindowIconifyCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, iconified: Int): Unit = callback(win.address(), iconified != 0)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetIconifyCb.invoke(ptr(windowHandle), stub)
  }

  override def setWindowMaximizeCallback(windowHandle: Long, callback: (Long, Boolean) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, maximized: Int): Unit = callback(win.address(), maximized != 0)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetMaximizeCb.invoke(ptr(windowHandle), stub)
  }

  override def setWindowCloseCallback(windowHandle: Long, callback: Long => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment): Unit = callback(win.address())
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetCloseCb.invoke(ptr(windowHandle), stub)
  }

  override def setDropCallback(windowHandle: Long, callback: (Long, Array[String]) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I, P)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, count: Int, paths: MemorySegment): Unit = {
                val reinterpreted = paths.reinterpret(P.byteSize() * count.toLong)
                val arr           = Array.tabulate(count) { i =>
                  val strPtr = reinterpreted.getAtIndex(P, i.toLong)
                  strPtr.reinterpret(Long.MaxValue).getString(0)
                }
                callback(win.address(), arr)
              }
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int], classOf[MemorySegment])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetDropCb.invoke(ptr(windowHandle), stub)
  }

  override def setWindowRefreshCallback(windowHandle: Long, callback: Long => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment): Unit = callback(win.address())
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetRefreshCb.invoke(ptr(windowHandle), stub)
  }

  // ─── Input callbacks ─────────────────────────────────────────────────

  override def setKeyCallback(windowHandle: Long, callback: (Long, Int, Int, Int, Int) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I, I, I, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, key: Int, scancode: Int, action: Int, mods: Int): Unit =
                callback(win.address(), key, scancode, action, mods)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(
              classOf[Unit],
              classOf[MemorySegment],
              classOf[Int],
              classOf[Int],
              classOf[Int],
              classOf[Int]
            )
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetKeyCb.invoke(ptr(windowHandle), stub)
  }

  override def setCharCallback(windowHandle: Long, callback: (Long, Int) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, codepoint: Int): Unit = callback(win.address(), codepoint)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetCharCb.invoke(ptr(windowHandle), stub)
  }

  override def setScrollCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, D, D)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, xOff: Double, yOff: Double): Unit =
                callback(win.address(), xOff, yOff)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Double], classOf[Double])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetScrollCb.invoke(ptr(windowHandle), stub)
  }

  override def setCursorPosCallback(windowHandle: Long, callback: (Long, Double, Double) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, D, D)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, x: Double, y: Double): Unit =
                callback(win.address(), x, y)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Double], classOf[Double])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetCurPosCb.invoke(ptr(windowHandle), stub)
  }

  override def setMouseButtonCallback(windowHandle: Long, callback: (Long, Int, Int, Int) => Unit): Unit = {
    val stub =
      if (callback == null) MemorySegment.NULL
      else {
        val desc   = FunctionDescriptor.ofVoid(P, I, I, I)
        val target = java.lang.invoke.MethodHandles
          .lookup()
          .bind(
            new AnyRef {
              @scala.annotation.nowarn("id=E198")
              def invoke(win: MemorySegment, button: Int, action: Int, mods: Int): Unit =
                callback(win.address(), button, action, mods)
            },
            "invoke",
            java.lang.invoke.MethodType.methodType(classOf[Unit], classOf[MemorySegment], classOf[Int], classOf[Int], classOf[Int])
          )
        linker.upcallStub(target, desc, upcallArena)
      }
    hSetMouseBtnCb.invoke(ptr(windowHandle), stub)
  }

  // ─── Window icon ────────────────────────────────────────────────────

  // GLFWimage struct layout: { int width; int height; unsigned char* pixels; }
  // 64-bit: 4 + 4 + 8 = 16 bytes (pointer aligned)
  private val GlfwImageSize: Long = 16L

  override def setWindowIcon(windowHandle: Long, images: Array[sge.graphics.Pixmap]): Unit =
    if (images.isEmpty) {
      hSetWinIcon.invoke(ptr(windowHandle), 0, MemorySegment.NULL)
    } else {
      val arena = Arena.ofConfined()
      try {
        val buf = arena.allocate(GlfwImageSize * images.length)
        var i   = 0
        while (i < images.length) {
          val pixmap = images(i)
          val pixels = pixmap.getPixels()
          pixels.position(0)
          val numBytes  = pixels.remaining()
          val nativeBuf = arena.allocate(numBytes.toLong)
          // Copy pixel bytes from Java ByteBuffer to native memory
          val slice = MemorySegment.ofBuffer(pixels)
          nativeBuf.copyFrom(slice)
          // Write GLFWimage fields
          val base = buf.asSlice(GlfwImageSize * i.toLong, GlfwImageSize)
          base.set(I, 0L, pixmap.getWidth().toInt)
          base.set(I, 4L, pixmap.getHeight().toInt)
          base.set(P, 8L, nativeBuf)
          i += 1
        }
        hSetWinIcon.invoke(ptr(windowHandle), images.length, buf)
      } finally arena.close()
    }
}

object WindowingOpsJvm {

  /** Creates a WindowingOpsJvm from a GLFW library loaded from the system library path.
    * @param libName
    *   the library name (e.g. "glfw", "glfw3")
    */
  def apply(libName: String = "glfw"): WindowingOpsJvm = {
    val found = findLibrary(libName)
    // Use System.load to load the library via the system's dynamic linker,
    // which correctly handles macOS framework dependencies (Cocoa, IOKit, etc.)
    System.load(found.toAbsolutePath.toString)
    val lookup = SymbolLookup.loaderLookup()
    new WindowingOpsJvm(lookup)
  }

  /** Locates a shared library on the library path or in the Homebrew Cellar. */
  private def findLibrary(libName: String): java.nio.file.Path = {
    val mappedName = System.mapLibraryName(libName)
    val libPath    = System.getProperty("java.library.path", "")
    val searchDirs = libPath.split(java.io.File.pathSeparator).toSeq ++
      brewCellarLibDirs(libName)
    // Search for exact mapped name, then versioned dylibs (e.g. libglfw.3.4.dylib)
    val candidates = searchDirs.iterator.flatMap { dir =>
      val base = java.nio.file.Path.of(dir)
      if (!java.nio.file.Files.isDirectory(base)) Iterator.empty
      else {
        val exact = base.resolve(mappedName)
        if (java.nio.file.Files.exists(exact)) Iterator(exact)
        else {
          // Look for versioned variants: libglfw.3.dylib, libglfw.3.4.dylib, etc.
          val prefix = s"lib$libName."
          try {
            val stream = java.nio.file.Files.list(base)
            try
              stream.iterator.nn.asInstanceOf[java.util.Iterator[java.nio.file.Path]].asScala.filter { p =>
                val name = p.getFileName.toString
                name.startsWith(prefix) && name.endsWith(".dylib") && name != mappedName
              }
            finally stream.close()
          } catch {
            case _: java.io.IOException => Iterator.empty
          }
        }
      }
    }
    candidates
      .nextOption()
      .getOrElse(
        throw new UnsatisfiedLinkError(
          s"Cannot find $mappedName in java.library.path: $libPath"
        )
      )
  }

  /** Homebrew Cellar lib directories for the given library name. */
  private def brewCellarLibDirs(libName: String): Seq[String] = {
    val cellar = java.nio.file.Path.of("/opt/homebrew/Cellar", libName)
    if (!java.nio.file.Files.isDirectory(cellar)) Seq.empty
    else
      try {
        val stream = java.nio.file.Files.list(cellar)
        try
          stream.iterator.nn.asInstanceOf[java.util.Iterator[java.nio.file.Path]].asScala.map(_.resolve("lib").toString).toSeq
        finally stream.close()
      } catch {
        case _: java.io.IOException => Seq.empty
      }
  }
}
