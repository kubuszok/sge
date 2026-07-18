/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import lowlevel.Nullable
import sge.platform.WindowingOps

import scala.collection.mutable.ArrayBuffer

/** ISS-807: GLFW error callback is wired AFTER windowing.init() — red test (reproducer; wave 2026-07-17-E, territory S).
  *
  * The original installs the GLFW error callback BEFORE glfwInit, so error descriptions produced during GLFW initialization itself reach the configured error stream:
  * {{{
  *   errorCallback = GLFWErrorCallback.createPrint(Lwjgl3ApplicationConfiguration.errorStream);
  *   GLFW.glfwSetErrorCallback(errorCallback);
  *   ...
  *   if (!GLFW.glfwInit()) { throw ... }               // Lwjgl3Application.java:84-89
  * }}}
  * SGE's `DesktopApplication` reverses that order: it calls `windowing.init()` FIRST (DesktopApplication.scala:107) and only then installs the error callback (DesktopApplication.scala:113). Any GLFW
  * error emitted during `init()` is therefore routed to the platform's Log-based default callback (WindowingOpsJvm.init) instead of `config.errorStream`.
  *
  * This is a headless structural/contract test: the real `DesktopApplication` constructor is driven through its init block + window creation against a recording [[Iss807OrderingWindowingOps]] that
  * appends every relevant call to a single ordered trace, and is stopped at the `glFactory` seam (pattern: [[DesktopConfigKnobsIss762RedSuite]] / [[DesktopFullscreenIss759RedSuite]]) so no native
  * library is touched. The assertion is on the observed ORDER of `setErrorCallback` vs `init`, not on any print formatting.
  */
class DesktopErrorCallbackOrderingIss807RedSuite extends munit.FunSuite {

  final private class Iss807Listener extends ApplicationListener {
    override def render(): Unit = {}
  }

  private def driveInitAndCaptureTrace(): Vector[String] = {
    val ops    = new Iss807OrderingWindowingOps
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    config.title = "iss807"
    intercept[Iss76xStopBeforeGlInit] {
      new DesktopApplication(
        listenerFactory = new Iss807Listener,
        config = config,
        windowing = ops,
        audioOps = new Iss76xStubAudioOps,
        glOps = new Iss76xStubGlOps,
        glFactory = () => throw new Iss76xStopBeforeGlInit
      )
    }
    assert(ops.trace.contains("init"), "test drive broke: DesktopApplication never called windowing.init()")
    ops.trace.toVector
  }

  test(
    "ISS-807: the GLFW error callback must be installed BEFORE windowing.init() (original: setErrorCallback pre-glfwInit, Lwjgl3Application.java:84-89)"
  ) {
    val trace    = driveInitAndCaptureTrace()
    val initIdx  = trace.indexOf("init")
    val setCbIdx = trace.indexOf("setErrorCallback")

    assert(
      setCbIdx >= 0,
      "DesktopApplication never installed a GLFW error callback at all (expected setErrorCallback wiring config.errorStream). Trace: " +
        trace.mkString("[", ", ", "]")
    )
    assert(
      setCbIdx < initIdx,
      "config.errorStream must be wired to the GLFW error callback BEFORE windowing.init(), so GLFW errors " +
        "raised during initialization reach errorStream (original installs GLFWErrorCallback.createPrint(errorStream) " +
        "before glfwInit, Lwjgl3Application.java:84-89). DesktopApplication instead calls init() at " +
        "DesktopApplication.scala:107 and setErrorCallback only at :113. Observed order: " +
        trace.mkString("[", ", ", "]")
    )
  }
}

/** Records the ordered sequence of the windowing calls relevant to ISS-807 (init, setErrorCallback) plus enough sane window/monitor state to let `DesktopApplication`'s constructor reach the
  * `glFactory` seam. Models one primary monitor (100L) at (0,0) with a single 1920x1080@60 mode.
  */
final private[sge] class Iss807OrderingWindowingOps extends WindowingOps {
  val trace: ArrayBuffer[String] = ArrayBuffer.empty

  private var nextWindowHandle: Long = 800001L
  private val monitorA:         Long = 100L

  // ─── Initialization (ordered) ────────────────────────────────────────
  override def init():                                                      Boolean = { trace += "init"; true }
  override def setErrorCallback(callback: Nullable[(Int, String) => Unit]): Unit    = { trace += "setErrorCallback"; () }
  override def setInitHint(hint:          Int, value: Int):                 Unit    = { trace += "setInitHint"; () }
  override def terminate():                                                 Unit    = ()
  override def platform:                                                    Int     = WindowingOps.GLFW_PLATFORM_NULL

  // ─── Window lifecycle ────────────────────────────────────────────────
  override def createWindow(width: Int, height: Int, title: String): Long = {
    trace += "createWindow"
    val handle = nextWindowHandle
    nextWindowHandle += 1
    handle
  }
  override def destroyWindow(windowHandle:         Long): Unit    = ()
  override def getNativeWindowHandle(windowHandle: Long): Long    = 555L
  override def windowShouldClose(windowHandle:     Long): Boolean = false
  override def swapBuffers(windowHandle:           Long): Unit    = ()
  override def pollEvents():                              Unit    = ()

  // ─── Window properties ───────────────────────────────────────────────
  override def setWindowTitle(windowHandle:     Long, title: String):           Unit       = ()
  override def getWindowSize(windowHandle:      Long):                          (Int, Int) = (640, 480)
  override def setWindowSize(windowHandle:      Long, width: Int, height: Int): Unit       = ()
  override def getWindowPos(windowHandle:       Long):                          (Int, Int) = (10, 10)
  override def setWindowPos(windowHandle:       Long, x:     Int, y:      Int): Unit       = ()
  override def getFramebufferSize(windowHandle: Long):                          (Int, Int) = (640, 480)
  override def iconifyWindow(windowHandle:      Long):                          Unit       = ()
  override def restoreWindow(windowHandle:      Long):                          Unit       = ()
  override def maximizeWindow(windowHandle:     Long):                          Unit       = ()
  override def showWindow(windowHandle:         Long):                          Unit       = ()
  override def hideWindow(windowHandle:         Long):                          Unit       = ()
  override def focusWindow(windowHandle:        Long):                          Unit       = ()

  // ─── Clipboard ───────────────────────────────────────────────────────
  override def getClipboardString(windowHandle: Long):                  String = ""
  override def setClipboardString(windowHandle: Long, content: String): Unit   = ()

  // ─── Input mode ──────────────────────────────────────────────────────
  override def getInputMode(windowHandle: Long, mode: Int):             Int  = 0
  override def setInputMode(windowHandle: Long, mode: Int, value: Int): Unit = ()

  // ─── Cursor ──────────────────────────────────────────────────────────
  override def createStandardCursor(shape: Int):                                               Long = 700001L
  override def createCursor(pixmap:        sge.graphics.Pixmap, xHotspot: Int, yHotspot: Int): Long = 700002L
  override def setCursor(windowHandle:     Long, cursorHandle:            Long):               Unit = ()
  override def destroyCursor(cursorHandle: Long):                                              Unit = ()

  // ─── Monitor ─────────────────────────────────────────────────────────
  override def primaryMonitor:                      Long        = monitorA
  override def monitors:                            Array[Long] = Array(monitorA)
  override def getMonitorName(monitorHandle: Long): String      = "monitor-A"
  override def getMonitorPos(monitorHandle:  Long): (Int, Int)  = (0, 0)

  // ─── Fullscreen ──────────────────────────────────────────────────────
  override def setWindowMonitor(windowHandle:       Long, monitorHandle: Long, x:        Int, y:        Int, width:     Int, height: Int, refreshRate: Int): Unit = ()
  override def getWindowMonitor(windowHandle:       Long):                                                                                                   Long = 0L
  override def setWindowAttrib(windowHandle:        Long, attrib:        Int, value:     Int):                                                               Unit = ()
  override def getWindowAttrib(windowHandle:        Long, attrib:        Int):                                                                               Int  = 0
  override def setWindowIcon(windowHandle:          Long, images:        Array[sge.graphics.Pixmap]):                                                        Unit = ()
  override def setWindowShouldClose(windowHandle:   Long, value:         Boolean):                                                                           Unit = ()
  override def setWindowSizeLimits(windowHandle:    Long, minWidth:      Int, minHeight: Int, maxWidth: Int, maxHeight: Int):                                Unit = ()
  override def requestWindowAttention(windowHandle: Long):                                                                                                   Unit = ()

  // ─── Window hints ────────────────────────────────────────────────────
  override def setWindowHint(hint: Int, value: Int): Unit = ()
  override def defaultWindowHints():                 Unit = ()

  // ─── Context ─────────────────────────────────────────────────────────
  override def makeContextCurrent(windowHandle: Long):   Unit    = ()
  override def setSwapInterval(interval:        Int):    Unit    = ()
  override def extensionSupported(extension:    String): Boolean = false

  // ─── Monitor extended ────────────────────────────────────────────────
  override def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int)                            = (600, 340)
  override def getVideoModes(monitorHandle:          Long): Array[(Int, Int, Int, Int, Int, Int)] = Array(getVideoMode(monitorHandle))
  override def getVideoMode(monitorHandle:           Long): (Int, Int, Int, Int, Int, Int)        = (1920, 1080, 60, 8, 8, 8)

  // ─── Callbacks ───────────────────────────────────────────────────────
  override def setFramebufferSizeCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int) => Unit]):      Unit = ()
  override def setWindowFocusCallback(windowHandle:     Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = ()
  override def setWindowIconifyCallback(windowHandle:   Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = ()
  override def setWindowMaximizeCallback(windowHandle:  Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = ()
  override def setWindowCloseCallback(windowHandle:     Long, callback: Nullable[Long => Unit]):                  Unit = ()
  override def setDropCallback(windowHandle:            Long, callback: Nullable[(Long, Array[String]) => Unit]): Unit = ()
  override def setWindowRefreshCallback(windowHandle:   Long, callback: Nullable[Long => Unit]):                  Unit = ()

  // ─── Input callbacks ─────────────────────────────────────────────────
  override def setKeyCallback(windowHandle:         Long, callback: Nullable[(Long, Int, Int, Int, Int) => Unit]): Unit = ()
  override def setCharCallback(windowHandle:        Long, callback: Nullable[(Long, Int) => Unit]):                Unit = ()
  override def setScrollCallback(windowHandle:      Long, callback: Nullable[(Long, Double, Double) => Unit]):     Unit = ()
  override def setCursorPosCallback(windowHandle:   Long, callback: Nullable[(Long, Double, Double) => Unit]):     Unit = ()
  override def setMouseButtonCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int, Int) => Unit]):      Unit = ()

  // ─── Input polling ───────────────────────────────────────────────────
  override def getMouseButton(windowHandle: Long, button: Int):               Int  = WindowingOps.GLFW_RELEASE
  override def setCursorPos(windowHandle:   Long, x:      Double, y: Double): Unit = ()

  // ─── Time ────────────────────────────────────────────────────────────
  override def time: Double = 0.0
}
