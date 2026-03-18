/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtApplication.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtApplication -> BrowserApplication
 *   Convention: Scala.js only; GWT EntryPoint/Panel/Widget -> direct DOM manipulation
 *   Convention: GWT AnimationScheduler -> window.requestAnimationFrame
 *   Convention: Preloader replaced by BrowserAssetLoader (fetch-based)
 *   Convention: AgentInfo JSNI helper dropped (use navigator.userAgent directly)
 *   Convention: LoadingListener callback replaced by simple onReady/onSetup hooks
 *   Idiom: Gdx.* globals -> Sge context (given); postRunnable uses scala.collection.mutable.ArrayBuffer
 *   Idiom: GdxRuntimeException -> SgeError.GraphicsError
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import org.scalajs.dom
import org.scalajs.dom.{ HTMLCanvasElement, document, window }
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js
import sge.audio.DefaultBrowserAudio
import sge.files.{ BrowserAssetLoader, BrowserFiles }
import sge.graphics.{ GL20, GL30, WebGL20, WebGL30 }
import sge.graphics.glutils.GLVersion
import sge.input.DefaultBrowserInput
import sge.noop.NoopAudio
import sge.utils.{ Clipboard, Nullable }

/** Browser (Scala.js) application entry point. Creates an HTML canvas, obtains a WebGL context, sets up all subsystems (graphics, audio, input, files, net), and runs a `requestAnimationFrame`-based
  * main loop.
  *
  * Usage:
  * {{{
  *   val app = BrowserApplication(MyListener(), new BrowserApplicationConfig(800, 600))
  * }}}
  *
  * @param listenerFactory
  *   a context function that creates the application listener when given an [[Sge]] context
  * @param config
  *   the browser application configuration
  * @param baseUrl
  *   base URL for asset loading (default: "assets/")
  */
class BrowserApplication(
  listenerFactory: Sge ?=> ApplicationListener,
  config:          BrowserApplicationConfig,
  baseUrl:         String = "assets/"
) extends Application {

  // --- Runnables ---

  private val runnables:       ArrayBuffer[Runnable] = ArrayBuffer.empty
  private val runnablesHelper: ArrayBuffer[Runnable] = ArrayBuffer.empty

  // --- Lifecycle listeners ---

  private val lifecycleListeners: ArrayBuffer[LifecycleListener] = ArrayBuffer.empty

  // --- Preferences cache ---

  private val prefsCache: scala.collection.mutable.Map[String, Preferences] =
    scala.collection.mutable.Map.empty

  // --- Subsystems ---

  private val canvas                     = createCanvas()
  private val (gl20, gl30Opt, glVersion) = createGLContext(canvas)

  private val _graphics = new BrowserGraphics(canvas, config, gl20, gl30Opt, glVersion)
  private val _audio: Audio = if (!config.disableAudio) new DefaultBrowserAudio(this) else new NoopAudio
  private val assetLoader = new BrowserAssetLoader(baseUrl)
  private val _files      = new BrowserFiles(assetLoader)
  private val _net        = new BrowserNet(config)
  private val _clipboard  = new BrowserClipboard
  private val _input: Input = new DefaultBrowserInput(canvas, config)(using sgeContext)

  // Sge context — lazy so that DefaultBrowserInput (by-name Sge) can be constructed
  // before sgeContext is evaluated. All subsystem vals above are initialized first.
  lazy val sgeContext: Sge = Sge(
    application = this,
    graphics = _graphics,
    audio = _audio,
    files = _files,
    input = _input,
    net = _net
  )

  // Set initial viewport
  gl20.glViewport(Pixels.zero, Pixels.zero, _graphics.width, _graphics.height)

  private var lastWidth:  Pixels = _graphics.width
  private var lastHeight: Pixels = _graphics.height

  // Materialize listener from factory now that the Sge context is ready
  private val listener: ApplicationListener = {
    given Sge = sgeContext
    listenerFactory
  }

  // Preload assets from manifest if enabled, then start the game.
  // If assets.txt is not found (404), start without preloading.
  if (config.preloadAssets) {
    import scala.concurrent.ExecutionContext.Implicits.global
    val callback = new BrowserAssetLoader.PreloadCallback {
      def update(loaded: Int, total: Int, lastAssetSize: Long): Unit = {
        val loadingDiv = document.getElementById("loading")
        if (loadingDiv != null) {
          loadingDiv.textContent = s"Loading assets\u2026 $loaded / $total"
        }
      }
      def error(path: String, cause: Throwable): Unit =
        dom.console.warn(s"[sge] Failed to preload: $path \u2014 ${cause.getMessage}")
      def finished(): Unit = ()
    }
    assetLoader
      .preload("assets.txt", callback)
      .toFuture
      .recover { case _: Throwable =>
        dom.console.info("[sge] No assets.txt manifest found; starting without preload")
      }
      .foreach(_ => startGameLoop())
  } else {
    startGameLoop()
  }

  private def startGameLoop(): Unit = {
    // Remove the loading overlay now that the game is ready
    val loadingDiv = document.getElementById("loading")
    if (loadingDiv != null) loadingDiv.parentNode.removeChild(loadingDiv)

    // Initialize the listener
    listener.create()
    listener.resize(_graphics.width, _graphics.height)

    // Set up visibility change listener
    addVisibilityChangeListener()

    // Set up resize listener for non-fixed-size applications
    if (!config.isFixedSizeApplication) {
      addResizeListener()
    }

    // Start the main loop
    setupMainLoop()
  }

  // --- Canvas creation ---

  private def createCanvas(): HTMLCanvasElement = {
    val canvas: HTMLCanvasElement = config.canvasId.fold {
      // No canvasId specified — create a new canvas element and append to body
      val c = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
      document.body.appendChild(c)
      c
    } { id =>
      document.getElementById(id).asInstanceOf[HTMLCanvasElement]
    }

    if (!config.isFixedSizeApplication) {
      // Resizable application — fill the browser window minus padding
      val density = if (config.usePhysicalPixels) {
        window.asInstanceOf[js.Dynamic].devicePixelRatio.asInstanceOf[js.UndefOr[Double]].getOrElse(1.0)
      } else 1.0
      val width  = ((window.innerWidth.toInt - config.padHorizontal) * density).toInt
      val height = ((window.innerHeight.toInt - config.padVertical) * density).toInt
      canvas.width = width
      canvas.height = height
      if (config.usePhysicalPixels) {
        canvas.style.width = s"${window.innerWidth.toInt - config.padHorizontal}px"
        canvas.style.height = s"${window.innerHeight.toInt - config.padVertical}px"
      }
    } else {
      canvas.width = config.width
      canvas.height = config.height
      if (config.usePhysicalPixels) {
        val density =
          window.asInstanceOf[js.Dynamic].devicePixelRatio.asInstanceOf[js.UndefOr[Double]].getOrElse(1.0)
        canvas.style.width = s"${(config.width / density).toInt}px"
        canvas.style.height = s"${(config.height / density).toInt}px"
      }
    }

    canvas
  }

  // --- WebGL context creation ---

  private def createGLContext(
    canvas: HTMLCanvasElement
  ): (GL20, Nullable[GL30], GLVersion) = {
    val attributes = js.Dynamic.literal(
      antialias = config.antialiasing,
      stencil = config.stencil,
      alpha = config.alpha,
      premultipliedAlpha = config.premultipliedAlpha,
      preserveDrawingBuffer = config.preserveDrawingBuffer,
      xrCompatible = config.xrCompatible
    )

    var context: js.Dynamic     = null.asInstanceOf[js.Dynamic]
    var gl20:    GL20           = null.asInstanceOf[GL20]
    var gl30:    Nullable[GL30] = Nullable.empty
    var isWebGL2 = false

    if (config.useGL30) {
      context = canvas.asInstanceOf[js.Dynamic].getContext("webgl2", attributes).asInstanceOf[js.Dynamic]
      if (context != null && !js.isUndefined(context)) {
        isWebGL2 = true
      } else {
        context = null.asInstanceOf[js.Dynamic]
      }
    }

    if (isWebGL2) {
      val webgl30 = new WebGL30(context)
      gl30 = Nullable(webgl30)
      gl20 = webgl30 // WebGL30 extends WebGL20 which extends GL20
    } else {
      context = canvas.asInstanceOf[js.Dynamic].getContext("webgl", attributes).asInstanceOf[js.Dynamic]
      if (context == null || js.isUndefined(context)) {
        context = canvas.asInstanceOf[js.Dynamic].getContext("experimental-webgl", attributes).asInstanceOf[js.Dynamic]
      }
      if (context == null || js.isUndefined(context)) {
        throw utils.SgeError.GraphicsError("WebGL not supported by this browser")
      }
      gl20 = new WebGL20(context)
    }

    val versionString  = gl20.glGetString(GL20.GL_VERSION)
    val vendorString   = gl20.glGetString(GL20.GL_VENDOR)
    val rendererString = gl20.glGetString(GL20.GL_RENDERER)
    val glVersion      = new GLVersion(Application.ApplicationType.WebGL, versionString, vendorString, rendererString)

    (gl20, gl30, glVersion)
  }

  // --- Main loop ---

  private val mainLoopCallback: js.Function1[Double, Unit] = { (_: Double) =>
    try
      mainLoop()
    catch {
      case t: Throwable =>
        utils.Log.error(s"BrowserApplication exception: ${t.getMessage}", t)
        throw t
    }
    window.requestAnimationFrame(mainLoopCallback)
  }

  private def setupMainLoop(): Unit =
    window.requestAnimationFrame(mainLoopCallback)

  private def mainLoop(): Unit = {
    _graphics.update()

    // Check for resize
    if (_graphics.width != lastWidth || _graphics.height != lastHeight) {
      lastWidth = _graphics.width
      lastHeight = _graphics.height
      _graphics.gl20.glViewport(Pixels.zero, Pixels.zero, lastWidth, lastHeight)
      listener.resize(lastWidth, lastHeight)
    }

    // Process posted runnables
    runnablesHelper.addAll(runnables)
    runnables.clear()
    var i = 0
    while (i < runnablesHelper.size) {
      runnablesHelper(i).run()
      i += 1
    }
    runnablesHelper.clear()

    _graphics._frameId += 1
    listener.render()

    _input match {
      case bi: DefaultBrowserInput => bi.reset()
      case _ => ()
    }
  }

  // --- Visibility change (pause/resume) ---

  private def addVisibilityChangeListener(): Unit =
    document.addEventListener("visibilitychange", (_: dom.Event) => onVisibilityChange())

  private def onVisibilityChange(): Unit = {
    val hidden = document.asInstanceOf[js.Dynamic].hidden.asInstanceOf[Boolean]
    if (hidden) {
      lifecycleListeners.foreach(_.pause())
      listener.pause()
    } else {
      lifecycleListeners.foreach(_.resume())
      listener.resume()
    }
  }

  // --- Resize handler ---

  private def addResizeListener(): Unit =
    window.addEventListener("resize", (_: dom.Event) => onResize())

  private def onResize(): Unit = {
    var width  = window.innerWidth.toInt - config.padHorizontal
    var height = window.innerHeight.toInt - config.padVertical

    // Ignore degenerate sizes
    if (width != 0 && height != 0) {
      if (config.usePhysicalPixels) {
        val density =
          window.asInstanceOf[js.Dynamic].devicePixelRatio.asInstanceOf[js.UndefOr[Double]].getOrElse(1.0)
        width = (width * density).toInt
        height = (height * density).toInt
      }
      _graphics.setCanvasSize(width, height)
    }
  }

  // --- Application trait implementation ---

  override def applicationListener: ApplicationListener = listener

  override def graphics: Graphics = _graphics
  override def audio:    Audio    = _audio
  override def input:    Input    = _input
  override def files:    Files    = _files
  override def net:      Net      = _net

  override def applicationType: Application.ApplicationType = Application.ApplicationType.WebGL

  override def version: Int = 0

  override def javaHeap: Long = {
    val perf = window.asInstanceOf[js.Dynamic].performance
    if (!js.isUndefined(perf) && !js.isUndefined(perf.memory)) {
      perf.memory.usedJSHeapSize.asInstanceOf[Double].toLong
    } else 0L
  }

  override def nativeHeap: Long = javaHeap

  override def getPreferences(name: String): Preferences =
    prefsCache.getOrElseUpdate(name, new BrowserPreferences(name))

  override def clipboard: Clipboard = _clipboard

  override def postRunnable(runnable: Runnable): Unit = runnables += runnable

  override def exit(): Unit = () // No-op on browser

  override def addLifecycleListener(listener: LifecycleListener): Unit =
    lifecycleListeners += listener

  override def removeLifecycleListener(listener: LifecycleListener): Unit =
    lifecycleListeners -= listener

  /** Returns the Sge context for this application. Can be used to provide `given Sge` in user code. */
  def sge: Sge = sgeContext
}
