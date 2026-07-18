/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import lowlevel.Nullable
import sge.platform.{ AudioOps, GlOps, WindowingOps }

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** ISS-759: desktop fullscreen cluster — red tests (reproducer; wave 2026-07-16-A, territory C).
  *
  * The desktop backend never honors `config.fullscreenMode` at window creation and cannot target a non-current monitor when switching to fullscreen at runtime:
  *
  *   - (a) `DesktopApplication.createGlfwWindow` (DesktopApplication.scala:394-409) creates the window with `config.windowWidth/Height` and no monitor, reading `config.fullscreenMode` only to skip
  *     centering. The original `Lwjgl3Application.createGlfwWindow` (Lwjgl3Application.java:515-518) creates the window with `config.fullscreenMode.width/height` on
  *     `config.fullscreenMode.getMonitor()`.
  *   - (b) `DesktopDisplayMode.toDisplayMode` (DesktopDisplayMode.scala:41) drops `monitorHandle`, so `DesktopGraphics.setFullscreenMode` (DesktopGraphics.scala:312-325) re-resolves every requested
  *     mode against the *current* monitor. The original `Lwjgl3Graphics.setFullscreenMode` (Lwjgl3Graphics.java:414-434) targets `newMode.getMonitor()` — the monitor the mode was queried from.
  *   - (c) `DesktopApplicationConfig`'s companion (DesktopApplicationConfig.scala:239) defers the pre-launch monitor/display-mode queries that the original `Lwjgl3ApplicationConfiguration` provides
  *     as statics (`getDisplayMode`, `getDisplayModes`, `getPrimaryMonitor`, `getMonitors` — Lwjgl3ApplicationConfiguration.java:286-341), although `WindowingOps` already exposes
  *     `monitors`/`getVideoModes` (used by DesktopGraphics.scala:239-256).
  *
  * ISS-761 (Wayland: `WindowingOpsJvm.getNativeWindowHandle` has no `GLFW_PLATFORM_WAYLAND` branch — WindowingOpsJvm.scala:381, twin WindowingOpsNative.scala:287 — and `init()` sets no
  * `GLFW_PLATFORM` init hint) is NOT red-tested here: both live inside the concrete FFI implementations that a recording seam necessarily replaces, and the observed platform comes from the live
  * `glfwGetPlatform()` of a real session. See the reproducer report for the implementer contract.
  *
  * These are headless structural/contract tests: all windowing calls are routed through a recording [[WindowingOps]] stub and the app path is stopped right before GL bindings initialization (the
  * `glFactory` seam runs after `createGlfwWindow` in `DesktopApplication.setupWindow`), so no native library is touched.
  */
class DesktopFullscreenIss759RedSuite extends munit.FunSuite {

  /** Marker thrown by the `glFactory` seam to stop `DesktopApplication`'s constructor after window creation but before GL bindings init (which would need a real GL context). */
  final private class StopBeforeGlInit extends RuntimeException("test drive stops before GL bindings init")

  /** A do-nothing listener; `render()` is the only abstract member and is never reached (the drive stops before the loop). */
  private def listener: ApplicationListener = new ApplicationListener {
    override def render(): Unit = {}
  }

  /** Records every windowing call relevant to the fullscreen cluster; models two monitors:
    *
    *   - monitor A (handle 100L, primary) at (0, 0) with a single 1920x1080@60 mode — the window (created at (10, 10) sized 640x480) sits on it,
    *   - monitor B (handle 200L) at (1920, 0) with a single 2560x1440@144 mode — the fullscreen target in all tests.
    */
  final private class RecordingWindowingOps extends WindowingOps {
    val monitorA: Long = 100L
    val monitorB: Long = 200L

    // (width, height, title) per createWindow call.
    // NOTE for the implementer: the faithful port of Lwjgl3Application.java:515-518 must carry the
    // fullscreen mode's monitor into window creation. When the WindowingOps creation seam gains a
    // monitor parameter (or a monitor-aware variant), record that monitor in `fullscreenMonitor`
    // below so `fullscreenMonitorOf` sees it — the assertions in this suite are formulated against
    // the observable end state (created size + fullscreen monitor), not one fixed signature.
    val createWindowCalls:    ArrayBuffer[(Int, Int, String)] = ArrayBuffer.empty
    val createdWindowHandles: ArrayBuffer[Long]               = ArrayBuffer.empty

    // (windowHandle, monitorHandle, x, y, width, height, refreshRate) per setWindowMonitor call.
    val setWindowMonitorCalls: ArrayBuffer[(Long, Long, Int, Int, Int, Int, Int)] = ArrayBuffer.empty

    // (hint, value) per setInitHint / setWindowHint call.
    val initHintCalls:   ArrayBuffer[(Int, Int)] = ArrayBuffer.empty
    val windowHintCalls: ArrayBuffer[(Int, Int)] = ArrayBuffer.empty

    // windowHandle -> monitor the window is currently fullscreen on (0 = windowed).
    private val fullscreenMonitor: mutable.Map[Long, Long] = mutable.Map.empty

    /** Monitor the window ended up fullscreen on, or 0 if it was never made fullscreen. */
    def fullscreenMonitorOf(windowHandle: Long): Long = fullscreenMonitor.getOrElse(windowHandle, 0L)

    private var nextWindowHandle: Long = 900001L

    // ─── Initialization ──────────────────────────────────────────────────
    override def setInitHint(hint: Int, value: Int): Unit    = initHintCalls += ((hint, value))
    override def init():                             Boolean = true
    override def terminate():                        Unit    = {}
    override def platform:                           Int     = WindowingOps.GLFW_PLATFORM_NULL

    // ─── Window lifecycle ────────────────────────────────────────────────
    override def createWindow(width: Int, height: Int, title: String): Long = {
      createWindowCalls += ((width, height, title))
      val handle = nextWindowHandle
      nextWindowHandle += 1
      createdWindowHandles += handle
      handle
    }
    override def destroyWindow(windowHandle:         Long): Unit    = {}
    override def getNativeWindowHandle(windowHandle: Long): Long    = 555L
    override def windowShouldClose(windowHandle:     Long): Boolean = false
    override def swapBuffers(windowHandle:           Long): Unit    = {}
    override def pollEvents():                              Unit    = {}

    // ─── Window properties ───────────────────────────────────────────────
    override def setWindowTitle(windowHandle:     Long, title: String):           Unit       = {}
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
    override def createStandardCursor(shape: Int):                      Long = 0L
    override def setCursor(windowHandle:     Long, cursorHandle: Long): Unit = {}
    override def destroyCursor(cursorHandle: Long):                     Unit = {}

    // ─── Monitor ─────────────────────────────────────────────────────────
    override def primaryMonitor:                      Long        = monitorA
    override def monitors:                            Array[Long] = Array(monitorA, monitorB)
    override def getMonitorName(monitorHandle: Long): String      =
      if (monitorHandle == monitorB) "monitor-B" else "monitor-A"
    override def getMonitorPos(monitorHandle: Long): (Int, Int) =
      if (monitorHandle == monitorB) (1920, 0) else (0, 0)

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
    override def getWindowMonitor(windowHandle:       Long):                                                               Long = fullscreenMonitorOf(windowHandle)
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
    override def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int) =
      if (monitorHandle == monitorB) (2560, 1440, 144, 8, 8, 8) else (1920, 1080, 60, 8, 8, 8)

    // ─── Callbacks ───────────────────────────────────────────────────────
    override def setFramebufferSizeCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int) => Unit]):      Unit = {}
    override def setWindowFocusCallback(windowHandle:     Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = {}
    override def setWindowIconifyCallback(windowHandle:   Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = {}
    override def setWindowMaximizeCallback(windowHandle:  Long, callback: Nullable[(Long, Boolean) => Unit]):       Unit = {}
    override def setWindowCloseCallback(windowHandle:     Long, callback: Nullable[Long => Unit]):                  Unit = {}
    override def setDropCallback(windowHandle:            Long, callback: Nullable[(Long, Array[String]) => Unit]): Unit = {}
    override def setWindowRefreshCallback(windowHandle:   Long, callback: Nullable[Long => Unit]):                  Unit = {}

    // ─── Input callbacks ─────────────────────────────────────────────────
    override def setKeyCallback(windowHandle:         Long, callback: Nullable[(Long, Int, Int, Int, Int) => Unit]): Unit = {}
    override def setCharCallback(windowHandle:        Long, callback: Nullable[(Long, Int) => Unit]):                Unit = {}
    override def setScrollCallback(windowHandle:      Long, callback: Nullable[(Long, Double, Double) => Unit]):     Unit = {}
    override def setCursorPosCallback(windowHandle:   Long, callback: Nullable[(Long, Double, Double) => Unit]):     Unit = {}
    override def setMouseButtonCallback(windowHandle: Long, callback: Nullable[(Long, Int, Int, Int) => Unit]):      Unit = {}

    // ─── Input polling ───────────────────────────────────────────────────
    override def getMouseButton(windowHandle: Long, button: Int):               Int  = WindowingOps.GLFW_RELEASE
    override def setCursorPos(windowHandle:   Long, x:      Double, y: Double): Unit = {}

    // ─── Time ────────────────────────────────────────────────────────────
    override def time: Double = 0.0
  }

  /** EGL ops stub: hands out a fake context and no-ops everything else. */
  final private class StubGlOps extends GlOps {
    override def createContext(windowHandle:   Long, r: Int, g: Int, b: Int, a: Int, depth: Int, stencil: Int, samples: Int, sharedContextHandle: Long): Long = 1L
    override def destroyContext(contextHandle: Long):                                                                                                    Unit = {}
    override def makeCurrent(contextHandle:    Long):                                                                                                    Unit = {}
    override def swapEglBuffers(contextHandle: Long):                                                                                                    Unit = {}
    override def setSwapInterval(interval:     Int):                                                                                                     Unit = {}
    override def getProcAddress(name:          String):                                                                                                  Long = 0L
  }

  /** Audio ops stub: never exercised (all test configs set `disableAudio = true`; the value is only stored in `PlatformOps.audio`). */
  final private class StubAudioOps extends AudioOps {
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
  final private class StubDesktopApplicationBase(windowing: WindowingOps) extends DesktopApplicationBase {
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

  /** Drives the real `DesktopApplication` constructor through window creation (`createGlfwWindow` + `setupWindow`) against the recording ops, stopping at the `glFactory` seam. */
  private def driveAppCreation(config: DesktopApplicationConfig): RecordingWindowingOps = {
    val ops = new RecordingWindowingOps
    intercept[StopBeforeGlInit] {
      new DesktopApplication(
        listenerFactory = listener,
        config = config,
        windowing = ops,
        audioOps = new StubAudioOps,
        glOps = new StubGlOps,
        glFactory = () => throw new StopBeforeGlInit
      )
    }
    assert(ops.createWindowCalls.nonEmpty, "test drive broke: DesktopApplication never reached window creation")
    ops
  }

  private def fullscreenConfig(ops: RecordingWindowingOps): DesktopApplicationConfig = {
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    config.title = "iss759"
    // The application must open fullscreen at 2560x1440@144 on monitor B (not the primary monitor A).
    config.fullscreenMode = Nullable(DesktopDisplayMode(ops.monitorB, 2560, 1440, 144, 24))
    config
  }

  test("ISS-759 (a): window creation honors config.fullscreenMode's size (Lwjgl3Application.java:515-518)") {
    val probe              = new RecordingWindowingOps // only for monitor handles in the config
    val ops                = driveAppCreation(fullscreenConfig(probe))
    val (width, height, _) = ops.createWindowCalls.head
    assertEquals(
      (width, height),
      (2560, 1440),
      "config.fullscreenMode is set, so the window must be created with the fullscreen mode's size " +
        "(original: glfwCreateWindow(config.fullscreenMode.width, config.fullscreenMode.height, ...), " +
        "Lwjgl3Application.java:515-518); DesktopApplication.scala:394 passes config.windowWidth/Height instead"
    )
  }

  test("ISS-759 (a): window creation targets config.fullscreenMode's monitor (Lwjgl3Application.java:515-518)") {
    val probe        = new RecordingWindowingOps
    val ops          = driveAppCreation(fullscreenConfig(probe))
    val windowHandle = ops.createdWindowHandles.head
    assertEquals(
      ops.fullscreenMonitorOf(windowHandle),
      ops.monitorB,
      "config.fullscreenMode is set, so the window must be created fullscreen on the mode's monitor " +
        "(original: glfwCreateWindow(..., config.fullscreenMode.getMonitor(), ...), Lwjgl3Application.java:515-518); " +
        "the WindowingOps creation seam carries no monitor at all (WindowingOps.createWindow(width, height, title))"
    )
  }

  test("ISS-759 (b): setFullscreenMode targets the monitor the display mode came from (Lwjgl3Graphics.java:414-434)") {
    val ops = new RecordingWindowingOps
    // setVSync at the end of setFullscreenMode goes through the global EGL ops.
    sge.platform.PlatformOps.gl = new StubGlOps
    val config = new DesktopApplicationConfig
    val window = new DesktopWindow(listener, ArrayBuffer.empty, config, new StubDesktopApplicationBase(ops), ops, new StubGlOps)
    window.create(900001L)
    val graphics = window.graphics

    // The window sits on monitor A; request a display mode queried from monitor B.
    val monitorB = graphics.monitors.find(_.name == "monitor-B").getOrElse(fail("monitor-B not reported by Graphics.monitors"))
    val modeOnB  = graphics.getDisplayModes(monitorB)(0)
    assertEquals((modeOnB.width, modeOnB.height, modeOnB.refreshRate), (2560, 1440, 144), "fixture sanity: monitor B's mode")

    assert(graphics.setFullscreenMode(modeOnB), "setFullscreenMode reported failure")

    assert(ops.setWindowMonitorCalls.nonEmpty, "setFullscreenMode must call setWindowMonitor")
    val (_, monitorHandle, _, _, _, _, _) = ops.setWindowMonitorCalls.last
    assertEquals(
      monitorHandle,
      ops.monitorB,
      "setFullscreenMode(mode-from-monitor-B) must go fullscreen on monitor B " +
        "(original: glfwSetWindowMonitor(..., newMode.getMonitor(), ...), Lwjgl3Graphics.java:414-434); " +
        "DesktopDisplayMode.toDisplayMode (DesktopDisplayMode.scala:41) drops monitorHandle, so " +
        "DesktopGraphics.setFullscreenMode (DesktopGraphics.scala:312-325) can only re-resolve against the current monitor A"
    )
  }

  test(
    "ISS-759 (c): DesktopApplicationConfig companion exposes pre-launch monitor/display-mode queries (Lwjgl3ApplicationConfiguration.java:286-341)"
  ) {
    // Structural contract test: the original provides static getDisplayMode()/getDisplayMode(Monitor)/
    // getDisplayModes()/getDisplayModes(Monitor)/getPrimaryMonitor()/getMonitors(); the SGE companion
    // (DesktopApplicationConfig.scala:239) defers them although WindowingOps already exposes
    // monitors/getVideoModes (consumed by DesktopGraphics.scala:239-256). Reflection keeps this suite
    // compiling while the API is absent, and accepts both Java-style (getMonitors) and SGE-style
    // (monitors — "no Java-style getters") names so the faithful fix turns it green either way.
    val methodNames = DesktopApplicationConfig.getClass.getMethods.map(_.getName).toSet
    def assertExposes(candidates: Set[String], original: String): Unit =
      assert(
        methodNames.exists(candidates),
        s"DesktopApplicationConfig companion must port $original (Lwjgl3ApplicationConfiguration.java:286-341); " +
          s"expected one of ${candidates.toList.sorted.mkString("/")}, found only: ${methodNames.toList.sorted.mkString(", ")}"
      )
    assertExposes(Set("getMonitors", "monitors"), "static Monitor[] getMonitors()")
    assertExposes(Set("getPrimaryMonitor", "primaryMonitor"), "static Monitor getPrimaryMonitor()")
    assertExposes(Set("getDisplayMode", "displayMode"), "static DisplayMode getDisplayMode()")
    assertExposes(Set("getDisplayModes", "displayModes"), "static DisplayMode[] getDisplayModes()")
  }
}
