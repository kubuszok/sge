/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidGraphics.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidGraphics (same name, different package)
 *   Convention: delegates to ops interfaces; no Activity subclass needed in sge core
 *   Convention: GL setup via ops-based adapters (AndroidGL20Adapter/AndroidGL30Adapter)
 *   Idiom: split packages; Nullable; no return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.{ Cursor, GL20, GL30, GL31, GL32, Pixmap }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.glutils.GLVersion
import sge.graphics.{ AndroidGL20Adapter, AndroidGL30Adapter }
import sge.platform.android._
import sge.utils.{ Nullable, Seconds }

/** An implementation of [[Graphics]] for Android.
  *
  * Unlike LibGDX's AndroidGraphics (which implements GLSurfaceView.Renderer), this is a plain class that delegates all Android-specific operations to ops interfaces. The GL surface view's renderer
  * callback drives the frame loop externally.
  *
  * @param config
  *   the Android application configuration
  * @param provider
  *   the Android platform provider
  * @param displayMetrics
  *   display metrics operations
  * @param glSurfaceView
  *   the GL surface view operations
  * @param cursorOps
  *   cursor operations
  */
class AndroidGraphics(
  private val config:         AndroidConfigOps,
  private val provider:       AndroidPlatformProvider,
  private val displayMetrics: DisplayMetricsOps,
  private val glSurfaceView:  GLSurfaceViewOps,
  private val cursorOps:      CursorOps
) extends Graphics {

  // ─── GL instances ─────────────────────────────────────────────────────

  private var _gl20:       GL20             = scala.compiletime.uninitialized
  private var _gl30:       Nullable[GL30]   = Nullable.empty
  private var _glVersion:  GLVersion        = scala.compiletime.uninitialized
  private var _extensions: Nullable[String] = Nullable.empty

  // ─── Frame state ──────────────────────────────────────────────────────

  @volatile var _width:  Int = 0
  @volatile var _height: Int = 0

  private var _bufferFormat:  Graphics.BufferFormat = Graphics.BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.numSamples, false)
  private var _lastFrameTime: Long                  = System.nanoTime()
  private var _deltaTime:     Seconds               = Seconds.zero
  private var _frameId:       Long                  = -1L
  private var _frameStart:    Long                  = System.nanoTime()
  private var _frames:        Int                   = 0
  private var _fps:           Int                   = 0
  private var _isContinuous:  Boolean               = true

  // ─── Safe insets ──────────────────────────────────────────────────────

  @volatile private[sge] var _safeInsetLeft:   Int = 0
  @volatile private[sge] var _safeInsetTop:    Int = 0
  @volatile private[sge] var _safeInsetRight:  Int = 0
  @volatile private[sge] var _safeInsetBottom: Int = 0

  // ─── GL setup (called from renderer's onSurfaceCreated) ───────────────

  /** Sets up GL instances based on config and detected GL version.
    *
    * Called from the GL thread when the surface is created.
    *
    * @param versionString
    *   the GL_VERSION string
    * @param vendorString
    *   the GL_VENDOR string
    * @param rendererString
    *   the GL_RENDERER string
    */
  def setupGL(versionString: String, vendorString: String, rendererString: String): Unit = {
    _glVersion = new GLVersion(Application.ApplicationType.Android, versionString, vendorString, rendererString)
    if (config.useGL30 && _glVersion.majorVersion > 2) {
      if (_gl30.isEmpty) {
        val gl30Ops = provider.createGL30()
        val gl30    = AndroidGL30Adapter(gl30Ops)
        _gl30 = Nullable(gl30)
        _gl20 = gl30
      }
    } else {
      if (_gl20 == null) { // scalafix:ok
        val gl20Ops = provider.createGL20()
        _gl20 = AndroidGL20Adapter(gl20Ops)
      }
    }
  }

  /** Updates display metrics. Called when the surface changes or is created. */
  private[sge] def updatePpi(): Unit = {
    // DisplayMetricsOps caches the values — just trigger a refresh if needed
  }

  /** Updates safe area insets from display cutouts. */
  private[sge] def updateSafeInsets(window: AnyRef): Unit = {
    displayMetrics.updateSafeInsets(window)
    _safeInsetLeft = displayMetrics.safeInsetLeft
    _safeInsetTop = displayMetrics.safeInsetTop
    _safeInsetRight = displayMetrics.safeInsetRight
    _safeInsetBottom = displayMetrics.safeInsetBottom
  }

  /** Updates frame timing. Called from the render loop (onDrawFrame equivalent). */
  def updateFrameTiming(isResuming: Boolean): Unit = {
    val time = System.nanoTime()
    // After pause, deltaTime can be huge — cut it off on resume
    if (!isResuming) {
      _deltaTime = Seconds((time - _lastFrameTime) / 1000000000.0f)
    } else {
      _deltaTime = Seconds.zero
    }
    _lastFrameTime = time

    if (time - _frameStart > 1000000000L) {
      _fps = _frames
      _frames = 0
      _frameStart = time
    }
    _frames += 1
    _frameId += 1
  }

  /** Updates the buffer format after EGL config is determined. */
  private[sge] def updateBufferFormat(r: Int, g: Int, b: Int, a: Int, depth: Int, stencil: Int, samples: Int, coverageSampling: Boolean): Unit =
    _bufferFormat = Graphics.BufferFormat(r, g, b, a, depth, stencil, samples, coverageSampling)

  // ─── GL availability ──────────────────────────────────────────────────

  override def gl30Available: Boolean = _gl30.isDefined
  override def gl31Available: Boolean = false
  override def gl32Available: Boolean = false

  override def gl20: GL20           = _gl20
  override def gl30: Nullable[GL30] = _gl30
  override def gl31: Nullable[GL31] = Nullable.empty
  override def gl32: Nullable[GL32] = Nullable.empty

  override def gl20_=(value: GL20): Unit =
    _gl20 = value

  override def gl30_=(value: GL30): Unit = {
    _gl30 = Nullable(value)
    _gl20 = value
  }

  override def gl31_=(value: GL31): Unit = () // Not supported on Android in SGE
  override def gl32_=(value: GL32): Unit = () // Not supported on Android in SGE

  // ─── Dimensions ───────────────────────────────────────────────────────

  override def width:  Pixels = Pixels(_width)
  override def height: Pixels = Pixels(_height)

  // On Android, back buffer size equals logical size
  override def backBufferWidth:  Pixels = Pixels(_width)
  override def backBufferHeight: Pixels = Pixels(_height)

  override def backBufferScale: Float = 1f

  // ─── Safe insets ──────────────────────────────────────────────────────

  override def safeInsetLeft:   Pixels = Pixels(_safeInsetLeft)
  override def safeInsetTop:    Pixels = Pixels(_safeInsetTop)
  override def safeInsetBottom: Pixels = Pixels(_safeInsetBottom)
  override def safeInsetRight:  Pixels = Pixels(_safeInsetRight)

  // ─── Frame timing ────────────────────────────────────────────────────

  override def frameId:         Long    = _frameId
  override def deltaTime:       Seconds = _deltaTime
  override def rawDeltaTime:    Seconds = _deltaTime
  override def framesPerSecond: Int     = _fps

  // ─── Type / version ──────────────────────────────────────────────────

  override def graphicsType: Graphics.GraphicsType = Graphics.GraphicsType.AndroidGL
  override def glVersion:    Graphics.GLVersion    = _glVersion

  // ─── DPI / density ───────────────────────────────────────────────────

  override def ppiX:    Float = displayMetrics.ppiX
  override def ppiY:    Float = displayMetrics.ppiY
  override def ppcX:    Float = displayMetrics.ppcX
  override def ppcY:    Float = displayMetrics.ppcY
  override def density: Float = displayMetrics.density

  // ─── Display modes / monitors ─────────────────────────────────────────

  override def supportsDisplayModeChange(): Boolean = false

  override def primaryMonitor: Graphics.Monitor        = Graphics.Monitor(0, 0, "Primary Monitor")
  override def monitor:        Graphics.Monitor        = primaryMonitor
  override def monitors:       Array[Graphics.Monitor] = Array(primaryMonitor)

  override def displayModes: Array[Graphics.DisplayMode] = Array(displayMode)

  override def getDisplayModes(mon: Graphics.Monitor): Array[Graphics.DisplayMode] = displayModes

  override def displayMode: Graphics.DisplayMode = {
    val bpp                 = config.r + config.g + config.b + config.a
    val (w, h, refresh, bp) = displayMetrics.displayMode(glSurfaceView.view, bpp)
    Graphics.DisplayMode(w, h, refresh, bp)
  }

  override def getDisplayMode(mon: Graphics.Monitor): Graphics.DisplayMode = displayMode

  override def setFullscreenMode(displayMode: Graphics.DisplayMode): Boolean = false

  override def setWindowedMode(width: Pixels, height: Pixels): Boolean = false

  override def setTitle(title: String): Unit = () // Ignored on Android

  override def setUndecorated(undecorated: Boolean): Unit = () // Handled by Activity flags

  override def setResizable(resizable: Boolean): Unit = () // Ignored on Android

  override def setVSync(vsync: Boolean): Unit = () // No-op on Android

  override def setForegroundFPS(fps: Int): Unit = () // Not applicable on Android

  // ─── Buffer format / extensions ──────────────────────────────────────

  override def bufferFormat: Graphics.BufferFormat = _bufferFormat

  override def supportsExtension(extension: String): Boolean = {
    val ext = _extensions.fold {
      val e = _gl20.glGetString(GL20.GL_EXTENSIONS)
      _extensions = Nullable(e)
      e
    }(identity)
    ext.contains(extension)
  }

  // ─── Continuous rendering ─────────────────────────────────────────────

  override def setContinuousRendering(isContinuous: Boolean): Unit = {
    _isContinuous = isContinuous
    glSurfaceView.setContinuousRendering(isContinuous)
  }

  override def continuousRendering: Boolean = _isContinuous

  override def requestRendering(): Unit = glSurfaceView.requestRender()

  override def fullscreen: Boolean = true

  // ─── Cursor ───────────────────────────────────────────────────────────

  override def newCursor(pixmap: Pixmap, xHotspot: Pixels, yHotspot: Pixels): Nullable[Cursor] =
    Nullable.empty

  override def setCursor(cursor: Cursor): Unit = () // Custom pixmap cursors not supported on Android

  override def setSystemCursor(systemCursor: SystemCursor): Unit =
    cursorOps.setSystemCursor(glSurfaceView.view, systemCursor.ordinal)
}
