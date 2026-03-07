/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/graphics/MockGraphics.java
 *   Renames: MockGraphics -> NoopGraphics
 *   Convention: extends Graphics directly (Java extends AbstractGraphics); configurable width/height (Java returns 0);
 *     non-null monitor/displayMode stubs (Java returns null); getGL20 throws (Java returns null);
 *     getGL30/31/32 return Nullable.empty (Java returns null); getGLVersion returns new Object (Java has GLVersion);
 *     getPpi/Ppc return 96-based values (Java returns 0); getBufferFormat returns real instance (Java returns null);
 *     setContinuousRendering/isContinuousRendering track state (Java ignores); incrementFrameId() merged into updateTime();
 *     getTargetRenderInterval/targetRenderInterval omitted (backend-specific)
 *   Idiom: Nullable (getGL30/31/32, newCursor), split packages
 *   Audited: 2026-03-04
 */
package sge
package noop

import sge.Application
import sge.graphics.{ Cursor, Pixmap }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.{ GL20, GL30, GL31, GL32 }
import sge.graphics.glutils.GLVersion
import sge.utils.{ Nanos, Nullable, TimeUtils }

/** A no-op [[sge.Graphics]] implementation for headless/testing use. Tracks frame timing via [[updateTime]] but provides no GL context.
  */
class NoopGraphics(
  val noopWidth:  Int = 640,
  val noopHeight: Int = 480
) extends Graphics {

  private var _deltaTime:         Float           = 0.0f
  private var _frameId:           Long            = 0L
  private var _fps:               Int             = 0
  private var _lastFrameTime:     sge.utils.Nanos = TimeUtils.nanoTime()
  private var _frameCounterStart: sge.utils.Nanos = _lastFrameTime
  private var _frameCounter:      Int             = 0
  private var _continuous:        Boolean         = true

  /** Advances frame timing. Call once per headless app loop iteration. */
  def updateTime(): Unit = {
    val now = TimeUtils.nanoTime()
    _deltaTime = (now - _lastFrameTime).toFloat / 1000000000.0f
    _lastFrameTime = now
    _frameId += 1L
    _frameCounter += 1

    val elapsed = now - _frameCounterStart
    if (elapsed >= sge.utils.Nanos(1000000000L)) {
      _fps = _frameCounter
      _frameCounter = 0
      _frameCounterStart = now
    }
  }

  // ---- GL availability ----

  override def isGL30Available(): Boolean = false

  override def isGL31Available(): Boolean = false

  override def isGL32Available(): Boolean = false

  override def getGL20(): GL20 =
    throw new UnsupportedOperationException("NoopGraphics has no GL context")

  override def getGL30(): Nullable[GL30] = Nullable.empty

  override def getGL31(): Nullable[GL31] = Nullable.empty

  override def getGL32(): Nullable[GL32] = Nullable.empty

  override def setGL20(gl20: GL20): Unit = {}

  override def setGL30(gl30: GL30): Unit = {}

  override def setGL31(gl31: GL31): Unit = {}

  override def setGL32(gl32: GL32): Unit = {}

  // ---- dimensions ----

  override def getWidth(): Pixels = Pixels(noopWidth)

  override def getHeight(): Pixels = Pixels(noopHeight)

  override def getBackBufferWidth(): Pixels = Pixels(noopWidth)

  override def getBackBufferHeight(): Pixels = Pixels(noopHeight)

  override def getBackBufferScale(): Float = 1.0f

  override def getSafeInsetLeft(): Pixels = Pixels.zero

  override def getSafeInsetTop(): Pixels = Pixels.zero

  override def getSafeInsetBottom(): Pixels = Pixels.zero

  override def getSafeInsetRight(): Pixels = Pixels.zero

  // ---- timing ----

  override def getFrameId(): Long = _frameId

  override def getDeltaTime(): Float = _deltaTime

  @deprecated("use getDeltaTime() instead", "1.0")
  override def getRawDeltaTime(): Float = _deltaTime

  override def getFramesPerSecond(): Int = _fps

  // ---- type and version ----

  override def getType(): Graphics.GraphicsType = Graphics.GraphicsType.Mock

  override def getGLVersion(): Graphics.GLVersion =
    GLVersion(Application.ApplicationType.HeadlessDesktop, "0.0.0", "Noop", "Noop")

  // ---- density / PPI ----

  override def getPpiX(): Float = 96.0f

  override def getPpiY(): Float = 96.0f

  override def getPpcX(): Float = 96.0f / 2.54f

  override def getPpcY(): Float = 96.0f / 2.54f

  override def getDensity(): Float = 1.0f

  // ---- display modes / monitors ----

  override def supportsDisplayModeChange(): Boolean = false

  private val noopMonitor: Graphics.Monitor = Graphics.Monitor(0, 0, "Noop Monitor")

  private val noopDisplayMode: Graphics.DisplayMode = Graphics.DisplayMode(noopWidth, noopHeight, 60, 32)

  override def getPrimaryMonitor(): Graphics.Monitor = noopMonitor

  override def getMonitor(): Graphics.Monitor = noopMonitor

  override def getMonitors(): Array[Graphics.Monitor] = Array(noopMonitor)

  override def getDisplayModes(): Array[Graphics.DisplayMode] = Array(noopDisplayMode)

  override def getDisplayModes(monitor: Graphics.Monitor): Array[Graphics.DisplayMode] = Array(noopDisplayMode)

  override def getDisplayMode(): Graphics.DisplayMode = noopDisplayMode

  override def getDisplayMode(monitor: Graphics.Monitor): Graphics.DisplayMode = noopDisplayMode

  // ---- window operations (no-ops) ----

  override def setFullscreenMode(displayMode: Graphics.DisplayMode): Boolean = false

  override def setWindowedMode(width: Pixels, height: Pixels): Boolean = false

  override def setTitle(title: String): Unit = {}

  override def setUndecorated(undecorated: Boolean): Unit = {}

  override def setResizable(resizable: Boolean): Unit = {}

  override def setVSync(vsync: Boolean): Unit = {}

  override def setForegroundFPS(fps: Int): Unit = {}

  // ---- buffer format ----

  override def getBufferFormat(): Graphics.BufferFormat =
    Graphics.BufferFormat(r = 8, g = 8, b = 8, a = 8, depth = 24, stencil = 8, samples = 0, coverageSampling = false)

  override def supportsExtension(extension: String): Boolean = false

  // ---- continuous rendering ----

  override def setContinuousRendering(isContinuous: Boolean): Unit =
    _continuous = isContinuous

  override def isContinuousRendering(): Boolean = _continuous

  override def requestRendering(): Unit = {}

  override def isFullscreen(): Boolean = false

  // ---- cursor (no-ops) ----

  override def newCursor(pixmap: Pixmap, xHotspot: Pixels, yHotspot: Pixels): Nullable[Cursor] = Nullable.empty

  override def setCursor(cursor: Cursor): Unit = {}

  override def setSystemCursor(systemCursor: SystemCursor): Unit = {}
}
