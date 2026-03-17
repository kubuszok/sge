/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Application.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Application -> DesktopApplication
 *   Convention: Gdx.* static fields -> Sge context (using Sge) propagation
 *   Convention: GLFW calls abstracted through WindowingOps FFI trait
 *   Convention: ANGLE always-on (no GL20/GL30/GL31/GL32 native OpenGL paths)
 *   Convention: Library loading moved to platform-specific companion objects
 *   Convention: GLDebugMessageSeverity removed (ANGLE doesn't support ARB/KHR debug)
 *   Convention: loadANGLE/postLoadANGLE removed (SGE loads ANGLE libs at init time)
 *   Idiom: split packages; Nullable; no return; ArrayBuffer for window list
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.ClearMask
import sge.net.DesktopNet
import sge.noop.NoopDesktopAudio
import sge.platform.{ GlOps, WindowingOps }
import sge.utils.{ Clipboard, Nullable, SgeError }

import scala.collection.mutable.ArrayBuffer

/** Main desktop application class. Initializes the windowing system, creates the main window, and runs the game loop.
  *
  * The constructor blocks until the application exits. All windowing calls go through [[WindowingOps]], which is implemented via Panama (JVM) or @extern (Native).
  *
  * @param listener
  *   the application logic callbacks
  * @param config
  *   the application configuration (copied internally)
  * @param windowing
  *   the windowing FFI operations
  * @param audioOps
  *   the audio FFI operations
  * @param glOps
  *   EGL context management operations (platform-specific: Panama on JVM, @extern on Native)
  * @param glFactory
  *   factory for creating GL32 bindings (platform-specific: Panama on JVM, @extern on Native)
  * @author
  *   See AUTHORS file (original implementation)
  */
class DesktopApplication(
  listener:                    ApplicationListener,
  config:                      DesktopApplicationConfig,
  private val windowing:       WindowingOps,
  private val audioOps:        sge.platform.AudioOps,
  private val glOps:           GlOps,
  private val glFactory:       () => sge.graphics.GL32,
  private val recorderFactory: (Int, Boolean) => sge.audio.AudioRecorder = (_, _) => throw new UnsupportedOperationException("AudioRecorder not available on this platform")
) extends DesktopApplicationBase {

  // ─── State ──────────────────────────────────────────────────────────

  private val _config:                  DesktopApplicationConfig                          = DesktopApplicationConfig.copy(config)
  private[sge] val windows:             ArrayBuffer[DesktopWindow]                        = ArrayBuffer.empty
  @volatile private var _currentWindow: Nullable[DesktopWindow]                           = Nullable.empty
  private var _audio:                   DesktopAudio                                      = scala.compiletime.uninitialized
  private val _files:                   sge.files.DesktopFiles                            = sge.files.DesktopFiles()
  private val _net:                     DesktopNet                                        = DesktopNet(this)
  private val _preferences:             scala.collection.mutable.Map[String, Preferences] =
    scala.collection.mutable.Map.empty
  private val _clipboard: DesktopClipboard = DesktopClipboard(
    () => Nullable(windowing.getClipboardString(0L)),
    s => windowing.setClipboardString(0L, s)
  )

  @volatile private var _running: Boolean = true

  private val _runnables:          ArrayBuffer[Runnable]          = ArrayBuffer.empty
  private val _executedRunnables:  ArrayBuffer[Runnable]          = ArrayBuffer.empty
  private val _lifecycleListeners: ArrayBuffer[LifecycleListener] = ArrayBuffer.empty

  private val _sync: Sync = Sync(windowing)

  // ─── Sge context (created after first window + GL init) ─────────────
  private var _sge: Sge = scala.compiletime.uninitialized

  /** The [[Sge]] context for this application. Available after initialization is complete. */
  def sgeContext: Sge = _sge

  // ─── Initialization ─────────────────────────────────────────────────

  {
    // Wire platform FFI so DesktopCursor et al. can access windowing ops
    sge.platform.PlatformOps.windowing = windowing
    sge.platform.PlatformOps.audio = audioOps
    sge.platform.PlatformOps.gl = glOps

    if (!windowing.init()) {
      throw SgeError.GraphicsError("Unable to initialize windowing system")
    }

    if (_config.title.isEmpty) _config.title = listener.getClass.getSimpleName

    // Audio
    if (!_config.disableAudio) {
      try
        _audio = createAudio(_config)
      catch {
        case t: Throwable =>
          utils.Log.warn(s"Couldn't initialize audio, disabling audio: ${t.getMessage}")
          _audio = NoopDesktopAudio()
      }
    } else {
      _audio = NoopDesktopAudio()
    }

    // Create main window
    val mainWindow = createWindowInternal(_config, listener, 0L)
    windows += mainWindow

    // Build Sge context
    _sge = Sge(
      application = this,
      graphics = mainWindow.getGraphics(),
      audio = _audio,
      files = _files,
      input = mainWindow.getInput(),
      net = _net
    )

    try {
      given Sge = _sge
      loop()
      cleanupWindows()
    } catch {
      case e: RuntimeException => throw e
      case t: Throwable        => throw SgeError.GraphicsError("Application error", Some(t))
    } finally
      cleanup()
  }

  // ─── Main loop ──────────────────────────────────────────────────────

  protected def loop()(using Sge): Unit = {
    val closedWindows = ArrayBuffer.empty[DesktopWindow]
    while (_running && windows.nonEmpty) {
      _audio.update()

      var haveWindowsRendered = false
      closedWindows.clear()
      var targetFramerate = -2
      var i               = 0
      while (i < windows.size) {
        val win = windows(i)
        _currentWindow.foreach { cw =>
          if (cw ne win) {
            win.makeCurrent()
            _currentWindow = Nullable(win)
          }
        }
        if (_currentWindow.isEmpty) {
          win.makeCurrent()
          _currentWindow = Nullable(win)
        }
        if (targetFramerate == -2) targetFramerate = win.config.foregroundFPS
        _lifecycleListeners.synchronized {
          haveWindowsRendered |= win.update()
        }
        if (win.shouldClose()) {
          closedWindows += win
        }
        i += 1
      }
      windowing.pollEvents()

      // Process application-level runnables
      var shouldRequestRendering = false
      _runnables.synchronized {
        shouldRequestRendering = _runnables.nonEmpty
        _executedRunnables.clear()
        _executedRunnables.addAll(_runnables)
        _runnables.clear()
      }
      i = 0
      while (i < _executedRunnables.size) {
        _executedRunnables(i).run()
        i += 1
      }
      if (shouldRequestRendering) {
        // Must follow Runnables execution so changes done by Runnables are reflected
        // in the following render.
        i = 0
        while (i < windows.size) {
          if (!windows(i).getGraphics().isContinuousRendering()) {
            windows(i).requestRendering()
          }
          i += 1
        }
      }

      // Handle closed windows
      i = 0
      while (i < closedWindows.size) {
        val closedWindow = closedWindows(i)
        if (windows.size == 1) {
          // Lifecycle listener methods have to be called before ApplicationListener methods.
          var j = _lifecycleListeners.size - 1
          while (j >= 0) {
            val l = _lifecycleListeners(j)
            l.pause()
            l.dispose()
            j -= 1
          }
          _lifecycleListeners.clear()
        }
        closedWindow.close()
        windows -= closedWindow
        i += 1
      }

      if (!haveWindowsRendered) {
        // Sleep a few milliseconds in case no rendering was requested
        // with continuous rendering disabled.
        try Thread.sleep(1000 / _config.idleFPS)
        catch { case _: InterruptedException => () }
      } else if (targetFramerate > 0) {
        _sync.sync(targetFramerate)
      }
    }
  }

  protected def cleanupWindows(): Unit = {
    _lifecycleListeners.synchronized {
      _lifecycleListeners.foreach { ll =>
        ll.pause()
        ll.dispose()
      }
    }
    windows.foreach(_.close())
    windows.clear()
  }

  protected def cleanup(): Unit = {
    DesktopCursor.disposeSystemCursors()
    _audio.close()
    windowing.terminate()
  }

  // ─── Window creation ────────────────────────────────────────────────

  /** Creates a new window with a separate listener. The actual window creation is deferred to after the current frame.
    */
  def newWindow(listener: ApplicationListener, windowConfig: DesktopWindowConfig): DesktopWindow = {
    val appConfig = DesktopApplicationConfig.copy(_config)
    appConfig.setWindowConfiguration(windowConfig)
    if (appConfig.title.isEmpty) appConfig.title = listener.getClass.getSimpleName
    createWindowInternal(appConfig, listener, windows.head.getWindowHandle())
  }

  private def createWindowInternal(
    config:        DesktopApplicationConfig,
    listener:      ApplicationListener,
    sharedContext: Long
  ): DesktopWindow = {
    val win = DesktopWindow(listener, _lifecycleListeners, config, this, windowing, glOps)
    if (sharedContext == 0L) {
      // the main window is created immediately
      setupWindow(win, config, sharedContext)
    } else {
      // creation of additional windows is deferred to avoid GL context trouble
      postRunnable { () =>
        setupWindow(win, config, sharedContext)
        windows += win
      }
    }
    win
  }

  private[sge] def setupWindow(
    window:        DesktopWindow,
    config:        DesktopApplicationConfig,
    sharedContext: Long
  ): Unit = {
    val windowHandle = createGlfwWindow(config, sharedContext)
    window.create(windowHandle)

    // Create EGL context for this window (ANGLE manages the GL context, not GLFW)
    // EGL needs the platform-native window handle (NSWindow/X11 Window/HWND), not the GLFW handle
    val nativeHandle = windowing.getNativeWindowHandle(windowHandle)
    val eglCtx       = glOps.createContext(nativeHandle, config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples)
    if (eglCtx == 0L) {
      throw SgeError.GraphicsError("Failed to create EGL context")
    }
    window.setEglContext(eglCtx)

    // Set swap interval via EGL (not GLFW, since we use GLFW_NO_API)
    glOps.setSwapInterval(if (config.vSyncEnabled) 1 else 0)

    // Initialize GL bindings (ANGLE on JVM, @extern on Native)
    val gl32 = glFactory()
    window.getGraphics().initGL(gl32, Nullable(gl32), Nullable(gl32), Nullable(gl32))

    // Apply window icon from config if set
    config.windowIconPaths.foreach { paths =>
      config.windowIconFileType.foreach { fileType =>
        val pixmaps = paths.map { path =>
          new sge.graphics.Pixmap(_files.getFileHandle(path, fileType))
        }
        window.setIcon(pixmaps*)
        pixmaps.foreach(_.close())
      }
    }

    window.setVisible(config.initialVisible)

    // Clear to initial background color (double-buffered)
    var i = 0
    while (i < 2) {
      val c = config.initialBackgroundColor
      window.getGraphics().getGL20().glClearColor(c.r, c.g, c.b, c.a)
      window.getGraphics().getGL20().glClear(ClearMask.ColorBufferBit)
      glOps.swapEglBuffers(eglCtx)
      i += 1
    }

    _currentWindow.foreach { cw =>
      // the call above switches the OpenGL context to the newly created window,
      // ensure that the invariant "currentWindow is the window with the current active OpenGL context" holds
      cw.makeCurrent()
    }
  }

  private def createGlfwWindow(config: DesktopApplicationConfig, sharedContextWindow: Long): Long = {
    windowing.defaultWindowHints()
    // ANGLE manages the GL context via EGL, so tell GLFW not to create one
    windowing.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API)
    windowing.setWindowHint(WindowingOps.GLFW_VISIBLE, WindowingOps.GLFW_FALSE)
    windowing.setWindowHint(WindowingOps.GLFW_RESIZABLE, if (config.windowResizable) WindowingOps.GLFW_TRUE else WindowingOps.GLFW_FALSE)
    windowing.setWindowHint(WindowingOps.GLFW_DECORATED, if (config.windowDecorated) WindowingOps.GLFW_TRUE else WindowingOps.GLFW_FALSE)
    windowing.setWindowHint(WindowingOps.GLFW_MAXIMIZED, if (config.windowMaximized) WindowingOps.GLFW_TRUE else WindowingOps.GLFW_FALSE)
    windowing.setWindowHint(WindowingOps.GLFW_AUTO_ICONIFY, if (config.autoIconify) WindowingOps.GLFW_TRUE else WindowingOps.GLFW_FALSE)
    val windowHandle = windowing.createWindow(config.windowWidth, config.windowHeight, config.title)

    if (windowHandle == 0L) {
      throw SgeError.GraphicsError("Couldn't create window")
    }

    windowing.setWindowSizeLimits(
      windowHandle,
      config.windowMinWidth,
      config.windowMinHeight,
      config.windowMaxWidth,
      config.windowMaxHeight
    )

    // Center window if position is -1, -1
    if (config.fullscreenMode.isEmpty) {
      if (config.windowX == -1 && config.windowY == -1) {
        val windowWidth = {
          var w = scala.math.max(config.windowWidth, config.windowMinWidth)
          if (config.windowMaxWidth > -1) w = scala.math.min(w, config.windowMaxWidth)
          w
        }
        val windowHeight = {
          var h = scala.math.max(config.windowHeight, config.windowMinHeight)
          if (config.windowMaxHeight > -1) h = scala.math.min(h, config.windowMaxHeight)
          h
        }
        val monitorHandle =
          if (config.windowMaximized) {
            config.maximizedMonitor.fold(windowing.getPrimaryMonitor())(_.monitorHandle)
          } else {
            windowing.getPrimaryMonitor()
          }
        // getPrimaryMonitor() can return 0 (NULL) if no monitors are detected or
        // GLFW hasn't fully initialized — guard to avoid native assertion crash.
        val (mx, my)             = if (monitorHandle != 0L) windowing.getMonitorPos(monitorHandle) else (0, 0)
        val (mw, mh, _, _, _, _) =
          if (monitorHandle != 0L) windowing.getVideoMode(monitorHandle) else (windowWidth, windowHeight, 0, 0, 0, 0)
        val newX = mx + (mw - windowWidth) / 2
        val newY = my + (mh - windowHeight) / 2
        windowing.setWindowPos(windowHandle, newX, newY)
      } else {
        windowing.setWindowPos(windowHandle, config.windowX, config.windowY)
      }

      if (config.windowMaximized) {
        windowing.maximizeWindow(windowHandle)
      }
    }

    // Note: no makeContextCurrent or setSwapInterval here — with GLFW_NO_API,
    // EGL manages the GL context. See setupWindow() for EGL context creation.

    windowHandle
  }

  // ─── Application trait ──────────────────────────────────────────────

  override def getApplicationListener(): ApplicationListener =
    _currentWindow.fold(listener)(_.listener)

  @scala.annotation.nowarn("msg=deprecated") // orNull — returns null when no window is current (Application trait requires non-Nullable return)
  override def getGraphics(): Graphics =
    _currentWindow.map(_.getGraphics()).orNull

  override def getAudio(): Audio = _audio

  @scala.annotation.nowarn("msg=deprecated") // orNull — returns null when no window is current (Application trait requires non-Nullable return)
  override def getInput(): Input =
    _currentWindow.map(_.getInput()).orNull

  override def getFiles(): Files = _files

  override def getNet(): Net = _net

  override def getType(): Application.ApplicationType = Application.ApplicationType.Desktop

  override def getVersion(): Int = 0

  override def getJavaHeap(): Long =
    Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

  override def getNativeHeap(): Long = getJavaHeap()

  override def getPreferences(name: String): Preferences =
    _preferences.getOrElseUpdate(
      name, {
        val file = java.io.File(_config.preferencesDirectory, name)
        sge.files.DesktopPreferences(
          sge.files.DesktopFileHandle(file, _config.preferencesFileType, sge.files.DesktopFiles.externalPath)
        )
      }
    )

  override def getClipboard(): Clipboard = _clipboard

  override def postRunnable(runnable: Runnable): Unit = _runnables.synchronized {
    _runnables += runnable
  }

  override def exit(): Unit = _running = false

  override def addLifecycleListener(listener: LifecycleListener): Unit =
    _lifecycleListeners.synchronized {
      _lifecycleListeners += listener
    }

  override def removeLifecycleListener(listener: LifecycleListener): Unit =
    _lifecycleListeners.synchronized {
      _lifecycleListeners -= listener
    }

  // ─── DesktopApplicationBase ─────────────────────────────────────────

  override def createAudio(config: DesktopApplicationConfig): DesktopAudio =
    new sge.audio.MiniaudioEngine(
      config.audioDeviceSimultaneousSources,
      config.audioDeviceBufferSize,
      config.audioDeviceBufferCount,
      audioOps,
      recorderFactory
    )

  override def createInput(window: DesktopWindow): DesktopInput =
    DefaultDesktopInput(window, windowing)
}
