/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3WindowConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3WindowConfiguration -> DesktopWindowConfig
 *   Convention: Java-style setters -> public vars; batch setters kept as convenience methods
 *   Idiom: Nullable for optional fields; split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.files.FileType
import sge.graphics.Color
import sge.utils.Nullable

/** Per-window configuration for desktop applications. Can be used when creating additional windows via the desktop application.
  *
  * All fields are public vars with sensible defaults. Convenience methods are provided for common multi-field operations.
  */
class DesktopWindowConfig {

  /** Window position (-1 means centered on primary monitor). */
  var windowX: Int = -1
  var windowY: Int = -1

  /** Window size in logical pixels. */
  var windowWidth:  Int = 640
  var windowHeight: Int = 480

  /** Minimum/maximum window size constraints. -1 means unrestricted. */
  var windowMinWidth:  Int = -1
  var windowMinHeight: Int = -1
  var windowMaxWidth:  Int = -1
  var windowMaxHeight: Int = -1

  /** Whether the windowed mode window is resizable. */
  var windowResizable: Boolean = true

  /** Whether the windowed mode window is decorated (title bar, borders). */
  var windowDecorated: Boolean = true

  /** Whether the window starts maximized. Ignored if fullscreen. */
  var windowMaximized: Boolean = false

  /** Monitor to maximize on, if maximized. */
  var maximizedMonitor: Nullable[DesktopMonitor] = Nullable.empty

  /** Whether the window should auto-iconify and restore previous video mode on input focus loss. Does nothing in windowed mode.
    */
  var autoIconify: Boolean = true

  /** Icon file type and paths for the window title bar. Has no effect on macOS. */
  var windowIconFileType: Nullable[FileType]      = Nullable.empty
  var windowIconPaths:    Nullable[Array[String]] = Nullable.empty

  /** The window event listener. */
  var windowListener: Nullable[DesktopWindowListener] = Nullable.empty

  /** Fullscreen display mode. When set, the window runs in fullscreen on the mode's monitor. */
  var fullscreenMode: Nullable[DesktopDisplayMode] = Nullable.empty

  /** Window title. If empty, the application listener's class name is used. */
  var title: String = ""

  /** Initial background color before the first frame renders. */
  var initialBackgroundColor: Color = Color.BLACK

  /** Whether the window is visible on creation. */
  var initialVisible: Boolean = true

  /** Whether to use vsync. For multi-window applications, only one (the main) window should enable vsync. */
  var vSyncEnabled: Boolean = true

  // ---- convenience methods ----

  /** Sets the window size in logical pixels. */
  def setWindowedMode(width: Int, height: Int): Unit = {
    windowWidth = width
    windowHeight = height
  }

  /** Sets the window position. Use -1 for both to center on primary monitor. */
  def setWindowPosition(x: Int, y: Int): Unit = {
    windowX = x
    windowY = y
  }

  /** Sets minimum and maximum size limits for the window. -1 means unrestricted. */
  def setWindowSizeLimits(minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int): Unit = {
    windowMinWidth = minWidth
    windowMinHeight = minHeight
    windowMaxWidth = maxWidth
    windowMaxHeight = maxHeight
  }

  /** Sets the icon that will be used in the window's title bar. Has no effect on macOS.
    * @param filePaths
    *   one or more internal image paths (JPEG, PNG, or BMP). Good sizes: 16x16, 32x32, 48x48.
    */
  def setWindowIcon(filePaths: String*): Unit =
    setWindowIcon(FileType.Internal, filePaths*)

  /** Sets the icon that will be used in the window's title bar. Has no effect on macOS.
    * @param fileType
    *   the file type for resolving the paths
    * @param filePaths
    *   one or more image paths (JPEG, PNG, or BMP). Good sizes: 16x16, 32x32, 48x48.
    */
  def setWindowIcon(fileType: FileType, filePaths: String*): Unit = {
    windowIconFileType = Nullable(fileType)
    windowIconPaths = Nullable(filePaths.toArray)
  }

  /** Copies all window configuration fields from another config. */
  def setWindowConfiguration(config: DesktopWindowConfig): Unit = {
    windowX = config.windowX
    windowY = config.windowY
    windowWidth = config.windowWidth
    windowHeight = config.windowHeight
    windowMinWidth = config.windowMinWidth
    windowMinHeight = config.windowMinHeight
    windowMaxWidth = config.windowMaxWidth
    windowMaxHeight = config.windowMaxHeight
    windowResizable = config.windowResizable
    windowDecorated = config.windowDecorated
    windowMaximized = config.windowMaximized
    maximizedMonitor = config.maximizedMonitor
    autoIconify = config.autoIconify
    windowIconFileType = config.windowIconFileType
    windowIconPaths = config.windowIconPaths.fold(Nullable.empty[Array[String]])(paths => Nullable(paths.clone()))
    windowListener = config.windowListener
    fullscreenMode = config.fullscreenMode
    title = config.title
    initialBackgroundColor = config.initialBackgroundColor
    initialVisible = config.initialVisible
    vSyncEnabled = config.vSyncEnabled
  }
}
