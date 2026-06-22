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
import lowlevel.Nullable
import sge.utils.{ Clipboard, SgeError }

import scala.collection.mutable.ArrayBuffer

/** Main desktop application class. Initializes the windowing system, creates the main window, and runs the game loop.
  *
  * The constructor blocks until the application exits. All windowing calls go through [[WindowingOps]], which is implemented via Panama (JVM) or @extern (Native).
  *
  * @param listenerFactory
  *   a context function that creates the application listener when given an [[Sge]] context
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
  listenerFactory:             Sge ?=> ApplicationListener,
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

    // Create main window (listener is materialized later in initializeListener when Sge is ready)
    val mainWindow = createWindowInternal(_config, listenerFactory, 0L)
    windows += mainWindow

    // Build Sge context
    _sge = Sge(
      application = this,
      graphics = mainWindow.graphics,
      audio = _audio,
      files = _files,
      input = mainWindow.input,
      net = _net
    )

    try {
      given Sge = _sge
      // Load extension dependencies once, before the game listener's create() runs.
      // create() is invoked lazily on the first frame inside loop() (via
      // DesktopWindow.initializeListener), so blocking here guarantees extensions
      // are loaded first. On JVM/Native loads are synchronous (already-completed
      // Futures); parasitic runs the sequential fold's continuations inline.
      scala.concurrent.Await.result(
        SgeExtension.loadAll(_config.extensions)(using _sge, scala.concurrent.ExecutionContext.parasitic),
        scala.concurrent.duration.Duration(60, java.util.concurrent.TimeUnit.SECONDS)
      )
      loop()
      cleanupWindows()
    } catch {
      case e: RuntimeException => throw e
      case t: Throwable        => throw SgeError.GraphicsError("Application error", Some(t))
    } finally cleanup()
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

      // Drive per-frame hooks (SGE-original) once per frame, right after input
      // pollEvents so polling-based subsystems (e.g. the controllers extension)
      // refresh their state before the next window render consumes it.
      runFrameHooks()

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
          if (!windows(i).graphics.continuousRendering) {
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
  def newWindow(listenerFactory: Sge ?=> ApplicationListener, windowConfig: DesktopWindowConfig): DesktopWindow = {
    val appConfig = DesktopApplicationConfig.copy(_config)
    appConfig.setWindowConfiguration(windowConfig)
    createWindowInternal(appConfig, listenerFactory, windows.head.windowHandle)
  }

  private def createWindowInternal(
    config:          DesktopApplicationConfig,
    listenerFactory: Sge ?=> ApplicationListener,
    sharedContext:   Long
  ): DesktopWindow = {
    val win = DesktopWindow(listenerFactory, _lifecycleListeners, config, this, windowing, glOps)
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
    // `sharedContext` is the GLFW window handle of the window this one should
    // share GL resources with (0 for the main window). Map it to that window's
    // stored EGL context handle and thread it into createContext so the two
    // contexts share a GL resource namespace (ISS-538 clause 2); if it can't be
    // resolved, fall back to 0 (share with the display's primary context).
    val sharedEglCtx =
      if (sharedContext == 0L) 0L
      else windows.find(_.windowHandle == sharedContext).fold(0L)(_.eglContext)
    val eglCtx = glOps.createContext(nativeHandle, config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples, sharedEglCtx)
    if (eglCtx == 0L) {
      throw SgeError.GraphicsError("Failed to create EGL context")
    }
    window.setEglContext(eglCtx)

    // Set swap interval via EGL (not GLFW, since we use GLFW_NO_API)
    glOps.setSwapInterval(if (config.vSyncEnabled) 1 else 0)

    // Initialize GL bindings (ANGLE on JVM, @extern on Native)
    // The AngleGL32 instance implements all ES levels, but it must only be
    // *advertised* (via gl30/gl31/gl32 availability) at the level the live EGL
    // context actually supports — otherwise gl31Available/gl32Available are
    // unconditionally true and callers invoke ES 3.1/3.2 entry points
    // (glDispatchCompute, etc.) on an ES 3.0 context → GL errors (ISS-539).
    // The context is already current here, so query its real major.minor.
    val gl32           = glFactory()
    val (major, minor) = queryGlVersion(gl32)
    // gl30 is present whenever the context is >= 3.0 (it is — desktop EGL
    // requests ES 3.0). gl31/gl32 are gated on the queried version.
    val gl30Slot = if (major > 3 || (major == 3 && minor >= 0)) Nullable(gl32) else Nullable.empty[sge.graphics.GL30]
    val gl31Slot = if (major > 3 || (major == 3 && minor >= 1)) Nullable(gl32) else Nullable.empty[sge.graphics.GL31]
    val gl32Slot = if (major > 3 || (major == 3 && minor >= 2)) Nullable(gl32) else Nullable.empty[sge.graphics.GL32]
    window.graphics.initGL(gl32, gl30Slot, gl31Slot, gl32Slot)

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
      window.graphics.gl20.glClearColor(c.r, c.g, c.b, c.a)
      window.graphics.gl20.glClear(ClearMask.ColorBufferBit)
      glOps.swapEglBuffers(eglCtx)
      i += 1
    }

    _currentWindow.foreach { cw =>
      // the call above switches the OpenGL context to the newly created window,
      // ensure that the invariant "currentWindow is the window with the current active OpenGL context" holds
      cw.makeCurrent()
    }
  }

  /** Query the live GL context's major.minor version (the context must already be current). Primary path is glGetIntegerv(GL_MAJOR_VERSION / GL_MINOR_VERSION) — integer queries available on any ES
    * 3.0 core context. If the major comes back as 0 (some drivers gate these queries), fall back to parsing glGetString(GL_VERSION), whose ES form is "OpenGL ES <major>.<minor> <vendor>". Used to
    * gate gl31/gl32 availability so we advertise only what the context actually supports (ISS-539).
    */
  private def queryGlVersion(gl: sge.graphics.GL30): (Int, Int) = {
    val buf = sge.utils.BufferUtils.newIntBuffer(1)
    gl.glGetIntegerv(sge.graphics.GL30.GL_MAJOR_VERSION, buf)
    val major = buf.get(0)
    gl.glGetIntegerv(sge.graphics.GL30.GL_MINOR_VERSION, buf)
    val minor = buf.get(0)
    if (major > 0) {
      (major, minor)
    } else {
      // Fallback: parse the version string, e.g. "OpenGL ES 3.0 (ANGLE ...)".
      val versionString = Nullable(gl.glGetString(sge.graphics.GL20.GL_VERSION))
      versionString.fold((0, 0)) { s =>
        """(\d+)\.(\d+)""".r.findFirstMatchIn(s).fold((0, 0)) { m =>
          (m.group(1).toInt, m.group(2).toInt)
        }
      }
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
            config.maximizedMonitor.fold(windowing.primaryMonitor)(_.monitorHandle)
          } else {
            windowing.primaryMonitor
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

  override def applicationListener: ApplicationListener =
    DesktopApplication.listenerOrFail(_currentWindow, windows)

  override def graphics: Graphics =
    DesktopApplication.currentOrFail(_currentWindow)(_.graphics, "graphics")

  override def audio: Audio = _audio

  override def input: Input =
    DesktopApplication.currentOrFail(_currentWindow)(_.input, "input")

  override def files: Files = _files

  override def net: Net = _net

  override def applicationType: Application.ApplicationType = Application.ApplicationType.Desktop

  override def version: Int = 0

  override def javaHeap: Long =
    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

  override def nativeHeap: Long = javaHeap

  override def getPreferences(name: String): Preferences =
    _preferences.getOrElseUpdate(
      name, {
        val file = java.io.File(_config.preferencesDirectory, name)
        sge.files.DesktopPreferences(
          sge.files.DesktopFileHandle(file, _config.preferencesFileType, sge.files.DesktopFiles.externalPath)
        )
      }
    )

  override def clipboard: Clipboard = _clipboard

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

object DesktopApplication {

  /** Resolves a window-bound accessor (graphics, input) from the currently-current window, failing fast with a clearly-messaged [[SgeError.GraphicsError]] when no window is current.
    *
    * The [[Application]] trait requires non-`Nullable` returns for `graphics`/`input`, but those resources only exist while a window is current. Rather than returning a bare `null` (which produces an
    * NPE far from cause in an otherwise null-free API), this surfaces an explicit error naming the missing-current-window condition and the affected accessor. The happy path (window present) is
    * behaviorally identical to a direct field read.
    *
    * @param window
    *   the currently-current window, if any
    * @param select
    *   extracts the desired accessor from the window
    * @param what
    *   the accessor name, used in the failure message
    */
  private[sge] def currentOrFail[A](window: Nullable[DesktopWindow])(select: DesktopWindow => A, what: String): A =
    window.map(select).getOrElse(throw SgeError.GraphicsError(s"No current window; $what is unavailable until a window becomes current"))

  /** Resolves the [[ApplicationListener]] from the currently-current window, or — when no window is current — from the first existing window, failing fast with a clearly-messaged
    * [[SgeError.GraphicsError]] when there is no window at all to source the listener from.
    *
    * The [[Application]] trait requires a non-`Nullable` `applicationListener`, but the listener only exists once a window has been created. The previous direct `windows.head` read threw a
    * `NoSuchElementException` (an exception far from cause) when `windows` was empty in an otherwise null-free API; this surfaces an explicit error naming the missing-window condition instead. The
    * happy paths are behaviorally identical: a current window yields its listener; no current window but a non-empty window list yields the first window's listener.
    *
    * @param current
    *   the currently-current window, if any
    * @param windows
    *   the existing windows (used as a fallback source when no window is current)
    */
  private[sge] def listenerOrFail(current: Nullable[DesktopWindow], windows: collection.Seq[DesktopWindow]): ApplicationListener =
    current.fold {
      if (windows.isEmpty)
        throw SgeError.GraphicsError("No window available; applicationListener is unavailable until a window is created")
      else windows.head.listener
    }(_.listener)
}
