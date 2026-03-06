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
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.{ Cursor, GL20, GL30, GL31, GL32, Pixmap }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.glutils.{ GLVersion, HdpiMode }
import sge.platform.WindowingOps
import sge.utils.Nullable

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
  private var _deltaTime:         Float                 = 0f
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
    windowing.setFramebufferSizeCallback(window.getWindowHandle(), onFramebufferResize)
  }

  private def initiateGL(): Unit = {
    val versionString  = _gl20.glGetString(GL20.GL_VERSION)
    val vendorString   = _gl20.glGetString(GL20.GL_VENDOR)
    val rendererString = _gl20.glGetString(GL20.GL_RENDERER)
    _glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString)
  }

  private val onFramebufferResize: (Long, Int, Int) => Unit = { (_, _, _) =>
    updateFramebufferInfo()
    if (window.isListenerInitialized()) {
      window.makeCurrent()
      _gl20.glViewport(0, 0, _backBufferWidth, _backBufferHeight)
      window.listener.resize(getWidth(), getHeight())
      update()
      window.listener.render()
      windowing.swapBuffers(window.getWindowHandle())
    }
  }

  // ─── Frame info ───────────────────────────────────────────────────────

  private[sge] def updateFramebufferInfo(): Unit = {
    val (fbW, fbH) = windowing.getFramebufferSize(window.getWindowHandle())
    _backBufferWidth = fbW
    _backBufferHeight = fbH
    val (winW, winH) = windowing.getWindowSize(window.getWindowHandle())
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
      _deltaTime = 0f
    } else {
      _deltaTime = (time - _lastFrameTime) / 1000000000.0f
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

  override def isGL30Available(): Boolean = _gl30.isDefined
  override def isGL31Available(): Boolean = _gl31.isDefined
  override def isGL32Available(): Boolean = _gl32.isDefined

  override def getGL20(): GL20           = _gl20
  override def getGL30(): Nullable[GL30] = _gl30
  override def getGL31(): Nullable[GL31] = _gl31
  override def getGL32(): Nullable[GL32] = _gl32

  override def setGL20(gl20: GL20): Unit = _gl20 = gl20
  override def setGL30(gl30: GL30): Unit = _gl30 = Nullable(gl30)
  override def setGL31(gl31: GL31): Unit = _gl31 = Nullable(gl31)
  override def setGL32(gl32: GL32): Unit = _gl32 = Nullable(gl32)

  // ─── Dimensions ───────────────────────────────────────────────────────

  override def getWidth(): Int =
    if (window.config.hdpiMode == HdpiMode.Pixels) _backBufferWidth else _logicalWidth

  override def getHeight(): Int =
    if (window.config.hdpiMode == HdpiMode.Pixels) _backBufferHeight else _logicalHeight

  override def getBackBufferWidth():  Int = _backBufferWidth
  override def getBackBufferHeight(): Int = _backBufferHeight

  def getLogicalWidth():  Int = _logicalWidth
  def getLogicalHeight(): Int = _logicalHeight

  override def getBackBufferScale(): Float =
    if (_logicalWidth != 0) _backBufferWidth.toFloat / _logicalWidth.toFloat else 1f

  // ─── Frame timing ────────────────────────────────────────────────────

  override def getFrameId():         Long  = _frameId
  override def getDeltaTime():       Float = _deltaTime
  override def getRawDeltaTime():    Float = _deltaTime
  override def getFramesPerSecond(): Int   = _fps

  // ─── Type / version ──────────────────────────────────────────────────

  override def getType():      Graphics.GraphicsType = Graphics.GraphicsType.LWJGL3
  override def getGLVersion(): Graphics.GLVersion    = _glVersion

  // ─── DPI / density ───────────────────────────────────────────────────

  override def getPpiX(): Float = getPpcX() * 2.54f
  override def getPpiY(): Float = getPpcY() * 2.54f

  override def getPpcX(): Float = {
    val mon          = getCurrentDesktopMonitor()
    val (sizeXmm, _) = windowing.getMonitorPhysicalSize(mon.monitorHandle)
    if (sizeXmm == 0) 1f
    else {
      val mode = getDesktopDisplayMode(mon)
      mode.width / sizeXmm.toFloat * 10f
    }
  }

  override def getPpcY(): Float = {
    val mon          = getCurrentDesktopMonitor()
    val (_, sizeYmm) = windowing.getMonitorPhysicalSize(mon.monitorHandle)
    if (sizeYmm == 0) 1f
    else {
      val mode = getDesktopDisplayMode(mon)
      mode.height / sizeYmm.toFloat * 10f
    }
  }

  override def getDensity(): Float = getPpiX() / 160f

  // ─── Safe insets (none on desktop) ────────────────────────────────────

  override def getSafeInsetLeft():   Int = 0
  override def getSafeInsetTop():    Int = 0
  override def getSafeInsetBottom(): Int = 0
  override def getSafeInsetRight():  Int = 0

  // ─── Display modes / monitors ─────────────────────────────────────────

  override def supportsDisplayModeChange(): Boolean = true

  override def getPrimaryMonitor(): Graphics.Monitor = {
    val handle = windowing.getPrimaryMonitor()
    toDesktopMonitor(handle).toMonitor
  }

  override def getMonitor(): Graphics.Monitor = getCurrentDesktopMonitor().toMonitor

  override def getMonitors(): Array[Graphics.Monitor] =
    windowing.getMonitors().map(h => toDesktopMonitor(h).toMonitor)

  override def getDisplayModes(): Array[Graphics.DisplayMode] =
    getDesktopDisplayModes(getCurrentDesktopMonitor()).map(_.toDisplayMode)

  override def getDisplayModes(monitor: Graphics.Monitor): Array[Graphics.DisplayMode] = {
    // Find the desktop monitor matching by name and position
    val desktopMon = findDesktopMonitor(monitor)
    getDesktopDisplayModes(desktopMon).map(_.toDisplayMode)
  }

  override def getDisplayMode(): Graphics.DisplayMode =
    getDesktopDisplayMode(getCurrentDesktopMonitor()).toDisplayMode

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

  private[sge] def getCurrentDesktopMonitor(): DesktopMonitor = {
    val monitors = windowing.getMonitors().map(toDesktopMonitor)
    if (monitors.isEmpty) toDesktopMonitor(windowing.getPrimaryMonitor())
    else {
      val (windowX, windowY)          = windowing.getWindowPos(window.getWindowHandle())
      val (windowWidth, windowHeight) = windowing.getWindowSize(window.getWindowHandle())
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
    val monitors = windowing.getMonitors().map(toDesktopMonitor)
    monitors.find(m => m.name == monitor.name && m.virtualX == monitor.virtualX && m.virtualY == monitor.virtualY).getOrElse(toDesktopMonitor(windowing.getPrimaryMonitor()))
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
    window.getInput().resetPollingStates()
    // Find the desktop display mode matching the requested one
    val mon         = getCurrentDesktopMonitor()
    val desktopMode = getDesktopDisplayModes(mon)
      .find { dm =>
        dm.width == displayMode.width && dm.height == displayMode.height && dm.refreshRate == displayMode.refreshRate
      }
      .getOrElse(
        DesktopDisplayMode(mon.monitorHandle, displayMode.width, displayMode.height, displayMode.refreshRate, displayMode.bitsPerPixel)
      )

    if (isFullscreen()) {
      windowing.setWindowMonitor(window.getWindowHandle(), desktopMode.monitorHandle, 0, 0, desktopMode.width, desktopMode.height, desktopMode.refreshRate)
    } else {
      storeCurrentWindowPositionAndDisplayMode()
      windowing.setWindowMonitor(window.getWindowHandle(), desktopMode.monitorHandle, 0, 0, desktopMode.width, desktopMode.height, desktopMode.refreshRate)
    }
    updateFramebufferInfo()
    setVSync(window.config.vSyncEnabled)
    true
  }

  private def storeCurrentWindowPositionAndDisplayMode(): Unit = {
    windowPosXBeforeFullscreen = window.getPositionX()
    windowPosYBeforeFullscreen = window.getPositionY()
    windowWidthBeforeFullscreen = _logicalWidth
    windowHeightBeforeFullscreen = _logicalHeight
    displayModeBeforeFullscreen = Nullable(getDesktopDisplayMode(getCurrentDesktopMonitor()))
  }

  override def setWindowedMode(width: Int, height: Int): Boolean = {
    window.getInput().resetPollingStates()
    if (!isFullscreen()) {
      if (width != _logicalWidth || height != _logicalHeight) {
        windowing.setWindowSize(window.getWindowHandle(), width, height)
        // Center the window on current monitor
        val mon  = getCurrentDesktopMonitor()
        val mode = getDesktopDisplayMode(mon)
        val newX = mon.virtualX + (mode.width - width) / 2
        val newY = mon.virtualY + (mode.height - height) / 2
        window.setPosition(newX, newY)
      }
    } else {
      if (displayModeBeforeFullscreen.isEmpty) {
        storeCurrentWindowPositionAndDisplayMode()
      }
      val refreshRate = displayModeBeforeFullscreen.fold(0)(_.refreshRate)
      if (width != windowWidthBeforeFullscreen || height != windowHeightBeforeFullscreen) {
        val mon  = getCurrentDesktopMonitor()
        val mode = getDesktopDisplayMode(mon)
        val newX = mon.virtualX + (mode.width - width) / 2
        val newY = mon.virtualY + (mode.height - height) / 2
        windowing.setWindowMonitor(window.getWindowHandle(), 0L, newX, newY, width, height, refreshRate)
      } else {
        windowing.setWindowMonitor(window.getWindowHandle(), 0L, windowPosXBeforeFullscreen, windowPosYBeforeFullscreen, width, height, refreshRate)
      }
    }
    updateFramebufferInfo()
    true
  }

  override def setTitle(title: String): Unit =
    windowing.setWindowTitle(window.getWindowHandle(), if (title == null) "" else title)

  override def setUndecorated(undecorated: Boolean): Unit = {
    window.config.windowDecorated = !undecorated
    windowing.setWindowAttrib(window.getWindowHandle(), WindowingOps.GLFW_DECORATED, if (undecorated) 0 else 1)
  }

  override def setResizable(resizable: Boolean): Unit = {
    window.config.windowResizable = resizable
    windowing.setWindowAttrib(window.getWindowHandle(), WindowingOps.GLFW_RESIZABLE, if (resizable) 1 else 0)
  }

  override def setVSync(vsync: Boolean): Unit = {
    window.config.vSyncEnabled = vsync
    windowing.setSwapInterval(if (vsync) 1 else 0)
  }

  override def setForegroundFPS(fps: Int): Unit =
    window.config.foregroundFPS = fps

  // ─── Buffer format / extensions ──────────────────────────────────────

  override def getBufferFormat(): Graphics.BufferFormat = _bufferFormat

  override def supportsExtension(extension: String): Boolean =
    windowing.extensionSupported(extension)

  // ─── Continuous rendering ─────────────────────────────────────────────

  override def setContinuousRendering(isContinuous: Boolean): Unit =
    _isContinuous = isContinuous

  override def isContinuousRendering(): Boolean = _isContinuous

  override def requestRendering(): Unit =
    window.requestRendering()

  override def isFullscreen(): Boolean =
    windowing.getWindowMonitor(window.getWindowHandle()) != 0L

  // ─── Cursor ───────────────────────────────────────────────────────────

  override def newCursor(pixmap: Pixmap, xHotspot: Int, yHotspot: Int): Nullable[Cursor] =
    // Custom pixmap cursors require WindowingOps.createCursorFromImage (deferred)
    Nullable.empty

  override def setCursor(cursor: Cursor): Unit = cursor match {
    case dc: DesktopCursor =>
      windowing.setCursor(window.getWindowHandle(), dc.glfwCursor)
    case _ => ()
  }

  override def setSystemCursor(systemCursor: SystemCursor): Unit =
    DesktopCursor.setSystemCursor(window.getWindowHandle(), systemCursor)

  // ─── Lifecycle ────────────────────────────────────────────────────────

  override def close(): Unit = {
    // Cleanup callback — the window's close() will destroy the native window
  }
}

object DesktopGraphics {

  /** GL_TEXTURE_CUBE_MAP_SEAMLESS constant (desktop GL 3.2 extension). */
  private[sge] val GL_TEXTURE_CUBE_MAP_SEAMLESS: Int = 0x884f
}
