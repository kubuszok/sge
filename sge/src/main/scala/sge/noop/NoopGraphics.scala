/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/graphics/MockGraphics.java
 *   Renames: MockGraphics -> NoopGraphics
 *   Convention: extends Graphics directly (Java extends AbstractGraphics); configurable width/height (Java returns 0);
 *     non-null monitor/displayMode stubs (Java returns null); gl20 throws (Java returns null);
 *     gl30/31/32 return Nullable.empty (Java returns null); glVersion returns new Object (Java has GLVersion);
 *     getPpi/Ppc return 96-based values (Java returns 0); bufferFormat returns real instance (Java returns null);
 *     setContinuousRendering/continuousRendering track state (Java ignores); incrementFrameId() merged into updateTime();
 *     getTargetRenderInterval/targetRenderInterval omitted (backend-specific)
 *   Idiom: Nullable (gl30/31/32, newCursor), split packages
 *   Audited: 2026-03-04
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 195
 * Covenant-baseline-methods: NoopGraphics,_continuous,_deltaTime,_fps,_frameCounter,_frameCounterStart,_frameId,_lastFrameTime,backBufferHeight,backBufferScale,backBufferWidth,bufferFormat,continuousRendering,deltaTime,density,displayMode,displayModes,elapsed,frameId,framesPerSecond,fullscreen,getDisplayMode,getDisplayModes,gl20,gl20_,gl30,gl30Available,gl30_,gl31,gl31Available,gl31_,gl32,gl32Available,gl32_,glVersion,graphicsType,height,monitor,monitors,newCursor,noopDisplayMode,noopHeight,noopMonitor,noopWidth,now,ppcX,ppcY,ppiX,ppiY,primaryMonitor,rawDeltaTime,requestRendering,safeInsetBottom,safeInsetLeft,safeInsetRight,safeInsetTop,setContinuousRendering,setCursor,setForegroundFPS,setFullscreenMode,setResizable,setSystemCursor,setTitle,setUndecorated,setVSync,setWindowedMode,supportsDisplayModeChange,supportsExtension,updateTime,width
 * Covenant-source-reference: backends/gdx-backend-headless/mock/graphics/MockGraphics.java
 * Covenant-verified: 2026-04-19
 */
package sge
package noop

import sge.Application
import sge.graphics.{ Cursor, Pixmap }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.{ GL20, GL30, GL31, GL32 }
import sge.graphics.glutils.GLVersion
import sge.utils.{ Nanos, Nullable, Seconds, TimeUtils }

/** A no-op [[sge.Graphics]] implementation for headless/testing use. Tracks frame timing via [[updateTime]] but provides no GL context.
  */
class NoopGraphics(
  val noopWidth:  Int = 640,
  val noopHeight: Int = 480
) extends Graphics {

  private var _deltaTime:         Seconds         = Seconds.zero
  private var _frameId:           Long            = 0L
  private var _fps:               Int             = 0
  private var _lastFrameTime:     sge.utils.Nanos = TimeUtils.nanoTime()
  private var _frameCounterStart: sge.utils.Nanos = _lastFrameTime
  private var _frameCounter:      Int             = 0
  private var _continuous:        Boolean         = true

  /** Advances frame timing. Call once per headless app loop iteration. */
  def updateTime(): Unit = {
    val now = TimeUtils.nanoTime()
    _deltaTime = Seconds((now - _lastFrameTime).toFloat / 1000000000.0f)
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

  override def gl30Available: Boolean = false

  override def gl31Available: Boolean = false

  override def gl32Available: Boolean = false

  override def gl20: GL20 =
    throw new UnsupportedOperationException("NoopGraphics has no GL context")

  override def gl30: Nullable[GL30] = Nullable.empty

  override def gl31: Nullable[GL31] = Nullable.empty

  override def gl32: Nullable[GL32] = Nullable.empty

  override def gl20_=(value: GL20): Unit = {}

  override def gl30_=(value: GL30): Unit = {}

  override def gl31_=(value: GL31): Unit = {}

  override def gl32_=(value: GL32): Unit = {}

  // ---- dimensions ----

  override def width: Pixels = Pixels(noopWidth)

  override def height: Pixels = Pixels(noopHeight)

  override def backBufferWidth: Pixels = Pixels(noopWidth)

  override def backBufferHeight: Pixels = Pixels(noopHeight)

  override def backBufferScale: Float = 1.0f

  override def safeInsetLeft: Pixels = Pixels.zero

  override def safeInsetTop: Pixels = Pixels.zero

  override def safeInsetBottom: Pixels = Pixels.zero

  override def safeInsetRight: Pixels = Pixels.zero

  // ---- timing ----

  override def frameId: Long = _frameId

  override def deltaTime: Seconds = _deltaTime

  @deprecated("use deltaTime instead", "1.0")
  override def rawDeltaTime: Seconds = _deltaTime

  override def framesPerSecond: Int = _fps

  // ---- type and version ----

  override def graphicsType: Graphics.GraphicsType = Graphics.GraphicsType.Mock

  override def glVersion: Graphics.GLVersion =
    GLVersion(Application.ApplicationType.HeadlessDesktop, "0.0.0", "Noop", "Noop")

  // ---- density / PPI ----

  override def ppiX: Float = 96.0f

  override def ppiY: Float = 96.0f

  override def ppcX: Float = 96.0f / 2.54f

  override def ppcY: Float = 96.0f / 2.54f

  override def density: Float = 1.0f

  // ---- display modes / monitors ----

  override def supportsDisplayModeChange(): Boolean = false

  private val noopMonitor: Graphics.Monitor = Graphics.Monitor(0, 0, "Noop Monitor")

  private val noopDisplayMode: Graphics.DisplayMode = Graphics.DisplayMode(noopWidth, noopHeight, 60, 32)

  override def primaryMonitor: Graphics.Monitor = noopMonitor

  override def monitor: Graphics.Monitor = noopMonitor

  override def monitors: Array[Graphics.Monitor] = Array(noopMonitor)

  override def displayModes: Array[Graphics.DisplayMode] = Array(noopDisplayMode)

  override def getDisplayModes(monitor: Graphics.Monitor): Array[Graphics.DisplayMode] = Array(noopDisplayMode)

  override def displayMode: Graphics.DisplayMode = noopDisplayMode

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

  override def bufferFormat: Graphics.BufferFormat =
    Graphics.BufferFormat(r = 8, g = 8, b = 8, a = 8, depth = 24, stencil = 8, samples = 0, coverageSampling = false)

  override def supportsExtension(extension: String): Boolean = false

  // ---- continuous rendering ----

  override def setContinuousRendering(isContinuous: Boolean): Unit =
    _continuous = isContinuous

  override def continuousRendering: Boolean = _continuous

  override def requestRendering(): Unit = {}

  override def fullscreen: Boolean = false

  // ---- cursor (no-ops) ----

  override def newCursor(pixmap: Pixmap, xHotspot: Pixels, yHotspot: Pixels): Nullable[Cursor] = Nullable.empty

  override def setCursor(cursor: Cursor): Unit = {}

  override def setSystemCursor(systemCursor: SystemCursor): Unit = {}
}
