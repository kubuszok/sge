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

/** ISS-759 fix-side regression tests (audit bounce #1 findings; complements [[DesktopFullscreenIss759RedSuite]], whose assertions stay untouched).
  *
  *   - Finding 1: the display-mode → monitor side table in [[DesktopGraphics]] must be keyed by reference identity, not structural equality. Two monitors routinely expose structurally-equal modes
  *     (any dual same-model monitor setup); an equality-keyed table collides them and `setFullscreenMode(mode-from-A)` lands on B. This fixture gives BOTH monitors identical mode lists on purpose.
  *   - Finding 2: the pre-launch companion queries must be functionally connected to `config.fullscreenMode`: the original's recipe `config.setFullscreenMode(getDisplayMode(monitor))` works because
  *     the statics covertly return handle-carrying `Lwjgl3DisplayMode`s. SGE's companion returns [[DesktopDisplayMode]]/[[DesktopMonitor]] directly, so
  *     `config.fullscreenMode = Nullable(DesktopApplicationConfig.displayMode(monitor))` must work end-to-end with no handle fabrication.
  *
  * Headless: all windowing calls go through a recording [[WindowingOps]] stub; the app drive stops at the `glFactory` seam before GL bindings init.
  */
class DesktopFullscreenIss759FixSuite extends munit.FunSuite {

  /** Marker thrown by the `glFactory` seam to stop `DesktopApplication`'s constructor after window creation but before GL bindings init (which would need a real GL context). */
  final private class StopBeforeGlInit extends RuntimeException("test drive stops before GL bindings init")

  /** A do-nothing listener; `render()` is the only abstract member and is never reached (the drive stops before the loop). */
  private def listener: ApplicationListener = new ApplicationListener {
    override def render(): Unit = {}
  }

  /** Records every windowing call relevant to the fullscreen cluster; models two monitors with IDENTICAL mode lists (the Finding-1 collision precondition):
    *
    *   - monitor A (handle 100L, primary) at (0, 0) — the window (created at (10, 10) sized 640x480) sits on it,
    *   - monitor B (handle 200L) at (1920, 0),
    *
    * both exposing a single 1920x1080@60 mode.
    */
  final private class RecordingWindowingOps extends WindowingOps {
    val monitorA: Long = 100L
    val monitorB: Long = 200L

    // (width, height, title) per createWindow call.
    val createWindowCalls:    ArrayBuffer[(Int, Int, String)] = ArrayBuffer.empty
    val createdWindowHandles: ArrayBuffer[Long]               = ArrayBuffer.empty

    // (windowHandle, monitorHandle, x, y, width, height, refreshRate) per setWindowMonitor call.
    val setWindowMonitorCalls: ArrayBuffer[(Long, Long, Int, Int, Int, Int, Int)] = ArrayBuffer.empty

    // (windowHandle, width, height) per setWindowSize call.
    val setWindowSizeCalls: ArrayBuffer[(Long, Int, Int)] = ArrayBuffer.empty

    // windowHandle -> monitor the window is currently fullscreen on (0 = windowed).
    private val fullscreenMonitor: mutable.Map[Long, Long] = mutable.Map.empty

    /** Monitor the window ended up fullscreen on, or 0 if it was never made fullscreen. */
    def fullscreenMonitorOf(windowHandle: Long): Long = fullscreenMonitor.getOrElse(windowHandle, 0L)

    private var nextWindowHandle: Long = 900001L

    // ─── Initialization ──────────────────────────────────────────────────
    override def setInitHint(hint: Int, value: Int): Unit    = {}
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
    override def setWindowTitle(windowHandle: Long, title: String):          Unit       = {}
    override def getWindowSize(windowHandle:  Long):                         (Int, Int) = (640, 480)
    override def setWindowSize(windowHandle: Long, width: Int, height: Int): Unit       =
      setWindowSizeCalls += ((windowHandle, width, height))
    override def getWindowPos(windowHandle:       Long):                 (Int, Int) = (10, 10)
    override def setWindowPos(windowHandle:       Long, x: Int, y: Int): Unit       = {}
    override def getFramebufferSize(windowHandle: Long):                 (Int, Int) = (640, 480)
    override def iconifyWindow(windowHandle:      Long):                 Unit       = {}
    override def restoreWindow(windowHandle:      Long):                 Unit       = {}
    override def maximizeWindow(windowHandle:     Long):                 Unit       = {}
    override def showWindow(windowHandle:         Long):                 Unit       = {}
    override def hideWindow(windowHandle:         Long):                 Unit       = {}
    override def focusWindow(windowHandle:        Long):                 Unit       = {}

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
    override def setWindowHint(hint: Int, value: Int): Unit = {}
    override def defaultWindowHints():                 Unit = {}

    // ─── Context ─────────────────────────────────────────────────────────
    override def makeContextCurrent(windowHandle: Long):   Unit    = {}
    override def setSwapInterval(interval:        Int):    Unit    = {}
    override def extensionSupported(extension:    String): Boolean = false

    // ─── Monitor extended ────────────────────────────────────────────────
    override def getMonitorPhysicalSize(monitorHandle: Long): (Int, Int)                            = (600, 340)
    override def getVideoModes(monitorHandle: Long):          Array[(Int, Int, Int, Int, Int, Int)] =
      Array(getVideoMode(monitorHandle))
    // IDENTICAL mode on both monitors — Finding 1's collision precondition (dual same-model monitors).
    override def getVideoMode(monitorHandle: Long): (Int, Int, Int, Int, Int, Int) = (1920, 1080, 60, 8, 8, 8)

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

  test(
    "ISS-759 fix (Finding 1): identical modes on two monitors — a mode queried from monitor A goes fullscreen on A, from B on B (identity-keyed side table)"
  ) {
    val ops = new RecordingWindowingOps
    // setVSync at the end of setFullscreenMode goes through the global EGL ops.
    sge.platform.PlatformOps.gl = new StubGlOps
    val config = new DesktopApplicationConfig
    val window = new DesktopWindow(listener, ArrayBuffer.empty, config, new StubDesktopApplicationBase(ops), ops, new StubGlOps)
    window.create(900001L)
    val graphics = window.graphics

    val monitorA = graphics.monitors.find(_.name == "monitor-A").getOrElse(fail("monitor-A not reported by Graphics.monitors"))
    val monitorB = graphics.monitors.find(_.name == "monitor-B").getOrElse(fail("monitor-B not reported by Graphics.monitors"))
    // Query A first, then B: with a structurally-keyed table, B's registration overwrites A's
    // (the modes are equal case class instances) and the A-mode below would land on B.
    val modeFromA = graphics.getDisplayModes(monitorA)(0)
    val modeFromB = graphics.getDisplayModes(monitorB)(0)
    assertEquals(modeFromA, modeFromB, "fixture sanity: the two monitors must expose structurally-equal modes")

    assert(graphics.setFullscreenMode(modeFromA), "setFullscreenMode(mode-from-A) reported failure")
    assert(ops.setWindowMonitorCalls.nonEmpty, "setFullscreenMode must call setWindowMonitor")
    val (_, monitorAfterA, _, _, _, _, _) = ops.setWindowMonitorCalls.last
    assertEquals(
      monitorAfterA,
      ops.monitorA,
      "a display mode queried from monitor A must go fullscreen on monitor A even when monitor B " +
        "exposes a structurally-equal mode — the mode→monitor side table must be keyed by instance " +
        "identity, not case-class equality (original: newMode.getMonitor(), Lwjgl3Graphics.java:424)"
    )

    assert(graphics.setFullscreenMode(modeFromB), "setFullscreenMode(mode-from-B) reported failure")
    val (_, monitorAfterB, _, _, _, _, _) = ops.setWindowMonitorCalls.last
    assertEquals(
      monitorAfterB,
      ops.monitorB,
      "the structurally-equal mode queried from monitor B must go fullscreen on monitor B"
    )
  }

  test(
    "ISS-759 fix (Finding 2): config.fullscreenMode = DesktopApplicationConfig.displayMode(monitor) works end-to-end with no handle fabrication"
  ) {
    val ops = new RecordingWindowingOps
    // Pre-launch queries reuse an already-installed windowing FFI instead of booting the platform default.
    sge.platform.PlatformOps.windowing = ops

    val monitorB = DesktopApplicationConfig.monitors.find(_.name == "monitor-B").getOrElse(fail("monitor-B not reported by DesktopApplicationConfig.monitors"))
    assertEquals(
      DesktopApplicationConfig.primaryMonitor.monitorHandle,
      ops.monitorA,
      "primaryMonitor must carry monitor A's native handle"
    )
    val mode = DesktopApplicationConfig.displayMode(monitorB)
    assertEquals(mode.monitorHandle, ops.monitorB, "displayMode(monitor) must carry the queried monitor's native handle")
    assertEquals((mode.width, mode.height, mode.refreshRate), (1920, 1080, 60), "fixture sanity: monitor B's mode")

    // The original's canonical startup-fullscreen recipe (Lwjgl3ApplicationConfiguration.getDisplayMode()
    // → config.setFullscreenMode(...)): no handle fabrication, straight from the companion query.
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    config.title = "iss759-fix"
    config.fullscreenMode = Nullable(mode)

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

    val (width, height, _) = ops.createWindowCalls.head
    assertEquals((width, height), (1920, 1080), "the window must be created at the companion-queried mode's size")
    assertEquals(
      ops.fullscreenMonitorOf(ops.createdWindowHandles.head),
      ops.monitorB,
      "the window must open fullscreen on the monitor the companion-queried mode came from"
    )
  }
}
