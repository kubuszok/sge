/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Window.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Window -> DesktopWindow
 *   Convention: GLFW calls abstracted through WindowingOps FFI trait
 *   Convention: GLFW callbacks -> Scala lambdas via WindowingOps
 *   Convention: synchronized -> synchronized (same semantics on JVM)
 *   Idiom: split packages; Nullable for optional fields; no return
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.platform.{ GlOps, WindowingOps }
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** Represents a desktop window managed by the windowing system (SDL3/GLFW).
  *
  * Manages a native window handle, GLFW callbacks (focus, iconify, maximize, close, drop, refresh), the associated graphics and input subsystems, and a per-window runnable queue.
  *
  * @param listenerFactory
  *   a context function that creates the application listener when given an [[Sge]] context
  * @param lifecycleListeners
  *   shared lifecycle listeners from the application
  * @param config
  *   the application configuration (copied per-window)
  * @param application
  *   the desktop application that owns this window
  * @param windowing
  *   the windowing FFI operations
  * @author
  *   badlogic (original implementation)
  */
class DesktopWindow private[sge] (
  private val listenerFactory:    Sge ?=> ApplicationListener,
  private val lifecycleListeners: ArrayBuffer[LifecycleListener],
  private[sge] val config:        DesktopApplicationConfig,
  val application:                DesktopApplicationBase,
  private val windowing:          WindowingOps,
  private val glOps:              GlOps
) extends AutoCloseable {

  private var _listener:            ApplicationListener             = scala.compiletime.uninitialized
  private var _windowHandle:        Long                            = 0L
  private var _eglContext:          Long                            = 0L
  private var _listenerInitialized: Boolean                         = false
  private var _sge:                 Sge                             = scala.compiletime.uninitialized
  private var _graphics:            DesktopGraphics                 = scala.compiletime.uninitialized
  private var _input:               DesktopInput                    = scala.compiletime.uninitialized
  private[sge] var windowListener:  Nullable[DesktopWindowListener] = config.windowListener

  /** The application listener for this window. Available after listener initialization. */
  def listener: ApplicationListener = _listener

  var iconified:                 Boolean = false
  var focused:                   Boolean = false
  @volatile var asyncResized:    Boolean = false
  private var _requestRendering: Boolean = false

  private val runnables:         ArrayBuffer[Runnable] = ArrayBuffer.empty
  private val executedRunnables: ArrayBuffer[Runnable] = ArrayBuffer.empty

  // ─── Window creation ────────────────────────────────────────────────────

  private[sge] def create(windowHandle: Long): Unit = {
    _windowHandle = windowHandle
    _input = application.createInput(this)
    _graphics = new DesktopGraphics(this, windowing)

    windowing.setWindowFocusCallback(windowHandle, onFocus)
    windowing.setWindowIconifyCallback(windowHandle, onIconify)
    windowing.setWindowMaximizeCallback(windowHandle, onMaximize)
    windowing.setWindowCloseCallback(windowHandle, onClose)
    windowing.setDropCallback(windowHandle, onDrop)
    windowing.setWindowRefreshCallback(windowHandle, onRefresh)

    windowListener.foreach(_.created(this))
  }

  // ─── Callbacks ──────────────────────────────────────────────────────────

  private val onFocus: (Long, Boolean) => Unit = { (_, isFocused) =>
    postRunnable { () =>
      if (isFocused) {
        if (config.pauseWhenLostFocus) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.resume())
          }
          _listener.resume()
        }
        windowListener.foreach(_.focusGained())
      } else {
        windowListener.foreach(_.focusLost())
        if (config.pauseWhenLostFocus) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.pause())
          }
          _listener.pause()
        }
      }
      this.focused = isFocused
    }
  }

  private val onIconify: (Long, Boolean) => Unit = { (_, isIconified) =>
    postRunnable { () =>
      windowListener.foreach(_.iconified(isIconified))
      this.iconified = isIconified
      if (isIconified) {
        if (config.pauseWhenMinimized) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.pause())
          }
          _listener.pause()
        }
      } else {
        if (config.pauseWhenMinimized) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.resume())
          }
          _listener.resume()
        }
      }
    }
  }

  private val onMaximize: (Long, Boolean) => Unit = { (_, maximized) =>
    postRunnable { () =>
      windowListener.foreach(_.maximized(maximized))
    }
  }

  private val onClose: Long => Unit = { handle =>
    postRunnable { () =>
      windowListener.foreach { wl =>
        if (!wl.closeRequested()) {
          windowing.setWindowShouldClose(handle, false)
        }
      }
    }
  }

  private val onDrop: (Long, Array[String]) => Unit = { (_, files) =>
    postRunnable { () =>
      windowListener.foreach(_.filesDropped(files))
    }
  }

  private val onRefresh: Long => Unit = { _ =>
    postRunnable { () =>
      windowListener.foreach(_.refreshRequested())
    }
  }

  // ─── Accessors ──────────────────────────────────────────────────────────

  def windowHandle: Long = _windowHandle

  private[sge] def eglContext: Long = _eglContext

  private[sge] def setEglContext(ctx: Long): Unit = _eglContext = ctx

  private[sge] def graphics: DesktopGraphics = _graphics

  private[sge] def input: DesktopInput = _input

  private[sge] def isListenerInitialized(): Boolean = _listenerInitialized

  /** The Sge context for this window. Available after listener initialization. */
  private[sge] def sgeContext: Sge = _sge

  // ─── Runnable queue ─────────────────────────────────────────────────────

  /** Post a [[Runnable]] to this window's event queue. */
  def postRunnable(runnable: Runnable): Unit = runnables.synchronized {
    runnables += runnable
  }

  // ─── Window properties ──────────────────────────────────────────────────

  /** Sets the position of the window in logical coordinates. */
  def setPosition(x: Int, y: Int): Unit =
    windowing.setWindowPos(_windowHandle, x, y)

  /** @return the window x position in logical coordinates. */
  def positionX: Int = windowing.getWindowPos(_windowHandle)._1

  /** @return the window y position in logical coordinates. */
  def positionY: Int = windowing.getWindowPos(_windowHandle)._2

  /** Sets the visibility of the window. */
  def setVisible(visible: Boolean): Unit =
    if (visible) windowing.showWindow(_windowHandle)
    else windowing.hideWindow(_windowHandle)

  /** Closes this window by setting the should-close flag. */
  def closeWindow(): Unit =
    windowing.setWindowShouldClose(_windowHandle, true)

  /** Minimizes (iconifies) the window. */
  def iconifyWindow(): Unit =
    windowing.iconifyWindow(_windowHandle)

  /** Whether the window is iconified. */
  def isIconified(): Boolean = iconified

  /** De-minimizes and de-maximizes the window. */
  def restoreWindow(): Unit =
    windowing.restoreWindow(_windowHandle)

  /** Maximizes the window. */
  def maximizeWindow(): Unit =
    windowing.maximizeWindow(_windowHandle)

  /** Brings the window to front and sets input focus. */
  def focusWindow(): Unit =
    windowing.focusWindow(_windowHandle)

  /** Whether the window has input focus. */
  def isFocused(): Boolean = focused

  /** Sets the window title. */
  def setTitle(title: CharSequence): Unit =
    windowing.setWindowTitle(_windowHandle, title.toString)

  /** Sets minimum and maximum size limits for the window. Use -1 for unrestricted. */
  def setSizeLimits(minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int): Unit =
    windowing.setWindowSizeLimits(_windowHandle, minWidth, minHeight, maxWidth, maxHeight)

  /** Sets the window icon from one or more Pixmap images. Each Pixmap should be RGBA8888 format; non-RGBA8888 Pixmaps are converted automatically. Has no effect on macOS or Wayland.
    *
    * @param images
    *   one or more Pixmaps at different sizes (e.g. 16x16, 32x32, 48x48). Pass no arguments to reset to default.
    */
  def setIcon(images: sge.graphics.Pixmap*): Unit = {
    val platform = windowing.platform
    if (platform != WindowingOps.GLFW_PLATFORM_COCOA && platform != WindowingOps.GLFW_PLATFORM_WAYLAND) {
      if (images.isEmpty) {
        windowing.setWindowIcon(_windowHandle, Array.empty)
      } else {
        // Convert non-RGBA8888 pixmaps
        val pixmaps    = new Array[sge.graphics.Pixmap](images.length)
        val tmpPixmaps = new Array[sge.graphics.Pixmap](images.length)
        var i          = 0
        while (i < images.length) {
          val pixmap = images(i)
          if (pixmap.format != sge.graphics.Pixmap.Format.RGBA8888) {
            val rgba = new sge.graphics.Pixmap(pixmap.width.toInt, pixmap.height.toInt, sge.graphics.Pixmap.Format.RGBA8888)
            rgba.setBlending(sge.graphics.Pixmap.Blending.None)
            rgba.drawPixmap(pixmap, Pixels.zero, Pixels.zero)
            tmpPixmaps(i) = rgba
            pixmaps(i) = rgba
          } else {
            pixmaps(i) = pixmap
          }
          i += 1
        }
        windowing.setWindowIcon(_windowHandle, pixmaps)
        // Dispose temporary format-converted pixmaps
        i = 0
        while (i < tmpPixmaps.length) {
          if (tmpPixmaps(i) != null) tmpPixmaps(i).close()
          i += 1
        }
      }
    }
  }

  /** Requests user attention (e.g. taskbar flash). */
  def flash(): Unit =
    windowing.requestWindowAttention(_windowHandle)

  /** Requests a rendering frame (for non-continuous rendering). */
  private[sge] def requestRendering(): Unit = this.synchronized {
    _requestRendering = true
  }

  private[sge] def shouldClose(): Boolean =
    windowing.windowShouldClose(_windowHandle)

  // ─── Window handle change ─────────────────────────────────────────────

  private[sge] def windowHandleChanged(windowHandle: Long): Unit = {
    _windowHandle = windowHandle
    _input.windowHandleChanged(windowHandle)
  }

  // ─── Frame update ─────────────────────────────────────────────────────

  /** Called each frame by the application loop. Returns true if the window rendered this frame. */
  private[sge] def update()(using sge: Sge): Boolean = {
    if (!_listenerInitialized) {
      initializeListener(sge)
    }

    runnables.synchronized {
      executedRunnables.addAll(runnables)
      runnables.clear()
    }

    var i = 0
    while (i < executedRunnables.size) {
      executedRunnables(i).run()
      i += 1
    }
    var shouldRender = executedRunnables.nonEmpty || _graphics.continuousRendering
    executedRunnables.clear()

    if (!iconified) _input.update()

    this.synchronized {
      shouldRender |= _requestRendering && !iconified
      _requestRendering = false
    }

    // Handle async resize (glfw_async mode)
    if (asyncResized) {
      asyncResized = false
      _graphics.updateFramebufferInfo()
      _graphics.gl20.glViewport(Pixels.zero, Pixels.zero, _graphics.backBufferWidth, _graphics.backBufferHeight)
      _listener.resize(_graphics.width, _graphics.height)
      _graphics.update()
      _listener.render()
      glOps.swapEglBuffers(_eglContext)
      true
    } else if (shouldRender) {
      _graphics.update()
      _listener.render()
      glOps.swapEglBuffers(_eglContext)
      if (!iconified) _input.prepareNext()
      true
    } else {
      if (!iconified) _input.prepareNext()
      false
    }
  }

  private[sge] def initializeListener(sge: Sge): Unit =
    if (!_listenerInitialized) {
      _sge = sge
      given Sge = sge
      _listener = listenerFactory
      _listener.create()
      _listener.resize(_graphics.width, _graphics.height)
      _listenerInitialized = true
    }

  /** Makes this window's EGL context current. ANGLE manages the GL context, not GLFW. */
  private[sge] def makeCurrent(): Unit =
    glOps.makeCurrent(_eglContext)

  // ─── Lifecycle ────────────────────────────────────────────────────────

  override def close(): Unit = {
    _listener.pause()
    _listener.dispose()
    _graphics.close()
    _input.close()
    windowing.setWindowFocusCallback(_windowHandle, null)
    windowing.setWindowIconifyCallback(_windowHandle, null)
    windowing.setWindowCloseCallback(_windowHandle, null)
    windowing.setDropCallback(_windowHandle, null)
    if (_eglContext != 0L) {
      glOps.destroyContext(_eglContext)
      _eglContext = 0L
    }
    windowing.destroyWindow(_windowHandle)
  }

  // ─── Identity ─────────────────────────────────────────────────────────

  override def hashCode(): Int = {
    val prime = 31
    prime + (_windowHandle ^ (_windowHandle >>> 32)).toInt
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: DesktopWindow => _windowHandle == other._windowHandle
    case _ => false
  }
}
