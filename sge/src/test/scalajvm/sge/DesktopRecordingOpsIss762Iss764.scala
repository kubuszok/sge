/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.platform.{ AudioOps, GlOps, WindowingOps }

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/* Shared headless recording/stub fixture for the ISS-762 (dead desktop config knobs) and ISS-764
 * (dead custom-cursor API) red suites (reproducer; wave 2026-07-17-C, territory I).
 *
 * Pattern follows DesktopFullscreenIss759RedSuite: all windowing calls are routed through a
 * recording WindowingOps stub, and the app/window drives are stopped right before GL bindings
 * initialization (the `glFactory` seam runs after `createGlfwWindow` in
 * `DesktopApplication.setupWindow`), so no native library is touched. The suites' assertions are
 * formulated against the observable end state recorded here, not one fixed FFI signature.
 */

/** Marker thrown by the `glFactory` seam to stop `DesktopApplication`'s constructor after window creation but before GL bindings init (which would need a real GL context). */
final private[sge] class Iss76xStopBeforeGlInit extends RuntimeException("test drive stops before GL bindings init")

/** Records every windowing call relevant to ISS-762 (window hints, creation titles, title updates) and ISS-764 (cursor creation and installation); models a single primary monitor (handle 100L) at (0,
  * 0) with one 1920x1080@60 video mode.
  */
final private[sge] class Iss76xRecordingWindowingOps extends WindowingOps {
  val monitorA: Long = 100L

  // (width, height, title) per createWindow call.
  val createWindowCalls:    ArrayBuffer[(Int, Int, String)] = ArrayBuffer.empty
  val createdWindowHandles: ArrayBuffer[Long]               = ArrayBuffer.empty

  // (windowHandle, title) per setWindowTitle call.
  val setWindowTitleCalls: ArrayBuffer[(Long, String)] = ArrayBuffer.empty

  // (hint, value) per setInitHint / setWindowHint call.
  val initHintCalls:   ArrayBuffer[(Int, Int)] = ArrayBuffer.empty
  val windowHintCalls: ArrayBuffer[(Int, Int)] = ArrayBuffer.empty

  // (windowHandle, monitorHandle, x, y, width, height, refreshRate) per setWindowMonitor call.
  val setWindowMonitorCalls: ArrayBuffer[(Long, Long, Int, Int, Int, Int, Int)] = ArrayBuffer.empty

  // ─── Cursor recording (ISS-764) ──────────────────────────────────────
  //
  // NOTE for the implementer (anticipated-new-method pattern, cf. the createWindow note in
  // DesktopFullscreenIss759RedSuite): WindowingOps has no create-cursor-from-image seam yet — only
  // createStandardCursor (WindowingOps.scala:191). The faithful port of Lwjgl3Cursor.java:72-77
  // (glfwCreateCursor over a GLFWImage built from the pixmap) must add one (e.g.
  // `createCursor(pixmap: Pixmap, xHotspot: Int, yHotspot: Int): Long`). When that seam lands,
  // override it here: append the call's (pixmap.width, pixmap.height, xHotspot, yHotspot) to
  // `imageCursorCalls`, generate a fresh handle via `newCursorHandle()`, append it to
  // `createdImageCursorHandles`, and return it. The ISS-764 assertions are formulated against this
  // observable end state (recorded creation + the returned handle flowing into setCursor), not one
  // fixed signature.

  // (width, height, xHotspot, yHotspot) per create-cursor-from-image call.
  val imageCursorCalls: ArrayBuffer[(Int, Int, Int, Int)] = ArrayBuffer.empty

  // Handles returned by create-cursor-from-image calls, in order.
  val createdImageCursorHandles: ArrayBuffer[Long] = ArrayBuffer.empty

  // Shape constants per createStandardCursor call.
  val standardCursorCalls: ArrayBuffer[Int] = ArrayBuffer.empty

  // (windowHandle, cursorHandle) per setCursor call.
  val setCursorCalls: ArrayBuffer[(Long, Long)] = ArrayBuffer.empty

  // Handles per destroyCursor call.
  val destroyedCursors: ArrayBuffer[Long] = ArrayBuffer.empty

  private var nextCursorHandle: Long = 700001L

  /** Hands out a fresh fake native cursor handle. */
  def newCursorHandle(): Long = {
    val handle = nextCursorHandle
    nextCursorHandle += 1
    handle
  }

  // windowHandle -> title the windowing layer last saw for it (creation title or setWindowTitle).
  private val windowTitle: mutable.Map[Long, String] = mutable.Map.empty

  /** The title the windowing layer last saw for the window: the createWindow title unless overridden by a later setWindowTitle. */
  def effectiveTitleOf(windowHandle: Long): String = windowTitle.getOrElse(windowHandle, "")

  // windowHandle -> monitor the window is currently fullscreen on (0 = windowed).
  private val fullscreenMonitor: mutable.Map[Long, Long] = mutable.Map.empty

  private var nextWindowHandle: Long = 900001L

  // The last error callback installed via setErrorCallback (ISS-762 errorStream), or empty.
  var errorCallback: lowlevel.Nullable[(Int, String) => Unit] = lowlevel.Nullable.empty

  // ─── Initialization ──────────────────────────────────────────────────
  override def setInitHint(hint: Int, value: Int):                                   Unit    = initHintCalls += ((hint, value))
  override def init():                                                               Boolean = true
  override def terminate():                                                          Unit    = {}
  override def platform:                                                             Int     = WindowingOps.GLFW_PLATFORM_NULL
  override def setErrorCallback(callback: lowlevel.Nullable[(Int, String) => Unit]): Unit    =
    errorCallback = callback

  // ─── Window lifecycle ────────────────────────────────────────────────
  override def createWindow(width: Int, height: Int, title: String): Long = {
    createWindowCalls += ((width, height, title))
    val handle = nextWindowHandle
    nextWindowHandle += 1
    createdWindowHandles += handle
    windowTitle.update(handle, title)
    handle
  }
  override def destroyWindow(windowHandle:         Long): Unit    = {}
  override def getNativeWindowHandle(windowHandle: Long): Long    = 555L
  override def windowShouldClose(windowHandle:     Long): Boolean = false
  override def swapBuffers(windowHandle:           Long): Unit    = {}
  override def pollEvents():                              Unit    = {}

  // ─── Window properties ───────────────────────────────────────────────
  override def setWindowTitle(windowHandle: Long, title: String): Unit = {
    setWindowTitleCalls += ((windowHandle, title))
    windowTitle.update(windowHandle, title)
  }
  override def getWindowSize(windowHandle:      Long):                          (Int, Int) = (640, 480)
  override def setWindowSize(windowHandle:      Long, width: Int, height: Int): Unit       = {}
  override def getWindowPos(windowHandle:       Long):                          (Int, Int) = (10, 10)
  override def setWindowPos(windowHandle:       Long, x:     Int, y:      Int): Unit       = {}
  override def getFramebufferSize(windowHandle: Long):                          (Int, Int) = (640, 480)
  override def iconifyWindow(windowHandle:      Long):                          Unit       = {}
  override def restoreWindow(windowHandle:      Long):                          Unit       = {}
  override def maximizeWindow(windowHandle:     Long):                          Unit       = {}
  override def showWindow(windowHandle:         Long):                          Unit       = {}
  override def hideWindow(windowHandle:         Long):                          Unit       = {}
  override def focusWindow(windowHandle:        Long):                          Unit       = {}

  // ─── Clipboard ───────────────────────────────────────────────────────
  override def getClipboardString(windowHandle: Long):                  String = ""
  override def setClipboardString(windowHandle: Long, content: String): Unit   = {}

  // ─── Input mode ──────────────────────────────────────────────────────
  override def getInputMode(windowHandle: Long, mode: Int):             Int  = 0
  override def setInputMode(windowHandle: Long, mode: Int, value: Int): Unit = {}

  // ─── Cursor ──────────────────────────────────────────────────────────
  override def createStandardCursor(shape: Int): Long = {
    standardCursorCalls += shape
    newCursorHandle()
  }
  // ISS-764 anticipated-new-method override (see the note above): record the create-cursor-from-image
  // call carrying the pixmap dimensions + hotspot, hand out a fresh handle, and remember it so the
  // suite can assert that same handle flows into setCursor.
  override def createCursor(pixmap: sge.graphics.Pixmap, xHotspot: Int, yHotspot: Int): Long = {
    imageCursorCalls += ((pixmap.width.toInt, pixmap.height.toInt, xHotspot, yHotspot))
    val handle = newCursorHandle()
    createdImageCursorHandles += handle
    handle
  }
  override def setCursor(windowHandle: Long, cursorHandle: Long): Unit =
    setCursorCalls += ((windowHandle, cursorHandle))
  override def destroyCursor(cursorHandle: Long): Unit =
    destroyedCursors += cursorHandle

  // ─── Monitor ─────────────────────────────────────────────────────────
  override def primaryMonitor:                      Long        = monitorA
  override def monitors:                            Array[Long] = Array(monitorA)
  override def getMonitorName(monitorHandle: Long): String      = "monitor-A"
  override def getMonitorPos(monitorHandle:  Long): (Int, Int)  = (0, 0)

  // ─── Fullscreen ──────────────────────────────────────────────────────
  override def setWindowMonitor(windowHandle: Long, monitorHandle: Long, x: Int, y: Int, width: Int, height: Int, refreshRate: Int): Unit = {
    setWindowMonitorCalls += ((windowHandle, monitorHandle, x, y, width, height, refreshRate))
    if (monitorHandle == 0L) {
      fullscreenMonitor.remove(windowHandle)
      ()
    } else {
      fullscreenMonitor.update(windowHandle, monitorHandle)
    }
  }
  override def getWindowMonitor(windowHandle:       Long):                                                               Long = fullscreenMonitor.getOrElse(windowHandle, 0L)
  override def setWindowAttrib(windowHandle:        Long, attrib:   Int, value:     Int):                                Unit = {}
  override def getWindowAttrib(windowHandle:        Long, attrib:   Int):                                                Int  = 0
  override def setWindowIcon(windowHandle:          Long, images:   Array[sge.graphics.Pixmap]):                         Unit = {}
  override def setWindowShouldClose(windowHandle:   Long, value:    Boolean):                                            Unit = {}
  override def setWindowSizeLimits(windowHandle:    Long, minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int): Unit = {}
  override def requestWindowAttention(windowHandle: Long):                                                               Unit = {}

  // ─── Window hints ────────────────────────────────────────────────────
  override def setWindowHint(hint: Int, value: Int): Unit = windowHintCalls += ((hint, value))
  override def defaultWindowHints():                 Unit = {}

  // ─── Context ─────────────────────────────────────────────────────────
  override def makeContextCurrent(windowHandle: Long):   Unit    = {}
  override def setSwapInterval(interval:        Int):    Unit    = {}
  override def extensionSupported(extension:    String): Boolean = false

  // ─── Monitor extended ────────────────────────────────────────────────
  override def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int)                            = (600, 340)
  override def getVideoModes(monitorHandle: Long):          Array[(Int, Int, Int, Int, Int, Int)] =
    Array(getVideoMode(monitorHandle))
  override def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int) = (1920, 1080, 60, 8, 8, 8)

  // ─── Callbacks ───────────────────────────────────────────────────────
  override def setFramebufferSizeCallback(windowHandle: Long, callback: (Long, Int, Int) => Unit):      Unit = {}
  override def setWindowFocusCallback(windowHandle:     Long, callback: (Long, Boolean) => Unit):       Unit = {}
  override def setWindowIconifyCallback(windowHandle:   Long, callback: (Long, Boolean) => Unit):       Unit = {}
  override def setWindowMaximizeCallback(windowHandle:  Long, callback: (Long, Boolean) => Unit):       Unit = {}
  override def setWindowCloseCallback(windowHandle:     Long, callback: Long => Unit):                  Unit = {}
  override def setDropCallback(windowHandle:            Long, callback: (Long, Array[String]) => Unit): Unit = {}
  override def setWindowRefreshCallback(windowHandle:   Long, callback: Long => Unit):                  Unit = {}

  // ─── Input callbacks ─────────────────────────────────────────────────
  override def setKeyCallback(windowHandle:         Long, callback: (Long, Int, Int, Int, Int) => Unit): Unit = {}
  override def setCharCallback(windowHandle:        Long, callback: (Long, Int) => Unit):                Unit = {}
  override def setScrollCallback(windowHandle:      Long, callback: (Long, Double, Double) => Unit):     Unit = {}
  override def setCursorPosCallback(windowHandle:   Long, callback: (Long, Double, Double) => Unit):     Unit = {}
  override def setMouseButtonCallback(windowHandle: Long, callback: (Long, Int, Int, Int) => Unit):      Unit = {}

  // ─── Input polling ───────────────────────────────────────────────────
  override def getMouseButton(windowHandle: Long, button: Int):               Int  = WindowingOps.GLFW_RELEASE
  override def setCursorPos(windowHandle:   Long, x:      Double, y: Double): Unit = {}

  // ─── Time ────────────────────────────────────────────────────────────
  override def time: Double = 0.0
}

/** EGL ops stub: hands out a fake context and no-ops everything else. */
final private[sge] class Iss76xStubGlOps extends GlOps {
  override def createContext(windowHandle:   Long, r: Int, g: Int, b: Int, a: Int, depth: Int, stencil: Int, samples: Int, sharedContextHandle: Long): Long = 1L
  override def destroyContext(contextHandle: Long):                                                                                                    Unit = {}
  override def makeCurrent(contextHandle:    Long):                                                                                                    Unit = {}
  override def swapEglBuffers(contextHandle: Long):                                                                                                    Unit = {}
  override def setSwapInterval(interval:     Int):                                                                                                     Unit = {}
  override def getProcAddress(name:          String):                                                                                                  Long = 0L
}

/** Audio ops stub: never exercised (all test configs set `disableAudio = true`; the value is only stored in `PlatformOps.audio`). */
final private[sge] class Iss76xStubAudioOps extends AudioOps {
  override def initEngine(simultaneousSources:         Int, bufferSize:  Int, bufferCount:      Int):                                     Long          = 0L
  override def shutdownEngine(engineHandle:            Long):                                                                             Unit          = {}
  override def updateEngine(engineHandle:              Long):                                                                             Unit          = {}
  override def createSound(engineHandle:               Long, pcmData:    Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int):     Long          = 0L
  override def disposeSound(soundHandle:               Long):                                                                             Unit          = {}
  override def playSound(soundHandle:                  Long, volume:     Float, pitch:          Float, pan:    Float, loop:     Boolean): Long          = 0L
  override def stopSound(instanceId:                   Long):                                                                             Unit          = {}
  override def pauseSound(instanceId:                  Long):                                                                             Unit          = {}
  override def resumeSound(instanceId:                 Long):                                                                             Unit          = {}
  override def stopAllInstances(soundHandle:           Long):                                                                             Unit          = {}
  override def pauseAllInstances(soundHandle:          Long):                                                                             Unit          = {}
  override def resumeAllInstances(soundHandle:         Long):                                                                             Unit          = {}
  override def setSoundVolume(instanceId:              Long, volume:     Float):                                                          Unit          = {}
  override def setSoundPitch(instanceId:               Long, pitch:      Float):                                                          Unit          = {}
  override def setSoundPan(instanceId:                 Long, pan:        Float, volume:         Float):                                   Unit          = {}
  override def setSoundLooping(instanceId:             Long, looping:    Boolean):                                                        Unit          = {}
  override def createMusic(engineHandle:               Long, filePath:   String):                                                         Long          = 0L
  override def disposeMusic(musicHandle:               Long):                                                                             Unit          = {}
  override def playMusic(musicHandle:                  Long):                                                                             Unit          = {}
  override def pauseMusic(musicHandle:                 Long):                                                                             Unit          = {}
  override def stopMusic(musicHandle:                  Long):                                                                             Unit          = {}
  override def isMusicPlaying(musicHandle:             Long):                                                                             Boolean       = false
  override def getMusicVolume(musicHandle:             Long):                                                                             Float         = 0f
  override def setMusicVolume(musicHandle:             Long, volume:     Float):                                                          Unit          = {}
  override def setMusicPitch(musicHandle:              Long, pitch:      Float):                                                          Unit          = {}
  override def setMusicPan(musicHandle:                Long, pan:        Float, volume:         Float):                                   Unit          = {}
  override def isMusicLooping(musicHandle:             Long):                                                                             Boolean       = false
  override def setMusicLooping(musicHandle:            Long, looping:    Boolean):                                                        Unit          = {}
  override def setMusicPosition(musicHandle:           Long, position:   Float):                                                          Unit          = {}
  override def getMusicPosition(musicHandle:           Long):                                                                             Float         = 0f
  override def getMusicDuration(musicHandle:           Long):                                                                             Float         = 0f
  override def createAudioDevice(engineHandle:         Long, sampleRate: Int, isMono:           Boolean):                                 Long          = 0L
  override def disposeAudioDevice(deviceHandle:        Long):                                                                             Unit          = {}
  override def writeAudioDevice(deviceHandle:          Long, data:       Array[Byte], offset:   Int, length:   Int):                      Unit          = {}
  override def setAudioDeviceVolume(deviceHandle:      Long, volume:     Float):                                                          Unit          = {}
  override def pauseAudioDevice(deviceHandle:          Long):                                                                             Unit          = {}
  override def resumeAudioDevice(deviceHandle:         Long):                                                                             Unit          = {}
  override def getAudioDeviceLatency(deviceHandle:     Long):                                                                             Int           = 0
  override def getAvailableOutputDevices(engineHandle: Long):                                                                             Array[String] = Array.empty
  override def switchOutputDevice(engineHandle:        Long, deviceName: String):                                                         Boolean       = false
}

/** Minimal [[DesktopApplicationBase]] host so [[DesktopWindow]]/[[DesktopGraphics]] can be exercised without a running [[DesktopApplication]] (whose constructor blocks in `loop()`). */
final private[sge] class Iss76xStubDesktopApplicationBase(windowing: sge.platform.WindowingOps) extends DesktopApplicationBase {
  override def createAudio(config:               DesktopApplicationConfig): DesktopAudio                = throw new UnsupportedOperationException("not used in this test")
  override def createInput(window:               DesktopWindow):            DesktopInput                = DefaultDesktopInput(window, windowing)
  override def applicationListener:                                         ApplicationListener         = throw new UnsupportedOperationException("not used in this test")
  override def graphics:                                                    Graphics                    = throw new UnsupportedOperationException("not used in this test")
  override def audio:                                                       Audio                       = throw new UnsupportedOperationException("not used in this test")
  override def input:                                                       Input                       = throw new UnsupportedOperationException("not used in this test")
  override def files:                                                       Files                       = throw new UnsupportedOperationException("not used in this test")
  override def net:                                                         Net                         = throw new UnsupportedOperationException("not used in this test")
  override def applicationType:                                             Application.ApplicationType = Application.ApplicationType.Desktop
  override def version:                                                     Int                         = 0
  override def javaHeap:                                                    Long                        = 0L
  override def nativeHeap:                                                  Long                        = 0L
  override def getPreferences(name:              String):                   Preferences                 = throw new UnsupportedOperationException("not used in this test")
  override def clipboard:                                                   sge.utils.Clipboard         = throw new UnsupportedOperationException("not used in this test")
  override def postRunnable(runnable:            Runnable):                 Unit                        = {}
  override def exit():                                                      Unit                        = {}
  override def addLifecycleListener(listener:    LifecycleListener):        Unit                        = {}
  override def removeLifecycleListener(listener: LifecycleListener):        Unit                        = {}
}
