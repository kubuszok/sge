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
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.platform.WindowingOps
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** Represents a desktop window managed by the windowing system (SDL3/GLFW).
  *
  * Manages a native window handle, GLFW callbacks (focus, iconify, maximize, close, drop, refresh), the associated graphics and input subsystems, and a per-window runnable queue.
  *
  * @param listener
  *   the application listener providing game logic callbacks
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
  val listener:                   ApplicationListener,
  private val lifecycleListeners: ArrayBuffer[LifecycleListener],
  private[sge] val config:        DesktopApplicationConfig,
  val application:                DesktopApplicationBase,
  private val windowing:          WindowingOps
) extends AutoCloseable {

  private var _windowHandle:        Long                            = 0L
  private var _listenerInitialized: Boolean                         = false
  private var _graphics:            DesktopGraphics                 = scala.compiletime.uninitialized
  private var _input:               DesktopInput                    = scala.compiletime.uninitialized
  private[sge] var windowListener:  Nullable[DesktopWindowListener] = config.windowListener

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
          listener.resume()
        }
        windowListener.foreach(_.focusGained())
      } else {
        windowListener.foreach(_.focusLost())
        if (config.pauseWhenLostFocus) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.pause())
          }
          listener.pause()
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
          listener.pause()
        }
      } else {
        if (config.pauseWhenMinimized) {
          lifecycleListeners.synchronized {
            lifecycleListeners.foreach(_.resume())
          }
          listener.resume()
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

  def getWindowHandle(): Long = _windowHandle

  private[sge] def getGraphics(): DesktopGraphics = _graphics

  private[sge] def getInput(): DesktopInput = _input

  private[sge] def isListenerInitialized(): Boolean = _listenerInitialized

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
  def getPositionX(): Int = windowing.getWindowPos(_windowHandle)._1

  /** @return the window y position in logical coordinates. */
  def getPositionY(): Int = windowing.getWindowPos(_windowHandle)._2

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
  private[sge] def update(): Boolean = {
    if (!_listenerInitialized) {
      initializeListener()
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
    var shouldRender = executedRunnables.nonEmpty || _graphics.isContinuousRendering()
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
      _graphics.getGL20().glViewport(0, 0, _graphics.getBackBufferWidth(), _graphics.getBackBufferHeight())
      listener.resize(_graphics.getWidth(), _graphics.getHeight())
      _graphics.update()
      listener.render()
      windowing.swapBuffers(_windowHandle)
      true
    } else if (shouldRender) {
      _graphics.update()
      listener.render()
      windowing.swapBuffers(_windowHandle)
      if (!iconified) _input.prepareNext()
      true
    } else {
      if (!iconified) _input.prepareNext()
      false
    }
  }

  private[sge] def initializeListener(): Unit =
    if (!_listenerInitialized) {
      listener.create()
      listener.resize(_graphics.getWidth(), _graphics.getHeight())
      _listenerInitialized = true
    }

  /** Makes this window's GL context current and updates the Sge context. */
  private[sge] def makeCurrent(): Unit =
    windowing.makeContextCurrent(_windowHandle)

  // ─── Lifecycle ────────────────────────────────────────────────────────

  override def close(): Unit = {
    listener.pause()
    listener.dispose()
    _graphics.close()
    _input.close()
    windowing.setWindowFocusCallback(_windowHandle, null)
    windowing.setWindowIconifyCallback(_windowHandle, null)
    windowing.setWindowCloseCallback(_windowHandle, null)
    windowing.setDropCallback(_windowHandle, null)
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
