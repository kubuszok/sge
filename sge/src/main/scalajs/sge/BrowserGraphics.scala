/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtGraphics.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtGraphics -> BrowserGraphics; GwtMonitor -> BrowserMonitor
 *   Convention: Scala.js only; JSNI -> js.Dynamic for fullscreen/orientation/density
 *   Convention: Canvas created externally (by BrowserApplication), passed as constructor param
 *   Convention: GL20/GL30 created externally and passed in (WebGL20/WebGL30 are separate files)
 *   Idiom: OrientationLockType moved to BrowserApplicationConfig companion
 *   Idiom: Nullable for GL30/GL31/GL32 return types
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import org.scalajs.dom
import org.scalajs.dom.{ HTMLCanvasElement, document, window }
import scala.scalajs.js
import sge.graphics.{ Cursor, GL20, GL30, GL31, GL32, Pixmap, WebGL20 }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.glutils.GLVersion
import sge.utils.Nullable

/** Browser Graphics implementation using an HTML Canvas and WebGL.
  *
  * @param canvas
  *   the HTML canvas element
  * @param config
  *   the browser application configuration
  * @param initialGL20
  *   the initial GL20 implementation (WebGL20 or WebGL30)
  * @param initialGL30
  *   the initial GL30 implementation, or Nullable.empty if WebGL2 not available
  * @param glVersion
  *   the detected GL version
  */
class BrowserGraphics(
  val canvas:             HTMLCanvasElement,
  config:                 BrowserApplicationConfig,
  private var _gl20:      GL20,
  private var _gl30:      Nullable[GL30],
  private val _glVersion: GLVersion
) extends Graphics {

  private var fps:           Float = 0f
  private var lastTimeStamp: Long  = System.currentTimeMillis()
  var _frameId:              Long  = -1L
  private var _deltaTime:    Float = 0f
  private var time:          Float = 0f
  private var frames:        Int   = 0

  // --- GL accessors ---

  override def gl30Available: Boolean = _gl30.isDefined
  override def gl31Available: Boolean = false
  override def gl32Available: Boolean = false

  override def gl20: GL20           = _gl20
  override def gl30: Nullable[GL30] = _gl30
  override def gl31: Nullable[GL31] = Nullable.empty
  override def gl32: Nullable[GL32] = Nullable.empty

  override def gl20_=(value: GL20): Unit = _gl20 = value

  override def gl30_=(value: GL30): Unit = {
    _gl30 = Nullable(value)
    _gl20 = value.asInstanceOf[GL20]
  }

  override def gl31_=(value: GL31): Unit = ()
  override def gl32_=(value: GL32): Unit = ()

  // --- Dimensions ---

  override def width:            Pixels = Pixels(canvas.width)
  override def height:           Pixels = Pixels(canvas.height)
  override def backBufferWidth:  Pixels = Pixels(canvas.width)
  override def backBufferHeight: Pixels = Pixels(canvas.height)
  override def backBufferScale:  Float  = 1f

  override def safeInsetLeft:   Pixels = Pixels.zero
  override def safeInsetTop:    Pixels = Pixels.zero
  override def safeInsetBottom: Pixels = Pixels.zero
  override def safeInsetRight:  Pixels = Pixels.zero

  // --- Frame timing ---

  override def frameId:         Long  = _frameId
  override def deltaTime:       Float = _deltaTime
  override def rawDeltaTime:    Float = _deltaTime
  override def framesPerSecond: Int   = fps.toInt

  /** Update frame timing. Called once per frame by BrowserApplication. */
  def update(): Unit = {
    val currTimeStamp = System.currentTimeMillis()
    _deltaTime = (currTimeStamp - lastTimeStamp) / 1000.0f
    lastTimeStamp = currTimeStamp
    time += _deltaTime
    frames += 1
    if (time > 1) {
      fps = frames.toFloat
      time = 0
      frames = 0
    }
  }

  // --- Display info ---

  override def graphicsType: Graphics.GraphicsType = Graphics.GraphicsType.WebGL
  override def glVersion:    GLVersion             = _glVersion

  override def ppiX:    Float = 96f * getNativeScreenDensity().toFloat
  override def ppiY:    Float = 96f * getNativeScreenDensity().toFloat
  override def ppcX:    Float = ppiX / 2.54f
  override def ppcY:    Float = ppiY / 2.54f
  override def density: Float = getNativeScreenDensity().toFloat / 160f

  // --- Display modes ---

  override def supportsDisplayModeChange(): Boolean =
    BrowserFeaturePolicy.allowsFeature("fullscreen") && supportsFullscreen

  override def displayModes: Array[Graphics.DisplayMode] =
    Array(displayMode)

  override def displayMode: Graphics.DisplayMode = {
    val d  = if (config.usePhysicalPixels) getNativeScreenDensity() else 1.0
    val sw = (window.screen.width * d).toInt
    val sh = (window.screen.height * d).toInt
    val cd = window.screen.asInstanceOf[js.Dynamic].colorDepth.asInstanceOf[Int]
    Graphics.DisplayMode(sw, sh, 60, cd)
  }

  override def getDisplayModes(monitor: Graphics.Monitor): Array[Graphics.DisplayMode] =
    displayModes

  override def getDisplayMode(monitor: Graphics.Monitor): Graphics.DisplayMode =
    displayMode

  private val _primaryMonitor: Graphics.Monitor = Graphics.Monitor(0, 0, "Primary Monitor")

  override def primaryMonitor: Graphics.Monitor = _primaryMonitor

  override def monitor: Graphics.Monitor = _primaryMonitor

  override def monitors: Array[Graphics.Monitor] =
    Array(_primaryMonitor)

  // --- Fullscreen ---

  override def setFullscreenMode(displayMode: Graphics.DisplayMode): Boolean = {
    val supported = this.displayMode
    if (displayMode.width != supported.width && displayMode.height != supported.height) false
    else {
      canvas.width = displayMode.width
      canvas.height = displayMode.height
      val elem = canvas.asInstanceOf[js.Dynamic]
      if (!js.isUndefined(elem.requestFullscreen)) {
        elem.requestFullscreen()
        document.addEventListener("fullscreenchange", (_: dom.Event) => fullscreenChanged())
        true
      } else if (!js.isUndefined(elem.webkitRequestFullScreen)) {
        elem.webkitRequestFullScreen()
        document.addEventListener("webkitfullscreenchange", (_: dom.Event) => fullscreenChanged())
        true
      } else false
    }
  }

  override def setWindowedMode(width: Pixels, height: Pixels): Boolean = {
    if (fullscreen) exitFullscreen()
    if (config.isFixedSizeApplication) setCanvasSize(width.toInt, height.toInt)
    true
  }

  override def fullscreen: Boolean = {
    val doc = document.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(doc.fullscreenElement)) doc.fullscreenElement != null
    else if (!js.isUndefined(doc.webkitFullscreenElement)) doc.webkitFullscreenElement != null
    else false
  }

  private def supportsFullscreen: Boolean = {
    val doc = document.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(doc.fullscreenEnabled)) doc.fullscreenEnabled.asInstanceOf[Boolean]
    else if (!js.isUndefined(doc.webkitFullscreenEnabled)) doc.webkitFullscreenEnabled.asInstanceOf[Boolean]
    else false
  }

  private def exitFullscreen(): Unit = {
    val doc = document.asInstanceOf[js.Dynamic]
    if (!js.isUndefined(doc.exitFullscreen)) doc.exitFullscreen()
    else if (!js.isUndefined(doc.webkitExitFullscreen)) doc.webkitExitFullscreen()
  }

  private def fullscreenChanged(): Unit =
    if (!fullscreen) {
      if (config.isFixedSizeApplication) setCanvasSize(config.width, config.height)
    }

  /** Set the canvas size in pixels. */
  def setCanvasSize(width: Int, height: Int): Unit = {
    canvas.width = width
    canvas.height = height
    if (config.usePhysicalPixels) {
      val density = getNativeScreenDensity()
      canvas.style.width = s"${width / density}px"
      canvas.style.height = s"${height / density}px"
    }
  }

  // --- Window properties ---

  override def setTitle(title:                      String):  Unit    = document.title = title
  override def setUndecorated(undecorated:          Boolean): Unit    = ()
  override def setResizable(resizable:              Boolean): Unit    = ()
  override def setVSync(vsync:                      Boolean): Unit    = ()
  override def setForegroundFPS(fps:                Int):     Unit    = ()
  override def setContinuousRendering(isContinuous: Boolean): Unit    = ()
  override def continuousRendering:                           Boolean = true
  override def requestRendering():                            Unit    = ()

  // --- Buffer format ---

  override def bufferFormat: Graphics.BufferFormat =
    Graphics.BufferFormat(8, 8, 8, 0, 16, if (config.stencil) 8 else 0, 0, false)

  override def supportsExtension(extension: String): Boolean = {
    // Use the existing GL context — do NOT create a second context via canvas.getContext()
    val gl        = _gl20.asInstanceOf[WebGL20].gl
    val supported = gl.getSupportedExtensions()
    if (supported == null) false
    else {
      val arr = supported.asInstanceOf[js.Array[String]]
      arr.exists(_ == extension)
    }
  }

  // --- Cursor ---

  override def newCursor(pixmap: Pixmap, xHotspot: Pixels, yHotspot: Pixels): Nullable[Cursor] =
    // Custom pixmap cursors require encoding the pixmap as a data URL — deferred
    Nullable.empty

  override def setCursor(cursor: Cursor): Unit =
    canvas.style.cursor = cursor.asInstanceOf[BrowserCursor].cssCursorProperty

  override def setSystemCursor(systemCursor: SystemCursor): Unit =
    canvas.style.cursor = BrowserCursor.cssNameFor(systemCursor)

  // --- Helpers ---

  private def getNativeScreenDensity(): Double =
    window.asInstanceOf[js.Dynamic].devicePixelRatio.asInstanceOf[js.UndefOr[Double]].getOrElse(1.0)
}
