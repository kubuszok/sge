/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Cursor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Cursor -> DesktopCursor
 *   Convention: GLFW calls via WindowingOps FFI trait (not direct LWJGL)
 *   Convention: custom Pixmap cursor deferred (needs WindowingOps.createCursor with image data)
 *   Convention: system cursor cache + setSystemCursor use WindowingOps.createStandardCursor/setCursor
 *   Idiom: Nullable (0 null), split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.Cursor
import sge.platform.PlatformOps
import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

/** Desktop [[Cursor]] implementation backed by native windowing system cursors (GLFW/SDL3 via [[sge.platform.WindowingOps]]).
  *
  * @param glfwCursor
  *   the native cursor handle returned by the windowing system
  */
class DesktopCursor private[sge] (val glfwCursor: Long) extends Cursor {

  override def close(): Unit =
    if (glfwCursor != 0) {
      DesktopCursor.cursors -= this
      PlatformOps.windowing.destroyCursor(glfwCursor)
    }
}

object DesktopCursor {

  /** All active cursors (for bulk cleanup when a window is destroyed). */
  private[sge] val cursors: mutable.ArrayBuffer[DesktopCursor] = mutable.ArrayBuffer.empty

  /** Cached system cursor handles, keyed by SystemCursor enum. */
  private val systemCursors: mutable.Map[Cursor.SystemCursor, Long] = mutable.Map.empty

  /** The GLFW input mode before SystemCursor.None was set, or -1 if not set. */
  private var inputModeBeforeNoneCursor: Int = -1

  // GLFW constants for cursor input mode
  private val GLFW_CURSOR:        Int = 0x00033001
  private val GLFW_CURSOR_HIDDEN: Int = 0x00034002

  // GLFW standard cursor shape constants
  private val GLFW_ARROW_CURSOR:       Int = 0x00036001
  private val GLFW_IBEAM_CURSOR:       Int = 0x00036002
  private val GLFW_CROSSHAIR_CURSOR:   Int = 0x00036003
  private val GLFW_HAND_CURSOR:        Int = 0x00036004 // "pointing hand"
  private val GLFW_HRESIZE_CURSOR:     Int = 0x00036005
  private val GLFW_VRESIZE_CURSOR:     Int = 0x00036006
  private val GLFW_RESIZE_NWSE_CURSOR: Int = 0x00036007
  private val GLFW_RESIZE_NESW_CURSOR: Int = 0x00036008
  private val GLFW_RESIZE_ALL_CURSOR:  Int = 0x00036009
  private val GLFW_NOT_ALLOWED_CURSOR: Int = 0x0003600a

  /** Sets a system cursor on the given window handle.
    *
    * @param windowHandle
    *   the native window handle
    * @param systemCursor
    *   the system cursor to set
    */
  private[sge] def setSystemCursor(windowHandle: Long, systemCursor: Cursor.SystemCursor): Unit = boundary {
    val wops = PlatformOps.windowing

    if (systemCursor == Cursor.SystemCursor.None) {
      if (inputModeBeforeNoneCursor == -1) {
        inputModeBeforeNoneCursor = wops.getInputMode(windowHandle, GLFW_CURSOR)
      }
      wops.setInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
      break(())
    } else if (inputModeBeforeNoneCursor != -1) {
      wops.setInputMode(windowHandle, GLFW_CURSOR, inputModeBeforeNoneCursor)
      inputModeBeforeNoneCursor = -1
    }

    val glfwCursor = systemCursors.getOrElse(
      systemCursor, {
        val shape = systemCursor match {
          case Cursor.SystemCursor.Arrow            => GLFW_ARROW_CURSOR
          case Cursor.SystemCursor.Crosshair        => GLFW_CROSSHAIR_CURSOR
          case Cursor.SystemCursor.Hand             => GLFW_HAND_CURSOR
          case Cursor.SystemCursor.HorizontalResize => GLFW_HRESIZE_CURSOR
          case Cursor.SystemCursor.VerticalResize   => GLFW_VRESIZE_CURSOR
          case Cursor.SystemCursor.Ibeam            => GLFW_IBEAM_CURSOR
          case Cursor.SystemCursor.NWSEResize       => GLFW_RESIZE_NWSE_CURSOR
          case Cursor.SystemCursor.NESWResize       => GLFW_RESIZE_NESW_CURSOR
          case Cursor.SystemCursor.AllResize        => GLFW_RESIZE_ALL_CURSOR
          case Cursor.SystemCursor.NotAllowed       => GLFW_NOT_ALLOWED_CURSOR
          case Cursor.SystemCursor.None             => 0 // handled above
        }
        val handle = wops.createStandardCursor(shape)
        if (handle == 0) break(())
        systemCursors.put(systemCursor, handle)
        handle
      }
    )

    wops.setCursor(windowHandle, glfwCursor)
  }

  /** Disposes all system cursors. Called during application shutdown. */
  private[sge] def disposeSystemCursors(): Unit = {
    systemCursors.values.foreach(PlatformOps.windowing.destroyCursor)
    systemCursors.clear()
  }

  /** Disposes all cursors associated with a specific window. */
  private[sge] def disposeForWindow(window: DesktopWindow): Unit = {
    val toRemove = cursors.filter(_.glfwCursor != 0)
    toRemove.foreach(_.close())
  }
}
