/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3WindowListener.java
 *                  backends/gdx-backend-lwjgl3/.../Lwjgl3WindowAdapter.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3WindowListener -> DesktopWindowListener
 *   Merged with: Lwjgl3WindowAdapter (default no-op methods in trait)
 *   Convention: trait with default implementations replaces interface + adapter pair
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Receives notifications of various window events, such as iconification, focus loss and gain, and window close events. Can be set per window via [[DesktopApplicationConfig]] and
  * [[DesktopWindowConfig]]. Close events can be canceled by returning false.
  *
  * All methods have default no-op implementations (merging the original Lwjgl3WindowAdapter).
  *
  * @author
  *   badlogic (original implementation)
  */
trait DesktopWindowListener {

  /** Called after the window is created. Before this callback is received, it's unsafe to use any [[DesktopWindow]] member functions which, for their part, involve calling windowing system functions.
    *
    * @param window
    *   the window instance
    */
  def created(window: DesktopWindow): Unit = ()

  /** Called when the window is iconified (i.e. its minimize button was clicked), or when restored from the iconified state. When a window becomes iconified, its [[ApplicationListener]] will be
    * paused, and when restored it will be resumed.
    *
    * @param isIconified
    *   true if window is iconified, false if it leaves the iconified state
    */
  def iconified(isIconified: Boolean): Unit = ()

  /** Called when the window is maximized, or restored from the maximized state.
    *
    * @param isMaximized
    *   true if window is maximized, false if it leaves the maximized state
    */
  def maximized(isMaximized: Boolean): Unit = ()

  /** Called when the window lost focus to another window. The window's [[ApplicationListener]] will continue to be called.
    */
  def focusLost(): Unit = ()

  /** Called when the window gained focus. */
  def focusGained(): Unit = ()

  /** Called when the user requested to close the window, e.g. clicking the close button or pressing the window closing keyboard shortcut.
    *
    * @return
    *   whether the window should actually close
    */
  def closeRequested(): Boolean = true

  /** Called when external files are dropped into the window, e.g from the Desktop.
    *
    * @param files
    *   array with absolute paths to the files
    */
  def filesDropped(files: Array[String]): Unit = ()

  /** Called when the window content is damaged and needs to be refreshed. When this occurs, rendering is automatically requested.
    */
  def refreshRequested(): Unit = ()
}
