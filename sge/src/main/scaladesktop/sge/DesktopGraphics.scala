/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Graphics.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Graphics -> DesktopGraphics
 *   Convention: GLFW calls abstracted through WindowingOps FFI trait
 *   Convention: GL instances created based on ANGLE config, not reflective Class.forName
 *   Convention: Lwjgl3Monitor/Lwjgl3DisplayMode inner classes -> DesktopMonitor/DesktopDisplayMode
 *   Idiom: split packages; Nullable; no return
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.{ Cursor, GL20, GL30, GL31, GL32, Pixmap }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.glutils.{ GLVersion, HdpiMode }
import sge.platform.WindowingOps
import sge.utils.{ Nullable, Seconds }

/** Desktop implementation of [[Graphics]]. Manages the GL context, frame timing, display mode queries, and cursor.
  *
  * All windowing system calls are routed through the [[WindowingOps]] FFI trait, which abstracts GLFW/SDL3 for both JVM (Panama) and Native (@extern) backends.
  *
  * @param window
  *   the desktop window this graphics instance belongs to
  * @param windowing
  *   the windowing FFI operations
  * @author
  *   badlogic (original implementation)
  */
class DesktopGraphics private[sge] (
  private[sge] val window: DesktopWindow,
  private val windowing:   WindowingOps
) extends Graphics
    with AutoCloseable {

  // ─── GL instances ─────────────────────────────────────────────────────

  private var _gl20:      GL20           = scala.compiletime.uninitialized
  private var _gl30:      Nullable[GL30] = Nullable.empty
  private var _gl31:      Nullable[GL31] = Nullable.empty
  private var _gl32:      Nullable[GL32] = Nullable.empty
  private var _glVersion: GLVersion      = scala.compiletime.uninitialized

  // ─── Frame state ──────────────────────────────────────────────────────

  @volatile private var _backBufferWidth:  Int     = 0
  @volatile private var _backBufferHeight: Int     = 0
  @volatile private var _logicalWidth:     Int     = 0
  @volatile private var _logicalHeight:    Int     = 0
  @volatile private var _isContinuous:     Boolean = true

  private var _bufferFormat:      Graphics.BufferFormat = scala.compiletime.uninitialized
  private var _lastFrameTime:     Long                  = -1L
  private var _deltaTime:         Seconds               = Seconds.zero
  private var _resetDeltaTime:    Boolean               = false
  private var _frameId:           Long                  = 0L
  private var _frameCounterStart: Long                  = 0L
  private var _frames:            Int                   = 0
  private var _fps:               Int                   = 0

  // ─── Fullscreen state ─────────────────────────────────────────────────

  private var windowPosXBeforeFullscreen:   Int                          = 0
  private var windowPosYBeforeFullscreen:   Int                          = 0
  private var windowWidthBeforeFullscreen:  Int                          = 0
  private var windowHeightBeforeFullscreen: Int                          = 0
  private var displayModeBeforeFullscreen:  Nullable[DesktopDisplayMode] = Nullable.empty

  // ─── Initialization ───────────────────────────────────────────────────

  private[sge] def initGL(gl20: GL20, gl30: Nullable[GL30], gl31: Nullable[GL31], gl32: Nullable[GL32]): Unit = {
    _gl20 = gl20
    _gl30 = gl30
    _gl31 = gl31
    _gl32 = gl32
    updateFramebufferInfo()
    initiateGL()
    windowing.setFramebufferSizeCallback(window.windowHandle, onFramebufferResize)
  }

  private def initiateGL(): Unit = {
    val versionString  = _gl20.glGetString(GL20.GL_VERSION)
    val vendorString   = _gl20.glGetString(GL20.GL_VENDOR)
    val rendererString = _gl20.glGetString(GL20.GL_RENDERER)
    _glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString)
  }

  private val onFramebufferResize: (Long, Int, Int) => Unit = { (_, _, _) =>
    updateFramebufferInfo()
    // Update CALayer contentsScale when moving between displays with different DPI
    windowing.updateNativeLayerScale(window.windowHandle)
    if (window.isListenerInitialized()) {
      window.makeCurrent()
      _gl20.glViewport(Pixels.zero, Pixels.zero, Pixels(_backBufferWidth), Pixels(_backBufferHeight))
      window.listener.resize(width, height)
      update()
      window.listener.render()
      sge.platform.PlatformOps.gl.swapEglBuffers(window.eglContext)
    }
  }

  // ─── Frame info ───────────────────────────────────────────────────────

  private[sge] def updateFramebufferInfo(): Unit = {
    val (fbW, fbH) = windowing.getFramebufferSize(window.windowHandle)
    _backBufferWidth = fbW
    _backBufferHeight = fbH
    val (winW, winH) = windowing.getWindowSize(window.windowHandle)
    _logicalWidth = winW
    _logicalHeight = winH
    val c = window.config
    _bufferFormat = Graphics.BufferFormat(c.r, c.g, c.b, c.a, c.depth, c.stencil, c.samples, false)
  }

  private[sge] def update(): Unit = {
    val time = System.nanoTime()
    if (_lastFrameTime == -1L) _lastFrameTime = time
    if (_resetDeltaTime) {
      _resetDeltaTime = false
      _deltaTime = Seconds.zero
    } else {
      _deltaTime = Seconds((time - _lastFrameTime) / 1000000000.0f)
    }
    _lastFrameTime = time

    if (time - _frameCounterStart >= 1000000000L) {
      _fps = _frames
      _frames = 0
      _frameCounterStart = time
    }
    _frames += 1
    _frameId += 1
  }

  def resetDeltaTime(): Unit = _resetDeltaTime = true

  // ─── GL availability ──────────────────────────────────────────────────

  override def gl30Available: Boolean = _gl30.isDefined
  override def gl31Available: Boolean = _gl31.isDefined
  override def gl32Available: Boolean = _gl32.isDefined

  override def gl20: GL20           = _gl20
  override def gl30: Nullable[GL30] = _gl30
  override def gl31: Nullable[GL31] = _gl31
  override def gl32: Nullable[GL32] = _gl32

  override def gl20_=(value: GL20): Unit = _gl20 = value
  override def gl30_=(value: GL30): Unit = _gl30 = Nullable(value)
  override def gl31_=(value: GL31): Unit = _gl31 = Nullable(value)
  override def gl32_=(value: GL32): Unit = _gl32 = Nullable(value)

  // ─── Dimensions ───────────────────────────────────────────────────────

  override def width: Pixels =
    Pixels(if (window.config.hdpiMode == HdpiMode.Pixels) _backBufferWidth else _logicalWidth)

  override def height: Pixels =
    Pixels(if (window.config.hdpiMode == HdpiMode.Pixels) _backBufferHeight else _logicalHeight)

  override def backBufferWidth:  Pixels = Pixels(_backBufferWidth)
  override def backBufferHeight: Pixels = Pixels(_backBufferHeight)

  def logicalWidth:  Int = _logicalWidth
  def logicalHeight: Int = _logicalHeight

  override def backBufferScale: Float =
    if (_logicalWidth != 0) _backBufferWidth.toFloat / _logicalWidth.toFloat else 1f

  // ─── Frame timing ────────────────────────────────────────────────────

  override def frameId:         Long    = _frameId
  override def deltaTime:       Seconds = _deltaTime
  override def framesPerSecond: Int     = _fps

  // ─── Type / version ──────────────────────────────────────────────────

  override def graphicsType: Graphics.GraphicsType = Graphics.GraphicsType.LWJGL3
  override def glVersion:    Graphics.GLVersion    = _glVersion

  // ─── DPI / density ───────────────────────────────────────────────────

  override def ppiX: Float = ppcX * 2.54f
  override def ppiY: Float = ppcY * 2.54f

  override def ppcX: Float = {
    val mon          = currentDesktopMonitor
    val (sizeXmm, _) = windowing.getMonitorPhysicalSize(mon.monitorHandle)
    if (sizeXmm == 0) 1f
    else {
      val mode = getDesktopDisplayMode(mon)
      mode.width / sizeXmm.toFloat * 10f
    }
  }

  override def ppcY: Float = {
    val mon          = currentDesktopMonitor
    val (_, sizeYmm) = windowing.getMonitorPhysicalSize(mon.monitorHandle)
    if (sizeYmm == 0) 1f
    else {
      val mode = getDesktopDisplayMode(mon)
      mode.height / sizeYmm.toFloat * 10f
    }
  }

  override def density: Float = ppiX / 160f

  // ─── Safe insets (none on desktop) ────────────────────────────────────

  override def safeInsetLeft:   Pixels = Pixels.zero
  override def safeInsetTop:    Pixels = Pixels.zero
  override def safeInsetBottom: Pixels = Pixels.zero
  override def safeInsetRight:  Pixels = Pixels.zero

  // ─── Display modes / monitors ─────────────────────────────────────────

  override def supportsDisplayModeChange(): Boolean = true

  override def primaryMonitor: Graphics.Monitor = {
    val handle = windowing.primaryMonitor
    toDesktopMonitor(handle).toMonitor
  }

  override def monitor: Graphics.Monitor = currentDesktopMonitor.toMonitor

  override def monitors: Array[Graphics.Monitor] =
    windowing.monitors.map(h => toDesktopMonitor(h).toMonitor)

  override def displayModes: Array[Graphics.DisplayMode] =
    getDesktopDisplayModes(currentDesktopMonitor).map(_.toDisplayMode)

  override def getDisplayModes(monitor: Graphics.Monitor): Array[Graphics.DisplayMode] = {
    // Find the desktop monitor matching by name and position
    val desktopMon = findDesktopMonitor(monitor)
    getDesktopDisplayModes(desktopMon).map(_.toDisplayMode)
  }

  override def displayMode: Graphics.DisplayMode =
    getDesktopDisplayMode(currentDesktopMonitor).toDisplayMode

  override def getDisplayMode(monitor: Graphics.Monitor): Graphics.DisplayMode = {
    val desktopMon = findDesktopMonitor(monitor)
    getDesktopDisplayMode(desktopMon).toDisplayMode
  }

  // ─── Desktop-specific monitor/display helpers ─────────────────────────

  private def toDesktopMonitor(handle: Long): DesktopMonitor = {
    val name   = windowing.getMonitorName(handle)
    val (x, y) = windowing.getMonitorPos(handle)
    DesktopMonitor(handle, x, y, name)
  }

  private[sge] def currentDesktopMonitor: DesktopMonitor = {
    val monitors = windowing.monitors.map(toDesktopMonitor)
    if (monitors.isEmpty) toDesktopMonitor(windowing.primaryMonitor)
    else {
      val (windowX, windowY)          = windowing.getWindowPos(window.windowHandle)
      val (windowWidth, windowHeight) = windowing.getWindowSize(window.windowHandle)
      var bestOverlap                 = 0
      var result                      = monitors(0)
      var i                           = 0
      while (i < monitors.length) {
        val mon      = monitors(i)
        val mode     = getDesktopDisplayMode(mon)
        val overlapX = scala.math.max(0, scala.math.min(windowX + windowWidth, mon.virtualX + mode.width) - scala.math.max(windowX, mon.virtualX))
        val overlapY = scala.math.max(0, scala.math.min(windowY + windowHeight, mon.virtualY + mode.height) - scala.math.max(windowY, mon.virtualY))
        val overlap  = overlapX * overlapY
        if (overlap > bestOverlap) {
          bestOverlap = overlap
          result = mon
        }
        i += 1
      }
      result
    }
  }

  private def findDesktopMonitor(monitor: Graphics.Monitor): DesktopMonitor = {
    val monitors = windowing.monitors.map(toDesktopMonitor)
    monitors.find(m => m.name == monitor.name && m.virtualX == monitor.virtualX && m.virtualY == monitor.virtualY).getOrElse(toDesktopMonitor(windowing.primaryMonitor))
  }

  private def getDesktopDisplayModes(monitor: DesktopMonitor): Array[DesktopDisplayMode] =
    windowing.getVideoModes(monitor.monitorHandle).map { case (w, h, rr, rb, gb, bb) =>
      DesktopDisplayMode(monitor.monitorHandle, w, h, rr, rb + gb + bb)
    }

  private def getDesktopDisplayMode(monitor: DesktopMonitor): DesktopDisplayMode = {
    val (w, h, rr, rb, gb, bb) = windowing.getVideoMode(monitor.monitorHandle)
    DesktopDisplayMode(monitor.monitorHandle, w, h, rr, rb + gb + bb)
  }

  // ─── Fullscreen / windowed ────────────────────────────────────────────

  override def setFullscreenMode(displayMode: Graphics.DisplayMode): Boolean = {
    window.input.resetPollingStates()
    // Find the desktop display mode matching the requested one
    val mon         = currentDesktopMonitor
    val desktopMode = getDesktopDisplayModes(mon)
      .find { dm =>
        dm.width == displayMode.width && dm.height == displayMode.height && dm.refreshRate == displayMode.refreshRate
      }
      .getOrElse(
        DesktopDisplayMode(mon.monitorHandle, displayMode.width, displayMode.height, displayMode.refreshRate, displayMode.bitsPerPixel)
      )

    if (fullscreen) {
      windowing.setWindowMonitor(window.windowHandle, desktopMode.monitorHandle, 0, 0, desktopMode.width, desktopMode.height, desktopMode.refreshRate)
    } else {
      storeCurrentWindowPositionAndDisplayMode()
      windowing.setWindowMonitor(window.windowHandle, desktopMode.monitorHandle, 0, 0, desktopMode.width, desktopMode.height, desktopMode.refreshRate)
    }
    updateFramebufferInfo()
    setVSync(window.config.vSyncEnabled)
    true
  }

  private def storeCurrentWindowPositionAndDisplayMode(): Unit = {
    windowPosXBeforeFullscreen = window.positionX
    windowPosYBeforeFullscreen = window.positionY
    windowWidthBeforeFullscreen = _logicalWidth
    windowHeightBeforeFullscreen = _logicalHeight
    displayModeBeforeFullscreen = Nullable(getDesktopDisplayMode(currentDesktopMonitor))
  }

  override def setWindowedMode(width: Pixels, height: Pixels): Boolean = {
    window.input.resetPollingStates()
    if (!fullscreen) {
      if (width.toInt != _logicalWidth || height.toInt != _logicalHeight) {
        windowing.setWindowSize(window.windowHandle, width.toInt, height.toInt)
        // Center the window on current monitor
        val mon  = currentDesktopMonitor
        val mode = getDesktopDisplayMode(mon)
        val newX = mon.virtualX + (mode.width - width.toInt) / 2
        val newY = mon.virtualY + (mode.height - height.toInt) / 2
        window.setPosition(newX, newY)
      }
    } else {
      if (displayModeBeforeFullscreen.isEmpty) {
        storeCurrentWindowPositionAndDisplayMode()
      }
      val refreshRate = displayModeBeforeFullscreen.fold(0)(_.refreshRate)
      if (width.toInt != windowWidthBeforeFullscreen || height.toInt != windowHeightBeforeFullscreen) {
        val mon  = currentDesktopMonitor
        val mode = getDesktopDisplayMode(mon)
        val newX = mon.virtualX + (mode.width - width.toInt) / 2
        val newY = mon.virtualY + (mode.height - height.toInt) / 2
        windowing.setWindowMonitor(window.windowHandle, 0L, newX, newY, width.toInt, height.toInt, refreshRate)
      } else {
        windowing.setWindowMonitor(
          window.windowHandle,
          0L,
          windowPosXBeforeFullscreen,
          windowPosYBeforeFullscreen,
          width.toInt,
          height.toInt,
          refreshRate
        )
      }
    }
    updateFramebufferInfo()
    true
  }

  override def setTitle(title: String): Unit =
    windowing.setWindowTitle(window.windowHandle, if (title == null) "" else title) // null-safe — callers may pass null title

  override def setUndecorated(undecorated: Boolean): Unit = {
    window.config.windowDecorated = !undecorated
    windowing.setWindowAttrib(window.windowHandle, WindowingOps.GLFW_DECORATED, if (undecorated) 0 else 1)
  }

  override def setResizable(resizable: Boolean): Unit = {
    window.config.windowResizable = resizable
    windowing.setWindowAttrib(window.windowHandle, WindowingOps.GLFW_RESIZABLE, if (resizable) 1 else 0)
  }

  override def setVSync(vsync: Boolean): Unit = {
    window.config.vSyncEnabled = vsync
    sge.platform.PlatformOps.gl.setSwapInterval(if (vsync) 1 else 0)
  }

  override def setForegroundFPS(fps: Int): Unit =
    window.config.foregroundFPS = fps

  // ─── Buffer format / extensions ──────────────────────────────────────

  override def bufferFormat: Graphics.BufferFormat = _bufferFormat

  override def supportsExtension(extension: String): Boolean =
    windowing.extensionSupported(extension)

  // ─── Continuous rendering ─────────────────────────────────────────────

  override def setContinuousRendering(isContinuous: Boolean): Unit =
    _isContinuous = isContinuous

  override def continuousRendering: Boolean = _isContinuous

  override def requestRendering(): Unit =
    window.requestRendering()

  override def fullscreen: Boolean =
    windowing.getWindowMonitor(window.windowHandle) != 0L

  // ─── Cursor ───────────────────────────────────────────────────────────

  override def newCursor(pixmap: Pixmap, xHotspot: Pixels, yHotspot: Pixels): Nullable[Cursor] =
    // Custom pixmap cursors require WindowingOps.createCursorFromImage (deferred)
    Nullable.empty

  override def setCursor(cursor: Cursor): Unit = cursor match {
    case dc: DesktopCursor =>
      windowing.setCursor(window.windowHandle, dc.glfwCursor)
    case _ => ()
  }

  override def setSystemCursor(systemCursor: SystemCursor): Unit =
    DesktopCursor.setSystemCursor(window.windowHandle, systemCursor)

  // ─── Lifecycle ────────────────────────────────────────────────────────

  override def close(): Unit = {
    // Cleanup callback — the window's close() will destroy the native window
  }
}

object DesktopGraphics {

  /** GL_TEXTURE_CUBE_MAP_SEAMLESS constant (desktop GL 3.2 extension). */
  private[sge] val GL_TEXTURE_CUBE_MAP_SEAMLESS: Int = 0x884f
}
